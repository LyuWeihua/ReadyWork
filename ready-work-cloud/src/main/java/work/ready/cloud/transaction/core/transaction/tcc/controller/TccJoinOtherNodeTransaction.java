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
package work.ready.cloud.transaction.core.transaction.tcc.controller;

import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.transaction.core.context.DtxThreadContext;
import work.ready.cloud.transaction.core.controller.DtxLocalController;
import work.ready.cloud.transaction.core.interceptor.DtxTransactionInfo;
import work.ready.cloud.transaction.core.controller.TransactionClearancer;
import work.ready.cloud.transaction.core.controller.TransactionController;
import work.ready.cloud.transaction.common.exception.TransactionClearException;
import work.ready.cloud.transaction.common.exception.TransactionException;
import work.ready.cloud.transaction.core.transaction.tcc.TccTransactionType;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

public class TccJoinOtherNodeTransaction implements DtxLocalController {
    private final Log logger = LogFactory.getLog(getClass());

    private final TccTransactionType tccTransaction;

    private final TransactionClearancer transactionClearancer;

    private final TransactionController transactionController;

    public TccJoinOtherNodeTransaction() {
        this.transactionClearancer = Cloud.getTransactionManager().getClearancer();
        this.transactionController = Cloud.getTransactionManager().getController();
        this.tccTransaction = (TccTransactionType)Cloud.getTransactionManager().getTransactionType(TccTransactionType.name);
    }

    @Override
    public void preBusinessCode(DtxTransactionInfo info) throws TransactionException {

        try {
            tccTransaction.tccTransactionInfo(info.getTransactionInfo().getUnitId(), () -> TccTransactionInfo.from(info))
                    .setMethodArguments(info.getArguments());
        } catch (Throwable throwable) {
            throw new TransactionException(throwable);
        }
    }

    @Override
    public void onBusinessCodeError(DtxTransactionInfo info, Throwable throwable) {
        try {
            transactionClearancer.clean(
                    DtxThreadContext.current().getGroupId(),
                    info.getTransactionInfo().getUnitId(),
                    info.getTransactionInfo().getTransactionType(),
                    0);
        } catch (TransactionClearException e) {
            logger.error(e, "tcc > clean transaction error.");
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
