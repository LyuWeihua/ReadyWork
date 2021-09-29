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
import work.ready.core.component.cache.annotation.CachePut;
import work.ready.core.config.ConfigInjector;
import work.ready.core.server.Ready;

import java.lang.reflect.Method;

public class CachePutInterceptor implements Interceptor {

    private AopCache aopCache = Ready.cacheManager().getAopCache();

    @Override
    public void intercept(Invocation inv) throws Throwable {

        inv.invoke();

        Method method = inv.getMethod();
        CachePut cachePut = method.getAnnotation(CachePut.class);
        if (cachePut == null) {
            return;
        }

        Object data = inv.getReturnValue();

        String unless = ConfigInjector.getStringValue(cachePut.unless());
        if (Utils.isUnless(unless, method, inv.getArgs())) {
            return;
        }

        Class targetClass = inv.getTarget().getClass();
        String cacheName = ConfigInjector.getStringValue(cachePut.name());
        Utils.ensureCachenameAvailable(method, targetClass, cacheName);
        String cacheKey = Utils.buildCacheKey(ConfigInjector.getStringValue(cachePut.key()), targetClass, method, inv.getArgs());

        Utils.putDataToCache(aopCache, cachePut.liveSeconds(),cacheName,cacheKey,data);
    }

}
