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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableStruct {

    public TableStruct(String tableName) {
        this.tableName = tableName;
    }

    private String tableName;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    private List<String> primaryKeys = new ArrayList<>();

    public List<String> getPrimaryKeys() {
        return primaryKeys;
    }

    public void setPrimaryKeys(List<String> primaryKeys) {
        this.primaryKeys = primaryKeys;
    }

    private List<String> fullyQualifiedPrimaryKeys;

    private Map<String, String> columns = new HashMap<>();

    public Map<String, String> getColumns() {
        return columns;
    }

    public List<String> getFullyQualifiedPrimaryKeys() {
        if (this.fullyQualifiedPrimaryKeys != null) {
            return this.fullyQualifiedPrimaryKeys;
        }
        List<String> pks = new ArrayList<>();
        this.getPrimaryKeys().forEach(key -> pks.add(tableName + '.' + key));
        this.fullyQualifiedPrimaryKeys = pks;
        return this.fullyQualifiedPrimaryKeys;
    }
}
