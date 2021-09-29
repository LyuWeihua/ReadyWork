/**
 *
 * Original work Copyright 2017-2019 CodingApi
 * Modified work Copyright (c) 2020 WeiHua Lyu [ready.work]
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
package work.ready.cloud.transaction.core.transaction.txc.analyse;

import work.ready.cloud.transaction.core.transaction.txc.analyse.bean.TableStruct;
import work.ready.core.server.Ready;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TableStructAnalyser {

    public TableStruct analyse(Connection connection, String table) throws SQLException {
        ResultSet structRs = null;
        ResultSet columnSet = null;
        TableStruct tableStruct = new TableStruct(table);
        try {

            structRs = connection.getMetaData().getPrimaryKeys(connection.getCatalog(), connection.getSchema(), table);
            columnSet = connection.getMetaData().getColumns(connection.getCatalog(), connection.getSchema(), table, "%");
            while (structRs.next()) {
                tableStruct.getPrimaryKeys().add(structRs.getString("COLUMN_NAME").toUpperCase());
            }
            while (columnSet.next()) {
                tableStruct.getColumns().put(columnSet.getString("COLUMN_NAME").toUpperCase(), columnSet.getString("TYPE_NAME"));
            }
        } catch (SQLException e) {
            try {
                Ready.dbManager().close(structRs);
                Ready.dbManager().close(columnSet);
            } catch (SQLException ignored) {
            }
            throw e;
        }
        return tableStruct;
    }

    public boolean existsTable(Connection connection, String table) throws SQLException {
        ResultSet resultSet = null;
        try {
            resultSet = connection.getMetaData().getTables(connection.getCatalog(), connection.getSchema(), table.toUpperCase(), null);
            if (resultSet.next()) {
                return true;
            }
        } catch (SQLException e) {
            throw e;
        } finally {
            Ready.dbManager().close(resultSet);
        }
        return false;
    }
}
