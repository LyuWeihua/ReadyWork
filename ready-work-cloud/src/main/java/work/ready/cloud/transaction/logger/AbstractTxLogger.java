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
package work.ready.cloud.transaction.logger;

import work.ready.cloud.ReadyCloud;
import work.ready.cloud.transaction.logger.db.TxLog;
import work.ready.cloud.transaction.common.Transaction;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;
import work.ready.core.tools.StrUtil;

import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AbstractTxLogger implements TxLogger {

    private static ExecutorService loggerSaveService;

    private final Log logger;

    protected final TxLoggerConfig txLoggerConfig;

    public AbstractTxLogger(Class<?> className) {
        this.txLoggerConfig = ReadyCloud.getConfig().getTransaction().getTxLogger();
        if(loggerSaveService == null) {
            loggerSaveService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        }
        this.logger = LogFactory.getLog(className);
        this.logger.setForward(2); 
    }

    @Override
    public void trace(String groupId, String unitId, String tag, String content, Object... args) {
        if (!txLoggerConfig.isOnlyError()) {
            saveTxLog(groupId, unitId, tag, content, args);
        }
        logger.debug(content + " @group(" + groupId + ")", args);
    }

    @Override
    public void error(String groupId, String unitId, String tag, String content, Object... args) {
        saveTxLog(groupId, unitId, tag, content, args);
        logger.error(content + " @group(" + groupId + ")", args);
    }

    private void saveTxLog(String groupId, String unitId, String tag, String content, Object... args) {
        TxLog txLog = new TxLog();
        txLog.setContent(content);
        txLog.setArgs(args);
        txLog.setTag(tag);
        txLog.setGroupId(StrUtil.isEmpty(groupId) ? "" : groupId);
        txLog.setUnitId(StrUtil.isEmpty(unitId) ? "" : unitId);
        txLog.setAppName(Transaction.APPLICATION_ID);
        txLog.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS").format(Ready.now()));
        loggerSaveService.execute(() -> saveLog(txLog));
    }

    public abstract void saveLog(TxLog txLog);
}
