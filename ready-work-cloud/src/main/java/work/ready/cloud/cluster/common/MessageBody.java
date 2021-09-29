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

public class MessageBody implements Serializable {

    public MessageBody(String action, String groupId, Serializable data, int state) {
        this.action = action;
        this.groupId = groupId;
        this.data = data;
        this.state = state;
    }

    public MessageBody() {

    }

    private String action;

    public String getAction() {
        return action;
    }

    public MessageBody setAction(String action) {
        this.action = action;
        return this;
    }

    private String groupId;

    public String getGroupId() {
        return groupId;
    }

    public MessageBody setGroupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    private Serializable data;

    public Serializable getData() {
        return data;
    }

    public MessageBody setData(Serializable data) {
        this.data = data;
        return this;
    }

    private int state = MessageState.STATE_REQUEST;

    public int getState() {
        return state;
    }

    public MessageBody setState(int state) {
        this.state = state;
        return this;
    }

    public boolean isStateOk() {
        return MessageState.STATE_OK == state;
    }

    public <T> T loadBean(Class<T> tClass){
        return (T)data;
    }

    @Override
    public String toString() {
        return "{" +
                "action='" + action + '\'' +
                ", groupId='" + groupId + '\'' +
                ", data=" + data +
                ", state=" + state +
                '}';
    }
}
