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
package work.ready.core.security.cors;

import work.ready.core.aop.Interceptor;
import work.ready.core.aop.Invocation;
import work.ready.core.config.ConfigInjector;
import work.ready.core.handler.RequestMethod;
import work.ready.core.handler.response.HttpResponse;
import work.ready.core.tools.StrUtil;

public class CorsInterceptor implements Interceptor {

    @Override
    public void intercept(Invocation inv) throws Throwable {

        EnableCORS enableCORS = inv.getMethod().getAnnotation(EnableCORS.class);

        if (enableCORS == null) {
            enableCORS = inv.getController().getClass().getAnnotation(EnableCORS.class);
        }

        if (enableCORS == null) {
            inv.invoke();
            return;
        }

        doConfigCORS(inv, enableCORS);

        RequestMethod method = inv.getController().getRequest().getMethod();
        if (RequestMethod.OPTIONS.equals(method)) {
            inv.getController().renderText("");
        } else {
            inv.invoke();
        }
    }

    private void doConfigCORS(Invocation inv, EnableCORS enableCORS) {

        HttpResponse response = inv.getController().getResponse();

        String allowOrigin = ConfigInjector.getStringValue(enableCORS.allowOrigin());
        String allowCredentials = ConfigInjector.getStringValue(enableCORS.allowCredentials());
        String allowHeaders = ConfigInjector.getStringValue(enableCORS.allowHeaders());
        String allowMethods = ConfigInjector.getStringValue(enableCORS.allowMethods());
        String exposeHeaders = ConfigInjector.getStringValue(enableCORS.exposeHeaders());
        String requestHeaders = ConfigInjector.getStringValue(enableCORS.requestHeaders());
        String requestMethod = ConfigInjector.getStringValue(enableCORS.requestMethod());
        String origin = ConfigInjector.getStringValue(enableCORS.origin());
        String maxAge = ConfigInjector.getStringValue(enableCORS.maxAge());

        response.setHeader("Access-Control-Allow-Origin", allowOrigin);
        response.setHeader("Access-Control-Allow-Methods", allowMethods);
        response.setHeader("Access-Control-Allow-Headers", allowHeaders);
        response.setHeader("Access-Control-Max-Age", maxAge);
        response.setHeader("Access-Control-Allow-Credentials", allowCredentials);

        if (StrUtil.notBlank(exposeHeaders)) {
            response.setHeader("Access-Control-Expose-Headers", exposeHeaders);
        }

        if (StrUtil.notBlank(requestHeaders)) {
            response.setHeader("Access-Control-Request-Headers", requestHeaders);
        }

        if (StrUtil.notBlank(requestMethod)) {
            response.setHeader("Access-Control-Request-Method", requestMethod);
        }

        if (StrUtil.notBlank(origin)) {
            response.setHeader("Origin", origin);
        }

    }
}
