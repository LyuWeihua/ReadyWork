/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package work.ready.core.database.query;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

public class BasicRowProcessor implements RowProcessor {

    private static final BeanProcessor defaultConvert = new BeanProcessor();

    private static final BasicRowProcessor instance = new BasicRowProcessor();

    protected static Map<String, Object> createCaseInsensitiveHashMap(final int cols) {
        return new CaseInsensitiveHashMap(cols);
    }

    @Deprecated
    public static BasicRowProcessor instance() {
        return instance;
    }

    private final BeanProcessor convert;

    public BasicRowProcessor() {
        this(defaultConvert);
    }

    public BasicRowProcessor(final BeanProcessor convert) {
        this.convert = convert;
    }

    @Override
    public Object[] toArray(final ResultSet rs) throws SQLException {
        final ResultSetMetaData meta = rs.getMetaData();
        final int cols = meta.getColumnCount();
        final Object[] result = new Object[cols];

        for (int i = 0; i < cols; i++) {
            result[i] = rs.getObject(i + 1);
        }

        return result;
    }

    @Override
    public <T> T toBean(final ResultSet rs, final Class<? extends T> type) throws SQLException {
        return this.convert.toBean(rs, type);
    }

    @Override
    public <T> List<T> toBeanList(final ResultSet rs, final Class<? extends T> type) throws SQLException {
        return this.convert.toBeanList(rs, type);
    }

    @Override
    public Map<String, Object> toMap(final ResultSet rs) throws SQLException {
        final ResultSetMetaData rsmd = rs.getMetaData();
        final int cols = rsmd.getColumnCount();
        final Map<String, Object> result = createCaseInsensitiveHashMap(cols);

        for (int i = 1; i <= cols; i++) {
            String columnName = rsmd.getColumnLabel(i);
            if (null == columnName || 0 == columnName.length()) {
              columnName = rsmd.getColumnName(i);
            }
            result.put(columnName, rs.getObject(i));
        }

        return result;
    }

    private static final class CaseInsensitiveHashMap extends LinkedHashMap<String, Object> {

        private CaseInsensitiveHashMap(final int initialCapacity) {
            super(initialCapacity);
        }

        private final Map<String, String> lowerCaseMap = new HashMap<>();

        private static final long serialVersionUID = -2848100435296897392L;

        @Override
        public boolean containsKey(final Object key) {
            final Object realKey = lowerCaseMap.get(key.toString().toLowerCase(Locale.ENGLISH));
            return super.containsKey(realKey);

        }

        @Override
        public Object get(final Object key) {
            final Object realKey = lowerCaseMap.get(key.toString().toLowerCase(Locale.ENGLISH));
            return super.get(realKey);
        }

        @Override
        public Object put(final String key, final Object value) {
            
            final Object oldKey = lowerCaseMap.put(key.toLowerCase(Locale.ENGLISH), key);
            final Object oldValue = super.remove(oldKey);
            super.put(key, value);
            return oldValue;
        }

        @Override
        public void putAll(final Map<? extends String, ?> m) {
            for (final Map.Entry<? extends String, ?> entry : m.entrySet()) {
                final String key = entry.getKey();
                final Object value = entry.getValue();
                this.put(key, value);
            }
        }

        @Override
        public Object remove(final Object key) {
            final Object realKey = lowerCaseMap.remove(key.toString().toLowerCase(Locale.ENGLISH));
            return super.remove(realKey);
        }
    }

}
