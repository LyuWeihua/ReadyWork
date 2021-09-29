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
package work.ready.core.component.cache;

import work.ready.core.aop.AopComponent;
import work.ready.core.component.cache.annotation.CacheEvict;
import work.ready.core.component.cache.annotation.CachePut;
import work.ready.core.component.cache.annotation.Cacheable;
import work.ready.core.component.cache.annotation.CachesEvict;
import work.ready.core.component.cache.caffeine.CaffeineCache;
import work.ready.core.component.cache.interceptor.CacheEvictInterceptor;
import work.ready.core.component.cache.interceptor.CacheInterceptor;
import work.ready.core.component.cache.interceptor.CachePutInterceptor;
import work.ready.core.component.cache.interceptor.CachesEvictInterceptor;
import work.ready.core.event.Event;
import work.ready.core.event.GeneralEvent;
import work.ready.core.module.CoreContext;
import work.ready.core.module.Initializer;
import work.ready.core.server.Ready;
import work.ready.core.tools.ClassUtil;
import work.ready.core.tools.StrUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CacheManager {

    private final CoreContext context;
    private AopCache aopCache;
    private Cache dbCache;
    protected List<Initializer<CacheManager>> initializers = new ArrayList<>();
    private Map<String, Cache> cacheMap = new ConcurrentHashMap<>();
    private CacheConfig config;

    public CacheManager(CoreContext context) {
        this.context = context;
        config = Ready.getMainApplicationConfig().getCache();
        if(config == null) config = new CacheConfig();
        Ready.post(new GeneralEvent(Event.CACHE_MANAGER_CREATE, this));
    }

    public void addCacheType(String cacheType, Class<? extends Cache> cacheClass){
        config.cacheProvider.put(cacheType, cacheClass);
    }

    public void addInitializer(Initializer<CacheManager> initializer) {
        this.initializers.add(initializer);
        initializers.sort(Comparator.comparing(Initializer::order));
    }

    public void startInit() {
        try {
            for (Initializer<CacheManager> i : initializers) {
                i.startInit(this);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if(StrUtil.notBlank(config.getAopCacheType())){
            aopCache = new AopCache(getCache(config.getAopCacheType()));
        } else {
            aopCache = new AopCache(new CaffeineCache()); 
        }
        if(StrUtil.notBlank(config.getDbCacheType())){
            dbCache = getCache(config.getDbCacheType());
        } else {
            dbCache = new CaffeineCache(); 
        }

        interceptorIntegration();
    }

    public void endInit() {
        try {
            for (Initializer<CacheManager> i : initializers) {
                i.endInit(this);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void interceptorIntegration(){
        context.getInterceptorManager().addAopComponent(new AopComponent().setAnnotation(CacheEvict.class)
                .setInterceptorClass(CacheEvictInterceptor.class));
        context.getInterceptorManager().addAopComponent(new AopComponent().setAnnotation(CachesEvict.class)
                .setInterceptorClass(CachesEvictInterceptor.class));
        context.getInterceptorManager().addAopComponent(new AopComponent().setAnnotation(CachePut.class)
                .setInterceptorClass(CachePutInterceptor.class));
        context.getInterceptorManager().addAopComponent(new AopComponent().setAnnotation(Cacheable.class)
                .setInterceptorClass(CacheInterceptor.class));
    }

    public CacheConfig getConfig() { return config; }

    public Cache getDbCache(){
        return dbCache;
    }

    public AopCache getAopCache() { return aopCache; }

    public Cache getCache() {
        return getCache(config.getCacheType());
    }

    public Cache getCache(String type) {
        if (StrUtil.isBlank(type)) {
            throw new IllegalArgumentException("type must not be null or blank.");
        }

        Cache cache = cacheMap.get(type);
        if (cache != null) {
            return cache;
        }

        synchronized (type.intern()) {
            cache = cacheMap.get(type);
            if (cache == null) {
                cache = buildCache(type, config);
                cacheMap.put(type, cache);
            }
        }

        return cache;
    }

    private Cache buildCache(String type, CacheConfig config) {
        for(Map.Entry<String, Class<? extends Cache>> entry : config.cacheProvider.entrySet()){
            if(type.equals(entry.getKey())) return (Cache)ClassUtil.newInstance(entry.getValue());
        }

        ServiceLoader<Cache> cacheServiceLoader = ServiceLoader.load(Cache.class, CacheManager.class.getClassLoader());
        return cacheServiceLoader.findFirst().orElseThrow(()->new RuntimeException("Unknown type of cache: " + config.getCacheType()));
    }
}
