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
package work.ready.core.database.handlers;

import java.sql.ResultSet;
import java.sql.SQLException;

import work.ready.core.database.query.RowProcessor;

public class BeanMapHandler<K, V> extends AbstractKeyedHandler<K, V> {

    private final Class<V> type;

    private final RowProcessor convert;

    private final int columnIndex;

    private final String columnName;

    public BeanMapHandler(final Class<V> type) {
        this(type, ArrayHandler.ROW_PROCESSOR, 1, null);
    }

    public BeanMapHandler(final Class<V> type, final RowProcessor convert) {
        this(type, convert, 1, null);
    }

    public BeanMapHandler(final Class<V> type, final int columnIndex) {
        this(type, ArrayHandler.ROW_PROCESSOR, columnIndex, null);
    }

    public BeanMapHandler(final Class<V> type, final String columnName) {
        this(type, ArrayHandler.ROW_PROCESSOR, 1, columnName);
    }

    private BeanMapHandler(final Class<V> type, final RowProcessor convert,
            final int columnIndex, final String columnName) {
        this.type = type;
        this.convert = convert;
        this.columnIndex = columnIndex;
        this.columnName = columnName;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected K createKey(final ResultSet rs) throws SQLException {
        return (columnName == null) ?
               (K) rs.getObject(columnIndex) :
               (K) rs.getObject(columnName);
    }

    @Override
    protected V createRow(final ResultSet rs) throws SQLException {
        return this.convert.toBean(rs, type);
    }

}
