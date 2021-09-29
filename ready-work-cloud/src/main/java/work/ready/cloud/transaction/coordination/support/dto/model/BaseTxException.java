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

package work.ready.cloud.transaction.coordination.support.dto.model;

import work.ready.cloud.transaction.common.message.params.TxExceptionParams;
import work.ready.core.database.Bean;
import work.ready.core.database.Model;

import java.util.Date;

public abstract class BaseTxException<M extends BaseTxException<M>> extends Model<M> implements Bean {

    public Long getId() {
        return getLong(Column.id.field);
    }

    public void setId(Long id) {
        set(Column.id.field, id);
    }

    public String getGroupId() {
        return getStr(Column.groupId.field);
    }

    public void setGroupId(String groupId) {
        set(Column.groupId.field, groupId);
    }

    public String getUnitId() {
        return getStr(Column.unitId.field);
    }

    public void setUnitId(String unitId) {
        set(Column.unitId.field, unitId);
    }

    public String getNodeName() {
        return getStr(Column.nodeName.field);
    }

    public void setNodeName(String nodeName) {
        set(Column.nodeName.field, nodeName);
    }

    public Integer getTransactionState() {
        return getInt(Column.transactionState.field);
    }

    public void setTransactionState(Integer transactionState) {
        set(Column.transactionState.field, transactionState);
    }

    public short getRegistrar() {
        return getShort(Column.registrar.field);
    }

    public void setRegistrar(short registrar) {
        set(Column.registrar.field, registrar);
    }

    public short getExState() {
        return getShort(Column.exState.field);
    }

    public void setExState(short exState) {
        set(Column.exState.field, exState);
    }

    public Date getCreatedTime() {
        return getDate(Column.createdTime.field);
    }

    public void setCreatedTime(Date createdTime) {
        set(Column.createdTime.field, createdTime);
    }

    public String getRemark() {
        return getStr(Column.remark.field);
    }

    public void setRemark(String remark) {
        set(Column.remark.field, remark);
    }

    public enum Column implements Model.Column {
        
        id("ID"),
        
        groupId("GROUP_ID"),
        
        unitId("UNIT_ID"),
        
        nodeName("NODE_NAME"),
        
        transactionState("TRANSACTION_STATE"),
        
        registrar("REGISTRAR"),
        
        exState("EX_STATE"),
        
        createdTime("CREATED_TIME"),
        
        remark("REMARK");

        private final String field;
        public String get(){ return field; }
        Column(String field) {
            this.field = field;
        }
    }
}
