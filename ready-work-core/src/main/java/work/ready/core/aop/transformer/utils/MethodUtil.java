/**
 *
 * Original work copyright dyagent
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
package work.ready.core.aop.transformer.utils;

import net.bytebuddy.description.method.MethodDescription.InDefinedShape;
import net.bytebuddy.description.type.TypeDescription;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static work.ready.core.aop.transformer.utils.ParameterUtil.parametersStr;
import static work.ready.core.aop.transformer.utils.ParameterUtil.parametersTypeStr;

public class MethodUtil {

    private static final char DOT = '.';
    private static final char SPACE = ' ';

    public static String signature(Class<?> clazz, Constructor constructor) {
        
        return constructor.getName() + DOT + parametersTypeStr(
                constructor.getParameterTypes());
    }

    public static String signature(Class<?> clazz, Method method) {
        return clazz.getCanonicalName() + DOT + method.getName() + DOT + parametersTypeStr(method.getParameterTypes());
    }

    public static String signature(TypeDescription typeDescription,
            InDefinedShape methodDescription) {
        if (methodDescription.isConstructor()) {
            
            return methodDescription.getName() + DOT + parametersTypeStr(methodDescription.getParameters());
        }
        return typeDescription.getCanonicalName() + DOT + methodDescription.getName() + DOT +
                parametersTypeStr(methodDescription.getParameters());
    }

    public static String methodStr(TypeDescription typeDescription,
            InDefinedShape methodDescription) {
        return methodDescription.getReturnType().getActualName() + SPACE + typeDescription.getCanonicalName() + DOT + methodDescription.getName() +
                parametersStr(methodDescription.getParameters());
    }

    public static String methodStr(Object instance, Method method, Class<?>[] parameterTypes) {
        return method.getReturnType().getCanonicalName() + SPACE + instance.getClass().getCanonicalName() + DOT + method.getName() +
                parametersStr(parameterTypes);
    }

}
