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

import org.apache.ignite.IgniteMessaging;
import org.apache.ignite.lang.IgniteBiPredicate;
import work.ready.cloud.ReadyCloud;
import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.cluster.CloudThreadFactory;
import work.ready.cloud.jdbc.ReadyJdbcDriver;
import work.ready.cloud.jdbc.oltp.JdbcUserOperationHandler;
import work.ready.core.aop.transformer.TransformerManager;
import work.ready.core.database.DatasourceAgent;
import work.ready.core.database.DatabaseManager;
import work.ready.core.database.DbChangeEvent;
import work.ready.core.database.datasource.DataSourceConfig;
import work.ready.core.event.cloud.Event;
import work.ready.core.event.GeneralEvent;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.module.ShutdownHook;
import work.ready.core.server.Ready;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EventHandler {

    private static final Log logger = LogFactory.getLog(EventHandler.class);
    private String coreDbPwd;

    public void listen(Cloud cloud){
        Ready.eventManager().addListener(this, "dbManagerCreateListener",
                (setter -> setter.addName(Event.DATABASE_MANAGER_CREATE)));
        dbChangeEventListener(cloud);
    }

    public EventHandler setCoreDbPwd(String pwd) {
        coreDbPwd = pwd;
        return this;
    }

    public void dbManagerCreateListener(GeneralEvent event) {
        if(ReadyCloud.isReady()){
            DatabaseManager dbManager = event.getSender();

            ExecutorService pool = Executors.newCachedThreadPool(new CloudThreadFactory());
            
            Ready.shutdownHook.add(ShutdownHook.STAGE_6, (inMs)->{
                pool.shutdown();
            });
            dbManager.setDbChangeEventFilter(0, evt -> {
                if(!evt.isInternal()) return; 
                pool.submit(()->{
                
                IgniteMessaging rmtMsg = Cloud.message(Cloud.cluster().forRemotes());
                
                rmtMsg.send("DbChangeEvent", evt.toMessage());
                });
            });

            if(dbManager.getDatabaseConfig().getH2server().isEnabled()) {
                dbManager.getDatabaseConfig().getH2server().setEnabled(false);
                logger.error("H2 Server disabled, it is not allowed to start in Cloud mode.");
            }
            if(!ReadyCloud.getConfig().isAllowH2DB()) {
                dbManager.getDatabaseConfig().getDataSource().forEach((name, ds) -> {
                    if (ds.isEnabled() && ds.getType().equals(DataSourceConfig.TYPE_H2)) {
                        ds.setEnabled(false);
                        logger.error("H2 JDBC Connection " + name + " disabled, it is not allowed to use in Cloud mode.");
                    }
                });
            }

            if(ReadyCloud.getNodeType().equals(Cloud.NodeType.APPLICATION_WITH_OLTP)) {
                Connection conn = null;
                var handler = TransformerManager.getInterceptor(JdbcUserOperationHandler.class);
                try {
                    if(handler != null) {
                        conn = DriverManager.getConnection(ReadyJdbcDriver.OLTP_URL_PREFIX + Cloud.getBindIp() + ":" + Cloud.cluster().localNode().attribute("clientListenerPort") + ";user=ignite;password=ignite");
                        Statement stmt = conn.createStatement();
                        ((JdbcUserOperationHandler)handler).setHandle(true);
                        stmt.executeUpdate("ALTER USER \"ignite\" WITH PASSWORD '" + coreDbPwd + "';");
                        stmt.executeUpdate("CREATE USER \"" + ReadyCloud.getConfig().getDbUser() + "\" WITH PASSWORD '" + ReadyCloud.getConfig().getDbPassword() + "';");
                        stmt.close();
                    }
                } catch (Exception e) { }
                finally {
                    if(handler != null) {
                        ((JdbcUserOperationHandler)handler).setHandle(false);
                    }
                    try {
                        if (conn != null) {
                            conn.close();
                        }
                    } catch (Exception e) {}
                }
            }
            DatasourceAgent igniteDb = dbManager.assignAgent(getCoreDbConfig().setPassword(coreDbPwd));
            dbManager.register(igniteDb, false); 

            DatasourceAgent publicDb = dbManager.assignAgent(getDbConfig());
            dbManager.register(publicDb); 

            ReadyDbSqlHandler handler = new ReadyDbSqlHandler();
            dbManager.getConfig((name, config)->{
                    if(config.getType().equals(DataSourceConfig.TYPE_IGNITE)) {
                        dbManager.addSqlExecuteHandlers(name, handler);
                    }
                });
        }
    }

    private void dbChangeEventListener(Cloud cloud) {
        
        IgniteBiPredicate<UUID, String> messageListener = new IgniteBiPredicate<>() {
            @Override
            public boolean apply(UUID nodeId, String msg) {
                if(logger.isDebugEnabled()) {
                    logger.debug("node received DbChangeEvent message [msg=" + msg + ", from remote=" + nodeId + "]");
                }
                DbChangeEvent event = DbChangeEvent.fromMessage(msg);
                event.setInternal(false); 
                Ready.dbManager().dbChangeNotify(event);
                return true; 
            }
        };
        IgniteMessaging rmtMsg = Cloud.message();
        rmtMsg.localListen("DbChangeEvent", messageListener);
    }

    private static DataSourceConfig getCoreDbConfig() {
        return new DataSourceConfig().setName(Cloud.CORE_DB_NAME)
                .setDriverClass(ReadyJdbcDriver.class.getCanonicalName())
                .setType(DataSourceConfig.TYPE_IGNITE)
                .setUsername("ignite")
                .setSchema(Cloud.CORE_DB_SCHEMA)
                .setJdbcUrl(ReadyJdbcDriver.OLTP_URL_PREFIX + Cloud.getBindIp() + ":" + Cloud.cluster().localNode().attribute("clientListenerPort") + ";"); 
    }

    private static DataSourceConfig getDbConfig() {
        return new DataSourceConfig().setName(Cloud.PUBLIC_DB_NAME)
                .setDriverClass(ReadyJdbcDriver.class.getCanonicalName())
                .setType(DataSourceConfig.TYPE_IGNITE)
                .setSchema("PUBLIC")
                .setUsername(ReadyCloud.getConfig().getDbUser())
                .setPassword(ReadyCloud.getConfig().getDbPassword())
                .setJdbcUrl(ReadyJdbcDriver.OLTP_URL_PREFIX + Cloud.getBindIp() + ":" + Cloud.cluster().localNode().attribute("clientListenerPort") + ";schema=PUBLIC;"); 
    }
}
