/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package work.ready.cloud.jdbc.olap;

import java.sql.SQLException;
import java.util.List;

interface Cursor {

    List<JdbcColumnInfo> columns();

    default int columnSize() {
        return columns().size();
    }

    boolean next() throws SQLException;

    Object column(int column);

    int batchSize();

    void close() throws SQLException;
}