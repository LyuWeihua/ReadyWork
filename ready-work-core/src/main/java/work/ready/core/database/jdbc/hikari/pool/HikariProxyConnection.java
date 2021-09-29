package work.ready.core.database.jdbc.hikari.pool;

import java.lang.Class;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.ShardingKey;
import java.sql.Statement;
import java.sql.Struct;
import java.sql.Wrapper;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import work.ready.core.database.jdbc.common.ConnectionInformation;
import work.ready.core.database.jdbc.event.JdbcEventListener;
import work.ready.core.database.jdbc.hikari.util.FastList;

public final class HikariProxyConnection extends ProxyConnection implements Wrapper, Connection {
    private JdbcEventListener jdbcEventListener;

    private ConnectionInformation connectionInformation;

    protected HikariProxyConnection(final PoolEntry poolEntry, final Connection connection,
            final FastList<Statement> openStatements, final ProxyLeakTask leakTask, final long now,
            final boolean isReadOnly, final boolean isAutoCommit) {
        super(poolEntry, connection, openStatements, leakTask, now, isReadOnly, isAutoCommit);
    }

    public void advancedFeatureSupport(final ConnectionInformation connectionInformation,
            final JdbcEventListener jdbcEventListener) {
        this.connectionInformation = connectionInformation;
        this.jdbcEventListener = jdbcEventListener;
    }

    public JdbcEventListener getJdbcEventListener() {
        return jdbcEventListener;
    }

    public ConnectionInformation getConnectionInformation() {
        return connectionInformation;
    }

    public String getPoolName() {
        return getPoolEntry().getPoolName();
    }

