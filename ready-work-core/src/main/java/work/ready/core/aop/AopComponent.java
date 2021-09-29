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

package work.ready.core.aop;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

public class AopComponent {
    private Class<? extends Annotation> annotation;
    private Class<? extends Interceptor> interceptorClass;
    private Map<String, Object> injectProperties = new HashMap<>();

    public AopComponent(){

    }

    public AopComponent(Class<? extends Annotation> annotation, Class<? extends Interceptor> interceptorClass, Map<String, Object> injectProperties){
        this.annotation = annotation;
        this.interceptorClass = interceptorClass;
        this.injectProperties = injectProperties;
    }

    public Class<? extends Annotation> getAnnotation() {
        return annotation;
    }

    public AopComponent setAnnotation(Class<? extends Annotation> annotation) {
        this.annotation = annotation;
        return this;
    }

    public Class<? extends Interceptor> getInterceptorClass() {
        return interceptorClass;
    }

    public AopComponent setInterceptorClass(Class<? extends Interceptor> interceptorClass) {
        this.interceptorClass = interceptorClass;
        return this;
    }

    public AopComponent addInjectProperty(String field, Object value){
        injectProperties.put(field, value);
        return this;
    }

    public Map<String, Object> getInjectProperties() {
        return injectProperties;
    }

    public AopComponent setInjectProperties(Map<String, Object> injectProperties) {
        this.injectProperties = injectProperties;
        return this;
    }
}
