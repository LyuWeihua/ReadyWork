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

package work.ready.core.apm.collector.jdbc;

import work.ready.core.apm.collector.jdbc.interceptor.ConnectionInterceptor;
import work.ready.core.apm.collector.jdbc.interceptor.StatementInterceptor;
import work.ready.core.apm.collector.jdbc.listener.JdbcExecuteListener;
import work.ready.core.apm.collector.jdbc.listener.JdbcSqlDebugListener;
import work.ready.core.apm.model.CollectorConfig;
import work.ready.core.database.DatabaseManager;
import work.ready.core.database.jdbc.event.JdbcEventListenerManager;
import work.ready.core.event.Event;
import work.ready.core.server.Ready;

import java.util.ArrayList;
import java.util.List;

public class JdbcConfig extends CollectorConfig {
    public static final String name = "jdbc";
    private boolean enableParam = true;
    private boolean enabled = true;
    private List<String> preparedStatement;
    private List<String> jdbcConnection;
    private long spend = -1;

    @Override
    public String getCollectorName() {
        return name;
    }

    @Override
    public void active() {
        
        Ready.eventManager().addListener((setter -> setter.addName(Event.DATABASE_MANAGER_AFTER_INIT).setAsync(true)), (event)->{
            ((DatabaseManager)event.getObject()).getSqlDebugger().addListener(new JdbcSqlDebugListener());
        });
    }

    @Override
    public List<Class<?>> getCollectorClasses() {
        List<Class<?>> interceptors = new ArrayList<>();
        if(!getJdbcConnection().isEmpty()) {
            interceptors.add(ConnectionInterceptor.class);
        }
        if(!getPreparedStatement().isEmpty()) {
            interceptors.add(StatementInterceptor.class);
        }
        return interceptors;
    }

    @Override
    public int getOrder() {
        return 2;
    }

    public JdbcConfig setEnableParam(boolean enableParam) {
        this.enableParam = enableParam;
        return this;
    }

    public JdbcConfig setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public List<String> getPreparedStatement() {
        if(preparedStatement == null) {
            preparedStatement = new ArrayList<>();

        }
        return preparedStatement;
    }

    public JdbcConfig setPreparedStatement(List<String> preparedStatement) {
        this.preparedStatement = preparedStatement;
        return this;
    }

    public JdbcConfig setPreparedStatement(String preparedStatement) {
        getPreparedStatement().add(preparedStatement);
        return this;
    }

    public List<String> getJdbcConnection() {
        if(jdbcConnection == null) {
            jdbcConnection = new ArrayList<>();

        }
        return jdbcConnection;
    }

    public JdbcConfig setJdbcConnection(List<String> jdbcConnection) {
        this.jdbcConnection = jdbcConnection;
        return this;
    }

    public JdbcConfig setJdbcConnection(String jdbcConnection) {
        getJdbcConnection().add(jdbcConnection);
        return this;
    }

    public JdbcConfig setSpend(long spend) {
        this.spend = spend;
        return this;
    }

    public boolean isEnableParam() {
        return enableParam;
    }

    public long getSpend() {
        return spend;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
