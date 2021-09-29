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
package work.ready.cloud.transaction.core.check;

import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.transaction.TransactionConfig;
import work.ready.cloud.transaction.core.context.DtxNodeContext;
import work.ready.cloud.transaction.core.context.TxContext;
import work.ready.cloud.transaction.core.controller.TransactionClearancer;
import work.ready.cloud.transaction.core.corelog.aspect.CoreLogger;
import work.ready.cloud.transaction.common.exception.TransactionClearException;
import work.ready.cloud.transaction.logger.TxLogger;
import work.ready.cloud.transaction.common.Transaction;
import work.ready.cloud.transaction.core.message.ReliableMessenger;
import work.ready.cloud.transaction.core.message.ExceptionReporter;
import work.ready.cloud.cluster.common.MessageException;
import work.ready.cloud.transaction.common.message.params.TxExceptionParams;
import work.ready.core.ioc.annotation.DisposableBean;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

public class DefaultTransactionChecker implements TransactionChecker, DisposableBean {
    private static final Log logger = LogFactory.getLog(DefaultTransactionChecker.class);
    private static final Map<String, ScheduledFuture> delayTasks = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduledExecutorService =
            Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

    private static final TxLogger txLogger = TxLogger.newLogger(DefaultTransactionChecker.class);

    private TransactionClearancer transactionClearancer;

    private final ReliableMessenger reliableMessenger;

    private final TransactionConfig transactionConfig;

    private final CoreLogger coreLogger;

    private final ExceptionReporter exceptionReporter;

    private final DtxNodeContext nodeContext;

    public DefaultTransactionChecker() {
        this.transactionConfig = Cloud.getTransactionManager().getConfig();
        this.coreLogger = Cloud.getTransactionManager().getCoreLogger();
        this.exceptionReporter = Cloud.getTransactionManager().getExceptionReporter();
        this.nodeContext = Cloud.getTransactionManager().getNodeContext();
        this.reliableMessenger = Cloud.getTransactionManager().getMessenger();
    }

    @Override
    public void setTransactionClearancer(TransactionClearancer transactionClearancer) {
        this.transactionClearancer = transactionClearancer;
    }

    @Override
    public void startDelayChecking(String groupId, String unitId, String transactionType) {
        txLogger.taskTrace(groupId, unitId, "start delay checking task");
        ScheduledFuture scheduledFuture = scheduledExecutorService.schedule(() -> {
            try {
                TxContext txContext = nodeContext.txContext(groupId);
                if (Objects.nonNull(txContext)) {
                    synchronized (txContext.getLock()) {
                        txLogger.taskTrace(groupId, unitId, "checking waiting for business code finish.");
                        txContext.getLock().wait();
                    }
                }
                int state = reliableMessenger.askTransactionState(groupId, unitId);
                txLogger.taskTrace(groupId, unitId, "ask transaction state %s", state);
                if (state == -1) {
                    txLogger.error(this.getClass().getSimpleName(), "delay clean transaction error.");
                    onAskTransactionStateException(groupId, unitId, transactionType);
                } else {
                    transactionClearancer.clean(groupId, unitId, transactionType, state);
                    coreLogger.clearLog(groupId, unitId);
                }

            } catch (MessageException e) {
                onAskTransactionStateException(groupId, unitId, transactionType);
            } catch (TransactionClearException | InterruptedException e) {
                txLogger.error(this.getClass().getSimpleName(), "%s clean transaction error.", transactionType);
            }
        }, transactionConfig.getTxTimeout(), TimeUnit.MILLISECONDS);
        delayTasks.put(groupId + unitId, scheduledFuture);
    }

    @Override
    public void stopDelayChecking(String groupId, String unitId) {
        ScheduledFuture scheduledFuture = delayTasks.get(groupId + unitId);
        if (Objects.nonNull(scheduledFuture)) {
            txLogger.taskTrace(groupId, unitId, "cancel %s:%s checking.", groupId, unitId);
            scheduledFuture.cancel(true);
        }
    }

    private void onAskTransactionStateException(String groupId, String unitId, String transactionType) {
        try {
            
            exceptionReporter.reportTransactionState(groupId, unitId, TxExceptionParams.ASK_ERROR, 0);
            logger.warn("%s > has compensation info!", transactionType);

            transactionClearancer.cleanWithoutAspectLog(groupId, unitId, transactionType, 0);
        } catch (TransactionClearException e) {
            txLogger.error(groupId, unitId, Transaction.TAG_TASK, "%s > clean transaction error.", transactionType);
        }
    }

    @Override
    public void destroy() {
        scheduledExecutorService.shutdown();
        try {
            
            scheduledExecutorService.awaitTermination(6, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
    }
}
