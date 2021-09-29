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
import work.ready.cloud.transaction.core.transaction.TransactionClearanceService;
import work.ready.cloud.transaction.core.transaction.lcn.LcnTransactionType;
import work.ready.cloud.transaction.core.transaction.lcn.resource.LcnConnectionProxy;
import work.ready.cloud.transaction.core.message.ExceptionReporter;
import work.ready.cloud.transaction.common.exception.DtxNodeContextException;
import work.ready.cloud.transaction.common.exception.TransactionClearException;
import work.ready.cloud.transaction.common.message.params.TxExceptionParams;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

import java.util.Map;

public class LcnTransactionClearanceService implements TransactionClearanceService {

    private static final Log logger = LogFactory.getLog(LcnTransactionClearanceService.class);
    
    public static final short LCN_CLEAN_ERROR = 11;
    private final ExceptionReporter exceptionReporter;
    private final LcnTransactionType lcnTransaction;

    public LcnTransactionClearanceService() {
        this.exceptionReporter = Cloud.getTransactionManager().getExceptionReporter();
        this.lcnTransaction = (LcnTransactionType)Cloud.getTransactionManager().getTransactionType(LcnTransactionType.name);
    }

    @Override
    public void clear(String groupId, int state, String unitId, String unitType) throws TransactionClearException {
        try {

            Map<String, LcnConnectionProxy> connections = lcnTransaction.getLcnConnections(groupId);
            if(connections != null) {
                connections.forEach((name, conn) -> {
                    conn.notify(state);
                    logger.warn("LCN %s SUCCESS, GroupId = %s, unitId = %s, unitType = %s, dataSource = %s", state == 1 ? "COMMIT" : "ROLLBACK", groupId, unitId, unitType, name);
                });
            } else {
                logger.warn("LCN %s FAILURE, GroupId = %s, unitId = %s, unitType = %s, could not find dataSource! Please make sure you didn't do any LCN type of database operations during this transaction.", state == 1 ? "COMMIT" : "ROLLBACK", groupId, unitId, unitType);
            }

        } catch (DtxNodeContextException e) {
            logger.warn(e, "no lcn connection when clear transaction.");
        } catch (Throwable e1) {
            logger.error(e1, "LCN clearance exception.");
            reportLcnCleanException(groupId, unitId, state);
        }
    }

    private void reportLcnCleanException(String groupId, String unitId, int state) {
        TxExceptionParams txExceptionParams = new TxExceptionParams();
        txExceptionParams.setGroupId(groupId);
        txExceptionParams.setRegistrar(LCN_CLEAN_ERROR);
        txExceptionParams.setTransactionState(state);
        txExceptionParams.setUnitId(unitId);
        exceptionReporter.report(txExceptionParams);
    }
}
