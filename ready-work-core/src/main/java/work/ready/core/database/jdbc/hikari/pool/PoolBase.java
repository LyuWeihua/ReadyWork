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

import work.ready.core.database.jdbc.hikari.HikariConfig;
import work.ready.core.database.jdbc.hikari.SQLExceptionOverride;
import work.ready.core.database.jdbc.hikari.pool.HikariPool.PoolInitializationException;
import work.ready.core.database.jdbc.hikari.util.DriverDataSource;
import work.ready.core.database.jdbc.hikari.util.PropertyElf;
import work.ready.core.database.jdbc.hikari.util.UtilityElf;
import work.ready.core.database.jdbc.hikari.util.UtilityElf.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;

import static work.ready.core.database.jdbc.hikari.pool.ProxyConnection.*;
import static work.ready.core.database.jdbc.hikari.util.ClockSource.*;
import static work.ready.core.database.jdbc.hikari.util.UtilityElf.createInstance;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

abstract class PoolBase
{
   private final Logger logger = LoggerFactory.getLogger(PoolBase.class);

   public final HikariConfig config;
   IMetricsTrackerDelegate metricsTracker = new NopMetricsTrackerDelegate();

   protected final String poolName;

   volatile String catalog;
   final AtomicReference<Exception> lastConnectionFailure;

   long connectionTimeout;
   long validationTimeout;

   SQLExceptionOverride exceptionOverride;

   private static final String[] RESET_STATES = {"readOnly", "autoCommit", "isolation", "catalog", "netTimeout", "schema"};
   private static final int UNINITIALIZED = -1;
   private static final int TRUE = 1;
   private static final int FALSE = 0;

   private int networkTimeout;
   private int isNetworkTimeoutSupported;
   private int isQueryTimeoutSupported;
   private int defaultTransactionIsolation;
   private int transactionIsolation;
   private Executor netTimeoutExecutor;
   private DataSource dataSource;

   private final String schema;
   private final boolean isReadOnly;
   private final boolean isAutoCommit;

   private final boolean isUseJdbc4Validation;
   private final boolean isIsolateInternalQueries;

   private volatile boolean isValidChecked;

   PoolBase(final HikariConfig config)
   {
      this.config = config;

      this.networkTimeout = UNINITIALIZED;
      this.catalog = config.getCatalog();
      this.schema = config.getSchema();
      this.isReadOnly = config.isReadOnly();
      this.isAutoCommit = config.isAutoCommit();
      this.exceptionOverride = UtilityElf.createInstance(config.getExceptionOverrideClassName(), SQLExceptionOverride.class);
      this.transactionIsolation = UtilityElf.getTransactionIsolation(config.getTransactionIsolation());

      this.isQueryTimeoutSupported = UNINITIALIZED;
      this.isNetworkTimeoutSupported = UNINITIALIZED;
      this.isUseJdbc4Validation = config.getConnectionTestQuery() == null;
      this.isIsolateInternalQueries = config.isIsolateInternalQueries();

      this.poolName = config.getPoolName();
      this.connectionTimeout = config.getConnectionTimeout();
      this.validationTimeout = config.getValidationTimeout();
      this.lastConnectionFailure = new AtomicReference<>();

      initializeDataSource();
   }

   @Override
   public String toString()
   {
      return poolName;
   }

   abstract void recycle(final PoolEntry poolEntry);

   void quietlyCloseConnection(final Connection connection, final String closureReason)
   {
      if (connection != null) {
         try {
            logger.debug("{} - Closing connection {}: {}", poolName, connection, closureReason);

            try {
               setNetworkTimeout(connection, SECONDS.toMillis(15));
            }
            catch (SQLException e) {
               
            }
            finally {
               connection.close(); 
            }
         }
         catch (Exception e) {
            logger.debug("{} - Closing connection {} failed", poolName, connection, e);
         }
      }
   }

   boolean isConnectionAlive(final Connection connection)
   {
      try {
         try {
            setNetworkTimeout(connection, validationTimeout);

            final int validationSeconds = (int) Math.max(1000L, validationTimeout) / 1000;

            if (isUseJdbc4Validation) {
               return connection.isValid(validationSeconds);
            }

            try (Statement statement = connection.createStatement()) {
               if (isNetworkTimeoutSupported != TRUE) {
                  setQueryTimeout(statement, validationSeconds);
               }

               statement.execute(config.getConnectionTestQuery());
            }
         }
         finally {
            setNetworkTimeout(connection, networkTimeout);

            if (isIsolateInternalQueries && !isAutoCommit) {
               connection.rollback();
            }
         }

         return true;
      }
      catch (Exception e) {
         lastConnectionFailure.set(e);
         logger.warn("{} - Failed to validate connection {} ({}). Possibly consider using a shorter maxLifetime value.",
                     poolName, connection, e.getMessage());
         return false;
      }
   }

