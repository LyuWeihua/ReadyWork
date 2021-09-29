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

import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;

import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class DefaultSqlDebugger implements SqlDebugger {
    private static final Log logger = LogFactory.getLog(DefaultSqlDebugger.class);
    private final List<SqlDebugger> listeners = new ArrayList<>();
    private AtomicLong counter;
    private ThreadLocal<Object[]> debug;
    private final boolean isDebug;

    public DefaultSqlDebugger() {
        isDebug = Ready.dbManager().databaseConfig.isSqlDebug();
        if(isDebug) {
            counter = new AtomicLong(1);
            debug = ThreadLocal.withInitial(()->new Object[4]);
        }
    }

    @Override
    public void beforeAudit(String datasource, String sql) {
        if(isDebug) {
            debug.get()[0] = counter.getAndIncrement();
            debug.get()[1] = sql;
            logger.debug("SqlDebugger[%d] DataSource: %s, SQL: \n%s", debug.get()[0], datasource, sql);
        }
        if(listeners.size() > 0) {
            listeners.forEach(l->l.beforeAudit(datasource, sql));
        }
    }

    @Override
    public void beforeExecute(String datasource, String sql) {
        if(isDebug) {
            debug.get()[2] = sql;
            debug.get()[3] = System.nanoTime();
            logger.debug("SqlDebugger[%d] DataSource: %s, SQL: \n%s", debug.get()[0], datasource, sql);
        }
        if(listeners.size() > 0) {
            listeners.forEach(l->l.beforeExecute(datasource, sql));
        }
    }

    @Override
    public void afterExecute(long timeElapsedNanos, SQLException e) {
        if(isDebug) {
            if (e != null) {
                logger.error("SqlDebugger[%d] success: %s, total elapsed: %sms", debug.get()[0], false, Duration.ofNanos(timeElapsedNanos).toMillis());
            } else {
                logger.debug("SqlDebugger[%d] success: %s, execution elapsed: %sms, total elapsed: %sms", debug.get()[0], true, Duration.ofNanos(System.nanoTime() - (long) debug.get()[3]).toMillis(), Duration.ofNanos(timeElapsedNanos).toMillis());
            }
        }
        if(listeners.size() > 0) {
            listeners.forEach(l->l.afterExecute(timeElapsedNanos, e));
        }
    }

    @Override
    public void addListener(SqlDebugger listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(SqlDebugger listener) {
        listeners.remove(listener);
    }
}
