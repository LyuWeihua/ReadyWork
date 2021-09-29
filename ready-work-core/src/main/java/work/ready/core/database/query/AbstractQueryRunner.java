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

import work.ready.core.database.DatabaseManager;
import work.ready.core.server.Ready;

import javax.sql.DataSource;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.Arrays;

public abstract class AbstractQueryRunner {
    
    private volatile boolean pmdKnownBroken = false;

    @Deprecated
    protected final DataSource ds;
    protected final DatabaseManager manager;
    
    private final StatementConfiguration stmtConfig;

    public AbstractQueryRunner() {
        this.ds = null;
        this.manager = Ready.dbManager();
        this.stmtConfig = null;
    }

    public AbstractQueryRunner(final boolean pmdKnownBroken) {
        this.pmdKnownBroken = pmdKnownBroken;
        this.ds = null;
        this.manager = Ready.dbManager();
        this.stmtConfig = null;
    }

    public AbstractQueryRunner(final DataSource ds) {
        this.ds = ds;
        this.manager = Ready.dbManager();
        this.stmtConfig = null;
    }

    public AbstractQueryRunner(final DataSource ds, final boolean pmdKnownBroken) {
        this.pmdKnownBroken = pmdKnownBroken;
        this.ds = ds;
        this.manager = Ready.dbManager();
        this.stmtConfig = null;
    }

    public AbstractQueryRunner(final DataSource ds, final boolean pmdKnownBroken, final StatementConfiguration stmtConfig) {
        this.pmdKnownBroken = pmdKnownBroken;
        this.ds = ds;
        this.manager = Ready.dbManager();
        this.stmtConfig = stmtConfig;
    }

    public AbstractQueryRunner(final DataSource ds, final StatementConfiguration stmtConfig) {
        this.ds = ds;
        this.manager = Ready.dbManager();
        this.stmtConfig = stmtConfig;
    }

    public AbstractQueryRunner(final StatementConfiguration stmtConfig) {
        this.ds = null;
        this.manager = Ready.dbManager();
        this.stmtConfig = stmtConfig;
    }

    public void close(final Connection conn) throws SQLException {
        manager.close(conn);
    }

    public void close(final ResultSet rs) throws SQLException {
        manager.close(rs);
    }

    public void close(final Statement stmt) throws SQLException {
        manager.close(stmt);
    }

    public void closeQuietly(final Connection conn) {
        manager.closeQuietly(conn);
    }

    public void closeQuietly(final ResultSet rs) {
        manager.closeQuietly(rs);
    }

    public void closeQuietly(final Statement statement) {
        manager.closeQuietly(statement);
    }

    private void configureStatement(final Statement stmt) throws SQLException {

        if (stmtConfig != null) {
            if (stmtConfig.isFetchDirectionSet()) {
                stmt.setFetchDirection(stmtConfig.getFetchDirection());
            }

            if (stmtConfig.isFetchSizeSet()) {
                stmt.setFetchSize(stmtConfig.getFetchSize());
            }

            if (stmtConfig.isMaxFieldSizeSet()) {
                stmt.setMaxFieldSize(stmtConfig.getMaxFieldSize());
            }

            if (stmtConfig.isMaxRowsSet()) {
                stmt.setMaxRows(stmtConfig.getMaxRows());
            }

            if (stmtConfig.isQueryTimeoutSet()) {
                stmt.setQueryTimeout(stmtConfig.getQueryTimeout());
            }
        }
    }

    public void fillStatement(final PreparedStatement stmt, final Object... params)
            throws SQLException {

        ParameterMetaData pmd = null;
        if (!pmdKnownBroken) {
            try {
                pmd = stmt.getParameterMetaData();
                if (pmd == null) { 
                    pmdKnownBroken = true;
                } else {
                    final int stmtCount = pmd.getParameterCount();
                    final int paramsCount = params == null ? 0 : params.length;

                    if (stmtCount != paramsCount) {
                        throw new SQLException("Wrong number of parameters: expected "
                                + stmtCount + ", was given " + paramsCount);
                    }
                }
            } catch (final SQLFeatureNotSupportedException ex) {
                pmdKnownBroken = true;
            }
            
        }

        if (params == null) {
            return;
        }

        CallableStatement call = null;
        if (stmt instanceof CallableStatement) {
            call = (CallableStatement) stmt;
        }

        for (int i = 0; i < params.length; i++) {
            if (params[i] != null) {
                if (call != null && params[i] instanceof OutParameter) {
                    ((OutParameter)params[i]).register(call, i + 1);
                } else {
                    stmt.setObject(i + 1, params[i]);
                }
            } else {

                int sqlType = Types.VARCHAR;
                if (!pmdKnownBroken) {
                    
                    try {
                        
                        sqlType = pmd.getParameterType(i + 1);
                    } catch (final SQLException e) {
                        pmdKnownBroken = true;
                    }
                }
                stmt.setNull(i + 1, sqlType);
            }
        }
    }