   Exception getLastConnectionFailure()
   {
      return lastConnectionFailure.get();
   }

   public DataSource getUnwrappedDataSource()
   {
      return dataSource;
   }

   PoolEntry newPoolEntry() throws Exception
   {
      return new PoolEntry(newConnection(), this, isReadOnly, isAutoCommit);
   }

   void resetConnectionState(final Connection connection, final ProxyConnection proxyConnection, final int dirtyBits) throws SQLException
   {
      int resetBits = 0;

      if ((dirtyBits & DIRTY_BIT_READONLY) != 0 && proxyConnection.getReadOnlyState() != isReadOnly) {
         connection.setReadOnly(isReadOnly);
         resetBits |= DIRTY_BIT_READONLY;
      }

      if ((dirtyBits & DIRTY_BIT_AUTOCOMMIT) != 0 && proxyConnection.getAutoCommitState() != isAutoCommit) {
         connection.setAutoCommit(isAutoCommit);
         resetBits |= DIRTY_BIT_AUTOCOMMIT;
      }

      if ((dirtyBits & DIRTY_BIT_ISOLATION) != 0 && proxyConnection.getTransactionIsolationState() != transactionIsolation) {
         connection.setTransactionIsolation(transactionIsolation);
         resetBits |= DIRTY_BIT_ISOLATION;
      }

      if ((dirtyBits & DIRTY_BIT_CATALOG) != 0 && catalog != null && !catalog.equals(proxyConnection.getCatalogState())) {
         connection.setCatalog(catalog);
         resetBits |= DIRTY_BIT_CATALOG;
      }

      if ((dirtyBits & DIRTY_BIT_NETTIMEOUT) != 0 && proxyConnection.getNetworkTimeoutState() != networkTimeout) {
         setNetworkTimeout(connection, networkTimeout);
         resetBits |= DIRTY_BIT_NETTIMEOUT;
      }

      if ((dirtyBits & DIRTY_BIT_SCHEMA) != 0 && schema != null && !schema.equals(proxyConnection.getSchemaState())) {
         connection.setSchema(schema);
         resetBits |= DIRTY_BIT_SCHEMA;
      }

      if (resetBits != 0 && logger.isDebugEnabled()) {
         logger.debug("{} - Reset ({}) on connection {}", poolName, stringFromResetBits(resetBits), connection);
      }
   }

   void shutdownNetworkTimeoutExecutor()
   {
      if (netTimeoutExecutor instanceof ThreadPoolExecutor) {
         ((ThreadPoolExecutor) netTimeoutExecutor).shutdownNow();
      }
   }

   long getLoginTimeout()
   {
      try {
         return (dataSource != null) ? dataSource.getLoginTimeout() : SECONDS.toSeconds(5);
      } catch (SQLException e) {
         return SECONDS.toSeconds(5);
      }
   }

   void handleMBeans(final HikariPool hikariPool, final boolean register)
   {
      if (!config.isRegisterMbeans()) {
         return;
      }

      try {
         final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

         ObjectName beanConfigName, beanPoolName;
         if ("true".equals(System.getProperty("hikaricp.jmx.register2.0"))) {
             beanConfigName = new ObjectName("com.zaxxer.hikari:type=PoolConfig,name=" + poolName);
             beanPoolName = new ObjectName("com.zaxxer.hikari:type=Pool,name=" + poolName);
         } else {
            beanConfigName = new ObjectName("com.zaxxer.hikari:type=PoolConfig (" + poolName + ")");
            beanPoolName = new ObjectName("com.zaxxer.hikari:type=Pool (" + poolName + ")");
         }
         if (register) {
            if (!mBeanServer.isRegistered(beanConfigName)) {
               mBeanServer.registerMBean(config, beanConfigName);
               mBeanServer.registerMBean(hikariPool, beanPoolName);
            } else {
               logger.error("{} - JMX name ({}) is already registered.", poolName, poolName);
            }
         }
         else if (mBeanServer.isRegistered(beanConfigName)) {
            mBeanServer.unregisterMBean(beanConfigName);
            mBeanServer.unregisterMBean(beanPoolName);
         }
      }
      catch (Exception e) {
         logger.warn("{} - Failed to {} management beans.", poolName, (register ? "register" : "unregister"), e);
      }
   }

