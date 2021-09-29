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

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.Map;

public abstract class BaseResultSetHandler<T> implements ResultSetHandler<T> {

    private ResultSet rs;

    @Override
    public final T handle(final ResultSet rs) throws SQLException {
        if (this.rs != null) {
            throw new IllegalStateException("Re-entry not allowed!");
        }

        this.rs = rs;

        try {
            return handle();
        } finally {
            this.rs = null;
        }
    }

    protected abstract T handle() throws SQLException;

    protected final boolean absolute(final int row) throws SQLException {
        return rs.absolute(row);
    }

    protected final void afterLast() throws SQLException {
        rs.afterLast();
    }

    protected final void beforeFirst() throws SQLException {
        rs.beforeFirst();
    }

    protected final void cancelRowUpdates() throws SQLException {
        rs.cancelRowUpdates();
    }

    protected final void clearWarnings() throws SQLException {
        rs.clearWarnings();
    }

    protected final void close() throws SQLException {
        rs.close();
    }

    protected final void deleteRow() throws SQLException {
        rs.deleteRow();
    }

    protected final int findColumn(final String columnLabel) throws SQLException {
        return rs.findColumn(columnLabel);
    }

    protected final boolean first() throws SQLException {
        return rs.first();
    }

    protected final Array getArray(final int columnIndex) throws SQLException {
        return rs.getArray(columnIndex);
    }

    protected final Array getArray(final String columnLabel) throws SQLException {
        return rs.getArray(columnLabel);
    }

    protected final InputStream getAsciiStream(final int columnIndex) throws SQLException {
        return rs.getAsciiStream(columnIndex);
    }

    protected final InputStream getAsciiStream(final String columnLabel) throws SQLException {
        return rs.getAsciiStream(columnLabel);
    }

    @Deprecated
    protected final BigDecimal getBigDecimal(final int columnIndex, final int scale) throws SQLException {
        return rs.getBigDecimal(columnIndex, scale);
    }

    protected final BigDecimal getBigDecimal(final int columnIndex) throws SQLException {
        return rs.getBigDecimal(columnIndex);
    }

    @Deprecated
    protected final BigDecimal getBigDecimal(final String columnLabel, final int scale) throws SQLException {
        return rs.getBigDecimal(columnLabel, scale);
    }

    protected final BigDecimal getBigDecimal(final String columnLabel) throws SQLException {
        return rs.getBigDecimal(columnLabel);
    }

    protected final InputStream getBinaryStream(final int columnIndex) throws SQLException {
        return rs.getBinaryStream(columnIndex);
    }

    protected final InputStream getBinaryStream(final String columnLabel) throws SQLException {
        return rs.getBinaryStream(columnLabel);
    }

    protected final Blob getBlob(final int columnIndex) throws SQLException {
        return rs.getBlob(columnIndex);
    }

    protected final Blob getBlob(final String columnLabel) throws SQLException {
        return rs.getBlob(columnLabel);
    }

    protected final boolean getBoolean(final int columnIndex) throws SQLException {
        return rs.getBoolean(columnIndex);
    }

    protected final boolean getBoolean(final String columnLabel) throws SQLException {
        return rs.getBoolean(columnLabel);
    }

    protected final byte getByte(final int columnIndex) throws SQLException {
        return rs.getByte(columnIndex);
    }

    protected final byte getByte(final String columnLabel) throws SQLException {
        return rs.getByte(columnLabel);
    }

    protected final byte[] getBytes(final int columnIndex) throws SQLException {
        return rs.getBytes(columnIndex);
    }

    protected final byte[] getBytes(final String columnLabel) throws SQLException {
        return rs.getBytes(columnLabel);
    }

    protected final Reader getCharacterStream(final int columnIndex) throws SQLException {
        return rs.getCharacterStream(columnIndex);
    }

    protected final Reader getCharacterStream(final String columnLabel) throws SQLException {
        return rs.getCharacterStream(columnLabel);
    }

    protected final Clob getClob(final int columnIndex) throws SQLException {
        return rs.getClob(columnIndex);
    }

    protected final Clob getClob(final String columnLabel) throws SQLException {
        return rs.getClob(columnLabel);
    }

