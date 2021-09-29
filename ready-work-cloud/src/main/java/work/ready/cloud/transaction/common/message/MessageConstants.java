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

public class MessageConstants {

    public static final String ACTION_CREATE_GROUP = "createGroup";

    public static final String ACTION_JOIN_GROUP = "joinGroup";

    public static final String ACTION_NOTIFY_GROUP = "notifyGroup";

    public static final String ACTION_NOTIFY_UNIT = "notifyUnit";

    public static final String ACTION_ASK_TRANSACTION_STATE = "askTxState";

    public static final String ACTION_WRITE_EXCEPTION = "writeException";

    public static final String ACTION_GET_ASPECT_LOG = "getAspectLog";

    public static final String ACTION_DELETE_ASPECT_LOG = "delAspectLog";

    public static final String ACTION_ACQUIRE_DTX_LOCK = "acquireLock";

    public static final String ACTION_RELEASE_DTX_LOCK = "releaseLock";

}
