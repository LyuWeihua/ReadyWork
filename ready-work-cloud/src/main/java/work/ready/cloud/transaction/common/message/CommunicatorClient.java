/**
 *
 * Copyright (c) 2020 WeiHua Lyu [ready.work]
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

package work.ready.cloud.transaction.common.message;

import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.cluster.common.MessageBody;
import work.ready.cloud.cluster.common.MessageCmd;
import work.ready.cloud.cluster.common.MessageException;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.tools.validator.Assert;

import java.time.Duration;
import java.util.UUID;

public class CommunicatorClient extends MessageClient {
    private static final Log logger = LogFactory.getLog(CommunicatorClient.class);

    @Override
    public ResponseState send(MessageCmd messageCmd) throws MessageException {
        MessageBody result = null;
        long startTime = System.nanoTime();
        logger.debug("Communicator cmd send begin");
        result = Cloud.reliableMessage().send(messageCmd.getNodeId(), DTX_COORDINATOR_CHANNEL, DTX_COMMUNICATOR_CHANNEL, messageCmd.getMessage());
        logger.debug("Communicator cmd send end, elapsed: %s ms", Duration.ofNanos(System.nanoTime() - startTime).toMillis());
        return result == null ? ResponseState.fail : ResponseState.success;
    }

    @Override
    public ResponseState reply(MessageCmd messageCmd) throws MessageException {
        Assert.notEmpty(messageCmd.getMessageId(), "reply id can not be empty");
        MessageBody result = null;
        long startTime = System.nanoTime();
        logger.debug("Communicator cmd reply[%s] begin",  messageCmd.getMessageId());
        result = Cloud.reliableMessage().reply(messageCmd.getNodeId(), DTX_COORDINATOR_CHANNEL, DTX_COMMUNICATOR_CHANNEL, messageCmd.getMessageId(), messageCmd.getMessage());
        logger.debug("Communicator cmd reply[%s] end, elapsed: %s ms",  messageCmd.getMessageId(), Duration.ofNanos(System.nanoTime() - startTime).toMillis());
        return result == null ? ResponseState.fail : ResponseState.success;
    }

    @Override
    public void finalReply(MessageCmd messageCmd) throws MessageException {
        Assert.notEmpty(messageCmd.getMessageId(), "reply id can not be empty");
        Cloud.reliableMessage().finalReply(messageCmd.getNodeId(), DTX_COORDINATOR_CHANNEL, DTX_COMMUNICATOR_CHANNEL, messageCmd.getMessageId(), messageCmd.getMessage());
    }

    @Override
    public ResponseState send(UUID nodeId, MessageBody msg) throws MessageException {
        MessageCmd messageCmd = new MessageCmd();
        messageCmd.setMessage(msg);
        messageCmd.setNodeId(nodeId);
        return send(messageCmd);
    }

    @Override
    public MessageBody request(MessageCmd messageCmd) throws MessageException {
        return request0(messageCmd, -1);
    }

    private MessageBody request0(MessageCmd messageCmd, long timeout) throws MessageException {
        return Cloud.reliableMessage().send(messageCmd.getNodeId(), DTX_COORDINATOR_CHANNEL, DTX_COMMUNICATOR_CHANNEL,
                messageCmd.getMessage(),
                timeout > 0 ? Duration.ofMillis(timeout) : Duration.ofMillis(60 * 1000));
    }

    @Override
    public MessageBody request(UUID nodeId, MessageBody msg) throws MessageException {
        return request(nodeId, msg, -1);
    }

    @Override
    public MessageBody request(UUID nodeId, MessageBody msg, long timeout) throws MessageException {
        long startTime = System.nanoTime();
        MessageCmd messageCmd = new MessageCmd();
        messageCmd.setMessage(msg);
        messageCmd.setNodeId(nodeId);
        logger.debug("Communicator cmd request%s begin", messageCmd.getMessageId() != null ? "[" + messageCmd.getMessageId() + "]" : "");
        MessageBody result = request0(messageCmd, timeout);
        logger.debug("Communicator cmd request%s end, elapsed: %s ms",  messageCmd.getMessageId() != null ? "[" + messageCmd.getMessageId() + "]" : "", Duration.ofNanos(System.nanoTime() - startTime).toMillis());
        return result;
    }

}
