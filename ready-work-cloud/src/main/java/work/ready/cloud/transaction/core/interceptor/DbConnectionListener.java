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

package work.ready.cloud.transaction.core.interceptor;

import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.transaction.core.context.DtxThreadContext;
import work.ready.cloud.transaction.core.transaction.TxConnectionProxy;
import work.ready.cloud.transaction.core.transaction.TransactionResourceHandler;
import work.ready.core.database.jdbc.common.ConnectionInformation;
import work.ready.core.database.jdbc.event.ConnectionListener;
import work.ready.core.database.jdbc.hikari.ReadyDataSource;
import work.ready.core.database.jdbc.hikari.pool.HikariProxyConnection;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class DbConnectionListener implements ConnectionListener {

    private static final Log logger = LogFactory.getLog(DbConnectionListener.class);

    @Override
    public void onBeforeGetConnection(ConnectionInformation connectionInformation) {

    }

    @Override
    public void onAfterGetConnection(ConnectionInformation connectionInformation, SQLException e) {
        DtxThreadContext dtxThreadContext = DtxThreadContext.current();
        if (dtxThreadContext != null && dtxThreadContext.isActivate() && ((ReadyDataSource)connectionInformation.getDataSource()).isEnabledTransaction()) {
            String transactionType = dtxThreadContext.getTransactionType();
            TransactionResourceHandler resourceHandler = Cloud.getTransactionManager().getTransactionResourceHandler(transactionType);
            try {
                HikariProxyConnection connection = (HikariProxyConnection) connectionInformation.getConnection();
                if(connection != null) {
                    String dataSource = connection.getPoolName();
                    Connection newConnection = resourceHandler.prepareConnection(dataSource, connectionInformation::getConnection);
                    connectionInformation.setConnection(newConnection);
                    if(newConnection instanceof TxConnectionProxy && !((TxConnectionProxy) newConnection).isWrapperFor(connection)) {
                        
                        try {
                            connection.close();
                        } catch (SQLException ignore) {
                        }
                    }
                    logger.debug("from %s get connection: %s.", dataSource, newConnection);
                }
            } catch (Throwable ex) {
                logger.error(ex, "Transaction resource exception: ");
            }
        }
    }

    @Override
    public void onAfterConnectionClose(ConnectionInformation connectionInformation, SQLException e) {

    }
}
