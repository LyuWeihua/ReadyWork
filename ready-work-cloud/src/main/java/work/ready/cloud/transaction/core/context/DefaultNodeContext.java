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
package work.ready.cloud.transaction.core.context;

import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.transaction.TransactionConfig;
import work.ready.cloud.transaction.tracing.TracingContext;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;

public class DefaultNodeContext implements DtxNodeContext {
    private static final Log logger = LogFactory.getLog(DefaultNodeContext.class);
    private final AttachmentCache attachmentCache;
    private final TransactionConfig transactionConfig;

    public DefaultNodeContext() {
        this.transactionConfig = Cloud.getTransactionManager().getConfig();
        this.attachmentCache = Ready.beanManager().get(AttachmentCache.class, MapAttachmentCache.class);
    }

    @Override
    public boolean containsKey(String mainKey, String key) {
        return attachmentCache.containsKey(mainKey, key);
    }

    @Override
    public boolean containsKey(String key) {
        return attachmentCache.containsKey(key);
    }

    @Override
    public <T> T attachment(String mainKey, String key) {
        return attachmentCache.attachment(mainKey, key);
    }

    @Override
    public <T> T attachment(String key) {
        return attachmentCache.attachment(key);
    }

    @Override
    public void attach(String mainKey, String key, Object attachment) {
        attachmentCache.attach(mainKey, key, attachment);
    }

    @Override
    public void attach(String key, Object attachment) {
        attachmentCache.attach(key, attachment);
    }

    @Override
    public TxContext startTx() {
        TxContext txContext = new TxContext();
        
        txContext.setStarter(!TracingContext.tracing().hasGroup());
        if (txContext.isStarter()) {
            TracingContext.tracing().beginTransactionGroup();
        }
        txContext.setGroupId(TracingContext.tracing().groupId());
        String txContextKey = txContext.getGroupId() + ".dtx";
        attachmentCache.attach(txContextKey, txContext);
        logger.debug("Start TxContext[%s]", txContext.getGroupId());
        return txContext;
    }

    @Override
    public void destroyTx(String groupId) {
        attachmentCache.remove(groupId + ".dtx");
        logger.debug("Destroy TxContext[%s]", groupId);
    }

    @Override
    public TxContext txContext(String groupId) {
        return attachmentCache.attachment(groupId + ".dtx");
    }

    @Override
    public TxContext txContext() {
        return txContext(TracingContext.tracing().groupId());
    }

    @Override
    public void destroyTx() {
        if (!hasTxContext()) {
            throw new IllegalStateException("none TxContext.");
        }
        destroyTx(txContext().getGroupId());
    }

    @Override
    public boolean hasTxContext() {
        return TracingContext.tracing().hasGroup() && txContext(TracingContext.tracing().groupId()) != null;
    }

    @Override
    public boolean isDtxTimeout() {
        if (!hasTxContext()) {
            throw new IllegalStateException("none TxContext.");
        }
        return (Ready.currentTimeMillis() - txContext().getCreateTime()) >= transactionConfig.getTxTimeout();
    }

    @Override
    public int dtxState(String groupId) {
        return this.attachmentCache.containsKey(groupId, "rollback-only") ? 0 : 1;
    }

    @Override
    public void setRollbackOnly(String groupId) {
        this.attachmentCache.attach(groupId, "rollback-only", true);
    }

    @Override
    public void clearGroup(String groupId) {
        
        this.attachmentCache.removeAll(groupId);
    }
}
