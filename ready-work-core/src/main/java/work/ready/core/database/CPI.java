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

import java.util.Map;
import java.util.Set;

@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class CPI {

	public static final Map<String, Object> getAttrs(Model model) {
		return model._getAttrs();
	}

	public static final Set<String> getModifyFlag(Model model) {
		return model._getModifyFlag();
	}

	public static final Table getTable(Model model) {
		return model._getTable();
	}

	public static final Config getConfig(Model model) {
		return model._getConfig();
	}

	public static final Class<? extends Model> getUsefulClass(Model model) {
		return model._getUserClass();
	}

	public static void setColumnsMap(Record record, Map<String, Object> columns) {
		record.setColumnsMap(columns);
	}

	public static void setTablePrimaryKey(Table table, String primaryKey) {
		table.setPrimaryKey(primaryKey);
	}
}

