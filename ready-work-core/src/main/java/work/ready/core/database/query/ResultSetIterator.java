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
import java.sql.SQLException;
import java.util.Iterator;

public class ResultSetIterator implements Iterator<Object[]> {

    private final ResultSet rs;

    private final RowProcessor convert;

    public ResultSetIterator(final ResultSet rs) {
        this(rs, new BasicRowProcessor());
    }

    public ResultSetIterator(final ResultSet rs, final RowProcessor convert) {
        this.rs = rs;
        this.convert = convert;
    }

    @Override
    public boolean hasNext() {
        try {
            return !rs.isLast();
        } catch (final SQLException e) {
            rethrow(e);
            return false;
        }
    }

    @Override
    public Object[] next() {
        try {
            rs.next();
            return this.convert.toArray(rs);
        } catch (final SQLException e) {
            rethrow(e);
            return null;
        }
    }

    @Override
    public void remove() {
        try {
            this.rs.deleteRow();
        } catch (final SQLException e) {
            rethrow(e);
        }
    }

    protected void rethrow(final SQLException e) {
        throw new RuntimeException(e.getMessage());
    }

    public static Iterable<Object[]> iterable(final ResultSet rs) {
        return new Iterable<Object[]>() {

            @Override
            public Iterator<Object[]> iterator() {
                return new ResultSetIterator(rs);
            }

        };
    }

}
