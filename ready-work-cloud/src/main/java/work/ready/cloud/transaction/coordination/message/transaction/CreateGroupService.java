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
import work.ready.cloud.transaction.coordination.core.TransactionCoordinator;

import java.io.Serializable;

public class CreateGroupService implements CoordinationCmdService {

    private static final TxLogger txLogger = TxLogger.newLogger(CreateGroupService.class);

    private final TransactionCoordinator transactionCoordinator;

    public CreateGroupService() {
        transactionCoordinator = Cloud.getTransactionManager().getCoordinator().getTransactionCoordinator();
    }

    @Override
    public Serializable execute(TransactionCmd transactionCmd) throws TxCoordinationException {
        try {
            transactionCoordinator.begin(transactionCmd.getGroupId());
        } catch (TransactionException e) {
            throw new TxCoordinationException(e);
        }
        txLogger.txTrace(transactionCmd.getGroupId(), null, "created group");
        return null;
    }
}
