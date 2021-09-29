
/**
 *
 * Original work Copyright (c) 2011-2019, James Zhan 詹波 (jfinal@126.com).
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

package work.ready.core.database.transaction;

import work.ready.core.aop.Interceptor;
import work.ready.core.aop.Invocation;
import work.ready.core.database.DatabaseException;
import work.ready.core.database.NestedTransactionHelpException;
import work.ready.core.database.annotation.Transactional;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;

import java.sql.Connection;
import java.util.HashMap;

public class TransactionInterceptor implements Interceptor {
    private static final Log logger = LogFactory.getLog(TransactionInterceptor.class);
    private final LocalTransactionManager txManager;

    public TransactionInterceptor() {
        var transactionManager = Ready.dbManager().getTransactionManager();
        if(!(transactionManager instanceof LocalTransactionManager)) {
            throw new RuntimeException("It is not supported by " + transactionManager.getClass().getSimpleName() + ", LocalTransactionManager is required.");
        }
        txManager = (LocalTransactionManager)transactionManager;
    }

    @Override
    public void intercept(Invocation inv) throws Throwable {
        Transactional transactional = inv.getMethod().getAnnotation(Transactional.class);
        if(transactional == null) {
            transactional = inv.getMethod().getDeclaringClass().getAnnotation(Transactional.class);
        }
        if(txManager.inLocalTransaction()) {
            if(transactional.propagation().equals(Propagation.NEVER)) {
                throw new RuntimeException("transactional logic with NEVER propagation.");
            }
            inv.invoke();
            return;
        }

        if(transactional.propagation().equals(Propagation.MANDATORY)) {
            throw new RuntimeException("transactional logic with MANDATORY propagation, but there is no existing transaction.");
        }

        if(transactional.propagation().equals(Propagation.SUPPORTS) || transactional.propagation().equals(Propagation.NEVER)) {
            inv.invoke();
            return;
        }

        txManager.startTransaction();
        Long transactionId = txManager.getTransactionId();
        HashMap<String, Connection> connections = null;
        try {
            inv.invoke();
            connections = txManager.endTransaction();
            if(connections != null) {
                for (var entry : connections.entrySet()) {
                    entry.getValue().commit();
                    logger.debug("finish transaction %s, final commit for %s.", transactionId, entry.getKey());
                }
            }
        } catch (NestedTransactionHelpException e) {
            if(connections == null) {
                connections = txManager.endTransaction();
            }
            if(connections != null) {
                for (var entry : connections.entrySet()) {
                    try {
                        entry.getValue().rollback();
                        logger.warn("failed transaction %s, rollback for %s.", transactionId, entry.getKey());
                    } catch (Exception e1) {
                        logger.error(e1, "failed transaction %s, %s rollback exception", transactionId, entry.getKey());
                    }
                }
            }
        } catch (Throwable t) {
            if(connections == null) {
                connections = txManager.endTransaction();
            }
            if(connections != null) {
                for (var entry : connections.entrySet()) {
                    try {
                        entry.getValue().rollback();
                        logger.warn("failed transaction %s, rollback for %s.", transactionId, entry.getKey());
                    } catch (Exception e1) {
                        logger.error(e1, "failed transaction %s, %s rollback exception", transactionId, entry.getKey());
                    }
                }
            }
            throw t instanceof RuntimeException ? (RuntimeException)t : new DatabaseException(t);
        } finally {
            if(connections != null) {
                for (var entry : connections.entrySet()) {
                    try {
                        entry.getValue().close();
                    } catch (Throwable t) {
                        logger.error(t,"%s transaction exception, close connection failed", entry.getKey());	
                    }
                }
            }
        }
    }
}
