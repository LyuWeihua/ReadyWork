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
package work.ready.cloud.transaction.coordination.support.dto;

import work.ready.cloud.cluster.common.MessageBody;

import java.util.Date;

public class ExceptionInfo {

    private long id;

    private String groupId;

    private String unitId;

    private String nodeName;

    private short registrar;

    private short exState;

    private Date createTime;

    private String remark;

    private MessageBody transactionInfo;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

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

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public short getRegistrar() {
        return registrar;
    }

    public void setRegistrar(short registrar) {
        this.registrar = registrar;
    }

    public short getExState() {
        return exState;
    }

    public void setExState(short exState) {
        this.exState = exState;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public MessageBody getTransactionInfo() {
        return transactionInfo;
    }

    public void setTransactionInfo(MessageBody transactionInfo) {
        this.transactionInfo = transactionInfo;
    }
}
