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

import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.transaction.TransactionConfig;
import work.ready.cloud.transaction.common.exception.TxBusinessException;
import work.ready.cloud.transaction.common.message.CommunicatorClient;
import work.ready.cloud.cluster.common.MessageBody;
import work.ready.cloud.cluster.common.MessageException;
import work.ready.cloud.transaction.common.message.params.JoinGroupParams;
import work.ready.cloud.transaction.common.message.params.NotifyGroupParams;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DefaultMessenger implements ReliableMessenger {
    private static final Log logger = LogFactory.getLog(DefaultMessenger.class);

    public static final int NO_TX_COORDINATOR = 40010;

    private final CommunicatorClient messageClient;

    private final TransactionConfig transactionConfig;

    public DefaultMessenger() {
        this.messageClient = Cloud.getTransactionManager().getCommunicatorClient();
        this.transactionConfig = Cloud.getTransactionManager().getConfig();
    }

    @Override
    public boolean acquireLocks(String groupId, Map<String, Set<String>> lockMap, int type) throws MessageException {
        MessageBody messageBody = request(MessageCreator.acquireLocks(groupId, lockMap, type));
        return messageBody.isStateOk();
    }

    @Override
    public void releaseLocks(Map<String, Set<String>> lockMap) throws MessageException {
        MessageBody messageBody = request(MessageCreator.releaseLocks(lockMap));
        if (!messageBody.isStateOk()) {
            throw new MessageException("release locks fail.");
        }
    }

    @Override
    public int notifyGroup(String groupId, int transactionState) throws MessageException, TxBusinessException {
        NotifyGroupParams notifyGroupParams = new NotifyGroupParams();
        notifyGroupParams.setGroupId(groupId);
        notifyGroupParams.setState(transactionState);
        MessageBody messageBody = request0(MessageCreator.notifyGroup(notifyGroupParams),
                transactionConfig.getCommunicationTimeout() * transactionConfig.getChainLevel());
        
        if (!messageBody.isStateOk()) {
            throw new TxBusinessException(messageBody.loadBean(Throwable.class));
        }
        return messageBody.loadBean(Integer.class);
    }

    @Override
    public void joinGroup(String groupId, String unitId, String type, int transactionState) throws MessageException, TxBusinessException {
        JoinGroupParams joinGroupParams = new JoinGroupParams();
        joinGroupParams.setGroupId(groupId);
        joinGroupParams.setUnitId(unitId);
        joinGroupParams.setType(type);
        joinGroupParams.setTransactionState(transactionState);
        MessageBody messageBody = request(MessageCreator.joinGroup(joinGroupParams));
        if (!messageBody.isStateOk()) {
            throw new TxBusinessException(messageBody.loadBean(Throwable.class));
        }
    }

    @Override
    public void createGroup(String groupId) throws MessageException, TxBusinessException {
        
        MessageBody messageBody = request(MessageCreator.createGroup(groupId));
        if (!messageBody.isStateOk()) {
            throw new TxBusinessException(messageBody.loadBean(Throwable.class));
        }
    }

    @Override
    public int askTransactionState(String groupId, String unitId) throws MessageException {
        MessageBody messageBody = request(MessageCreator.askTransactionState(groupId, unitId));
        if (messageBody.isStateOk()) {
            return messageBody.loadBean(Integer.class);
        }
        return -1;
    }

    @Override
    public MessageBody request(MessageBody messageBody) throws MessageException {
        return request0(messageBody, -1);
    }

    private MessageBody request0(MessageBody messageBody, long timeout) throws MessageException {
        return request(messageBody, timeout, "request fail");
    }

    private MessageBody request(MessageBody messageBody, long timeout, String whenNoCoordinatorMessage) throws MessageException {
        for (int i = 0; i < messageClient.getAllCoordinators().size() + 1; i++) {
            try {
                UUID nodeId = messageClient.pickCoordinator();
                MessageBody result = messageClient.request(nodeId, messageBody, timeout);
                logger.debug("request action: %s. Coordinator[%s]", messageBody.getAction(), nodeId);
                return result;
            } catch (MessageException e) {
                if (e.getCode() == NO_TX_COORDINATOR) {
                    throw new MessageException(e.getCode(), whenNoCoordinatorMessage + ". no coordinator is available.");
                }
            }
        }
        throw new MessageException(NO_TX_COORDINATOR, whenNoCoordinatorMessage + ". no coordinator is available.");
    }
}