    protected final int getConcurrency() throws SQLException {
        return rs.getConcurrency();
    }

    protected final String getCursorName() throws SQLException {
        return rs.getCursorName();
    }

    protected final Date getDate(final int columnIndex, final Calendar cal) throws SQLException {
        return rs.getDate(columnIndex, cal);
    }

    protected final Date getDate(final int columnIndex) throws SQLException {
        return rs.getDate(columnIndex);
    }

    protected final Date getDate(final String columnLabel, final Calendar cal) throws SQLException {
        return rs.getDate(columnLabel, cal);
    }

    protected final Date getDate(final String columnLabel) throws SQLException {
        return rs.getDate(columnLabel);
    }

    protected final double getDouble(final int columnIndex) throws SQLException {
        return rs.getDouble(columnIndex);
    }

    protected final double getDouble(final String columnLabel) throws SQLException {
        return rs.getDouble(columnLabel);
    }

    protected final int getFetchDirection() throws SQLException {
        return rs.getFetchDirection();
    }

    protected final int getFetchSize() throws SQLException {
        return rs.getFetchSize();
    }

    protected final float getFloat(final int columnIndex) throws SQLException {
        return rs.getFloat(columnIndex);
    }

    protected final float getFloat(final String columnLabel) throws SQLException {
        return rs.getFloat(columnLabel);
    }

    protected final int getHoldability() throws SQLException {
        return rs.getHoldability();
    }

    protected final int getInt(final int columnIndex) throws SQLException {
        return rs.getInt(columnIndex);
    }

    protected final int getInt(final String columnLabel) throws SQLException {
        return rs.getInt(columnLabel);
    }

    protected final long getLong(final int columnIndex) throws SQLException {
        return rs.getLong(columnIndex);
    }

    protected final long getLong(final String columnLabel) throws SQLException {
        return rs.getLong(columnLabel);
    }

    protected final ResultSetMetaData getMetaData() throws SQLException {
        return rs.getMetaData();
    }

    protected final Reader getNCharacterStream(final int columnIndex) throws SQLException {
        return rs.getNCharacterStream(columnIndex);
    }

    protected final Reader getNCharacterStream(final String columnLabel) throws SQLException {
        return rs.getNCharacterStream(columnLabel);
    }

    protected final NClob getNClob(final int columnIndex) throws SQLException {
        return rs.getNClob(columnIndex);
    }

    protected final NClob getNClob(final String columnLabel) throws SQLException {
        return rs.getNClob(columnLabel);
    }

    protected final String getNString(final int columnIndex) throws SQLException {
        return rs.getNString(columnIndex);
    }

    protected final String getNString(final String columnLabel) throws SQLException {
        return rs.getNString(columnLabel);
    }

    protected final Object getObject(final int columnIndex, final Map<String, Class<?>> map) throws SQLException {
        return rs.getObject(columnIndex, map);
    }

    protected final Object getObject(final int columnIndex) throws SQLException {
        return rs.getObject(columnIndex);
    }

    protected final Object getObject(final String columnLabel, final Map<String, Class<?>> map) throws SQLException {
        return rs.getObject(columnLabel, map);
    }

    protected final Object getObject(final String columnLabel) throws SQLException {
        return rs.getObject(columnLabel);
    }

    protected final Ref getRef(final int columnIndex) throws SQLException {
        return rs.getRef(columnIndex);
    }

    protected final Ref getRef(final String columnLabel) throws SQLException {
        return rs.getRef(columnLabel);
    }

    protected final int getRow() throws SQLException {
        return rs.getRow();
    }

    protected final RowId getRowId(final int columnIndex) throws SQLException {
        return rs.getRowId(columnIndex);
    }

    protected final RowId getRowId(final String columnLabel) throws SQLException {
        return rs.getRowId(columnLabel);
    }

    protected final SQLXML getSQLXML(final int columnIndex) throws SQLException {
        return rs.getSQLXML(columnIndex);
    }

    protected final SQLXML getSQLXML(final String columnLabel) throws SQLException {
        return rs.getSQLXML(columnLabel);
    }

