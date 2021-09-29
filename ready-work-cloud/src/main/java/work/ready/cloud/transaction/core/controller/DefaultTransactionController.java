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
package work.ready.cloud.transaction.core.controller;

import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.cluster.common.MessageException;
import work.ready.cloud.transaction.core.context.DtxThreadContext;
import work.ready.cloud.transaction.core.check.TransactionChecker;
import work.ready.cloud.transaction.core.context.DtxNodeContext;
import work.ready.cloud.transaction.core.corelog.aspect.CoreLogger;
import work.ready.cloud.transaction.core.interceptor.TransactionInfo;
import work.ready.cloud.transaction.core.message.ReliableMessenger;
import work.ready.cloud.transaction.common.Transaction;
import work.ready.cloud.transaction.common.exception.TxBusinessException;
import work.ready.cloud.transaction.common.exception.TransactionClearException;
import work.ready.cloud.transaction.common.exception.TransactionException;
import work.ready.cloud.transaction.logger.TxLogger;
import work.ready.core.server.Ready;

import java.util.Arrays;

public class DefaultTransactionController implements TransactionController {

    private static final TxLogger txLogger = TxLogger.newLogger(TransactionController.class);

    private final CoreLogger coreLogger;

    private final TransactionChecker transactionChecker;

    private final TransactionExceptionHandler transactionExceptionHandler;

    private final TransactionClearancer transactionClearancer;

    private final ReliableMessenger reliableMessenger;

    private final DtxNodeContext nodeContext;

    public DefaultTransactionController() {
        this.coreLogger = Cloud.getTransactionManager().getCoreLogger();
        this.transactionChecker = Cloud.getTransactionManager().getChecker();
        this.transactionClearancer = Cloud.getTransactionManager().getClearancer();
        this.reliableMessenger = Cloud.getTransactionManager().getMessenger();
        this.nodeContext = Cloud.getTransactionManager().getNodeContext();
        this.transactionExceptionHandler = Ready.beanManager().get(TransactionExceptionHandler.class, DefaultTransactionExceptionHandler.class);
    }

    @Override
    public void createGroup(String groupId, String unitId, TransactionInfo transactionInfo, String transactionType)
            throws TransactionException {
        
        try {
            
            txLogger.txTrace(groupId, unitId,
                    "create group > transaction type: %s", transactionType);
            
            reliableMessenger.createGroup(groupId);
            
            coreLogger.trace(groupId, unitId, transactionInfo);
        } catch (MessageException e) {
            
            transactionExceptionHandler.handleCreateGroupMessageException(groupId, e);
        } catch (TxBusinessException e) {
            
            transactionExceptionHandler.handleCreateGroupBusinessException(groupId, e.getCause());
        }
        txLogger.txTrace(groupId, unitId, "create group over");
    }

    @Override
    public void joinGroup(String groupId, String unitId, String transactionType, TransactionInfo transactionInfo)
            throws TransactionException {
        try {
            txLogger.txTrace(groupId, unitId, "join group > transaction type: %s", transactionType);

            reliableMessenger.joinGroup(groupId, unitId, transactionType, DtxThreadContext.transactionState(nodeContext.dtxState(groupId)));

            txLogger.txTrace(groupId, unitId, "join group message over.");

            transactionChecker.startDelayChecking(groupId, unitId, transactionType);

            coreLogger.trace(groupId, unitId, transactionInfo);
        } catch (MessageException e) {
            transactionExceptionHandler.handleJoinGroupMessageException(Arrays.asList(groupId, unitId, transactionType), e);
        } catch (TxBusinessException e) {
            transactionExceptionHandler.handleJoinGroupBusinessException(Arrays.asList(groupId, unitId, transactionType), e);
        }
        txLogger.txTrace(groupId, unitId, "join group logic over");
    }

    @Override
    public void notifyGroup(String groupId, String unitId, String transactionType, int state) {
        try {
            txLogger.txTrace(
                    groupId, unitId, "notify group > transaction type: %s, state: %s.", transactionType, state);
            if (nodeContext.isDtxTimeout()) {
                txLogger.trace(groupId, unitId, Transaction.TX_ERROR, "dtx timeout.");
                throw new TxBusinessException("dtx timeout.");
            }
            state = reliableMessenger.notifyGroup(groupId, state);
            transactionClearancer.clean(groupId, unitId, transactionType, state);
        } catch (TransactionClearException e) {
            txLogger.trace(groupId, unitId, Transaction.TX_ERROR, "clean transaction fail.");
        } catch (MessageException e) {
            transactionExceptionHandler.handleNotifyGroupMessageException(Arrays.asList(groupId, state, unitId, transactionType), e);
        } catch (TxBusinessException e) {
            
            transactionExceptionHandler.handleNotifyGroupBusinessException(Arrays.asList(groupId, state, unitId, transactionType), e);
        }
        txLogger.txTrace(groupId, unitId, "notify group end, state: %s.", state);
    }

}
