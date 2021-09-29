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

import work.ready.core.database.jdbc.common.ResultSetInformation;
import work.ready.core.database.jdbc.common.StatementInformation;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public interface ResultSetListener extends JdbcListener {

    void onAfterGetResultSet(StatementInformation statementInformation, long timeElapsedNanos, SQLException e);

    void onBeforeResultSetNext(ResultSetInformation resultSetInformation) throws SQLException;

    void onAfterResultSetNext(ResultSetInformation resultSetInformation, long timeElapsedNanos, boolean hasNext, SQLException e);

    void onAfterResultSetClose(ResultSetInformation resultSetInformation, SQLException e);

    void onAfterResultSetGet(ResultSetInformation resultSetInformation, String columnLabel, Object value, SQLException e);

    void onAfterResultSetGet(ResultSetInformation resultSetInformation, int columnIndex, Object value, SQLException e);

}
