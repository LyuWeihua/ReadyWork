/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package work.ready.core.database.query;

public class StatementConfiguration {
    private final Integer fetchDirection;
    private final Integer fetchSize;
    private final Integer maxFieldSize;
    private final Integer maxRows;
    private final Integer queryTimeout;

    public StatementConfiguration(final Integer fetchDirection, final Integer fetchSize,
                                  final Integer maxFieldSize, final Integer maxRows,
                                  final Integer queryTimeout) {
        this.fetchDirection = fetchDirection;
        this.fetchSize = fetchSize;
        this.maxFieldSize = maxFieldSize;
        this.maxRows = maxRows;
        this.queryTimeout = queryTimeout;
    }

    public Integer getFetchDirection() {
        return fetchDirection;
    }

    public boolean isFetchDirectionSet() {
        return fetchDirection != null;
    }

    public Integer getFetchSize() {
        return fetchSize;
    }

    public boolean isFetchSizeSet() {
        return fetchSize != null;
    }

    public Integer getMaxFieldSize() {
        return maxFieldSize;
    }

    public boolean isMaxFieldSizeSet() {
        return maxFieldSize != null;
    }

    public Integer getMaxRows() {
        return maxRows;
    }

    public boolean isMaxRowsSet() {
        return maxRows != null;
    }

    public Integer getQueryTimeout() {
        return queryTimeout;
    }

    public boolean isQueryTimeoutSet() {
        return queryTimeout != null;
    }

    public static final class Builder {
        private Integer fetchDirection;
        private Integer fetchSize;
        private Integer maxRows;
        private Integer queryTimeout;
        private Integer maxFieldSize;

        public Builder fetchDirection(final Integer fetchDirection) {
            this.fetchDirection = fetchDirection;
            return this;
        }

        public Builder fetchSize(final Integer fetchSize) {
            this.fetchSize = fetchSize;
            return this;
        }

        public Builder maxRows(final Integer maxRows) {
            this.maxRows = maxRows;
            return this;
        }

        public Builder queryTimeout(final Integer queryTimeout) {
            this.queryTimeout = queryTimeout;
            return this;
        }

        public Builder maxFieldSize(final Integer maxFieldSize) {
            this.maxFieldSize = maxFieldSize;
            return this;
        }

        public StatementConfiguration build() {
            return new StatementConfiguration(fetchDirection, fetchSize, maxFieldSize, maxRows, queryTimeout);
        }
    }
}
