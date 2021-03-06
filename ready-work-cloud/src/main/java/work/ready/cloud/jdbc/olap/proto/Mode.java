/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package work.ready.cloud.jdbc.olap.proto;

import java.util.Locale;

public enum Mode {
    CLI,
    PLAIN,
    JDBC,
    ODBC;

    public static Mode fromString(String mode) {
        if (mode == null || mode.isEmpty()) {
            return PLAIN;
        }
        return Mode.valueOf(mode.toUpperCase(Locale.ROOT));
    }

    @Override
    public String toString() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public static boolean isDriver(Mode mode) {
        return mode == JDBC || mode == ODBC;
    }

    public static boolean isDedicatedClient(Mode mode) {
        return mode == JDBC || mode == ODBC || mode == CLI;
    }
}
