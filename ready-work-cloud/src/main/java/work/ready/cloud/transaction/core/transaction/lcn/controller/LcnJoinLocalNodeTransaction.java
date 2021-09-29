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
package work.ready.cloud.transaction.core.transaction.lcn.controller;

import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.transaction.core.context.DtxNodeContext;
import work.ready.cloud.transaction.core.context.DtxThreadContext;
import work.ready.cloud.transaction.core.interceptor.DtxTransactionInfo;
import work.ready.cloud.transaction.common.Transaction;
import work.ready.cloud.transaction.common.exception.TransactionException;

public class LcnJoinLocalNodeTransaction extends LcnJoinOtherNodeTransaction {
    private final DtxNodeContext nodeContext;

    public LcnJoinLocalNodeTransaction() {
        super();
        this.nodeContext = Cloud.getTransactionManager().getNodeContext();
    }

    @Override
    public void preBusinessCode(DtxTransactionInfo info) {
        
        DtxThreadContext.activate();
    }

    @Override
    public void onBusinessCodeError(DtxTransactionInfo info, Throwable throwable) {
        if(info.getTransactionInfo().getUnitId().equals(nodeContext.txContext(info.getGroupId()).getUnitIdByType(Transaction.LCN).get(0))) {
            super.onBusinessCodeError(info, throwable);
        }
    }

    @Override
    public void onBusinessCodeSuccess(DtxTransactionInfo info, Object result) throws TransactionException {
        if(info.getTransactionInfo().getUnitId().equals(nodeContext.txContext(info.getGroupId()).getUnitIdByType(Transaction.LCN).get(0))) {
            super.onBusinessCodeSuccess(info, result);
        }
    }

}
