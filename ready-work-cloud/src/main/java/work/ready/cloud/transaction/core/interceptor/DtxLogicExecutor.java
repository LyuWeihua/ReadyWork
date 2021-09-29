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
package work.ready.cloud.transaction.core.interceptor;

import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.transaction.core.context.DtxNodeContext;
import work.ready.cloud.transaction.core.controller.DtxLocalController;
import work.ready.cloud.transaction.core.propagation.DefaultPropagationResolver;
import work.ready.cloud.transaction.core.propagation.PropagationResolver;
import work.ready.cloud.transaction.core.propagation.PropagationState;
import work.ready.cloud.transaction.common.exception.TransactionException;
import work.ready.cloud.transaction.logger.TxLogger;
import work.ready.cloud.transaction.common.Transaction;
import work.ready.core.server.Ready;

public class DtxLogicExecutor {

    private static final TxLogger txLogger = TxLogger.newLogger(DtxLogicExecutor.class);

    private final DtxNodeContext nodeContext;

    private final PropagationResolver propagationResolver;

    public DtxLogicExecutor() {
        this.nodeContext = Cloud.getTransactionManager().getNodeContext();
        this.propagationResolver = Ready.beanManager().get(PropagationResolver.class, DefaultPropagationResolver.class);
    }

    public Object run(DtxTransactionInfo info) throws Throwable {

        String transactionType = info.getTransactionInfo().getTransactionType();

        PropagationState propagationState = propagationResolver.resolvePropagationState(info);

        if (propagationState.isIgnored()) {
            return info.getBusinessCallback().call();
        }

        DtxLocalController dtxLocalController = Cloud.getTransactionManager().getBusinessController(transactionType, propagationState);

        try {
            
            nodeContext.txContext(info.getGroupId()).addTransactionTypes(transactionType, info.getTransactionInfo().getUnitId());

            dtxLocalController.preBusinessCode(info);

            txLogger.txTrace(
                    info.getGroupId(), info.getTransactionInfo().getUnitId(), "before business logic, unit type: %s", transactionType);

            Object result = dtxLocalController.doBusinessCode(info);

            txLogger.txTrace(info.getGroupId(), info.getTransactionInfo().getUnitId(), "business logic is successful");
            dtxLocalController.onBusinessCodeSuccess(info, result);
            return result;
        } catch (TransactionException e) {
            txLogger.error(info.getGroupId(), info.getTransactionInfo().getUnitId(), "exception happens before business logic");
            throw e;
        } catch (Throwable e) {
            
            txLogger.error(info.getGroupId(), info.getTransactionInfo().getUnitId(), Transaction.TAG_TRANSACTION,
                    "business logic exception");
            dtxLocalController.onBusinessCodeError(info, e);
            throw e;
        } finally {
            
            dtxLocalController.postBusinessCode(info);
        }
    }

}
