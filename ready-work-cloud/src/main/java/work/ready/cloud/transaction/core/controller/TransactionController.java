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
package work.ready.cloud.transaction.core.controller;

import work.ready.cloud.transaction.common.exception.TransactionException;
import work.ready.cloud.transaction.core.interceptor.TransactionInfo;

public interface TransactionController {

    void createGroup(String groupId, String unitId, TransactionInfo transactionInfo, String transactionType) throws TransactionException;

    void joinGroup(String groupId, String unitId, String transactionType, TransactionInfo transactionInfo) throws TransactionException;

    void notifyGroup(String groupId, String unitId, String transactionType, int state);

}