    protected final short getShort(final int columnIndex) throws SQLException {
        return rs.getShort(columnIndex);
    }

    protected final short getShort(final String columnLabel) throws SQLException {
        return rs.getShort(columnLabel);
    }

    protected final Statement getStatement() throws SQLException {
        return rs.getStatement();
    }

    protected final String getString(final int columnIndex) throws SQLException {
        return rs.getString(columnIndex);
    }

    protected final String getString(final String columnLabel) throws SQLException {
        return rs.getString(columnLabel);
    }

    protected final Time getTime(final int columnIndex, final Calendar cal) throws SQLException {
        return rs.getTime(columnIndex, cal);
    }

    protected final Time getTime(final int columnIndex) throws SQLException {
        return rs.getTime(columnIndex);
    }

    protected final Time getTime(final String columnLabel, final Calendar cal) throws SQLException {
        return rs.getTime(columnLabel, cal);
    }

    protected final Time getTime(final String columnLabel) throws SQLException {
        return rs.getTime(columnLabel);
    }

    protected final Timestamp getTimestamp(final int columnIndex, final Calendar cal) throws SQLException {
        return rs.getTimestamp(columnIndex, cal);
    }

    protected final Timestamp getTimestamp(final int columnIndex) throws SQLException {
        return rs.getTimestamp(columnIndex);
    }

    protected final Timestamp getTimestamp(final String columnLabel, final Calendar cal) throws SQLException {
        return rs.getTimestamp(columnLabel, cal);
    }

    protected final Timestamp getTimestamp(final String columnLabel) throws SQLException {
        return rs.getTimestamp(columnLabel);
    }

    protected final int getType() throws SQLException {
        return rs.getType();
    }

    protected final URL getURL(final int columnIndex) throws SQLException {
        return rs.getURL(columnIndex);
    }

    protected final URL getURL(final String columnLabel) throws SQLException {
        return rs.getURL(columnLabel);
    }

    @Deprecated
    protected final InputStream getUnicodeStream(final int columnIndex) throws SQLException {
        return rs.getUnicodeStream(columnIndex);
    }

    @Deprecated
    protected final InputStream getUnicodeStream(final String columnLabel) throws SQLException {
        return rs.getUnicodeStream(columnLabel);
    }

    protected final SQLWarning getWarnings() throws SQLException {
        return rs.getWarnings();
    }

    protected final void insertRow() throws SQLException {
        rs.insertRow();
    }

    protected final boolean isAfterLast() throws SQLException {
        return rs.isAfterLast();
    }

    protected final boolean isBeforeFirst() throws SQLException {
        return rs.isBeforeFirst();
    }

    protected final boolean isClosed() throws SQLException {
        return rs.isClosed();
    }

    protected final boolean isFirst() throws SQLException {
        return rs.isFirst();
    }

    protected final boolean isLast() throws SQLException {
        return rs.isLast();
    }

    protected final boolean isWrapperFor(final Class<?> iface) throws SQLException {
        return rs.isWrapperFor(iface);
    }

    protected final boolean last() throws SQLException {
        return rs.last();
    }

    protected final void moveToCurrentRow() throws SQLException {
        rs.moveToCurrentRow();
    }

    protected final void moveToInsertRow() throws SQLException {
        rs.moveToInsertRow();
    }

    protected final boolean next() throws SQLException {
        return rs.next();
    }

    protected final boolean previous() throws SQLException {
        return rs.previous();
    }

    protected final void refreshRow() throws SQLException {
        rs.refreshRow();
    }

    protected final boolean relative(final int rows) throws SQLException {
        return rs.relative(rows);
    }

    protected final boolean rowDeleted() throws SQLException {
        return rs.rowDeleted();
    }

    protected final boolean rowInserted() throws SQLException {
        return rs.rowInserted();
    }

    protected final boolean rowUpdated() throws SQLException {
        return rs.rowUpdated();
    }

    protected final void setFetchDirection(final int direction) throws SQLException {
        rs.setFetchDirection(direction);
    }

    protected final void setFetchSize(final int rows) throws SQLException {
        rs.setFetchSize(rows);
    }

    protected final <E> E unwrap(final Class<E> iface) throws SQLException {
        return rs.unwrap(iface);
    }

