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
import work.ready.cloud.transaction.common.exception.FastStorageException;
import work.ready.cloud.transaction.common.exception.TransactionException;
import work.ready.cloud.transaction.coordination.core.storage.FastStorage;

public class DefaultDtxContextRegistry implements DtxContextRegistry {

    private final FastStorage fastStorage;

    public DefaultDtxContextRegistry() {
        this.fastStorage = Cloud.getTransactionManager().getCoordinator().getFastStorage();
    }

    @Override
    public DtxContext create(String groupId) throws TransactionException {
        try {
            fastStorage.initGroup(groupId);
        } catch (FastStorageException e) {
            
            if (e.getCode() != FastStorageException.EX_CODE_REPEAT_GROUP) {
                throw new TransactionException(e);
            }
        }
        return get(groupId);
    }

    @Override
    public DtxContext get(String groupId) throws TransactionException {

        return new DefaultDtxContext(groupId, fastStorage);
    }

    @Override
    public void destroyContext(String groupId) {
        try {
            fastStorage.clearGroup(groupId);
        } catch (FastStorageException e) {
            
            if (e.getCode() == FastStorageException.EX_CODE_NO_GROUP) {
                return;
            }
            try {
                fastStorage.clearGroup(groupId);
            } catch (FastStorageException e1) {
                throw new IllegalStateException(e1);
            }
        }
    }

    @Override
    public int transactionState(String groupId) {
        try {
            return fastStorage.getTransactionState(groupId);
        } catch (FastStorageException e) {
            return -1;
        }
    }
}
