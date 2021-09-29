/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package work.ready.cloud.jdbc.olap;

import work.ready.cloud.jdbc.olap.proto.SqlVersion;

class InfoResponse {
    final String cluster;
    final SqlVersion version;

    InfoResponse(String clusterName, SqlVersion version) {
        this.cluster = clusterName;
        this.version = version;
    }

    @Override
    public String toString() {
        return cluster + "[" + version.toString() + "]";
    }
}
