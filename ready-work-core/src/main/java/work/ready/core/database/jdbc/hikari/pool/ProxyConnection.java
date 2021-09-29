/*
 * Copyright (C) 2013, 2014 Brett Wooldridge
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package work.ready.core.database.jdbc.hikari.pool;

import work.ready.core.database.jdbc.hikari.SQLExceptionOverride;
import work.ready.core.database.jdbc.hikari.util.FastList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;

import static work.ready.core.database.jdbc.hikari.SQLExceptionOverride.Override.DO_NOT_EVICT;
import static work.ready.core.database.jdbc.hikari.util.ClockSource.currentTime;

public abstract class ProxyConnection implements Connection
{
   static final int DIRTY_BIT_READONLY   = 0b000001;
   static final int DIRTY_BIT_AUTOCOMMIT = 0b000010;
   static final int DIRTY_BIT_ISOLATION  = 0b000100;
   static final int DIRTY_BIT_CATALOG    = 0b001000;
   static final int DIRTY_BIT_NETTIMEOUT = 0b010000;
   static final int DIRTY_BIT_SCHEMA     = 0b100000;

   private static final Logger LOGGER;
   private static final Set<String> ERROR_STATES;
   private static final Set<Integer> ERROR_CODES;

   @SuppressWarnings("WeakerAccess")
   protected Connection delegate;

   private final PoolEntry poolEntry;
   private final ProxyLeakTask leakTask;
   private final FastList<Statement> openStatements;

   private int dirtyBits;
   private long lastAccess;
   private boolean isCommitStateDirty;

   private boolean isReadOnly;
   private boolean isAutoCommit;
   private int networkTimeout;
   private int transactionIsolation;
   private String dbcatalog;
   private String dbschema;

   static {
      LOGGER = LoggerFactory.getLogger(ProxyConnection.class);

      ERROR_STATES = new HashSet<>();
      ERROR_STATES.add("0A000"); 
      ERROR_STATES.add("57P01"); 
      ERROR_STATES.add("57P02"); 
      ERROR_STATES.add("57P03"); 
      ERROR_STATES.add("01002"); 
      ERROR_STATES.add("JZ0C0"); 
      ERROR_STATES.add("JZ0C1"); 

      ERROR_CODES = new HashSet<>();
      ERROR_CODES.add(500150);
      ERROR_CODES.add(2399);
   }

   protected ProxyConnection(final PoolEntry poolEntry,
                             final Connection connection,
                             final FastList<Statement> openStatements,
                             final ProxyLeakTask leakTask,
                             final long now,
                             final boolean isReadOnly,
                             final boolean isAutoCommit) {
      this.poolEntry = poolEntry;
      this.delegate = connection;
      this.openStatements = openStatements;
      this.leakTask = leakTask;
      this.lastAccess = now;
      this.isReadOnly = isReadOnly;
      this.isAutoCommit = isAutoCommit;
   }

   @Override
   public final String toString()
   {
      return this.getClass().getSimpleName() + '@' + System.identityHashCode(this) + " wrapping " + delegate;
   }

   final boolean getAutoCommitState()
   {
      return isAutoCommit;
   }

   final String getCatalogState()
   {
      return dbcatalog;
   }

   final String getSchemaState()
   {
      return dbschema;
   }

   final int getTransactionIsolationState()
   {
      return transactionIsolation;
   }

   final boolean getReadOnlyState()
   {
      return isReadOnly;
   }

   final int getNetworkTimeoutState()
   {
      return networkTimeout;
   }

   final PoolEntry getPoolEntry()
   {
      return poolEntry;
   }

   @SuppressWarnings("ConstantConditions")
   final SQLException checkException(SQLException sqle)
   {
      boolean evict = false;
      SQLException nse = sqle;
      final SQLExceptionOverride exceptionOverride = poolEntry.getPoolBase().exceptionOverride;
      for (int depth = 0; delegate != ClosedConnection.CLOSED_CONNECTION && nse != null && depth < 10; depth++) {
         final String sqlState = nse.getSQLState();
         if (sqlState != null && sqlState.startsWith("08")
             || nse instanceof SQLTimeoutException
             || ERROR_STATES.contains(sqlState)
             || ERROR_CODES.contains(nse.getErrorCode())) {

            if (exceptionOverride != null && exceptionOverride.adjudicate(nse) == DO_NOT_EVICT) {
               break;
            }

            evict = true;
            break;
         }
         else {
            nse = nse.getNextException();
         }
      }

      if (evict) {
         SQLException exception = (nse != null) ? nse : sqle;
         LOGGER.warn("{} - Connection {} marked as broken because of SQLSTATE({}), ErrorCode({})",
            poolEntry.getPoolName(), delegate, exception.getSQLState(), exception.getErrorCode(), exception);
         leakTask.cancel();
         poolEntry.evict("(connection is broken)");
         delegate = ClosedConnection.CLOSED_CONNECTION;
      }

      return sqle;
   }

   final synchronized void untrackStatement(final Statement statement)
   {
      openStatements.remove(statement);
   }

   final void markCommitStateDirty()
   {
      if (isAutoCommit) {
         lastAccess = currentTime();
      }
      else {
         isCommitStateDirty = true;
      }
   }

   void cancelLeakTask()
   {
      leakTask.cancel();
   }

   private synchronized <T extends Statement> T trackStatement(final T statement)
   {
      openStatements.add(statement);

      return statement;
   }

   @SuppressWarnings("EmptyTryBlock")
   private synchronized void closeStatements()
   {
      final int size = openStatements.size();
      if (size > 0) {
         for (int i = 0; i < size && delegate != ClosedConnection.CLOSED_CONNECTION; i++) {
            try (Statement ignored = openStatements.get(i)) {
               
            }
            catch (SQLException e) {
               LOGGER.warn("{} - Connection {} marked as broken because of an exception closing open statements during Connection.close()",
                           poolEntry.getPoolName(), delegate);
               leakTask.cancel();
               poolEntry.evict("(exception closing Statements during Connection.close())");
               delegate = ClosedConnection.CLOSED_CONNECTION;
            }
         }

         openStatements.clear();
      }
   }

   @Override
   public void close() throws SQLException
   {
      
      closeStatements();

      if (delegate != ClosedConnection.CLOSED_CONNECTION) {
         leakTask.cancel();

         try {
            if (isCommitStateDirty && !isAutoCommit) {
               delegate.rollback();
               lastAccess = currentTime();
               LOGGER.debug("{} - Executed rollback on connection {} due to dirty commit state on close().", poolEntry.getPoolName(), delegate);
            }

            if (dirtyBits != 0) {
               poolEntry.resetConnectionState(this, dirtyBits);
               lastAccess = currentTime();
            }

            delegate.clearWarnings();
         }
         catch (SQLException e) {
            
            if (!poolEntry.isMarkedEvicted()) {
               throw checkException(e);
            }
         }
         finally {
            delegate = ClosedConnection.CLOSED_CONNECTION;
            poolEntry.recycle(lastAccess);
         }
      }
   }

   @Override
   @SuppressWarnings("RedundantThrows")
   public boolean isClosed() throws SQLException
   {
      return (delegate == ClosedConnection.CLOSED_CONNECTION);
   }

   @Override
   public Statement createStatement() throws SQLException
   {
      return ProxyFactory.getProxyStatement(this, trackStatement(delegate.createStatement()));
   }

   @Override
   public Statement createStatement(int resultSetType, int concurrency) throws SQLException
   {
      return ProxyFactory.getProxyStatement(this, trackStatement(delegate.createStatement(resultSetType, concurrency)));
   }

   @Override
   public Statement createStatement(int resultSetType, int concurrency, int holdability) throws SQLException
   {
      return ProxyFactory.getProxyStatement(this, trackStatement(delegate.createStatement(resultSetType, concurrency, holdability)));
   }

   @Override
   public CallableStatement prepareCall(String sql) throws SQLException
   {
      return ProxyFactory.getProxyCallableStatement(this, sql, trackStatement(delegate.prepareCall(sql)));
   }

   @Override
   public CallableStatement prepareCall(String sql, int resultSetType, int concurrency) throws SQLException
   {
      return ProxyFactory.getProxyCallableStatement(this, sql, trackStatement(delegate.prepareCall(sql, resultSetType, concurrency)));
   }

   @Override
   public CallableStatement prepareCall(String sql, int resultSetType, int concurrency, int holdability) throws SQLException
   {
      return ProxyFactory.getProxyCallableStatement(this, sql, trackStatement(delegate.prepareCall(sql, resultSetType, concurrency, holdability)));
   }

   @Override
   public PreparedStatement prepareStatement(String sql) throws SQLException
   {
      return ProxyFactory.getProxyPreparedStatement(this, sql, trackStatement(delegate.prepareStatement(sql)));
   }

   @Override
   public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException
   {
      return ProxyFactory.getProxyPreparedStatement(this, sql, trackStatement(delegate.prepareStatement(sql, autoGeneratedKeys)));
   }

   @Override
   public PreparedStatement prepareStatement(String sql, int resultSetType, int concurrency) throws SQLException
   {
      return ProxyFactory.getProxyPreparedStatement(this, sql, trackStatement(delegate.prepareStatement(sql, resultSetType, concurrency)));
   }

   @Override
   public PreparedStatement prepareStatement(String sql, int resultSetType, int concurrency, int holdability) throws SQLException
   {
      return ProxyFactory.getProxyPreparedStatement(this, sql, trackStatement(delegate.prepareStatement(sql, resultSetType, concurrency, holdability)));
   }

   @Override
   public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException
   {
      return ProxyFactory.getProxyPreparedStatement(this, sql, trackStatement(delegate.prepareStatement(sql, columnIndexes)));
   }

   @Override
   public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException
   {
      return ProxyFactory.getProxyPreparedStatement(this, sql, trackStatement(delegate.prepareStatement(sql, columnNames)));
   }

   @Override
   public DatabaseMetaData getMetaData() throws SQLException
   {
      markCommitStateDirty();
      return ProxyFactory.getProxyDatabaseMetaData(this, delegate.getMetaData());
   }

   @Override
   public void commit() throws SQLException
   {
      delegate.commit();
      isCommitStateDirty = false;
      lastAccess = currentTime();
   }

   @Override
   public void rollback() throws SQLException
   {
      delegate.rollback();
      isCommitStateDirty = false;
      lastAccess = currentTime();
   }

   @Override
   public void rollback(Savepoint savepoint) throws SQLException
   {
      delegate.rollback(savepoint);
      isCommitStateDirty = false;
      lastAccess = currentTime();
   }

   @Override
   public void setAutoCommit(boolean autoCommit) throws SQLException
   {
      delegate.setAutoCommit(autoCommit);
      isAutoCommit = autoCommit;
      dirtyBits |= DIRTY_BIT_AUTOCOMMIT;
   }

   @Override
   public void setReadOnly(boolean readOnly) throws SQLException
   {
      delegate.setReadOnly(readOnly);
      isReadOnly = readOnly;
      isCommitStateDirty = false;
      dirtyBits |= DIRTY_BIT_READONLY;
   }

   @Override
   public void setTransactionIsolation(int level) throws SQLException
   {
      delegate.setTransactionIsolation(level);
      transactionIsolation = level;
      dirtyBits |= DIRTY_BIT_ISOLATION;
   }

   @Override
   public void setCatalog(String catalog) throws SQLException
   {
      delegate.setCatalog(catalog);
      dbcatalog = catalog;
      dirtyBits |= DIRTY_BIT_CATALOG;
   }

   @Override
   public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException
   {
      delegate.setNetworkTimeout(executor, milliseconds);
      networkTimeout = milliseconds;
      dirtyBits |= DIRTY_BIT_NETTIMEOUT;
   }

   @Override
   public void setSchema(String schema) throws SQLException
   {
      delegate.setSchema(schema);
      dbschema = schema;
      dirtyBits |= DIRTY_BIT_SCHEMA;
   }

   @Override
   public final boolean isWrapperFor(Class<?> iface) throws SQLException
   {
      return iface.isInstance(delegate) || (delegate != null && delegate.isWrapperFor(iface));
   }

   @Override
   @SuppressWarnings("unchecked")
   public final <T> T unwrap(Class<T> iface) throws SQLException
   {
      if (iface.isInstance(delegate)) {
         return (T) delegate;
      }
      else if (delegate != null) {
          return delegate.unwrap(iface);
      }

      throw new SQLException("Wrapped connection is not an instance of " + iface);
   }

   private static final class ClosedConnection
   {
      static final Connection CLOSED_CONNECTION = getClosedConnection();

      private static Connection getClosedConnection()
      {
         InvocationHandler handler = (proxy, method, args) -> {
            final String methodName = method.getName();
            if ("isClosed".equals(methodName)) {
               return Boolean.TRUE;
            }
            else if ("isValid".equals(methodName)) {
               return Boolean.FALSE;
            }
            if ("abort".equals(methodName)) {
               return Void.TYPE;
            }
            if ("close".equals(methodName)) {
               return Void.TYPE;
            }
            else if ("toString".equals(methodName)) {
               return ClosedConnection.class.getCanonicalName();
            }

            throw new SQLException("Connection is closed");
         };

         return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class[] { Connection.class }, handler);
      }
   }
}
