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
package work.ready.cloud.transaction.common.message.params;

import java.io.Serializable;

public class TxExceptionParams implements Serializable {

    public static final short UNKNOWN_ERROR = -1;

    public static final short NOTIFY_UNIT_ERROR = 0;

    public static final short ASK_ERROR = 1;

    public static final short NOTIFY_GROUP_ERROR = 2;

    private String groupId;

    private String unitId;

    private Short registrar;

    private Integer transactionState;

    private String remark;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getUnitId() {
        return unitId;
    }

    public void setUnitId(String unitId) {
        this.unitId = unitId;
    }

    public Short getRegistrar() {
        return registrar;
    }

    public void setRegistrar(Short registrar) {
        this.registrar = registrar;
    }

    public Integer getTransactionState() {
        return transactionState;
    }

    public void setTransactionState(Integer transactionState) {
        this.transactionState = transactionState;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
