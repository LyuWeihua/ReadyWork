/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package work.ready.cloud.jdbc.olap;

import work.ready.cloud.jdbc.olap.proto.SqlTypedParamValue;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class PreparedQuery {

    static class ParamInfo {
        EsType type;
        Object value;

        ParamInfo(Object value, EsType type) {
            this.value = value;
            this.type = type;
        }
    }

    private final String sql;
    private final ParamInfo[] params;

    private PreparedQuery(String sql, int paramCount) {
        this.sql = sql;
        this.params = new ParamInfo[paramCount];
        clearParams();
    }

    ParamInfo getParam(int param) throws JdbcSQLException {
        if (param < 1 || param > params.length) {
            throw new JdbcSQLException("Invalid parameter index [" + param + "]");
        }
        return params[param - 1];
    }

    void setParam(int param, Object value, EsType type) throws JdbcSQLException {
        if (param < 1 || param > params.length) {
            throw new JdbcSQLException("Invalid parameter index [" + param + "]");
        }
        params[param - 1].value = value;
        params[param - 1].type = type;
    }

    int paramCount() {
        return params.length;
    }

    void clearParams() {
        for (int i = 0; i < params.length; i++) {
            params[i] = new ParamInfo(null, EsType.KEYWORD);
        }
    }

    String sql() {
        return sql;
    }

    List<SqlTypedParamValue> params() {
        return Arrays.stream(this.params).map(
                paramInfo -> new SqlTypedParamValue(paramInfo.type.name(), paramInfo.value)
        ).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return sql() + " " + params();
    }

    static PreparedQuery prepare(String sql) throws SQLException {
        return new PreparedQuery(sql, SqlQueryParameterAnalyzer.parametersCount(sql));
    }
}
