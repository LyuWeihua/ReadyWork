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

package work.ready.core.database.cleverorm;

import work.ready.core.database.DatabaseManager;
import work.ready.core.database.DbChangeEvent;
import work.ready.core.database.DbChangeListener;
import work.ready.core.database.Table;
import work.ready.core.database.cleverorm.config.CleverOrmSchemaConfig;
import work.ready.core.event.Event;
import work.ready.core.event.GeneralEvent;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;
import work.ready.core.tools.define.BiTuple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutoCodeSchemaConfig implements DbChangeListener {
    private static final Log logger = LogFactory.getLog(AutoCodeSchemaConfig.class);
    public static final String READY_ORM_SCHEMA_CONFIG = "ready_orm_schema_config";
    private boolean configTableReady = false;
    private final AutoCodeGenerator autoCodeGenerator;

    public AutoCodeSchemaConfig(AutoCodeGenerator autoCodeGenerator){
        this.autoCodeGenerator = autoCodeGenerator;
    }

    public void listen(){
        Ready.eventManager().addListener(this, "afterDatabaseInitListener",
                (setter -> setter.addName(Event.DATABASE_MANAGER_AFTER_INIT).setAsync(true)));
    }

    public void afterDatabaseInitListener(GeneralEvent event) {
        DatabaseManager dbManager = event.getObject();
        if(dbManager.getConfig() != null && dbManager.getDb().tableExists(READY_ORM_SCHEMA_CONFIG)) {
            try {
                dbManager.getDatasourceAgent(dbManager.getConfig().getName()).addMapping(READY_ORM_SCHEMA_CONFIG, "ID", CleverOrmSchemaConfig.class);
            } catch (Exception e) {
                logger.warn(e, "CleverORM: preparing database table %s for AutoCoder incurs exception", READY_ORM_SCHEMA_CONFIG);
            }
            Table configTable = dbManager.getTable(CleverOrmSchemaConfig.class);
            if (configTable != null) {
                configTableReady = true;
                updateCache(null);
                Ready.dbManager().addDbChangeListener(configTable.getName(), this);
            }
        }
    }

    private void updateCache(Object[] id){
        if(configTableReady){
            CleverOrmSchemaConfig schemaConfig = Ready.dbManager().createModel(CleverOrmSchemaConfig.class);
            List<CleverOrmSchemaConfig> configs = schemaConfig.findAll();
            Map<String, BiTuple<String, String>> cache = new HashMap<>();
            configs.forEach(config->{
                if(config.getStatus() == CleverOrmSchemaConfig.Status.NORMAL) {
                    cache.put(config.getClassName() + "_" + config.getMethodName(), new BiTuple<>(config.getDataSource(), config.getSql()));
                }
            });
            autoCodeGenerator.autoCoderSchemaConfig = cache;
        }
    }

    @Override
    public void onInserted(DbChangeEvent event) {
        updateCache(event.getId());
    }

    @Override
    public void onUpdated(DbChangeEvent event) {
        updateCache(event.getId());
    }

    @Override
    public void onDeleted(DbChangeEvent event) {
        updateCache(event.getId());
    }

    @Override
    public void onReplaced(DbChangeEvent event) { updateCache(event.getId()); }

    @Override
    public void onMerged(DbChangeEvent event) { updateCache(event.getId()); }
}
