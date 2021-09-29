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

import work.ready.cloud.transaction.core.interceptor.DtxTransactionInfo;
import work.ready.cloud.transaction.common.exception.TransactionException;
import work.ready.core.database.annotation.Transactional;
import work.ready.core.tools.StrUtil;

import java.lang.reflect.Method;

public class TccTransactionInfo {

    private Class<?> executeClass;

    public Class<?> getExecuteClass() {
        return executeClass;
    }

    public void setExecuteClass(Class<?> executeClass) {
        this.executeClass = executeClass;
    }

    private String cancelMethod;

    public String getCancelMethod() {
        return cancelMethod;
    }

    public void setCancelMethod(String cancelMethod) {
        this.cancelMethod = cancelMethod;
    }

    private String confirmMethod;

    public String getConfirmMethod() {
        return confirmMethod;
    }

    public void setConfirmMethod(String confirmMethod) {
        this.confirmMethod = confirmMethod;
    }

    private Object[] methodArguments;

    public Object[] getMethodArguments() {
        return methodArguments;
    }

    public void setMethodArguments(Object[] methodArguments) {
        this.methodArguments = methodArguments;
    }

    private Class[] methodParameterTypes;

    public Class[] getMethodParameterTypes() {
        return methodParameterTypes;
    }

    public void setMethodParameterTypes(Class[] methodTypeParameter) {
        this.methodParameterTypes = methodTypeParameter;
    }

    static TccTransactionInfo from(DtxTransactionInfo info) throws TransactionException {
        Method method = info.getTransactionInfo().getBusinessMethod();
        Transactional tccTransaction = info.getTransactionInfo().getTxAnnotation();
        String cancelMethod = tccTransaction.cancelMethod();
        String confirmMethod = tccTransaction.confirmMethod();
        Class<?> executeClass = tccTransaction.executeClass();
        if (StrUtil.isEmpty(tccTransaction.cancelMethod())) {
            cancelMethod = "cancel" + StrUtil.firstCharToUpperCase(method.getName());
        }
        if (StrUtil.isEmpty(tccTransaction.confirmMethod())) {
            confirmMethod = "confirm" + StrUtil.firstCharToUpperCase(method.getName());
        }
        if (Void.class.isAssignableFrom(executeClass)) {
            executeClass = info.getTransactionInfo().getTargetClass();
        }

        TccTransactionInfo tccInfo = new TccTransactionInfo();
        tccInfo.setExecuteClass(executeClass);
        tccInfo.setCancelMethod(cancelMethod);
        tccInfo.setConfirmMethod(confirmMethod);
        tccInfo.setMethodArguments(info.getArguments());
        tccInfo.setMethodParameterTypes(info.getTransactionInfo().getParameterTypes());

        return tccInfo;
    }
}
