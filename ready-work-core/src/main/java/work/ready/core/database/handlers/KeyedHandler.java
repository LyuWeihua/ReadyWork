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

import work.ready.core.database.query.RowProcessor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class KeyedHandler<K> extends AbstractKeyedHandler<K, Map<String, Object>> {

    protected final RowProcessor convert;

    protected final int columnIndex;

    protected final String columnName;

    public KeyedHandler() {
        this(ArrayHandler.ROW_PROCESSOR, 1, null);
    }

    public KeyedHandler(final RowProcessor convert) {
        this(convert, 1, null);
    }

    public KeyedHandler(final int columnIndex) {
        this(ArrayHandler.ROW_PROCESSOR, columnIndex, null);
    }

    public KeyedHandler(final String columnName) {
        this(ArrayHandler.ROW_PROCESSOR, 1, columnName);
    }

    private KeyedHandler(final RowProcessor convert, final int columnIndex,
            final String columnName) {
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
    protected Map<String, Object> createRow(final ResultSet rs) throws SQLException {
        return this.convert.toMap(rs);
    }

}
