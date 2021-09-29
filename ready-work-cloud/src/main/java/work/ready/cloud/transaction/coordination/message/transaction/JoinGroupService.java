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
import work.ready.cloud.transaction.logger.TxLogger;
import work.ready.cloud.transaction.coordination.message.CoordinationCmdService;
import work.ready.cloud.transaction.coordination.message.TransactionCmd;
import work.ready.cloud.transaction.coordination.core.DtxContext;
import work.ready.cloud.transaction.coordination.core.DtxContextRegistry;
import work.ready.cloud.transaction.coordination.core.TransactionCoordinator;
import work.ready.cloud.transaction.common.message.CoordinatorClient;
import work.ready.cloud.transaction.common.message.params.JoinGroupParams;

import java.io.Serializable;

public class JoinGroupService implements CoordinationCmdService {

    private static final TxLogger txLogger = TxLogger.newLogger(JoinGroupService.class);

    private final TransactionCoordinator transactionCoordinator;

    private final DtxContextRegistry dtxContextRegistry;

    private final CoordinatorClient messageClient;

    public JoinGroupService() {
        this.transactionCoordinator = Cloud.getTransactionManager().getCoordinator().getTransactionCoordinator();
        this.dtxContextRegistry = Cloud.getTransactionManager().getCoordinator().getContextRegistry();
        this.messageClient = Cloud.getTransactionManager().getCoordinatorClient();
    }

    @Override
    public Serializable execute(TransactionCmd transactionCmd) throws TxCoordinationException {
        try {
            DtxContext dtxContext = dtxContextRegistry.get(transactionCmd.getGroupId());
            JoinGroupParams joinGroupParams = transactionCmd.getMessage().loadBean(JoinGroupParams.class);
            txLogger.txTrace(transactionCmd.getGroupId(), joinGroupParams.getUnitId(), "unit:%s try join group:%s",
                    joinGroupParams.getUnitId(), transactionCmd.getGroupId());
            transactionCoordinator.join(dtxContext, joinGroupParams.getUnitId(), joinGroupParams.getType(),
                    messageClient.getNodeName(transactionCmd.getNodeId()), joinGroupParams.getTransactionState());
            txLogger.txTrace(transactionCmd.getGroupId(), joinGroupParams.getUnitId(), "unit:%s joined.",
                    joinGroupParams.getUnitId());
        } catch (TransactionException e) {
            txLogger.error(this.getClass().getSimpleName(), e.getMessage());
            throw new TxCoordinationException(e.getLocalizedMessage());
        }
        
        return null;
    }
}
