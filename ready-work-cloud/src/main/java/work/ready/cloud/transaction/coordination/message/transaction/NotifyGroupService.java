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
package work.ready.cloud.transaction.coordination.message.transaction;

import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.transaction.common.exception.TransactionException;
import work.ready.cloud.transaction.common.exception.TxCoordinationException;
import work.ready.cloud.transaction.common.message.CoordinatorClient;
import work.ready.cloud.transaction.logger.TxLogger;
import work.ready.cloud.transaction.coordination.message.CoordinationCmdService;
import work.ready.cloud.transaction.coordination.message.TransactionCmd;
import work.ready.cloud.transaction.coordination.core.DtxContext;
import work.ready.cloud.transaction.coordination.core.DtxContextRegistry;
import work.ready.cloud.transaction.coordination.core.TransactionCoordinator;
import work.ready.cloud.transaction.common.message.params.NotifyGroupParams;

import java.io.Serializable;

public class NotifyGroupService implements CoordinationCmdService {

    private static final TxLogger txLogger = TxLogger.newLogger(NotifyGroupService.class);
    private final CoordinatorClient messageClient;
    private final TransactionCoordinator transactionCoordinator;

    private final DtxContextRegistry dtxContextRegistry;

    public NotifyGroupService() {
        this.transactionCoordinator = Cloud.getTransactionManager().getCoordinator().getTransactionCoordinator();
        this.dtxContextRegistry = Cloud.getTransactionManager().getCoordinator().getContextRegistry();
        this.messageClient = Cloud.getTransactionManager().getCoordinatorClient();
    }

    @Override
    public Serializable execute(TransactionCmd transactionCmd) throws TxCoordinationException {
        try {
            DtxContext dtxContext = dtxContextRegistry.get(transactionCmd.getGroupId());
            
            NotifyGroupParams notifyGroupParams = transactionCmd.getMessage().loadBean(NotifyGroupParams.class);
            int commitState = notifyGroupParams.getState();

            int transactionState = transactionCoordinator.transactionStateFromFastStorage(transactionCmd.getGroupId());
            if (transactionState == 0) {
                commitState = 0;
            }

            txLogger.txTrace(
                    transactionCmd.getGroupId(), "", "notify group state: %s", notifyGroupParams.getState());

            if (commitState == 1) {
                transactionCoordinator.commit(dtxContext);
            } else if (commitState == 0) {
                transactionCoordinator.rollback(dtxContext);
            }
            if (transactionState == 0) {
                txLogger.txTrace(transactionCmd.getGroupId(), "", "mandatory rollback for user.");
            }
            return commitState;
        } catch (TransactionException e) {
            throw new TxCoordinationException(e);
        } finally {
            transactionCoordinator.close(transactionCmd.getGroupId());
            
            txLogger.txTrace(transactionCmd.getGroupId(), "", "notify group successfully.");
        }
    }
}
