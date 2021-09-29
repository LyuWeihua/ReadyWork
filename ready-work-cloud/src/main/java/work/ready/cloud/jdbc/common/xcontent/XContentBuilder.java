/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http:www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package work.ready.cloud.jdbc.common.xcontent;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Function;

public final class XContentBuilder implements Closeable, Flushable {

    public static XContentBuilder builder(XContent xContent) throws IOException {
        return new XContentBuilder(xContent, new ByteArrayOutputStream());
    }

    public static XContentBuilder builder(XContent xContent, Set<String> includes, Set<String> excludes) throws IOException {
        return new XContentBuilder(xContent, new ByteArrayOutputStream(), includes, excludes);
    }

    private static final Map<Class<?>, Writer> WRITERS;
    private static final Map<Class<?>, HumanReadableTransformer> HUMAN_READABLE_TRANSFORMERS;
    private static final Map<Class<?>, Function<Object, Object>> DATE_TRANSFORMERS;
    static {
        Map<Class<?>, Writer> writers = new HashMap<>();
        writers.put(Boolean.class, (b, v) -> b.value((Boolean) v));
        writers.put(Byte.class, (b, v) -> b.value((Byte) v));
        writers.put(byte[].class, (b, v) -> b.value((byte[]) v));
        writers.put(Date.class, XContentBuilder::timeValue);
        writers.put(Double.class, (b, v) -> b.value((Double) v));
        writers.put(double[].class, (b, v) -> b.values((double[]) v));
        writers.put(Float.class, (b, v) -> b.value((Float) v));
        writers.put(float[].class, (b, v) -> b.values((float[]) v));
        writers.put(Integer.class, (b, v) -> b.value((Integer) v));
        writers.put(int[].class, (b, v) -> b.values((int[]) v));
        writers.put(Long.class, (b, v) -> b.value((Long) v));
        writers.put(long[].class, (b, v) -> b.values((long[]) v));
        writers.put(Short.class, (b, v) -> b.value((Short) v));
        writers.put(short[].class, (b, v) -> b.values((short[]) v));
        writers.put(String.class, (b, v) -> b.value((String) v));
        writers.put(String[].class, (b, v) -> b.values((String[]) v));
        writers.put(Locale.class, (b, v) -> b.value(v.toString()));
        writers.put(Class.class, (b, v) -> b.value(v.toString()));
        writers.put(ZonedDateTime.class, (b, v) -> b.value(v.toString()));
        writers.put(Calendar.class, XContentBuilder::timeValue);
        writers.put(GregorianCalendar.class, XContentBuilder::timeValue);
        writers.put(BigInteger.class, (b, v) -> b.value((BigInteger) v));
        writers.put(BigDecimal.class, (b, v) -> b.value((BigDecimal) v));

        Map<Class<?>, HumanReadableTransformer> humanReadableTransformer = new HashMap<>();
        Map<Class<?>, Function<Object, Object>> dateTransformers = new HashMap<>();

        dateTransformers.put(String.class, Function.identity());

        for (XContentBuilderExtension service : ServiceLoader.load(XContentBuilderExtension.class)) {
            Map<Class<?>, Writer> addlWriters = service.getXContentWriters();
            Map<Class<?>, HumanReadableTransformer> addlTransformers = service.getXContentHumanReadableTransformers();
            Map<Class<?>, Function<Object, Object>> addlDateTransformers = service.getDateTransformers();

            addlWriters.forEach((key, value) -> Objects.requireNonNull(value,
                "invalid null xcontent writer for class " + key));
            addlTransformers.forEach((key, value) -> Objects.requireNonNull(value,
                "invalid null xcontent transformer for human readable class " + key));
            dateTransformers.forEach((key, value) -> Objects.requireNonNull(value,
                "invalid null xcontent date transformer for class " + key));

            writers.putAll(addlWriters);
            humanReadableTransformer.putAll(addlTransformers);
            dateTransformers.putAll(addlDateTransformers);
        }

        WRITERS = Collections.unmodifiableMap(writers);
        HUMAN_READABLE_TRANSFORMERS = Collections.unmodifiableMap(humanReadableTransformer);
        DATE_TRANSFORMERS = Collections.unmodifiableMap(dateTransformers);
    }

