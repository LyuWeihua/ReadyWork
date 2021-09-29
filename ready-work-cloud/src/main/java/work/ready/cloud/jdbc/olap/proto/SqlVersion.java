/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package work.ready.cloud.jdbc.olap.proto;

import work.ready.core.module.Version;

import java.security.InvalidParameterException;

public class SqlVersion extends Version {

    public static final SqlVersion V_1_0_0 = new SqlVersion(1, 1, 0, "201025", null);
    public static final SqlVersion CURRENT = V_1_0_0;

    public SqlVersion(int major, int minor, int revision, String buildDate, String apiVersion) {
        this(Integer.valueOf(major).byteValue(), Integer.valueOf(minor).byteValue(), Integer.valueOf(revision).byteValue(), buildDate, apiVersion);
        if (major > Byte.MAX_VALUE || minor > Byte.MAX_VALUE || revision > Byte.MAX_VALUE) {
            throw new InvalidParameterException("Invalid version initialisers [" + major + ", " + minor + ", " + revision + "]");
        }
    }

    public SqlVersion(byte major, byte minor, byte revision, String buildDate, String apiVersion) {
        super(major, minor, revision, buildDate, apiVersion, "work.ready", "ready-work-jdbc", "WeiHua Lyu", "jdbc driver for ready work framework, http://ready.work");
    }

    protected SqlVersion(byte... parts) {
        super(parts);
        this.groupId = "work.ready";
        this.artifactId = "ready-work-jdbc";
    }

    public static SqlVersion fromString(String version) {
        if (version == null || version.isEmpty()) {
            return null;
        }
        return new SqlVersion(from(version));
    }

    public static boolean hasVersionCompatibility(SqlVersion version) {
        return version.compareTo(CURRENT) >= 0;
    }

    public static boolean isClientCompatible(SqlVersion version) {
        return CURRENT.compareTo(version) <= 0;
    }
}
