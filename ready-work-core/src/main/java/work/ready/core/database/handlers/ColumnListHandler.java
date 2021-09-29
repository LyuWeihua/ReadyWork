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

public class ColumnListHandler<T> extends AbstractListHandler<T> {

    private final int columnIndex;

    private final String columnName;

    public ColumnListHandler() {
        this(1, null);
    }

    public ColumnListHandler(final int columnIndex) {
        this(columnIndex, null);
    }

    public ColumnListHandler(final String columnName) {
        this(1, columnName);
    }

    private ColumnListHandler(final int columnIndex, final String columnName) {
        this.columnIndex = columnIndex;
        this.columnName = columnName;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected T handleRow(final ResultSet rs) throws SQLException {
        if (this.columnName == null) {
            return (T) rs.getObject(this.columnIndex);
        }
        return (T) rs.getObject(this.columnName);
   }

}
