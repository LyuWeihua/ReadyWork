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

package work.ready.cloud.jdbc.common.io.stream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class NamedWriteableRegistry {

    public static class Entry {

        public final Class<?> categoryClass;

        public final String name;

        public final Writeable.Reader<?> reader;

        public <T extends NamedWriteable> Entry(Class<T> categoryClass, String name, Writeable.Reader<? extends T> reader) {
            this.categoryClass = Objects.requireNonNull(categoryClass);
            this.name = Objects.requireNonNull(name);
            this.reader = Objects.requireNonNull(reader);
        }
    }

    private final Map<Class<?>, Map<String, Writeable.Reader<?>>> registry;

    @SuppressWarnings("rawtypes")
    public NamedWriteableRegistry(List<Entry> entries) {
        if (entries.isEmpty()) {
            registry = Collections.emptyMap();
            return;
        }
        entries = new ArrayList<>(entries);
        entries.sort((e1, e2) -> e1.categoryClass.getName().compareTo(e2.categoryClass.getName()));

        Map<Class<?>, Map<String, Writeable.Reader<?>>> registry = new HashMap<>();
        Map<String, Writeable.Reader<?>> readers = null;
        Class currentCategory = null;
        for (Entry entry : entries) {
            if (currentCategory != entry.categoryClass) {
                if (currentCategory != null) {
                    
                    registry.put(currentCategory, Collections.unmodifiableMap(readers));
                }
                readers = new HashMap<>();
                currentCategory = entry.categoryClass;
            }

            Writeable.Reader<?> oldReader = readers.put(entry.name, entry.reader);
            if (oldReader != null) {
                throw new IllegalArgumentException("NamedWriteable [" + currentCategory.getName() + "][" + entry.name + "]" +
                    " is already registered for [" + oldReader.getClass().getName() + "]," +
                    " cannot register [" + entry.reader.getClass().getName() + "]");
            }
        }
        
        registry.put(currentCategory, Collections.unmodifiableMap(readers));

        this.registry = Collections.unmodifiableMap(registry);
    }

    public <T> Writeable.Reader<? extends T> getReader(Class<T> categoryClass, String name) {
        Map<String, Writeable.Reader<?>> readers = registry.get(categoryClass);
        if (readers == null) {
            throw new IllegalArgumentException("Unknown NamedWriteable category [" + categoryClass.getName() + "]");
        }
        @SuppressWarnings("unchecked")
        Writeable.Reader<? extends T> reader = (Writeable.Reader<? extends T>)readers.get(name);
        if (reader == null) {
            throw new IllegalArgumentException("Unknown NamedWriteable [" + categoryClass.getName() + "][" + name + "]");
        }
        return reader;
    }
}