    @FunctionalInterface
    public interface Writer {
        void write(XContentBuilder builder, Object value) throws IOException;
    }

    @FunctionalInterface
    public interface HumanReadableTransformer {
        Object rawValue(Object value) throws IOException;
    }

    private final XContentGenerator generator;

    private final OutputStream bos;

    private boolean humanReadable = false;

    public XContentBuilder(XContent xContent, OutputStream bos) throws IOException {
        this(xContent, bos, Collections.emptySet(), Collections.emptySet());
    }

    public XContentBuilder(XContent xContent, OutputStream bos, Set<String> includes) throws IOException {
        this(xContent, bos, includes, Collections.emptySet());
    }

    public XContentBuilder(XContent xContent, OutputStream os, Set<String> includes, Set<String> excludes) throws IOException {
        this.bos = os;
        this.generator = xContent.createGenerator(bos, includes, excludes);
    }

    public XContentType contentType() {
        return generator.contentType();
    }

    public OutputStream getOutputStream() {
        return bos;
    }

    public XContentBuilder prettyPrint() {
        generator.usePrettyPrint();
        return this;
    }

    public boolean isPrettyPrint() {
        return generator.isPrettyPrint();
    }

    public XContentBuilder lfAtEnd() {
        generator.usePrintLineFeedAtEnd();
        return this;
    }

    public XContentBuilder humanReadable(boolean humanReadable) {
        this.humanReadable = humanReadable;
        return this;
    }

    public boolean humanReadable() {
        return this.humanReadable;
    }

    public XContentBuilder startObject() throws IOException {
        generator.writeStartObject();
        return this;
    }

    public XContentBuilder startObject(String name) throws IOException {
        return field(name).startObject();
    }

    public XContentBuilder endObject() throws IOException {
        generator.writeEndObject();
        return this;
    }

    public XContentBuilder startArray() throws IOException {
        generator.writeStartArray();
        return this;
    }

    public XContentBuilder startArray(String name) throws IOException {
        return field(name).startArray();
    }

    public XContentBuilder endArray() throws IOException {
        generator.writeEndArray();
        return this;
    }

    public XContentBuilder field(String name) throws IOException {
        ensureNameNotNull(name);
        generator.writeFieldName(name);
        return this;
    }

    public XContentBuilder nullField(String name) throws IOException {
        ensureNameNotNull(name);
        generator.writeNullField(name);
        return this;
    }

    public XContentBuilder nullValue() throws IOException {
        generator.writeNull();
        return this;
    }

    public XContentBuilder field(String name, Boolean value) throws IOException {
        return (value == null) ? nullField(name) : field(name, value.booleanValue());
    }

    public XContentBuilder field(String name, boolean value) throws IOException {
        ensureNameNotNull(name);
        generator.writeBooleanField(name, value);
        return this;
    }

    public XContentBuilder array(String name, boolean[] values) throws IOException {
        return field(name).values(values);
    }

    private XContentBuilder values(boolean[] values) throws IOException {
        if (values == null) {
            return nullValue();
        }
        startArray();
        for (boolean b : values) {
            value(b);
        }
        endArray();
        return this;
    }

    public XContentBuilder value(Boolean value) throws IOException {
        return (value == null) ? nullValue() : value(value.booleanValue());
    }

    public XContentBuilder value(boolean value) throws IOException {
        generator.writeBoolean(value);
        return this;
    }

    public XContentBuilder field(String name, Byte value) throws IOException {
        return (value == null) ? nullField(name) : field(name, value.byteValue());
    }

