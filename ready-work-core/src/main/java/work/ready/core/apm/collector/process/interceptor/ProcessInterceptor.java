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

package work.ready.core.apm.collector.process.interceptor;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import work.ready.core.aop.transformer.enhance.AbstractMethodInterceptor;
import work.ready.core.aop.transformer.enhance.ConstructorInterceptor;
import work.ready.core.aop.transformer.match.ClassMatch;
import work.ready.core.aop.transformer.match.FreedomMatch;
import work.ready.core.aop.transformer.utils.MatchUtil;
import work.ready.core.aop.transformer.utils.MethodUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

public class ProcessInterceptor extends AbstractMethodInterceptor implements ConstructorInterceptor {

    private final ElementMatcher.Junction<TypeDescription> typeJunction;
    private final ElementMatcher.Junction<MethodDescription> methodJunction;
    private ElementMatcher.Junction<ParameterDescription> parameterJunction;
    private final ProcessHandler handler = new ProcessHandler();

    public ProcessInterceptor(Map<String, Object> typeInclude, Map<String, Object> typeExclude, Map<String, Object> methodInclude, Map<String, Object> methodExclude) {
        typeJunction = MatchUtil.buildTypesMatcher(typeInclude, typeExclude);
        methodJunction = MatchUtil.buildMethodsMatcher(methodInclude, methodExclude);
    }

    public void setParameterJunction(ElementMatcher.Junction<ParameterDescription> parameterJunction) {
        this.parameterJunction = parameterJunction;
    }

    @Override
    public void onConstruct(Object instance, Constructor constructor, Object[] arguments) {
        handler.before(instance.getClass().getCanonicalName(), constructor.getName(), arguments, null);
        handler.after(instance.getClass().getCanonicalName(), constructor.getName(), arguments, null, null, new Object[]{MethodUtil.signature(instance.getClass(), constructor)});
    }

    @Override
    protected void before(Object instance, Method method, Object[] arguments, Class<?>[] parameterTypes) throws Throwable {
        handler.before(instance.getClass().getCanonicalName(), method.getName(), arguments, null);
    }

    @Override
    protected Object after(Object instance, Method method, Object[] arguments, Class<?>[] parameterTypes, Object result) throws Throwable {
        return handler.after(instance.getClass().getCanonicalName(), method.getName(), arguments, result, null, new Object[]{MethodUtil.signature(instance.getClass(), method)});
    }

    @Override
    protected void handleError(Object instance, Method method, Object[] arguments, Class<?>[] parameterTypes, Throwable t) throws Throwable {
        handler.after(instance.getClass().getCanonicalName(), method.getName(), arguments, null, t, new Object[]{MethodUtil.signature(instance.getClass(), method)});
    }

    @Override
    public ClassMatch focusOn() {
        return new FreedomMatch(typeJunction);
    }

    @Override
    public boolean matches(TypeDescription typeDescription, MethodDescription.InDefinedShape methodDescription, ParameterList<ParameterDescription.InDefinedShape> parameterDescriptions) {
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
