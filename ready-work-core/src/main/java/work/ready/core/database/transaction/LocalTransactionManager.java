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

import work.ready.core.aop.AopComponent;
import work.ready.core.database.annotation.Transactional;
import work.ready.core.database.jdbc.event.JdbcEventListenerManager;
import work.ready.core.server.Ready;

import java.sql.Connection;
import java.util.HashMap;

public class LocalTransactionManager implements TransactionManager {
    private final ThreadLocal<Long> currentTransaction;
    private final LocalTransactionConfig config;
    private final DbConnectionListener connectionListener;

    public LocalTransactionManager() {
        config = Ready.dbManager().getDatabaseConfig().getLocalTransactionConfig();
        if(config.isInheritedThread()) {
            currentTransaction = new InheritableThreadLocal<>();
        } else {
            currentTransaction = new ThreadLocal<>();
        }
        connectionListener = new DbConnectionListener(this);
        JdbcEventListenerManager.addListener(connectionListener);
        Ready.interceptorManager().addAopComponent(
                new AopComponent()
                        .setAnnotation(Transactional.class)
                        .setInterceptorClass(TransactionInterceptor.class)
        );
    }

    public LocalTransactionConfig getConfig() {
        return config;
    }

    @Override
    public boolean inLocalTransaction() {
        return currentTransaction.get() != null;
    }

    public Long getTransactionId() {
        return currentTransaction.get();
    }

    public void startTransaction() {
        currentTransaction.set(Ready.getId());
    }

    public HashMap<String, Connection> endTransaction() {
        Long transactionId = currentTransaction.get();
        currentTransaction.remove();
        return connectionListener.endTransaction(transactionId);
    }
}
