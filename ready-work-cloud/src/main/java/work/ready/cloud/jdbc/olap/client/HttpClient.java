/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package work.ready.cloud.jdbc.olap.client;

import work.ready.cloud.jdbc.common.xcontent.*;
import work.ready.cloud.jdbc.common.io.Streams;
import work.ready.cloud.jdbc.olap.proto.*;
import work.ready.core.tools.define.CheckedFunction;
import work.ready.cloud.jdbc.common.unit.TimeValue;
import work.ready.core.tools.define.BiTuple;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.SQLException;
import java.util.Collections;
import java.util.function.Function;

public class HttpClient {

    private final ConnectionConfiguration cfg;
    private final XContentType requestBodyContentType;

    public HttpClient(ConnectionConfiguration cfg) {
        this.cfg = cfg;
        this.requestBodyContentType = cfg.binaryCommunication() ? XContentType.CBOR : XContentType.JSON;
    }

    private NamedXContentRegistry registry = NamedXContentRegistry.EMPTY;

    public boolean ping(long timeoutInMs) throws SQLException {
        return head("/", timeoutInMs);
    }

    public MainResponse serverInfo() throws SQLException {
        return get("/", MainResponse::fromXContent);
    }

    public SqlQueryResponse basicQuery(String query, int fetchSize) throws SQLException {

        SqlQueryRequest sqlRequest = new SqlQueryRequest(query, Collections.emptyList(), Protocol.TIME_ZONE,
                fetchSize,
                TimeValue.timeValueMillis(cfg.queryTimeout()),
                TimeValue.timeValueMillis(cfg.pageTimeout()),
                null,
                Boolean.FALSE,
                null,
                new RequestInfo(Mode.CLI, ClientVersion.CURRENT),
                false,
                false,
                cfg.binaryCommunication());
        return query(sqlRequest);
    }

    public SqlQueryResponse query(SqlQueryRequest sqlRequest) throws SQLException {
        return post(Protocol.SQL_QUERY_REST_ENDPOINT, sqlRequest, SqlQueryResponse::fromXContent);
    }

    public SqlQueryResponse nextPage(String cursor) throws SQLException {
        
        SqlQueryRequest sqlRequest = new SqlQueryRequest(cursor, TimeValue.timeValueMillis(cfg.queryTimeout()),
                TimeValue.timeValueMillis(cfg.pageTimeout()), new RequestInfo(Mode.CLI), cfg.binaryCommunication());
        return post(Protocol.SQL_QUERY_REST_ENDPOINT, sqlRequest, SqlQueryResponse::fromXContent);
    }

    public boolean queryClose(String cursor, Mode mode) throws SQLException {
        SqlClearCursorResponse response = post(Protocol.CLEAR_CURSOR_REST_ENDPOINT,
            new SqlClearCursorRequest(cursor, new RequestInfo(mode)),
            SqlClearCursorResponse::fromXContent);
        return response.isSucceeded();
    }

    private <Request extends AbstractSqlRequest, Response> Response post(String path, Request request,
            CheckedFunction<XContentParser, Response, IOException> responseParser)
            throws SQLException {
        byte[] requestBytes = toXContent(request);
        String query = "error_trace";
        BiTuple<XContentType, byte[]> response =
            AccessController.doPrivileged((PrivilegedAction<JreHttpUrlConnection.ResponseOrException<BiTuple<XContentType, byte[]>>>) () ->
                JreHttpUrlConnection.http(path, query, cfg, con ->
                    con.request(
                        (out) -> out.write(requestBytes),
                        this::readFrom,
                        "POST",
                        requestBodyContentType.mediaTypeWithoutParameters() 
                    )
                )).getResponseOrThrowException();
        return fromXContent(response.get1(), response.get2(), responseParser);
    }

    private boolean head(String path, long timeoutInMs) throws SQLException {
        ConnectionConfiguration pingCfg = new ConnectionConfiguration(cfg.baseUri(), cfg.connectionString(), cfg.validateProperties(),
            cfg.binaryCommunication(), cfg.connectTimeout(), timeoutInMs, cfg.queryTimeout(), cfg.pageTimeout(), cfg.pageSize(),
            cfg.authUser(), cfg.authPass(), cfg.sslConfig(), cfg.proxyConfig());
        try {
            return AccessController.doPrivileged((PrivilegedAction<Boolean>) () ->
                JreHttpUrlConnection.http(path, "error_trace", pingCfg, JreHttpUrlConnection::head));
        } catch (ClientException ex) {
            throw new SQLException("Cannot ping server", ex);
        }
    }

    private <Response> Response get(String path, CheckedFunction<XContentParser, Response, IOException> responseParser)
        throws SQLException {
        BiTuple<XContentType, byte[]> response =
            AccessController.doPrivileged((PrivilegedAction<JreHttpUrlConnection.ResponseOrException<BiTuple<XContentType, byte[]>>>) () ->
                JreHttpUrlConnection.http(path, "error_trace", cfg, con ->
                    con.request(
                        null,
                        this::readFrom,
                        "GET"
                    )
                )).getResponseOrThrowException();
        return fromXContent(response.get1(), response.get2(), responseParser);
    }

    private <Request extends ToXContent> byte[] toXContent(Request xContent) {
        try(ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            try (XContentBuilder xContentBuilder = new XContentBuilder(requestBodyContentType.xContent(), buffer)) {
                if (xContent.isFragment()) {
                    xContentBuilder.startObject();
                }
                xContent.toXContent(xContentBuilder, ToXContent.EMPTY_PARAMS);
                if (xContent.isFragment()) {
                    xContentBuilder.endObject();
                }
            }
            return buffer.toByteArray();
        } catch (IOException ex) {
            throw new ClientException("Cannot serialize request", ex);
        }
    }

    private BiTuple<XContentType, byte[]> readFrom(InputStream inputStream, Function<String, String> headers) {
        String contentType = headers.apply("Content-Type");
        XContentType xContentType = XContentType.fromMediaType(contentType);
        if (xContentType == null) {
            throw new IllegalStateException("Unsupported Content-Type: " + contentType);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Streams.copy(inputStream, out);
        } catch (IOException ex) {
            throw new ClientException("Cannot deserialize response", ex);
        }
        return new BiTuple<>(xContentType, out.toByteArray());

    }

    private <Response> Response fromXContent(XContentType xContentType, byte[] bytesReference,
                                             CheckedFunction<XContentParser, Response, IOException> responseParser) {
        try (InputStream stream = new ByteArrayInputStream(bytesReference);
             XContentParser parser = xContentType.xContent().createParser(registry,
                 DeprecationHandler.THROW_UNSUPPORTED_OPERATION, stream)) {
            return responseParser.apply(parser);
        } catch (IOException ex) {
            throw new ClientException("Cannot parse response", ex);
        }
    }
}
