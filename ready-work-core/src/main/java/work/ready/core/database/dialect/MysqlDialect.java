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

package work.ready.core.database.dialect;

import work.ready.core.database.Record;
import work.ready.core.database.Table;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class MysqlDialect extends Dialect {

	@Override
	public String forTableBuilderDoBuild(String tableName) {
		return "select * from `" + tableName + "` where 1 = 2";
	}

	@Override
    public String forFindAll(String tableName) {
		return "select * from `" + tableName + "`";
	}

	@Override
	public void forModelSave(Table table, Map<String, Object> attrs, StringBuilder sql, List<Object> params) {
		sql.append("insert into `").append(table.getName()).append("`(");
		StringBuilder temp = new StringBuilder(") values(");
		for (Entry<String, Object> e: attrs.entrySet()) {
			String colName = e.getKey();
			if (table.hasColumnLabel(colName)) {
				if (params.size() > 0) {
					sql.append(", ");
					temp.append(", ");
				}
				sql.append('`').append(colName).append('`');
				temp.append('?');
				params.add(e.getValue());
			}
		}
		sql.append(temp.toString()).append(')');
	}

	@Override
	public String forModelDeleteById(Table table) {
		String[] pKeys = table.getPrimaryKey();
		StringBuilder sql = new StringBuilder(45);
		sql.append("delete from `");
		sql.append(table.getName());
		sql.append("` where ");
		for (int i=0; i<pKeys.length; i++) {
			if (i > 0) {
				sql.append(" and ");
			}
			sql.append('`').append(pKeys[i]).append("` = ?");
		}
		return sql.toString();
	}

	@Override
	public void forModelUpdate(Table table, Map<String, Object> attrs, Set<String> modifyFlag, StringBuilder sql, List<Object> params) {
		sql.append("update `").append(table.getName()).append("` set ");
		String[] pKeys = table.getPrimaryKey();
		for (Entry<String, Object> e : attrs.entrySet()) {
			String colName = e.getKey();
			if (modifyFlag.contains(colName) && !isPrimaryKey(colName, pKeys) && table.hasColumnLabel(colName)) {
				if (params.size() > 0) {
					sql.append(", ");
				}
				sql.append('`').append(colName).append("` = ? ");
				params.add(e.getValue());
			}
		}
		sql.append(" where ");
		for (int i=0; i<pKeys.length; i++) {
			if (i > 0) {
				sql.append(" and ");
			}
			sql.append('`').append(pKeys[i]).append("` = ?");
			params.add(attrs.get(pKeys[i]));
		}
	}

	@Override
	public String forModelFindById(Table table, String columns) {
		StringBuilder sql = new StringBuilder("select ");
		columns = columns.trim();
		if ("*".equals(columns)) {
			sql.append('*');
		}
		else {
			String[] arr = columns.split(",");
			for (int i=0; i<arr.length; i++) {
				if (i > 0) {
					sql.append(',');
				}
				sql.append('`').append(arr[i].trim()).append('`');
			}
		}

		sql.append(" from `");
		sql.append(table.getName());
		sql.append("` where ");
		String[] pKeys = table.getPrimaryKey();
		for (int i=0; i<pKeys.length; i++) {
			if (i > 0) {
				sql.append(" and ");
			}
			sql.append('`').append(pKeys[i]).append("` = ?");
		}
		return sql.toString();
	}

	@Override
	public String forDbFindById(String tableName, String[] pKeys) {
		tableName = tableName.trim();
		trimPrimaryKeys(pKeys);

		StringBuilder sql = new StringBuilder("select * from `").append(tableName).append("` where ");
		for (int i=0; i<pKeys.length; i++) {
			if (i > 0) {
				sql.append(" and ");
			}
			sql.append('`').append(pKeys[i]).append("` = ?");
		}
		return sql.toString();
	}

	@Override
	public String forDbDeleteById(String tableName, String[] pKeys) {
		tableName = tableName.trim();
		trimPrimaryKeys(pKeys);

		StringBuilder sql = new StringBuilder("delete from `").append(tableName).append("` where ");
		for (int i=0; i<pKeys.length; i++) {
			if (i > 0) {
				sql.append(" and ");
			}
			sql.append('`').append(pKeys[i]).append("` = ?");
		}
		return sql.toString();
	}

	@Override
	public void forDbSave(String tableName, String[] pKeys, Record record, StringBuilder sql, List<Object> params) {
		tableName = tableName.trim();
		trimPrimaryKeys(pKeys);

		sql.append("insert into `");
		sql.append(tableName).append("`(");
		StringBuilder temp = new StringBuilder();
		temp.append(") values(");

		for (Entry<String, Object> e: record.getColumns().entrySet()) {
			if (params.size() > 0) {
				sql.append(", ");
				temp.append(", ");
			}
			sql.append('`').append(e.getKey()).append('`');
			temp.append('?');
			params.add(e.getValue());
		}
		sql.append(temp.toString()).append(')');
	}

	@Override
	public void forDbUpdate(String tableName, String[] pKeys, Object[] ids, Record record, StringBuilder sql, List<Object> params) {
		tableName = tableName.trim();
		trimPrimaryKeys(pKeys);

		sql.append("update `").append(tableName).append("` set ");
		for (Entry<String, Object> e: record.getColumns().entrySet()) {
			String colName = e.getKey();
			if (!isPrimaryKey(colName, pKeys)) {
				if (params.size() > 0) {
					sql.append(", ");
				}
				sql.append('`').append(colName).append("` = ? ");
				params.add(e.getValue());
			}
		}
		sql.append(" where ");
		for (int i=0; i<pKeys.length; i++) {
			if (i > 0) {
				sql.append(" and ");
			}
			sql.append('`').append(pKeys[i]).append("` = ?");
			params.add(ids[i]);
		}
	}

	@Override
	public String forPaginate(int pageNumber, int pageSize, StringBuilder findSql) {
		int offset = pageSize * (pageNumber - 1);
		findSql.append(" limit ").append(offset).append(", ").append(pageSize);	
		return findSql.toString();
	}
}
