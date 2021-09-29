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

package work.ready.core.aop.transformer.match;

import java.util.HashMap;
import java.util.Map;

public class InterceptorPoint {

    private boolean enabled = true;
    private String interceptor;
    private Map<String, Object> typeInclude = new HashMap<>();
    private Map<String, Object> typeExclude = new HashMap<>();
    private Map<String, Object> methodInclude = new HashMap<>();
    private Map<String, Object> methodExclude = new HashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public InterceptorPoint setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public String getInterceptor() {
        return interceptor;
    }

    public InterceptorPoint setInterceptor(String interceptor) {
        this.interceptor = interceptor;
        return this;
    }

    public Map<String, Object> getTypeInclude() {
        return typeInclude;
    }

    public InterceptorPoint setTypeInclude(Map<String, Object> typeInclude) {
        this.typeInclude = typeInclude;
        return this;
    }

    public InterceptorPoint setTypeInclude(String type, String include) {
        this.typeInclude.put(type, include);
        return this;
    }

    public Map<String, Object> getTypeExclude() {
        return typeExclude;
    }

    public InterceptorPoint setTypeExclude(Map<String, Object> typeExclude) {
        this.typeExclude = typeExclude;
        return this;
    }

    public InterceptorPoint setTypeExclude(String type, String exclude) {
        this.typeExclude.put(type, exclude);
        return this;
    }

    public Map<String, Object> getMethodInclude() {
        return methodInclude;
    }

    public InterceptorPoint setMethodInclude(Map<String, Object> methodInclude) {
        this.methodInclude = methodInclude;
        return this;
    }

    public InterceptorPoint setMethodInclude(String type, String method) {
        this.methodInclude.put(type, method);
        return this;
    }

    public Map<String, Object> getMethodExclude() {
        return methodExclude;
    }

    public InterceptorPoint setMethodExclude(Map<String, Object> methodExclude) {
        this.methodExclude = methodExclude;
        return this;
    }

    public InterceptorPoint setMethodExclude(String type, String method) {
        this.methodExclude.put(type, method);
        return this;
    }
}
