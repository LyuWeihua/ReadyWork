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
package work.ready.cloud.transaction.core.interceptor;

import work.ready.core.database.annotation.Transactional;
import work.ready.core.database.transaction.Propagation;

import java.io.Serializable;
import java.lang.reflect.Method;

public class TransactionInfo implements Serializable {

    private Class<?> targetClass;
    
    private String methodName;

    private Class<?>[] parameterTypes;

    private String methodStr;

    private String transactionType;

    private Propagation transactionPropagation;

    private Method businessMethod;

    private Transactional txAnnotation;

    private String unitId;

    public TransactionInfo() {
    }

    public String getMethodStr() {
        return methodStr;
    }

    void setMethodStr(String methodStr) {
        this.methodStr = methodStr;
    }

    public Class<?> getTargetClass() {
        return targetClass;
    }

    void setTargetClass(Class<?> targetClass) {
        this.targetClass = targetClass;
    }

    public String getMethodName() {
        return methodName;
    }

    void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public String getTransactionType() {
        return transactionType;
    }

    void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public Propagation getTransactionPropagation() {
        return transactionPropagation;
    }

    void setTransactionPropagation(Propagation transactionPropagation) {
        this.transactionPropagation = transactionPropagation;
    }

    public Method getBusinessMethod() {
        return businessMethod;
    }

    void setBusinessMethod(Method businessMethod) {
        this.businessMethod = businessMethod;
    }

    public Transactional getTxAnnotation() {
        return txAnnotation;
    }

    public void setTxAnnotation(Transactional txAnnotation) {
        this.txAnnotation = txAnnotation;
    }

    public String getUnitId() {
        return unitId;
    }

    void setUnitId(String unitId) {
        this.unitId = unitId;
    }
}
