/**
 *
 * Original work Copyright (c) 2002 P6Spy
 * Modified work Copyright (c) 2020 WeiHua Lyu [ready.work]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package work.ready.core.database.jdbc.event;

import work.ready.core.database.jdbc.common.*;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

class CompoundJdbcEventListener extends JdbcEventListener {
  private final InnerEventListener innerEventListener = InnerEventListener.INSTANCE;
  private final List<ConnectionListener> connectionListeners = new CopyOnWriteArrayList<>();
  private final List<TransactionListener> transactionListeners = new CopyOnWriteArrayList<>();
  private final List<StatementListener> statementListeners = new CopyOnWriteArrayList<>();
  private final List<ExecuteListener> executeListeners = new CopyOnWriteArrayList<>();
  private final List<ExecuteBatchListener> executeBatchListeners = new CopyOnWriteArrayList<>();
  private final List<ResultSetListener> resultSetListeners = new CopyOnWriteArrayList<>();

  CompoundJdbcEventListener() {
  }

  void addListener(JdbcListener listener) {
    if(listener instanceof ConnectionListener) {
      connectionListeners.add((ConnectionListener)listener);
    }
    if(listener instanceof TransactionListener) {
      transactionListeners.add((TransactionListener)listener);
    }
    if(listener instanceof StatementListener) {
      statementListeners.add((StatementListener)listener);
    }
    if(listener instanceof ExecuteListener) {
      executeListeners.add((ExecuteListener)listener);
    }
    if(listener instanceof ExecuteBatchListener) {
      executeBatchListeners.add((ExecuteBatchListener)listener);
    }
    if(listener instanceof ResultSetListener) {
      resultSetListeners.add((ResultSetListener)listener);
    }
  }

  void removeListener(JdbcListener listener) {
    if(listener instanceof ConnectionListener) {
      connectionListeners.remove(listener);
    }
    if(listener instanceof TransactionListener) {
      transactionListeners.remove(listener);
    }
    if(listener instanceof StatementListener) {
      statementListeners.remove(listener);
    }
    if(listener instanceof ExecuteListener) {
      executeListeners.remove(listener);
    }
    if(listener instanceof ExecuteBatchListener) {
      executeBatchListeners.remove(listener);
    }
    if(listener instanceof ResultSetListener) {
      resultSetListeners.remove(listener);
    }
  }

  public <T extends JdbcListener> Collection<T> getEventListeners(Class<T> type) {
    Set<JdbcListener> list = new HashSet<>();
    if(ConnectionListener.class.isAssignableFrom(type)) {
      list.addAll(connectionListeners);
    }
    if(TransactionListener.class.isAssignableFrom(type)) {
      list.addAll(transactionListeners);
    }
    if(StatementListener.class.isAssignableFrom(type)) {
      list.addAll(statementListeners);
    }
    if(ExecuteListener.class.isAssignableFrom(type)) {
      list.addAll(executeListeners);
    }
    if(ExecuteBatchListener.class.isAssignableFrom(type)) {
      list.addAll(executeBatchListeners);
    }
    if(ResultSetListener.class.isAssignableFrom(type)) {
      list.addAll(resultSetListeners);
    }
    return (Collection<T>)Collections.unmodifiableCollection(list);
  }

  @Override
  public void onBeforeGetConnection(ConnectionInformation connectionInformation) throws SQLException {
    for (ConnectionListener eventListener : connectionListeners) {
      eventListener.onBeforeGetConnection(connectionInformation);
    }
  }

  @Override
  public void onAfterGetConnection(ConnectionInformation connectionInformation, SQLException e) {
    for (ConnectionListener eventListener : connectionListeners) {
      eventListener.onAfterGetConnection(connectionInformation, e);
    }
  }

  @Override
  public void onBeforeAddBatch(PreparedStatementInformation statementInformation) throws SQLException {
    for (ExecuteBatchListener eventListener : executeBatchListeners) {
      eventListener.onBeforeAddBatch(statementInformation);
    }
  }

  @Override
  public void onAfterAddBatch(PreparedStatementInformation statementInformation, long timeElapsedNanos, SQLException e) {
    innerEventListener.onAfterAddBatch(statementInformation, timeElapsedNanos, e);
    for (ExecuteBatchListener eventListener : executeBatchListeners) {
      eventListener.onAfterAddBatch(statementInformation, timeElapsedNanos, e);
    }
  }

  @Override
  public String onBeforeAddBatch(StatementInformation statementInformation, String sql) throws SQLException {
    for (ExecuteBatchListener eventListener : executeBatchListeners) {
      sql = eventListener.onBeforeAddBatch(statementInformation, sql);
    }
    return sql;
  }

  @Override
  public void onAfterAddBatch(StatementInformation statementInformation, long timeElapsedNanos, String sql, SQLException e) {
    for (ExecuteBatchListener eventListener : executeBatchListeners) {
      eventListener.onAfterAddBatch(statementInformation, timeElapsedNanos, sql, e);
    }
  }

  @Override
  public void onBeforeExecute(PreparedStatementInformation statementInformation) throws SQLException {
    for (ExecuteListener eventListener : executeListeners) {
      eventListener.onBeforeExecute(statementInformation);
    }
  }

  @Override
  public void onAfterExecute(PreparedStatementInformation statementInformation, long timeElapsedNanos, SQLException e) {
    innerEventListener.onAfterExecute(statementInformation, timeElapsedNanos, e);
    for (ExecuteListener eventListener : executeListeners) {
      eventListener.onAfterExecute(statementInformation, timeElapsedNanos, e);
    }
    statementInformation.clearParameters();
  }

  @Override
  public String onBeforeExecute(StatementInformation statementInformation, String sql) throws SQLException {
    for (ExecuteListener eventListener : executeListeners) {
      sql = eventListener.onBeforeExecute(statementInformation, sql);
    }
    return sql;
  }

  @Override
  public void onAfterExecute(StatementInformation statementInformation, long timeElapsedNanos, String sql, SQLException e) {
    innerEventListener.onAfterExecute(statementInformation, timeElapsedNanos, sql, e);
    for (ExecuteListener eventListener : executeListeners) {
      eventListener.onAfterExecute(statementInformation, timeElapsedNanos, sql, e);
    }
    if(statementInformation instanceof PreparedStatementInformation) {
      ((PreparedStatementInformation) statementInformation).clearParameters();
    }
  }

  @Override
  public void onBeforeExecuteBatch(StatementInformation statementInformation) throws SQLException {
    for (ExecuteBatchListener eventListener : executeBatchListeners) {
      eventListener.onBeforeExecuteBatch(statementInformation);
    }
  }

  @Override
  public void onAfterExecuteBatch(StatementInformation statementInformation, long timeElapsedNanos, int[] updateCounts, SQLException e) {
    innerEventListener.onAfterExecuteBatch(statementInformation, timeElapsedNanos, updateCounts, e);
    for (ExecuteBatchListener eventListener : executeBatchListeners) {
      eventListener.onAfterExecuteBatch(statementInformation, timeElapsedNanos, updateCounts, e);
    }
    if(statementInformation instanceof PreparedStatementInformation) {
      ((PreparedStatementInformation) statementInformation).clearParameters();
    }
  }

  @Override
  public void onBeforeExecuteUpdate(PreparedStatementInformation statementInformation) throws SQLException {
    for (ExecuteListener eventListener : executeListeners) {
      eventListener.onBeforeExecuteUpdate(statementInformation);
    }
  }

  @Override
  public void onAfterExecuteUpdate(PreparedStatementInformation statementInformation, long timeElapsedNanos, int rowCount, SQLException e) {
    innerEventListener.onAfterExecuteUpdate(statementInformation, timeElapsedNanos, rowCount, e);
    for (ExecuteListener eventListener : executeListeners) {
      eventListener.onAfterExecuteUpdate(statementInformation, timeElapsedNanos, rowCount, e);
    }
    statementInformation.clearParameters();
  }

  @Override
  public String onBeforeExecuteUpdate(StatementInformation statementInformation, String sql) throws SQLException {
    for (ExecuteListener eventListener : executeListeners) {
      sql = eventListener.onBeforeExecuteUpdate(statementInformation, sql);
    }
    return sql;
  }

  @Override
  public void onAfterExecuteUpdate(StatementInformation statementInformation, long timeElapsedNanos, String sql, int rowCount, SQLException e) {
    innerEventListener.onAfterExecuteUpdate(statementInformation, timeElapsedNanos, sql, rowCount, e);
    for (ExecuteListener eventListener : executeListeners) {
      eventListener.onAfterExecuteUpdate(statementInformation, timeElapsedNanos, sql, rowCount, e);
    }
    if(statementInformation instanceof PreparedStatementInformation) {
      ((PreparedStatementInformation) statementInformation).clearParameters();
    }
  }

  @Override
  public void onBeforeExecuteQuery(PreparedStatementInformation statementInformation) throws SQLException {
    for (ExecuteListener eventListener : executeListeners) {
      eventListener.onBeforeExecuteQuery(statementInformation);
    }
  }

  @Override
  public void onAfterExecuteQuery(PreparedStatementInformation statementInformation, long timeElapsedNanos, SQLException e) {
    innerEventListener.onAfterExecuteQuery(statementInformation, timeElapsedNanos, e);
    for (ExecuteListener eventListener : executeListeners) {
      eventListener.onAfterExecuteQuery(statementInformation, timeElapsedNanos, e);
    }
    statementInformation.clearParameters();
  }

  @Override
  public String onBeforeExecuteQuery(StatementInformation statementInformation, String sql) throws SQLException {
    for (ExecuteListener eventListener : executeListeners) {
      sql = eventListener.onBeforeExecuteQuery(statementInformation, sql);
    }
    return sql;
  }

  @Override
  public void onAfterExecuteQuery(StatementInformation statementInformation, long timeElapsedNanos, String sql, SQLException e) {
    innerEventListener.onAfterExecuteQuery(statementInformation, timeElapsedNanos, sql, e);
    for (ExecuteListener eventListener : executeListeners) {
      eventListener.onAfterExecuteQuery(statementInformation, timeElapsedNanos, sql, e);
    }
    if(statementInformation instanceof PreparedStatementInformation) {
      ((PreparedStatementInformation) statementInformation).clearParameters();
    }
  }

  @Override
  public void onAfterPreparedStatementSet(PreparedStatementInformation statementInformation, int parameterIndex, Object value, SQLException e) {
    innerEventListener.onAfterPreparedStatementSet(statementInformation, parameterIndex, value, e);
    for (StatementListener eventListener : statementListeners) {
      eventListener.onAfterPreparedStatementSet(statementInformation, parameterIndex, value, e);
    }
  }

  @Override
  public void onAfterCallableStatementSet(CallableStatementInformation statementInformation, String parameterName, Object value, SQLException e) {
    innerEventListener.onAfterCallableStatementSet(statementInformation, parameterName, value, e);
    for (StatementListener eventListener : statementListeners) {
      eventListener.onAfterCallableStatementSet(statementInformation, parameterName, value, e);
    }
  }

  @Override
  public void onAfterGetResultSet(StatementInformation statementInformation, long timeElapsedNanos, SQLException e) {
    innerEventListener.onAfterGetResultSet(statementInformation, timeElapsedNanos, e);
    for (ResultSetListener eventListener : resultSetListeners) {
      eventListener.onAfterGetResultSet(statementInformation, timeElapsedNanos, e);
    }
  }

  @Override
  public void onBeforeResultSetNext(ResultSetInformation resultSetInformation) throws SQLException {
    for (ResultSetListener eventListener : resultSetListeners) {
      eventListener.onBeforeResultSetNext(resultSetInformation);
    }
  }

  @Override
  public void onAfterResultSetNext(ResultSetInformation resultSetInformation, long timeElapsedNanos, boolean hasNext, SQLException e) {
    innerEventListener.onAfterResultSetNext(resultSetInformation, timeElapsedNanos, hasNext, e);
    for (ResultSetListener eventListener : resultSetListeners) {
      eventListener.onAfterResultSetNext(resultSetInformation, timeElapsedNanos, hasNext, e);
    }
  }

  @Override
  public void onAfterResultSetClose(ResultSetInformation resultSetInformation, SQLException e) {
    for (ResultSetListener eventListener : resultSetListeners) {
      eventListener.onAfterResultSetClose(resultSetInformation, e);
    }
  }

  @Override
  public void onAfterResultSetGet(ResultSetInformation resultSetInformation, String columnLabel, Object value, SQLException e) {
    for (ResultSetListener eventListener : resultSetListeners) {
      eventListener.onAfterResultSetGet(resultSetInformation, columnLabel, value, e);
    }
  }

  @Override
  public void onAfterResultSetGet(ResultSetInformation resultSetInformation, int columnIndex, Object value, SQLException e) {
    for (ResultSetListener eventListener : resultSetListeners) {
      eventListener.onAfterResultSetGet(resultSetInformation, columnIndex, value, e);
    }
  }

  @Override
  public void onBeforeCommit(ConnectionInformation connectionInformation) throws SQLException {
    for (TransactionListener eventListener : transactionListeners) {
      eventListener.onBeforeCommit(connectionInformation);
    }
  }

  @Override
  public void onAfterCommit(ConnectionInformation connectionInformation, long timeElapsedNanos, SQLException e) {
    for (TransactionListener eventListener : transactionListeners) {
      eventListener.onAfterCommit(connectionInformation, timeElapsedNanos, e);
    }
  }

  @Override
  public void onAfterConnectionClose(ConnectionInformation connectionInformation, SQLException e) {
    for (ConnectionListener eventListener : connectionListeners) {
      eventListener.onAfterConnectionClose(connectionInformation, e);
    }
  }

  @Override
  public void onBeforeRollback(ConnectionInformation connectionInformation) throws SQLException {
    for (TransactionListener eventListener : transactionListeners) {
      eventListener.onBeforeRollback(connectionInformation);
    }
  }

  @Override
  public void onAfterRollback(ConnectionInformation connectionInformation, long timeElapsedNanos, SQLException e) {
    for (TransactionListener eventListener : transactionListeners) {
      eventListener.onAfterRollback(connectionInformation, timeElapsedNanos, e);
    }
  }

  @Override
  public void onAfterStatementClose(StatementInformation statementInformation, SQLException e) {
    for (StatementListener eventListener : statementListeners) {
      eventListener.onAfterStatementClose(statementInformation, e);
    }
  }

  @Override
  public void onBeforeSetAutoCommit(ConnectionInformation connectionInformation, boolean newAutoCommit, boolean currentAutoCommit) throws SQLException {
    for (TransactionListener eventListener : transactionListeners) {
      eventListener.onBeforeSetAutoCommit(connectionInformation, newAutoCommit,currentAutoCommit);
    }
  }

  @Override
  public void onAfterSetAutoCommit(ConnectionInformation connectionInformation, boolean newAutoCommit, boolean oldAutoCommit, SQLException e) {
    for (TransactionListener eventListener : transactionListeners) {
      eventListener.onAfterSetAutoCommit(connectionInformation, newAutoCommit,oldAutoCommit,e);
    }
  }

}