    public XContentBuilder field(String name, byte value) throws IOException {
        return field(name).value(value);
    }

    public XContentBuilder value(Byte value) throws IOException {
        return (value == null) ? nullValue() : value(value.byteValue());
    }

    public XContentBuilder value(byte value) throws IOException {
        generator.writeNumber(value);
        return this;
    }

    public XContentBuilder field(String name, Double value) throws IOException {
        return (value == null) ? nullField(name) : field(name, value.doubleValue());
    }

    public XContentBuilder field(String name, double value) throws IOException {
        ensureNameNotNull(name);
        generator.writeNumberField(name, value);
        return this;
    }

    public XContentBuilder array(String name, double[] values) throws IOException {
        return field(name).values(values);
    }

    private XContentBuilder values(double[] values) throws IOException {
        if (values == null) {
            return nullValue();
        }
        startArray();
        for (double b : values) {
            value(b);
        }
        endArray();
        return this;
    }

    public XContentBuilder value(Double value) throws IOException {
        return (value == null) ? nullValue() : value(value.doubleValue());
    }

    public XContentBuilder value(double value) throws IOException {
        generator.writeNumber(value);
        return this;
    }

    public XContentBuilder field(String name, Float value) throws IOException {
        return (value == null) ? nullField(name) : field(name, value.floatValue());
    }

    public XContentBuilder field(String name, float value) throws IOException {
        ensureNameNotNull(name);
        generator.writeNumberField(name, value);
        return this;
    }

    public XContentBuilder array(String name, float[] values) throws IOException {
        return field(name).values(values);
    }

    private XContentBuilder values(float[] values) throws IOException {
        if (values == null) {
            return nullValue();
        }
        startArray();
        for (float f : values) {
            value(f);
        }
        endArray();
        return this;
    }

    public XContentBuilder value(Float value) throws IOException {
        return (value == null) ? nullValue() : value(value.floatValue());
    }

    public XContentBuilder value(float value) throws IOException {
        generator.writeNumber(value);
        return this;
    }

    public XContentBuilder field(String name, Integer value) throws IOException {
        return (value == null) ? nullField(name) : field(name, value.intValue());
    }

    public XContentBuilder field(String name, int value) throws IOException {
        ensureNameNotNull(name);
        generator.writeNumberField(name, value);
        return this;
    }

    public XContentBuilder array(String name, int[] values) throws IOException {
        return field(name).values(values);
    }

    private XContentBuilder values(int[] values) throws IOException {
        if (values == null) {
            return nullValue();
        }
        startArray();
        for (int i : values) {
            value(i);
        }
        endArray();
        return this;
    }

    public XContentBuilder value(Integer value) throws IOException {
        return (value == null) ? nullValue() : value(value.intValue());
    }

    public XContentBuilder value(int value) throws IOException {
        generator.writeNumber(value);
        return this;
    }

    public XContentBuilder field(String name, Long value) throws IOException {
        return (value == null) ? nullField(name) : field(name, value.longValue());
    }

    public XContentBuilder field(String name, long value) throws IOException {
        ensureNameNotNull(name);
        generator.writeNumberField(name, value);
        return this;
    }

    public XContentBuilder array(String name, long[] values) throws IOException {
        return field(name).values(values);
    }

    private XContentBuilder values(long[] values) throws IOException {
        if (values == null) {
            return nullValue();
        }
        startArray();
        for (long l : values) {
            value(l);
        }
        endArray();
        return this;
    }

    public XContentBuilder value(Long value) throws IOException {
        return (value == null) ? nullValue() : value(value.longValue());
    }

    public XContentBuilder value(long value) throws IOException {
        generator.writeNumber(value);
        return this;
    }

    public XContentBuilder field(String name, Short value) throws IOException {
        return (value == null) ? nullField(name) : field(name, value.shortValue());
    }

    public XContentBuilder field(String name, short value) throws IOException {
        return field(name).value(value);
    }

