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
package work.ready.cloud.transaction.core.transaction.txc.controller;

import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.transaction.core.context.DtxThreadContext;
import work.ready.cloud.transaction.core.controller.DtxLocalController;
import work.ready.cloud.transaction.core.interceptor.DtxTransactionInfo;
import work.ready.cloud.transaction.core.controller.TransactionClearancer;
import work.ready.cloud.transaction.core.controller.TransactionController;
import work.ready.cloud.transaction.common.exception.TransactionClearException;
import work.ready.cloud.transaction.common.exception.TransactionException;
import work.ready.cloud.transaction.logger.TxLogger;
import work.ready.cloud.transaction.common.Transaction;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

public class TxcJoinOtherNodeTransaction implements DtxLocalController {
    private static final Log logger = LogFactory.getLog(TxcJoinOtherNodeTransaction.class);
    private final TransactionClearancer transactionClearancer;
    private final TransactionController transactionController;

    private static final TxLogger txLogger = TxLogger.newLogger(TxcJoinOtherNodeTransaction.class);

    public TxcJoinOtherNodeTransaction() {
        this.transactionClearancer = Cloud.getTransactionManager().getClearancer();
        this.transactionController = Cloud.getTransactionManager().getController();
    }

    @Override
    public void preBusinessCode(DtxTransactionInfo info) {
        DtxThreadContext.activate();
    }

    @Override
    public void onBusinessCodeError(DtxTransactionInfo info, Throwable throwable) {
        try {
            logger.debug("txc > running > clean transaction.");
            transactionClearancer.clean(info.getGroupId(), info.getTransactionInfo().getUnitId(), info.getTransactionInfo().getTransactionType(), 0);
        } catch (TransactionClearException e) {
            logger.error(e, "txc > Clean Transaction Error");
            txLogger.trace(info.getGroupId(), info.getTransactionInfo().getUnitId(), Transaction.TX_ERROR, "clean transaction error");
        }
    }

    @Override
    public void onBusinessCodeSuccess(DtxTransactionInfo info, Object result) throws TransactionException {
        logger.debug("join group: [GroupId: %s, Method: %s]" , info.getGroupId(),
                info.getTransactionInfo().getMethodStr());
        transactionController.joinGroup(info.getGroupId(), info.getTransactionInfo().getUnitId(), info.getTransactionInfo().getTransactionType(),
                info.getTransactionInfo());
    }
}
