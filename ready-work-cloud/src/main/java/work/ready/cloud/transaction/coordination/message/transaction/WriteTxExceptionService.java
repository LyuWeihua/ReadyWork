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
import work.ready.cloud.transaction.common.exception.TxCoordinationException;
import work.ready.cloud.transaction.coordination.message.CoordinationCmdService;
import work.ready.cloud.transaction.coordination.message.TransactionCmd;
import work.ready.cloud.transaction.coordination.core.TransactionCoordinator;
import work.ready.cloud.transaction.coordination.support.dto.WriteTxExceptionDTO;
import work.ready.cloud.transaction.coordination.support.service.TxExceptionService;
import work.ready.cloud.transaction.common.message.CoordinatorClient;
import work.ready.cloud.transaction.common.message.params.TxExceptionParams;

import java.io.Serializable;
import java.util.Objects;

public class WriteTxExceptionService implements CoordinationCmdService {

    private final TxExceptionService compensationService;

    private final CoordinatorClient messageClient;

    private final TransactionCoordinator transactionCoordinator;

    public WriteTxExceptionService() {
        this.compensationService = Cloud.getTransactionManager().getTxExceptionService();
        this.messageClient = Cloud.getTransactionManager().getCoordinatorClient();
        this.transactionCoordinator = Cloud.getTransactionManager().getCoordinator().getTransactionCoordinator();
    }

    @Override
    public Serializable execute(TransactionCmd transactionCmd) throws TxCoordinationException {
        try {
            TxExceptionParams txExceptionParams = transactionCmd.getMessage().loadBean(TxExceptionParams.class);
            WriteTxExceptionDTO writeTxExceptionReq = new WriteTxExceptionDTO();
            writeTxExceptionReq.setNodeName(messageClient.getNodeName(transactionCmd.getNodeId()));

            int transactionState = transactionCoordinator.transactionStateFromFastStorage(transactionCmd.getGroupId());

            writeTxExceptionReq.setTransactionState(transactionState == -1 ? txExceptionParams.getTransactionState() : transactionState);
            writeTxExceptionReq.setGroupId(txExceptionParams.getGroupId());
            writeTxExceptionReq.setUnitId(txExceptionParams.getUnitId());
            writeTxExceptionReq.setRegistrar(Objects.isNull(txExceptionParams.getRegistrar()) ? -1 : txExceptionParams.getRegistrar());
            writeTxExceptionReq.setRemark(txExceptionParams.getRemark());
            compensationService.writeTxException(writeTxExceptionReq);
        } catch (Exception e) {
            throw new TxCoordinationException(e);
        }
        return null;
    }
}
