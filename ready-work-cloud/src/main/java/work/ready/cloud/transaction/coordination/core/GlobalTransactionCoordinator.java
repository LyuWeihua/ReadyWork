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
package work.ready.cloud.transaction.coordination.core;

import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.transaction.common.exception.TransactionException;
import work.ready.cloud.transaction.coordination.message.DefaultNotifyExceptionHandler;
import work.ready.cloud.transaction.logger.TxLogger;
import work.ready.cloud.transaction.coordination.message.MessageCreator;
import work.ready.cloud.transaction.coordination.message.NotifyExceptionHandler;
import work.ready.cloud.transaction.coordination.core.storage.TransactionUnit;
import work.ready.cloud.transaction.coordination.support.service.TxExceptionService;
import work.ready.cloud.transaction.common.message.CoordinatorClient;
import work.ready.cloud.cluster.common.MessageBody;
import work.ready.cloud.cluster.common.MessageException;
import work.ready.cloud.transaction.common.message.params.NotifyUnitParams;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class GlobalTransactionCoordinator implements TransactionCoordinator {
    private static final Log logger = LogFactory.getLog(GlobalTransactionCoordinator.class);
    private static final TxLogger txLogger = TxLogger.newLogger(GlobalTransactionCoordinator.class);

    private final NotifyExceptionHandler notifyExceptionHandler;

    private final CoordinatorClient messageClient;

    private final TxExceptionService exceptionService;

    private final DtxContextRegistry dtxContextRegistry;

    public GlobalTransactionCoordinator() {
        this.notifyExceptionHandler = Ready.beanManager().get(NotifyExceptionHandler.class, DefaultNotifyExceptionHandler.class);
        this.exceptionService = Cloud.getTransactionManager().getTxExceptionService();
        this.messageClient = Cloud.getTransactionManager().getCoordinatorClient();
        this.dtxContextRegistry = Cloud.getTransactionManager().getCoordinator().getContextRegistry();
    }

    @Override
    public void begin(String groupId) throws TransactionException {
        try {
            dtxContextRegistry.create(groupId);
        } catch (TransactionException e) {
            throw new TransactionException(e);
        }
    }

    @Override
    public void join(DtxContext dtxContext, String unitId, String unitType, String nodeName, int userState) throws TransactionException {
        
        if (userState == 0) {
            dtxContext.resetTransactionState(0);
        }
        TransactionUnit transactionUnit = new TransactionUnit();
        transactionUnit.setNodeName(nodeName);
        transactionUnit.setUnitId(unitId);
        transactionUnit.setType(unitType);
        dtxContext.join(transactionUnit);
    }

    @Override
    public void commit(DtxContext dtxContext) throws TransactionException {
        notifyTransaction(dtxContext, 1);
    }

    @Override
    public void rollback(DtxContext dtxContext) throws TransactionException {
        notifyTransaction(dtxContext, 0);
    }

    @Override
    public void close(String groupId) {
        dtxContextRegistry.destroyContext(groupId);
    }

    @Override
    public int transactionState(String groupId) {
        int state = exceptionService.transactionState(groupId);
        
        if (state != -1) {
            return state;
        }
        return dtxContextRegistry.transactionState(groupId);
    }

    @Override
    public int transactionStateFromFastStorage(String groupId) {
        return dtxContextRegistry.transactionState(groupId);
    }

    private void notifyTransaction(DtxContext dtxContext, int transactionState) throws TransactionException {
        List<TransactionUnit> transactionUnits = dtxContext.transactionUnits();
        logger.debug("group[%s]'s transaction units: %s", dtxContext.getGroupId(), transactionUnits);
        for (TransactionUnit transUnit : transactionUnits) {
            NotifyUnitParams notifyUnitParams = new NotifyUnitParams();
            notifyUnitParams.setGroupId(dtxContext.getGroupId());
            notifyUnitParams.setUnitId(transUnit.getUnitId());
            notifyUnitParams.setType(transUnit.getType());
            notifyUnitParams.setState(transactionState);
            txLogger.txTrace(dtxContext.getGroupId(), notifyUnitParams.getUnitId(), "notify %s's unit: %s",
                    transUnit.getNodeName(), transUnit.getUnitId());
            try {
                UUID nodeId = messageClient.getNodeId(transUnit.getNodeName());
                if (nodeId == null) {
                    
                    throw new MessageException("offline node.");
                }
                MessageBody respMsg =
                        messageClient.request(nodeId, MessageCreator.notifyUnit(notifyUnitParams));
                if (!respMsg.isStateOk()) {
                    List<Object> params = Arrays.asList(notifyUnitParams, transUnit.getNodeName());
                    notifyExceptionHandler.handleNotifyUnitBusinessException(params, respMsg.loadBean(Throwable.class));
                }
            } catch (MessageException e) {
                List<Object> params = Arrays.asList(notifyUnitParams, transUnit.getNodeName());
                notifyExceptionHandler.handleNotifyUnitMessageException(params, e);
            } finally {
                txLogger.txTrace(dtxContext.getGroupId(), notifyUnitParams.getUnitId(), "notify unit over");
            }
        }
    }

}
