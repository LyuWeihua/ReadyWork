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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

public class NamedXContentRegistry {
    
    public static final NamedXContentRegistry EMPTY = new NamedXContentRegistry(emptyList());

    public static class Entry {
        
        public final Class<?> categoryClass;

        public final ParseField name;

        private final ContextParser<Object, ?> parser;

        public <T> Entry(Class<T> categoryClass, ParseField name, CheckedFunction<XContentParser, ? extends T, IOException> parser) {
            this.categoryClass = Objects.requireNonNull(categoryClass);
            this.name = Objects.requireNonNull(name);
            this.parser = Objects.requireNonNull((p, c) -> parser.apply(p));
        }
        
        public <T> Entry(Class<T> categoryClass, ParseField name, ContextParser<Object, ? extends T> parser) {
            this.categoryClass = Objects.requireNonNull(categoryClass);
            this.name = Objects.requireNonNull(name);
            this.parser = Objects.requireNonNull(parser);
        }
    }

    private final Map<Class<?>, Map<String, Entry>> registry;

    public NamedXContentRegistry(List<Entry> entries) {
        if (entries.isEmpty()) {
            registry = emptyMap();
            return;
        }
        entries = new ArrayList<>(entries);
        entries.sort((e1, e2) -> e1.categoryClass.getName().compareTo(e2.categoryClass.getName()));

        Map<Class<?>, Map<String, Entry>> registry = new HashMap<>();
        Map<String, Entry> parsers = null;
        Class<?> currentCategory = null;
        for (Entry entry : entries) {
            if (currentCategory != entry.categoryClass) {
                if (currentCategory != null) {
                    
                    registry.put(currentCategory, unmodifiableMap(parsers));
                }
                parsers = new HashMap<>();
                currentCategory = entry.categoryClass;
            }

            for (String name : entry.name.getAllNamesIncludedDeprecated()) {
                Object old = parsers.put(name, entry);
                if (old != null) {
                    throw new IllegalArgumentException("NamedXContent [" + currentCategory.getName() + "][" + entry.name + "]" +
                        " is already registered for [" + old.getClass().getName() + "]," +
                        " cannot register [" + entry.parser.getClass().getName() + "]");
                }
            }
        }
        
        registry.put(currentCategory, unmodifiableMap(parsers));

        this.registry = unmodifiableMap(registry);
    }

    public <T, C> T parseNamedObject(Class<T> categoryClass, String name, XContentParser parser, C context) throws IOException {
        Map<String, Entry> parsers = registry.get(categoryClass);
        if (parsers == null) {
            if (registry.isEmpty()) {
                
                throw new XContentParseException("named objects are not supported for this parser");
            }
            throw new XContentParseException("unknown named object category [" + categoryClass.getName() + "]");
        }
        Entry entry = parsers.get(name);
        if (entry == null) {
            throw new NamedObjectNotFoundException(parser.getTokenLocation(), "unknown field [" + name + "]", parsers.keySet());
        }
        if (false == entry.name.match(name, parser.getDeprecationHandler())) {
            
            throw new XContentParseException(parser.getTokenLocation(),
                    "unable to parse " + categoryClass.getSimpleName() + " with name [" + name + "]: parser didn't match");
        }
        return categoryClass.cast(entry.parser.parse(parser, context));
    }

}
