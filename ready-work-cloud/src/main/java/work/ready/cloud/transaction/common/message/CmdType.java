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

import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

public enum CmdType {

    notifyUnit("notify-unit", MessageConstants.ACTION_NOTIFY_UNIT),

    createGroup("create-group", MessageConstants.ACTION_CREATE_GROUP),

    joinGroup("join-group", MessageConstants.ACTION_JOIN_GROUP),

    notifyGroup("notify-group", MessageConstants.ACTION_NOTIFY_GROUP),

    acquireDtxLock("acquire-dtx-lock", MessageConstants.ACTION_ACQUIRE_DTX_LOCK),

    releaseDtxLock("release-dtx-lock", MessageConstants.ACTION_RELEASE_DTX_LOCK),

    askTransactionState("ask-transaction-state", MessageConstants.ACTION_ASK_TRANSACTION_STATE),

    writeCompensation("write-exception", MessageConstants.ACTION_WRITE_EXCEPTION),

    getAspectLog("get-aspect-log", MessageConstants.ACTION_GET_ASPECT_LOG),

    deleteAspectLog("delete-aspect-log", MessageConstants.ACTION_DELETE_ASPECT_LOG);

    private static final Log logger = LogFactory.getLog(CmdType.class);

    private String code;

    private String name;

    CmdType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static CmdType parserCmd(String cmd) {
        logger.debug("parsed cmd: %s", cmd);
        switch (cmd) {
            case MessageConstants.ACTION_CREATE_GROUP:
                return createGroup;
            case MessageConstants.ACTION_NOTIFY_GROUP:
                return notifyGroup;
            case MessageConstants.ACTION_NOTIFY_UNIT:
                return notifyUnit;
            case MessageConstants.ACTION_JOIN_GROUP:
                return joinGroup;
            case MessageConstants.ACTION_ACQUIRE_DTX_LOCK:
                return acquireDtxLock;
            case MessageConstants.ACTION_RELEASE_DTX_LOCK:
                return releaseDtxLock;
            case MessageConstants.ACTION_ASK_TRANSACTION_STATE:
                return askTransactionState;
            case MessageConstants.ACTION_WRITE_EXCEPTION:
                return writeCompensation;
            case MessageConstants.ACTION_GET_ASPECT_LOG:
                return getAspectLog;
            case MessageConstants.ACTION_DELETE_ASPECT_LOG:
                return deleteAspectLog;
            default:
                throw new IllegalStateException("unsupported cmd.");
        }
    }
}
