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

import work.ready.core.database.query.BasicRowProcessor;
import work.ready.core.database.query.ResultSetHandler;
import work.ready.core.database.query.RowProcessor;

public class ArrayHandler implements ResultSetHandler<Object[]> {

    static final RowProcessor ROW_PROCESSOR = new BasicRowProcessor();

    private static final Object[] EMPTY_ARRAY = new Object[0];

    private final RowProcessor convert;

    public ArrayHandler() {
        this(ROW_PROCESSOR);
    }

    public ArrayHandler(final RowProcessor convert) {
        this.convert = convert;
    }

    @Override
    public Object[] handle(final ResultSet rs) throws SQLException {
        return rs.next() ? this.convert.toArray(rs) : EMPTY_ARRAY;
    }

}