    public XContentBuilder array(String name, short[] values) throws IOException {
        return field(name).values(values);
    }

    private XContentBuilder values(short[] values) throws IOException {
        if (values == null) {
            return nullValue();
        }
        startArray();
        for (short s : values) {
            value(s);
        }
        endArray();
        return this;
    }

    public XContentBuilder value(Short value) throws IOException {
        return (value == null) ? nullValue() : value(value.shortValue());
    }

    public XContentBuilder value(short value) throws IOException {
        generator.writeNumber(value);
        return this;
    }

    public XContentBuilder field(String name, BigInteger value) throws IOException {
        if (value == null) {
            return nullField(name);
        }
        ensureNameNotNull(name);
        generator.writeNumberField(name, value);
        return this;
    }

    public XContentBuilder array(String name, BigInteger[] values) throws IOException {
        return field(name).values(values);
    }

    private XContentBuilder values(BigInteger[] values) throws IOException {
        if (values == null) {
            return nullValue();
        }
        startArray();
        for (BigInteger b : values) {
            value(b);
        }
        endArray();
        return this;
    }

    public XContentBuilder value(BigInteger value) throws IOException {
        if (value == null) {
            return nullValue();
        }
        generator.writeNumber(value);
        return this;
    }

    public XContentBuilder field(String name, BigDecimal value) throws IOException {
        if (value == null) {
            return nullField(name);
        }
        ensureNameNotNull(name);
        generator.writeNumberField(name, value);
        return this;
    }

    public XContentBuilder array(String name, BigDecimal[] values) throws IOException {
        return field(name).values(values);
    }

    private XContentBuilder values(BigDecimal[] values) throws IOException {
        if (values == null) {
            return nullValue();
        }
        startArray();
        for (BigDecimal b : values) {
            value(b);
        }
        endArray();
        return this;
    }

    public XContentBuilder value(BigDecimal value) throws IOException {
        if (value == null) {
            return nullValue();
        }
        generator.writeNumber(value);
        return this;
    }

    public XContentBuilder field(String name, String value) throws IOException {
        if (value == null) {
            return nullField(name);
        }
        ensureNameNotNull(name);
        generator.writeStringField(name, value);
        return this;
    }

    public XContentBuilder array(String name, String... values) throws IOException {
        return field(name).values(values);
    }

    private XContentBuilder values(String[] values) throws IOException {
        if (values == null) {
            return nullValue();
        }
        startArray();
        for (String s : values) {
            value(s);
        }
        endArray();
        return this;
    }

    public XContentBuilder value(String value) throws IOException {
        if (value == null) {
            return nullValue();
        }
        generator.writeString(value);
        return this;
    }

    public XContentBuilder field(String name, byte[] value) throws IOException {
        if (value == null) {
            return nullField(name);
        }
        ensureNameNotNull(name);
        generator.writeBinaryField(name, value);
        return this;
    }

    public XContentBuilder value(byte[] value) throws IOException {
        if (value == null) {
            return nullValue();
        }
        generator.writeBinary(value);
        return this;
    }

    public XContentBuilder field(String name, byte[] value, int offset, int length) throws IOException {
        return field(name).value(value, offset, length);
    }

    public XContentBuilder value(byte[] value, int offset, int length) throws IOException {
        if (value == null) {
            return nullValue();
        }
        generator.writeBinary(value, offset, length);
        return this;
    }

    public XContentBuilder utf8Value(byte[] bytes, int offset, int length) throws IOException {
        generator.writeUTF8String(bytes, offset, length);
        return this;
    }

    public XContentBuilder timeField(String name, Object timeValue) throws IOException {
        return field(name).timeValue(timeValue);
    }

