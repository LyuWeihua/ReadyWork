/**
 *
 * Original work Copyright (c) 2016 Coveo, under the MIT License
 * Modified work Copyright (c) 2020 WeiHua Lyu [ready.work]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package work.ready.core.security.access.limiter.storage;

import work.ready.core.component.redis.RedisConfig;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.security.access.limiter.limit.LimitKey;
import work.ready.core.security.access.limiter.storage.utils.AddAndGetRequest;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import work.ready.core.server.Ready;
import work.ready.core.tools.StrUtil;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RedisStorage implements LimitUsageStorage {
  private static final Log logger = LogFactory.getLog(RedisStorage.class);

  public static final String DEFAULT_PREFIX = "limiter";
  public static final String KEY_SEPARATOR = "|";

  private static final String KEY_SEPARATOR_SUBSTITUTE = "_";
  private static final String WILD_CARD_OPERATOR = "*";
  private static final String COUNTER_SCRIPT =
      "local counter = redis.call('INCRBY', KEYS[1], ARGV[1]); "
          + "if counter  > tonumber(ARGV[2]) + tonumber(ARGV[1])"
          + "then counter = redis.call('INCRBY', KEYS[1], -ARGV[1]) "
          + "end "
          + "return tostring(counter)";

  private final JedisPool jedisPool;
  private final String keyPrefix;

  public RedisStorage(){
    this.keyPrefix = "Limiter_";
    this.jedisPool = configJedisPool();
  }

  public RedisStorage(Builder builder) {
    this.jedisPool = builder.jedisPool;
    this.keyPrefix = builder.keyPrefix;
  }

  private static JedisPool configJedisPool(){
    RedisConfig config = Ready.cacheManager().getConfig().getRedis();
    if(config == null || !config.isConfigOk()) {
      logger.warn("Redis is required, but Redis config is incorrect, trying to get local default redis.");
      config = new RedisConfig();
    }
    return new JedisPool(config.getHost(), config.getPort());
  }

  @Override
  public Map<LimitKey, Integer> addAndGet(Collection<AddAndGetRequest> requests) {
    Map<LimitKey, Response<Long>> responses = new LinkedHashMap<>();

    try (Jedis jedis = jedisPool.getResource()) {
      try (Pipeline pipeline = jedis.pipelined()) {
        for (AddAndGetRequest request : requests) {
          pipeline.multi();
          LimitKey limitKey = LimitKey.fromRequest(request);
          String redisKey =
              Stream.of(
                      keyPrefix,
                      limitKey.getResource(),
                      limitKey.getLimitName(),
                      limitKey.getProperty(),
                      limitKey.getBucket().toString(),
                      limitKey.getExpiration().toString())
                  .map(RedisStorage::clean)
                  .collect(Collectors.joining(KEY_SEPARATOR));

          responses.put(limitKey, pipeline.incrBy(redisKey, request.getCost()));

          pipeline.expire(redisKey, (int) request.getExpiration().getSeconds() * 2);
          pipeline.exec();
        }

        pipeline.sync();
      } catch (Exception e) {
        logger.error("Exception of redis storage pipeline.", e);
      }
    }

    return responses
        .entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, kvp -> kvp.getValue().get().intValue()));
  }

  @Override
  public Map<LimitKey, Integer> addAndGetWithLimit(Collection<AddAndGetRequest> requests) {
    Map<LimitKey, Response<Object>> responses = new LinkedHashMap<>();

    try (Jedis jedis = jedisPool.getResource()) {
      try (Pipeline pipeline = jedis.pipelined()) {
        requests.forEach(
            request -> {
              pipeline.multi();
              LimitKey limitKey = LimitKey.fromRequest(request);
              String redisKey =
                  Stream.of(
                          keyPrefix,
                          limitKey.getResource(),
                          limitKey.getLimitName(),
                          limitKey.getProperty(),
                          limitKey.getBucket().toString(),
                          limitKey.getExpiration().toString())
                      .map(RedisStorage::clean)
                      .collect(Collectors.joining(KEY_SEPARATOR));

              responses.put(
                  limitKey,
                  pipeline.eval(
                      COUNTER_SCRIPT,
                      Collections.singletonList(redisKey),
                      Arrays.asList(
                          String.valueOf(request.getCost()), String.valueOf(request.getLimit()))));
              pipeline.expire(redisKey, (int) request.getExpiration().getSeconds() * 2);
              pipeline.exec();
            });
        pipeline.sync();
      } catch (Exception e) {
        logger.error("Exception of redis storage pipeline.", e);
      }
    }

    return responses
        .entrySet()
        .stream()
        .collect(
            Collectors.toMap(Map.Entry::getKey, kvp -> Integer.parseInt(kvp.getValue().get().toString())));
  }

  @Override
  public Map<LimitKey, Integer> getCurrentLimitCounters() {
    return getLimits(buildKeyPattern(keyPrefix, WILD_CARD_OPERATOR));
  }

  @Override
  public Map<LimitKey, Integer> getCurrentLimitCounters(String resource) {
    return getLimits(buildKeyPattern(keyPrefix, resource, WILD_CARD_OPERATOR));
  }

  @Override
  public Map<LimitKey, Integer> getCurrentLimitCounters(String resource, String limitName) {
    return getLimits(buildKeyPattern(keyPrefix, resource, limitName, WILD_CARD_OPERATOR));
  }

  @Override
  public Map<LimitKey, Integer> getCurrentLimitCounters(
      String resource, String limitName, String property) {
    return getLimits(buildKeyPattern(keyPrefix, resource, limitName, property, WILD_CARD_OPERATOR));
  }

  private Map<LimitKey, Integer> getLimits(String keyPattern) {
    Map<LimitKey, Integer> counters = new HashMap<>();

    try (Jedis jedis = jedisPool.getResource()) {
      Set<String> keys = jedis.keys(keyPattern);
      for (String key : keys) {
        String valueAsString = jedis.get(key);
        if (StrUtil.notBlank(valueAsString)) {
          int value = Integer.parseInt(valueAsString);

          String[] keyComponents = StrUtil.split(key, KEY_SEPARATOR);

          counters.put(
              new LimitKey(
                  keyComponents[1],
                  keyComponents[2],
                  keyComponents[3],
                  true,
                  Instant.parse(keyComponents[4]),
                  keyComponents.length == 6
                      ? Duration.parse(keyComponents[5])
                      : Duration
                          .ZERO), 
              value);
        } else {
          logger.info("Key '%s' has no value and will not be included in counters", key);
        }
      }
    }
    return Collections.unmodifiableMap(counters);
  }

  @Override
  public void close() {
    jedisPool.destroy();
  }

  private String buildKeyPattern(String... keyComponents) {
    return Arrays.asList(keyComponents)
        .stream()
        .map(RedisStorage::clean)
        .collect(Collectors.joining(KEY_SEPARATOR));
  }

  private static final String clean(String keyComponent) {
    return keyComponent.replace(KEY_SEPARATOR, KEY_SEPARATOR_SUBSTITUTE);
  }

  public static final Builder builder() {
    return new Builder();
  }

  public static class Builder {
    JedisPool jedisPool;
    String keyPrefix;

    private Builder() {
      this.keyPrefix = RedisStorage.DEFAULT_PREFIX;
    }

    public void setJedisPool(JedisPool jedisPool) {
      this.jedisPool = jedisPool;
    }

    public Builder withJedisPool(JedisPool jedisPool) {
      setJedisPool(jedisPool);
      return this;
    }

    public void setKeyPrefix(String keyPrefix) {
      this.keyPrefix = keyPrefix;
    }

    public Builder withKeyPrefix(String keyPrefix) {
      setKeyPrefix(keyPrefix);
      return this;
    }

    public RedisStorage build() {
      if(this.jedisPool == null) {
        keyPrefix = "Limiter_";
        jedisPool = configJedisPool();
      }
      return new RedisStorage(this);
    }
  }
}
