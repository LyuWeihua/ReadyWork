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
package work.ready.core.component.cache.interceptor;

import work.ready.core.aop.Interceptor;
import work.ready.core.aop.Invocation;
import work.ready.core.component.cache.AopCache;
import work.ready.core.component.cache.annotation.CacheEvict;
import work.ready.core.component.cache.annotation.CachesEvict;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class CachesEvictInterceptor implements Interceptor {
    private static final Log logger = LogFactory.getLog(CachesEvictInterceptor.class);

    private AopCache aopCache = Ready.cacheManager().getAopCache();

    @Override
    public void intercept(Invocation inv) throws Throwable {

        Method method = inv.getMethod();

        CachesEvict cachesEvict = method.getAnnotation(CachesEvict.class);
        if (cachesEvict == null) {
            inv.invoke();
            return;
        }

        CacheEvict[] evicts = cachesEvict.value();
        List<CacheEvict> beforeInvocations = null;
        List<CacheEvict> afterInvocations = null;

        for (CacheEvict evict : evicts) {
            if (evict.beforeInvocation()) {
                if (beforeInvocations == null) {
                    beforeInvocations = new ArrayList<>();
                }
                beforeInvocations.add(evict);
            } else {
                if (afterInvocations == null) {
                    afterInvocations = new ArrayList<>();
                }
                afterInvocations.add(evict);
            }
        }

        Class targetClass = inv.getTarget().getClass();
        try {
            doCachesEvict(inv.getArgs(), targetClass, method, beforeInvocations);
            inv.invoke();
        } finally {
            doCachesEvict(inv.getArgs(), targetClass, method, afterInvocations);
        }
    }

    private void doCachesEvict(Object[] arguments
            , Class targetClass
            , Method method
            , List<CacheEvict> cacheEvicts) {

        if (cacheEvicts == null || cacheEvicts.isEmpty()) {
            return;
        }

        for (CacheEvict evict : cacheEvicts) {
            try {
                Utils.doCacheEvict(aopCache, arguments, targetClass, method, evict);
            } catch (Exception ex) {
                logger.error(ex, "CacheEvict exception");
            }
        }
    }
}
