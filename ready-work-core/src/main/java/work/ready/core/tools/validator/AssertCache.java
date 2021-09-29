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

package work.ready.core.tools.validator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AssertCache {

    private static AssertCache ourInstance = new AssertCache();

    public static AssertCache getInstance() {
        return ourInstance;
    }

    private Map<Class, Set<Field>> classFieldSetMap;
    private Map<Field, Annotation[]> fieldAnnotationMap;

    private AssertCache() {
        classFieldSetMap = new HashMap<>();
        fieldAnnotationMap = new HashMap<>();
    }

    public void setClassFields(Class classType, Set<Field> fieldSet) {
        if (null == fieldSet) fieldSet = new HashSet<>();
        classFieldSetMap.put(classType, fieldSet);
    }

    public void setFieldAnnotations(Field field, Annotation[] annotations) {
        if (null == annotations) annotations = new Annotation[0];
        fieldAnnotationMap.put(field, annotations);
    }

    public Set<Field> getFieldsByClass(Class classType) {
        return classFieldSetMap.get(classType);
    }

    public Annotation[] getAnnotationsByField(Field field) {
        return fieldAnnotationMap.get(field);
    }

}
