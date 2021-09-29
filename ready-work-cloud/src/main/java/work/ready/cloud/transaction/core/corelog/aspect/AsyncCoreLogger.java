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
package work.ready.cloud.transaction.core.corelog.aspect;

import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.transaction.core.interceptor.TransactionInfo;
import work.ready.cloud.transaction.common.serializer.SerializerContext;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;
import work.ready.core.tools.ReadyThreadFactory;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AsyncCoreLogger implements CoreLogger {

    private static final Log logger = LogFactory.getLog(AsyncCoreLogger.class);
    private static final ExecutorService executorService =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ReadyThreadFactory("AsyncAspectLogger"));

    private final AspectLogHelper txLogHelper;

    public AsyncCoreLogger() {
        this.txLogHelper = Cloud.getTransactionManager().getAspectLogHelper();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            executorService.shutdown();
            try {
                executorService.awaitTermination(6, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
        }));
    }

    @Override
    public void trace(String groupId, String unitId, TransactionInfo transactionInfo) {
        executorService.submit(() -> {
            long t1 = System.nanoTime();
            byte[] bytes;
            bytes = SerializerContext.getInstance().serialize(transactionInfo);
            AspectLog txLog = new AspectLog();
            txLog.setBytes(bytes);
            txLog.setGroupId(groupId);
            txLog.setUnitId(unitId);
            txLog.setMethodStr(transactionInfo.getMethodStr());
            txLog.setTime(Ready.currentTimeMillis());
            txLog.setGroupIdHash(groupId.hashCode());
            txLog.setUnitIdHash(unitId.hashCode());

            boolean res = txLogHelper.save(txLog);
            long t2 = System.nanoTime();
            logger.debug("async save aspect log. result: %s groupId: %s, used time: %sms", res, groupId, Duration.ofNanos(t2 - t1).toMillis());
        });
    }

    @Override
    public void clearLog(String groupId, String unitId) {
        executorService.submit(() -> {
            long t1 = System.nanoTime();
            boolean res = txLogHelper.delete(groupId.hashCode(), unitId.hashCode());
            long t2 = System.nanoTime();
            logger.debug("async clear aspect log. result:%s, groupId: %s, used time: %sms", res, groupId, Duration.ofNanos(t2 - t1).toMillis());
        });
    }
}