   private void initializeDataSource()
   {
      final String jdbcUrl = config.getJdbcUrl();
      final String username = config.getUsername();
      final String password = config.getPassword();
      final String dsClassName = config.getDataSourceClassName();
      final String driverClassName = config.getDriverClassName();
      final String dataSourceJNDI = config.getDataSourceJNDI();
      final Properties dataSourceProperties = config.getDataSourceProperties();

      DataSource ds = config.getDataSource();
      if (dsClassName != null && ds == null) {
         ds = createInstance(dsClassName, DataSource.class);
         PropertyElf.setTargetFromProperties(ds, dataSourceProperties);
      }
      else if (jdbcUrl != null && ds == null) {
         ds = new DriverDataSource(jdbcUrl, driverClassName, dataSourceProperties, username, password);
      }
      else if (dataSourceJNDI != null && ds == null) {
         try {
            InitialContext ic = new InitialContext();
            ds = (DataSource) ic.lookup(dataSourceJNDI);
         } catch (NamingException e) {
            throw new PoolInitializationException(e);
         }
      }

      if (ds != null) {
         setLoginTimeout(ds);
         createNetworkTimeoutExecutor(ds, dsClassName, jdbcUrl);
      }

      this.dataSource = ds;
   }

   private Connection newConnection() throws Exception
   {
      final long start = currentTime();

      Connection connection = null;
      try {
         String username = config.getUsername();
         String password = config.getPassword();

         connection = (username == null) ? dataSource.getConnection() : dataSource.getConnection(username, password);
         if (connection == null) {
            throw new SQLTransientConnectionException("DataSource returned null unexpectedly");
         }

         setupConnection(connection);
         lastConnectionFailure.set(null);
         return connection;
      }
      catch (Exception e) {
         if (connection != null) {
            quietlyCloseConnection(connection, "(Failed to create/setup connection)");
         }
         else if (getLastConnectionFailure() == null) {
            logger.debug("{} - Failed to create/setup connection: {}", poolName, e.getMessage());
         }

         lastConnectionFailure.set(e);
         throw e;
      }
      finally {
         
         if (metricsTracker != null) {
            metricsTracker.recordConnectionCreated(elapsedMillis(start));
         }
      }
   }

   private void setupConnection(final Connection connection) throws ConnectionSetupException
   {
      try {
         if (networkTimeout == UNINITIALIZED) {
            networkTimeout = getAndSetNetworkTimeout(connection, validationTimeout);
         }
         else {
            setNetworkTimeout(connection, validationTimeout);
         }

         if (connection.isReadOnly() != isReadOnly) {
            connection.setReadOnly(isReadOnly);
         }

         if (connection.getAutoCommit() != isAutoCommit) {
            connection.setAutoCommit(isAutoCommit);
         }

         checkDriverSupport(connection);

         if (transactionIsolation != defaultTransactionIsolation) {
            connection.setTransactionIsolation(transactionIsolation);
         }

         if (catalog != null) {
            connection.setCatalog(catalog);
         }

         if (schema != null) {
            connection.setSchema(schema);
         }

         executeSql(connection, config.getConnectionInitSql(), true);

         setNetworkTimeout(connection, networkTimeout);
      }
      catch (SQLException e) {
         throw new ConnectionSetupException(e);
      }
   }

   private void checkDriverSupport(final Connection connection) throws SQLException
   {
      if (!isValidChecked) {
         checkValidationSupport(connection);
         checkDefaultIsolation(connection);

         isValidChecked = true;
      }
   }

   private void checkValidationSupport(final Connection connection) throws SQLException
   {
      try {
         if (isUseJdbc4Validation) {
            connection.isValid(1);
         }
         else {
            executeSql(connection, config.getConnectionTestQuery(), false);
         }
      }
      catch (Exception | AbstractMethodError e) {
         logger.error("{} - Failed to execute{} connection test query ({}).", poolName, (isUseJdbc4Validation ? " isValid() for connection, configure" : ""), e.getMessage());
         throw e;
      }
   }

   private void checkDefaultIsolation(final Connection connection) throws SQLException
   {
      try {
         defaultTransactionIsolation = connection.getTransactionIsolation();
         if (transactionIsolation == -1) {
            transactionIsolation = defaultTransactionIsolation;
         }
      }
      catch (SQLException e) {
         logger.warn("{} - Default transaction isolation level detection failed ({}).", poolName, e.getMessage());
         if (e.getSQLState() != null && !e.getSQLState().startsWith("08")) {
            throw e;
         }
      }
   }

   private void setQueryTimeout(final Statement statement, final int timeoutSec)
   {
      if (isQueryTimeoutSupported != FALSE) {
         try {
            statement.setQueryTimeout(timeoutSec);
            isQueryTimeoutSupported = TRUE;
         }
         catch (Exception e) {
            if (isQueryTimeoutSupported == UNINITIALIZED) {
               isQueryTimeoutSupported = FALSE;
               logger.info("{} - Failed to set query timeout for statement. ({})", poolName, e.getMessage());
            }
         }
      }
   }

