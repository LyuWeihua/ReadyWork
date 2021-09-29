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

import work.ready.cloud.transaction.common.exception.FastStorageException;
import work.ready.cloud.transaction.common.exception.TransactionException;
import work.ready.cloud.transaction.coordination.core.storage.FastStorage;
import work.ready.cloud.transaction.coordination.core.storage.TransactionUnit;

import java.util.List;

public class DefaultDtxContext implements DtxContext {

    private final FastStorage fastStorage;

    private final String groupId;

    DefaultDtxContext(String groupId, FastStorage fastStorage) {
        this.fastStorage = fastStorage;
        this.groupId = groupId;
    }

    @Override
    public void join(TransactionUnit transactionUnit) throws TransactionException {
        try {
            fastStorage.saveTransactionUnitToGroup(groupId, transactionUnit);
        } catch (FastStorageException e) {
            throw new TransactionException("attempts to join the non-existent transaction group. ["
                    + transactionUnit.getUnitId() + '@' + transactionUnit.getNodeName() + ']');
        }
    }

    @Override
    public void resetTransactionState(int state) throws TransactionException {
        try {
            fastStorage.saveTransactionState(groupId, state);
        } catch (FastStorageException e) {
            throw new TransactionException(e);
        }
    }

    @Override
    public List<TransactionUnit> transactionUnits() throws TransactionException {
        try {
            return fastStorage.findTransactionUnitsFromGroup(groupId);
        } catch (FastStorageException e) {
            throw new TransactionException(e);
        }
    }

    @Override
    public String getGroupId() {
        return this.groupId;
    }

    @Override
    public int transactionState() {
        try {
            return fastStorage.getTransactionState(groupId);
        } catch (FastStorageException e) {
            return -1;
        }
    }
}
