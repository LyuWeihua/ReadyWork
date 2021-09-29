package work.ready.core.database.jdbc.hikari.pool;

import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Wrapper;
import work.ready.core.database.jdbc.common.StatementInformation;
import work.ready.core.database.jdbc.event.JdbcEventListener;

public final class HikariProxyStatement extends ProxyStatement implements Wrapper, Statement {
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private JdbcEventListener jdbcEventListener;

    private StatementInformation statementInformation;

    HikariProxyStatement(ProxyConnection connection, Statement statement) {
        super(connection, statement);
    }

    protected HikariProxyStatement advancedFeatureSupport(
            final StatementInformation statementInformation,
            final JdbcEventListener jdbcEventListener) {
        this.statementInformation = statementInformation;
        this.jdbcEventListener = jdbcEventListener;
        return this;
    }

    public JdbcEventListener getJdbcEventListener() {
        return jdbcEventListener;
    }

    public StatementInformation getStatementInformation() {
        return statementInformation;
    }

    public void replaceStatement(final Statement statement) throws SQLException {
        this.delegate.close();
        this.delegate = statement;
    }

    @Override
    public boolean isWrapperFor(Class<?> arg0) throws SQLException {
        try { return delegate.isWrapperFor(arg0); } catch (SQLException e) { throw checkException(e); }
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
        try { return delegate.getWarnings(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void clearWarnings() throws SQLException {
        try {  delegate.clearWarnings(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void cancel() throws SQLException {
        try {  delegate.cancel(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getMaxRows() throws SQLException {
        try { return delegate.getMaxRows(); } catch (SQLException e) { throw checkException(e); }
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
        try { return delegate.getMaxFieldSize(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setMaxFieldSize(int arg0) throws SQLException {
        try {  delegate.setMaxFieldSize(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setMaxRows(int arg0) throws SQLException {
        try {  delegate.setMaxRows(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setEscapeProcessing(boolean arg0) throws SQLException {
        try {  delegate.setEscapeProcessing(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        try { return delegate.getQueryTimeout(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setQueryTimeout(int arg0) throws SQLException {
        try {  delegate.setQueryTimeout(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setCursorName(String arg0) throws SQLException {
        try {  delegate.setCursorName(arg0); } catch (SQLException e) { throw checkException(e); }
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
        try { return delegate.getUpdateCount(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean getMoreResults(int arg0) throws SQLException {
        try { return delegate.getMoreResults(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        try { return delegate.getMoreResults(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setFetchDirection(int arg0) throws SQLException {
        try {  delegate.setFetchDirection(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getFetchDirection() throws SQLException {
        try { return delegate.getFetchDirection(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setFetchSize(int arg0) throws SQLException {
        try {  delegate.setFetchSize(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getFetchSize() throws SQLException {
        try { return delegate.getFetchSize(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        try { return delegate.getResultSetConcurrency(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getResultSetType() throws SQLException {
        try { return delegate.getResultSetType(); } catch (SQLException e) { throw checkException(e); }
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
          delegate.addBatch(jdbcEventListener.onBeforeAddBatch(statementInformation, arg0));
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterAddBatch(statementInformation, System.nanoTime() - start, arg0, e);
        }
    }

    @Override
    public void clearBatch() throws SQLException {
        try {  delegate.clearBatch(); } catch (SQLException e) { throw checkException(e); }
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
        try { return delegate.getResultSetHoldability(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setPoolable(boolean arg0) throws SQLException {
        try {  delegate.setPoolable(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean isPoolable() throws SQLException {
        try { return delegate.isPoolable(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        try {  delegate.closeOnCompletion(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        try { return delegate.isCloseOnCompletion(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public long getLargeUpdateCount() throws SQLException {
        try { return delegate.getLargeUpdateCount(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void setLargeMaxRows(long arg0) throws SQLException {
        try {  delegate.setLargeMaxRows(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public long getLargeMaxRows() throws SQLException {
        try { return delegate.getLargeMaxRows(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public long[] executeLargeBatch() throws SQLException {
        try { return delegate.executeLargeBatch(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public long executeLargeUpdate(String arg0, int[] arg1) throws SQLException {
        try { return delegate.executeLargeUpdate(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public long executeLargeUpdate(String arg0, int arg1) throws SQLException {
        try { return delegate.executeLargeUpdate(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public long executeLargeUpdate(String arg0) throws SQLException {
        try { return delegate.executeLargeUpdate(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public long executeLargeUpdate(String arg0, String[] arg1) throws SQLException {
        try { return delegate.executeLargeUpdate(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public String enquoteLiteral(String arg0) throws SQLException {
        try { return delegate.enquoteLiteral(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public String enquoteIdentifier(String arg0, boolean arg1) throws SQLException {
        try { return delegate.enquoteIdentifier(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean isSimpleIdentifier(String arg0) throws SQLException {
        try { return delegate.isSimpleIdentifier(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public String enquoteNCharLiteral(String arg0) throws SQLException {
        try { return delegate.enquoteNCharLiteral(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean isClosed() throws SQLException {
        try { return delegate.isClosed(); } catch (SQLException e) { throw checkException(e); }
    }
}
