/*
 * Copyright (C) 2013 Brett Wooldridge
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

package work.ready.core.database.jdbc.hikari;

import work.ready.core.database.jdbc.hikari.pool.HikariPool;
import work.ready.core.database.jdbc.hikari.pool.HikariPool.PoolInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.atomic.AtomicBoolean;

import static work.ready.core.database.jdbc.hikari.pool.HikariPool.POOL_NORMAL;

class HikariDataSource extends HikariConfig implements DataSource, Closeable
{
   private static final Logger LOGGER = LoggerFactory.getLogger(HikariDataSource.class);

   private final AtomicBoolean isShutdown = new AtomicBoolean();

   private final HikariPool fastPathPool;
   private volatile HikariPool pool;

   public HikariDataSource()
   {
      super();
      fastPathPool = null;
   }

   public HikariDataSource(HikariConfig configuration)
   {
      configuration.validate();
      configuration.copyStateTo(this);

      LOGGER.info("{} - Starting...", configuration.getPoolName());
      pool = fastPathPool = new HikariPool(this);
      LOGGER.info("{} - Start completed.", configuration.getPoolName());

      this.seal();
   }

   @Override
   public Connection getConnection() throws SQLException
   {
      if (isClosed()) {
         throw new SQLException("HikariDataSource " + this + " has been closed.");
      }

      if (fastPathPool != null) {
         return fastPathPool.getConnection();
      }

      HikariPool result = pool;
      if (result == null) {
         synchronized (this) {
            result = pool;
            if (result == null) {
               validate();
               LOGGER.info("{} - Starting...", getPoolName());
               try {
                  pool = result = new HikariPool(this);
                  this.seal();
               }
               catch (PoolInitializationException pie) {
                  if (pie.getCause() instanceof SQLException) {
                     throw (SQLException) pie.getCause();
                  }
                  else {
                     throw pie;
                  }
               }
               LOGGER.info("{} - Start completed.", getPoolName());
            }
         }
      }

      return result.getConnection();
   }

   @Override
   public Connection getConnection(String username, String password) throws SQLException
   {
      throw new SQLFeatureNotSupportedException();
   }

   @Override
   public PrintWriter getLogWriter() throws SQLException
   {
      HikariPool p = pool;
      return (p != null ? p.getUnwrappedDataSource().getLogWriter() : null);
   }

   @Override
   public void setLogWriter(PrintWriter out) throws SQLException
   {
      HikariPool p = pool;
      if (p != null) {
         p.getUnwrappedDataSource().setLogWriter(out);
      }
   }

   @Override
   public void setLoginTimeout(int seconds) throws SQLException
   {
      HikariPool p = pool;
      if (p != null) {
         p.getUnwrappedDataSource().setLoginTimeout(seconds);
      }
   }

   @Override
   public int getLoginTimeout() throws SQLException
   {
      HikariPool p = pool;
      return (p != null ? p.getUnwrappedDataSource().getLoginTimeout() : 0);
   }

   @Override
   public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException
   {
      throw new SQLFeatureNotSupportedException();
   }

   @Override
   @SuppressWarnings("unchecked")
   public <T> T unwrap(Class<T> iface) throws SQLException
   {
      if (iface.isInstance(this)) {
         return (T) this;
      }

      HikariPool p = pool;
      if (p != null) {
         final DataSource unwrappedDataSource = p.getUnwrappedDataSource();
         if (iface.isInstance(unwrappedDataSource)) {
            return (T) unwrappedDataSource;
         }

         if (unwrappedDataSource != null) {
            return unwrappedDataSource.unwrap(iface);
         }
      }

      throw new SQLException("Wrapped DataSource is not an instance of " + iface);
   }

   @Override
   public boolean isWrapperFor(Class<?> iface) throws SQLException
   {
      if (iface.isInstance(this)) {
         return true;
      }

      HikariPool p = pool;
      if (p != null) {
         final DataSource unwrappedDataSource = p.getUnwrappedDataSource();
         if (iface.isInstance(unwrappedDataSource)) {
            return true;
         }

         if (unwrappedDataSource != null) {
            return unwrappedDataSource.isWrapperFor(iface);
         }
      }

      return false;
   }

   public boolean isRunning()
   {
      return pool != null && pool.poolState == POOL_NORMAL;
   }

   public HikariPoolMXBean getHikariPoolMXBean()
   {
      return pool;
   }

   public HikariConfigMXBean getHikariConfigMXBean()
   {
      return this;
   }

   public void evictConnection(Connection connection)
   {
      HikariPool p;
      if (!isClosed() && (p = pool) != null && connection.getClass().getName().startsWith(getClass().getPackageName())) {
         p.evictConnection(connection);
      }
   }

   @Override
   public void close()
   {
      if (isShutdown.getAndSet(true)) {
         return;
      }

      HikariPool p = pool;
      if (p != null) {
         try {
            LOGGER.info("{} - Shutdown initiated...", getPoolName());
            p.shutdown();
            LOGGER.info("{} - Shutdown completed.", getPoolName());
         }
         catch (InterruptedException e) {
            LOGGER.warn("{} - Interrupted during closing", getPoolName(), e);
            Thread.currentThread().interrupt();
         }
      }
   }

   public boolean isClosed()
   {
      return isShutdown.get();
   }

   @Override
   public String toString()
   {
      return "HikariDataSource (" + pool + ")";
   }
}
