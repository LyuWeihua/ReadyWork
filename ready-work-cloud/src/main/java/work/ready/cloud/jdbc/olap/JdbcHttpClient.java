/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package work.ready.cloud.jdbc.olap;

import work.ready.cloud.jdbc.olap.client.ClientVersion;
import work.ready.cloud.jdbc.olap.client.HttpClient;
import work.ready.cloud.jdbc.olap.proto.*;
import work.ready.cloud.jdbc.common.unit.TimeValue;
import work.ready.core.tools.define.BiTuple;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static work.ready.core.tools.StrUtil.EMPTY;

class JdbcHttpClient {
    private final HttpClient httpClient;
    private final JdbcConfiguration conCfg;
    private InfoResponse serverInfo;

    JdbcHttpClient(JdbcConfiguration conCfg) throws SQLException {
        this(conCfg, true);
    }

    JdbcHttpClient(JdbcConfiguration conCfg, boolean checkServer) throws SQLException {
        httpClient = new HttpClient(conCfg);
        this.conCfg = conCfg;
        if (checkServer) {
            this.serverInfo = fetchServerInfo();
            checkServerVersion();
        }
    }

    boolean ping(long timeoutInMs) throws SQLException {
        return httpClient.ping(timeoutInMs);
    }

    Cursor query(String sql, List<SqlTypedParamValue> params, RequestMeta meta) throws SQLException {
        int fetch = meta.fetchSize() > 0 ? meta.fetchSize() : conCfg.pageSize();
        SqlQueryRequest sqlRequest = new SqlQueryRequest(sql, params, conCfg.zoneId(),
                fetch,
                TimeValue.timeValueMillis(meta.timeoutInMs()),
                TimeValue.timeValueMillis(meta.queryTimeoutInMs()),
                null,
                Boolean.FALSE,
                null,
                new RequestInfo(Mode.PLAIN, ClientVersion.CURRENT),
                conCfg.fieldMultiValueLeniency(),
                conCfg.indexIncludeFrozen(),
                conCfg.binaryCommunication());
        SqlQueryResponse response = httpClient.query(sqlRequest);
        return new DefaultCursor(this, response.cursor(), toJdbcColumnInfo(response.columns()), response.rows(), meta);
    }

    BiTuple<String, List<List<Object>>> nextPage(String cursor, RequestMeta meta) throws SQLException {
        SqlQueryRequest sqlRequest = new SqlQueryRequest(cursor, TimeValue.timeValueMillis(meta.timeoutInMs()),
                TimeValue.timeValueMillis(meta.queryTimeoutInMs()), new RequestInfo(Mode.PLAIN), conCfg.binaryCommunication());
        SqlQueryResponse response = httpClient.query(sqlRequest);
        return new BiTuple<>(response.cursor(), response.rows());
    }

    boolean queryClose(String cursor) throws SQLException {
        return httpClient.queryClose(cursor, Mode.PLAIN);
    }

    InfoResponse serverInfo() throws SQLException {
        if (serverInfo == null) {
            serverInfo = fetchServerInfo();
        }
        return serverInfo;
    }

    private InfoResponse fetchServerInfo() throws SQLException {
        MainResponse mainResponse = httpClient.serverInfo();
        SqlVersion version = SqlVersion.fromString(mainResponse.getVersion());
        return new InfoResponse(mainResponse.getClusterName(), version);
    }

    private void checkServerVersion() throws SQLException {
        if (ClientVersion.isServerCompatible(serverInfo.version) == false) {
            throw new SQLException("This version of the JDBC driver is only compatible with Elasticsearch version " +
                ClientVersion.CURRENT.toString() + " or newer; attempting to connect to a server version " +
                serverInfo.version.toString());
        }
    }

    private List<JdbcColumnInfo> toJdbcColumnInfo(List<ColumnInfo> columns) throws SQLException {
        List<JdbcColumnInfo> cols = new ArrayList<>(columns.size());
        for (ColumnInfo info : columns) {
            cols.add(new JdbcColumnInfo(info.name(), TypeUtils.of(info.esType()), EMPTY, EMPTY, EMPTY, EMPTY, info.displaySize()));
        }
        return cols;
    }
}
