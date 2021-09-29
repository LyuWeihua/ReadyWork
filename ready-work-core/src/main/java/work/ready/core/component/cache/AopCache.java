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

package work.ready.core.component.cache;

import java.util.List;

public class AopCache {

    private Cache aopCache;

    AopCache(Cache aopCache){
        this.aopCache = aopCache;
    }

    public <T> T get(String cacheName, Object key) {
        return aopCache.get(cacheName, key);
    }

    public void put(String cacheName, Object key, Object value) {
        aopCache.put(cacheName, key, value);
    }

    public void put(String cacheName, Object key, Object value, int liveSeconds) {
        aopCache.put(cacheName, key, value, liveSeconds);
    }

    public List getKeys(String cacheName) {
        return aopCache.getKeys(cacheName);
    }

    public void remove(String cacheName, Object key) {
        aopCache.remove(cacheName, key);
    }

    public void removeAll(String cacheName) {
        aopCache.removeAll(cacheName);
    }

    public <T> T get(String cacheName, Object key, DataLoader dataLoader) {
        return aopCache.get(cacheName, key, dataLoader);
    }

    public <T> T get(String cacheName, Object key, DataLoader dataLoader, int liveSeconds) {
        return aopCache.get(cacheName, key, dataLoader, liveSeconds);
    }

    public Integer getTtl(String cacheName, Object key) {
        return aopCache.getTtl(cacheName, key);
    }

    public void setTtl(String cacheName, Object key, int seconds) {
        aopCache.setTtl(cacheName, key, seconds);
    }
}
