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
import work.ready.cloud.transaction.common.exception.TransactionClearException;
import work.ready.cloud.transaction.common.exception.TransactionException;
import work.ready.cloud.transaction.common.exception.UserRollbackException;
import work.ready.cloud.transaction.logger.TxLogger;
import work.ready.cloud.transaction.core.message.ExceptionReporter;
import work.ready.cloud.transaction.common.message.params.TxExceptionParams;

import java.util.List;

public class DefaultTransactionExceptionHandler implements TransactionExceptionHandler {

    private static final TxLogger txLogger = TxLogger.newLogger(DefaultTransactionExceptionHandler.class);

    private final TransactionClearancer transactionClearancer;

    private final ExceptionReporter exceptionReporter;

    public DefaultTransactionExceptionHandler() {
        this.transactionClearancer = Cloud.getTransactionManager().getClearancer();
        this.exceptionReporter = Cloud.getTransactionManager().getExceptionReporter();
    }

    @Override
    public void handleCreateGroupBusinessException(Object params, Throwable ex) throws TransactionException {
        throw new TransactionException(ex);
    }

    @Override
    public void handleCreateGroupMessageException(Object params, Throwable ex) throws TransactionException {
        throw new TransactionException(ex);
    }

    @Override
    public void handleJoinGroupBusinessException(Object params, Throwable ex) throws TransactionException {
        List paramList = (List) params;
        String groupId = (String) paramList.get(0);
        String unitId = (String) paramList.get(1);
        String unitType = (String) paramList.get(2);
        try {
            transactionClearancer.clean(groupId, unitId, unitType, 0);
        } catch (TransactionClearException e) {
            txLogger.error(groupId, unitId, "join group", "clean [%s]transaction fail.", unitType);
        }
        throw new TransactionException(ex);
    }

    @Override
    public void handleJoinGroupMessageException(Object params, Throwable ex) throws TransactionException {
        throw new TransactionException(ex);
    }

    @Override
    public void handleNotifyGroupBusinessException(Object params, Throwable ex) {
        List paramList = (List) params;
        String groupId = (String) paramList.get(0);
        int state = (int) paramList.get(1);
        String unitId = (String) paramList.get(2);
        String transactionType = (String) paramList.get(3);

        if (ex instanceof UserRollbackException) {
            state = 0;
        }
        if ((ex.getCause() != null && ex.getCause() instanceof UserRollbackException)) {
            state = 0;
        }

        try {
            transactionClearancer.clean(groupId, unitId, transactionType, state);
        } catch (TransactionClearException e) {
            txLogger.error(groupId, unitId, "notify group", "%s > clean transaction error.", transactionType);
        }
    }

    @Override
    public void handleNotifyGroupMessageException(Object params, Throwable ex) {
        
        List paramList = (List) params;
        String groupId = (String) paramList.get(0);
        int state = (int) paramList.get(1);
        if (state == 0) {
            handleNotifyGroupBusinessException(params, ex);
            return;
        }

        String unitId = (String) paramList.get(2);
        String transactionType = (String) paramList.get(3);
        try {
            transactionClearancer.cleanWithoutAspectLog(groupId, unitId, transactionType, state);
        } catch (TransactionClearException e) {
            txLogger.error(groupId, unitId, "notify group", "%s > cleanWithoutAspectLog transaction error.", transactionType);
        }

        exceptionReporter.reportTransactionState(groupId, null, TxExceptionParams.NOTIFY_GROUP_ERROR, state);
    }
}
