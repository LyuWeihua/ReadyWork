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
import work.ready.core.component.cache.annotation.Cacheable;
import work.ready.core.config.ConfigInjector;
import work.ready.core.database.Model;
import work.ready.core.database.ModelCopier;
import work.ready.core.database.Page;
import work.ready.core.handler.Controller;
import work.ready.core.handler.request.HttpRequest;
import work.ready.core.component.cache.RenderInfo;
import work.ready.core.render.Render;
import work.ready.core.server.Ready;
import work.ready.core.tools.ClassUtil;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CacheInterceptor implements Interceptor {

    private static final String NULL_VALUE = "NULL_VALUE";
    private static final String renderKey = "_renderKey";
    private ConcurrentHashMap<String, ReentrantLock> lockMap = new ConcurrentHashMap<String, ReentrantLock>(512);

    private AopCache aopCache = Ready.cacheManager().getAopCache();

    @Override
    public void intercept(Invocation inv) throws Throwable {

        Method method = inv.getMethod();
        Cacheable cacheable = method.getAnnotation(Cacheable.class);
        if (cacheable == null) {
            inv.invoke();
            return;
        }

        String unlessString = ConfigInjector.getStringValue(cacheable.unless());
        if (Utils.isUnless(unlessString, method, inv.getArgs())) {
            inv.invoke();
            return;
        }

        Class targetClass = inv.getTarget().getClass();
        boolean isController = Controller.class.isAssignableFrom(targetClass);

        String cacheName = ConfigInjector.getStringValue(cacheable.name());
        Utils.ensureCachenameAvailable(method, targetClass, cacheName);
        String cacheKey = Utils.buildCacheKey(ConfigInjector.getStringValue(cacheable.key()), targetClass, method, inv.getArgs());

        Object data = aopCache.get(cacheName, cacheKey);
        if (data != null) {
            if(isController){
                useCacheDataAndRender((Map<String, Object>)data, inv.getController());
            }else {
                if (NULL_VALUE.equals(data)) {
                    inv.setReturnValue(null);
                } else if (cacheable.returnCopyEnable()) {
                    inv.setReturnValue(getCopyObject(inv, data));
                } else {
                    inv.setReturnValue(data);
                }
            }
        } else {
            Lock lock = getLock(cacheName);
            lock.lock();					
            try {
                inv.invoke();
                if(isController){
                    cacheAction(cacheName, cacheKey, cacheable.liveSeconds(), inv.getController());
                } else {
                    data = inv.getReturnValue();
                    if (data != null) {

                        Utils.putDataToCache(aopCache, cacheable.liveSeconds(), cacheName, cacheKey, data);

                        if (cacheable.returnCopyEnable()) {
                            inv.setReturnValue(getCopyObject(inv, data));
                        }

                    } else if (cacheable.nullCacheEnable()) {
                        Utils.putDataToCache(aopCache, cacheable.liveSeconds(), cacheName, cacheKey, NULL_VALUE);
                    }
                }
            }
            finally {
                lock.unlock();
            }
        }
    }

    private ReentrantLock getLock(String key) {
        ReentrantLock lock = lockMap.get(key);
        if (lock != null) {
            return lock;
        }

        lock = new ReentrantLock();
        ReentrantLock previousLock = lockMap.putIfAbsent(key, lock);
        return previousLock == null ? lock : previousLock;
    }

    private <M extends Model> Object getCopyObject(Invocation inv, Object data) {
        if (data instanceof List) {
            return ModelCopier.copy((List<? extends Model>) data);
        } else if (data instanceof Set) {
            return ModelCopier.copy((Set<? extends Model>) data);
        } else if (data instanceof Page) {
            return ModelCopier.copy((Page<? extends Model>) data);
        } else if (data instanceof Model) {
            return ModelCopier.copy((Model) data);
        } else if (data.getClass().isArray()
                && Model.class.isAssignableFrom(data.getClass().getComponentType())) {
            return ModelCopier.copy((M[]) data);
        } else {
            throw newException(null, inv, data);
        }
    }

    private RuntimeException newException(Exception ex, Invocation inv, Object data) {
        String msg = "can not copy data for type [" + data.getClass().getName() + "] on method :"
                + ClassUtil.getMethodSignature(inv.getMethod())
                + " , due to @Cacheable(returnCopyEnable=true) annotation";

        return ex == null ? new RuntimeException(msg) : new RuntimeException(msg, ex);
    }

    protected void cacheAction(String cacheName, String cacheKey, int liveSeconds, Controller controller) {
        HttpRequest request = controller.getRequest();
        Map<String, Object> cacheData = new HashMap<String, Object>();
        for (Enumeration<String> names = request.getAttributeNames(); names.hasMoreElements();) {
            String name = names.nextElement();
            cacheData.put(name, request.getAttribute(name));
        }

        Render render = controller.getRender();
        if (render != null) {
            cacheData.put(renderKey, new RenderInfo(render));		
        }
        Utils.putDataToCache(aopCache, liveSeconds, cacheName, cacheKey, cacheData);
    }

    protected void useCacheDataAndRender(Map<String, Object> cacheData, Controller controller) {
        HttpRequest request = controller.getRequest();
        Set<Map.Entry<String, Object>> set = cacheData.entrySet();
        for (Iterator<Map.Entry<String, Object>> it = set.iterator(); it.hasNext();) {
            Map.Entry<String, Object> entry = it.next();
            request.setAttribute(entry.getKey(), entry.getValue());
        }
        request.removeAttribute(renderKey);

        RenderInfo renderInfo = (RenderInfo)cacheData.get(renderKey);
        if (renderInfo != null) {
            controller.render(renderInfo.createRender(controller.getRenderManager()));		
        }
    }
}
