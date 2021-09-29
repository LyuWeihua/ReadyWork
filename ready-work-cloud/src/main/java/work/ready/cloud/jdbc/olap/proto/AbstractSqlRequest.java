/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package work.ready.cloud.jdbc.olap.proto;

import work.ready.cloud.jdbc.common.xcontent.ToXContentFragment;

import java.util.Objects;

public abstract class AbstractSqlRequest implements ToXContentFragment {

    private final RequestInfo requestInfo;

    protected AbstractSqlRequest(RequestInfo requestInfo) {
        this.requestInfo = requestInfo;
    }

    public RequestInfo requestInfo() {
        return requestInfo;
    }

    public Mode mode() {
        return requestInfo.mode();
    }

    public String clientId() {
        return requestInfo.clientId();
    }

    public SqlVersion version() {
        return requestInfo.version();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractSqlRequest that = (AbstractSqlRequest) o;
        return Objects.equals(requestInfo, that.requestInfo);
    }

    @Override
    public int hashCode() {
        return requestInfo.hashCode();
    }

}
