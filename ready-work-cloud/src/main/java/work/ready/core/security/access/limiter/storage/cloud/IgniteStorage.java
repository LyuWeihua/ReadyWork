/**
 *
 * Copyright (c) 2020 WeiHua Lyu [ready.work]
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

package work.ready.core.security.access.limiter.storage.cloud;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.configuration.CacheConfiguration;
import work.ready.cloud.ReadyCloud;
import work.ready.cloud.cluster.Cloud;
import work.ready.core.security.access.limiter.limit.LimitKey;
import work.ready.core.security.access.limiter.storage.LimitUsageStorage;
import work.ready.core.security.access.limiter.storage.utils.AddAndGetRequest;
import work.ready.core.tools.StrUtil;

import javax.cache.Cache;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IgniteStorage implements LimitUsageStorage {

    private static IgniteCache<String, Integer> limiterStorage;
    private static IgniteStorage instance;

     static final String KEY_SEPARATOR = "|";
    private static final String KEY_SEPARATOR_SUBSTITUTE = "_";

    private IgniteStorage(){}  

    public static IgniteStorage getStorage(){
        if(!ReadyCloud.isReady()) {
            throw new RuntimeException("IgniteStorage for Distributed Limiter depends on ReadyCloud, please start server with cloud mode.");
        }
        if(instance == null) {
            synchronized (IgniteStorage.class) {
                if (instance == null) {
                    CacheConfiguration<String, Integer> config = new CacheConfiguration<>();
                    config.setName("LimiterStorage");
                    config.setEventsDisabled(true);
                    config.setCacheMode(ReadyCloud.getNodeMode().getMode());
                    config.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
                    config.setDataRegionName(Cloud.WITHOUT_PERSISTENCE);
                    limiterStorage = Cloud.getOrCreateCache(config);
                    instance = new IgniteStorage();
                }
            }
        }
        return instance;
    }

    @Override
    public Map<LimitKey, Integer> addAndGet(Collection<AddAndGetRequest> requests) {
        Map<LimitKey, Integer> responses = new LinkedHashMap<>();
        for (AddAndGetRequest request : requests) {
            LimitKey limitKey = LimitKey.fromRequest(request);
            String cacheKey =
                    Stream.of(
                            limitKey.getResource(),
                            limitKey.getLimitName(),
                            limitKey.getProperty(),
                            limitKey.getBucket().toString(),
                            limitKey.getExpiration().toString())
                            .map(IgniteStorage::clean)
                            .collect(Collectors.joining(KEY_SEPARATOR));

            ExpiryPolicy plc = new CreatedExpiryPolicy(new Duration(TimeUnit.SECONDS, (int) request.getExpiration().getSeconds() * 2));
            Lock lock = limiterStorage.lock(cacheKey + "_lock");
            lock.lock();
            try {
                Integer val = limiterStorage.get(cacheKey);
                val = val == null ? request.getCost() : val + request.getCost();
                limiterStorage.withExpiryPolicy(plc).put(cacheKey, val);
                responses.put(limitKey, val);
            } finally {
                lock.unlock();
            }
        }
        return responses;
    }

    @Override
    public Map<LimitKey, Integer> addAndGetWithLimit(Collection<AddAndGetRequest> requests) {
        Map<LimitKey, Integer> responses = new LinkedHashMap<>();
        for (AddAndGetRequest request : requests) {
            LimitKey limitKey = LimitKey.fromRequest(request);
            String cacheKey =
                    Stream.of(
                            limitKey.getResource(),
                            limitKey.getLimitName(),
                            limitKey.getProperty(),
                            limitKey.getBucket().toString(),
                            limitKey.getExpiration().toString())
                            .map(IgniteStorage::clean)
                            .collect(Collectors.joining(KEY_SEPARATOR));

            ExpiryPolicy plc = new CreatedExpiryPolicy(new Duration(TimeUnit.SECONDS, (int) request.getExpiration().getSeconds() * 2));
            Lock lock = limiterStorage.lock(cacheKey + "_lock");
            lock.lock();
            try {
                Integer val = limiterStorage.get(cacheKey);
                val = val == null ? request.getCost() : val > request.getLimit() ? val : val + request.getCost();
                limiterStorage.withExpiryPolicy(plc).put(cacheKey, val);
                responses.put(limitKey, val);
            } finally {
                lock.unlock();
            }
        }
        return responses;
    }

    @Override
    public Map<LimitKey, Integer> getCurrentLimitCounters() {
        return getLimits(null);
    }

    @Override
    public Map<LimitKey, Integer> getCurrentLimitCounters(String resource) {
        return getLimits(buildKeyPattern(resource));
    }

    @Override
    public Map<LimitKey, Integer> getCurrentLimitCounters(String resource, String limitName) {
        return getLimits(buildKeyPattern(resource, limitName));
    }

    @Override
    public Map<LimitKey, Integer> getCurrentLimitCounters(String resource, String limitName, String property) {
        return getLimits(buildKeyPattern(resource, limitName, property));
    }

    @Override
    public void close() { }

    private Map<LimitKey, Integer> getLimits(String keyPattern) {
        Map<LimitKey, Integer> counters = new LinkedHashMap<>();
        Iterator<Cache.Entry<String, Integer>> it = limiterStorage.iterator();
        while (it.hasNext()) {
            String key = it.next().getKey();
            if(StrUtil.notBlank(keyPattern) && !key.startsWith(keyPattern)) continue;
            Integer val = it.next().getValue();
            String[] keyComponents = StrUtil.split(key, KEY_SEPARATOR);
            counters.put(
                    new LimitKey(
                            keyComponents[1],
                            keyComponents[2],
                            keyComponents[3],
                            true,
                            Instant.parse(keyComponents[4]),
                            keyComponents.length == 6
                                    ? java.time.Duration.parse(keyComponents[5])
                                    : java.time.Duration
                                    .ZERO), 
                    val);
        }
        return Collections.unmodifiableMap(counters);
    }

    private static final String clean(String keyComponent) {
        return keyComponent.replace(KEY_SEPARATOR, KEY_SEPARATOR_SUBSTITUTE);
    }

    private String buildKeyPattern(String... keyComponents) {
        return Arrays.asList(keyComponents)
                .stream()
                .map(IgniteStorage::clean)
                .collect(Collectors.joining(KEY_SEPARATOR));
    }
}
