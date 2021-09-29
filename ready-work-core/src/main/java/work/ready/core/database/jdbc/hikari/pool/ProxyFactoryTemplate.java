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

import work.ready.core.database.jdbc.hikari.util.FastList;

import java.sql.*;

public final class ProxyFactoryTemplate
{
   private ProxyFactoryTemplate()
   {
      
   }

   static ProxyConnection getProxyConnection(final PoolEntry poolEntry, final Connection connection, final FastList<Statement> openStatements, final ProxyLeakTask leakTask, final long now, final boolean isReadOnly, final boolean isAutoCommit)
   {
      
      throw new IllegalStateException("You need to run the CLI build and you need target/classes in your classpath to run.");
   }

   static Statement getProxyStatement(final ProxyConnection connection, final Statement statement)
   {
      
      throw new IllegalStateException("You need to run the CLI build and you need target/classes in your classpath to run.");
   }

   static CallableStatement getProxyCallableStatement(final ProxyConnection connection, final String sql, final CallableStatement statement)
   {
      
      throw new IllegalStateException("You need to run the CLI build and you need target/classes in your classpath to run.");
   }

   static PreparedStatement getProxyPreparedStatement(final ProxyConnection connection, final String sql, final PreparedStatement statement)
   {
      
      throw new IllegalStateException("You need to run the CLI build and you need target/classes in your classpath to run.");
   }

   static ResultSet getProxyResultSet(final ProxyConnection connection, final ProxyStatement statement, final ResultSet resultSet)
   {
      
      throw new IllegalStateException("You need to run the CLI build and you need target/classes in your classpath to run.");
   }

   static DatabaseMetaData getProxyDatabaseMetaData(final ProxyConnection connection, final DatabaseMetaData metaData)
   {
      
      throw new IllegalStateException("You need to run the CLI build and you need target/classes in your classpath to run.");
   }
}
