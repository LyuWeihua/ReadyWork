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

package work.ready.core.database;

import work.ready.core.config.BaseConfig;
import work.ready.core.database.datasource.DataSourceConfig;
import work.ready.core.database.datasource.H2serverConfig;
import work.ready.core.database.transaction.LocalTransactionConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseConfig extends BaseConfig {

    private boolean sqlDebug = false;
    private Map<String, DataSourceConfig> dataSource = new HashMap<>();
    private ModelConfig modelConfig = new ModelConfig();
    private H2serverConfig h2server = new H2serverConfig();
    private LocalTransactionConfig localTransactionConfig = new LocalTransactionConfig();

    public boolean isSqlDebug() {
        return sqlDebug;
    }

    public void setSqlDebug(boolean sqlDebug) {
        this.sqlDebug = sqlDebug;
    }

    public Map<String, DataSourceConfig> getDataSource(){
        return dataSource;
    }

    public DataSourceConfig getDataSource(String name){
        return dataSource.get(name);
    }

    public void setDataSource(String name, DataSourceConfig dataSource) {
        this.dataSource.put(name, dataSource);
    }

    public ModelConfig getModelConfig() {
        return modelConfig;
    }

    public H2serverConfig getH2server(){
        return h2server;
    }

    public LocalTransactionConfig getLocalTransactionConfig() {
        return localTransactionConfig;
    }

    @Override
    public void validate() { 

    }
}
