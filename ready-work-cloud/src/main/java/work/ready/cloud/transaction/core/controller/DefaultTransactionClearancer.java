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

import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.transaction.core.check.TransactionChecker;
import work.ready.cloud.transaction.core.context.DtxNodeContext;
import work.ready.cloud.transaction.core.corelog.aspect.CoreLogger;
import work.ready.cloud.transaction.common.exception.TransactionClearException;
import work.ready.cloud.transaction.logger.TxLogger;

public class DefaultTransactionClearancer implements TransactionClearancer {

    private static final TxLogger txLogger = TxLogger.newLogger(TransactionClearancer.class);

    private final TransactionChecker transactionChecker;

    private final CoreLogger coreLogger;

    private final DtxNodeContext nodeContext;

    public DefaultTransactionClearancer() {
        this.transactionChecker = Cloud.getTransactionManager().getChecker();
        this.coreLogger = Cloud.getTransactionManager().getCoreLogger();
        this.nodeContext = Cloud.getTransactionManager().getNodeContext();
    }

    @Override
    public void clean(String groupId, String unitId, String unitType, int state) throws TransactionClearException {
        txLogger.txTrace(groupId, unitId, "clean transaction");
        try {
            cleanWithoutAspectLog(groupId, unitId, unitType, state);
            coreLogger.clearLog(groupId, unitId);
        } catch (TransactionClearException e) {
            if (!e.isNeedCompensation()) {
                coreLogger.clearLog(groupId, unitId);
            }
        } catch (Throwable throwable) {
            coreLogger.clearLog(groupId, unitId);
        }
        txLogger.txTrace(groupId, unitId, "clean transaction over");
    }

    @Override
    public void cleanWithoutAspectLog(String groupId, String unitId, String unitType, int state) throws TransactionClearException {
        try {
            Cloud.getTransactionManager().getTransactionClearanceService(unitType).clear(
                    groupId, state, unitId, unitType
            );
        } finally {
            nodeContext.clearGroup(groupId);

            transactionChecker.stopDelayChecking(groupId, unitId);
        }
    }
}
