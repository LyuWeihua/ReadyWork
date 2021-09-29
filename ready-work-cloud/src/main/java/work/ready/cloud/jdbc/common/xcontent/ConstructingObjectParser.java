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

import work.ready.cloud.jdbc.common.ParseField;
import work.ready.cloud.jdbc.common.xcontent.ObjectParser.NamedObjectParser;
import work.ready.cloud.jdbc.common.xcontent.ObjectParser.ValueType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public final class ConstructingObjectParser<Value, Context> extends AbstractObjectParser<Value, Context> implements
    BiFunction<XContentParser, Context, Value>, ContextParser<Context, Value>{

    private static final BiConsumer<?, ?> REQUIRED_CONSTRUCTOR_ARG_MARKER = (a, b) -> {
        throw new UnsupportedOperationException("I am just a marker I should never be called.");
    };

    private static final BiConsumer<?, ?> OPTIONAL_CONSTRUCTOR_ARG_MARKER = (a, b) -> {
        throw new UnsupportedOperationException("I am just a marker I should never be called.");
    };

    private final List<ConstructorArgInfo> constructorArgInfos = new ArrayList<>();
    private final ObjectParser<Target, Context> objectParser;
    private final BiFunction<Object[], Context, Value> builder;
    
    private int numberOfFields = 0;

    public ConstructingObjectParser(String name, Function<Object[], Value> builder) {
        this(name, false, builder);
    }

    public ConstructingObjectParser(String name, boolean ignoreUnknownFields, Function<Object[], Value> builder) {
        this(name, ignoreUnknownFields, (args, context) -> builder.apply(args));
    }

    public ConstructingObjectParser(String name, boolean ignoreUnknownFields, BiFunction<Object[], Context, Value> builder) {
        objectParser = new ObjectParser<>(name, ignoreUnknownFields, null);
        this.builder = builder;

    }

    @Override
    public Value apply(XContentParser parser, Context context) {
        try {
            return parse(parser, context);
        } catch (IOException e) {
            throw new XContentParseException(parser.getTokenLocation(), "[" + objectParser.getName()  + "] failed to parse object", e);
        }
    }

    @Override
    public Value parse(XContentParser parser, Context context) throws IOException {
        return objectParser.parse(parser, new Target(parser, context), context).finish();
    }

    @SuppressWarnings("unchecked") 
    public static <Value, FieldT> BiConsumer<Value, FieldT> constructorArg() {
        return (BiConsumer<Value, FieldT>) REQUIRED_CONSTRUCTOR_ARG_MARKER;
    }

    @SuppressWarnings("unchecked") 
    public static <Value, FieldT> BiConsumer<Value, FieldT> optionalConstructorArg() {
        return (BiConsumer<Value, FieldT>) OPTIONAL_CONSTRUCTOR_ARG_MARKER;
    }

    @Override
    public <T> void declareField(BiConsumer<Value, T> consumer, ContextParser<Context, T> parser, ParseField parseField, ValueType type) {
        if (consumer == null) {
            throw new IllegalArgumentException("[consumer] is required");
        }
        if (parser == null) {
            throw new IllegalArgumentException("[parser] is required");
        }
        if (parseField == null) {
            throw new IllegalArgumentException("[parseField] is required");
        }
        if (type == null) {
            throw new IllegalArgumentException("[type] is required");
        }

        if (isConstructorArg(consumer)) {
            
            int position = addConstructorArg(consumer, parseField);
            objectParser.declareField((target, v) -> target.constructorArg(position, v), parser, parseField, type);
        } else {
            numberOfFields += 1;
            objectParser.declareField(queueingConsumer(consumer, parseField), parser, parseField, type);
        }
    }

    @Override
    public <T> void declareNamedObject(BiConsumer<Value, T> consumer, NamedObjectParser<T, Context> namedObjectParser,
                                                ParseField parseField) {
        if (consumer == null) {
            throw new IllegalArgumentException("[consumer] is required");
        }
        if (namedObjectParser == null) {
            throw new IllegalArgumentException("[parser] is required");
        }
        if (parseField == null) {
            throw new IllegalArgumentException("[parseField] is required");
        }

        if (isConstructorArg(consumer)) {
            
            int position = addConstructorArg(consumer, parseField);
            objectParser.declareNamedObject((target, v) -> target.constructorArg(position, v), namedObjectParser, parseField);
        } else {
            numberOfFields += 1;
            objectParser.declareNamedObject(queueingConsumer(consumer, parseField), namedObjectParser, parseField);
        }
    }

    @Override
    public <T> void declareNamedObjects(BiConsumer<Value, List<T>> consumer, NamedObjectParser<T, Context> namedObjectParser,
            ParseField parseField) {

        if (consumer == null) {
            throw new IllegalArgumentException("[consumer] is required");
        }
        if (namedObjectParser == null) {
            throw new IllegalArgumentException("[parser] is required");
        }
        if (parseField == null) {
            throw new IllegalArgumentException("[parseField] is required");
        }

        if (isConstructorArg(consumer)) {
            
            int position = addConstructorArg(consumer, parseField);
            objectParser.declareNamedObjects((target, v) -> target.constructorArg(position, v), namedObjectParser, parseField);
        } else {
            numberOfFields += 1;
            objectParser.declareNamedObjects(queueingConsumer(consumer, parseField), namedObjectParser, parseField);
        }
    }

    @Override
    public <T> void declareNamedObjects(BiConsumer<Value, List<T>> consumer, NamedObjectParser<T, Context> namedObjectParser,
            Consumer<Value> orderedModeCallback, ParseField parseField) {
        if (consumer == null) {
            throw new IllegalArgumentException("[consumer] is required");
        }
        if (namedObjectParser == null) {
            throw new IllegalArgumentException("[parser] is required");
        }
        if (orderedModeCallback == null) {
            throw new IllegalArgumentException("[orderedModeCallback] is required");
        }
        if (parseField == null) {
            throw new IllegalArgumentException("[parseField] is required");
        }

        if (isConstructorArg(consumer)) {
            
            int position = addConstructorArg(consumer, parseField);
            objectParser.declareNamedObjects((target, v) -> target.constructorArg(position, v), namedObjectParser,
                    wrapOrderedModeCallBack(orderedModeCallback), parseField);
        } else {
            numberOfFields += 1;
            objectParser.declareNamedObjects(queueingConsumer(consumer, parseField), namedObjectParser,
                    wrapOrderedModeCallBack(orderedModeCallback), parseField);
        }
    }

    int getNumberOfFields() {
        return this.constructorArgInfos.size();
    }

    private boolean isConstructorArg(BiConsumer<?, ?> consumer) {
        return consumer == REQUIRED_CONSTRUCTOR_ARG_MARKER || consumer == OPTIONAL_CONSTRUCTOR_ARG_MARKER;
    }

    private int addConstructorArg(BiConsumer<?, ?> consumer, ParseField parseField) {
        int position = constructorArgInfos.size();
        boolean required = consumer == REQUIRED_CONSTRUCTOR_ARG_MARKER;
        constructorArgInfos.add(new ConstructorArgInfo(parseField, required));
        return position;
    }

    @Override
    public String getName() {
        return objectParser.getName();
    }

    @Override
    public void declareRequiredFieldSet(String... requiredSet) {
        objectParser.declareRequiredFieldSet(requiredSet);
    }

    @Override
    public void declareExclusiveFieldSet(String... exclusiveSet) {
        objectParser.declareExclusiveFieldSet(exclusiveSet);
    }

    private Consumer<Target> wrapOrderedModeCallBack(Consumer<Value> callback) {
        return (target) -> {
            if (target.targetObject != null) {
                
                callback.accept(target.targetObject);
                return;
            }
            
            target.queuedOrderedModeCallback = callback;
        };
    }

    private <T> BiConsumer<Target, T> queueingConsumer(BiConsumer<Value, T> consumer, ParseField parseField) {
        return (target, v) -> {
            if (target.targetObject != null) {
                
                consumer.accept(target.targetObject, v);
                return;
            }
            
            XContentLocation location = target.parser.getTokenLocation();
            target.queue(targetObject -> {
                try {
                    consumer.accept(targetObject, v);
                } catch (Exception e) {
                    throw new XContentParseException(location,
                            "[" + objectParser.getName() + "] failed to parse field [" + parseField.getPreferredName() + "]", e);
                }
            });
        };
    }

    private class Target {
        
        private final Object[] constructorArgs = new Object[constructorArgInfos.size()];
        
        private final XContentParser parser;

        private Context context;

        private int constructorArgsCollected = 0;
        
        private Consumer<Value>[] queuedFields;
        
        private Consumer<Value> queuedOrderedModeCallback;
        
        private int queuedFieldsCount = 0;
        
        private Value targetObject;

        Target(XContentParser parser, Context context) {
            this.parser = parser;
            this.context = context;
        }

        private void constructorArg(int position, Object value) {
            constructorArgs[position] = value;
            constructorArgsCollected++;
            if (constructorArgsCollected == constructorArgInfos.size()) {
                buildTarget();
            }
        }

        private void queue(Consumer<Value> queueMe) {
            assert targetObject == null: "Don't queue after the targetObject has been built! Just apply the consumer directly.";
            if (queuedFields == null) {
                @SuppressWarnings({"unchecked", "rawtypes"})
                Consumer<Value>[] queuedFields = new Consumer[numberOfFields];
                this.queuedFields = queuedFields;
            }
            queuedFields[queuedFieldsCount] = queueMe;
            queuedFieldsCount++;
        }

        private Value finish() {
            if (targetObject != null) {
                return targetObject;
            }
            
            StringBuilder message = null;
            for (int i = 0; i < constructorArgs.length; i++) {
                if (constructorArgs[i] != null) continue;
                ConstructorArgInfo arg = constructorArgInfos.get(i);
                if (false == arg.required) continue;
                if (message == null) {
                    message = new StringBuilder("Required [").append(arg.field);
                } else {
                    message.append(", ").append(arg.field);
                }
            }
            if (message != null) {
                
                throw new IllegalArgumentException(message.append(']').toString());
            }
            
            assert false == constructorArgInfos.isEmpty() : "[" + objectParser.getName() + "] must configure at least one constructor "
                        + "argument. If it doesn't have any it should use ObjectParser instead of ConstructingObjectParser. This is a bug "
                        + "in the parser declaration.";
            
            buildTarget();
            return targetObject;
        }

        private void buildTarget() {
            try {
                targetObject = builder.apply(constructorArgs, context);
                if (queuedOrderedModeCallback != null) {
                    queuedOrderedModeCallback.accept(targetObject);
                }
                while (queuedFieldsCount > 0) {
                    queuedFieldsCount -= 1;
                    queuedFields[queuedFieldsCount].accept(targetObject);
                }
            } catch (XContentParseException e) {
                throw new XContentParseException(e.getLocation(),
                    "failed to build [" + objectParser.getName() + "] after last required field arrived", e);
            } catch (Exception e) {
                throw new XContentParseException(null,
                        "Failed to build [" + objectParser.getName() + "] after last required field arrived", e);
            }
        }
    }

    private static class ConstructorArgInfo {
        final ParseField field;
        final boolean required;

        ConstructorArgInfo(ParseField field, boolean required) {
            this.field = field;
            this.required = required;
        }
    }
}
