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
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLType;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Wrapper;
import java.util.Calendar;
import java.util.Map;
import work.ready.core.database.jdbc.common.ResultSetInformation;
import work.ready.core.database.jdbc.event.JdbcEventListener;

public final class HikariProxyResultSet extends ProxyResultSet implements Wrapper, ResultSet {
    private JdbcEventListener jdbcEventListener;

    private ResultSetInformation resultSetInformation;

    protected HikariProxyResultSet(ProxyConnection connection, ProxyStatement statement,
            ResultSet resultSet) {
        super(connection, statement, resultSet);
    }

    protected HikariProxyResultSet advancedFeatureSupport(
            final ResultSetInformation resultSetInformation,
            final JdbcEventListener jdbcEventListener) {
        this.resultSetInformation = resultSetInformation;
        this.jdbcEventListener = jdbcEventListener;
        return this;
    }

    public JdbcEventListener getJdbcEventListener() {
        return jdbcEventListener;
    }

    public ResultSetInformation getResultSetInformation() {
        return resultSetInformation;
    }

    @Override
    public boolean isWrapperFor(Class<?> arg0) throws SQLException {
        try { return delegate.isWrapperFor(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Time getTime(String arg0) throws SQLException {
        SQLException e = null;
        try {
          Time value = delegate.getTime(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public Time getTime(int arg0, Calendar arg1) throws SQLException {
        SQLException e = null;
        try {
          Time value = delegate.getTime(arg0, arg1);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public Time getTime(String arg0, Calendar arg1) throws SQLException {
        SQLException e = null;
        try {
          Time value = delegate.getTime(arg0, arg1);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public Time getTime(int arg0) throws SQLException {
        SQLException e = null;
        try {
          Time value = delegate.getTime(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public Timestamp getTimestamp(String arg0) throws SQLException {
        SQLException e = null;
        try {
          Timestamp value = delegate.getTimestamp(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public Timestamp getTimestamp(int arg0, Calendar arg1) throws SQLException {
        SQLException e = null;
        try {
          Timestamp value = delegate.getTimestamp(arg0, arg1);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public Timestamp getTimestamp(String arg0, Calendar arg1) throws SQLException {
        SQLException e = null;
        try {
          Timestamp value = delegate.getTimestamp(arg0, arg1);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public Timestamp getTimestamp(int arg0) throws SQLException {
        SQLException e = null;
        try {
          Timestamp value = delegate.getTimestamp(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public Array getArray(String arg0) throws SQLException {
        SQLException e = null;
        try {
          Array value = delegate.getArray(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public Array getArray(int arg0) throws SQLException {
        SQLException e = null;
        try {
          Array value = delegate.getArray(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public boolean wasNull() throws SQLException {
        try { return delegate.wasNull(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public BigDecimal getBigDecimal(String arg0) throws SQLException {
        SQLException e = null;
        try {
          BigDecimal value = delegate.getBigDecimal(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Deprecated(
            forRemoval = false,
            since = "1.2"
    )
    @Override
    public BigDecimal getBigDecimal(String arg0, int arg1) throws SQLException {
        SQLException e = null;
        try {
          BigDecimal value = delegate.getBigDecimal(arg0, arg1);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public BigDecimal getBigDecimal(int arg0) throws SQLException {
        SQLException e = null;
        try {
          BigDecimal value = delegate.getBigDecimal(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Deprecated(
            forRemoval = false,
            since = "1.2"
    )
    @Override
    public BigDecimal getBigDecimal(int arg0, int arg1) throws SQLException {
        SQLException e = null;
        try {
          BigDecimal value = delegate.getBigDecimal(arg0, arg1);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public Blob getBlob(int arg0) throws SQLException {
        SQLException e = null;
        try {
          Blob value = delegate.getBlob(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public Blob getBlob(String arg0) throws SQLException {
        SQLException e = null;
        try {
          Blob value = delegate.getBlob(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public Clob getClob(int arg0) throws SQLException {
        SQLException e = null;
        try {
          Clob value = delegate.getClob(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public Clob getClob(String arg0) throws SQLException {
        SQLException e = null;
        try {
          Clob value = delegate.getClob(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public RowId getRowId(int arg0) throws SQLException {
        SQLException e = null;
        try {
          RowId value = delegate.getRowId(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public RowId getRowId(String arg0) throws SQLException {
        SQLException e = null;
        try {
          RowId value = delegate.getRowId(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public NClob getNClob(String arg0) throws SQLException {
        SQLException e = null;
        try {
          NClob value = delegate.getNClob(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public NClob getNClob(int arg0) throws SQLException {
        SQLException e = null;
        try {
          NClob value = delegate.getNClob(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public SQLXML getSQLXML(int arg0) throws SQLException {
        SQLException e = null;
        try {
          SQLXML value = delegate.getSQLXML(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public SQLXML getSQLXML(String arg0) throws SQLException {
        SQLException e = null;
        try {
          SQLXML value = delegate.getSQLXML(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public String getNString(String arg0) throws SQLException {
        SQLException e = null;
        try {
          String value = delegate.getNString(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public String getNString(int arg0) throws SQLException {
        SQLException e = null;
        try {
          String value = delegate.getNString(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public Reader getNCharacterStream(int arg0) throws SQLException {
        SQLException e = null;
        try {
          Reader value = delegate.getNCharacterStream(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public Reader getNCharacterStream(String arg0) throws SQLException {
        SQLException e = null;
        try {
          Reader value = delegate.getNCharacterStream(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public Reader getCharacterStream(String arg0) throws SQLException {
        SQLException e = null;
        try {
          Reader value = delegate.getCharacterStream(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public Reader getCharacterStream(int arg0) throws SQLException {
        SQLException e = null;
        try {
          Reader value = delegate.getCharacterStream(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public int findColumn(String arg0) throws SQLException {
        try { return delegate.findColumn(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Deprecated(
            forRemoval = false,
            since = "1.2"
    )
    @Override
    public InputStream getUnicodeStream(String arg0) throws SQLException {
        SQLException e = null;
        try {
          InputStream value = delegate.getUnicodeStream(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Deprecated(
            forRemoval = false,
            since = "1.2"
    )
    @Override
    public InputStream getUnicodeStream(int arg0) throws SQLException {
        SQLException e = null;
        try {
          InputStream value = delegate.getUnicodeStream(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public String getCursorName() throws SQLException {
        try { return delegate.getCursorName(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean isBeforeFirst() throws SQLException {
        try { return delegate.isBeforeFirst(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean isAfterLast() throws SQLException {
        try { return delegate.isAfterLast(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean isFirst() throws SQLException {
        try { return delegate.isFirst(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean isLast() throws SQLException {
        try { return delegate.isLast(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void beforeFirst() throws SQLException {
        try {  delegate.beforeFirst(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void afterLast() throws SQLException {
        try {  delegate.afterLast(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getRow() throws SQLException {
        try { return delegate.getRow(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getConcurrency() throws SQLException {
        try { return delegate.getConcurrency(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean rowUpdated() throws SQLException {
        try { return delegate.rowUpdated(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean rowInserted() throws SQLException {
        try { return delegate.rowInserted(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean rowDeleted() throws SQLException {
        try { return delegate.rowDeleted(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateNull(String arg0) throws SQLException {
        try {  delegate.updateNull(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateNull(int arg0) throws SQLException {
        try {  delegate.updateNull(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateBoolean(String arg0, boolean arg1) throws SQLException {
        try {  delegate.updateBoolean(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateBoolean(int arg0, boolean arg1) throws SQLException {
        try {  delegate.updateBoolean(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateByte(int arg0, byte arg1) throws SQLException {
        try {  delegate.updateByte(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateByte(String arg0, byte arg1) throws SQLException {
        try {  delegate.updateByte(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateShort(int arg0, short arg1) throws SQLException {
        try {  delegate.updateShort(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateShort(String arg0, short arg1) throws SQLException {
        try {  delegate.updateShort(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateInt(int arg0, int arg1) throws SQLException {
        try {  delegate.updateInt(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateInt(String arg0, int arg1) throws SQLException {
        try {  delegate.updateInt(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateLong(String arg0, long arg1) throws SQLException {
        try {  delegate.updateLong(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateLong(int arg0, long arg1) throws SQLException {
        try {  delegate.updateLong(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateFloat(String arg0, float arg1) throws SQLException {
        try {  delegate.updateFloat(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateFloat(int arg0, float arg1) throws SQLException {
        try {  delegate.updateFloat(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateDouble(String arg0, double arg1) throws SQLException {
        try {  delegate.updateDouble(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateDouble(int arg0, double arg1) throws SQLException {
        try {  delegate.updateDouble(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateBigDecimal(String arg0, BigDecimal arg1) throws SQLException {
        try {  delegate.updateBigDecimal(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateBigDecimal(int arg0, BigDecimal arg1) throws SQLException {
        try {  delegate.updateBigDecimal(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateString(int arg0, String arg1) throws SQLException {
        try {  delegate.updateString(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateString(String arg0, String arg1) throws SQLException {
        try {  delegate.updateString(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateDate(String arg0, Date arg1) throws SQLException {
        try {  delegate.updateDate(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateDate(int arg0, Date arg1) throws SQLException {
        try {  delegate.updateDate(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateTime(String arg0, Time arg1) throws SQLException {
        try {  delegate.updateTime(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateTime(int arg0, Time arg1) throws SQLException {
        try {  delegate.updateTime(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateTimestamp(String arg0, Timestamp arg1) throws SQLException {
        try {  delegate.updateTimestamp(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateTimestamp(int arg0, Timestamp arg1) throws SQLException {
        try {  delegate.updateTimestamp(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateAsciiStream(String arg0, InputStream arg1, long arg2) throws SQLException {
        try {  delegate.updateAsciiStream(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateAsciiStream(String arg0, InputStream arg1, int arg2) throws SQLException {
        try {  delegate.updateAsciiStream(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateAsciiStream(String arg0, InputStream arg1) throws SQLException {
        try {  delegate.updateAsciiStream(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateAsciiStream(int arg0, InputStream arg1, long arg2) throws SQLException {
        try {  delegate.updateAsciiStream(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateAsciiStream(int arg0, InputStream arg1, int arg2) throws SQLException {
        try {  delegate.updateAsciiStream(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateAsciiStream(int arg0, InputStream arg1) throws SQLException {
        try {  delegate.updateAsciiStream(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateBinaryStream(String arg0, InputStream arg1, int arg2) throws SQLException {
        try {  delegate.updateBinaryStream(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateBinaryStream(int arg0, InputStream arg1, int arg2) throws SQLException {
        try {  delegate.updateBinaryStream(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateBinaryStream(String arg0, InputStream arg1) throws SQLException {
        try {  delegate.updateBinaryStream(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateBinaryStream(String arg0, InputStream arg1, long arg2) throws SQLException {
        try {  delegate.updateBinaryStream(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateBinaryStream(int arg0, InputStream arg1, long arg2) throws SQLException {
        try {  delegate.updateBinaryStream(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateBinaryStream(int arg0, InputStream arg1) throws SQLException {
        try {  delegate.updateBinaryStream(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateCharacterStream(String arg0, Reader arg1) throws SQLException {
        try {  delegate.updateCharacterStream(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateCharacterStream(String arg0, Reader arg1, int arg2) throws SQLException {
        try {  delegate.updateCharacterStream(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateCharacterStream(String arg0, Reader arg1, long arg2) throws SQLException {
        try {  delegate.updateCharacterStream(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateCharacterStream(int arg0, Reader arg1) throws SQLException {
        try {  delegate.updateCharacterStream(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateCharacterStream(int arg0, Reader arg1, long arg2) throws SQLException {
        try {  delegate.updateCharacterStream(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateCharacterStream(int arg0, Reader arg1, int arg2) throws SQLException {
        try {  delegate.updateCharacterStream(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateObject(String arg0, Object arg1) throws SQLException {
        try {  delegate.updateObject(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateObject(String arg0, Object arg1, int arg2) throws SQLException {
        try {  delegate.updateObject(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateObject(int arg0, Object arg1, SQLType arg2, int arg3) throws SQLException {
        try {  delegate.updateObject(arg0, arg1, arg2, arg3); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateObject(String arg0, Object arg1, SQLType arg2, int arg3) throws SQLException {
        try {  delegate.updateObject(arg0, arg1, arg2, arg3); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateObject(int arg0, Object arg1, SQLType arg2) throws SQLException {
        try {  delegate.updateObject(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateObject(int arg0, Object arg1) throws SQLException {
        try {  delegate.updateObject(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateObject(String arg0, Object arg1, SQLType arg2) throws SQLException {
        try {  delegate.updateObject(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateObject(int arg0, Object arg1, int arg2) throws SQLException {
        try {  delegate.updateObject(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void insertRow() throws SQLException {
        try {  super.insertRow(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateRow() throws SQLException {
        try {  super.updateRow(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void deleteRow() throws SQLException {
        try {  super.deleteRow(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void refreshRow() throws SQLException {
        try {  delegate.refreshRow(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void cancelRowUpdates() throws SQLException {
        try {  delegate.cancelRowUpdates(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void moveToInsertRow() throws SQLException {
        try {  delegate.moveToInsertRow(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void moveToCurrentRow() throws SQLException {
        try {  delegate.moveToCurrentRow(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateRef(String arg0, Ref arg1) throws SQLException {
        try {  delegate.updateRef(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateRef(int arg0, Ref arg1) throws SQLException {
        try {  delegate.updateRef(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateBlob(String arg0, InputStream arg1) throws SQLException {
        try {  delegate.updateBlob(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateBlob(String arg0, Blob arg1) throws SQLException {
        try {  delegate.updateBlob(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateBlob(int arg0, Blob arg1) throws SQLException {
        try {  delegate.updateBlob(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateBlob(int arg0, InputStream arg1, long arg2) throws SQLException {
        try {  delegate.updateBlob(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateBlob(String arg0, InputStream arg1, long arg2) throws SQLException {
        try {  delegate.updateBlob(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateBlob(int arg0, InputStream arg1) throws SQLException {
        try {  delegate.updateBlob(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateClob(int arg0, Clob arg1) throws SQLException {
        try {  delegate.updateClob(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateClob(int arg0, Reader arg1) throws SQLException {
        try {  delegate.updateClob(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateClob(String arg0, Reader arg1) throws SQLException {
        try {  delegate.updateClob(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateClob(int arg0, Reader arg1, long arg2) throws SQLException {
        try {  delegate.updateClob(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateClob(String arg0, Reader arg1, long arg2) throws SQLException {
        try {  delegate.updateClob(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateClob(String arg0, Clob arg1) throws SQLException {
        try {  delegate.updateClob(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateArray(int arg0, Array arg1) throws SQLException {
        try {  delegate.updateArray(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateArray(String arg0, Array arg1) throws SQLException {
        try {  delegate.updateArray(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateRowId(String arg0, RowId arg1) throws SQLException {
        try {  delegate.updateRowId(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateRowId(int arg0, RowId arg1) throws SQLException {
        try {  delegate.updateRowId(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateNString(String arg0, String arg1) throws SQLException {
        try {  delegate.updateNString(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateNString(int arg0, String arg1) throws SQLException {
        try {  delegate.updateNString(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateNClob(int arg0, Reader arg1, long arg2) throws SQLException {
        try {  delegate.updateNClob(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateNClob(String arg0, NClob arg1) throws SQLException {
        try {  delegate.updateNClob(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateNClob(String arg0, Reader arg1, long arg2) throws SQLException {
        try {  delegate.updateNClob(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateNClob(int arg0, NClob arg1) throws SQLException {
        try {  delegate.updateNClob(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateNClob(int arg0, Reader arg1) throws SQLException {
        try {  delegate.updateNClob(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateNClob(String arg0, Reader arg1) throws SQLException {
        try {  delegate.updateNClob(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateSQLXML(int arg0, SQLXML arg1) throws SQLException {
        try {  delegate.updateSQLXML(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateSQLXML(String arg0, SQLXML arg1) throws SQLException {
        try {  delegate.updateSQLXML(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateNCharacterStream(String arg0, Reader arg1) throws SQLException {
        try {  delegate.updateNCharacterStream(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateNCharacterStream(int arg0, Reader arg1) throws SQLException {
        try {  delegate.updateNCharacterStream(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateNCharacterStream(int arg0, Reader arg1, long arg2) throws SQLException {
        try {  delegate.updateNCharacterStream(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateNCharacterStream(String arg0, Reader arg1, long arg2) throws SQLException {
        try {  delegate.updateNCharacterStream(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateBytes(String arg0, byte[] arg1) throws SQLException {
        try {  delegate.updateBytes(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void updateBytes(int arg0, byte[] arg1) throws SQLException {
        try {  delegate.updateBytes(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Object getObject(int arg0, Map<String, Class<?>> arg1) throws SQLException {
        SQLException e = null;
        try {
          Object value = delegate.getObject(arg0, arg1);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public <T> T getObject(int arg0, Class<T> arg1) throws SQLException {
        SQLException e = null;
        try {
          T value = delegate.getObject(arg0, arg1);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public Object getObject(int arg0) throws SQLException {
        SQLException e = null;
        try {
          Object value = delegate.getObject(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public Object getObject(String arg0) throws SQLException {
        SQLException e = null;
        try {
          Object value = delegate.getObject(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public <T> T getObject(String arg0, Class<T> arg1) throws SQLException {
        SQLException e = null;
        try {
          T value = delegate.getObject(arg0, arg1);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public Object getObject(String arg0, Map<String, Class<?>> arg1) throws SQLException {
        SQLException e = null;
        try {
          Object value = delegate.getObject(arg0, arg1);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public boolean getBoolean(int arg0) throws SQLException {
        SQLException e = null;
        try {
          boolean value = delegate.getBoolean(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public boolean getBoolean(String arg0) throws SQLException {
        SQLException e = null;
        try {
          boolean value = delegate.getBoolean(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public byte getByte(String arg0) throws SQLException {
        SQLException e = null;
        try {
          byte value = delegate.getByte(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public byte getByte(int arg0) throws SQLException {
        SQLException e = null;
        try {
          byte value = delegate.getByte(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public short getShort(int arg0) throws SQLException {
        SQLException e = null;
        try {
          short value = delegate.getShort(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public short getShort(String arg0) throws SQLException {
        SQLException e = null;
        try {
          short value = delegate.getShort(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public int getInt(int arg0) throws SQLException {
        SQLException e = null;
        try {
          int value = delegate.getInt(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public int getInt(String arg0) throws SQLException {
        SQLException e = null;
        try {
          int value = delegate.getInt(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public long getLong(int arg0) throws SQLException {
        SQLException e = null;
        try {
          long value = delegate.getLong(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public long getLong(String arg0) throws SQLException {
        SQLException e = null;
        try {
          long value = delegate.getLong(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public float getFloat(String arg0) throws SQLException {
        SQLException e = null;
        try {
          float value = delegate.getFloat(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public float getFloat(int arg0) throws SQLException {
        SQLException e = null;
        try {
          float value = delegate.getFloat(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public double getDouble(int arg0) throws SQLException {
        SQLException e = null;
        try {
          double value = delegate.getDouble(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public double getDouble(String arg0) throws SQLException {
        SQLException e = null;
        try {
          double value = delegate.getDouble(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public byte[] getBytes(int arg0) throws SQLException {
        SQLException e = null;
        try {
          byte[] value = delegate.getBytes(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public byte[] getBytes(String arg0) throws SQLException {
        SQLException e = null;
        try {
          byte[] value = delegate.getBytes(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public boolean last() throws SQLException {
        try { return delegate.last(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean next() throws SQLException {
        SQLException e = null;
        long start = System.nanoTime();
        boolean next = false;
        try {
          jdbcEventListener.onBeforeResultSetNext(resultSetInformation);
          next = delegate.next();
          return next;
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterResultSetNext(resultSetInformation, System.nanoTime() - start, next, e);
        }
    }

    @Override
    public boolean first() throws SQLException {
        try { return delegate.first(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public void close() throws SQLException {
        SQLException e = null;
        try {
          delegate.close();
        } catch (SQLException sqle) {
          e = sqle;
          throw checkException(e);
        } finally {
          jdbcEventListener.onAfterResultSetClose(resultSetInformation, e);
        }
    }

    @Override
    public int getType() throws SQLException {
        try { return delegate.getType(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public Ref getRef(int arg0) throws SQLException {
        SQLException e = null;
        try {
          Ref value = delegate.getRef(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public Ref getRef(String arg0) throws SQLException {
        SQLException e = null;
        try {
          Ref value = delegate.getRef(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public boolean previous() throws SQLException {
        try { return delegate.previous(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        try { return delegate.getMetaData(); } catch (SQLException e) { throw checkException(e); }
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
    public int getHoldability() throws SQLException {
        try { return delegate.getHoldability(); } catch (SQLException e) { throw checkException(e); }
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
    public boolean isClosed() throws SQLException {
        try { return delegate.isClosed(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public InputStream getAsciiStream(String arg0) throws SQLException {
        SQLException e = null;
        try {
          InputStream value = delegate.getAsciiStream(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public InputStream getAsciiStream(int arg0) throws SQLException {
        SQLException e = null;
        try {
          InputStream value = delegate.getAsciiStream(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public InputStream getBinaryStream(String arg0) throws SQLException {
        SQLException e = null;
        try {
          InputStream value = delegate.getBinaryStream(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public InputStream getBinaryStream(int arg0) throws SQLException {
        SQLException e = null;
        try {
          InputStream value = delegate.getBinaryStream(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public Date getDate(int arg0, Calendar arg1) throws SQLException {
        SQLException e = null;
        try {
          Date value = delegate.getDate(arg0, arg1);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public Date getDate(int arg0) throws SQLException {
        SQLException e = null;
        try {
          Date value = delegate.getDate(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public Date getDate(String arg0) throws SQLException {
        SQLException e = null;
        try {
          Date value = delegate.getDate(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public Date getDate(String arg0, Calendar arg1) throws SQLException {
        SQLException e = null;
        try {
          Date value = delegate.getDate(arg0, arg1);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public boolean relative(int arg0) throws SQLException {
        try { return delegate.relative(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean absolute(int arg0) throws SQLException {
        try { return delegate.absolute(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public URL getURL(String arg0) throws SQLException {
        SQLException e = null;
        try {
          URL value = delegate.getURL(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public URL getURL(int arg0) throws SQLException {
        SQLException e = null;
        try {
          URL value = delegate.getURL(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public String getString(String arg0) throws SQLException {
        SQLException e = null;
        try {
          String value = delegate.getString(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }

    @Override
    public String getString(int arg0) throws SQLException {
        SQLException e = null;
        try {
          String value = delegate.getString(arg0);
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, value, null);
          return value;
        } catch (SQLException sqle) {
          e = sqle;
          jdbcEventListener.onAfterResultSetGet(resultSetInformation, arg0, null, e);
          throw checkException(e);
        }
    }
}
