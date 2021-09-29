package work.ready.examples.cache.service;

import work.ready.core.component.cache.Cache;
import work.ready.core.ioc.annotation.Service;
import work.ready.core.server.Ready;

import java.util.List;

@Service
public class CacheService {

    private final Cache cache = Ready.cacheManager().getCache();
    private final String cacheName = "DefaultCache";

    public <T> void putCache(String key, T value) {
        cache.put(cacheName, key, value);
    }

    public <T> void putCache(String key, T value, int liveSeconds) {
        cache.put(cacheName, key, value, liveSeconds);
    }

    public <T> T getCache(String key) {
        return cache.get(cacheName, key);
    }

    public void removeCache(String key) {
        cache.remove(cacheName, key);
    }

    public List getKeys() {
        return cache.getKeys(cacheName);
    }
}
