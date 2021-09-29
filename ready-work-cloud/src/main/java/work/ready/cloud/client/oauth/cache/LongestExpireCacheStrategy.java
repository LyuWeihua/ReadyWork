/**
 *
 * Original work copyright (c) 2016 Network New Technologies Inc.
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
package work.ready.cloud.client.oauth.cache;

import work.ready.cloud.client.oauth.Jwt;

import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

public class LongestExpireCacheStrategy implements CacheStrategy {
    
    private final PriorityBlockingQueue<LongestExpireCacheKey> expiryQueue;

    private final ConcurrentHashMap<Jwt.Key, Jwt> cachedJwts;

    private int capacity;

    public LongestExpireCacheStrategy(int capacity) {
        this.capacity = capacity;
        Comparator<LongestExpireCacheKey> comparator = (o1, o2) -> {
            if(o1.getExpiry() > o2.getExpiry()) {
                return 1;
            } else if(o1.getExpiry() == o2.getExpiry()){
                return 0;
            } else {
                return -1;
            }
        };
        expiryQueue = new PriorityBlockingQueue(capacity, comparator);
        cachedJwts = new ConcurrentHashMap<>();
    }

    @Override
    public synchronized void cacheJwt(Jwt.Key cachedKey, Jwt jwt) {
        
        LongestExpireCacheKey leCachKey = new LongestExpireCacheKey(cachedKey);
        leCachKey.setExpiry(jwt.getExpire());
        if(cachedJwts.size() >= capacity) {
            if(expiryQueue.contains(leCachKey)) {
                expiryQueue.remove(leCachKey);
            } else {
                cachedJwts.remove(expiryQueue.peek().getCacheKey());
                expiryQueue.poll();
            }
        } else {
            if(expiryQueue.contains(leCachKey)) {
                expiryQueue.remove(leCachKey);
            }
        }
        expiryQueue.add(leCachKey);
        cachedJwts.put(cachedKey, jwt);
    }

    @Override
    public Jwt getCachedJwt(Jwt.Key key) {
        return cachedJwts.get(key);
    }

    private static class LongestExpireCacheKey {
        private long expiry;
        private Jwt.Key cacheKey;

        @Override
        public int hashCode() {
            return cacheKey.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return cacheKey.equals(obj);
        }

        LongestExpireCacheKey(Jwt.Key key) {
            this.cacheKey = key;
        }

        long getExpiry() {
            return expiry;
        }

        void setExpiry(long expiry) {
            this.expiry = expiry;
        }

        Jwt.Key getCacheKey() {
            return cacheKey;
        }
    }
}
