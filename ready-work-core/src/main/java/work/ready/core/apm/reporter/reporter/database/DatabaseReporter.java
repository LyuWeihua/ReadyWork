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

package work.ready.core.apm.reporter.reporter.database;

import work.ready.core.apm.ApmManager;
import work.ready.core.apm.collector.jdbc.JdbcContext;
import work.ready.core.apm.model.Span;
import work.ready.core.apm.reporter.Reporter;
import work.ready.core.database.*;
import work.ready.core.database.Record;
import work.ready.core.event.Event;
import work.ready.core.event.GeneralEvent;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class DatabaseReporter implements Reporter {
    private static final Log logger = LogFactory.getLog(DatabaseReporter.class);
    public static final String APM_DATABASE_STORE = "ready_apm_db_store";
    public static final String PRIMARY_KEY = "ID";
    private static final String name = "database";
    private boolean isReady = false;
    private DbPro db;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int report(Span span) {
        JdbcContext.turnOff();
        db.skipAudit().save(APM_DATABASE_STORE, PRIMARY_KEY, new Record(
                Map.of(
                        BaseApmDatabaseStore.Column.id.get(), Ready.getId(),
                        BaseApmDatabaseStore.Column.span.get(), span.toString(),
                        BaseApmDatabaseStore.Column.createdTime.get(), Ready.now()
                )));
        JdbcContext.turnOn();
        return 1;
    }

    @Override
    public int report(List<Span> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        List<Record> recordList = new ArrayList<>();
        Date now = Ready.now();
        list.forEach(span->
            recordList.add(new Record(
                    Map.of(
                            BaseApmDatabaseStore.Column.id.get(), Ready.getId(),
                            BaseApmDatabaseStore.Column.span.get(), span.toString(),
                            BaseApmDatabaseStore.Column.createdTime.get(), now
                    )))
        );
        JdbcContext.turnOff();
        db.skipAudit().batchSave(APM_DATABASE_STORE, recordList, list.size());
        JdbcContext.turnOn();
        return list.size();
    }

    @Override
    public int init() {
        Ready.eventManager().addListener(this, "afterDatabaseInitListener",
                (setter -> setter.addName(Event.DATABASE_MANAGER_AFTER_INIT).setAsync(true)));
        return 0;
    }

    public void afterDatabaseInitListener(GeneralEvent event) {
        DatabaseManager dbManager = event.getObject();
        String datasource = null;
        try {
            datasource = ApmManager.getConfig().getReporter().getDatasource();
            if(datasource == null) {
                datasource = dbManager.getConfig().getName();
            }
            dbManager.getDatasourceAgent(datasource).addMapping(APM_DATABASE_STORE, PRIMARY_KEY, ApmDatabaseStore.class);
        } catch (Exception e) {
            logger.error(e,"Apm datasource %s initialization incurs exception", datasource);
        }
        Table apmStoreTable = dbManager.getTable(ApmDatabaseStore.class);
        if(apmStoreTable != null) {
            db = dbManager.getDb().use(datasource);
            isReady = true;
        }
    }

    @Override
    public boolean isReady() {
        return isReady;
    }
}
