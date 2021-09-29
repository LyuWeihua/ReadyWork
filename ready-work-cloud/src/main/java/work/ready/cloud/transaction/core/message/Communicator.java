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
import work.ready.cloud.transaction.common.exception.TxCommunicationException;
import work.ready.cloud.transaction.common.message.*;
import work.ready.cloud.cluster.common.MessageBody;
import work.ready.cloud.cluster.common.MessageCmd;
import work.ready.cloud.cluster.common.MessageState;
import work.ready.cloud.cluster.common.MessageException;
import work.ready.cloud.transaction.common.message.params.NotifyUnitParams;
import work.ready.core.ioc.annotation.DisposableBean;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

import java.io.Serializable;

public class Communicator implements MessageHandler, DisposableBean {
    private static final Log logger = LogFactory.getLog(Communicator.class);

    private final CommunicatorClient messageClient;

    public Communicator() {
        this.messageClient = Cloud.getTransactionManager().getCommunicatorClient();
    }

    public void listen() {
        Cloud.reliableMessage().addListener(MessageClient.DTX_COMMUNICATOR_CHANNEL, (cmd, result) -> {
            if(MessageState.STATE_OK == cmd.getMessage().getState()) {
                if(logger.isTraceEnabled()) {
                    logger.trace("Communicator received STATE_OK reply for %s", cmd.getMessageId());
                }
            } else {
                callback(cmd);
            }
            return true;
        });
    }

    @Override
    public void callback(MessageCmd messageCmd) {
        if(logger.isTraceEnabled()) {
            logger.trace("Communicator received message [msg=" + messageCmd + ", from remote=" + messageCmd.getNodeId() + "]");
        }
        TransactionCmd transactionCmd = parser(messageCmd);
        String transactionType = transactionCmd.getTransactionType();
        String action = transactionCmd.getMessage().getAction();
        CmdExecuteService cmdExecuteService = null;
        try{
            cmdExecuteService = Cloud.getTransactionManager().getCmdExecuteService(transactionType, transactionCmd.getType());
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        MessageBody messageBody = null;
        try {
            if(cmdExecuteService == null) {
                if(MessageState.STATE_EXCEPTION == transactionCmd.getMessage().getState()) {
                    logger.error("Communicator received unhandled exception message: %s", messageCmd);
                } else {
                    logger.error("Communicator received unhandled message: %s", messageCmd);
                }
                messageBody = new MessageBody().setState(MessageState.STATE_OK);
            } else {
                Serializable message = cmdExecuteService.execute(transactionCmd);
                messageBody = MessageCreator.notifyUnitOkResponse(message, action);
            }
        } catch (TxCommunicationException e) {
            logger.error(e, "Communicator service execute error. action: ");
            messageBody = MessageCreator.notifyUnitFailResponse(e, action);
        } catch (Throwable e) {
            logger.error(e, "Communicator service execute error. action: " + action);
            messageBody = MessageCreator.notifyUnitFailResponse(e, action);
        } finally {
            if(messageBody != null) {
                logger.trace("Communicator sending " + messageBody);
                if (messageCmd.getMessageId() != null) {
                    try {
                        messageCmd.setMessage(messageBody);
                        messageClient.finalReply(messageCmd);
                    } catch (MessageException e) {
                        logger.error("response request[%s] error. error message: %s", messageCmd.getMessageId(), e.getMessage());
                    }
                }
            }
        }
    }

    @Override
    public void destroy() throws Exception {
    }

    public static TransactionCmd parser(MessageCmd messageCmd) {
        TransactionCmd cmd = new TransactionCmd();
        cmd.setMessageId(messageCmd.getMessageId());
        cmd.setType(CmdType.parserCmd(messageCmd.getMessage().getAction()));
        cmd.setGroupId(messageCmd.getMessage().getGroupId());

        if (messageCmd.getMessage().getAction().equals(MessageConstants.ACTION_NOTIFY_UNIT)) {
            NotifyUnitParams notifyUnitParams = messageCmd.getMessage().loadBean(NotifyUnitParams.class);
            cmd.setTransactionType(notifyUnitParams.getType());
        }

        cmd.setMessage(messageCmd.getMessage());
        return cmd;
    }
}
