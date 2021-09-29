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

package work.ready.core.component.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Pool;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class KryoSerializer implements Serializer {

    public static final Serializer instance = new KryoSerializer();
    private boolean registrationRequired = true;
    private List<Class<?>> register;
    static {
        System.setProperty("kryo.unsafe", "false");
    }

    private final Pool<Kryo> kryoPool = new Pool<Kryo>(true, false, 8) {
        @Override
        protected Kryo create () {
            Kryo kryo = new Kryo();
            kryo.setRegistrationRequired(registrationRequired);
            if(register != null) {
                register.forEach(kryo::register);
            }
            
            return kryo;
        }
    };

    public KryoSerializer() {

    }

    public KryoSerializer(boolean registrationRequired) {
        setRegistrationRequired(registrationRequired);
    }

    public boolean isRegistrationRequired() {
        return registrationRequired;
    }

    public void setRegistrationRequired(boolean registrationRequired) {
        this.registrationRequired = registrationRequired;
        kryoPool.clear();
    }

    public void register(Class<?> clazz) {
        if(register == null) {
            register = new ArrayList<>();
        }
        if(!register.contains(clazz)) {
            register.add(clazz);
            kryoPool.clear();
        }
    }

    @Override
    public byte[] serialize(Object obj) {
        if (obj == null) {
            return null;
        }
        Output output = null;
        Kryo kryo = kryoPool.obtain();
        try {
            output = new Output(new ByteArrayOutputStream());
            kryo.writeClassAndObject(output, obj);
            return output.toBytes();
        } finally {
            if (output != null) {
                output.close();
            }
            kryoPool.free(kryo);
        }
    }

    @Override
    public Object deserialize(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        ByteBufferInput input = null;
        Kryo kryo = kryoPool.obtain();
        try {
            input = new ByteBufferInput(bytes);
            return kryo.readClassAndObject(input);
        } finally {
            if (input != null) {
                input.close();
            }
            kryoPool.free(kryo);
        }
    }
}