    @Override
    public void setReadOnly(boolean arg0) throws SQLException {
        try {  super.setReadOnly(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void close() throws SQLException {
        SQLException e = null;
        long start = System.nanoTime();
        try {
            super.close();
        } catch (SQLException sqle) {
            e = sqle;
            throw checkException(e);
        } finally {
            connectionInformation.setTimeToCloseConnectionNs(System.nanoTime() - start);
            jdbcEventListener.onAfterConnectionClose(connectionInformation, e);
        }
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        try { return delegate.isReadOnly(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public String nativeSQL(String arg0) throws SQLException {
        try { return delegate.nativeSQL(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Statement createStatement(int arg0, int arg1) throws SQLException {
        try { return super.createStatement(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Statement createStatement() throws SQLException {
        try { return super.createStatement(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Statement createStatement(int arg0, int arg1, int arg2) throws SQLException {
        try { return super.createStatement(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public PreparedStatement prepareStatement(String arg0, int arg1, int arg2) throws SQLException {
        try { return super.prepareStatement(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public PreparedStatement prepareStatement(String arg0, int arg1, int arg2, int arg3) throws
            SQLException {
        try { return super.prepareStatement(arg0, arg1, arg2, arg3); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public PreparedStatement prepareStatement(String arg0, int[] arg1) throws SQLException {
        try { return super.prepareStatement(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public PreparedStatement prepareStatement(String arg0, int arg1) throws SQLException {
        try { return super.prepareStatement(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public PreparedStatement prepareStatement(String arg0) throws SQLException {
        try { return super.prepareStatement(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public PreparedStatement prepareStatement(String arg0, String[] arg1) throws SQLException {
        try { return super.prepareStatement(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public CallableStatement prepareCall(String arg0, int arg1, int arg2, int arg3) throws
            SQLException {
        try { return super.prepareCall(arg0, arg1, arg2, arg3); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public CallableStatement prepareCall(String arg0, int arg1, int arg2) throws SQLException {
        try { return super.prepareCall(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public CallableStatement prepareCall(String arg0) throws SQLException {
        try { return super.prepareCall(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setAutoCommit(boolean arg0) throws SQLException {
        SQLException e = null;
        boolean oldAutoCommit = delegate.getAutoCommit();
        try {
            jdbcEventListener.onBeforeSetAutoCommit(connectionInformation, arg0, oldAutoCommit);
            super.setAutoCommit(arg0);
        } catch (SQLException sqle){
            e = sqle;
            throw checkException(e);
        } finally {
            jdbcEventListener.onAfterSetAutoCommit(connectionInformation, arg0, oldAutoCommit, e);
        }
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        try { return delegate.getAutoCommit(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void rollback(Savepoint arg0) throws SQLException {
        SQLException e = null;
        long start = System.nanoTime();
        try {
            jdbcEventListener.onBeforeRollback(connectionInformation);
            super.rollback(arg0); 
        } catch (SQLException sqle) {
            e = sqle;
            throw checkException(e);
        } finally {
            jdbcEventListener.onAfterRollback(connectionInformation, System.nanoTime() - start, e);
        }
    }

    @Override
    public void rollback() throws SQLException {
        SQLException e = null;
        long start = System.nanoTime();
        try {
            jdbcEventListener.onBeforeRollback(connectionInformation);
            super.rollback();
        } catch (SQLException sqle) {
            e = sqle;
            throw checkException(e);
        } finally {
            jdbcEventListener.onAfterRollback(connectionInformation, System.nanoTime() - start, e);
        }
    }

    @Override
    public void commit() throws SQLException {
        SQLException e = null;
        long start = System.nanoTime();
        try {
            jdbcEventListener.onBeforeCommit(connectionInformation);
            super.commit();
        } catch (SQLException sqle) {
            e = sqle;
            throw checkException(e);
        } finally {
            jdbcEventListener.onAfterCommit(connectionInformation, System.nanoTime() - start, e);
        }
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        try { return super.getMetaData(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setCatalog(String arg0) throws SQLException {
        try {  super.setCatalog(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public String getCatalog() throws SQLException {
        try { return delegate.getCatalog(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setTransactionIsolation(int arg0) throws SQLException {
        try {  super.setTransactionIsolation(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        try { return delegate.getTransactionIsolation(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        try { return delegate.getWarnings(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void clearWarnings() throws SQLException {
        try {  delegate.clearWarnings(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        try { return delegate.getTypeMap(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> arg0) throws SQLException {
        try {  delegate.setTypeMap(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setHoldability(int arg0) throws SQLException {
        try {  delegate.setHoldability(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getHoldability() throws SQLException {
        try { return delegate.getHoldability(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        try { return delegate.setSavepoint(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Savepoint setSavepoint(String arg0) throws SQLException {
        try { return delegate.setSavepoint(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void releaseSavepoint(Savepoint arg0) throws SQLException {
        try {  delegate.releaseSavepoint(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Clob createClob() throws SQLException {
        try { return delegate.createClob(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Blob createBlob() throws SQLException {
        try { return delegate.createBlob(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public NClob createNClob() throws SQLException {
        try { return delegate.createNClob(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        try { return delegate.createSQLXML(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setClientInfo(String arg0, String arg1) throws SQLClientInfoException {
         ((Connection) delegate).setClientInfo(arg0, arg1);
    }

    @Override
    public void setClientInfo(Properties arg0) throws SQLClientInfoException {
         ((Connection) delegate).setClientInfo(arg0);
    }

    @Override
    public String getClientInfo(String arg0) throws SQLException {
        try { return delegate.getClientInfo(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        try { return delegate.getClientInfo(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Array createArrayOf(String arg0, Object[] arg1) throws SQLException {
        try { return delegate.createArrayOf(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Struct createStruct(String arg0, Object[] arg1) throws SQLException {
        try { return delegate.createStruct(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setSchema(String arg0) throws SQLException {
        try {  super.setSchema(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public String getSchema() throws SQLException {
        try { return delegate.getSchema(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setNetworkTimeout(Executor arg0, int arg1) throws SQLException {
        try {  super.setNetworkTimeout(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        try { return delegate.getNetworkTimeout(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void beginRequest() throws SQLException {
        try {  delegate.beginRequest(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void endRequest() throws SQLException {
        try {  delegate.endRequest(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean setShardingKeyIfValid(ShardingKey arg0, int arg1) throws SQLException {
        try { return delegate.setShardingKeyIfValid(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean setShardingKeyIfValid(ShardingKey arg0, ShardingKey arg1, int arg2) throws
            SQLException {
        try { return delegate.setShardingKeyIfValid(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setShardingKey(ShardingKey arg0, ShardingKey arg1) throws SQLException {
        try {  delegate.setShardingKey(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setShardingKey(ShardingKey arg0) throws SQLException {
        try {  delegate.setShardingKey(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean isClosed() throws SQLException {
        try { return super.isClosed(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean isValid(int arg0) throws SQLException {
        try { return delegate.isValid(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void abort(Executor arg0) throws SQLException {
        try {  delegate.abort(arg0); } catch (SQLException e) { throw checkException(e); }
    }
}