    public XContentBuilder timeField(String name, String readableName, long value) throws IOException {
        assert name.equals(readableName) == false :
            "expected raw and readable field names to differ, but they were both: " + name;
        if (humanReadable) {
            Function<Object, Object> longTransformer = DATE_TRANSFORMERS.get(Long.class);
            if (longTransformer == null) {
                throw new IllegalArgumentException("cannot write time value xcontent for unknown value of type Long");
            }
            field(readableName).value(longTransformer.apply(value));
        }
        field(name, value);
        return this;
    }

    public XContentBuilder timeValue(Object timeValue) throws IOException {
        if (timeValue == null) {
            return nullValue();
        } else {
            Function<Object, Object> transformer = DATE_TRANSFORMERS.get(timeValue.getClass());
            if (transformer == null) {
                throw new IllegalArgumentException("cannot write time value xcontent for unknown value of type " + timeValue.getClass());
            }
            return value(transformer.apply(timeValue));
        }
    }

    public XContentBuilder latlon(String name, double lat, double lon) throws IOException {
        return field(name).latlon(lat, lon);
    }

    public XContentBuilder latlon(double lat, double lon) throws IOException {
        return startObject().field("lat", lat).field("lon", lon).endObject();
    }

    public XContentBuilder value(Path value) throws IOException {
        if (value == null) {
            return nullValue();
        }
        return value(value.toString());
    }

    public XContentBuilder field(String name, Object value) throws IOException {
        return field(name).value(value);
    }

    public XContentBuilder array(String name, Object... values) throws IOException {
        return field(name).values(values, true);
    }

    private XContentBuilder values(Object[] values, boolean ensureNoSelfReferences) throws IOException {
        if (values == null) {
            return nullValue();
        }
        return value(Arrays.asList(values), ensureNoSelfReferences);
    }

    public XContentBuilder value(Object value) throws IOException {
        unknownValue(value, true);
        return this;
    }

