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

import java.lang.reflect.Method;
import java.util.function.Consumer;

public interface EasyInterceptor {

    default FieldDefine[] fieldDefine(){
        return null;
    }

    default int order(){
        return Integer.MAX_VALUE;
    }

    default Consumer<ElementMatcher.Junction<TypeDescription>> typeJunction() {
        return null;
    }

    default Consumer<ElementMatcher.Junction<MethodDescription>> methodJunction() {
        return null;
    }

    default boolean parameterMatcher(ParameterList<ParameterDescription.InDefinedShape> parameterList) {
        return true;
    }

    default Object around(Object instance, Method method, Object[] arguments, Class<?>[] parameterTypes, ProceedingJoinPoint point) throws Throwable
    {
        return point.proceed();
    }

    void before(Object instance, Method method, Object[] arguments, Class<?>[] parameterTypes) throws Throwable;

    Object after(Object instance, Method method, Object[] arguments, Class<?>[] parameterTypes, Object result) throws Throwable;

    void handleError(Object instance, Method method, Object[] arguments, Class<?>[] parameterTypes, Throwable t) throws Throwable;

    class IgnoreException extends RuntimeException {

    }
}
