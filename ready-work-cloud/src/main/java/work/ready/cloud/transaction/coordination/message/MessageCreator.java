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

import work.ready.cloud.transaction.common.message.MessageConstants;
import work.ready.cloud.cluster.common.MessageBody;
import work.ready.cloud.cluster.common.MessageState;
import work.ready.cloud.transaction.common.message.params.DeleteAspectLogParams;
import work.ready.cloud.transaction.common.message.params.GetAspectLogParams;
import work.ready.cloud.transaction.common.message.params.NotifyUnitParams;

import java.io.Serializable;

public class MessageCreator {

    public static MessageBody notifyUnit(NotifyUnitParams notifyUnitParams) {
        MessageBody msg = new MessageBody();
        msg.setGroupId(notifyUnitParams.getGroupId());
        msg.setAction(MessageConstants.ACTION_NOTIFY_UNIT);
        msg.setData(notifyUnitParams);
        return msg;
    }

    public static MessageBody okResponse(Serializable message, String action) {
        MessageBody messageBody = new MessageBody();
        messageBody.setState(MessageState.STATE_OK);
        messageBody.setAction(action);
        messageBody.setData(message);
        return messageBody;
    }

    public static MessageBody failResponse(Serializable message, String action) {
        MessageBody messageBody = new MessageBody();
        messageBody.setAction(action);
        messageBody.setState(MessageState.STATE_EXCEPTION);
        messageBody.setData(message);
        return messageBody;
    }

    public static MessageBody serverException(String action) {
        MessageBody messageBody = new MessageBody();
        messageBody.setAction(action);
        messageBody.setState(MessageState.STATE_EXCEPTION);
        return messageBody;
    }

    public static MessageBody getAspectLog(String groupId, String unitId) {
        GetAspectLogParams getAspectLogParams = new GetAspectLogParams();
        getAspectLogParams.setGroupId(groupId);
        getAspectLogParams.setUnitId(unitId);

        MessageBody messageBody = new MessageBody();
        messageBody.setGroupId(groupId);
        messageBody.setAction(MessageConstants.ACTION_GET_ASPECT_LOG);
        messageBody.setData(getAspectLogParams);
        return messageBody;
    }

    public static MessageBody deleteAspectLog(String groupId, String unitId) {
        DeleteAspectLogParams deleteAspectLogParams = new DeleteAspectLogParams();
        deleteAspectLogParams.setGroupId(groupId);
        deleteAspectLogParams.setUnitId(unitId);

        MessageBody messageBody = new MessageBody();
        messageBody.setData(deleteAspectLogParams);
        messageBody.setAction(MessageConstants.ACTION_DELETE_ASPECT_LOG);
        messageBody.setGroupId(groupId);
        return messageBody;
    }
}
