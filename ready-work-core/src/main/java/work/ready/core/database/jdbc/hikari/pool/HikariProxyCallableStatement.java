package work.ready.core.database.jdbc.hikari.pool;

import java.io.InputStream;
import java.io.Reader;
import java.lang.Class;
import java.lang.Deprecated;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLType;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Wrapper;
import java.util.Calendar;
import java.util.Map;
import work.ready.core.database.jdbc.common.CallableStatementInformation;
import work.ready.core.database.jdbc.event.JdbcEventListener;

public final class HikariProxyCallableStatement extends ProxyCallableStatement implements Wrapper, Statement, PreparedStatement, CallableStatement {
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private JdbcEventListener jdbcEventListener;

    private CallableStatementInformation statementInformation;

    protected HikariProxyCallableStatement(ProxyConnection connection,
            CallableStatement statement) {
        super(connection, statement);
    }

    protected HikariProxyCallableStatement advancedFeatureSupport(
            final CallableStatementInformation statementInformation,
            final JdbcEventListener jdbcEventListener) {
        this.statementInformation = statementInformation;
        this.jdbcEventListener = jdbcEventListener;
        return this;
    }

    public JdbcEventListener getJdbcEventListener() {
        return jdbcEventListener;
    }

    public CallableStatementInformation getStatementInformation() {
        return statementInformation;
    }

    public void replaceStatement(final Statement statement) throws SQLException {
        this.delegate.close();
        this.delegate = statement;
    }

