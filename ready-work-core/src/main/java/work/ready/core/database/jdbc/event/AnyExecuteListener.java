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

import work.ready.core.database.jdbc.common.PreparedStatementInformation;
import work.ready.core.database.jdbc.common.StatementInformation;

import java.sql.SQLException;

public abstract class AnyExecuteListener implements ExecuteListener, ExecuteBatchListener {

    public String onBeforeAnyExecute(StatementInformation statementInformation) throws SQLException
    {
        return statementInformation.getStatementQuery();
    }

    public void onAfterAnyExecute(StatementInformation statementInformation, long timeElapsedNanos, SQLException e)
    {

    }

    public String onBeforeAnyAddBatch(StatementInformation statementInformation) throws SQLException
    {
        return statementInformation.getStatementQuery();
    }

    public void onAfterAnyAddBatch(StatementInformation statementInformation, long timeElapsedNanos, SQLException e)
    {

    }

    @Override
    public void onBeforeExecute(PreparedStatementInformation statementInformation) throws SQLException {
        onBeforeAnyExecute(statementInformation);
    }

    @Override
    public String onBeforeExecute(StatementInformation statementInformation, String sql) throws SQLException {
        return onBeforeAnyExecute(statementInformation);
    }

    @Override
    public void onBeforeExecuteBatch(StatementInformation statementInformation) throws SQLException {
        onBeforeAnyExecute(statementInformation);
    }

    @Override
    public void onBeforeExecuteUpdate(PreparedStatementInformation statementInformation) throws SQLException {
        onBeforeAnyExecute(statementInformation);
    }

    @Override
    public String onBeforeExecuteUpdate(StatementInformation statementInformation, String sql) throws SQLException {
        return onBeforeAnyExecute(statementInformation);
    }

    @Override
    public void onBeforeExecuteQuery(PreparedStatementInformation statementInformation) throws SQLException {
        onBeforeAnyExecute(statementInformation);
    }

    @Override
    public String onBeforeExecuteQuery(StatementInformation statementInformation, String sql) throws SQLException {
        return onBeforeAnyExecute(statementInformation);
    }

    @Override
    public void onAfterExecute(PreparedStatementInformation statementInformation, long timeElapsedNanos, SQLException e) {
        onAfterAnyExecute(statementInformation, timeElapsedNanos, e);
    }

    @Override
    public void onAfterExecute(StatementInformation statementInformation, long timeElapsedNanos, String sql, SQLException e) {
        onAfterAnyExecute(statementInformation, timeElapsedNanos, e);
    }

    @Override
    public void onAfterExecuteBatch(StatementInformation statementInformation, long timeElapsedNanos, int[] updateCounts, SQLException e) {
        onAfterAnyExecute(statementInformation, timeElapsedNanos, e);
    }

    @Override
    public void onAfterExecuteUpdate(PreparedStatementInformation statementInformation, long timeElapsedNanos, int rowCount, SQLException e) {
        onAfterAnyExecute(statementInformation, timeElapsedNanos, e);
    }

    @Override
    public void onAfterExecuteUpdate(StatementInformation statementInformation, long timeElapsedNanos, String sql, int rowCount, SQLException e) {
        onAfterAnyExecute(statementInformation, timeElapsedNanos, e);
    }

    @Override
    public void onAfterExecuteQuery(PreparedStatementInformation statementInformation, long timeElapsedNanos, SQLException e) {
        onAfterAnyExecute(statementInformation, timeElapsedNanos, e);
    }

    @Override
    public void onAfterExecuteQuery(StatementInformation statementInformation, long timeElapsedNanos, String sql, SQLException e) {
        onAfterAnyExecute(statementInformation, timeElapsedNanos, e);
    }

    @Override
    public void onBeforeAddBatch(PreparedStatementInformation statementInformation) throws SQLException {
        onBeforeAnyAddBatch(statementInformation);
    }

    @Override
    public String onBeforeAddBatch(StatementInformation statementInformation, String sql) throws SQLException {
        return onBeforeAnyAddBatch(statementInformation);
    }

    @Override
    public void onAfterAddBatch(PreparedStatementInformation statementInformation, long timeElapsedNanos, SQLException e) {
        onAfterAnyAddBatch(statementInformation, timeElapsedNanos, e);
    }

    @Override
    public void onAfterAddBatch(StatementInformation statementInformation, long timeElapsedNanos, String sql, SQLException e) {
        onAfterAnyAddBatch(statementInformation, timeElapsedNanos, e);
    }
}
