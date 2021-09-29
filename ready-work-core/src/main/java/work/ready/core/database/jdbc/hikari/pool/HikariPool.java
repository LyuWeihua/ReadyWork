/*
 * Copyright (C) 2013,2014 Brett Wooldridge
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
import work.ready.core.database.jdbc.hikari.HikariPoolMXBean;
import work.ready.core.database.jdbc.hikari.util.ConcurrentBag;
import work.ready.core.database.jdbc.hikari.util.ConcurrentBag.IBagStateListener;
import work.ready.core.database.jdbc.hikari.util.SuspendResumeLock;
import work.ready.core.database.jdbc.hikari.util.UtilityElf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

import static work.ready.core.database.jdbc.hikari.util.ClockSource.*;
import static work.ready.core.database.jdbc.hikari.util.ConcurrentBag.IConcurrentBagEntry.STATE_IN_USE;
import static work.ready.core.database.jdbc.hikari.util.ConcurrentBag.IConcurrentBagEntry.STATE_NOT_IN_USE;
import static work.ready.core.database.jdbc.hikari.util.UtilityElf.*;
import static java.util.Collections.unmodifiableCollection;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public final class HikariPool extends PoolBase implements HikariPoolMXBean, IBagStateListener
{
   private final Logger logger = LoggerFactory.getLogger(HikariPool.class);

   public static final int POOL_NORMAL = 0;
   public static final int POOL_SUSPENDED = 1;
   public static final int POOL_SHUTDOWN = 2;

   public volatile int poolState;

   private final long aliveBypassWindowMs = Long.getLong("com.zaxxer.hikari.aliveBypassWindowMs", MILLISECONDS.toMillis(500));
   private final long housekeepingPeriodMs = Long.getLong("com.zaxxer.hikari.housekeeping.periodMs", SECONDS.toMillis(30));

   private static final String EVICTED_CONNECTION_MESSAGE = "(connection was evicted)";
   private static final String DEAD_CONNECTION_MESSAGE = "(connection is dead)";

   private final PoolEntryCreator poolEntryCreator = new PoolEntryCreator(null );
   private final PoolEntryCreator postFillPoolEntryCreator = new PoolEntryCreator("After adding ");
   private final Collection<Runnable> addConnectionQueueReadOnlyView;
   private final ThreadPoolExecutor addConnectionExecutor;
   private final ThreadPoolExecutor closeConnectionExecutor;

   private final ConcurrentBag<PoolEntry> connectionBag;

   private final ProxyLeakTaskFactory leakTaskFactory;
   private final SuspendResumeLock suspendResumeLock;

   private final ScheduledExecutorService houseKeepingExecutorService;
   private ScheduledFuture<?> houseKeeperTask;

   public HikariPool(final HikariConfig config)
   {
      super(config);

      this.connectionBag = new ConcurrentBag<>(this);
      this.suspendResumeLock = config.isAllowPoolSuspension() ? new SuspendResumeLock() : SuspendResumeLock.FAUX_LOCK;

      this.houseKeepingExecutorService = initializeHouseKeepingExecutorService();

      checkFailFast();

      handleMBeans(this, true);

      ThreadFactory threadFactory = config.getThreadFactory();

      final int maxPoolSize = config.getMaximumPoolSize();
      LinkedBlockingQueue<Runnable> addConnectionQueue = new LinkedBlockingQueue<>(maxPoolSize);
      this.addConnectionQueueReadOnlyView = unmodifiableCollection(addConnectionQueue);
      this.addConnectionExecutor = createThreadPoolExecutor(addConnectionQueue, poolName + " connection adder", threadFactory, new ThreadPoolExecutor.DiscardOldestPolicy());
      this.closeConnectionExecutor = createThreadPoolExecutor(maxPoolSize, poolName + " connection closer", threadFactory, new ThreadPoolExecutor.CallerRunsPolicy());

      this.leakTaskFactory = new ProxyLeakTaskFactory(config.getLeakDetectionThreshold(), houseKeepingExecutorService);

      this.houseKeeperTask = houseKeepingExecutorService.scheduleWithFixedDelay(new HouseKeeper(), 100L, housekeepingPeriodMs, MILLISECONDS);

      if (Boolean.getBoolean("com.zaxxer.hikari.blockUntilFilled") && config.getInitializationFailTimeout() > 1) {
         addConnectionExecutor.setMaximumPoolSize(Math.min(16, Runtime.getRuntime().availableProcessors()));
         addConnectionExecutor.setCorePoolSize(Math.min(16, Runtime.getRuntime().availableProcessors()));

         final long startTime = currentTime();
         while (elapsedMillis(startTime) < config.getInitializationFailTimeout() && getTotalConnections() < config.getMinimumIdle()) {
            quietlySleep(MILLISECONDS.toMillis(100));
         }

         addConnectionExecutor.setCorePoolSize(1);
         addConnectionExecutor.setMaximumPoolSize(1);
      }
   }

   public Connection getConnection() throws SQLException
   {
      return getConnection(connectionTimeout);
   }

   public Connection getConnection(final long hardTimeout) throws SQLException
   {
      suspendResumeLock.acquire();
      final long startTime = currentTime();

      try {
         long timeout = hardTimeout;
         do {
            PoolEntry poolEntry = connectionBag.borrow(timeout, MILLISECONDS);
            if (poolEntry == null) {
               break; 
            }

            final long now = currentTime();
            if (poolEntry.isMarkedEvicted() || (elapsedMillis(poolEntry.lastAccessed, now) > aliveBypassWindowMs && !isConnectionAlive(poolEntry.connection))) {
               closeConnection(poolEntry, poolEntry.isMarkedEvicted() ? EVICTED_CONNECTION_MESSAGE : DEAD_CONNECTION_MESSAGE);
               timeout = hardTimeout - elapsedMillis(startTime);
            }
            else {
               metricsTracker.recordBorrowStats(poolEntry, startTime);
               return poolEntry.createProxyConnection(leakTaskFactory.schedule(poolEntry), now);
            }
         } while (timeout > 0L);

         metricsTracker.recordBorrowTimeoutStats(startTime);
         throw createTimeoutException(startTime);
      }
      catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         throw new SQLException(poolName + " - Interrupted during connection acquisition", e);
      }
      finally {
         suspendResumeLock.release();
      }
   }

   public synchronized void shutdown() throws InterruptedException
   {
      try {
         poolState = POOL_SHUTDOWN;

         if (addConnectionExecutor == null) { 
            return;
         }

         logPoolState("Before shutdown ");

         if (houseKeeperTask != null) {
            houseKeeperTask.cancel(false);
            houseKeeperTask = null;
         }

         softEvictConnections();

         addConnectionExecutor.shutdown();
         addConnectionExecutor.awaitTermination(getLoginTimeout(), SECONDS);

         destroyHouseKeepingExecutorService();

         connectionBag.close();

         final ExecutorService assassinExecutor = createThreadPoolExecutor(config.getMaximumPoolSize(), poolName + " connection assassinator",
                                                                           config.getThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());
         try {
            final long start = currentTime();
            do {
               abortActiveConnections(assassinExecutor);
               softEvictConnections();
            } while (getTotalConnections() > 0 && elapsedMillis(start) < SECONDS.toMillis(10));
         }
         finally {
            assassinExecutor.shutdown();
            assassinExecutor.awaitTermination(10L, SECONDS);
         }

         shutdownNetworkTimeoutExecutor();
         closeConnectionExecutor.shutdown();
         closeConnectionExecutor.awaitTermination(10L, SECONDS);
      }
      finally {
         logPoolState("After shutdown ");
         handleMBeans(this, false);
         metricsTracker.close();
      }
   }

   public void evictConnection(Connection connection)
   {
      ProxyConnection proxyConnection = (ProxyConnection) connection;
      proxyConnection.cancelLeakTask();

      try {
         softEvictConnection(proxyConnection.getPoolEntry(), "(connection evicted by user)", !connection.isClosed() );
      }
      catch (SQLException e) {
         
      }
   }

   @Override
   public void addBagItem(final int waiting)
   {
      final boolean shouldAdd = waiting - addConnectionQueueReadOnlyView.size() >= 0; 
      if (shouldAdd) {
         addConnectionExecutor.submit(poolEntryCreator);
      }
      else {
         logger.debug("{} - Add connection elided, waiting {}, queue {}", poolName, waiting, addConnectionQueueReadOnlyView.size());
      }
   }

   @Override
   public int getActiveConnections()
   {
      return connectionBag.getCount(STATE_IN_USE);
   }

   @Override
   public int getIdleConnections()
   {
      return connectionBag.getCount(STATE_NOT_IN_USE);
   }

   @Override
   public int getTotalConnections()
   {
      return connectionBag.size();
   }

   @Override
   public int getThreadsAwaitingConnection()
   {
      return connectionBag.getWaitingThreadCount();
   }

   @Override
   public void softEvictConnections()
   {
      connectionBag.values().forEach(poolEntry -> softEvictConnection(poolEntry, "(connection evicted)", false ));
   }

   @Override
   public synchronized void suspendPool()
   {
      if (suspendResumeLock == SuspendResumeLock.FAUX_LOCK) {
         throw new IllegalStateException(poolName + " - is not suspendable");
      }
      else if (poolState != POOL_SUSPENDED) {
         suspendResumeLock.suspend();
         poolState = POOL_SUSPENDED;
      }
   }

   @Override
   public synchronized void resumePool()
   {
      if (poolState == POOL_SUSPENDED) {
         poolState = POOL_NORMAL;
         fillPool();
         suspendResumeLock.resume();
      }
   }

   void logPoolState(String... prefix)
   {
      if (logger.isDebugEnabled()) {
         logger.debug("{} - {}stats (total={}, active={}, idle={}, waiting={})",
                      poolName, (prefix.length > 0 ? prefix[0] : ""),
                      getTotalConnections(), getActiveConnections(), getIdleConnections(), getThreadsAwaitingConnection());
      }
   }

   @Override
   void recycle(final PoolEntry poolEntry)
   {
      metricsTracker.recordConnectionUsage(poolEntry);

      connectionBag.requite(poolEntry);
   }

   void closeConnection(final PoolEntry poolEntry, final String closureReason)
   {
      if (connectionBag.remove(poolEntry)) {
         final Connection connection = poolEntry.close();
         closeConnectionExecutor.execute(() -> {
            quietlyCloseConnection(connection, closureReason);
            if (poolState == POOL_NORMAL) {
               fillPool();
            }
         });
      }
   }

   int[] getPoolStateCounts()
   {
      return connectionBag.getStateCounts();
   }

   private PoolEntry createPoolEntry()
   {
      try {
         final PoolEntry poolEntry = newPoolEntry();

         final long maxLifetime = config.getMaxLifetime();
         if (maxLifetime > 0) {
            
            final long variance = maxLifetime > 10_000 ? ThreadLocalRandom.current().nextLong( maxLifetime / 40 ) : 0;
            final long lifetime = maxLifetime - variance;
            poolEntry.setFutureEol(houseKeepingExecutorService.schedule(new MaxLifetimeTask(poolEntry), lifetime, MILLISECONDS));
         }

         final long keepaliveTime = config.getKeepaliveTime();
         if (keepaliveTime > 0) {
            
            final long variance = ThreadLocalRandom.current().nextLong(keepaliveTime / 10);
            final long heartbeatTime = keepaliveTime - variance;
            poolEntry.setKeepalive(houseKeepingExecutorService.scheduleWithFixedDelay(new KeepaliveTask(poolEntry), heartbeatTime, heartbeatTime, MILLISECONDS));
         }

         return poolEntry;
      }
      catch (ConnectionSetupException e) {
         if (poolState == POOL_NORMAL) { 
            logger.error("{} - Error thrown while acquiring connection from data source", poolName, e.getCause());
            lastConnectionFailure.set(e);
         }
      }
      catch (Exception e) {
         if (poolState == POOL_NORMAL) { 
            logger.debug("{} - Cannot acquire connection from data source", poolName, e);
         }
      }

      return null;
   }

   private synchronized void fillPool()
   {
      final int connectionsToAdd = Math.min(config.getMaximumPoolSize() - getTotalConnections(), config.getMinimumIdle() - getIdleConnections())
                                   - addConnectionQueueReadOnlyView.size();
      if (connectionsToAdd <= 0) logger.debug("{} - Fill pool skipped, pool is at sufficient level.", poolName);

      for (int i = 0; i < connectionsToAdd; i++) {
         addConnectionExecutor.submit((i < connectionsToAdd - 1) ? poolEntryCreator : postFillPoolEntryCreator);
      }
   }

   private void abortActiveConnections(final ExecutorService assassinExecutor)
   {
      for (PoolEntry poolEntry : connectionBag.values(STATE_IN_USE)) {
         Connection connection = poolEntry.close();
         try {
            connection.abort(assassinExecutor);
         }
         catch (Throwable e) {
            quietlyCloseConnection(connection, "(connection aborted during shutdown)");
         }
         finally {
            connectionBag.remove(poolEntry);
         }
      }
   }

   private void checkFailFast()
   {
      final long initializationTimeout = config.getInitializationFailTimeout();
      if (initializationTimeout < 0) {
         return;
      }

      final long startTime = currentTime();
      do {
         final PoolEntry poolEntry = createPoolEntry();
         if (poolEntry != null) {
            if (config.getMinimumIdle() > 0) {
               connectionBag.add(poolEntry);
               logger.debug("{} - Added connection {}", poolName, poolEntry.connection);
            }
            else {
               quietlyCloseConnection(poolEntry.close(), "(initialization check complete and minimumIdle is zero)");
            }

            return;
         }

         if (getLastConnectionFailure() instanceof ConnectionSetupException) {
            throwPoolInitializationException(getLastConnectionFailure().getCause());
         }

         quietlySleep(SECONDS.toMillis(1));
      } while (elapsedMillis(startTime) < initializationTimeout);

      if (initializationTimeout > 0) {
         throwPoolInitializationException(getLastConnectionFailure());
      }
   }

   private void throwPoolInitializationException(Throwable t)
   {
      logger.error("{} - Exception during pool initialization.", poolName, t);
      destroyHouseKeepingExecutorService();
      throw new PoolInitializationException(t);
   }

   private boolean softEvictConnection(final PoolEntry poolEntry, final String reason, final boolean owner)
   {
      poolEntry.markEvicted();
      if (owner || connectionBag.reserve(poolEntry)) {
         closeConnection(poolEntry, reason);
         return true;
      }

      return false;
   }

   private ScheduledExecutorService initializeHouseKeepingExecutorService()
   {
      if (config.getScheduledExecutor() == null) {
         final ThreadFactory threadFactory = Optional.ofNullable(config.getThreadFactory()).orElseGet(() -> new DefaultThreadFactory(poolName + " housekeeper", true));
         final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, threadFactory, new ThreadPoolExecutor.DiscardPolicy());
         executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
         executor.setRemoveOnCancelPolicy(true);
         return executor;
      }
      else {
         return config.getScheduledExecutor();
      }
   }

   private void destroyHouseKeepingExecutorService()
   {
      if (config.getScheduledExecutor() == null) {
         houseKeepingExecutorService.shutdownNow();
      }
   }

   private SQLException createTimeoutException(long startTime)
   {
      logPoolState("Timeout failure ");
      metricsTracker.recordConnectionTimeout();

      String sqlState = null;
      final Throwable originalException = getLastConnectionFailure();
      if (originalException instanceof SQLException) {
         sqlState = ((SQLException) originalException).getSQLState();
      }
      final SQLException connectionException = new SQLTransientConnectionException(poolName + " - Connection is not available, request timed out after " + elapsedMillis(startTime) + "ms.", sqlState, originalException);
      if (originalException instanceof SQLException) {
         connectionException.setNextException((SQLException) originalException);
      }

      return connectionException;
   }

   private final class PoolEntryCreator implements Callable<Boolean>
   {
      private final String loggingPrefix;

      PoolEntryCreator(String loggingPrefix)
      {
         this.loggingPrefix = loggingPrefix;
      }

      @Override
      public Boolean call()
      {
         long sleepBackoff = 250L;
         while (poolState == POOL_NORMAL && shouldCreateAnotherConnection()) {
            final PoolEntry poolEntry = createPoolEntry();
            if (poolEntry != null) {
               connectionBag.add(poolEntry);
               logger.debug("{} - Added connection {}", poolName, poolEntry.connection);
               if (loggingPrefix != null) {
                  logPoolState(loggingPrefix);
               }
               return Boolean.TRUE;
            }

            if (loggingPrefix != null) logger.debug("{} - Connection add failed, sleeping with backoff: {}ms", poolName, sleepBackoff);
            quietlySleep(sleepBackoff);
            sleepBackoff = Math.min(SECONDS.toMillis(10), Math.min(connectionTimeout, (long) (sleepBackoff * 1.5)));
         }

         return Boolean.FALSE;
      }

      private synchronized boolean shouldCreateAnotherConnection() {
         return getTotalConnections() < config.getMaximumPoolSize() &&
            (connectionBag.getWaitingThreadCount() > 0 || getIdleConnections() < config.getMinimumIdle());
      }
   }

   private final class HouseKeeper implements Runnable
   {
      private volatile long previous = plusMillis(currentTime(), -housekeepingPeriodMs);

      @Override
      public void run()
      {
         try {
            
            connectionTimeout = config.getConnectionTimeout();
            validationTimeout = config.getValidationTimeout();
            leakTaskFactory.updateLeakDetectionThreshold(config.getLeakDetectionThreshold());
            catalog = (config.getCatalog() != null && !config.getCatalog().equals(catalog)) ? config.getCatalog() : catalog;

            final long idleTimeout = config.getIdleTimeout();
            final long now = currentTime();

            if (plusMillis(now, 128) < plusMillis(previous, housekeepingPeriodMs)) {
               logger.warn("{} - Retrograde clock change detected (housekeeper delta={}), soft-evicting connections from pool.",
                           poolName, elapsedDisplayString(previous, now));
               previous = now;
               softEvictConnections();
               return;
            }
            else if (now > plusMillis(previous, (3 * housekeepingPeriodMs) / 2)) {
               
               logger.warn("{} - Thread starvation or clock leap detected (housekeeper delta={}).", poolName, elapsedDisplayString(previous, now));
            }

            previous = now;

            String afterPrefix = "Pool ";
            if (idleTimeout > 0L && config.getMinimumIdle() < config.getMaximumPoolSize()) {
               logPoolState("Before cleanup ");
               afterPrefix = "After cleanup  ";

               final List<PoolEntry> notInUse = connectionBag.values(STATE_NOT_IN_USE);
               int toRemove = notInUse.size() - config.getMinimumIdle();
               for (PoolEntry entry : notInUse) {
                  if (toRemove > 0 && elapsedMillis(entry.lastAccessed, now) > idleTimeout && connectionBag.reserve(entry)) {
                     closeConnection(entry, "(connection has passed idleTimeout)");
                     toRemove--;
                  }
               }
            }

            logPoolState(afterPrefix);

            fillPool(); 
         }
         catch (Exception e) {
            logger.error("Unexpected exception in housekeeping task", e);
         }
      }
   }

   private final class MaxLifetimeTask implements Runnable
   {
      private final PoolEntry poolEntry;

      MaxLifetimeTask(final PoolEntry poolEntry)
      {
         this.poolEntry = poolEntry;
      }

      @Override
      public void run()
      {
         if (softEvictConnection(poolEntry, "(connection has passed maxLifetime)", false )) {
            addBagItem(connectionBag.getWaitingThreadCount());
         }
      }
   }

   private final class KeepaliveTask implements Runnable
   {
      private final PoolEntry poolEntry;

      KeepaliveTask(final PoolEntry poolEntry)
      {
         this.poolEntry = poolEntry;
      }

      @Override
      public void run()
      {
         if (connectionBag.reserve(poolEntry)) {
            if (!isConnectionAlive(poolEntry.connection)) {
               softEvictConnection(poolEntry, DEAD_CONNECTION_MESSAGE, true);
               addBagItem(connectionBag.getWaitingThreadCount());
            }
            else {
               connectionBag.unreserve(poolEntry);
               logger.debug("{} - keepalive: connection {} is alive", poolName, poolEntry.connection);
            }
         }
      }
   }

   public static class PoolInitializationException extends RuntimeException
   {
      private static final long serialVersionUID = 929872118275916520L;

      public PoolInitializationException(Throwable t)
      {
         super("Failed to initialize pool: " + t.getMessage(), t);
      }
   }
}
