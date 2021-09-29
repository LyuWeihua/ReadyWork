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

package work.ready.core.security.data;

import work.ready.core.database.DatabaseManager;

import static work.ready.core.aop.transformer.TransformerManager.METHOD_PREFIX;

public class CallerInspector extends Throwable {

    private static final String ignoreClassNamePrefix = DatabaseManager.class.getPackageName() + '.';
    private String className = null;
    private String methodName = null;

    public String getCallerClassName(){
        if(className == null) {
            var stack = getStackTrace();
            String originalClass = null;
            String originalMethod = null;
            for (StackTraceElement stackTraceElement : stack) {
                className = stackTraceElement.getClassName();
                if (className.startsWith(ignoreClassNamePrefix)) {
                    continue;
                }
                methodName = stackTraceElement.getMethodName();
                if(methodName.startsWith(METHOD_PREFIX)) {
                    originalClass = className;
                    originalMethod = methodName.substring(METHOD_PREFIX.length());
                }
                if(originalMethod != null) {
                    if(!className.equals(originalClass) || !methodName.equals(originalMethod)) {
                        continue;
                    }
                }
                int tag = className.indexOf("$$EnhancerBy");
                if (tag > 0) {
                    className = className.substring(0, tag);
                }
                break;
            }
        }
        return className;
    }

    public String getCallerMethodName(){
        return methodName;
    }

}