   private int getAndSetNetworkTimeout(final Connection connection, final long timeoutMs)
   {
      if (isNetworkTimeoutSupported != FALSE) {
         try {
            final int originalTimeout = connection.getNetworkTimeout();
            connection.setNetworkTimeout(netTimeoutExecutor, (int) timeoutMs);
            isNetworkTimeoutSupported = TRUE;
            return originalTimeout;
         }
         catch (Exception | AbstractMethodError e) {
            if (isNetworkTimeoutSupported == UNINITIALIZED) {
               isNetworkTimeoutSupported = FALSE;

               logger.info("{} - Driver does not support get/set network timeout for connections. ({})", poolName, e.getMessage());
               if (validationTimeout < SECONDS.toMillis(1)) {
                  logger.warn("{} - A validationTimeout of less than 1 second cannot be honored on drivers without setNetworkTimeout() support.", poolName);
               }
               else if (validationTimeout % SECONDS.toMillis(1) != 0) {
                  logger.warn("{} - A validationTimeout with fractional second granularity cannot be honored on drivers without setNetworkTimeout() support.", poolName);
               }
            }
         }
      }

      return 0;
   }

   private void setNetworkTimeout(final Connection connection, final long timeoutMs) throws SQLException
   {
      if (isNetworkTimeoutSupported == TRUE) {
         connection.setNetworkTimeout(netTimeoutExecutor, (int) timeoutMs);
      }
   }

   private void executeSql(final Connection connection, final String sql, final boolean isCommit) throws SQLException
   {
      if (sql != null) {
         try (Statement statement = connection.createStatement()) {
            
            statement.execute(sql);
         }

         if (isIsolateInternalQueries && !isAutoCommit) {
            if (isCommit) {
               connection.commit();
            }
            else {
               connection.rollback();
            }
         }
      }
   }

   private void createNetworkTimeoutExecutor(final DataSource dataSource, final String dsClassName, final String jdbcUrl)
   {
      
      if ((dsClassName != null && dsClassName.contains("Mysql")) ||
          (jdbcUrl != null && jdbcUrl.contains("mysql")) ||
          (dataSource != null && dataSource.getClass().getName().contains("Mysql"))) {
         netTimeoutExecutor = new SynchronousExecutor();
      }
      else {
         ThreadFactory threadFactory = config.getThreadFactory();
         threadFactory = threadFactory != null ? threadFactory : new DefaultThreadFactory(poolName + " network timeout executor", true);
         ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool(threadFactory);
         executor.setKeepAliveTime(15, SECONDS);
         executor.allowCoreThreadTimeOut(true);
         netTimeoutExecutor = executor;
      }
   }

   private void setLoginTimeout(final DataSource dataSource)
   {
      if (connectionTimeout != Integer.MAX_VALUE) {
         try {
            dataSource.setLoginTimeout(Math.max(1, (int) MILLISECONDS.toSeconds(500L + connectionTimeout)));
         }
         catch (Exception e) {
            logger.info("{} - Failed to set login timeout for data source. ({})", poolName, e.getMessage());
         }
      }
   }

   private String stringFromResetBits(final int bits)
   {
      final StringBuilder sb = new StringBuilder();
      for (int ndx = 0; ndx < RESET_STATES.length; ndx++) {
         if ( (bits & (0b1 << ndx)) != 0) {
            sb.append(RESET_STATES[ndx]).append(", ");
         }
      }

      sb.setLength(sb.length() - 2);  
      return sb.toString();
   }

   static class ConnectionSetupException extends Exception
   {
      private static final long serialVersionUID = 929872118275916521L;

      ConnectionSetupException(Throwable t)
      {
         super(t);
      }
   }

   private static class SynchronousExecutor implements Executor
   {
      
      @Override
      public void execute(Runnable command)
      {
         try {
            command.run();
         }
         catch (Exception t) {
            LoggerFactory.getLogger(PoolBase.class).debug("Failed to execute: {}", command, t);
         }
      }
   }

   interface IMetricsTrackerDelegate extends AutoCloseable
   {
      default void recordConnectionUsage(PoolEntry poolEntry) {}

      default void recordConnectionCreated(long connectionCreatedMillis) {}

      default void recordBorrowTimeoutStats(long startTime) {}

      default void recordBorrowStats(final PoolEntry poolEntry, final long startTime) {}

      default void recordConnectionTimeout() {}

      @Override
      default void close() {}
   }

   static final class NopMetricsTrackerDelegate implements IMetricsTrackerDelegate {}
}
