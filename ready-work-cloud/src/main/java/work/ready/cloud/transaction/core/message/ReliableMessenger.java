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
package work.ready.cloud.transaction.core.message;

import work.ready.cloud.transaction.common.exception.TxBusinessException;
import work.ready.cloud.cluster.common.MessageBody;
import work.ready.cloud.cluster.common.MessageException;

import java.util.Map;
import java.util.Set;

public interface ReliableMessenger {
    
    boolean acquireLocks(String groupId, Map<String, Set<String>> lockMap, int type) throws MessageException;

    void releaseLocks(Map<String, Set<String>> lockMap) throws MessageException;

    int notifyGroup(String groupId, int transactionState) throws MessageException, TxBusinessException;

    void joinGroup(String groupId, String unitId, String type, int transactionState) throws MessageException, TxBusinessException;

    void createGroup(String groupId) throws MessageException, TxBusinessException;

    int askTransactionState(String groupId, String unitId) throws MessageException;

    MessageBody request(MessageBody messageBody) throws MessageException;

}
