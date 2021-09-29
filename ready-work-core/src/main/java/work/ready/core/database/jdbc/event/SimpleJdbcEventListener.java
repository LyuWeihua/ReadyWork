/**
 *
 * Copyright (c) 2020 WeiHua Lyu [ready.work]
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

import work.ready.core.database.jdbc.common.ConnectionInformation;
import work.ready.core.database.jdbc.common.StatementInformation;

import java.sql.SQLException;

public abstract class SimpleJdbcEventListener extends AnyExecuteListener implements ConnectionListener, TransactionListener {

    @Override
    public String onBeforeAnyExecute(StatementInformation statementInformation) throws SQLException
    {
        return statementInformation.getStatementQuery();
    }

    @Override
    public void onAfterAnyExecute(StatementInformation statementInformation, long timeElapsedNanos, SQLException e)
    {

    }

    @Override
    public String onBeforeAnyAddBatch(StatementInformation statementInformation) throws SQLException
    {
        return statementInformation.getStatementQuery();
    }

    @Override
    public void onAfterAnyAddBatch(StatementInformation statementInformation, long timeElapsedNanos, SQLException e)
    {

    }

    @Override
    public void onBeforeGetConnection(ConnectionInformation connectionInformation) throws SQLException {
    }

    @Override
    public void onAfterGetConnection(ConnectionInformation connectionInformation, SQLException e) {
    }

    @Override
    public void onAfterConnectionClose(ConnectionInformation connectionInformation, SQLException e) {
    }

    @Override
    public void onBeforeCommit(ConnectionInformation connectionInformation) throws SQLException {
    }

    @Override
    public void onAfterCommit(ConnectionInformation connectionInformation, long timeElapsedNanos, SQLException e) {
    }

    @Override
    public void onBeforeRollback(ConnectionInformation connectionInformation) throws SQLException {
    }

    @Override
    public void onAfterRollback(ConnectionInformation connectionInformation, long timeElapsedNanos, SQLException e) {
    }

    @Override
    public void onBeforeSetAutoCommit(ConnectionInformation connectionInformation, boolean newAutoCommit, boolean currentAutoCommit) throws SQLException {
    }

    @Override
    public void onAfterSetAutoCommit(ConnectionInformation connectionInformation, boolean newAutoCommit, boolean oldAutoCommit, SQLException e) {
    }
}
