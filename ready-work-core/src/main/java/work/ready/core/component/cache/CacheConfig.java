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

import work.ready.core.component.cache.caffeine.CaffeineCache;
import work.ready.core.component.cache.redis.RedisCache;
import work.ready.core.component.redis.RedisConfig;
import work.ready.core.config.BaseConfig;

import java.util.HashMap;
import java.util.Map;

public class CacheConfig extends BaseConfig {

    public static final String TYPE_IGNITE = "ignite";
    public static final String TYPE_REDIS = "redis";
    public static final String TYPE_CAFFEINE = "caffeine";
    public static final String TYPE_NONE = "none";

    final Map<String, Class<? extends Cache>> cacheProvider = new HashMap<>();

    private String cacheType = TYPE_CAFFEINE;   

    private RedisConfig redis;

    private int aopCacheLiveSeconds = 0;
    private String aopCacheType;
    private String dbCacheType;

    public CacheConfig() {
        cacheProvider.put(TYPE_NONE, DummyCache.class);
        cacheProvider.put(TYPE_CAFFEINE, CaffeineCache.class);
        cacheProvider.put(TYPE_REDIS, RedisCache.class);
    }

    public String getCacheType() {
        return cacheType;
    }

    public void setCacheType(String cacheType) {
        this.cacheType = cacheType;
    }

    public RedisConfig getRedis(){
        return redis;
    }

    public void setRedis(RedisConfig redis){
        this.redis = redis;
    }

    public int getAopCacheLiveSeconds() {
        return aopCacheLiveSeconds;
    }

    public void setAopCacheLiveSeconds(int aopCacheLiveSeconds) {
        this.aopCacheLiveSeconds = aopCacheLiveSeconds;
    }

    public String getAopCacheType() {
        return aopCacheType;
    }

    public void setAopCacheType(String aopCacheType) {
        this.aopCacheType = aopCacheType;
    }

    public String getDbCacheType() {
        return dbCacheType;
    }

    public void setDbCacheType(String dbCacheType) {
        this.dbCacheType = dbCacheType;
    }

    @Override
    public void validate() { 
    }
}
