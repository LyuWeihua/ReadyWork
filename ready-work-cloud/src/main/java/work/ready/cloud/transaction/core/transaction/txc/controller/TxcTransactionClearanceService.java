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
import work.ready.cloud.transaction.core.transaction.TransactionClearanceService;
import work.ready.cloud.transaction.core.transaction.txc.analyse.DefaultTxcService;
import work.ready.cloud.transaction.core.transaction.txc.analyse.TxcService;
import work.ready.cloud.transaction.core.transaction.txc.analyse.bean.StatementInfo;
import work.ready.cloud.transaction.core.transaction.txc.exception.TxcLogicException;
import work.ready.cloud.transaction.common.exception.TransactionClearException;
import work.ready.cloud.transaction.core.message.ExceptionReporter;
import work.ready.cloud.transaction.common.message.params.TxExceptionParams;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;

import java.util.List;
import java.util.Map;

public class TxcTransactionClearanceService implements TransactionClearanceService {
    private static final Log logger = LogFactory.getLog(TxcTransactionClearanceService.class);
    
    public static final short TXC_UNDO_ERROR = 13;

    private final TxcService txcService;

    private final ExceptionReporter exceptionReporter;

    public TxcTransactionClearanceService() {
        this.txcService = Ready.beanManager().get(TxcService.class, DefaultTxcService.class);
        this.exceptionReporter = Cloud.getTransactionManager().getExceptionReporter();
    }

    @Override
    public void clear(String groupId, int state, String unitId, String unitType) throws TransactionClearException {
        boolean rethrowTxcException = false;
        try {
            if (state == 0) {
                txcService.undo(groupId, unitId);
            }
        } catch (TxcLogicException e) {
            @SuppressWarnings("unchecked")
            Map<String, List<StatementInfo>> statementInfo = (Map<String, List<StatementInfo>>) e.getAttachment();
            reportTxcUndoException(groupId, unitId, statementInfo);
            rethrowTxcException = true;
            logger.warn("txc undo logic encounters exception, need compensation !");
        }

        try {
            
            txcService.cleanTxc(groupId, unitId);
        } catch (TxcLogicException e) {
            throw new TransactionClearException(e);
        }

        if (rethrowTxcException) {
            throw TransactionClearException.needCompensation();
        }
    }

    private void reportTxcUndoException(String groupId, String unitId, Map<String, List<StatementInfo>> statementInfo) {
        TxExceptionParams exceptionParams = new TxExceptionParams();
        exceptionParams.setGroupId(groupId);
        exceptionParams.setUnitId(unitId);
        exceptionParams.setRegistrar(TXC_UNDO_ERROR);
        exceptionParams.setTransactionState(0);
        exceptionParams.setRemark(statementInfo.toString());
        exceptionReporter.report(exceptionParams);
    }
}
