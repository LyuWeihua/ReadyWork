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

import work.ready.core.database.jdbc.common.PreparedStatementInformation;
import work.ready.core.database.jdbc.common.StatementInformation;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public interface ExecuteListener extends JdbcListener {

    void onBeforeExecute(PreparedStatementInformation statementInformation) throws SQLException;

    void onAfterExecute(PreparedStatementInformation statementInformation, long timeElapsedNanos, SQLException e);

    String onBeforeExecute(StatementInformation statementInformation, String sql) throws SQLException;

    void onAfterExecute(StatementInformation statementInformation, long timeElapsedNanos, String sql, SQLException e);

    void onBeforeExecuteUpdate(PreparedStatementInformation statementInformation) throws SQLException;

    void onAfterExecuteUpdate(PreparedStatementInformation statementInformation, long timeElapsedNanos, int rowCount, SQLException e);

    String onBeforeExecuteUpdate(StatementInformation statementInformation, String sql) throws SQLException;

    void onAfterExecuteUpdate(StatementInformation statementInformation, long timeElapsedNanos, String sql, int rowCount, SQLException e);

    void onBeforeExecuteQuery(PreparedStatementInformation statementInformation) throws SQLException;

    void onAfterExecuteQuery(PreparedStatementInformation statementInformation, long timeElapsedNanos, SQLException e);

    String onBeforeExecuteQuery(StatementInformation statementInformation, String sql) throws SQLException;

    void onAfterExecuteQuery(StatementInformation statementInformation, long timeElapsedNanos, String sql, SQLException e);

}