    @Override
    public boolean isWrapperFor(Class<?> arg0) throws SQLException {
        try { return ((CallableStatement) delegate).isWrapperFor(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean execute(String arg0) throws SQLException {
        statementInformation.setStatementQuery(arg0);
        SQLException e = null;
        long start = System.nanoTime();
        try {
          return super.execute(jdbcEventListener.onBeforeExecute(statementInformation, arg0));
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterExecute(statementInformation, System.nanoTime() - start, arg0, e);
        }
    }

    @Override
    public boolean execute(String arg0, String[] arg1) throws SQLException {
        statementInformation.setStatementQuery(arg0);
        SQLException e = null;
        long start = System.nanoTime();
        try {
          return super.execute(jdbcEventListener.onBeforeExecute(statementInformation, arg0), arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterExecute(statementInformation, System.nanoTime() - start, arg0, e);
        }
    }

    @Override
    public boolean execute(String arg0, int arg1) throws SQLException {
        statementInformation.setStatementQuery(arg0);
        SQLException e = null;
        long start = System.nanoTime();
        try {
          return super.execute(jdbcEventListener.onBeforeExecute(statementInformation, arg0), arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterExecute(statementInformation, System.nanoTime() - start, arg0, e);
        }
    }

    @Override
    public boolean execute(String arg0, int[] arg1) throws SQLException {
        statementInformation.setStatementQuery(arg0);
        SQLException e = null;
        long start = System.nanoTime();
        try {
          return super.execute(jdbcEventListener.onBeforeExecute(statementInformation, arg0), arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterExecute(statementInformation, System.nanoTime() - start, arg0, e);
        }
    }

    @Override
    public void close() throws SQLException {
        SQLException e = null;
        try {
          super.close();
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterStatementClose(statementInformation, e);
        }
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        try { return ((CallableStatement) delegate).getWarnings(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void clearWarnings() throws SQLException {
        try {  ((CallableStatement) delegate).clearWarnings(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void cancel() throws SQLException {
        try {  ((CallableStatement) delegate).cancel(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getMaxRows() throws SQLException {
        try { return ((CallableStatement) delegate).getMaxRows(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public ResultSet executeQuery(String arg0) throws SQLException {
        statementInformation.setStatementQuery(arg0);
        SQLException e = null;
        long start = System.nanoTime();
        try {
          return super.executeQuery(jdbcEventListener.onBeforeExecuteQuery(statementInformation, arg0));
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterExecuteQuery(statementInformation, System.nanoTime() - start, arg0, e);
        }
    }

    @Override
    public int executeUpdate(String arg0, String[] arg1) throws SQLException {
        statementInformation.setStatementQuery(arg0);
        SQLException e = null;
        long start = System.nanoTime();
        int rowCount = 0;
        try {
          rowCount = super.executeUpdate(jdbcEventListener.onBeforeExecuteUpdate(statementInformation, arg0), arg1);
          return rowCount;
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterExecuteUpdate(statementInformation, System.nanoTime() - start, arg0, rowCount, e);
        }
    }

    @Override
    public int executeUpdate(String arg0, int arg1) throws SQLException {
        statementInformation.setStatementQuery(arg0);
        SQLException e = null;
        long start = System.nanoTime();
        int rowCount = 0;
        try {
          rowCount = super.executeUpdate(jdbcEventListener.onBeforeExecuteUpdate(statementInformation, arg0), arg1);
          return rowCount;
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterExecuteUpdate(statementInformation, System.nanoTime() - start, arg0, rowCount, e);
        }
    }

    @Override
    public int executeUpdate(String arg0) throws SQLException {
        statementInformation.setStatementQuery(arg0);
        SQLException e = null;
        long start = System.nanoTime();
        int rowCount = 0;
        try {
          rowCount = super.executeUpdate(jdbcEventListener.onBeforeExecuteUpdate(statementInformation, arg0));
          return rowCount;
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterExecuteUpdate(statementInformation, System.nanoTime() - start, arg0, rowCount, e);
        }
    }

    @Override
    public int executeUpdate(String arg0, int[] arg1) throws SQLException {
        statementInformation.setStatementQuery(arg0);
        SQLException e = null;
        long start = System.nanoTime();
        int rowCount = 0;
        try {
          rowCount = super.executeUpdate(jdbcEventListener.onBeforeExecuteUpdate(statementInformation, arg0), arg1);
          return rowCount;
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterExecuteUpdate(statementInformation, System.nanoTime() - start, arg0, rowCount, e);
        }
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        try { return ((CallableStatement) delegate).getMaxFieldSize(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setMaxFieldSize(int arg0) throws SQLException {
        try {  ((CallableStatement) delegate).setMaxFieldSize(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setMaxRows(int arg0) throws SQLException {
        try {  ((CallableStatement) delegate).setMaxRows(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setEscapeProcessing(boolean arg0) throws SQLException {
        try {  ((CallableStatement) delegate).setEscapeProcessing(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        try { return ((CallableStatement) delegate).getQueryTimeout(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setQueryTimeout(int arg0) throws SQLException {
        try {  ((CallableStatement) delegate).setQueryTimeout(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setCursorName(String arg0) throws SQLException {
        try {  ((CallableStatement) delegate).setCursorName(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        SQLException e = null;
        long start = System.nanoTime();
        try {
          return super.getResultSet();
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterGetResultSet(statementInformation, System.nanoTime() - start, e);
        }
    }

    @Override
    public int getUpdateCount() throws SQLException {
        try { return ((CallableStatement) delegate).getUpdateCount(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean getMoreResults(int arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getMoreResults(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        try { return ((CallableStatement) delegate).getMoreResults(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setFetchDirection(int arg0) throws SQLException {
        try {  ((CallableStatement) delegate).setFetchDirection(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getFetchDirection() throws SQLException {
        try { return ((CallableStatement) delegate).getFetchDirection(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setFetchSize(int arg0) throws SQLException {
        try {  ((CallableStatement) delegate).setFetchSize(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getFetchSize() throws SQLException {
        try { return ((CallableStatement) delegate).getFetchSize(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        try { return ((CallableStatement) delegate).getResultSetConcurrency(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getResultSetType() throws SQLException {
        try { return ((CallableStatement) delegate).getResultSetType(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void addBatch(String arg0) throws SQLException {
        if (statementInformation.getStatementQuery() == null) {
           statementInformation.setStatementQuery(arg0);
        } else {
           String lineSeparator = LINE_SEPARATOR;
           if(!statementInformation.getStatementQuery().endsWith(";")) {
               lineSeparator = ";" + lineSeparator;
           }   statementInformation.setStatementQuery(statementInformation.getStatementQuery() + lineSeparator + arg0);
        }
        SQLException e = null;
        long start = System.nanoTime();
        try {
          ((CallableStatement) delegate).addBatch(jdbcEventListener.onBeforeAddBatch(statementInformation, arg0));
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterAddBatch(statementInformation, System.nanoTime() - start, arg0, e);
        }
    }

    @Override
    public void clearBatch() throws SQLException {
        try {  ((CallableStatement) delegate).clearBatch(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int[] executeBatch() throws SQLException {
        SQLException e = null;
        long start = System.nanoTime();
        int[] updateCounts = null;
        try {
          jdbcEventListener.onBeforeExecuteBatch(statementInformation);
          updateCounts = super.executeBatch();
          return updateCounts;
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterExecuteBatch(statementInformation, System.nanoTime() - start, updateCounts, e);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        try { return super.getConnection(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        try { return super.getGeneratedKeys(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        try { return ((CallableStatement) delegate).getResultSetHoldability(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setPoolable(boolean arg0) throws SQLException {
        try {  ((CallableStatement) delegate).setPoolable(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean isPoolable() throws SQLException {
        try { return ((CallableStatement) delegate).isPoolable(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        try {  ((CallableStatement) delegate).closeOnCompletion(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        try { return ((CallableStatement) delegate).isCloseOnCompletion(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public long getLargeUpdateCount() throws SQLException {
        try { return ((CallableStatement) delegate).getLargeUpdateCount(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setLargeMaxRows(long arg0) throws SQLException {
        try {  ((CallableStatement) delegate).setLargeMaxRows(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public long getLargeMaxRows() throws SQLException {
        try { return ((CallableStatement) delegate).getLargeMaxRows(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public long[] executeLargeBatch() throws SQLException {
        try { return ((CallableStatement) delegate).executeLargeBatch(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public long executeLargeUpdate(String arg0, int[] arg1) throws SQLException {
        try { return ((CallableStatement) delegate).executeLargeUpdate(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public long executeLargeUpdate(String arg0, int arg1) throws SQLException {
        try { return ((CallableStatement) delegate).executeLargeUpdate(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public long executeLargeUpdate(String arg0) throws SQLException {
        try { return ((CallableStatement) delegate).executeLargeUpdate(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public long executeLargeUpdate(String arg0, String[] arg1) throws SQLException {
        try { return ((CallableStatement) delegate).executeLargeUpdate(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public String enquoteLiteral(String arg0) throws SQLException {
        try { return ((CallableStatement) delegate).enquoteLiteral(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public String enquoteIdentifier(String arg0, boolean arg1) throws SQLException {
        try { return ((CallableStatement) delegate).enquoteIdentifier(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean isSimpleIdentifier(String arg0) throws SQLException {
        try { return ((CallableStatement) delegate).isSimpleIdentifier(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public String enquoteNCharLiteral(String arg0) throws SQLException {
        try { return ((CallableStatement) delegate).enquoteNCharLiteral(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean isClosed() throws SQLException {
        try { return ((CallableStatement) delegate).isClosed(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setTime(int arg0, Time arg1, Calendar arg2) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setTime(arg0, arg1, arg2);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setTime(int arg0, Time arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setTime(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setArray(int arg0, Array arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setArray(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setNull(int arg0, int arg1, String arg2) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setNull(arg0, arg1, arg2); 
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, null, e);
        }
    }

    @Override
    public void setNull(int arg0, int arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setNull(arg0, arg1); 
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, null, e);
        }
    }

    @Override
    public void setString(int arg0, String arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setString(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setBytes(int arg0, byte[] arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setBytes(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setBigDecimal(int arg0, BigDecimal arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setBigDecimal(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setAsciiStream(int arg0, InputStream arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setAsciiStream(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setAsciiStream(int arg0, InputStream arg1, int arg2) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setAsciiStream(arg0, arg1, arg2);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setAsciiStream(int arg0, InputStream arg1, long arg2) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setAsciiStream(arg0, arg1, arg2);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Deprecated(
            forRemoval = false,
            since = "1.2"
    )
    @Override
    public void setUnicodeStream(int arg0, InputStream arg1, int arg2) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setUnicodeStream(arg0, arg1, arg2);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setBinaryStream(int arg0, InputStream arg1, int arg2) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setBinaryStream(arg0, arg1, arg2);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setBinaryStream(int arg0, InputStream arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setBinaryStream(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setBinaryStream(int arg0, InputStream arg1, long arg2) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setBinaryStream(arg0, arg1, arg2);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void clearParameters() throws SQLException {
        try {  ((CallableStatement) delegate).clearParameters(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setObject(int arg0, Object arg1, SQLType arg2) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setObject(arg0, arg1, arg2);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setObject(int arg0, Object arg1, int arg2) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setObject(arg0, arg1, arg2);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setObject(int arg0, Object arg1, int arg2, int arg3) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setObject(arg0, arg1, arg2, arg3);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setObject(int arg0, Object arg1, SQLType arg2, int arg3) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setObject(arg0, arg1, arg2, arg3);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setObject(int arg0, Object arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setObject(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setCharacterStream(int arg0, Reader arg1, int arg2) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setCharacterStream(arg0, arg1, arg2);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setCharacterStream(int arg0, Reader arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setCharacterStream(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setCharacterStream(int arg0, Reader arg1, long arg2) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setCharacterStream(arg0, arg1, arg2);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setRef(int arg0, Ref arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setRef(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setBlob(int arg0, InputStream arg1, long arg2) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setBlob(arg0, arg1, arg2);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setBlob(int arg0, Blob arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setBlob(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setBlob(int arg0, InputStream arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setBlob(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setClob(int arg0, Reader arg1, long arg2) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setClob(arg0, arg1, arg2);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setClob(int arg0, Reader arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setClob(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setClob(int arg0, Clob arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setClob(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        try { return ((CallableStatement) delegate).getParameterMetaData(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setRowId(int arg0, RowId arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setRowId(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setNString(int arg0, String arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setNString(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setNCharacterStream(int arg0, Reader arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setNCharacterStream(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setNCharacterStream(int arg0, Reader arg1, long arg2) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setNCharacterStream(arg0, arg1, arg2);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setNClob(int arg0, NClob arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setNClob(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setNClob(int arg0, Reader arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setNClob(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setNClob(int arg0, Reader arg1, long arg2) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setNClob(arg0, arg1, arg2);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setSQLXML(int arg0, SQLXML arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setSQLXML(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public boolean execute() throws SQLException {
        SQLException e = null;
        long start = System.nanoTime();
        try {
          jdbcEventListener.onBeforeExecute(statementInformation);
          return super.execute();
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterExecute(statementInformation, System.nanoTime() - start, e);
        }
    }

    @Override
    public void setBoolean(int arg0, boolean arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setBoolean(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setByte(int arg0, byte arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setByte(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setShort(int arg0, short arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setShort(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setInt(int arg0, int arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setInt(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setLong(int arg0, long arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setLong(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setFloat(int arg0, float arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setFloat(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setDouble(int arg0, double arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setDouble(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        try { return ((CallableStatement) delegate).getMetaData(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        SQLException e = null;
        long start = System.nanoTime();
        try {
          jdbcEventListener.onBeforeExecuteQuery(statementInformation);
          return super.executeQuery();
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterExecuteQuery(statementInformation, System.nanoTime() - start, e);
        }
    }

    @Override
    public int executeUpdate() throws SQLException {
        SQLException e = null;
        long start = System.nanoTime();
        int rowCount = 0;
        try {
          jdbcEventListener.onBeforeExecuteUpdate(statementInformation);
          rowCount = super.executeUpdate();
          return rowCount;
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterExecuteUpdate(statementInformation, System.nanoTime() - start, rowCount, e);
        }
    }

    @Override
    public void addBatch() throws SQLException {
        SQLException e = null;
        long start = System.nanoTime();
        try {
          jdbcEventListener.onBeforeAddBatch(statementInformation);
          ((CallableStatement) delegate).addBatch();
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterAddBatch(statementInformation, System.nanoTime() - start, e);
        }
    }

    @Override
    public long executeLargeUpdate() throws SQLException {
        try { return ((CallableStatement) delegate).executeLargeUpdate(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setURL(int arg0, URL arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setURL(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setDate(int arg0, Date arg1, Calendar arg2) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setDate(arg0, arg1, arg2);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setDate(int arg0, Date arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setDate(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setTimestamp(int arg0, Timestamp arg1, Calendar arg2) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setTimestamp(arg0, arg1, arg2);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setTimestamp(int arg0, Timestamp arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setTimestamp(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public Time getTime(String arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getTime(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Time getTime(int arg0, Calendar arg1) throws SQLException {
        try { return ((CallableStatement) delegate).getTime(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Time getTime(String arg0, Calendar arg1) throws SQLException {
        try { return ((CallableStatement) delegate).getTime(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Time getTime(int arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getTime(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setTime(String arg0, Time arg1, Calendar arg2) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setTime(arg0, arg1, arg2);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setTime(String arg0, Time arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setTime(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public Timestamp getTimestamp(String arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getTimestamp(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Timestamp getTimestamp(int arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getTimestamp(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Timestamp getTimestamp(String arg0, Calendar arg1) throws SQLException {
        try { return ((CallableStatement) delegate).getTimestamp(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Timestamp getTimestamp(int arg0, Calendar arg1) throws SQLException {
        try { return ((CallableStatement) delegate).getTimestamp(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Array getArray(int arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getArray(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Array getArray(String arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getArray(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setNull(String arg0, int arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setNull(arg0, arg1); 
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation,  arg0, null, e);
        }
    }

    @Override
    public void setNull(String arg0, int arg1, String arg2) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setNull(arg0, arg1, arg2); 
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation,  arg0, null, e);
        }
    }

    @Override
    public void setString(String arg0, String arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setString(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setBytes(String arg0, byte[] arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setBytes(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setBigDecimal(String arg0, BigDecimal arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setBigDecimal(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setAsciiStream(String arg0, InputStream arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setAsciiStream(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setAsciiStream(String arg0, InputStream arg1, long arg2) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setAsciiStream(arg0, arg1, arg2);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setAsciiStream(String arg0, InputStream arg1, int arg2) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setAsciiStream(arg0, arg1, arg2);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setBinaryStream(String arg0, InputStream arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setBinaryStream(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setBinaryStream(String arg0, InputStream arg1, int arg2) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setBinaryStream(arg0, arg1, arg2);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setBinaryStream(String arg0, InputStream arg1, long arg2) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setBinaryStream(arg0, arg1, arg2);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setObject(String arg0, Object arg1, int arg2) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setObject(arg0, arg1, arg2);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setObject(String arg0, Object arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setObject(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setObject(String arg0, Object arg1, SQLType arg2) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setObject(arg0, arg1, arg2);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setObject(String arg0, Object arg1, int arg2, int arg3) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setObject(arg0, arg1, arg2, arg3);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setObject(String arg0, Object arg1, SQLType arg2, int arg3) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setObject(arg0, arg1, arg2, arg3);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setCharacterStream(String arg0, Reader arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setCharacterStream(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setCharacterStream(String arg0, Reader arg1, long arg2) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setCharacterStream(arg0, arg1, arg2);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setCharacterStream(String arg0, Reader arg1, int arg2) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setCharacterStream(arg0, arg1, arg2);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setBlob(String arg0, Blob arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setBlob(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setBlob(String arg0, InputStream arg1, long arg2) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setBlob(arg0, arg1, arg2);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setBlob(String arg0, InputStream arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setBlob(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setClob(String arg0, Clob arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setClob(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setClob(String arg0, Reader arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setClob(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setClob(String arg0, Reader arg1, long arg2) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setClob(arg0, arg1, arg2);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setRowId(String arg0, RowId arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setRowId(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setNString(String arg0, String arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setNString(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setNCharacterStream(String arg0, Reader arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setNCharacterStream(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setNCharacterStream(String arg0, Reader arg1, long arg2) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setNCharacterStream(arg0, arg1, arg2);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setNClob(String arg0, Reader arg1, long arg2) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setNClob(arg0, arg1, arg2);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setNClob(String arg0, NClob arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setNClob(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setNClob(String arg0, Reader arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setNClob(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setSQLXML(String arg0, SQLXML arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setSQLXML(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public boolean wasNull() throws SQLException {
        try { return ((CallableStatement) delegate).wasNull(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void registerOutParameter(int arg0, SQLType arg1, String arg2) throws SQLException {
        try {  ((CallableStatement) delegate).registerOutParameter(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void registerOutParameter(String arg0, SQLType arg1) throws SQLException {
        try {  ((CallableStatement) delegate).registerOutParameter(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void registerOutParameter(String arg0, SQLType arg1, int arg2) throws SQLException {
        try {  ((CallableStatement) delegate).registerOutParameter(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void registerOutParameter(int arg0, SQLType arg1, int arg2) throws SQLException {
        try {  ((CallableStatement) delegate).registerOutParameter(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void registerOutParameter(int arg0, SQLType arg1) throws SQLException {
        try {  ((CallableStatement) delegate).registerOutParameter(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void registerOutParameter(String arg0, SQLType arg1, String arg2) throws SQLException {
        try {  ((CallableStatement) delegate).registerOutParameter(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void registerOutParameter(String arg0, int arg1, String arg2) throws SQLException {
        try {  ((CallableStatement) delegate).registerOutParameter(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void registerOutParameter(String arg0, int arg1, int arg2) throws SQLException {
        try {  ((CallableStatement) delegate).registerOutParameter(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void registerOutParameter(int arg0, int arg1, String arg2) throws SQLException {
        try {  ((CallableStatement) delegate).registerOutParameter(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void registerOutParameter(String arg0, int arg1) throws SQLException {
        try {  ((CallableStatement) delegate).registerOutParameter(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void registerOutParameter(int arg0, int arg1) throws SQLException {
        try {  ((CallableStatement) delegate).registerOutParameter(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void registerOutParameter(int arg0, int arg1, int arg2) throws SQLException {
        try {  ((CallableStatement) delegate).registerOutParameter(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public BigDecimal getBigDecimal(int arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getBigDecimal(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public BigDecimal getBigDecimal(String arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getBigDecimal(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Deprecated(
            forRemoval = false,
            since = "1.2"
    )
    @Override
    public BigDecimal getBigDecimal(int arg0, int arg1) throws SQLException {
        try { return ((CallableStatement) delegate).getBigDecimal(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Blob getBlob(String arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getBlob(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Blob getBlob(int arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getBlob(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Clob getClob(String arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getClob(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Clob getClob(int arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getClob(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public RowId getRowId(int arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getRowId(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public RowId getRowId(String arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getRowId(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public NClob getNClob(String arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getNClob(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public NClob getNClob(int arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getNClob(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public SQLXML getSQLXML(int arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getSQLXML(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public SQLXML getSQLXML(String arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getSQLXML(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public String getNString(int arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getNString(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public String getNString(String arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getNString(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Reader getNCharacterStream(String arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getNCharacterStream(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Reader getNCharacterStream(int arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getNCharacterStream(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Reader getCharacterStream(int arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getCharacterStream(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Reader getCharacterStream(String arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getCharacterStream(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public <T> T getObject(int arg0, Class<T> arg1) throws SQLException {
        try { return ((CallableStatement) delegate).getObject(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Object getObject(int arg0, Map<String, Class<?>> arg1) throws SQLException {
        try { return ((CallableStatement) delegate).getObject(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Object getObject(String arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getObject(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Object getObject(int arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getObject(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public <T> T getObject(String arg0, Class<T> arg1) throws SQLException {
        try { return ((CallableStatement) delegate).getObject(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Object getObject(String arg0, Map<String, Class<?>> arg1) throws SQLException {
        try { return ((CallableStatement) delegate).getObject(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean getBoolean(int arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getBoolean(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean getBoolean(String arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getBoolean(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public byte getByte(int arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getByte(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public byte getByte(String arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getByte(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public short getShort(String arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getShort(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public short getShort(int arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getShort(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getInt(int arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getInt(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getInt(String arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getInt(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public long getLong(String arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getLong(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public long getLong(int arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getLong(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public float getFloat(String arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getFloat(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public float getFloat(int arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getFloat(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public double getDouble(String arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getDouble(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public double getDouble(int arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getDouble(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public byte[] getBytes(int arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getBytes(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public byte[] getBytes(String arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getBytes(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setBoolean(String arg0, boolean arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setBoolean(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setByte(String arg0, byte arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setByte(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setShort(String arg0, short arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setShort(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setInt(String arg0, int arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setInt(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setLong(String arg0, long arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setLong(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setFloat(String arg0, float arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setFloat(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setDouble(String arg0, double arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setDouble(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public Ref getRef(int arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getRef(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Ref getRef(String arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getRef(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setURL(String arg0, URL arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setURL(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public Date getDate(int arg0, Calendar arg1) throws SQLException {
        try { return ((CallableStatement) delegate).getDate(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Date getDate(String arg0, Calendar arg1) throws SQLException {
        try { return ((CallableStatement) delegate).getDate(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Date getDate(String arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getDate(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Date getDate(int arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getDate(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setDate(String arg0, Date arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setDate(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setDate(String arg0, Date arg1, Calendar arg2) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setDate(arg0, arg1, arg2);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setTimestamp(String arg0, Timestamp arg1, Calendar arg2) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setTimestamp(arg0, arg1, arg2);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void setTimestamp(String arg0, Timestamp arg1) throws SQLException {
        SQLException e = null;
        try {
          ((CallableStatement) delegate).setTimestamp(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterCallableStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public URL getURL(int arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getURL(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public URL getURL(String arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getURL(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public String getString(int arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getString(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public String getString(String arg0) throws SQLException {
        try { return ((CallableStatement) delegate).getString(arg0); } catch (SQLException e) { throw checkException(e); }
    }
}
