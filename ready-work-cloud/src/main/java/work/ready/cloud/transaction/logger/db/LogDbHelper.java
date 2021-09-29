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
package work.ready.cloud.transaction.logger.db;

import work.ready.cloud.cluster.Cloud;
import work.ready.core.database.datasource.DataSourceConfig;
import work.ready.core.database.datasource.HikariCp;
import work.ready.core.database.handlers.ScalarHandler;
import work.ready.core.database.jdbc.hikari.ReadyDataSource;
import work.ready.core.database.query.QueryRunner;
import work.ready.core.database.query.ResultSetHandler;
import work.ready.core.ioc.annotation.DisposableBean;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;

import java.sql.SQLException;

import static work.ready.cloud.transaction.DistributedTransactionManager.LOCAL_STORAGE_PATH;

public class LogDbHelper implements DisposableBean {
    private static final Log logger = LogFactory.getLog(LogDbHelper.class);
    
    private ReadyDataSource hikariDataSource;

    private QueryRunner queryRunner;

    public QueryRunner getQueryRunner() {
        return queryRunner;
    }

    public void init(String datasource, boolean createH2) {
        if(!createH2) {
            hikariDataSource = (ReadyDataSource) Ready.dbManager().getConfig(datasource).getDataSource();
            hikariDataSource.setAttribute(ReadyDataSource.isEnabledTransaction, false);
            queryRunner = new QueryRunner(hikariDataSource);
            logger.info("TxLogger is using datasource %s.", datasource);
        } else {
            String dbPath = "/" + LOCAL_STORAGE_PATH + "/" + Cloud.getConsistentId() + "_tx_log.db";
            DataSourceConfig hikariConfig = new DataSourceConfig();
            hikariConfig.setDriverClass(org.h2.Driver.class.getName());
            hikariConfig.setJdbcUrl(String.format("jdbc:h2:file:%s;MODE=MySQL;LOCK_MODE=3", dbPath));
            hikariDataSource = ((ReadyDataSource)new HikariCp(hikariConfig.setName(datasource)).getDataSource());
            hikariDataSource.setAttribute(ReadyDataSource.isEnabledTransaction, false);
            queryRunner = new QueryRunner(hikariDataSource);

            logger.info("Init LogDbHelper DATABASE at %s", dbPath);
        }
    }

    public int update(String sql, Object... params) {
        try {
            return queryRunner.update(sql, params);
        } catch (SQLException e) {
            logger.error(e, "update error");
            return 0;
        }
    }

    public <T> T query(String sql, ResultSetHandler<T> rsh, Object... params) {
        try {
            return queryRunner.query(sql, rsh, params);
        } catch (SQLException e) {
            logger.error(e, "query error");
            return null;
        }
    }

    public <T> T query(String sql, ScalarHandler<T> scalarHandler, Object... params) {
        try {
            return queryRunner.query(sql, scalarHandler, params);
        } catch (SQLException e) {
            logger.error(e, "query error");
            return null;
        }
    }

    @Override
    public void destroy() throws Exception {
        if(hikariDataSource != null) {

        }
        logger.info("LogDbHelper closed.");
    }
}
