/**
 * Copyright (c) 2011-2019, James Zhan 詹波 (jfinal@126.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package work.ready.core.database;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SqlParam implements Serializable {

	private static final long serialVersionUID = -8586448059592782381L;

	String sql;
	List<Object> paramList;

	public SqlParam setSql(String sql) {
		this.sql = sql;
		return this;
	}

	public SqlParam addParam(Object param) {
		if (paramList == null) {
			paramList = new ArrayList<Object>();
		}
		paramList.add(param);
		return this;
	}

	public String getSql() {
		return sql;
	}

	public Object[] getParam() {
		if (paramList == null || paramList.size() == 0) {
			return Db.NULL_PARAM_ARRAY;
		} else {
			return paramList.toArray(new Object[paramList.size()]);
		}
	}

	public SqlParam clear() {
		sql = null;
		if (paramList != null) {
			paramList.clear();
		}
		return this;
	}

	public String toString() {
		return "Sql: " + sql + "\nParam: " + paramList;
	}
}