    protected final void updateArray(final int columnIndex, final Array x) throws SQLException {
        rs.updateArray(columnIndex, x);
    }

    protected final void updateArray(final String columnLabel, final Array x) throws SQLException {
        rs.updateArray(columnLabel, x);
    }

    protected final void updateAsciiStream(final int columnIndex, final InputStream x, final int length) throws SQLException {
        rs.updateAsciiStream(columnIndex, x, length);
    }

    protected final void updateAsciiStream(final int columnIndex, final InputStream x, final long length) throws SQLException {
        rs.updateAsciiStream(columnIndex, x, length);
    }

    protected final void updateAsciiStream(final int columnIndex, final InputStream x) throws SQLException {
        rs.updateAsciiStream(columnIndex, x);
    }

    protected final void updateAsciiStream(final String columnLabel, final InputStream x, final int length) throws SQLException {
        rs.updateAsciiStream(columnLabel, x, length);
    }

    protected final void updateAsciiStream(final String columnLabel, final InputStream x, final long length) throws SQLException {
        rs.updateAsciiStream(columnLabel, x, length);
    }

    protected final void updateAsciiStream(final String columnLabel, final InputStream x) throws SQLException {
        rs.updateAsciiStream(columnLabel, x);
    }

    protected final void updateBigDecimal(final int columnIndex, final BigDecimal x) throws SQLException {
        rs.updateBigDecimal(columnIndex, x);
    }

    protected final void updateBigDecimal(final String columnLabel, final BigDecimal x) throws SQLException {
        rs.updateBigDecimal(columnLabel, x);
    }

    protected final void updateBinaryStream(final int columnIndex, final InputStream x, final int length) throws SQLException {
        rs.updateBinaryStream(columnIndex, x, length);
    }

    protected final void updateBinaryStream(final int columnIndex, final InputStream x, final long length) throws SQLException {
        rs.updateBinaryStream(columnIndex, x, length);
    }

    protected final void updateBinaryStream(final int columnIndex, final InputStream x) throws SQLException {
        rs.updateBinaryStream(columnIndex, x);
    }

    protected final void updateBinaryStream(final String columnLabel, final InputStream x, final int length) throws SQLException {
        rs.updateBinaryStream(columnLabel, x, length);
    }

    protected final void updateBinaryStream(final String columnLabel, final InputStream x, final long length) throws SQLException {
        rs.updateBinaryStream(columnLabel, x, length);
    }

    protected final void updateBinaryStream(final String columnLabel, final InputStream x) throws SQLException {
        rs.updateBinaryStream(columnLabel, x);
    }

    protected final void updateBlob(final int columnIndex, final Blob x) throws SQLException {
        rs.updateBlob(columnIndex, x);
    }

    protected final void updateBlob(final int columnIndex, final InputStream inputStream, final long length) throws SQLException {
        rs.updateBlob(columnIndex, inputStream, length);
    }

    protected final void updateBlob(final int columnIndex, final InputStream inputStream) throws SQLException {
        rs.updateBlob(columnIndex, inputStream);
    }

    protected final void updateBlob(final String columnLabel, final Blob x) throws SQLException {
        rs.updateBlob(columnLabel, x);
    }

    protected final void updateBlob(final String columnLabel, final InputStream inputStream, final long length) throws SQLException {
        rs.updateBlob(columnLabel, inputStream, length);
    }

    protected final void updateBlob(final String columnLabel, final InputStream inputStream) throws SQLException {
        rs.updateBlob(columnLabel, inputStream);
    }

    protected final void updateBoolean(final int columnIndex, final boolean x) throws SQLException {
        rs.updateBoolean(columnIndex, x);
    }

    protected final void updateBoolean(final String columnLabel, final boolean x) throws SQLException {
        rs.updateBoolean(columnLabel, x);
    }

    protected final void updateByte(final int columnIndex, final byte x) throws SQLException {
        rs.updateByte(columnIndex, x);
    }

    protected final void updateByte(final String columnLabel, final byte x) throws SQLException {
        rs.updateByte(columnLabel, x);
    }

