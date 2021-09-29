/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package work.ready.cloud.jdbc.olap.proto;

import work.ready.cloud.jdbc.common.ParseField;
import work.ready.cloud.jdbc.common.xcontent.ConstructingObjectParser;
import work.ready.cloud.jdbc.common.xcontent.ObjectParser.ValueType;
import work.ready.cloud.jdbc.common.xcontent.XContentParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static work.ready.cloud.jdbc.olap.proto.Protocol.*;
import static work.ready.cloud.jdbc.common.xcontent.ConstructingObjectParser.constructorArg;
import static work.ready.cloud.jdbc.common.xcontent.ConstructingObjectParser.optionalConstructorArg;

public class SqlQueryResponse {

    @SuppressWarnings("unchecked")
    public static final ConstructingObjectParser<SqlQueryResponse, Void> PARSER = new ConstructingObjectParser<>("sql", true,
            objects -> new SqlQueryResponse(
                    objects[0] == null ? "" : (String) objects[0],
                    (List<ColumnInfo>) objects[1],
                    (List<List<Object>>) objects[2]));

    public static final ParseField CURSOR = new ParseField(CURSOR_NAME);
    public static final ParseField COLUMNS = new ParseField(COLUMNS_NAME);
    public static final ParseField ROWS = new ParseField(ROWS_NAME);

    static {
        PARSER.declareString(optionalConstructorArg(), CURSOR);
        PARSER.declareObjectArray(optionalConstructorArg(), (p, c) -> ColumnInfo.fromXContent(p), COLUMNS);
        PARSER.declareField(constructorArg(), (p, c) -> parseRows(p), ROWS, ValueType.OBJECT_ARRAY);
    }

    private final String cursor;
    private final List<ColumnInfo> columns;
    
    private final List<List<Object>> rows;

    public SqlQueryResponse(String cursor, List<ColumnInfo> columns, List<List<Object>> rows) {
        this.cursor = cursor;
        this.columns = columns;
        this.rows = rows;
    }

    public String cursor() {
        return cursor;
    }

    public long size() {
        return rows.size();
    }

    public List<ColumnInfo> columns() {
        return columns;
    }

    public List<List<Object>> rows() {
        return rows;
    }

    public static SqlQueryResponse fromXContent(XContentParser parser) {
        return PARSER.apply(parser, null);
    }

    public static List<List<Object>> parseRows(XContentParser parser) throws IOException {
        List<List<Object>> list = new ArrayList<>();
        while (parser.nextToken() != XContentParser.Token.END_ARRAY) {
            if (parser.currentToken() == XContentParser.Token.START_ARRAY) {
                list.add(parseRow(parser));
            } else {
                throw new IllegalStateException("expected start array but got [" + parser.currentToken() + "]");
            }
        }
        return list;
    }

    public static List<Object> parseRow(XContentParser parser) throws IOException {
        List<Object> list = new ArrayList<>();
        while (parser.nextToken() != XContentParser.Token.END_ARRAY) {
            if (parser.currentToken().isValue()) {
                list.add(ProtoUtils.parseFieldsValue(parser));
            } else if (parser.currentToken() == XContentParser.Token.VALUE_NULL) {
                list.add(null);
            } else {
                throw new IllegalStateException("expected value but got [" + parser.currentToken() + "]");
            }
        }
        return list;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SqlQueryResponse that = (SqlQueryResponse) o;
        return Objects.equals(cursor, that.cursor) &&
                Objects.equals(columns, that.columns) &&
                Objects.equals(rows, that.rows);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cursor, columns, rows);
    }

}
