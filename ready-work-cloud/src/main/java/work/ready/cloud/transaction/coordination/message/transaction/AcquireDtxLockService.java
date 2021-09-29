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
import work.ready.cloud.transaction.common.exception.FastStorageException;
import work.ready.cloud.transaction.common.exception.TxCoordinationException;
import work.ready.cloud.transaction.coordination.message.CoordinationCmdService;
import work.ready.cloud.transaction.coordination.message.TransactionCmd;
import work.ready.cloud.transaction.coordination.core.storage.FastStorage;
import work.ready.cloud.transaction.coordination.core.storage.LockValue;
import work.ready.cloud.transaction.common.message.params.DtxLockParams;
import java.io.Serializable;

public class AcquireDtxLockService implements CoordinationCmdService {

    private final FastStorage fastStorage;

    public AcquireDtxLockService() {
        fastStorage = Cloud.getTransactionManager().getCoordinator().getFastStorage();
    }

    @Override
    public Serializable execute(TransactionCmd transactionCmd) throws TxCoordinationException {
        DtxLockParams dtxLockParams = transactionCmd.getMessage().loadBean(DtxLockParams.class);
        try {
            LockValue lockValue = new LockValue();
            lockValue.setGroupId(dtxLockParams.getGroupId());
            lockValue.setLockType(dtxLockParams.getLockType());
            fastStorage.acquireLocks(dtxLockParams.getContextId(), dtxLockParams.getLockMap(), lockValue);
            return true;
        } catch (FastStorageException e) {
            throw new TxCoordinationException(e);
        }
    }
}
