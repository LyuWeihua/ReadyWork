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
package work.ready.cloud.transaction.core.message.service;

import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.transaction.core.corelog.aspect.AspectLog;
import work.ready.cloud.transaction.core.corelog.aspect.AspectLogHelper;
import work.ready.cloud.transaction.common.exception.TxCommunicationException;
import work.ready.cloud.transaction.core.interceptor.TransactionInfo;
import work.ready.cloud.transaction.common.serializer.SerializerContext;
import work.ready.cloud.transaction.core.message.CmdExecuteService;
import work.ready.cloud.transaction.core.message.TransactionCmd;
import work.ready.cloud.transaction.common.message.params.GetAspectLogParams;

import java.io.Serializable;
import java.util.Objects;

public class GetAspectLogService implements CmdExecuteService {

    private final AspectLogHelper aspectLogHelper;

    public GetAspectLogService() {
        this.aspectLogHelper = Cloud.getTransactionManager().getAspectLogHelper();
    }

    @Override
    public Serializable execute(TransactionCmd transactionCmd) throws TxCommunicationException {
        try {
            GetAspectLogParams getAspectLogParams =transactionCmd.getMessage().loadBean(GetAspectLogParams.class);
            AspectLog txLog = aspectLogHelper.getTxLog(getAspectLogParams.getGroupId(), getAspectLogParams.getUnitId());
            if (Objects.isNull(txLog)) {
                throw new TxCommunicationException("non exists aspect log.");
            }

            TransactionInfo transactionInfo = (TransactionInfo)SerializerContext.getInstance().deserialize(txLog.getBytes());
            return transactionInfo;
        } catch (Exception e) { 
            throw new TxCommunicationException(e);
        }
    }
}
