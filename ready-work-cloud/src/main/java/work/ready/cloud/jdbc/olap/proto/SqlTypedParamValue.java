/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package work.ready.cloud.jdbc.olap.proto;

import work.ready.cloud.jdbc.common.ParseField;
import work.ready.cloud.jdbc.common.xcontent.*;

import java.io.IOException;
import java.util.Objects;

import static work.ready.cloud.jdbc.olap.proto.ProtoUtils.parseFieldsValue;
import static work.ready.cloud.jdbc.common.xcontent.ConstructingObjectParser.constructorArg;

public class SqlTypedParamValue implements ToXContentObject {
    private static final ConstructingObjectParser<SqlTypedParamValue, Void> PARSER =
            new ConstructingObjectParser<>("params", true, objects ->
            new SqlTypedParamValue((String) objects[1], objects[0]
                    ));

    private static final ParseField VALUE = new ParseField("value");
    private static final ParseField TYPE = new ParseField("type");

    static {
        PARSER.declareField(constructorArg(), (p, c) -> parseFieldsValue(p), VALUE, ObjectParser.ValueType.VALUE);
        PARSER.declareString(constructorArg(), TYPE);
    }

    public final Object value;
    public final String type;
    private boolean hasExplicitType;        
    private XContentLocation tokenLocation; 

    public SqlTypedParamValue(String type, Object value) {
        this(type, value, true);
    }

    public SqlTypedParamValue(String type, Object value, boolean hasExplicitType) {
        this.value = value;
        this.type = type;
        this.hasExplicitType = hasExplicitType;
    }

    public boolean hasExplicitType() {
        return hasExplicitType;
    }

    public void hasExplicitType(boolean hasExplicitType) {
        this.hasExplicitType = hasExplicitType;
    }

    public XContentLocation tokenLocation() {
        return tokenLocation;
    }

    public void tokenLocation(XContentLocation tokenLocation) {
        this.tokenLocation = tokenLocation;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.value(value);
        return builder;
    }

    public static SqlTypedParamValue fromXContent(XContentParser parser) {
        return PARSER.apply(parser, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SqlTypedParamValue that = (SqlTypedParamValue) o;
        return Objects.equals(value, that.value)
                && Objects.equals(type, that.type)
                && Objects.equals(hasExplicitType, that.hasExplicitType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, type, hasExplicitType);
    }

    @Override
    public String toString() {
        return String.valueOf(value) + " [" + type + "][" + hasExplicitType + "][" + tokenLocation + "]";
    }
}
