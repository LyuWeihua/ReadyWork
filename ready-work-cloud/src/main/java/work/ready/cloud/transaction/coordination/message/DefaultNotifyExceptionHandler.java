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
package work.ready.cloud.transaction.coordination.message;

import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.transaction.common.message.params.TxExceptionParams;
import work.ready.cloud.transaction.coordination.support.dto.WriteTxExceptionDTO;
import work.ready.cloud.transaction.coordination.support.service.TxExceptionService;
import work.ready.cloud.transaction.common.message.params.NotifyUnitParams;

import java.util.List;

public class DefaultNotifyExceptionHandler implements NotifyExceptionHandler {

    private final TxExceptionService compensationService;

    public DefaultNotifyExceptionHandler() {
        this.compensationService = Cloud.getTransactionManager().getTxExceptionService();
    }

    public DefaultNotifyExceptionHandler(TxExceptionService compensationService) {
        this.compensationService = compensationService;
    }

    @Override
    public void handleNotifyUnitBusinessException(Object params, Throwable e) {
        
        handleNotifyUnitMessageException(params, e);
    }

    @Override
    public void handleNotifyUnitMessageException(Object params, Throwable e) {
        
        List paramList = ((List) params);
        String modName = (String) paramList.get(1);

        NotifyUnitParams notifyUnitParams = (NotifyUnitParams) paramList.get(0);
        WriteTxExceptionDTO writeTxExceptionReq = new WriteTxExceptionDTO(notifyUnitParams.getGroupId(),
                notifyUnitParams.getUnitId(), modName, notifyUnitParams.getState());
        writeTxExceptionReq.setRegistrar(TxExceptionParams.NOTIFY_UNIT_ERROR);
        compensationService.writeTxException(writeTxExceptionReq);
    }
}
