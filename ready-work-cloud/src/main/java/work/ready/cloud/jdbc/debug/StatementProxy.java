/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package work.ready.cloud.jdbc.debug;

import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

final class StatementProxy extends DebuggingInvoker {

    StatementProxy(DebugLog log, Object target, Object con) {
        super(log, target, con);
    }

    @Override
    protected Object postProcess(Object result, Object proxy) {
        if (result instanceof Connection) {
            return parent;
        }
        if (result instanceof ResultSet) {
            return JdbcDebug.proxy(new ResultSetProxy(log, result, proxy));
        }
        if (result instanceof ParameterMetaData) {
            return JdbcDebug.proxy(new ParameterMetaDataProxy(log, result));
        }
        if (result instanceof ResultSetMetaData) {
            return JdbcDebug.proxy(new ResultSetMetaDataProxy(log, result));
        }

        return result;
    }
}
