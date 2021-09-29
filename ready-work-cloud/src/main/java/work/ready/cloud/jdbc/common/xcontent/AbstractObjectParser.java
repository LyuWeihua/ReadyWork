/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package work.ready.cloud.jdbc.common.xcontent;

import work.ready.core.tools.define.CheckedFunction;
import work.ready.cloud.jdbc.common.ParseField;
import work.ready.cloud.jdbc.common.xcontent.ObjectParser.NamedObjectParser;
import work.ready.cloud.jdbc.common.xcontent.ObjectParser.ValueType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class AbstractObjectParser<Value, Context> {

    public abstract <T> void declareField(BiConsumer<Value, T> consumer, ContextParser<Context, T> parser, ParseField parseField,
            ValueType type);

    public abstract <T> void declareNamedObject(BiConsumer<Value, T> consumer, NamedObjectParser<T, Context> namedObjectParser,
                                                 ParseField parseField);

    public abstract <T> void declareNamedObjects(BiConsumer<Value, List<T>> consumer, NamedObjectParser<T, Context> namedObjectParser,
            ParseField parseField);

    public abstract <T> void declareNamedObjects(BiConsumer<Value, List<T>> consumer, NamedObjectParser<T, Context> namedObjectParser,
            Consumer<Value> orderedModeCallback, ParseField parseField);

    public abstract String getName();

    public <T> void declareField(BiConsumer<Value, T> consumer, CheckedFunction<XContentParser, T, IOException> parser,
            ParseField parseField, ValueType type) {
        if (parser == null) {
            throw new IllegalArgumentException("[parser] is required");
        }
        declareField(consumer, (p, c) -> parser.apply(p), parseField, type);
    }

    public <T> void declareObject(BiConsumer<Value, T> consumer, ContextParser<Context, T> objectParser, ParseField field) {
        declareField(consumer, (p, c) -> objectParser.parse(p, c), field, ValueType.OBJECT);
    }

    public <T> void declareObjectOrNull(BiConsumer<Value, T> consumer, ContextParser<Context, T> objectParser, T nullValue,
            ParseField field) {
        declareField(consumer, (p, c) -> p.currentToken() == XContentParser.Token.VALUE_NULL ? nullValue : objectParser.parse(p, c),
                field, ValueType.OBJECT_OR_NULL);
    }

    public void declareFloat(BiConsumer<Value, Float> consumer, ParseField field) {
        
        declareField(consumer, p -> p.floatValue(), field, ValueType.FLOAT);
    }

    public void declareFloatOrNull(BiConsumer<Value, Float> consumer, float nullValue, ParseField field) {
        declareField(consumer, p -> p.currentToken() == XContentParser.Token.VALUE_NULL ? nullValue : p.floatValue(),
                field, ValueType.FLOAT_OR_NULL);
    }

    public void declareDouble(BiConsumer<Value, Double> consumer, ParseField field) {
        
        declareField(consumer, p -> p.doubleValue(), field, ValueType.DOUBLE);
    }

    public void declareDoubleOrNull(BiConsumer<Value, Double> consumer, double nullValue, ParseField field) {
        declareField(consumer, p -> p.currentToken() == XContentParser.Token.VALUE_NULL ? nullValue : p.doubleValue(),
                field, ValueType.DOUBLE_OR_NULL);
    }

    public void declareLong(BiConsumer<Value, Long> consumer, ParseField field) {
        
        declareField(consumer, p -> p.longValue(), field, ValueType.LONG);
    }

    public void declareLongOrNull(BiConsumer<Value, Long> consumer, long nullValue, ParseField field) {
        
        declareField(consumer, p -> p.currentToken() == XContentParser.Token.VALUE_NULL ? nullValue : p.longValue(),
            field, ValueType.LONG_OR_NULL);
    }

    public void declareInt(BiConsumer<Value, Integer> consumer, ParseField field) {
        
        declareField(consumer, p -> p.intValue(), field, ValueType.INT);
    }

    public void declareIntOrNull(BiConsumer<Value, Integer> consumer, int nullValue, ParseField field) {
        declareField(consumer, p -> p.currentToken() == XContentParser.Token.VALUE_NULL ? nullValue : p.intValue(),
                field, ValueType.INT_OR_NULL);
    }

    public void declareString(BiConsumer<Value, String> consumer, ParseField field) {
        declareField(consumer, XContentParser::text, field, ValueType.STRING);
    }

    public <T> void declareString(BiConsumer<Value, T> consumer, Function<String, T> fromStringFunction, ParseField field) {
        declareField(consumer, p -> fromStringFunction.apply(p.text()), field, ValueType.STRING);
    }

    public void declareStringOrNull(BiConsumer<Value, String> consumer, ParseField field) {
        declareField(consumer, (p) -> p.currentToken() == XContentParser.Token.VALUE_NULL ? null : p.text(), field,
                ValueType.STRING_OR_NULL);
    }

    public void declareBoolean(BiConsumer<Value, Boolean> consumer, ParseField field) {
        declareField(consumer, XContentParser::booleanValue, field, ValueType.BOOLEAN);
    }

    public <T> void declareObjectArray(BiConsumer<Value, List<T>> consumer, ContextParser<Context, T> objectParser, ParseField field) {
        declareFieldArray(consumer, (p, c) -> objectParser.parse(p, c), field, ValueType.OBJECT_ARRAY);
    }

    public <
        T> void declareObjectArrayOrNull(
        BiConsumer<Value, List<T>> consumer,
        ContextParser<Context, T> objectParser,
        ParseField field
    ) {
        declareField(
            (value, list) -> { if (list != null) consumer.accept(value, list); },
            (p, c) -> p.currentToken() == XContentParser.Token.VALUE_NULL ? null : parseArray(p, () -> objectParser.parse(p, c)),
            field,
            ValueType.OBJECT_ARRAY_OR_NULL
        );
    }

    public void declareStringArray(BiConsumer<Value, List<String>> consumer, ParseField field) {
        declareFieldArray(consumer, (p, c) -> p.text(), field, ValueType.STRING_ARRAY);
    }

    public void declareDoubleArray(BiConsumer<Value, List<Double>> consumer, ParseField field) {
        declareFieldArray(consumer, (p, c) -> p.doubleValue(), field, ValueType.DOUBLE_ARRAY);
    }

    public void declareFloatArray(BiConsumer<Value, List<Float>> consumer, ParseField field) {
        declareFieldArray(consumer, (p, c) -> p.floatValue(), field, ValueType.FLOAT_ARRAY);
    }

    public void declareLongArray(BiConsumer<Value, List<Long>> consumer, ParseField field) {
        declareFieldArray(consumer, (p, c) -> p.longValue(), field, ValueType.LONG_ARRAY);
    }

    public void declareIntArray(BiConsumer<Value, List<Integer>> consumer, ParseField field) {
        declareFieldArray(consumer, (p, c) -> p.intValue(), field, ValueType.INT_ARRAY);
    }

    public <T> void declareFieldArray(BiConsumer<Value, List<T>> consumer, ContextParser<Context, T> itemParser,
                                      ParseField field, ValueType type) {
        declareField(consumer, (p, c) -> parseArray(p, () -> itemParser.parse(p, c)), field, type);
    }

    public abstract void declareRequiredFieldSet(String... requiredSet);

    public abstract void declareExclusiveFieldSet(String... exclusiveSet);

    private interface IOSupplier<T> {
        T get() throws IOException;
    }

    private static <T> List<T> parseArray(XContentParser parser, IOSupplier<T> supplier) throws IOException {
        List<T> list = new ArrayList<>();
        if (parser.currentToken().isValue()
                || parser.currentToken() == XContentParser.Token.VALUE_NULL
                || parser.currentToken() == XContentParser.Token.START_OBJECT) {
            list.add(supplier.get()); 
        } else {
            while (parser.nextToken() != XContentParser.Token.END_ARRAY) {
                if (parser.currentToken().isValue()
                        || parser.currentToken() == XContentParser.Token.VALUE_NULL
                        || parser.currentToken() == XContentParser.Token.START_OBJECT) {
                    list.add(supplier.get());
                } else {
                    throw new IllegalStateException("expected value but got [" + parser.currentToken() + "]");
                }
            }
        }
        return list;
    }
}
