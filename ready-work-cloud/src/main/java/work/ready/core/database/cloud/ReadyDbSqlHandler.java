/**
 *
 * Copyright (c) 2020 WeiHua Lyu [ready.work]
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
 *
 */

package work.ready.core.database.cloud;

import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import work.ready.cloud.ReadyCloud;
import work.ready.core.database.jdbc.common.SqlExecuteHandler;
import work.ready.core.database.jdbc.common.StatementInformation;
import work.ready.core.security.data.SqlAuditTuple;

import java.sql.SQLException;

public class ReadyDbSqlHandler implements SqlExecuteHandler {

    @Override
    public void beforeAudit(SqlAuditTuple sqlAuditTuple) {
        if(sqlAuditTuple.getStatement() instanceof CreateTable) {
            sqlAuditTuple.setChanged(ReadyCloud.getInstance().verifyCreateTableDDL((CreateTable)sqlAuditTuple.getStatement()));
        }
    }

    @Override
    public void beforeSqlExecute(StatementInformation statementInformation, Statement parsed, String sqlWithValues) throws SQLException {

    }

    @Override
    public void afterSqlExecute(StatementInformation statementInformation, long timeElapsedNanos, SQLException e) {

    }
}
