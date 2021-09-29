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

import java.sql.CallableStatement;
import java.sql.SQLException;

public class OutParameter<T> {
    private final int sqlType;
    private final Class<T> javaType;
    private T value = null;

    public OutParameter(final int sqlType, final Class<T> javaType) {
        this.sqlType = sqlType;
        this.javaType = javaType;
    }

    public OutParameter(final int sqlType, final Class<T> javaType, final T value) {
        this.sqlType = sqlType;
        this.javaType = javaType;
        this.value = value;
    }

    public int getSqlType() {
        return sqlType;
    }

    public Class<T> getJavaType() {
        return javaType;
    }

    public T getValue() {
        return value;
    }

    public void setValue(final T value) {
        this.value = value;
    }

    void setValue(final CallableStatement stmt, final int index) throws SQLException {
        final Object object = stmt.getObject(index);
        value = javaType.cast(object);
    }

    void register(final CallableStatement stmt, final int index) throws SQLException {
        stmt.registerOutParameter(index, sqlType);
        if (value != null) {
            stmt.setObject(index, value);
        }
    }

    @Override
    public String toString() {
        return "OutParameter{" + "sqlType=" + sqlType + ", javaType="
            + javaType + ", value=" + value + '}';
    }
}
