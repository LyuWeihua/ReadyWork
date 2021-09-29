/**
 *
 * Original work Copyright core-ng
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
package work.ready.core.handler.websocket;

import java.util.HashMap;
import java.util.Map;

public class BeanMappers {
    public final Map<Class<?>, BeanMapper<?>> mappers = new HashMap();

    public <T> BeanMapper<T> register(Class<T> beanClass) {
        @SuppressWarnings("unchecked")
        BeanMapper<T> mapper = (BeanMapper<T>) mappers.get(beanClass);
        if (!mappers.containsKey(beanClass)) {
            mapper = new BeanMapper<>(beanClass);
            mappers.put(beanClass, mapper);
        }
        return mapper;
    }

    public <T> BeanMapper<T> mapper(Class<T> beanClass) {
        @SuppressWarnings("unchecked")
        BeanMapper<T> mapper = (BeanMapper<T>) mappers.get(beanClass);
        if (mapper == null) {
            if (beanClass.getPackageName().startsWith("java")) {   
                throw new Error("bean class must not be java built-in class, class=" + beanClass.getCanonicalName());
            }
            throw new Error("bean class is not registered, please use http().bean() to register, class=" + beanClass.getCanonicalName());
        }
        return mapper;
    }
}
