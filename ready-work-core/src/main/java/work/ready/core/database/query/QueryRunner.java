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
import java.sql.*;
import java.util.LinkedList;
import java.util.List;

public class QueryRunner extends AbstractQueryRunner {

    public QueryRunner() {
    }

    public QueryRunner(final boolean pmdKnownBroken) {
        super(pmdKnownBroken);
    }

    public QueryRunner(final DataSource ds) {
        super(ds);
    }

    public QueryRunner(final StatementConfiguration stmtConfig) {
        super(stmtConfig);
    }

    public QueryRunner(final DataSource ds, final boolean pmdKnownBroken) {
        super(ds, pmdKnownBroken);
    }

    public QueryRunner(final DataSource ds, final StatementConfiguration stmtConfig) {
        super(ds, stmtConfig);
    }

    public QueryRunner(final DataSource ds, final boolean pmdKnownBroken, final StatementConfiguration stmtConfig) {
        super(ds, pmdKnownBroken, stmtConfig);
    }

    public int[] batch(final Connection conn, final String sql, final Object[][] params) throws SQLException {
        return this.batch(conn, false, sql, params);
    }

    public int[] batch(final String sql, final Object[][] params) throws SQLException {
        final Connection conn = this.prepareConnection();

        return this.batch(conn, true, sql, params);
    }

    private int[] batch(final Connection conn, final boolean closeConn, final String sql, final Object[][] params) throws SQLException {
        if (conn == null) {
            throw new SQLException("Null connection");
        }

        if (sql == null) {
            if (closeConn) {
                close(conn);
            }
            throw new SQLException("Null SQL statement");
        }

        if (params == null) {
            if (closeConn) {
                close(conn);
            }
            throw new SQLException("Null parameters. If parameters aren't need, pass an empty array.");
        }

        PreparedStatement stmt = null;
        int[] rows = null;
        try {
            stmt = this.prepareStatement(conn, sql);

            for (final Object[] param : params) {
                this.fillStatement(stmt, param);
                stmt.addBatch();
            }
            rows = stmt.executeBatch();

        } catch (final SQLException e) {
            this.rethrow(e, sql, (Object[])params);
        } finally {
            close(stmt);
            if (closeConn) {
                close(conn);
            }
        }

        return rows;
    }

    @Deprecated
    public <T> T query(final Connection conn, final String sql, final Object param, final ResultSetHandler<T> rsh) throws SQLException {
        return this.<T>query(conn, false, sql, rsh, param);
    }

    @Deprecated
    public <T> T query(final Connection conn, final String sql, final Object[] params, final ResultSetHandler<T> rsh) throws SQLException {
        return this.<T>query(conn, false, sql, rsh, params);
    }

    public <T> T query(final Connection conn, final String sql, final ResultSetHandler<T> rsh, final Object... params) throws SQLException {
        return this.<T>query(conn, false, sql, rsh, params);
    }

    public <T> T query(final Connection conn, final String sql, final ResultSetHandler<T> rsh) throws SQLException {
        return this.<T>query(conn, false, sql, rsh, (Object[]) null);
    }

    @Deprecated
    public <T> T query(final String sql, final Object param, final ResultSetHandler<T> rsh) throws SQLException {
        final Connection conn = this.prepareConnection();

        return this.<T>query(conn, true, sql, rsh, param);
    }

    @Deprecated
    public <T> T query(final String sql, final Object[] params, final ResultSetHandler<T> rsh) throws SQLException {
        final Connection conn = this.prepareConnection();

        return this.<T>query(conn, true, sql, rsh, params);
    }

    public <T> T query(final String sql, final ResultSetHandler<T> rsh, final Object... params) throws SQLException {
        final Connection conn = this.prepareConnection();

        return this.<T>query(conn, true, sql, rsh, params);
    }

    public <T> T query(final String sql, final ResultSetHandler<T> rsh) throws SQLException {
        final Connection conn = this.prepareConnection();

        return this.<T>query(conn, true, sql, rsh, (Object[]) null);
    }

