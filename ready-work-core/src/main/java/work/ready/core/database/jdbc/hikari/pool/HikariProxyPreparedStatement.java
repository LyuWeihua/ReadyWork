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
import work.ready.core.database.jdbc.common.PreparedStatementInformation;
import work.ready.core.database.jdbc.event.JdbcEventListener;

public final class HikariProxyPreparedStatement extends ProxyPreparedStatement implements Wrapper, Statement, PreparedStatement {
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private JdbcEventListener jdbcEventListener;

    private PreparedStatementInformation statementInformation;

    HikariProxyPreparedStatement(ProxyConnection connection, PreparedStatement statement) {
        super(connection, statement);
    }

    protected HikariProxyPreparedStatement advancedFeatureSupport(
            final PreparedStatementInformation statementInformation,
            final JdbcEventListener jdbcEventListener) {
        this.statementInformation = statementInformation;
        this.jdbcEventListener = jdbcEventListener;
        return this;
    }

    public JdbcEventListener getJdbcEventListener() {
        return jdbcEventListener;
    }

    public PreparedStatementInformation getStatementInformation() {
        return statementInformation;
    }

    public void replaceStatement(final Statement statement) throws SQLException {
        this.delegate.close();
        this.delegate = statement;
    }

    @Override
    public boolean isWrapperFor(Class<?> arg0) throws SQLException {
        try { return ((PreparedStatement) delegate).isWrapperFor(arg0); } catch (SQLException e) { throw checkException(e); }
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
        try { return ((PreparedStatement) delegate).getWarnings(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void clearWarnings() throws SQLException {
        try {  ((PreparedStatement) delegate).clearWarnings(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void cancel() throws SQLException {
        try {  ((PreparedStatement) delegate).cancel(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getMaxRows() throws SQLException {
        try { return ((PreparedStatement) delegate).getMaxRows(); } catch (SQLException e) { throw checkException(e); }
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
        try { return ((PreparedStatement) delegate).getMaxFieldSize(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setMaxFieldSize(int arg0) throws SQLException {
        try {  ((PreparedStatement) delegate).setMaxFieldSize(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setMaxRows(int arg0) throws SQLException {
        try {  ((PreparedStatement) delegate).setMaxRows(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setEscapeProcessing(boolean arg0) throws SQLException {
        try {  ((PreparedStatement) delegate).setEscapeProcessing(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        try { return ((PreparedStatement) delegate).getQueryTimeout(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setQueryTimeout(int arg0) throws SQLException {
        try {  ((PreparedStatement) delegate).setQueryTimeout(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setCursorName(String arg0) throws SQLException {
        try {  ((PreparedStatement) delegate).setCursorName(arg0); } catch (SQLException e) { throw checkException(e); }
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
        try { return ((PreparedStatement) delegate).getUpdateCount(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean getMoreResults(int arg0) throws SQLException {
        try { return ((PreparedStatement) delegate).getMoreResults(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        try { return ((PreparedStatement) delegate).getMoreResults(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setFetchDirection(int arg0) throws SQLException {
        try {  ((PreparedStatement) delegate).setFetchDirection(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getFetchDirection() throws SQLException {
        try { return ((PreparedStatement) delegate).getFetchDirection(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setFetchSize(int arg0) throws SQLException {
        try {  ((PreparedStatement) delegate).setFetchSize(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getFetchSize() throws SQLException {
        try { return ((PreparedStatement) delegate).getFetchSize(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        try { return ((PreparedStatement) delegate).getResultSetConcurrency(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getResultSetType() throws SQLException {
        try { return ((PreparedStatement) delegate).getResultSetType(); } catch (SQLException e) { throw checkException(e); }
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
          ((PreparedStatement) delegate).addBatch(jdbcEventListener.onBeforeAddBatch(statementInformation, arg0));
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterAddBatch(statementInformation, System.nanoTime() - start, arg0, e);
        }
    }

    @Override
    public void clearBatch() throws SQLException {
        try {  ((PreparedStatement) delegate).clearBatch(); } catch (SQLException e) { throw checkException(e); }
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
        try { return ((PreparedStatement) delegate).getResultSetHoldability(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setPoolable(boolean arg0) throws SQLException {
        try {  ((PreparedStatement) delegate).setPoolable(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean isPoolable() throws SQLException {
        try { return ((PreparedStatement) delegate).isPoolable(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        try {  ((PreparedStatement) delegate).closeOnCompletion(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        try { return ((PreparedStatement) delegate).isCloseOnCompletion(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public long getLargeUpdateCount() throws SQLException {
        try { return ((PreparedStatement) delegate).getLargeUpdateCount(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setLargeMaxRows(long arg0) throws SQLException {
        try {  ((PreparedStatement) delegate).setLargeMaxRows(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public long getLargeMaxRows() throws SQLException {
        try { return ((PreparedStatement) delegate).getLargeMaxRows(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public long[] executeLargeBatch() throws SQLException {
        try { return ((PreparedStatement) delegate).executeLargeBatch(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public long executeLargeUpdate(String arg0, int[] arg1) throws SQLException {
        try { return ((PreparedStatement) delegate).executeLargeUpdate(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public long executeLargeUpdate(String arg0, int arg1) throws SQLException {
        try { return ((PreparedStatement) delegate).executeLargeUpdate(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public long executeLargeUpdate(String arg0) throws SQLException {
        try { return ((PreparedStatement) delegate).executeLargeUpdate(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public long executeLargeUpdate(String arg0, String[] arg1) throws SQLException {
        try { return ((PreparedStatement) delegate).executeLargeUpdate(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public String enquoteLiteral(String arg0) throws SQLException {
        try { return ((PreparedStatement) delegate).enquoteLiteral(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public String enquoteIdentifier(String arg0, boolean arg1) throws SQLException {
        try { return ((PreparedStatement) delegate).enquoteIdentifier(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean isSimpleIdentifier(String arg0) throws SQLException {
        try { return ((PreparedStatement) delegate).isSimpleIdentifier(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public String enquoteNCharLiteral(String arg0) throws SQLException {
        try { return ((PreparedStatement) delegate).enquoteNCharLiteral(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean isClosed() throws SQLException {
        try { return ((PreparedStatement) delegate).isClosed(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setTime(int arg0, Time arg1, Calendar arg2) throws SQLException {
        SQLException e = null;
        try {
          ((PreparedStatement) delegate).setTime(arg0, arg1, arg2);
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
          ((PreparedStatement) delegate).setTime(arg0, arg1);
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
          ((PreparedStatement) delegate).setArray(arg0, arg1);
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
          ((PreparedStatement) delegate).setNull(arg0, arg1, arg2); 
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
          ((PreparedStatement) delegate).setNull(arg0, arg1); 
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
          ((PreparedStatement) delegate).setString(arg0, arg1);
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
          ((PreparedStatement) delegate).setBytes(arg0, arg1);
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
          ((PreparedStatement) delegate).setBigDecimal(arg0, arg1);
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
          ((PreparedStatement) delegate).setAsciiStream(arg0, arg1);
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
          ((PreparedStatement) delegate).setAsciiStream(arg0, arg1, arg2);
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
          ((PreparedStatement) delegate).setAsciiStream(arg0, arg1, arg2);
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
          ((PreparedStatement) delegate).setUnicodeStream(arg0, arg1, arg2);
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
          ((PreparedStatement) delegate).setBinaryStream(arg0, arg1, arg2);
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
          ((PreparedStatement) delegate).setBinaryStream(arg0, arg1);
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
          ((PreparedStatement) delegate).setBinaryStream(arg0, arg1, arg2);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public void clearParameters() throws SQLException {
        try {  ((PreparedStatement) delegate).clearParameters(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setObject(int arg0, Object arg1, SQLType arg2) throws SQLException {
        SQLException e = null;
        try {
          ((PreparedStatement) delegate).setObject(arg0, arg1, arg2);
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
          ((PreparedStatement) delegate).setObject(arg0, arg1, arg2);
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
          ((PreparedStatement) delegate).setObject(arg0, arg1, arg2, arg3);
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
          ((PreparedStatement) delegate).setObject(arg0, arg1, arg2, arg3);
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
          ((PreparedStatement) delegate).setObject(arg0, arg1);
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
          ((PreparedStatement) delegate).setCharacterStream(arg0, arg1, arg2);
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
          ((PreparedStatement) delegate).setCharacterStream(arg0, arg1);
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
          ((PreparedStatement) delegate).setCharacterStream(arg0, arg1, arg2);
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
          ((PreparedStatement) delegate).setRef(arg0, arg1);
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
          ((PreparedStatement) delegate).setBlob(arg0, arg1, arg2);
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
          ((PreparedStatement) delegate).setBlob(arg0, arg1);
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
          ((PreparedStatement) delegate).setBlob(arg0, arg1);
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
          ((PreparedStatement) delegate).setClob(arg0, arg1, arg2);
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
          ((PreparedStatement) delegate).setClob(arg0, arg1);
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
          ((PreparedStatement) delegate).setClob(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        try { return ((PreparedStatement) delegate).getParameterMetaData(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setRowId(int arg0, RowId arg1) throws SQLException {
        SQLException e = null;
        try {
          ((PreparedStatement) delegate).setRowId(arg0, arg1);
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
          ((PreparedStatement) delegate).setNString(arg0, arg1);
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
          ((PreparedStatement) delegate).setNCharacterStream(arg0, arg1);
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
          ((PreparedStatement) delegate).setNCharacterStream(arg0, arg1, arg2);
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
          ((PreparedStatement) delegate).setNClob(arg0, arg1);
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
          ((PreparedStatement) delegate).setNClob(arg0, arg1);
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
          ((PreparedStatement) delegate).setNClob(arg0, arg1, arg2);
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
          ((PreparedStatement) delegate).setSQLXML(arg0, arg1);
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
          ((PreparedStatement) delegate).setBoolean(arg0, arg1);
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
          ((PreparedStatement) delegate).setByte(arg0, arg1);
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
          ((PreparedStatement) delegate).setShort(arg0, arg1);
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
          ((PreparedStatement) delegate).setInt(arg0, arg1);
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
          ((PreparedStatement) delegate).setLong(arg0, arg1);
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
          ((PreparedStatement) delegate).setFloat(arg0, arg1);
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
          ((PreparedStatement) delegate).setDouble(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        try { return ((PreparedStatement) delegate).getMetaData(); } catch (SQLException e) { throw checkException(e); }
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
          ((PreparedStatement) delegate).addBatch();
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterAddBatch(statementInformation, System.nanoTime() - start, e);
        }
    }

    @Override
    public long executeLargeUpdate() throws SQLException {
        try { return ((PreparedStatement) delegate).executeLargeUpdate(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setURL(int arg0, URL arg1) throws SQLException {
        SQLException e = null;
        try {
          ((PreparedStatement) delegate).setURL(arg0, arg1);
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
          ((PreparedStatement) delegate).setDate(arg0, arg1, arg2);
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
          ((PreparedStatement) delegate).setDate(arg0, arg1);
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
          ((PreparedStatement) delegate).setTimestamp(arg0, arg1, arg2);
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
          ((PreparedStatement) delegate).setTimestamp(arg0, arg1);
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterPreparedStatementSet(statementInformation, arg0, arg1, e);
        }
    }
}
