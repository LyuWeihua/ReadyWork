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

import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;

import java.util.Arrays;

public class ParameterUtil {

    private static final String EMPTY_PARAMETERS_TYPE_STR = "[]";
    private static final String EMPTY_PARENTHESES = "()";

    public static String parametersTypeStr(Class<?>[] parameterTypes) {
        if (parameterTypes == null) {
            return EMPTY_PARAMETERS_TYPE_STR;
        }
        return Arrays.toString(parameterTypes);
    }

    public static String parametersTypeStr(ParameterList<ParameterDescription.InDefinedShape> parameterTypes) {
        if (parameterTypes == null) {
            return EMPTY_PARAMETERS_TYPE_STR;
        }
        StringBuilder parametersTypeStr = new StringBuilder("[");
        for (ParameterDescription.InDefinedShape pdi : parameterTypes) {
            parametersTypeStr.append(pdi.getType().asErasure()).append(", ");
        }
        int length = parametersTypeStr.length();
        if (length > 1) {
            
            parametersTypeStr.delete(length - 2, length);
        }
        parametersTypeStr.append("]");
        return parametersTypeStr.toString();
    }

    public static String parametersStr(ParameterList<ParameterDescription.InDefinedShape> parameterTypes) {
        if (parameterTypes == null) {
            return EMPTY_PARENTHESES;
        }
        StringBuilder parametersTypeStr = new StringBuilder("(");
        for (ParameterDescription.InDefinedShape pdi : parameterTypes) {

            parametersTypeStr.append(pdi.getType().getTypeName()).append(", ");
        }
        int length = parametersTypeStr.length();
        if (length > 1) {
            
            parametersTypeStr.delete(length - 2, length);
        }
        parametersTypeStr.append(")");
        return parametersTypeStr.toString();
    }

    public static String parametersStr(Class<?>[] parameterTypes) {
        if (parameterTypes == null) {
            return EMPTY_PARENTHESES;
        }
        StringBuilder parametersTypeStr = new StringBuilder("(");
        for ( Class<?> argClass: parameterTypes) {
            parametersTypeStr.append(argClass.getName()).append(", ");
        }
        int length = parametersTypeStr.length();
        if (length > 1) {
            
            parametersTypeStr.delete(length - 2, length);
        }
        parametersTypeStr.append(")");
        return parametersTypeStr.toString();
    }
}
