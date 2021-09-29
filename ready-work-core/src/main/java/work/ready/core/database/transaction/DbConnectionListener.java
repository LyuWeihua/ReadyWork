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

package work.ready.core.database.transaction;

import work.ready.core.database.jdbc.common.ConnectionInformation;
import work.ready.core.database.jdbc.event.ConnectionListener;
import work.ready.core.database.jdbc.hikari.ReadyDataSource;
import work.ready.core.database.jdbc.hikari.pool.HikariProxyConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

class DbConnectionListener implements ConnectionListener {
    private static final ConcurrentHashMap<Long, HashMap<String, Connection>> transactionConnections = new ConcurrentHashMap<>(16);
    private final LocalTransactionManager manager;

    DbConnectionListener(LocalTransactionManager manager) {
        this.manager = manager;
    }

    HashMap<String, Connection> endTransaction(Long transactionId) {
        return transactionConnections.remove(transactionId);
    }

    @Override
    public void onBeforeGetConnection(ConnectionInformation connectionInformation) {

    }

    @Override
    public void onAfterGetConnection(ConnectionInformation connectionInformation, SQLException e) {
        if(manager.getTransactionId() != null) {
            HikariProxyConnection connection = (HikariProxyConnection) connectionInformation.getConnection();
            if (connection != null && ((ReadyDataSource)connectionInformation.getDataSource()).isEnabledTransaction()) {
                synchronized (manager.getTransactionId()) {
                    HashMap<String, Connection> current = transactionConnections.computeIfAbsent(manager.getTransactionId(), (id) -> new HashMap<>(6));
                    String dataSource = connection.getPoolName();
                    if (current.containsKey(dataSource)) {
                        connectionInformation.setConnection(current.get(dataSource));
                        
                        try {
                            connection.close();
                        } catch (SQLException ignore) {
                        }
                    } else {
                        try {
                            current.put(dataSource, connection);
                            connection.setAutoCommit(false);
                        } catch (SQLException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onAfterConnectionClose(ConnectionInformation connectionInformation, SQLException e) {

    }
}
