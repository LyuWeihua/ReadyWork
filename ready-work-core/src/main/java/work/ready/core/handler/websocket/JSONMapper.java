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

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.temporal.ChronoField.*;

public class JSONMapper<T> {

    public static final ObjectMapper mapper = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        return JsonMapper.builder()
                         .addModule(timeModule())
                         
                         .addModule(new AfterburnerModule().setUseValueClassLoader(false))
                         .defaultDateFormat(new StdDateFormat())
                         
                         .visibility(new VisibilityChecker.Std(NONE, NONE, NONE, NONE, PUBLIC_ONLY))
                         .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                         .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                         .deactivateDefaultTyping()
                         .build();
    }

    private static JavaTimeModule timeModule() {
        var module = new JavaTimeModule();

        DateTimeFormatter localTimeFormatter = new DateTimeFormatterBuilder()
            .parseStrict()
            .appendValue(HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(MINUTE_OF_HOUR, 2)
            .appendLiteral(':')
            .appendValue(SECOND_OF_MINUTE, 2)
            .appendFraction(NANO_OF_SECOND, 3, 9, true) 
            .toFormatter();

        module.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer(ISO_INSTANT));
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(new DateTimeFormatterBuilder()
            .parseStrict()
            .append(ISO_LOCAL_DATE)
            .appendLiteral('T')
            .append(localTimeFormatter)
            .toFormatter()));
        module.addSerializer(LocalTime.class, new LocalTimeSerializer(new DateTimeFormatterBuilder()
            .parseStrict()
            .append(localTimeFormatter)
            .toFormatter()));
        return module;
    }

    private final ObjectReader reader;
    private final ObjectWriter writer;

    public JSONMapper(Type instanceType) {
        JavaType type = mapper.getTypeFactory().constructType(instanceType);
        reader = mapper.readerFor(type);
        writer = mapper.writerFor(type);
    }

    public T fromJSON(byte[] json) {
        try {
            return reader.readValue(json);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] toJSON(T instance) {
        try {
            return (writer.writeValueAsString(instance)).getBytes(UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
