/**
 *
 * Original work Copyright (c) 2015-2020, Michael Yang 杨福海 (fuhai999@gmail.com).
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
package work.ready.core.component.cache.redis;

import work.ready.core.component.cache.BaseCache;
import work.ready.core.component.cache.DataLoader;
import work.ready.core.component.redis.Redis;
import work.ready.core.component.redis.RedisScanResult;
import work.ready.core.component.redis.jedis.Jedis;
import work.ready.core.component.redis.jedis.JedisCluster;
import work.ready.core.component.redis.RedisConfig;
import work.ready.core.server.Ready;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RedisCache extends BaseCache {

    private Redis redis;
    private static final Object dataLocker = new Object();
    private static final String redisCacheNamesKey = "cache_names";

    public RedisCache() {
        RedisConfig config = Ready.cacheManager().getConfig().getRedis();
        if(config == null) config = new RedisConfig();
        try {
            redis = config.isCluster() ? new JedisCluster(config) : new Jedis(config);
        } catch (Exception e) {
            throw new RuntimeException("Redis client initialize failed, please check the cache paragraph of application config.");
        }
    }

    @Override
    public <T> T get(String cacheName, Object key) {
        T value = redis.get(buildKey(cacheName, key));
        return NULL.equals(value) ? null : value;
    }

    @Override
    public void put(String cacheName, Object key, Object value) {
        redis.set(buildKey(cacheName, key), wrapNull(value)); 
        redis.sadd(redisCacheNamesKey, cacheName);
    }

    @Override
    public void put(String cacheName, Object key, Object value, int liveSeconds) {
        if (liveSeconds <= 0) {
            put(cacheName, key, value);
            return;
        }

        redis.setex(buildKey(cacheName, key), liveSeconds, wrapNull(value));
        redis.sadd(redisCacheNamesKey, cacheName);
    }

    @Override
    public void remove(String cacheName, Object key) {
        redis.del(buildKey(cacheName, key));
    }

    @Override
    public void removeAll(String cacheName) {
        String cursor = "0";
        int scanCount = 1000;
        List<String> scanKeys = null;
        do {
            RedisScanResult redisScanResult = redis.scan(cacheName + ":*", cursor, scanCount);
            if (redisScanResult != null) {
                scanKeys = redisScanResult.getResults();
                cursor = redisScanResult.getCursor();

                if (scanKeys != null && scanKeys.size() > 0){
                    redis.del(scanKeys.toArray(new String[0]));
                }

                if (redisScanResult.isCompleteIteration()) {
                    
                    scanKeys = null;
                }
            }
        } while (scanKeys != null && scanKeys.size() != 0);

        redis.srem(redisCacheNamesKey, cacheName);
    }

    @Override
    public <T> T get(String cacheName, Object key, DataLoader dataLoader) {
        Object data = get(cacheName, key);
        if (data == null) {
            synchronized (dataLocker) {
                data = get(cacheName, key);
                if (data == null) {
                    data = dataLoader.load();
                    put(cacheName, key, data);
                }
            }
        }
        return (T) data;
    }

    private String buildKey(String cacheName, Object key) {
        StringBuilder keyBuilder = new StringBuilder(cacheName).append(":");
        if (key instanceof String) {
            keyBuilder.append("S");
        } else if (key instanceof Number) {
            keyBuilder.append("I");
        } else if (key == null) {
            keyBuilder.append("S");
            key = "null";
        } else {
            keyBuilder.append("O");
        }
        return keyBuilder.append(":").append(key).toString();
    }

    @Override
    public <T> T get(String cacheName, Object key, DataLoader dataLoader, int liveSeconds) {
        if (liveSeconds <= 0) {
            return get(cacheName, key, dataLoader);
        }

        Object data = get(cacheName, key);
        if (data == null) {
            synchronized (dataLocker) {
                data = get(cacheName, key);
                if (data == null) {
                    data = dataLoader.load();
                    put(cacheName, key, data, liveSeconds);
                }
            }
        }
        return (T) data;
    }

    @Override
    public Integer getTtl(String cacheName, Object key) {
        Long ttl = redis.ttl(buildKey(cacheName, key));
        return ttl != null ? ttl.intValue() : null;
    }

    @Override
    public void setTtl(String cacheName, Object key, int seconds) {
        redis.expire(buildKey(cacheName, key), seconds);
    }

    @Override
    public List getNames() {
        Set set = redis.smembers(redisCacheNamesKey);
        return set == null ? null : new ArrayList(set);
    }

    @Override
    public List getKeys(String cacheName) {
        List<String> keys = new ArrayList<>();
        String cursor = "0";
        int scanCount = 1000;
        List<String> scanKeys = null;
        do {
            RedisScanResult redisScanResult = redis.scan(cacheName + ":*", cursor, scanCount);
            if (redisScanResult != null) {
                scanKeys = redisScanResult.getResults();
                cursor = redisScanResult.getCursor();

                if (scanKeys != null && scanKeys.size() > 0) {
                    for (String key : scanKeys) {
                        keys.add(key.substring(cacheName.length() + 3));
                    }
                }

                if (redisScanResult.isCompleteIteration()) {
                    
                    scanKeys = null;
                }
            }
        } while (scanKeys != null && scanKeys.size() != 0);

        return keys;
    }

    @Override
    public void refresh(String cacheName, Object key) {

    }

    @Override
    public void refresh(String cacheName) {

    }

    public Redis getRedis() {
        return redis;
    }

    public redis.clients.jedis.Jedis getJedis() {
        return redis instanceof Jedis ? ((Jedis) redis).getJedis() : null;
    }

    public redis.clients.jedis.JedisCluster getJedisCluster() {
        return redis instanceof JedisCluster ? ((JedisCluster) redis).getJedisCluster() : null;
    }
}
