/**
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
 */
package work.ready.core.database.jdbc.hikari;

import work.ready.core.database.jdbc.event.JdbcEventListenerManager;
import work.ready.core.database.jdbc.common.ConnectionInformation;
import work.ready.core.database.jdbc.event.JdbcEventListener;
import work.ready.core.database.jdbc.hikari.pool.HikariProxyConnection;
import work.ready.core.tools.define.CopyOnWriteMap;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ReadyDataSource extends HikariDataSource {
    public static final String isEnabledTransaction = "isEnabledTransaction";
    private Map<String, Object> attribute = new CopyOnWriteMap<>();

    public ReadyDataSource() {
        super();
    }

    public ReadyDataSource(HikariConfig configuration)
    {
        super(configuration);
    }

    @Override
    public Connection getConnection() throws SQLException
    {
        final long start = System.nanoTime();

        final Connection conn;
        final JdbcEventListener jdbcEventListener = JdbcEventListenerManager.getListener();
        final ConnectionInformation connectionInformation = ConnectionInformation.fromDataSource(this);

        try {
            jdbcEventListener.onBeforeGetConnection(connectionInformation);
            conn = connectionInformation.getConnection() != null ? connectionInformation.getConnection() : super.getConnection();
            if(conn instanceof HikariProxyConnection) {
                ((HikariProxyConnection) conn).advancedFeatureSupport(connectionInformation, jdbcEventListener);
            }
            connectionInformation.setConnection(conn);
            if (conn.getMetaData() != null) {
                connectionInformation.setUrl(conn.getMetaData().getURL());
            }
            connectionInformation.setTimeToGetConnectionNs(System.nanoTime() - start);
            jdbcEventListener.onAfterGetConnection(connectionInformation, null);
        } catch (SQLException e) {
            connectionInformation.setTimeToGetConnectionNs(System.nanoTime() - start);
            jdbcEventListener.onAfterGetConnection(connectionInformation, e);
            if(connectionInformation.getConnection() == null) {
                throw e;
            }
        }

        return connectionInformation.getConnection();
    }

    public boolean isEnabledTransaction() {
        if(attribute.containsKey(isEnabledTransaction)) {
            return attribute.get(isEnabledTransaction).equals(true);
        }
        return false;
    }

    public Map<String, Object> getAttribute() {
        return Collections.unmodifiableMap(attribute);
    }

    public Object getAttribute(String name) {
        return attribute.get(name);
    }

    public void setAttribute(String name, Object value) {
        attribute.put(name, value);
    }
}
