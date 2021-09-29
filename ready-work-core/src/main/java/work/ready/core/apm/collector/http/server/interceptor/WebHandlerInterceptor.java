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

package work.ready.core.apm.collector.http.server.interceptor;

import io.undertow.server.HttpHandler;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import work.ready.core.aop.transformer.enhance.AbstractMethodInterceptor;
import work.ready.core.aop.transformer.match.ClassMatch;
import work.ready.core.aop.transformer.match.SubTypeMatch;
import work.ready.core.aop.transformer.utils.MethodUtil;
import work.ready.core.apm.ApmManager;
import work.ready.core.apm.collector.http.server.WebServerConfig;

import java.lang.reflect.Method;

public class WebHandlerInterceptor extends AbstractMethodInterceptor {

    private ElementMatcher.Junction<TypeDescription> typeJunction;
    private ElementMatcher.Junction<MethodDescription> methodJunction;
    private ElementMatcher.Junction<ParameterDescription> parameterJunction;
    private final WebHandlerHandler handler = new WebHandlerHandler();

    public void setParameterJunction(ElementMatcher.Junction<ParameterDescription> parameterJunction) {
        this.parameterJunction = parameterJunction;
    }

    @Override
    protected void before(Object instance, Method method, Object[] arguments, Class<?>[] parameterTypes) throws Throwable {
        handler.before(instance.getClass().getCanonicalName(), method.getName(), arguments, new Object[]{instance});
    }

    @Override
    protected Object after(Object instance, Method method, Object[] arguments, Class<?>[] parameterTypes, Object result) throws Throwable {
        return handler.after(instance.getClass().getCanonicalName(), method.getName(), arguments, result, null, new Object[]{instance});
    }

    @Override
    protected void handleError(Object instance, Method method, Object[] arguments, Class<?>[] parameterTypes, Throwable t) throws Throwable {
        handler.after(instance.getClass().getCanonicalName(), method.getName(), arguments, null, t, new Object[]{instance});
    }

    @Override
    public ClassMatch focusOn() {
        var matcher = new SubTypeMatch(HttpHandler.class);
        if(ApmManager.getConfig(WebServerConfig.class).getHandlerExclude() != null && !ApmManager.getConfig(WebServerConfig.class).getHandlerExclude().isEmpty()) {
            matcher.and(ElementMatchers.not(ElementMatchers.namedOneOf(ApmManager.getConfig(WebServerConfig.class).getHandlerExclude().toArray(new String[]{}))));
        }
        return matcher;
    }

    @Override
    public boolean matches(TypeDescription typeDescription, MethodDescription.InDefinedShape methodDescription, ParameterList<ParameterDescription.InDefinedShape> parameterDescriptions) {
        if(methodJunction == null) {
            methodJunction = ElementMatchers.isMethod()
                    .and(ElementMatchers.<MethodDescription>named("handleRequest"))
                    .and(ElementMatchers.takesArguments(1))
                    .and(ElementMatchers.takesArgument(0, ElementMatchers.named("io.undertow.server.HttpServerExchange")));
        }
        if(parameterJunction != null && parameterDescriptions != null && !parameterDescriptions.isEmpty()) {
            boolean matches = false;
            for(ParameterDescription p : parameterDescriptions) {
                if(parameterJunction.matches(p)) {
                    matches = true; break;
                }
            }
            return matches && methodJunction.matches(methodDescription);
        } else {
            return methodJunction.matches(methodDescription);
        }
    }

}
