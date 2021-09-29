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

package work.ready.core.component.proxy;

import java.lang.annotation.Annotation;

public class JavaCoder {
    private Class<? extends Annotation> annotation;
    private Class<?> assignableFrom;
    private String nameContains;
    private CodeGenerator generator;
    private int order = 0;

    public Class<? extends Annotation> getAnnotation() {
        return annotation;
    }

    public JavaCoder setAnnotation(Class<? extends Annotation> annotation) {
        this.annotation = annotation;
        return this;
    }

    public Class<?> getAssignableFrom() {
        return assignableFrom;
    }

    public JavaCoder setAssignableFrom(Class<?> assignableFrom) {
        this.assignableFrom = assignableFrom;
        return this;
    }

    public String getNameContains() {
        return nameContains;
    }

    public JavaCoder setNameContains(String nameContains) {
        this.nameContains = nameContains;
        return this;
    }

    public CodeGenerator getGenerator() {
        return generator;
    }

    public JavaCoder setGenerator(CodeGenerator generator) {
        this.generator = generator;
        return this;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
