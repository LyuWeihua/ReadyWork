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
import work.ready.cloud.transaction.TransactionConfig;
import work.ready.cloud.transaction.core.context.DtxThreadContext;
import work.ready.cloud.transaction.core.context.DtxNodeContext;
import work.ready.cloud.transaction.core.context.TxContext;
import work.ready.cloud.transaction.common.BusinessCallback;
import work.ready.cloud.transaction.tracing.TracingContext;
import work.ready.cloud.transaction.tracing.TracingHelper;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;
import work.ready.core.tools.ReadyThreadFactory;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

public class TransactionHandler {
    private static final Log logger = LogFactory.getLog(TransactionHandler.class);
    private final TransactionConfig config;
    private final DtxLogicExecutor transactionLogicExecutor;
    private final DtxNodeContext nodeContext;
    private final ThreadPoolExecutor poolExecutor = (ThreadPoolExecutor)Executors.newCachedThreadPool(new ReadyThreadFactory("SyncTransaction", 7));

    public TransactionHandler() {
        this.config = Cloud.getTransactionManager().getConfig();
        this.transactionLogicExecutor = Ready.beanManager().get(DtxLogicExecutor.class);
        this.nodeContext = Cloud.getTransactionManager().getNodeContext();
    }

    public Object runTransaction(TransactionInfo info, Object[] args, BusinessCallback business) throws Throwable {

        if (DtxThreadContext.current() != null) {
            
            if(config.isLocalThreadTransactionIsolation()) {
                String groupId = DtxThreadContext.current().getGroupId();
                Future<Object> futureResult = poolExecutor.submit(() -> {
                    
                    DtxThreadContext.close();
                    TracingContext.init(Map.of(TracingHelper.GROUP_ID, groupId, TracingHelper.APP_MAP, ""));
                    try {
                        return handleTransaction(info, args, business);
                    } catch (Throwable e) {
                        if(e instanceof Exception) {
                            throw (Exception)e;
                        } else {
                            throw new Exception(e);
                        }
                    }
                });
                return futureResult.get();
            } else {
                
                logger.debug("Combining with parent transaction.", info.getTransactionType());
                return business.call();
            }
        }
        return handleTransaction(info, args, business);
    }

    private Object handleTransaction(TransactionInfo info, Object[] args, BusinessCallback business) throws Throwable {
        logger.debug("<---- Transaction start ---->");
        DtxThreadContext dtxThreadContext = DtxThreadContext.getOrNew();
        TxContext txContext;
        if (nodeContext.hasTxContext()) {
            
            txContext = nodeContext.txContext();
            dtxThreadContext.setInGroup(true);
            logger.debug("Unit[%s] use parent's TxContext[%s].", info.getUnitId(), txContext.getGroupId());
        } else {
            
            logger.debug("no TxContext exist on this node, create and start a transaction on this node.");
            txContext = nodeContext.startTx();
        }

        dtxThreadContext.setUnitId(info.getUnitId());
        dtxThreadContext.setGroupId(txContext.getGroupId());
        dtxThreadContext.setTransactionType(info.getTransactionType());

        DtxTransactionInfo txInfo = new DtxTransactionInfo();
        txInfo.setBusinessCallback(business);
        txInfo.setArguments(args);
        txInfo.setGroupId(txContext.getGroupId());
        txInfo.setTransactionInfo(info);
        
        if(dtxThreadContext.isInGroup()) {
            txInfo.setStarter(false);
        } else {
            txInfo.setStarter(txContext.isStarter()); 
        }

        try {
            return transactionLogicExecutor.run(txInfo);
        } finally {
            DtxThreadContext.close();

            if (!dtxThreadContext.isInGroup()) {
                
                synchronized (txContext.getLock()) {
                    txContext.getLock().notifyAll();
                }
                nodeContext.destroyTx();
            }
            TracingContext.tracing().destroy();
            logger.debug("<---- Transaction end ---->");
        }
    }

}
