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
package work.ready.core.component.redis;

import work.ready.core.component.cache.redis.RedisCache;
import work.ready.core.server.Ready;

import static work.ready.core.component.cache.CacheConfig.TYPE_REDIS;

public class RedisLock {

    long expireMsecs = 1000 * 60;
    long timeoutMsecs = 0;

    private final String lockName;
    private boolean locked = false;
    private final Redis redis;

    public RedisLock(String lockName) {
        if (lockName == null) {
            throw new NullPointerException("lockName must not null !");
        }
        this.lockName = lockName;
        this.redis = ((RedisCache)Ready.cacheManager().getCache(TYPE_REDIS)).getRedis();
    }

    public RedisLock(String lockName, long timeoutMsecs) {
        if (lockName == null) {
            throw new NullPointerException("lockName must not null !");
        }
        this.lockName = lockName;
        this.timeoutMsecs = timeoutMsecs;
        this.redis = ((RedisCache)Ready.cacheManager().getCache(TYPE_REDIS)).getRedis();
    }

    public long getTimeoutMsecs() {
        return timeoutMsecs;
    }

    public void setTimeoutMsecs(long timeoutMsecs) {
        this.timeoutMsecs = timeoutMsecs;
    }

    public long getExpireMsecs() {
        return expireMsecs;
    }

    public void setExpireMsecs(long expireMsecs) {
        this.expireMsecs = expireMsecs;
    }

    public void runIfAcquired(Runnable runnable) {
        if (runnable == null) {
            throw new NullPointerException("runnable must not null!");
        }
        try {
            if (acquire()) {
                runnable.run();
            }
        } finally {
            
            release();
        }
    }

    public boolean acquire() {
        long timeout = timeoutMsecs;

        do {
            long expires = Ready.currentTimeMillis() + expireMsecs + 1;

            Long result = redis.setnx(lockName, expires);
            if (result != null && result == 1) {
                
                locked = true;
                return true;
            }

            Long savedValue = redis.get(lockName);
            if (savedValue != null && savedValue < Ready.currentTimeMillis()) {

                Long oldValue = redis.getSet(lockName, expires);

                if (oldValue != null && oldValue.equals(savedValue)) {

                    locked = true;
                    return true;
                }
            }

            if (timeout > 0) {
                timeout -= 100;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        } while (timeout > 0);

        return false;
    }

    public boolean isLocked() {
        return locked;
    }

    public void release() {
        if (!isLocked()) {
            return;
        }
        if (redis.del(lockName) > 0) {
            locked = false;
        }
    }
}