    protected final void updateBytes(final int columnIndex, final byte[] x) throws SQLException {
        rs.updateBytes(columnIndex, x);
    }

    protected final void updateBytes(final String columnLabel, final byte[] x) throws SQLException {
        rs.updateBytes(columnLabel, x);
    }

    protected final void updateCharacterStream(final int columnIndex, final Reader x, final int length) throws SQLException {
        rs.updateCharacterStream(columnIndex, x, length);
    }

    protected final void updateCharacterStream(final int columnIndex, final Reader x, final long length) throws SQLException {
        rs.updateCharacterStream(columnIndex, x, length);
    }

    protected final void updateCharacterStream(final int columnIndex, final Reader x) throws SQLException {
        rs.updateCharacterStream(columnIndex, x);
    }

    protected final void updateCharacterStream(final String columnLabel, final Reader reader, final int length) throws SQLException {
        rs.updateCharacterStream(columnLabel, reader, length);
    }

    protected final void updateCharacterStream(final String columnLabel, final Reader reader, final long length) throws SQLException {
        rs.updateCharacterStream(columnLabel, reader, length);
    }

    protected final void updateCharacterStream(final String columnLabel, final Reader reader) throws SQLException {
        rs.updateCharacterStream(columnLabel, reader);
    }

    protected final void updateClob(final int columnIndex, final Clob x) throws SQLException {
        rs.updateClob(columnIndex, x);
    }

    protected final void updateClob(final int columnIndex, final Reader reader, final long length) throws SQLException {
        rs.updateClob(columnIndex, reader, length);
    }

    protected final void updateClob(final int columnIndex, final Reader reader) throws SQLException {
        rs.updateClob(columnIndex, reader);
    }

    protected final void updateClob(final String columnLabel, final Clob x) throws SQLException {
        rs.updateClob(columnLabel, x);
    }

    protected final void updateClob(final String columnLabel, final Reader reader, final long length) throws SQLException {
        rs.updateClob(columnLabel, reader, length);
    }

    protected final void updateClob(final String columnLabel, final Reader reader) throws SQLException {
        rs.updateClob(columnLabel, reader);
    }

    protected final void updateDate(final int columnIndex, final Date x) throws SQLException {
        rs.updateDate(columnIndex, x);
    }

    protected final void updateDate(final String columnLabel, final Date x) throws SQLException {
        rs.updateDate(columnLabel, x);
    }

    protected final void updateDouble(final int columnIndex, final double x) throws SQLException {
        rs.updateDouble(columnIndex, x);
    }

    protected final void updateDouble(final String columnLabel, final double x) throws SQLException {
        rs.updateDouble(columnLabel, x);
    }

    protected final void updateFloat(final int columnIndex, final float x) throws SQLException {
        rs.updateFloat(columnIndex, x);
    }

    protected final void updateFloat(final String columnLabel, final float x) throws SQLException {
        rs.updateFloat(columnLabel, x);
    }

    protected final void updateInt(final int columnIndex, final int x) throws SQLException {
        rs.updateInt(columnIndex, x);
    }

    protected final void updateInt(final String columnLabel, final int x) throws SQLException {
        rs.updateInt(columnLabel, x);
    }

    protected final void updateLong(final int columnIndex, final long x) throws SQLException {
        rs.updateLong(columnIndex, x);
    }

    protected final void updateLong(final String columnLabel, final long x) throws SQLException {
        rs.updateLong(columnLabel, x);
    }

    protected final void updateNCharacterStream(final int columnIndex, final Reader x, final long length) throws SQLException {
        rs.updateNCharacterStream(columnIndex, x, length);
    }

    protected final void updateNCharacterStream(final int columnIndex, final Reader x) throws SQLException {
        rs.updateNCharacterStream(columnIndex, x);
    }

    protected final void updateNCharacterStream(final String columnLabel, final Reader reader, final long length) throws SQLException {
        rs.updateNCharacterStream(columnLabel, reader, length);
    }

    protected final void updateNCharacterStream(final String columnLabel, final Reader reader) throws SQLException {
        rs.updateNCharacterStream(columnLabel, reader);
    }

    protected final void updateNClob(final int columnIndex, final NClob nClob) throws SQLException {
        rs.updateNClob(columnIndex, nClob);
    }

