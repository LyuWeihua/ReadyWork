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
package work.ready.cloud.transaction.core.message.service;

import work.ready.cloud.transaction.core.context.DtxNodeContext;
import work.ready.cloud.transaction.core.context.TxContext;
import work.ready.cloud.transaction.core.controller.TransactionClearancer;
import work.ready.cloud.transaction.common.exception.TransactionClearException;
import work.ready.cloud.transaction.common.exception.TxCommunicationException;
import work.ready.cloud.transaction.logger.TxLogger;
import work.ready.cloud.transaction.core.message.CmdExecuteService;
import work.ready.cloud.transaction.core.message.TransactionCmd;
import work.ready.cloud.transaction.common.message.params.NotifyUnitParams;

import java.io.Serializable;

public abstract class DefaultNotifiedUnitService implements CmdExecuteService {

    private static final TxLogger txLogger = TxLogger.newLogger(DefaultNotifiedUnitService.class);

    private final TransactionClearancer transactionClearancer;

    private final DtxNodeContext nodeContext;

    protected DefaultNotifiedUnitService(TransactionClearancer transactionClearancer, DtxNodeContext nodeContext) {
        this.transactionClearancer = transactionClearancer;
        this.nodeContext = nodeContext;
    }

    @Override
    public Serializable execute(TransactionCmd transactionCmd) throws TxCommunicationException {
        try {
            NotifyUnitParams notifyUnitParams = transactionCmd.getMessage().loadBean(NotifyUnitParams.class);
            
            TxContext txContext = nodeContext.txContext(transactionCmd.getGroupId());

            if (txContext != null && !txContext.isStarter()) {
                synchronized (txContext.getLock()) {
                    txLogger.txTrace(transactionCmd.getGroupId(), notifyUnitParams.getUnitId(),
                            "clean transaction cmd waiting for business code finish.");
                    txContext.getLock().wait();
                }
            }
            
            transactionClearancer.clean(
                    notifyUnitParams.getGroupId(),
                    notifyUnitParams.getUnitId(),
                    notifyUnitParams.getType(),
                    notifyUnitParams.getState());
            return true;
        } catch (TransactionClearException | InterruptedException e) {
            throw new TxCommunicationException(e);
        }
    }
}
