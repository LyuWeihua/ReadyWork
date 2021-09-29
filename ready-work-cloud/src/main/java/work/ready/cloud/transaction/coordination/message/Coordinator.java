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
package work.ready.cloud.transaction.coordination.message;

import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.transaction.common.message.CmdType;
import work.ready.cloud.transaction.common.message.CoordinatorClient;
import work.ready.cloud.transaction.common.message.MessageClient;
import work.ready.cloud.transaction.common.message.MessageHandler;
import work.ready.cloud.cluster.common.MessageBody;
import work.ready.cloud.cluster.common.MessageCmd;
import work.ready.cloud.cluster.common.MessageState;
import work.ready.cloud.cluster.common.MessageException;
import work.ready.cloud.transaction.coordination.core.DtxContextRegistry;
import work.ready.cloud.transaction.coordination.core.DefaultDtxContextRegistry;
import work.ready.cloud.transaction.coordination.core.GlobalTransactionCoordinator;
import work.ready.cloud.transaction.coordination.core.TransactionCoordinator;
import work.ready.cloud.transaction.coordination.core.storage.FastStorage;
import work.ready.cloud.transaction.coordination.core.storage.IgniteStorage;
import work.ready.core.ioc.annotation.DisposableBean;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;

import java.io.Serializable;

public class Coordinator implements MessageHandler, DisposableBean {
    private static final Log logger = LogFactory.getLog(Coordinator.class);
    private final CoordinatorClient messageClient;
    private TransactionCoordinator transactionCoordinator;
    private DtxContextRegistry contextRegistry;
    private FastStorage fastStorage;

    public Coordinator() {
        var config = Cloud.getTransactionManager().getConfig();
        config.setConcurrentLevel(Math.max(Runtime.getRuntime().availableProcessors() * 10, config.getConcurrentLevel()));
        this.messageClient = Cloud.getTransactionManager().getCoordinatorClient();
    }

    public TransactionCoordinator getTransactionCoordinator() {
        if(transactionCoordinator == null) {
            transactionCoordinator = Ready.beanManager().get(TransactionCoordinator.class, GlobalTransactionCoordinator.class);
        }
        return transactionCoordinator;
    }

    public DtxContextRegistry getContextRegistry() {
        if(contextRegistry == null) {
            contextRegistry = Ready.beanManager().get(DtxContextRegistry.class, DefaultDtxContextRegistry.class);
        }
        return contextRegistry;
    }

    public FastStorage getFastStorage() {
        if(fastStorage == null) {
            fastStorage = Ready.beanManager().get(FastStorage.class, IgniteStorage.class);
        }
        return fastStorage;
    }

    public void listen() {
        Cloud.reliableMessage().addListener(MessageClient.DTX_COORDINATOR_CHANNEL, (cmd, result)->{
            if(MessageState.STATE_OK == cmd.getMessage().getState()) {
                if(logger.isTraceEnabled()) {
                    logger.trace("Coordinator received STATE_OK reply for %s", cmd.getMessageId());
                }
            } else {
                callback(cmd);
            }
            return true;
        });
    }

    @Override
    public void callback(MessageCmd messageCmd) {
        try {
            if(logger.isTraceEnabled()) {
                logger.trace("Coordinator received message [msg=" + messageCmd + ", from remote=" + messageCmd.getNodeId() + "]");
            }
            TransactionCmd transactionCmd = parser(messageCmd);
            String action = transactionCmd.getMessage().getAction();
            CoordinationCmdService cmdService = Cloud.getTransactionManager().getCoordinationCmdService(transactionCmd.getType());
            MessageBody messageBody = null;
            try {
                if(cmdService == null) {
                    if(MessageState.STATE_EXCEPTION == transactionCmd.getMessage().getState()) {
                        logger.error("Coordinator received unhandled exception message: %s", messageCmd);
                    } else {
                        logger.error("Coordinator received unhandled message: %s", messageCmd);
                    }
                    messageBody = new MessageBody().setState(MessageState.STATE_OK);
                } else {
                    Serializable message = cmdService.execute(transactionCmd);
                    messageBody = MessageCreator.okResponse(message, action);
                }
            } catch (Throwable e) {
                logger.error(e, "Coordinator execute service error. action: " + action);
                messageBody = MessageCreator.failResponse(e, action);
            } finally {
                if(messageBody != null) {
                    
                    if (messageCmd.getMessageId() != null) {
                        messageBody.setGroupId(messageCmd.getMessage().getGroupId());
                        messageCmd.setMessage(messageBody);
                        logger.trace("Coordinator sending " + messageBody);
                        try {
                            messageClient.finalReply(messageCmd);
                        } catch (MessageException ignored) {
                            ignored.printStackTrace();
                        }
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            if (messageCmd.getMessageId() != null) {
                logger.info("Coordinator send response.");
                String action = messageCmd.getMessage().getAction();
                
                messageCmd.setMessage(MessageCreator.serverException(action));
                try {
                    messageClient.finalReply(messageCmd);
                    logger.info("send response ok.");
                } catch (MessageException ignored) {
                    logger.error("requester:%s dead.", messageCmd.getNodeId());
                }
            }
        }
    }

    @Override
    public void destroy() throws Exception {
    }

    private TransactionCmd parser(MessageCmd messageCmd) {
        TransactionCmd cmd = new TransactionCmd();
        cmd.setMessageId(messageCmd.getMessageId());
        cmd.setNodeId(messageCmd.getNodeId());
        cmd.setType(CmdType.parserCmd(messageCmd.getMessage().getAction()));
        cmd.setGroupId(messageCmd.getMessage().getGroupId());
        cmd.setMessage(messageCmd.getMessage());
        return cmd;
    }

}