    private void unknownValue(Object value, boolean ensureNoSelfReferences) throws IOException {
        if (value == null) {
            nullValue();
            return;
        }
        Writer writer = WRITERS.get(value.getClass());
        if (writer != null) {
            writer.write(this, value);
        } else if (value instanceof Path) {
            
            value((Path) value);
        } else if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            final Map<String, ?> valueMap = (Map<String, ?>) value;
            map(valueMap, ensureNoSelfReferences, true);
        } else if (value instanceof Iterable) {
            value((Iterable<?>) value, ensureNoSelfReferences);
        } else if (value instanceof Object[]) {
            values((Object[]) value, ensureNoSelfReferences);
        } else if (value instanceof ToXContent) {
            value((ToXContent) value);
        } else if (value instanceof Enum<?>) {
            
            value(Objects.toString(value));
        } else {
            throw new IllegalArgumentException("cannot write xcontent for unknown value of type " + value.getClass());
        }
    }

    public XContentBuilder field(String name, ToXContent value) throws IOException {
        return field(name).value(value);
    }

    public XContentBuilder field(String name, ToXContent value, ToXContent.Params params) throws IOException {
        return field(name).value(value, params);
    }

    private XContentBuilder value(ToXContent value) throws IOException {
        return value(value, ToXContent.EMPTY_PARAMS);
    }

    private XContentBuilder value(ToXContent value, ToXContent.Params params) throws IOException {
        if (value == null) {
            return nullValue();
        }
        value.toXContent(this, params);
        return this;
    }

    public XContentBuilder field(String name, Map<String, Object> values) throws IOException {
        return field(name).map(values);
    }

    public XContentBuilder map(Map<String, ?> values) throws IOException {
        return map(values, true, true);
    }

    public XContentBuilder mapContents(Map<String, ?> values) throws IOException {
        return map(values, true, false);
    }

    private XContentBuilder map(Map<String, ?> values, boolean ensureNoSelfReferences, boolean writeStartAndEndHeaders) throws IOException {
        if (values == null) {
            return nullValue();
        }

        if (ensureNoSelfReferences) {
            ensureNoSelfReferences(values);
        }

        if (writeStartAndEndHeaders) {
            startObject();
        }
        for (Map.Entry<String, ?> value : values.entrySet()) {
            field(value.getKey());
            
            unknownValue(value.getValue(), false);
        }
        if (writeStartAndEndHeaders) {
            endObject();
        }
        return this;
    }

    public XContentBuilder field(String name, Iterable<?> values) throws IOException {
        return field(name).value(values);
    }

    private XContentBuilder value(Iterable<?> values, boolean ensureNoSelfReferences) throws IOException {
        if (values == null) {
            return nullValue();
        }

        if (values instanceof Path) {
            
            value((Path) values);
        } else {

            if (ensureNoSelfReferences) {
                ensureNoSelfReferences(values);
            }
            startArray();
            for (Object value : values) {
                
                unknownValue(value, false);
            }
            endArray();
        }
        return this;
    }

    public XContentBuilder humanReadableField(String rawFieldName, String readableFieldName, Object value) throws IOException {
        assert rawFieldName.equals(readableFieldName) == false :
            "expected raw and readable field names to differ, but they were both: " + rawFieldName;
        if (humanReadable) {
            field(readableFieldName, Objects.toString(value));
        }
        HumanReadableTransformer transformer = HUMAN_READABLE_TRANSFORMERS.get(value.getClass());
        if (transformer != null) {
            Object rawValue = transformer.rawValue(value);
            field(rawFieldName, rawValue);
        } else {
            throw new IllegalArgumentException("no raw transformer found for class " + value.getClass());
        }
        return this;
    }

    public XContentBuilder percentageField(String rawFieldName, String readableFieldName, double percentage) throws IOException {
        assert rawFieldName.equals(readableFieldName) == false :
            "expected raw and readable field names to differ, but they were both: " + rawFieldName;
        if (humanReadable) {
            field(readableFieldName, String.format(Locale.ROOT, "%1.1f%%", percentage));
        }
        field(rawFieldName, percentage);
        return this;
    }

    @Deprecated
    public XContentBuilder rawField(String name, InputStream value) throws IOException {
        generator.writeRawField(name, value);
        return this;
    }

    public XContentBuilder rawField(String name, InputStream value, XContentType contentType) throws IOException {
        generator.writeRawField(name, value, contentType);
        return this;
    }

    public XContentBuilder rawValue(InputStream stream, XContentType contentType) throws IOException {
        generator.writeRawValue(stream, contentType);
        return this;
    }

    public XContentBuilder copyCurrentStructure(XContentParser parser) throws IOException {
        generator.copyCurrentStructure(parser);
        return this;
    }

    @Override
    public void flush() throws IOException {
        generator.flush();
    }

    @Override
    public void close() {
        try {
            generator.close();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to close the XContentBuilder", e);
        }
    }

    public XContentGenerator generator() {
        return this.generator;
    }

    static void ensureNameNotNull(String name) {
        ensureNotNull(name, "Field name cannot be null");
    }

    static void ensureNotNull(Object value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void ensureNoSelfReferences(Object value) {
        Iterable<?> it = convert(value);
        if (it != null) {
            ensureNoSelfReferences(it, value, Collections.newSetFromMap(new IdentityHashMap<>()));
        }
    }

    private static Iterable<?> convert(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map) {
            return ((Map<?,?>) value).values();
        } else if ((value instanceof Iterable) && (value instanceof Path == false)) {
            return (Iterable<?>) value;
        } else if (value instanceof Object[]) {
            return Arrays.asList((Object[]) value);
        } else {
            return null;
        }
    }

    private static void ensureNoSelfReferences(final Iterable<?> value, Object originalReference, final Set<Object> ancestors) {
        if (value != null) {
            if (ancestors.add(originalReference) == false) {
                throw new IllegalArgumentException("Iterable object is self-referencing itself");
            }
            for (Object o : value) {
                ensureNoSelfReferences(convert(o), o, ancestors);
            }
            ancestors.remove(originalReference);
        }
    }

}