    public void fillStatementWithBean(final PreparedStatement stmt, final Object bean,
            final PropertyDescriptor[] properties) throws SQLException {
        final Object[] params = new Object[properties.length];
        for (int i = 0; i < properties.length; i++) {
            final PropertyDescriptor property = properties[i];
            Object value = null;
            final Method method = property.getReadMethod();
            if (method == null) {
                throw new RuntimeException("No read method for bean property "
                        + bean.getClass() + " " + property.getName());
            }
            try {
                value = method.invoke(bean);
            } catch (final IllegalArgumentException e) {
                throw new RuntimeException(
                        "Couldn't invoke method with 0 arguments: " + method, e);
            } catch (final InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException("Couldn't invoke method: " + method,
                        e);
            }
            params[i] = value;
        }
        fillStatement(stmt, params);
    }

    public void fillStatementWithBean(final PreparedStatement stmt, final Object bean,
            final String... propertyNames) throws SQLException {
        PropertyDescriptor[] descriptors;
        try {
            descriptors = Introspector.getBeanInfo(bean.getClass())
                    .getPropertyDescriptors();
        } catch (final IntrospectionException e) {
            throw new RuntimeException("Couldn't introspect bean "
                    + bean.getClass().toString(), e);
        }
        final PropertyDescriptor[] sorted = new PropertyDescriptor[propertyNames.length];
        for (int i = 0; i < propertyNames.length; i++) {
            final String propertyName = propertyNames[i];
            if (propertyName == null) {
                throw new NullPointerException("propertyName can't be null: "
                        + i);
            }
            boolean found = false;
            for (final PropertyDescriptor descriptor : descriptors) {
                if (propertyName.equals(descriptor.getName())) {
                    sorted[i] = descriptor;
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new RuntimeException("Couldn't find bean property: "
                        + bean.getClass() + " " + propertyName);
            }
        }
        fillStatementWithBean(stmt, bean, sorted);
    }

    public DataSource getDataSource() {
        return this.ds;
    }

    public boolean isPmdKnownBroken() {
        return pmdKnownBroken;
    }

    protected CallableStatement prepareCall(final Connection conn, final String sql)
            throws SQLException {

        return conn.prepareCall(sql);
    }

    protected Connection prepareConnection() throws SQLException {
        if (this.getDataSource() == null) {
            throw new SQLException(
                    "QueryRunner requires a DataSource to be "
                            + "invoked in this way, or a Connection should be passed in");
        }
        return this.getDataSource().getConnection();
    }

    protected PreparedStatement prepareStatement(final Connection conn, final String sql)
            throws SQLException {

        @SuppressWarnings("resource")
        final
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            configureStatement(ps);
        } catch (final SQLException e) {
            ps.close();
            throw e;
        }
        return ps;
    }

    protected PreparedStatement prepareStatement(final Connection conn, final String sql, final int returnedKeys)
            throws SQLException {

        @SuppressWarnings("resource")
        final
        PreparedStatement ps = conn.prepareStatement(sql, returnedKeys);
        try {
            configureStatement(ps);
        } catch (final SQLException e) {
            ps.close();
            throw e;
        }
        return ps;
    }

    protected void rethrow(final SQLException cause, final String sql, final Object... params)
            throws SQLException {

        String causeMessage = cause.getMessage();
        if (causeMessage == null) {
            causeMessage = "";
        }
        final StringBuffer msg = new StringBuffer(causeMessage);

        msg.append(" Query: ");
        msg.append(sql);
        msg.append(" Parameters: ");

        if (params == null) {
            msg.append("[]");
        } else {
            msg.append(Arrays.deepToString(params));
        }

        final SQLException e = new SQLException(msg.toString(), cause.getSQLState(),
                cause.getErrorCode());
        e.setNextException(cause);

        throw e;
    }

    protected ResultSet wrap(final ResultSet rs) {
        return rs;
    }

}
