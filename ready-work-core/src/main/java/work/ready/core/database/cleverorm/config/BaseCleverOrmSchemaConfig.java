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

package work.ready.core.database.cleverorm.config;

import work.ready.core.database.Bean;
import work.ready.core.database.Model;

@SuppressWarnings("serial")
public abstract class BaseCleverOrmSchemaConfig<M extends BaseCleverOrmSchemaConfig<M>> extends Model<M> implements Bean {

	public void setId(Long id) {
		set(Column.id.field, id);
	}

	public Long getId() {
		return getLong(Column.id.field);
	}

	public void setCode(String code) {
		set(Column.code.field, code);
	}

	public String getCode() {
		return getStr(Column.code.field);
	}

	public void setSystemId(Long systemId) {
		set(Column.systemId.field, systemId);
	}

	public Long getSystemId() {
		return getLong(Column.systemId.field);
	}

	public void setClassName(String className) {
		set(Column.className.field, className);
	}

	public String getClassName() {
		return getStr(Column.className.field);
	}

	public void setMethodName(String methodName) {
		set(Column.methodName.field, methodName);
	}

	public String getMethodName() {
		return getStr(Column.methodName.field);
	}

	public void setTag(String tag) {
		set(Column.tag.field, tag);
	}

	public String getTag() {
		return getStr(Column.tag.field);
	}

	public void setDataSource(String dataSource) {
		set(Column.dataSource.field, dataSource);
	}

	public String getDataSource() {
		return getStr(Column.dataSource.field);
	}

	public void setSql(String sql) {
		set(Column.sql.field, sql);
	}

	public String getSql() {
		return getStr(Column.sql.field);
	}

	public void setDescription(String description) {
		set(Column.description.field, description);
	}

	public String getDescription() {
		return getStr(Column.description.field);
	}

	public void setIsSystem(Boolean isSystem) {
		set(Column.isSystem.field, isSystem);
	}

	public Boolean getIsSystem() {
		return get(Column.isSystem.field);
	}

	public void setSortId(Integer sortId) {
		set(Column.sortId.field, sortId);
	}

	public Integer getSortId() {
		return getInt(Column.sortId.field);
	}

	public void setStatus(Integer status) {
		set(Column.status.field, status);
	}

	public Integer getStatus() {
		return getInt(Column.status.field);
	}

	public void setCreateUserId(Long createUserId) {
		set(Column.createUserId.field, createUserId);
	}

	public Long getCreateUserId() {
		return getLong(Column.createUserId.field);
	}

	public void setCreatedTime(java.util.Date createdTime) {
		set(Column.createdTime.field, createdTime);
	}

	public java.util.Date getCreatedTime() {
		return get(Column.createdTime.field);
	}

	public void setModifyUserId(Long modifyUserId) {
		set(Column.modifyUserId.field, modifyUserId);
	}

	public Long getModifyUserId() {
		return getLong(Column.modifyUserId.field);
	}

	public void setModifiedTime(java.util.Date modifiedTime) {
		set(Column.modifiedTime.field, modifiedTime);
	}

	public java.util.Date getModifiedTime() {
		return get(Column.modifiedTime.field);
	}

	public void setVersion(Long version) {
		set(Column.version.field, version);
	}

	public Long getVersion() {
		return getLong(Column.version.field);
	}

	public enum Column implements Model.Column {
		    id("ID"),
		    code("CODE"),
		    systemId("SYSTEM_ID"),
		    className("CLASS_NAME"),
		    methodName("METHOD_NAME"),
		    tag("TAG"),
		    dataSource("DATA_SOURCE"),
		    sql("SQL"),
		    description("DESCRIPTION"),
		    isSystem("IS_SYSTEM"),
		    sortId("SORT_ID"),
		    status("STATUS"),
		    createUserId("CREATE_USER_ID"),
		    createdTime("CREATED_TIME"),
		    modifyUserId("MODIFY_USER_ID"),
		    modifiedTime("MODIFIED_TIME"),
		    version("VERSION");

		private final String field;
        public String get(){ return field; }
		Column(String field) {
			this.field = field;
		}
	}
}
