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
package work.ready.cloud.transaction.core.corelog.aspect;

import java.io.Serializable;

public class AspectLog implements Serializable {

    private long id;

    public void setId(long id) {
        this.id = id;
    }

    private long groupIdHash;

    public void setGroupIdHash(long groupIdHash) {
        this.groupIdHash = groupIdHash;
    }

    private long unitIdHash;

    public void setUnitIdHash(long unitIdHash) {
        this.unitIdHash = unitIdHash;
    }

    private String unitId;

    public void setUnitId(String unitId) {
        this.unitId = unitId;
    }

    public String getUnitId() {
        return unitId;
    }

    private String groupId;

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupId() {
        return groupId;
    }

    private byte[] bytes;

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public byte[] getBytes() {
        return bytes;
    }

    private String methodStr;

    public void setMethodStr(String methodStr) {
        this.methodStr = methodStr;
    }

    public String getMethodStr() {
        return methodStr;
    }

    private long time;

    public void setTime(long time) {
        this.time = time;
    }

    public long getTime() {
        return time;
    }
}
