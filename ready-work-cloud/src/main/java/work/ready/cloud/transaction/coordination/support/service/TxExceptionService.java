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
package work.ready.cloud.transaction.coordination.support.service;

import work.ready.cloud.transaction.common.TxInitializer;
import work.ready.cloud.transaction.common.exception.TransactionStateException;
import work.ready.cloud.transaction.common.exception.TxCoordinationException;
import work.ready.cloud.transaction.coordination.support.dto.WriteTxExceptionDTO;
import work.ready.cloud.transaction.coordination.support.dto.ExceptionList;
import work.ready.cloud.cluster.common.MessageBody;

import java.util.List;

public interface TxExceptionService extends TxInitializer {

    void writeTxException(WriteTxExceptionDTO writeTxExceptionReq);

    int transactionState(String groupId);

    ExceptionList exceptionList(Integer page, Integer limit, Integer exState, String keyword, Integer registrar);

    MessageBody getTransactionInfo(String groupId, String unitId) throws TxCoordinationException, TransactionStateException;

    void deleteExceptions(List<Long> ids) throws TxCoordinationException;

    void deleteTransactionInfo(String groupId, String unitId, String nodeName) throws TxCoordinationException;
}
