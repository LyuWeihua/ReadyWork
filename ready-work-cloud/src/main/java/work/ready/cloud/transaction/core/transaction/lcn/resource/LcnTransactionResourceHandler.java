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
package work.ready.cloud.transaction.core.transaction.lcn.resource;

import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.transaction.core.context.DtxThreadContext;
import work.ready.cloud.transaction.core.transaction.TransactionResourceHandler;
import work.ready.cloud.transaction.common.ConnectionCallback;
import work.ready.cloud.transaction.common.exception.DtxNodeContextException;
import work.ready.cloud.transaction.core.transaction.lcn.LcnTransactionType;

import java.sql.Connection;

public class LcnTransactionResourceHandler implements TransactionResourceHandler {

    private final LcnTransactionType lcnTransaction;

    public LcnTransactionResourceHandler() {
        this.lcnTransaction = (LcnTransactionType)Cloud.getTransactionManager().getTransactionType(LcnTransactionType.name);
    }

    @Override
    public Connection prepareConnection(String dataSource, ConnectionCallback connectionCallback) throws Throwable {
        String groupId = DtxThreadContext.current().getGroupId();
        try {
            return lcnTransaction.getLcnConnection(groupId, dataSource);
        } catch (DtxNodeContextException e) {
            LcnConnectionProxy lcnConnectionProxy = new LcnConnectionProxy(connectionCallback.call());
            lcnTransaction.setLcnConnection(groupId, dataSource, lcnConnectionProxy);
            lcnConnectionProxy.setAutoCommit(false);
            return lcnConnectionProxy;
        }
    }
}
