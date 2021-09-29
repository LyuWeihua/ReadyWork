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

package work.ready.core.aop.transformer.enhance;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import work.ready.core.aop.transformer.match.ClassMatch;
import work.ready.core.aop.transformer.match.FreedomMatch;
import work.ready.core.aop.transformer.utils.MatchUtil;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Function;

public class EasyInterceptorHandler extends AbstractMethodInterceptor {

    private final EasyInterceptor interceptor;
    private final ElementMatcher.Junction<TypeDescription> typeJunction;
    private final ElementMatcher.Junction<MethodDescription> methodJunction;
    private ElementMatcher.Junction<ParameterDescription> parameterJunction;

    public EasyInterceptorHandler(EasyInterceptor interceptor, Map<String, Object> typeInclude, Map<String, Object> typeExclude, Map<String, Object> methodInclude, Map<String, Object> methodExclude) {
        this.interceptor = interceptor;
        this.typeJunction = MatchUtil.buildTypesMatcher(typeInclude, typeExclude);
        this.methodJunction = MatchUtil.buildMethodsMatcher(methodInclude, methodExclude);
        if(interceptor.typeJunction() != null) {
            interceptor.typeJunction().accept(typeJunction);
        }
        if(interceptor.methodJunction() != null) {
            interceptor.methodJunction().accept(methodJunction);
        }
    }

    @Override
    public FieldDefine[] fieldDefine() {
        return interceptor.fieldDefine();
    }

    @Override
    public int order(){
        return interceptor.order();
    }

    public void setParameterJunction(ElementMatcher.Junction<ParameterDescription> parameterJunction) {
        this.parameterJunction = parameterJunction;
    }

    @Override
    protected Object around(Object instance, Method method, Object[] arguments, Class<?>[] parameterTypes, ProceedingJoinPoint point) throws Throwable
    {
        return interceptor.around(instance, method, arguments, parameterTypes, point);
    }

    @Override
    protected void before(Object instance, Method method, Object[] arguments, Class<?>[] parameterTypes) throws Throwable {
        interceptor.before(instance, method, arguments, parameterTypes);
    }

    @Override
    protected Object after(Object instance, Method method, Object[] arguments, Class<?>[] parameterTypes, Object result) throws Throwable {
        return interceptor.after(instance, method, arguments, parameterTypes, result);
    }

    @Override
    protected void handleError(Object instance, Method method, Object[] arguments, Class<?>[] parameterTypes, Throwable t) throws Throwable {
        interceptor.handleError(instance, method, arguments, parameterTypes, t);
    }

    @Override
    public ClassMatch focusOn() {
        return new FreedomMatch(typeJunction);
    }

    @Override
    public boolean matches(TypeDescription typeDescription, MethodDescription.InDefinedShape methodDescription, ParameterList<ParameterDescription.InDefinedShape> parameterDescriptions) {
        if(parameterDescriptions != null && !parameterDescriptions.isEmpty()) {
            boolean matchesMethod = methodJunction.matches(methodDescription);
            boolean matches = matchesMethod && interceptor.parameterMatcher(parameterDescriptions);
            if(matchesMethod && !matches && parameterJunction != null) {
                for (ParameterDescription p : parameterDescriptions) {
                    if (parameterJunction.matches(p)) {
                        matches = true;
                        break;
                    }
                }
            }
            return matches;
        } else {
            return methodJunction.matches(methodDescription);
        }
    }
}