    protected final void updateNClob(final int columnIndex, final Reader reader, final long length) throws SQLException {
        rs.updateNClob(columnIndex, reader, length);
    }

    protected final void updateNClob(final int columnIndex, final Reader reader) throws SQLException {
        rs.updateNClob(columnIndex, reader);
    }

    protected final void updateNClob(final String columnLabel, final NClob nClob) throws SQLException {
        rs.updateNClob(columnLabel, nClob);
    }

    protected final void updateNClob(final String columnLabel, final Reader reader, final long length) throws SQLException {
        rs.updateNClob(columnLabel, reader, length);
    }

    protected final void updateNClob(final String columnLabel, final Reader reader) throws SQLException {
        rs.updateNClob(columnLabel, reader);
    }

    protected final void updateNString(final int columnIndex, final String nString) throws SQLException {
        rs.updateNString(columnIndex, nString);
    }

    protected final void updateNString(final String columnLabel, final String nString) throws SQLException {
        rs.updateNString(columnLabel, nString);
    }

    protected final void updateNull(final int columnIndex) throws SQLException {
        rs.updateNull(columnIndex);
    }

    protected final void updateNull(final String columnLabel) throws SQLException {
        rs.updateNull(columnLabel);
    }

    protected final void updateObject(final int columnIndex, final Object x, final int scaleOrLength) throws SQLException {
        rs.updateObject(columnIndex, x, scaleOrLength);
    }

    protected final void updateObject(final int columnIndex, final Object x) throws SQLException {
        rs.updateObject(columnIndex, x);
    }

    protected final void updateObject(final String columnLabel, final Object x, final int scaleOrLength) throws SQLException {
        rs.updateObject(columnLabel, x, scaleOrLength);
    }

    protected final void updateObject(final String columnLabel, final Object x) throws SQLException {
        rs.updateObject(columnLabel, x);
    }

    protected final void updateRef(final int columnIndex, final Ref x) throws SQLException {
        rs.updateRef(columnIndex, x);
    }

    protected final void updateRef(final String columnLabel, final Ref x) throws SQLException {
        rs.updateRef(columnLabel, x);
    }

    protected final void updateRow() throws SQLException {
        rs.updateRow();
    }

    protected final void updateRowId(final int columnIndex, final RowId x) throws SQLException {
        rs.updateRowId(columnIndex, x);
    }

    protected final void updateRowId(final String columnLabel, final RowId x) throws SQLException {
        rs.updateRowId(columnLabel, x);
    }

    protected final void updateSQLXML(final int columnIndex, final SQLXML xmlObject) throws SQLException {
        rs.updateSQLXML(columnIndex, xmlObject);
    }

    protected final void updateSQLXML(final String columnLabel, final SQLXML xmlObject) throws SQLException {
        rs.updateSQLXML(columnLabel, xmlObject);
    }

    protected final void updateShort(final int columnIndex, final short x) throws SQLException {
        rs.updateShort(columnIndex, x);
    }

    protected final void updateShort(final String columnLabel, final short x) throws SQLException {
        rs.updateShort(columnLabel, x);
    }

    protected final void updateString(final int columnIndex, final String x) throws SQLException {
        rs.updateString(columnIndex, x);
    }

    protected final void updateString(final String columnLabel, final String x) throws SQLException {
        rs.updateString(columnLabel, x);
    }

    protected final void updateTime(final int columnIndex, final Time x) throws SQLException {
        rs.updateTime(columnIndex, x);
    }

    protected final void updateTime(final String columnLabel, final Time x) throws SQLException {
        rs.updateTime(columnLabel, x);
    }

    protected final void updateTimestamp(final int columnIndex, final Timestamp x) throws SQLException {
        rs.updateTimestamp(columnIndex, x);
    }

    protected final void updateTimestamp(final String columnLabel, final Timestamp x) throws SQLException {
        rs.updateTimestamp(columnLabel, x);
    }

    protected final boolean wasNull() throws SQLException {
        return rs.wasNull();
    }

    protected final ResultSet getAdaptedResultSet() {
        return rs;
    }

}
