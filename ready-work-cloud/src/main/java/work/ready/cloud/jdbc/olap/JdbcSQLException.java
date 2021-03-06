/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package work.ready.cloud.jdbc.olap;

import java.sql.SQLException;

public class JdbcSQLException extends SQLException {

    public JdbcSQLException(String message) {
        super(message);
    }

    public JdbcSQLException(Throwable cause, String message) {
        super(message, cause);
    }
}