    private <T> T query(final Connection conn, final boolean closeConn, final String sql, final ResultSetHandler<T> rsh, final Object... params)
            throws SQLException {
        if (conn == null) {
            throw new SQLException("Null connection");
        }

        if (sql == null) {
            if (closeConn) {
                close(conn);
            }
            throw new SQLException("Null SQL statement");
        }

        if (rsh == null) {
            if (closeConn) {
                close(conn);
            }
            throw new SQLException("Null ResultSetHandler");
        }

        Statement stmt = null;
        ResultSet rs = null;
        T result = null;

        try {
            if (params != null && params.length > 0) {
                final PreparedStatement ps = this.prepareStatement(conn, sql);
                stmt = ps;
                this.fillStatement(ps, params);
                rs = this.wrap(ps.executeQuery());
            } else {
                stmt = conn.createStatement();
                rs = this.wrap(stmt.executeQuery(sql));
            }
            result = rsh.handle(rs);

        } catch (final SQLException e) {
            this.rethrow(e, sql, params);

        } finally {
            closeQuietly(rs);
            closeQuietly(stmt);
            if (closeConn) {
                close(conn);
            }
        }

        return result;
    }

    public int update(final Connection conn, final String sql) throws SQLException {
        return this.update(conn, false, sql, (Object[]) null);
    }

    public int update(final Connection conn, final String sql, final Object param) throws SQLException {
        return this.update(conn, false, sql, param);
    }

    public int update(final Connection conn, final String sql, final Object... params) throws SQLException {
        return update(conn, false, sql, params);
    }

    public int update(final String sql) throws SQLException {
        final Connection conn = this.prepareConnection();

        return this.update(conn, true, sql, (Object[]) null);
    }

    public int update(final String sql, final Object param) throws SQLException {
        final Connection conn = this.prepareConnection();

        return this.update(conn, true, sql, param);
    }

    public int update(final String sql, final Object... params) throws SQLException {
        final Connection conn = this.prepareConnection();

        return this.update(conn, true, sql, params);
    }

    private int update(final Connection conn, final boolean closeConn, final String sql, final Object... params) throws SQLException {
        if (conn == null) {
            throw new SQLException("Null connection");
        }

        if (sql == null) {
            if (closeConn) {
                close(conn);
            }
            throw new SQLException("Null SQL statement");
        }

        Statement stmt = null;
        int rows = 0;

        try {
            if (params != null && params.length > 0) {
                final PreparedStatement ps = this.prepareStatement(conn, sql);
                stmt = ps;
                this.fillStatement(ps, params);
                rows = ps.executeUpdate();
            } else {
                stmt = conn.createStatement();
                rows = stmt.executeUpdate(sql);
            }

        } catch (final SQLException e) {
            this.rethrow(e, sql, params);

        } finally {
            close(stmt);
            if (closeConn) {
                close(conn);
            }
        }

        return rows;
    }

    public <T> T insert(final String sql, final ResultSetHandler<T> rsh) throws SQLException {
        return insert(this.prepareConnection(), true, sql, rsh, (Object[]) null);
    }

    public <T> T insert(final String sql, final ResultSetHandler<T> rsh, final Object... params) throws SQLException {
        return insert(this.prepareConnection(), true, sql, rsh, params);
    }

    public <T> T insert(final Connection conn, final String sql, final ResultSetHandler<T> rsh) throws SQLException {
        return insert(conn, false, sql, rsh, (Object[]) null);
    }

    public <T> T insert(final Connection conn, final String sql, final ResultSetHandler<T> rsh, final Object... params) throws SQLException {
        return insert(conn, false, sql, rsh, params);
    }

    private <T> T insert(final Connection conn, final boolean closeConn, final String sql, final ResultSetHandler<T> rsh, final Object... params)
            throws SQLException {
        if (conn == null) {
            throw new SQLException("Null connection");
        }

        if (sql == null) {
            if (closeConn) {
                close(conn);
            }
            throw new SQLException("Null SQL statement");
        }

        if (rsh == null) {
            if (closeConn) {
                close(conn);
            }
            throw new SQLException("Null ResultSetHandler");
        }

        Statement stmt = null;
        T generatedKeys = null;

        try {
            if (params != null && params.length > 0) {
                final PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                stmt = ps;
                this.fillStatement(ps, params);
                ps.executeUpdate();
            } else {
                stmt = conn.createStatement();
                stmt.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
            }
            final ResultSet resultSet = stmt.getGeneratedKeys();
            generatedKeys = rsh.handle(resultSet);
        } catch (final SQLException e) {
            this.rethrow(e, sql, params);
        } finally {
            close(stmt);
            if (closeConn) {
                close(conn);
            }
        }

        return generatedKeys;
    }

    public <T> T insertBatch(final String sql, final ResultSetHandler<T> rsh, final Object[][] params) throws SQLException {
        return insertBatch(this.prepareConnection(), true, sql, rsh, params);
    }

