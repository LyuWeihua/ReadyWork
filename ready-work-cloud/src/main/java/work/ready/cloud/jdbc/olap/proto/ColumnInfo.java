/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package work.ready.cloud.jdbc.olap.proto;

import work.ready.cloud.jdbc.common.ParseField;
import work.ready.cloud.jdbc.common.xcontent.ConstructingObjectParser;
import work.ready.cloud.jdbc.common.xcontent.ToXContentObject;
import work.ready.cloud.jdbc.common.xcontent.XContentBuilder;
import work.ready.cloud.jdbc.common.xcontent.XContentParser;

import java.io.IOException;
import java.util.Objects;

import static work.ready.cloud.jdbc.common.xcontent.ConstructingObjectParser.constructorArg;
import static work.ready.cloud.jdbc.common.xcontent.ConstructingObjectParser.optionalConstructorArg;

public class ColumnInfo implements ToXContentObject {

    private static final ConstructingObjectParser<ColumnInfo, Void> PARSER =
        new ConstructingObjectParser<>("column_info", true, objects ->
            new ColumnInfo(
                objects[0] == null ? "" : (String) objects[0],
                (String) objects[1],
                (String) objects[2],
                (Integer) objects[3]));

    private static final ParseField TABLE = new ParseField("table");
    private static final ParseField NAME = new ParseField("name");
    private static final ParseField ES_TYPE = new ParseField("type");
    private static final ParseField DISPLAY_SIZE = new ParseField("display_size");

    static {
        PARSER.declareString(optionalConstructorArg(), TABLE);
        PARSER.declareString(constructorArg(), NAME);
        PARSER.declareString(constructorArg(), ES_TYPE);
        PARSER.declareInt(optionalConstructorArg(), DISPLAY_SIZE);
    }

    private final String table;
    private final String name;
    private final String esType;
    private final Integer displaySize;

    public ColumnInfo(String table, String name, String esType, Integer displaySize) {
        this.table = table;
        this.name = name;
        this.esType = esType;
        this.displaySize = displaySize;
    }

    public ColumnInfo(String table, String name, String esType) {
        this.table = table;
        this.name = name;
        this.esType = esType;
        this.displaySize = null;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        if (table != null && table.isEmpty() == false) {
            builder.field("table", table);
        }
        builder.field("name", name);
        builder.field("type", esType);
        if (displaySize != null) {
            builder.field("display_size", displaySize);
        }
        return builder.endObject();
    }

    public static ColumnInfo fromXContent(XContentParser parser) {
        return PARSER.apply(parser, null);
    }

    public String table() {
        return table;
    }

    public String name() {
        return name;
    }

    public String esType() {
        return esType;
    }

    public int displaySize() {
        return displaySize == null ? 0 : displaySize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ColumnInfo that = (ColumnInfo) o;
        return Objects.equals(displaySize, that.displaySize) &&
            Objects.equals(table, that.table) &&
            Objects.equals(name, that.name) &&
            Objects.equals(esType, that.esType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(table, name, esType, displaySize);
    }

    @Override
    public String toString() {
        return ProtoUtils.toString(this);
    }
}