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
import java.util.HashMap;
import java.util.Map;

import work.ready.core.database.query.ResultSetHandler;

public abstract class AbstractKeyedHandler<K, V> implements ResultSetHandler<Map<K, V>> {

    @Override
    public Map<K, V> handle(final ResultSet rs) throws SQLException {
        final Map<K, V> result = createMap();
        while (rs.next()) {
            result.put(createKey(rs), createRow(rs));
        }
        return result;
    }

    protected Map<K, V> createMap() {
        return new HashMap<>();
    }

    protected abstract K createKey(ResultSet rs) throws SQLException;

    protected abstract V createRow(ResultSet rs) throws SQLException;

}
