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

import javax.sql.DataSource;
import java.sql.*;

public abstract class JdbcEventListener implements ConnectionListener, TransactionListener, ResultSetListener, StatementListener, ExecuteListener, ExecuteBatchListener {

  @Override
  public void onBeforeGetConnection(ConnectionInformation connectionInformation) throws SQLException {
  }

  @Override
  public void onAfterGetConnection(ConnectionInformation connectionInformation, SQLException e) {
  }

  @Override
  public void onBeforeAddBatch(PreparedStatementInformation statementInformation) throws SQLException {
  }

  @Override
  public void onAfterAddBatch(PreparedStatementInformation statementInformation, long timeElapsedNanos, SQLException e) {
  }

  @Override
  public String onBeforeAddBatch(StatementInformation statementInformation, String sql) throws SQLException {
    return sql;
  }

  @Override
  public void onAfterAddBatch(StatementInformation statementInformation, long timeElapsedNanos, String sql, SQLException e) {
  }

  @Override
  public void onBeforeExecute(PreparedStatementInformation statementInformation) throws SQLException {
  }

  @Override
  public void onAfterExecute(PreparedStatementInformation statementInformation, long timeElapsedNanos, SQLException e) {
  }

  @Override
  public String onBeforeExecute(StatementInformation statementInformation, String sql) throws SQLException {
      return sql;
  }

  @Override
  public void onAfterExecute(StatementInformation statementInformation, long timeElapsedNanos, String sql, SQLException e) {
  }

  @Override
  public void onBeforeExecuteBatch(StatementInformation statementInformation) throws SQLException {
  }

  @Override
  public void onAfterExecuteBatch(StatementInformation statementInformation, long timeElapsedNanos, int[] updateCounts, SQLException e) {
  }

  @Override
  public void onBeforeExecuteUpdate(PreparedStatementInformation statementInformation) throws SQLException {
  }

  @Override
  public void onAfterExecuteUpdate(PreparedStatementInformation statementInformation, long timeElapsedNanos, int rowCount, SQLException e) {
  }

  @Override
  public String onBeforeExecuteUpdate(StatementInformation statementInformation, String sql) throws SQLException {
      return sql;
  }

  @Override
  public void onAfterExecuteUpdate(StatementInformation statementInformation, long timeElapsedNanos, String sql, int rowCount, SQLException e) {
  }

  @Override
  public void onBeforeExecuteQuery(PreparedStatementInformation statementInformation) throws SQLException {
  }

  @Override
  public void onAfterExecuteQuery(PreparedStatementInformation statementInformation, long timeElapsedNanos, SQLException e) {
  }

  @Override
  public String onBeforeExecuteQuery(StatementInformation statementInformation, String sql) throws SQLException {
      return sql;
  }

  @Override
  public void onAfterExecuteQuery(StatementInformation statementInformation, long timeElapsedNanos, String sql, SQLException e) {
  }

  @Override
  public void onAfterPreparedStatementSet(PreparedStatementInformation statementInformation, int parameterIndex, Object value, SQLException e) {
  }

  @Override
  public void onAfterCallableStatementSet(CallableStatementInformation statementInformation, String parameterName, Object value, SQLException e) {
  }

  @Override
  public void onAfterGetResultSet(StatementInformation statementInformation, long timeElapsedNanos, SQLException e) {
  }

  @Override
  public void onBeforeResultSetNext(ResultSetInformation resultSetInformation) throws SQLException {
  }

  @Override
  public void onAfterResultSetNext(ResultSetInformation resultSetInformation, long timeElapsedNanos, boolean hasNext, SQLException e) {
  }

  @Override
  public void onAfterResultSetClose(ResultSetInformation resultSetInformation, SQLException e) {
  }

  @Override
  public void onAfterResultSetGet(ResultSetInformation resultSetInformation, String columnLabel, Object value, SQLException e) {
  }

  @Override
  public void onAfterResultSetGet(ResultSetInformation resultSetInformation, int columnIndex, Object value, SQLException e) {
  }

  @Override
  public void onBeforeCommit(ConnectionInformation connectionInformation) throws SQLException {
  }

  @Override
  public void onAfterCommit(ConnectionInformation connectionInformation, long timeElapsedNanos, SQLException e) {
  }

  @Override
  public void onAfterConnectionClose(ConnectionInformation connectionInformation, SQLException e) {
  }

  @Override
  public void onBeforeRollback(ConnectionInformation connectionInformation) throws SQLException {
  }

  @Override
  public void onAfterRollback(ConnectionInformation connectionInformation, long timeElapsedNanos, SQLException e) {
  }

  @Override
  public void onAfterStatementClose(StatementInformation statementInformation, SQLException e) {
  }

  @Override
  public void onBeforeSetAutoCommit(ConnectionInformation connectionInformation, boolean newAutoCommit, boolean currentAutoCommit) throws SQLException {
  }

  @Override
  public void onAfterSetAutoCommit(ConnectionInformation connectionInformation, boolean newAutoCommit, boolean oldAutoCommit, SQLException e) {
  }

}
