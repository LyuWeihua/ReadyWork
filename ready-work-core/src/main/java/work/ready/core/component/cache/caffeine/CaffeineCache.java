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
package work.ready.core.component.cache.caffeine;

import com.github.benmanes.caffeine.cache.Cache;
import work.ready.core.component.cache.BaseCache;
import work.ready.core.component.cache.DataLoader;
import work.ready.core.server.Ready;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CaffeineCache extends BaseCache {

    private Map<String, Cache> cacheMap = new ConcurrentHashMap<>();

    protected Cache getCache(String cacheName) {
        return cacheMap.get(cacheName);
    }

    protected Cache getOrCreateCache(String cacheName) {
        Cache cache = cacheMap.get(cacheName);
        if (cache == null) {
            synchronized (CaffeineCache.class) {
                cache = cacheMap.get(cacheName);
                if (cache == null) {
                    cache = createCacheBuilder().build(cacheName);
                    cacheMap.put(cacheName,cache);
                }
            }
        }
        return cache;
    }

    protected CaffeineCacheBuilder createCacheBuilder() {
        return new DefaultCaffeineCacheBuilder();
    }

    @Override
    public <T> T get(String cacheName, Object key) {
        Cache cache = getOrCreateCache(cacheName);
        CaffeineCacheObject data = (CaffeineCacheObject) cache.getIfPresent(key);
        if (data == null) {
            return null;
        }

        if (data.isDue()) {
            cache.invalidate(key);
            return null;
        }
        return (T) data.getValue();
    }

    @Override
    public void put(String cacheName, Object key, Object value) {
        putData(getOrCreateCache(cacheName), key, new CaffeineCacheObject(value));
    }

    @Override
    public void put(String cacheName, Object key, Object value, int liveSeconds) {
        putData(getOrCreateCache(cacheName), key, new CaffeineCacheObject(value, liveSeconds));
    }

    @Override
    public void remove(String cacheName, Object key) {
        Cache cache = getCache(cacheName);
        if (cache != null) {
            cache.invalidate(key);
        }
    }

    @Override
    public void removeAll(String cacheName) {
        Cache cache = getCache(cacheName);
        if (cache != null) {
            cache.invalidateAll();
        }
        cacheMap.remove(cacheName);
    }

    @Override
    public <T> T get(String cacheName, Object key, DataLoader dataLoader) {
        Cache cache = getOrCreateCache(cacheName);
        CaffeineCacheObject data = (CaffeineCacheObject) cache.getIfPresent(key);
        if (data == null || data.isDue()) {
            Object newValue = dataLoader.load();
            if (newValue != null) {
                data = new CaffeineCacheObject(newValue);
                putData(cache, key, data);
            }
            return (T) newValue;
        } else {
            return (T) data.getValue();
        }
    }

    @Override
    public <T> T get(String cacheName, Object key, DataLoader dataLoader, int liveSeconds) {
        Cache cache = getOrCreateCache(cacheName);
        CaffeineCacheObject data = (CaffeineCacheObject) cache.getIfPresent(key);
        if (data == null || data.isDue()) {
            Object newValue = dataLoader.load();
            if (newValue != null) {
                data = new CaffeineCacheObject(newValue, liveSeconds);
                putData(cache, key, data);
            }
            return (T) newValue;
        } else {
            return (T) data.getValue();
        }
    }

    @Override
    public Integer getTtl(String cacheName, Object key) {
        Cache cache = getCache(cacheName);
        if (cache == null) {
            return null;
        }

        CaffeineCacheObject data = (CaffeineCacheObject) cache.getIfPresent(key);
        if (data == null) {
            return null;
        }

        return data.getTtl();
    }

    @Override
    public void setTtl(String cacheName, Object key, int seconds) {
        Cache cache = getCache(cacheName);
        if (cache == null) {
            return;
        }

        CaffeineCacheObject data = (CaffeineCacheObject) cache.getIfPresent(key);
        if (data == null) {
            return;
        }

        data.setLiveSeconds(seconds);
        putData(cache, key, data);
    }

    @Override
    public List getNames() {
        return new ArrayList(cacheMap.keySet());
    }

    @Override
    public List getKeys(String cacheName) {
        Cache cache = getCache(cacheName);
        return cache == null ? null : new ArrayList(cache.asMap().keySet());
    }

    protected void putData(Cache cache, Object key, CaffeineCacheObject value) {
        value.setCachetime(Ready.currentTimeMillis());
        cache.put(key, value);
    }

    @Override
    public void refresh(String cacheName, Object key) {

    }

    @Override
    public void refresh(String cacheName) {

    }
}
