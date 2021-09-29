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
package work.ready.cloud.transaction.coordination.core.storage;

import work.ready.cloud.transaction.common.exception.FastStorageException;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface FastStorage {

    void initGroup(String groupId) throws FastStorageException;

    boolean containsGroup(String groupId);

    List<TransactionUnit> findTransactionUnitsFromGroup(String groupId) throws FastStorageException;

    void saveTransactionUnitToGroup(String groupId, TransactionUnit transactionUnit) throws FastStorageException;

    void clearGroup(String groupId) throws FastStorageException;

    void saveTransactionState(String groupId, int state) throws FastStorageException;

    int getTransactionState(String groupId) throws FastStorageException;

    void acquireLocks(String contextId, Map<String, Set<String>> lockMap, LockValue lockValue) throws FastStorageException;

    void releaseLocks(String contextId, Map<String, Set<String>> lockMap) throws FastStorageException;

}
