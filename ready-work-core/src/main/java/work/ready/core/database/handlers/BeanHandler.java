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

import work.ready.core.database.query.ResultSetHandler;
import work.ready.core.database.query.RowProcessor;

public class BeanHandler<T> implements ResultSetHandler<T> {

    private final Class<? extends T> type;

    private final RowProcessor convert;

    public BeanHandler(final Class<? extends T> type) {
        this(type, ArrayHandler.ROW_PROCESSOR);
    }

    public BeanHandler(final Class<? extends T> type, final RowProcessor convert) {
        this.type = type;
        this.convert = convert;
    }

    @Override
    public T handle(final ResultSet rs) throws SQLException {
        return rs.next() ? this.convert.toBean(rs, this.type) : null;
    }

}
