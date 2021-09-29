/**
 *
 * Original work Copyright 2017-2019 CodingApi
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
package work.ready.cloud.transaction.common.serializer;

import work.ready.cloud.transaction.core.transaction.txc.analyse.bean.FieldCluster;
import work.ready.cloud.transaction.core.transaction.txc.analyse.bean.FieldValue;
import work.ready.cloud.transaction.core.transaction.txc.analyse.undo.TableRecord;
import work.ready.cloud.transaction.core.transaction.txc.analyse.undo.TableRecordList;
import work.ready.core.component.serializer.KryoSerializer;
import work.ready.core.component.serializer.Serializer;

import java.util.ArrayList;

public class SerializerContext implements Serializer {

    private final KryoSerializer kryoSerializer;

    private SerializerContext(){
        
        kryoSerializer = (KryoSerializer)KryoSerializer.instance;
        kryoSerializer.register(TableRecordList.class);
        kryoSerializer.register(TableRecord.class);
        kryoSerializer.register(FieldCluster.class);
        kryoSerializer.register(FieldValue.class);
        kryoSerializer.register(ArrayList.class);
        kryoSerializer.register(java.lang.Boolean.class);
        kryoSerializer.register(java.lang.Long.class);
        kryoSerializer.register(java.math.BigInteger.class);
        kryoSerializer.register(java.math.BigDecimal.class);
        kryoSerializer.register(java.lang.Double.class);
        kryoSerializer.register(java.lang.Integer.class);
        kryoSerializer.register(java.lang.Float.class);
        kryoSerializer.register(java.lang.Short.class);
        kryoSerializer.register(java.lang.Byte.class);
        kryoSerializer.register(byte[].class);
        kryoSerializer.register(java.lang.Character.class);
        kryoSerializer.register(java.lang.String.class);
        kryoSerializer.register(java.util.UUID.class);
        kryoSerializer.register(java.sql.Time.class);
        kryoSerializer.register(java.sql.Timestamp.class);
        kryoSerializer.register(java.sql.Date.class);
        kryoSerializer.register(java.util.Date.class);
        kryoSerializer.register(java.sql.Clob.class);
        kryoSerializer.register(java.sql.Blob.class);
        kryoSerializer.register(Class.class);
    }

    private static SerializerContext context = null;

    public static SerializerContext getInstance() {
        if (context == null) {
            synchronized (SerializerContext.class) {
                if (context == null) {
                    context = new SerializerContext();
                }
            }
        }
        return context;
    }

    @Override
    public Object deserialize(byte[] bytes) {
        return kryoSerializer.deserialize(bytes);
    }

    @Override
    public byte[] serialize(Object obj) {
        return kryoSerializer.serialize(obj);
    }
}