    public <T> T insertBatch(final Connection conn, final String sql, final ResultSetHandler<T> rsh, final Object[][] params) throws SQLException {
        return insertBatch(conn, false, sql, rsh, params);
    }

    private <T> T insertBatch(final Connection conn, final boolean closeConn, final String sql, final ResultSetHandler<T> rsh, final Object[][] params)
            throws SQLException {
        if (conn == null) {
            throw new SQLException("Null connection");
        }

        if (sql == null) {
            if (closeConn) {
                close(conn);
            }
            throw new SQLException("Null SQL statement");
        }

        if (params == null) {
            if (closeConn) {
                close(conn);
            }
            throw new SQLException("Null parameters. If parameters aren't need, pass an empty array.");
        }

        PreparedStatement stmt = null;
        T generatedKeys = null;
        try {
            stmt = this.prepareStatement(conn, sql, Statement.RETURN_GENERATED_KEYS);

            for (final Object[] param : params) {
                this.fillStatement(stmt, param);
                stmt.addBatch();
            }
            stmt.executeBatch();
            final ResultSet rs = stmt.getGeneratedKeys();
            generatedKeys = rsh.handle(rs);

        } catch (final SQLException e) {
            this.rethrow(e, sql, (Object[])params);
        } finally {
            close(stmt);
            if (closeConn) {
                close(conn);
            }
        }

        return generatedKeys;
    }

    public int execute(final Connection conn, final String sql, final Object... params) throws SQLException {
        return this.execute(conn, false, sql, params);
    }

    public int execute(final String sql, final Object... params) throws SQLException {
        final Connection conn = this.prepareConnection();

        return this.execute(conn, true, sql, params);
    }

    public <T> List<T> execute(final Connection conn, final String sql, final ResultSetHandler<T> rsh, final Object... params) throws SQLException {
        return this.execute(conn, false, sql, rsh, params);
    }

    public <T> List<T> execute(final String sql, final ResultSetHandler<T> rsh, final Object... params) throws SQLException {
        final Connection conn = this.prepareConnection();

        return this.execute(conn, true, sql, rsh, params);
    }

    private int execute(final Connection conn, final boolean closeConn, final String sql, final Object... params) throws SQLException {
        if (conn == null) {
            throw new SQLException("Null connection");
        }

        if (sql == null) {
            if (closeConn) {
                close(conn);
            }
            throw new SQLException("Null SQL statement");
        }

        CallableStatement stmt = null;
        int rows = 0;

        try {
            stmt = this.prepareCall(conn, sql);
            this.fillStatement(stmt, params);
            stmt.execute();
            rows = stmt.getUpdateCount();
            this.retrieveOutParameters(stmt, params);

        } catch (final SQLException e) {
            this.rethrow(e, sql, params);

        } finally {
            close(stmt);
            if (closeConn) {
                close(conn);
            }
        }

        return rows;
    }

    private <T> List<T> execute(final Connection conn, final boolean closeConn, final String sql, final ResultSetHandler<T> rsh, final Object... params) throws SQLException {
        if (conn == null) {
            throw new SQLException("Null connection");
        }

        if (sql == null) {
            if (closeConn) {
                close(conn);
            }
            throw new SQLException("Null SQL statement");
        }

        if (rsh == null) {
            if (closeConn) {
                close(conn);
            }
            throw new SQLException("Null ResultSetHandler");
        }

        CallableStatement stmt = null;
        final List<T> results = new LinkedList<>();

        try {
            stmt = this.prepareCall(conn, sql);
            this.fillStatement(stmt, params);
            boolean moreResultSets = stmt.execute();

            ResultSet rs = null;
            while (moreResultSets) {
                try {
                    rs = this.wrap(stmt.getResultSet());
                    results.add(rsh.handle(rs));
                    moreResultSets = stmt.getMoreResults();

                } finally {
                    close(rs);
                }
            }
            this.retrieveOutParameters(stmt, params);

        } catch (final SQLException e) {
            this.rethrow(e, sql, params);

        } finally {
            close(stmt);
            if (closeConn) {
                close(conn);
            }
        }

        return results;
    }

    private void retrieveOutParameters(final CallableStatement stmt, final Object[] params) throws SQLException {
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof OutParameter) {
                    ((OutParameter)params[i]).setValue(stmt, i + 1);
                }
            }
        }
    }
}
