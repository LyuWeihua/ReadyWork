/*
 * Copyright (C) 2014 Brett Wooldridge
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

import work.ready.core.database.jdbc.hikari.util.ConcurrentBag.IConcurrentBagEntry;
import work.ready.core.database.jdbc.hikari.util.FastList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import static work.ready.core.database.jdbc.hikari.util.ClockSource.*;

final class PoolEntry implements IConcurrentBagEntry
{
   private static final Logger LOGGER = LoggerFactory.getLogger(PoolEntry.class);
   private static final AtomicIntegerFieldUpdater<PoolEntry> stateUpdater;

   Connection connection;
   long lastAccessed;
   long lastBorrowed;

   @SuppressWarnings("FieldCanBeLocal")
   private volatile int state = 0;
   private volatile boolean evict;

   private volatile ScheduledFuture<?> endOfLife;
   private volatile ScheduledFuture<?> keepalive;

   private final FastList<Statement> openStatements;
   private final HikariPool hikariPool;

   private final boolean isReadOnly;
   private final boolean isAutoCommit;

   static
   {
      stateUpdater = AtomicIntegerFieldUpdater.newUpdater(PoolEntry.class, "state");
   }

   PoolEntry(final Connection connection, final PoolBase pool, final boolean isReadOnly, final boolean isAutoCommit)
   {
      this.connection = connection;
      this.hikariPool = (HikariPool) pool;
      this.isReadOnly = isReadOnly;
      this.isAutoCommit = isAutoCommit;
      this.lastAccessed = currentTime();
      this.openStatements = new FastList<>(Statement.class, 16);
   }

   void recycle(final long lastAccessed)
   {
      if (connection != null) {
         this.lastAccessed = lastAccessed;
         hikariPool.recycle(this);
      }
   }

   void setFutureEol(final ScheduledFuture<?> endOfLife)
   {
      this.endOfLife = endOfLife;
   }

   public void setKeepalive(ScheduledFuture<?> keepalive) {
      this.keepalive = keepalive;
   }

   Connection createProxyConnection(final ProxyLeakTask leakTask, final long now)
   {
      return ProxyFactory.getProxyConnection(this, connection, openStatements, leakTask, now, isReadOnly, isAutoCommit);
   }

   void resetConnectionState(final ProxyConnection proxyConnection, final int dirtyBits) throws SQLException
   {
      hikariPool.resetConnectionState(connection, proxyConnection, dirtyBits);
   }

   String getPoolName()
   {
      return hikariPool.toString();
   }

   boolean isMarkedEvicted()
   {
      return evict;
   }

   void markEvicted()
   {
      this.evict = true;
   }

   void evict(final String closureReason)
   {
      hikariPool.closeConnection(this, closureReason);
   }

   long getMillisSinceBorrowed()
   {
      return elapsedMillis(lastBorrowed);
   }

   PoolBase getPoolBase()
   {
      return hikariPool;
   }

   @Override
   public String toString()
   {
      final long now = currentTime();
      return connection
         + ", accessed " + elapsedDisplayString(lastAccessed, now) + " ago, "
         + stateToString();
   }

   @Override
   public int getState()
   {
      return stateUpdater.get(this);
   }

   @Override
   public boolean compareAndSet(int expect, int update)
   {
      return stateUpdater.compareAndSet(this, expect, update);
   }

   @Override
   public void setState(int update)
   {
      stateUpdater.set(this, update);
   }

   Connection close()
   {
      ScheduledFuture<?> eol = endOfLife;
      if (eol != null && !eol.isDone() && !eol.cancel(false)) {
         LOGGER.warn("{} - maxLifeTime expiration task cancellation unexpectedly returned false for connection {}", getPoolName(), connection);
      }

      ScheduledFuture<?> ka = keepalive;
      if (ka != null && !ka.isDone() && !ka.cancel(false)) {
         LOGGER.warn("{} - keepalive task cancellation unexpectedly returned false for connection {}", getPoolName(), connection);
      }

      Connection con = connection;
      connection = null;
      endOfLife = null;
      keepalive = null;
      return con;
   }

   private String stateToString()
   {
      switch (state) {
      case STATE_IN_USE:
         return "IN_USE";
      case STATE_NOT_IN_USE:
         return "NOT_IN_USE";
      case STATE_REMOVED:
         return "REMOVED";
      case STATE_RESERVED:
         return "RESERVED";
      default:
         return "Invalid";
      }
   }
}
