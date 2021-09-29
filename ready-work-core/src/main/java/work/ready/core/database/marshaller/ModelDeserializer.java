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

package work.ready.core.database.marshaller;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import work.ready.core.database.Model;
import work.ready.core.database.Table;
import work.ready.core.exception.ApiException;
import work.ready.core.security.data.CallerInspector;
import work.ready.core.server.Ready;
import work.ready.core.service.status.Status;
import work.ready.core.tools.ClassUtil;
import work.ready.core.tools.define.ConcurrentReferenceHashMap;
import work.ready.core.tools.StrUtil;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ModelDeserializer<T extends Model> extends JsonDeserializer<T> {

    private Class<T> modelClass;
    private static final Map<Class<?>, Set<Method>> methodsCache = new ConcurrentReferenceHashMap<>(256);

    public ModelDeserializer(Class<T> modelClass){
        this.modelClass = modelClass;
    }

    @Override
    public T deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        ObjectCodec oc = jp.getCodec();
        JsonNode node = oc.readTree(jp);
        T model = null;
        try {
            model = modelClass.getConstructor().newInstance();
            Set<Method> potentialSetters = methodsCache.get(modelClass);
            if(potentialSetters == null) {
                synchronized (ModelDeserializer.class) {
                    potentialSetters = methodsCache.get(modelClass);
                    if(potentialSetters == null) {
                        Method[] ignoreMethods = Model.class.getMethods();
                        Method[] potentialMethods = modelClass.getMethods();
                        potentialSetters = new HashSet<>(Arrays.asList(potentialMethods));
                        potentialSetters.removeAll(Arrays.asList(ignoreMethods));
                        methodsCache.put(modelClass, potentialSetters);
                    }
                }
            }
            String modelPrefix = StrUtil.firstCharToLowerCase(modelClass.getSimpleName()) + ".";
            var nodeIterator = node.fieldNames();
            while (nodeIterator.hasNext()) {
                String next = nodeIterator.next();
                String fieldName = (next.startsWith(modelPrefix)) ? next.substring(modelPrefix.length()) : next;
                fieldName = StrUtil.toCamelCase(fieldName);
                var setterIterator = potentialSetters.iterator();
                while (setterIterator.hasNext()) {
                    Method method = setterIterator.next();
                    String methodName = method.getName();
                    int indexOfSet = methodName.indexOf("set");
                    if (indexOfSet == 0 && methodName.length() > 3) {    
                        if (StrUtil.firstCharToLowerCase(methodName.substring(3)).equals(fieldName)) {
                            if (method.getParameterCount() == 1) {
                                Class<?>[] types = method.getParameterTypes();
                                if(node.get(next).isNull())
                                    method.invoke(model, new Object[]{null});
                                else
                                    method.invoke(model, ClassUtil.typeCast(types[0], node.get(next).asText()));
                                break;
                            }
                        }
                    }
                }
            }
        } catch (ParseException pe){
            throw new ApiException(new Status("ERROR10014", pe.getMessage()));
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e){
            throw new RuntimeException(e);
        }
        if(Ready.dbManager().getDataSecurityInspector() != null){
            CallerInspector callerInspector = new CallerInspector();
            Table table = model._getTable(false);
            Map data = Ready.dbManager().getDataSecurityInspector().inputExamine(callerInspector.getCallerClassName(), callerInspector.getCallerMethodName(),
                    table != null ? table.getDatasource() : null, table != null ? table.getName() : null, model.getAttrs());
            return (T)model._setAttrs(data);
        }
        return model;
    }
}
