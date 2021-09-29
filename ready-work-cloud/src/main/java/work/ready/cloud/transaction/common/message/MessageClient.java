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
package work.ready.cloud.transaction.common.message;

import org.apache.ignite.cluster.ClusterNode;
import work.ready.cloud.ReadyCloud;
import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.cluster.common.MessageBody;
import work.ready.cloud.cluster.common.MessageCmd;
import work.ready.cloud.cluster.common.MessageException;
import work.ready.cloud.registry.base.URL;
import work.ready.cloud.registry.base.URLParam;
import work.ready.cloud.transaction.DistributedTransactionManager;
import work.ready.core.server.Constant;
import work.ready.core.server.Ready;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static work.ready.cloud.transaction.core.message.DefaultMessenger.NO_TX_COORDINATOR;

public abstract class MessageClient {

    public static final String DTX_COORDINATOR_CHANNEL = "DTX_COORDINATOR_CHANNEL";
    public static final String DTX_COMMUNICATOR_CHANNEL = "DTX_COMMUNICATOR_CHANNEL";

    public abstract ResponseState send(MessageCmd messageCmd) throws MessageException;

    public abstract ResponseState reply(MessageCmd messageCmd) throws MessageException;

    public abstract void finalReply(MessageCmd messageCmd) throws MessageException;

    public abstract ResponseState send(UUID nodeId, MessageBody msg) throws MessageException;

    public abstract MessageBody request(MessageCmd messageCmd) throws MessageException;

    public abstract MessageBody request(UUID nodeId, MessageBody msg) throws MessageException;

    public abstract MessageBody request(UUID nodeId, MessageBody msg, long timeout) throws MessageException;

    public UUID pickCoordinator() throws MessageException {
        URL url = Cloud.discover(Constant.PROTOCOL_DEFAULT, DistributedTransactionManager.SERVICE_ID, Ready.getBootstrapConfig().getActiveProfile(), null);
        if(url != null) {
            return UUID.fromString(url.getParameter(URLParam.nodeId.getName()));
        }
        throw new MessageException(NO_TX_COORDINATOR, "no available coordinator");
    }

    public List<UUID> getAllCoordinators() {
        var list = Cloud.discoverAll(Constant.PROTOCOL_DEFAULT, DistributedTransactionManager.SERVICE_ID, Ready.getBootstrapConfig().getActiveProfile());
        return list.stream().map(uri->UUID.fromString(uri.getParameter(URLParam.nodeId.getName()))).collect(Collectors.toList());
    }

    public UUID getNodeId(String nodeName) {
        var nodes = Cloud.cluster().nodes();
        for(ClusterNode node : nodes) {
            if(node.consistentId().equals(nodeName)) {
                return node.id();
            }
        }
        return null;
    }

    public String getNodeName(UUID nodeId) {
        return Cloud.cluster().node(nodeId).consistentId().toString();
    }
}
