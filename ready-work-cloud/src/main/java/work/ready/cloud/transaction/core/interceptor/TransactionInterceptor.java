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

import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.transaction.DistributedTransactionManager;
import work.ready.cloud.transaction.common.Transaction;
import work.ready.cloud.transaction.common.exception.TransactionTypeException;
import work.ready.core.aop.Interceptor;
import work.ready.core.aop.Invocation;
import work.ready.core.database.annotation.Transactional;
import work.ready.core.server.Ready;
import work.ready.core.tools.HashUtil;
import work.ready.core.tools.StrUtil;
import work.ready.core.tools.define.ConcurrentReferenceHashMap;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

public class TransactionInterceptor implements Interceptor {

    private static final Map<String, TransactionInfo> TransactionInfoCache = new ConcurrentReferenceHashMap<>();
    private final TransactionHandler transactionHandler;

    public TransactionInterceptor() {
        var transactionManager = Ready.dbManager().getTransactionManager();
        if(!(transactionManager instanceof DistributedTransactionManager)) {
            throw new RuntimeException("It is not supported by " + transactionManager.getClass().getSimpleName() + ", DistributedTransactionManager is required.");
        }
        transactionHandler = Ready.beanManager().get(TransactionHandler.class);
    }

    @Override
    public void intercept(Invocation invocation) throws Throwable {
        TransactionInfo transaction = getFromCache(invocation);
        if(invocation.getArgs().length > 0) { 
            transaction.setUnitId(HashUtil.md5(transaction.getUnitId() + Arrays.hashCode(invocation.getArgs())));
        }
        transactionHandler.runTransaction(transaction, invocation.getArgs(), invocation::invoke);
    }

    public static TransactionInfo getFromCache(Invocation methodInvocation) {
        Method method = methodInvocation.getMethod();
        String signature = method.toString();
        String unitId = HashUtil.md5(Transaction.APPLICATION_ID + signature);
        return TransactionInfoCache.computeIfAbsent(unitId, (id)->{
            TransactionInfo info = new TransactionInfo();
            info.setBusinessMethod(method);
            
            Transactional transactional = method.getAnnotation(Transactional.class);
            if(transactional == null) {
                transactional = method.getDeclaringClass().getAnnotation(Transactional.class);
            }
            info.setTxAnnotation(transactional);
            info.setTransactionType(getTransactionType(transactional.type(), method));
            info.setTransactionPropagation(transactional.propagation());
            info.setTargetClass(methodInvocation.getTarget().getClass());
            info.setMethodName(method.getName());
            info.setMethodStr(signature);
            info.setParameterTypes(method.getParameterTypes());
            info.setUnitId(unitId);
            return info;
        });
    }

    private static String getTransactionType(String type, Method method) {
        String finalType;
        if(StrUtil.notBlank(type)) {
            finalType = type.toLowerCase();
        } else {
            finalType =  Cloud.getTransactionManager().getConfig().getDefaultType();
        }
        try {
            if (Cloud.getTransactionManager().verifyTransactionType(finalType, method)) {
                return finalType;
            }
        } catch (TransactionTypeException e) {
            throw new RuntimeException("invalid transaction type " + type + " on " + method.toString(), e);
        }
        throw new RuntimeException("unsupported transaction type " + type + " on " + method.toString());
    }
}
