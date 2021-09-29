/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package work.ready.cloud.jdbc.olap.proto;

import work.ready.cloud.jdbc.common.ParseField;
import work.ready.cloud.jdbc.common.xcontent.ConstructingObjectParser;
import work.ready.cloud.jdbc.common.xcontent.XContentParser;

import java.util.Objects;

import static work.ready.cloud.jdbc.common.xcontent.ConstructingObjectParser.optionalConstructorArg;

public class SqlClearCursorResponse {

    public static final ParseField SUCCEEDED = new ParseField("succeeded");
    public static final ConstructingObjectParser<SqlClearCursorResponse, Void> PARSER =
        new ConstructingObjectParser<>(SqlClearCursorResponse.class.getName(), true,
            objects -> new SqlClearCursorResponse(objects[0] == null ? false : (boolean) objects[0]));

    static {
        PARSER.declareBoolean(optionalConstructorArg(), SUCCEEDED);
    }

    private final boolean succeeded;

    public SqlClearCursorResponse(boolean succeeded) {
        this.succeeded = succeeded;
    }

    public boolean isSucceeded() {
        return succeeded;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SqlClearCursorResponse response = (SqlClearCursorResponse) o;
        return succeeded == response.succeeded;
    }

    @Override
    public int hashCode() {
        return Objects.hash(succeeded);
    }

    public static SqlClearCursorResponse fromXContent(XContentParser parser) {
        return PARSER.apply(parser, null);
    }

}
