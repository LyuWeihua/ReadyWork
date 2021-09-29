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
package work.ready.cloud.transaction.core.transaction.txc.analyse.bean;

import java.sql.Statement;
import java.util.List;
import java.util.Map;

public class InsertImageParams {

    private Statement statement;

    public Statement getStatement() {
        return statement;
    }

    public void setStatement(Statement statement) {
        this.statement = statement;
    }

    private String tableName;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    private List<Map<String, Object>> primaryKeyValuesList;

    public List<Map<String, Object>> getPrimaryKeyValuesList() {
        return primaryKeyValuesList;
    }

    public void setPrimaryKeyValuesList(List<Map<String, Object>> primaryKeyValuesList) {
        this.primaryKeyValuesList = primaryKeyValuesList;
    }

    private List<String> fullyQualifiedPrimaryKeys;

    public List<String> getFullyQualifiedPrimaryKeys() {
        return fullyQualifiedPrimaryKeys;
    }

    public void setFullyQualifiedPrimaryKeys(List<String> fullyQualifiedPrimaryKeys) {
        this.fullyQualifiedPrimaryKeys = fullyQualifiedPrimaryKeys;
    }
}
