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
import work.ready.cloud.transaction.core.transaction.TransactionClearanceService;
import work.ready.cloud.transaction.common.exception.TransactionClearException;
import work.ready.cloud.transaction.core.message.ExceptionReporter;
import work.ready.cloud.transaction.common.message.params.TxExceptionParams;
import work.ready.cloud.transaction.core.transaction.tcc.TccTransactionType;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;
import work.ready.core.tools.ClassUtil;
import work.ready.core.tools.ReadyThreadFactory;
import work.ready.core.tools.define.LambdaFinal;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TccTransactionClearanceService implements TransactionClearanceService {

    private static final Log logger = LogFactory.getLog(TccTransactionClearanceService.class);
    
    public static final short TCC_CLEAN_ERROR = 12;
    private static final ExecutorService executorService = Executors.newCachedThreadPool(new ReadyThreadFactory("TccTxClearance"));
    private final ExceptionReporter exceptionReporter;
    private final TccTransactionType tccTransaction;

    public TccTransactionClearanceService() {
        this.exceptionReporter = Cloud.getTransactionManager().getExceptionReporter();
        this.tccTransaction = (TccTransactionType)Cloud.getTransactionManager().getTransactionType(TccTransactionType.name);
    }

    @Override
    public void clear(String groupId, int state, String unitId, String unitType) throws TransactionClearException {
        LambdaFinal<Method> exeMethod = new LambdaFinal<>();
        LambdaFinal<Boolean> withParameter = new LambdaFinal<>();
        try {
            TccTransactionInfo tccInfo = tccTransaction.tccTransactionInfo(unitId, null);
            Object object = Ready.beanManager().get(ClassUtil.getUserClass(tccInfo.getExecuteClass()));
            withParameter.set(false);
            try {
                exeMethod.set(ClassUtil.getUserClass(tccInfo.getExecuteClass()).getMethod(
                        state == 1 ? tccInfo.getConfirmMethod() : tccInfo.getCancelMethod(),
                        tccInfo.getMethodParameterTypes()));
                withParameter.set(true);
            } catch (NoSuchMethodException e) {
                exeMethod.set(ClassUtil.getUserClass(tccInfo.getExecuteClass()).getMethod(
                        state == 1 ? tccInfo.getConfirmMethod() : tccInfo.getCancelMethod()));
            }
            try {
                Future<Boolean> futureResult = executorService.submit(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        
                        DtxThreadContext.close();
                        
                        if(withParameter.get()) {
                            exeMethod.get().invoke(object, tccInfo.getMethodArguments());
                        } else {
                            exeMethod.get().invoke(object);
                        }
                        return true;
                    }
                });
                futureResult.get();
                logger.warn("User's TCC transaction %s logic is finished.", state == 1 ? "confirm" : "cancel");
            } catch (Throwable e) {
                logger.error(e, "Tcc clearance exception.", e);
                reportTccCleanException(groupId, unitId, state);
            }
        } catch (Throwable e) {
            throw new TransactionClearException(e.getMessage());
        }
    }

    private void reportTccCleanException(String groupId, String unitId, int state) {
        TxExceptionParams txExceptionParams = new TxExceptionParams();
        txExceptionParams.setGroupId(groupId);
        txExceptionParams.setRegistrar(TCC_CLEAN_ERROR);
        txExceptionParams.setTransactionState(state);
        txExceptionParams.setUnitId(unitId);
        exceptionReporter.report(txExceptionParams);
    }
}
