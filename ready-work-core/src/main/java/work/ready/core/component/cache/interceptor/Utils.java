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

import work.ready.core.component.cache.AopCache;
import work.ready.core.component.cache.annotation.CacheEvict;
import work.ready.core.config.ConfigInjector;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Constant;
import work.ready.core.template.Engine;
import work.ready.core.tools.ClassUtil;
import work.ready.core.tools.StrUtil;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

class Utils {

    private static final Log logger = LogFactory.getLog(Utils.class);
    static final Engine ENGINE = new Engine("CacheRender");

    static String engineRender(String template, Method method, Object[] arguments) {
        Map<String, Object> datas = new HashMap();
        int x = 0;
        for (Parameter p : method.getParameters()) {
            if (!p.isNamePresent()) {
                
                throw new RuntimeException(" Maven or IDE config is error. see http://www.jfinal.com/doc/3-3 ");
            }
            datas.put(p.getName(), arguments[x++]);
        }

        return ENGINE.getTemplateByString(template).renderToString(datas);
    }

    static String buildCacheKey(String key, Class clazz, Method method, Object[] arguments) {

        clazz = ClassUtil.getUserClass(clazz);

        if (StrUtil.notBlank(key)) {
            return renderKey(key, method, arguments);
        }

        StringBuilder keyBuilder = new StringBuilder(clazz.getName());
        keyBuilder.append("#").append(method.getName());

        if (arguments == null || arguments.length == 0) {
            return keyBuilder.toString();
        }

        Class[] paramTypes = method.getParameterTypes();
        int index = 0;
        for (Object argument : arguments) {
            String argStr = ClassUtil.convertToString(argument);
            ensureArgumentNotNull(argStr, clazz, method);
            keyBuilder
                    .append(paramTypes[index++].getClass().getName())
                    .append(":")
                    .append(argStr)
                    .append("-");
        }

        return keyBuilder.deleteCharAt(keyBuilder.length() - 1).toString();

    }

    private static String renderKey(String key, Method method, Object[] arguments) {
        if (!key.contains("#(") || !key.contains(")")) {
            return key;
        }

        return engineRender(key, method, arguments);
    }

    public static void ensureArgumentNotNull(String argument, Class clazz, Method method) {
        if (argument == null) {
            throw new RuntimeException("not support empty key for annotation @Cacheable, @CacheEvict or @CachePut " +
                    "at method[" + ClassUtil.getMethodSignature(method) + "], " +
                    "please config key properties in @Cacheable, @CacheEvict or @CachePut annotation.");
        }
    }

    public static void ensureCachenameAvailable(Method method, Class targetClass, String cacheName) {
        if (StrUtil.isBlank(cacheName)) {
            throw new RuntimeException(String.format("name of annotation is empty on method [%s]",
                    ClassUtil.getMethodSignature(method)));
        }
    }

    static boolean isUnless(String unlessString, Method method, Object[] arguments) {

        if (StrUtil.isBlank(unlessString)) {
            return false;
        }

        String template = new StringBuilder("#(")
                .append(unlessString)
                .append(")")
                .toString();

        return "true".equals(engineRender(template,method, arguments));
    }

    static void doCacheEvict(AopCache aopCache, Object[] arguments, Class targetClass, Method method, CacheEvict evict) {
        String unless = ConfigInjector.getStringValue(evict.unless());
        if (Utils.isUnless(unless, method, arguments)) {
            return;
        }

        String cacheName = ConfigInjector.getStringValue(evict.name());
        if (StrUtil.isBlank(cacheName)) {
            throw new RuntimeException(String.format("CacheEvict.name()  must not empty in method [%s].",
                    ClassUtil.getMethodSignature(method)));
        }

        String cacheKey = ConfigInjector.getStringValue(evict.key());

        if (StrUtil.isBlank(cacheKey) || "*".equals(cacheKey)) {
            aopCache.removeAll(cacheName);
        } else {
            cacheKey = Utils.buildCacheKey(cacheKey, targetClass, method, arguments);
            aopCache.remove(cacheName, cacheKey);
        }
    }

    static void putDataToCache(AopCache aopCache, int liveSeconds, String cacheName, String cacheKey, Object data) {
         liveSeconds = liveSeconds > 0
                ? liveSeconds
                : Constant.DEFAULT_AOP_CACHE_TIME;
        if (liveSeconds > 0) {
            aopCache.put(cacheName, cacheKey, data, liveSeconds);
        } else {
            aopCache.put(cacheName, cacheKey, data);
        }
    }

}
