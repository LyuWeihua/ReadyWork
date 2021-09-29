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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import work.ready.core.database.Model;

import java.io.IOException;
import java.util.Map;

public class ModelSerializer<T> extends JsonSerializer<T> {
    @Override
    public void serialize(T model, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if(model != null) {
            Map<String, Object> map = ((Model<?>)model).getData(true);
            jsonGenerator.writeObject(map);
        }
    }
}
