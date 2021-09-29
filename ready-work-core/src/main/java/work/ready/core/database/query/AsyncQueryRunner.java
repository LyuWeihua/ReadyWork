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

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class AsyncQueryRunner extends AbstractQueryRunner {

    private final ExecutorService executorService;
    private final QueryRunner queryRunner;

    public AsyncQueryRunner(final ExecutorService executorService, final QueryRunner queryRunner) {
        this.executorService = executorService;
        this.queryRunner = queryRunner;
    }

    public AsyncQueryRunner(final ExecutorService executorService) {
        this(null, false, executorService);
    }

    @Deprecated
    public AsyncQueryRunner(final boolean pmdKnownBroken, final ExecutorService executorService) {
        this(null, pmdKnownBroken, executorService);
    }

    @Deprecated
    public AsyncQueryRunner(final DataSource ds, final ExecutorService executorService) {
        this(ds, false, executorService);
    }

    @Deprecated
    public AsyncQueryRunner(final DataSource ds, final boolean pmdKnownBroken, final ExecutorService executorService) {
        super(ds, pmdKnownBroken);
        this.executorService = executorService;
        this.queryRunner = new QueryRunner(ds, pmdKnownBroken);
    }

    @Deprecated
    protected class BatchCallableStatement implements Callable<int[]> {
        private final String sql;
        private final Object[][] params;
        private final Connection conn;
        private final boolean closeConn;
        private final PreparedStatement ps;

        public BatchCallableStatement(final String sql, final Object[][] params, final Connection conn, final boolean closeConn, final PreparedStatement ps) {
            this.sql = sql;
            this.params = params.clone();
            this.conn = conn;
            this.closeConn = closeConn;
            this.ps = ps;
        }

        @Override
        public int[] call() throws SQLException {
            int[] ret = null;

            try {
                ret = ps.executeBatch();
            } catch (final SQLException e) {
                rethrow(e, sql, (Object[])params);
            } finally {
                close(ps);
                if (closeConn) {
                    close(conn);
                }
            }

            return ret;
        }
    }

    public Future<int[]> batch(final Connection conn, final String sql, final Object[][] params) throws SQLException {
        return executorService.submit(new Callable<int[]>() {

            @Override
            public int[] call() throws Exception {
                return queryRunner.batch(conn, sql, params);
            }

        });
    }

    public Future<int[]> batch(final String sql, final Object[][] params) throws SQLException {
        return executorService.submit(new Callable<int[]>() {

            @Override
            public int[] call() throws Exception {
                return queryRunner.batch(sql, params);
            }

        });
    }

    protected class QueryCallableStatement<T> implements Callable<T> {
        private final String sql;
        private final Object[] params;
        private final Connection conn;
        private final boolean closeConn;
        private final PreparedStatement ps;
        private final ResultSetHandler<T> rsh;

        public QueryCallableStatement(final Connection conn, final boolean closeConn, final PreparedStatement ps,
                final ResultSetHandler<T> rsh, final String sql, final Object... params) {
            this.sql = sql;
            this.params = params;
            this.conn = conn;
            this.closeConn = closeConn;
            this.ps = ps;
            this.rsh = rsh;
        }

        @Override
        public T call() throws SQLException {
            ResultSet rs = null;
            T ret = null;

            try {
                rs = wrap(ps.executeQuery());
                ret = rsh.handle(rs);
            } catch (final SQLException e) {
                rethrow(e, sql, params);
            } finally {
                try {
                    close(rs);
                } finally {
                    close(ps);
                    if (closeConn) {
                        close(conn);
                    }
                }
            }

            return ret;
        }

    }

    public <T> Future<T> query(final Connection conn, final String sql, final ResultSetHandler<T> rsh, final Object... params)
            throws SQLException {
        return executorService.submit(new Callable<T>() {

            @Override
            public T call() throws Exception {
                return queryRunner.query(conn, sql, rsh, params);
            }

        });
    }

    public <T> Future<T> query(final Connection conn, final String sql, final ResultSetHandler<T> rsh) throws SQLException {
        return executorService.submit(new Callable<T>() {

            @Override
            public T call() throws Exception {
                return queryRunner.query(conn, sql, rsh);
            }

        });
    }

    public <T> Future<T> query(final String sql, final ResultSetHandler<T> rsh, final Object... params) throws SQLException {
        return executorService.submit(new Callable<T>() {

            @Override
            public T call() throws Exception {
                return queryRunner.query(sql, rsh, params);
            }

        });
    }

    public <T> Future<T> query(final String sql, final ResultSetHandler<T> rsh) throws SQLException {
        return executorService.submit(new Callable<T>() {

            @Override
            public T call() throws Exception {
                return queryRunner.query(sql, rsh);
            }

        });
    }

    @Deprecated
    protected class UpdateCallableStatement implements Callable<Integer> {
        private final String sql;
        private final Object[] params;
        private final Connection conn;
        private final boolean closeConn;
        private final PreparedStatement ps;

        public UpdateCallableStatement(final Connection conn, final boolean closeConn, final PreparedStatement ps, final String sql, final Object... params) {
            this.sql = sql;
            this.params = params;
            this.conn = conn;
            this.closeConn = closeConn;
            this.ps = ps;
        }

        @Override
        public Integer call() throws SQLException {
            int rows = 0;

            try {
                rows = ps.executeUpdate();
            } catch (final SQLException e) {
                rethrow(e, sql, params);
            } finally {
                close(ps);
                if (closeConn) {
                    close(conn);
                }
            }

            return Integer.valueOf(rows);
        }

    }

    public Future<Integer> update(final Connection conn, final String sql) throws SQLException {
        return executorService.submit(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                return Integer.valueOf(queryRunner.update(conn, sql));
            }

        });
    }

    public Future<Integer> update(final Connection conn, final String sql, final Object param) throws SQLException {
        return executorService.submit(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                return Integer.valueOf(queryRunner.update(conn, sql, param));
            }

        });
    }

    public Future<Integer> update(final Connection conn, final String sql, final Object... params) throws SQLException {
        return executorService.submit(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                return Integer.valueOf(queryRunner.update(conn, sql, params));
            }

        });
    }

    public Future<Integer> update(final String sql) throws SQLException {
        return executorService.submit(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                return Integer.valueOf(queryRunner.update(sql));
            }

        });
    }

    public Future<Integer> update(final String sql, final Object param) throws SQLException {
        return executorService.submit(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                return Integer.valueOf(queryRunner.update(sql, param));
            }

        });
    }

    public Future<Integer> update(final String sql, final Object... params) throws SQLException {
        return executorService.submit(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                return Integer.valueOf(queryRunner.update(sql, params));
            }

        });
    }

    public <T> Future<T> insert(final String sql, final ResultSetHandler<T> rsh) throws SQLException {
        return executorService.submit(new Callable<T>() {

            @Override
            public T call() throws Exception {
                return queryRunner.insert(sql, rsh);
            }

        });
    }

    public <T> Future<T> insert(final String sql, final ResultSetHandler<T> rsh, final Object... params) throws SQLException {
        return executorService.submit(new Callable<T>() {

            @Override
            public T call() throws Exception {
                return queryRunner.insert(sql, rsh, params);
            }
        });
    }

    public <T> Future<T> insert(final Connection conn, final String sql, final ResultSetHandler<T> rsh) throws SQLException {
        return executorService.submit(new Callable<T>() {

            @Override
            public T call() throws Exception {
                return queryRunner.insert(conn, sql, rsh);
            }
        });
    }

    public <T> Future<T> insert(final Connection conn, final String sql, final ResultSetHandler<T> rsh, final Object... params) throws SQLException {
        return executorService.submit(new Callable<T>() {

            @Override
            public T call() throws Exception {
                return queryRunner.insert(conn, sql, rsh, params);
            }
        });
    }

    public <T> Future<T> insertBatch(final String sql, final ResultSetHandler<T> rsh, final Object[][] params) throws SQLException {
        return executorService.submit(new Callable<T>() {

            @Override
            public T call() throws Exception {
                return queryRunner.insertBatch(sql, rsh, params);
            }
        });
    }

    public <T> Future<T> insertBatch(final Connection conn, final String sql, final ResultSetHandler<T> rsh, final Object[][] params) throws SQLException {
        return executorService.submit(new Callable<T>() {

            @Override
            public T call() throws Exception {
                return queryRunner.insertBatch(conn, sql, rsh, params);
            }
        });
    }

}
