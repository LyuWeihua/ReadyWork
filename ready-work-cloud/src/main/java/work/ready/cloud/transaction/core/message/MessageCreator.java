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

import work.ready.cloud.transaction.common.Transaction;
import work.ready.cloud.transaction.common.message.MessageConstants;
import work.ready.cloud.cluster.common.MessageBody;
import work.ready.cloud.cluster.common.MessageState;
import work.ready.cloud.transaction.common.message.params.*;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public class MessageCreator {

    public static MessageBody createGroup(String groupId) {
        MessageBody msg = new MessageBody();
        msg.setGroupId(groupId);
        msg.setAction(MessageConstants.ACTION_CREATE_GROUP);
        return msg;
    }

    public static MessageBody joinGroup(JoinGroupParams joinGroupParams) {
        MessageBody msg = new MessageBody();
        msg.setGroupId(joinGroupParams.getGroupId());
        msg.setAction(MessageConstants.ACTION_JOIN_GROUP);
        msg.setData(joinGroupParams);
        return msg;
    }

    public static MessageBody notifyGroup(NotifyGroupParams notifyGroupParams) {
        MessageBody msg = new MessageBody();
        msg.setGroupId(notifyGroupParams.getGroupId());
        msg.setAction(MessageConstants.ACTION_NOTIFY_GROUP);
        msg.setData(notifyGroupParams);
        return msg;
    }

    public static MessageBody acquireLocks(String groupId, Map<String, Set<String>> lockMap, int lockType) {
        DtxLockParams dtxLockParams = new DtxLockParams();
        dtxLockParams.setGroupId(groupId);
        dtxLockParams.setContextId(Transaction.APPLICATION_ID);
        dtxLockParams.setLockMap(lockMap);
        dtxLockParams.setLockType(lockType);
        MessageBody messageBody = new MessageBody();
        messageBody.setAction(MessageConstants.ACTION_ACQUIRE_DTX_LOCK);
        messageBody.setData(dtxLockParams);
        return messageBody;
    }

    public static MessageBody releaseLocks(Map<String, Set<String>> lockMap) {
        DtxLockParams dtxLockParams = new DtxLockParams();
        dtxLockParams.setContextId(Transaction.APPLICATION_ID);
        dtxLockParams.setLockMap(lockMap);
        MessageBody messageBody = new MessageBody();
        messageBody.setAction(MessageConstants.ACTION_RELEASE_DTX_LOCK);
        messageBody.setData(dtxLockParams);
        return messageBody;
    }

    public static MessageBody notifyUnitOkResponse(Serializable message, String action) {
        MessageBody messageBody = new MessageBody();
        messageBody.setAction(action);
        messageBody.setState(MessageState.STATE_OK);
        messageBody.setData(message);
        return messageBody;
    }

    public static MessageBody notifyUnitFailResponse(Serializable message, String action) {
        MessageBody messageBody = new MessageBody();
        messageBody.setState(MessageState.STATE_EXCEPTION);
        messageBody.setAction(action);
        messageBody.setData(message);
        return messageBody;
    }

    public static MessageBody askTransactionState(String groupId, String unitId) {
        MessageBody messageBody = new MessageBody();
        messageBody.setGroupId(groupId);
        messageBody.setAction(MessageConstants.ACTION_ASK_TRANSACTION_STATE);
        messageBody.setData(new AskTransactionStateParams(groupId, unitId));
        return messageBody;
    }

    public static MessageBody writeTxException(TxExceptionParams txExceptionParams) {
        MessageBody messageBody = new MessageBody();
        messageBody.setAction(MessageConstants.ACTION_WRITE_EXCEPTION);
        messageBody.setGroupId(txExceptionParams.getGroupId());
        messageBody.setData(txExceptionParams);
        return messageBody;
    }
}
