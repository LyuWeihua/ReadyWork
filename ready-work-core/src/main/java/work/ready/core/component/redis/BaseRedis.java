/**
 *
 * Original work Copyright (c) 2015-2020, Michael Yang 杨福海 (fuhai999@gmail.com).
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
package work.ready.core.component.redis;

import work.ready.core.component.serializer.JdkSerializer;
import work.ready.core.component.serializer.KryoSerializer;
import work.ready.core.component.serializer.Serializer;
import work.ready.core.handler.session.MapSession;
import work.ready.core.module.ShutdownHook;
import work.ready.core.server.Ready;
import work.ready.core.tools.ClassUtil;
import work.ready.core.tools.StrUtil;

import java.util.*;

public abstract class BaseRedis implements Redis {

    private final Serializer serializer;
    private boolean close = false;

    public BaseRedis(RedisConfig config) {
        if (StrUtil.notBlank(config.getSerializer())) {
            if (JdkSerializer.class.getName().equals(config.getSerializer()) || JdkSerializer.class.getSimpleName().equals(config.getSerializer())) {
                serializer = JdkSerializer.instance;
            } else if (KryoSerializer.class.getName().equals(config.getSerializer()) || KryoSerializer.class.getSimpleName().equals(config.getSerializer())) {
                serializer = new KryoSerializer(false);
            } else {
                try{
                    Class<Serializer> clazz = (Class<Serializer>) Class.forName(config.getSerializer(), false, Ready.getClassLoader());
                    serializer = (Serializer) ClassUtil.newInstance(clazz);
                } catch (Exception e) {
                    throw new RuntimeException("Serializer class " + config.getSerializer() +" load failed");
                }
            }
        } else {
            serializer = new KryoSerializer(false);
        }
        Ready.shutdownHook.add(ShutdownHook.STAGE_2, (inMs)->{
                System.err.println("redis disconnection.");
                close = true;
        });
    }

    public boolean isClose() {
        return close;
    }

    @Override
    public byte[] keyToBytes(Object key) {
        return key.toString().getBytes();
    }

    @Override
    public String bytesToKey(byte[] bytes) {
        return new String(bytes);
    }

    @Override
    public byte[][] keysToBytesArray(Object... keys) {
        byte[][] result = new byte[keys.length][];
        for (int i = 0; i < result.length; i++) {
            result[i] = keyToBytes(keys[i]);
        }
        return result;
    }

    @Override
    public void fieldSetFromBytesSet(Set<byte[]> data, Set<Object> result) {
        for (byte[] fieldBytes : data) {
            result.add(valueFromBytes(fieldBytes));
        }
    }

    @Override
    public byte[] valueToBytes(Object value) {
        return serializer.serialize(value);
    }

    @Override
    public Object valueFromBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return serializer.deserialize(bytes);
    }

    @Override
    public byte[][] valuesToBytesArray(Object... valuesArray) {
        byte[][] data = new byte[valuesArray.length][];
        for (int i = 0; i < data.length; i++) {
            data[i] = valueToBytes(valuesArray[i]);
        }
        return data;
    }

    @Override
    public void valueSetFromBytesSet(Set<byte[]> data, Set<Object> result) {
        for (byte[] valueBytes : data) {
            result.add(valueFromBytes(valueBytes));
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List valueListFromBytesList(Collection<byte[]> data) {
        List<Object> result = new ArrayList<Object>();
        for (byte[] d : data) {
            Object object = null;
            try {
                object = valueFromBytes(d);
            } catch (Throwable ex) {
                
                object = new String(d);
            }
            result.add(object);
        }
        return result;
    }

}

