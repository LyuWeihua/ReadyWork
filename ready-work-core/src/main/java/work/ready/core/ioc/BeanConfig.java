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

package work.ready.core.ioc;

import work.ready.core.tools.define.Kv;

import java.util.*;

public class BeanConfig {
    private Class<?> clazz;
    private Class<?> assignableFrom;
    private String nameContains;
    private List<Map<Class<?>, Object>> constructorParams = new ArrayList<>();
    private Map<String, Object> injectProperties = new HashMap<>();
    private String initMethod;
    private String destroyMethod;

    public Class<?> getClazz() {
        return clazz;
    }

    public BeanConfig setClazz(Class<?> clazz) {
        this.clazz = clazz;
        return this;
    }

    public Class<?> getAssignableFrom() {
        return assignableFrom;
    }

    public BeanConfig setAssignableFrom(Class<?> assignableFrom) {
        this.assignableFrom = assignableFrom;
        return this;
    }

    public String getNameContains() {
        return nameContains;
    }

    public BeanConfig setNameContains(String nameContains) {
        this.nameContains = nameContains;
        return this;
    }

    public List<Map<Class<?>, Object>> getConstructorParams() {
        return constructorParams;
    }

    public BeanConfig addConstructorParam(Class<?> type, Object param){
        addConstructorParam(Kv.by(type, param));
        return this;
    }

    public BeanConfig addConstructorParam(Map<Class<?>, Object> param){
        constructorParams.add(param);
        return this;
    }

    public Map<String, Object> getInjectProperties() {
        return injectProperties;
    }

    public BeanConfig addInjectProperty(String property, Object value){
        injectProperties.put(property, value);
        return this;
    }

    public String getInitMethod() {
        return initMethod;
    }

    public BeanConfig setInitMethod(String initMethod) {
        this.initMethod = initMethod;
        return this;
    }

    public String getDestroyMethod() {
        return destroyMethod;
    }

    public BeanConfig setDestroyMethod(String destroyMethod) {
        this.destroyMethod = destroyMethod;
        return this;
    }
}
