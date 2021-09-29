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

package work.ready.cloud.cluster.common;

import java.io.Serializable;
import java.util.UUID;

public class MessageCmd implements Serializable {

    private String sendToChannel;

    private String receiveChannel;

    private String messageId;

    private MessageBody message;

    private UUID nodeId;

    public String getSendToChannel() {
        return sendToChannel;
    }

    public MessageCmd setSendToChannel(String sendToChannel) {
        this.sendToChannel = sendToChannel;
        return this;
    }

    public String getReceiveChannel() {
        return receiveChannel;
    }

    public MessageCmd setReceiveChannel(String receiveChannel) {
        this.receiveChannel = receiveChannel;
        return this;
    }

    public MessageCmd setChannel(String channel) {
        this.sendToChannel = channel;
        this.receiveChannel = channel;
        return this;
    }

    public String getChannel() {
        return sendToChannel;
    }

    public String getMessageId() {
        return messageId;
    }

    public MessageCmd setMessageId(String messageId) {
        this.messageId = messageId;
        return this;
    }

    public MessageBody getMessage() {
        return message;
    }

    public MessageCmd setMessage(MessageBody message) {
        this.message = message;
        return this;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public MessageCmd setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
        return this;
    }

    @Override
    public String toString() {
        return "{" +
                "sendToChannel='" + sendToChannel + '\'' +
                ", receiveChannel='" + receiveChannel + '\'' +
                ", messageId='" + messageId + '\'' +
                ", message=" + message +
                ", nodeId=" + nodeId +
                '}';
    }
}
