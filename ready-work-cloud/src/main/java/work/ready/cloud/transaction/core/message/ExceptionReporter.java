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
package work.ready.cloud.transaction.core.message;

import work.ready.cloud.transaction.logger.TxLogger;
import work.ready.cloud.cluster.common.MessageException;
import work.ready.cloud.transaction.common.message.params.TxExceptionParams;
import work.ready.core.server.Ready;

public class ExceptionReporter {

    private final ReliableMessenger reliableMessenger;

    private static final TxLogger txLogger = TxLogger.newLogger(ExceptionReporter.class);

    private static final String REPORT_ERROR_MESSAGE = "reporting transaction state encounters exception";

    public ExceptionReporter() {
        this.reliableMessenger = Ready.beanManager().get(ReliableMessenger.class);
    }

    public void reportTransactionState(String groupId, String unitId, Short registrar, int state) {
        TxExceptionParams txExceptionParams = new TxExceptionParams();
        txExceptionParams.setGroupId(groupId);
        txExceptionParams.setRegistrar(registrar);
        txExceptionParams.setTransactionState(state);
        txExceptionParams.setUnitId(unitId);
        report(txExceptionParams);
    }

    public void report(TxExceptionParams exceptionParams) {
        try {
            reliableMessenger.request(MessageCreator.writeTxException(exceptionParams));
        } catch (MessageException e) {
            txLogger.trace(exceptionParams.getGroupId(), exceptionParams.getUnitId(), "Exception Reporter", REPORT_ERROR_MESSAGE);
        }
    }
}
