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
import work.ready.core.database.DatabaseManager;
import work.ready.core.database.Record;
import work.ready.core.exception.ApiException;
import work.ready.core.server.Ready;
import work.ready.core.service.status.Status;
import work.ready.core.tools.ClassUtil;
import work.ready.core.tools.StrUtil;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.Map;

public class RecordDeserializer extends JsonDeserializer<Record> {

    public RecordDeserializer(){}

    @Override
    public Record deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        ObjectCodec oc = jp.getCodec();
        JsonNode node = oc.readTree(jp);
        Record record = null;
        try {
            record = Record.class.getConstructor(Map.class).newInstance(Ready.dbManager().getConfig().getContainerFactory().getColumnsMap());
            var Iterator = node.fieldNames();
            while (Iterator.hasNext()) {
                String next = Iterator.next();
                if(node.get(next).isBoolean()){
                    record.set(next, ClassUtil.typeCast(Boolean.class, node.get(next).asText()));
                }else if(node.get(next).isFloat()){
                    record.set(next, ClassUtil.typeCast(Float.class, node.get(next).asText()));
                }else if(node.get(next).isDouble()){
                    record.set(next, ClassUtil.typeCast(Double.class, node.get(next).asText()));
                }else if(node.get(next).isShort()){
                    record.set(next, ClassUtil.typeCast(Short.class, node.get(next).asText()));
                }else if(node.get(next).isInt()){
                    record.set(next, ClassUtil.typeCast(Integer.class, node.get(next).asText()));
                }else if(node.get(next).isLong()){
                    record.set(next, ClassUtil.typeCast(Long.class, node.get(next).asText()));
                }else if(node.get(next).isBigInteger()){
                    record.set(next, ClassUtil.typeCast(BigInteger.class, node.get(next).asText()));
                }else if(node.get(next).isBigDecimal()){
                    record.set(next, ClassUtil.typeCast(BigDecimal.class, node.get(next).asText()));
                }else if(node.get(next).isBinary()){
                    record.set(next, ClassUtil.typeCast(Byte[].class, node.get(next).asText()));
                }else if(node.get(next).isTextual()){
                    record.set(next, node.get(next).asText());
                }else if(node.get(next).isEmpty()){
                    record.set(next, "");
                }else if(node.get(next).isNull()){
                    record.set(next, null);
                }
            }
        } catch (ParseException pe){
            throw new ApiException(new Status("ERROR10014", pe.getMessage()));
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e){
            throw new RuntimeException(e);
        }
        return record;
    }
}
