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

public interface ExecuteBatchListener extends JdbcListener {

    void onBeforeAddBatch(PreparedStatementInformation statementInformation) throws SQLException;

    void onAfterAddBatch(PreparedStatementInformation statementInformation, long timeElapsedNanos, SQLException e);

    String onBeforeAddBatch(StatementInformation statementInformation, String sql) throws SQLException;

    void onAfterAddBatch(StatementInformation statementInformation, long timeElapsedNanos, String sql, SQLException e);

    void onBeforeExecuteBatch(StatementInformation statementInformation) throws SQLException;

    void onAfterExecuteBatch(StatementInformation statementInformation, long timeElapsedNanos, int[] updateCounts, SQLException e);

}
