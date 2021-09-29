/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package work.ready.cloud.jdbc.common;

import work.ready.cloud.jdbc.common.xcontent.DeprecationHandler;
import work.ready.cloud.jdbc.common.xcontent.XContentLocation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

public class ParseField {
    private final String name;
    private final String[] deprecatedNames;
    private String allReplacedWith = null;
    private final String[] allNames;
    private boolean fullyDeprecated = false;

    private static final String[] EMPTY = new String[0];

    public ParseField(String name, String... deprecatedNames) {
        this.name = name;
        if (deprecatedNames == null || deprecatedNames.length == 0) {
            this.deprecatedNames = EMPTY;
        } else {
            final HashSet<String> set = new HashSet<>();
            Collections.addAll(set, deprecatedNames);
            this.deprecatedNames = set.toArray(new String[set.size()]);
        }
        Set<String> allNames = new HashSet<>();
        allNames.add(name);
        Collections.addAll(allNames, this.deprecatedNames);
        this.allNames = allNames.toArray(new String[allNames.size()]);
    }

    public String getPreferredName() {
        return name;
    }

    public String[] getAllNamesIncludedDeprecated() {
        return allNames;
    }

    public ParseField withDeprecation(String... deprecatedNames) {
        return new ParseField(this.name, deprecatedNames);
    }

    public ParseField withAllDeprecated(String allReplacedWith) {
        ParseField parseField = this.withDeprecation(getAllNamesIncludedDeprecated());
        parseField.allReplacedWith = allReplacedWith;
        return parseField;
    }

    public ParseField withAllDeprecated() {
        ParseField parseField = this.withDeprecation(getAllNamesIncludedDeprecated());
        parseField.fullyDeprecated = true;
        return parseField;
    }

    public boolean match(String fieldName, DeprecationHandler deprecationHandler) {
        return match(null, () -> XContentLocation.UNKNOWN, fieldName, deprecationHandler);
    }

    public boolean match(String parserName, Supplier<XContentLocation> location, String fieldName, DeprecationHandler deprecationHandler) {
        Objects.requireNonNull(fieldName, "fieldName cannot be null");

        if (fullyDeprecated == false && allReplacedWith == null && fieldName.equals(name)) {
            return true;
        }

        for (String depName : deprecatedNames) {
            if (fieldName.equals(depName)) {
                if (fullyDeprecated) {
                    deprecationHandler.usedDeprecatedField(parserName, location, fieldName);
                } else if (allReplacedWith == null) {
                    deprecationHandler.usedDeprecatedName(parserName, location, fieldName, name);
                } else {
                    deprecationHandler.usedDeprecatedField(parserName, location, fieldName, allReplacedWith);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return getPreferredName();
    }

    public String getAllReplacedWith() {
        return allReplacedWith;
    }

    public String[] getDeprecatedNames() {
        return deprecatedNames;
    }

    public static class CommonFields {
        public static final ParseField FIELD = new ParseField("field");
        public static final ParseField FIELDS = new ParseField("fields");
        public static final ParseField FORMAT = new ParseField("format");
        public static final ParseField MISSING = new ParseField("missing");
        public static final ParseField TIME_ZONE = new ParseField("time_zone");
    }
}
