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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class ProxyResultSet implements ResultSet
{
   protected final ProxyConnection connection;
   protected final ProxyStatement statement;
   final ResultSet delegate;

   protected ProxyResultSet(ProxyConnection connection, ProxyStatement statement, ResultSet resultSet)
   {
      this.connection = connection;
      this.statement = statement;
      this.delegate = resultSet;
   }

   final SQLException checkException(SQLException e)
   {
      return connection.checkException(e);
   }

   @Override
   public String toString()
   {
      return this.getClass().getSimpleName() + '@' + System.identityHashCode(this) + " wrapping " + delegate;
   }

   @Override
   public final Statement getStatement() throws SQLException
   {
      return statement;
   }

   @Override
   public void updateRow() throws SQLException
   {
      connection.markCommitStateDirty();
      delegate.updateRow();
   }

   @Override
   public void insertRow() throws SQLException
   {
      connection.markCommitStateDirty();
      delegate.insertRow();
   }

   @Override
   public void deleteRow() throws SQLException
   {
      connection.markCommitStateDirty();
      delegate.deleteRow();
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

      throw new SQLException("Wrapped ResultSet is not an instance of " + iface);
   }
}
