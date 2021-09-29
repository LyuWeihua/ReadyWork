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

import work.ready.core.database.*;
import work.ready.core.database.Record;
import work.ready.core.database.builder.KeepByteAndShortModelBuilder;
import work.ready.core.database.builder.KeepByteAndShortRecordBuilder;
import work.ready.core.tools.ClassUtil;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

public abstract class Dialect {

	protected boolean keepByteAndShort = false;
	protected ModelBuilder modelBuilder = null;
	protected RecordBuilder recordBuilder = null;

	public abstract String forTableBuilderDoBuild(String tableName);
	public abstract String forPaginate(int pageNumber, int pageSize, StringBuilder findSql);

	public abstract String forModelFindById(Table table, String columns);
	public abstract String forModelDeleteById(Table table);
	public abstract void forModelSave(Table table, Map<String, Object> attrs, StringBuilder sql, List<Object> params);
	public abstract void forModelUpdate(Table table, Map<String, Object> attrs, Set<String> modifyFlag, StringBuilder sql, List<Object> params);

	public abstract String forDbFindById(String tableName, String[] pKeys);
	public abstract String forDbDeleteById(String tableName, String[] pKeys);
	public abstract void forDbSave(String tableName, String[] pKeys, Record record, StringBuilder sql, List<Object> params);
	public abstract void forDbUpdate(String tableName, String[] pKeys, Object[] ids, Record record, StringBuilder sql, List<Object> params);

	public String forFindAll(String tableName) {
		return "select * from " + tableName;
	}

	public Dialect(){
		modelBuilder = new ModelBuilder();
		recordBuilder = new RecordBuilder();
		recordBuilder.setModelBuilder(modelBuilder);
	}

	public Dialect setKeepByteAndShort(boolean keepByteAndShort) {
		this.keepByteAndShort = keepByteAndShort;

		if (keepByteAndShort) {
			if (ClassUtil.getUserClass(modelBuilder.getClass()) == ModelBuilder.class) {
				modelBuilder = new KeepByteAndShortModelBuilder();
			}
			if (ClassUtil.getUserClass(recordBuilder.getClass()) == RecordBuilder.class) {
				recordBuilder = new KeepByteAndShortRecordBuilder();
			}
		} else {
			if (ClassUtil.getUserClass(modelBuilder.getClass()) == KeepByteAndShortModelBuilder.class) {
				modelBuilder = new ModelBuilder();
			}
			if (ClassUtil.getUserClass(recordBuilder.getClass()) == KeepByteAndShortRecordBuilder.class) {
				recordBuilder = new RecordBuilder();
			}
		}
		return this;
	}

	public boolean isKeepByteAndShort() {
		return keepByteAndShort;
	}

	public Dialect setModelBuilder(ModelBuilder modelBuilder) {
		this.modelBuilder = modelBuilder;
		return this;
	}

	public Dialect setRecordBuilder(RecordBuilder recordBuilder) {
		this.recordBuilder = recordBuilder;
		return this;
	}

	@SuppressWarnings("rawtypes")
	public <T> List<T> buildModelList(ResultSet rs, Class<? extends Model> modelClass) throws SQLException, ReflectiveOperationException {
		return modelBuilder.build(rs, modelClass);
	}

	@SuppressWarnings("rawtypes")
	public <T> void eachModel(ResultSet rs, Class<? extends Model> modelClass, Function<T, Boolean> func) throws SQLException, ReflectiveOperationException {
		modelBuilder.build(rs, modelClass, func);
	}

	public List<Record> buildRecordList(Config config, ResultSet rs) throws SQLException {
		return recordBuilder.build(config, rs);
	}

	public void eachRecord(Config config, ResultSet rs, Function<Record, Boolean> func) throws SQLException {
		recordBuilder.build(config, rs, func);
	}

	public void getModelGeneratedKey(Model<?> model, PreparedStatement pst, Table table) throws SQLException {
		String[] pKeys = table.getPrimaryKey();
		ResultSet rs = pst.getGeneratedKeys();
		for (String pKey : pKeys) {
			if (model.get(pKey) == null || isOracle()) {
				if (rs.next()) {
					Class<?> colType = table.getColumnType(pKey);
					if (colType != null) {	
						if (colType == Integer.class || colType == int.class) {
							model.set(pKey, rs.getInt(1));
						} else if (colType == Long.class || colType == long.class) {
							model.set(pKey, rs.getLong(1));
						} else if (colType == BigInteger.class) {
							processGeneratedBigIntegerKey(model, pKey, rs.getObject(1));
						} else {
							model.set(pKey, rs.getObject(1));	
						}
					}
				}
			}
		}
		rs.close();
	}

	protected void processGeneratedBigIntegerKey(Model<?> model, String pKey, Object v) {
		if (v instanceof BigInteger) {
			model.set(pKey, (BigInteger)v);
		} else if (v instanceof Number) {
			Number n = (Number)v;
			model.set(pKey, BigInteger.valueOf(n.longValue()));
		} else {
			model.set(pKey, v);
		}
	}

	public void getRecordGeneratedKey(PreparedStatement pst, Record record, String[] pKeys) throws SQLException {
		ResultSet rs = pst.getGeneratedKeys();
		for (String pKey : pKeys) {
			if (record.get(pKey) == null || isOracle()) {
				if (rs.next()) {
					record.set(pKey, rs.getObject(1));	
				}
			}
		}
		rs.close();
	}

	public boolean isOracle() {
		return false;
	}

	public boolean isTakeOverDbPaginate() {
		return false;
	}

	public Page<Record> takeOverDbPaginate(DbPro dbPro, Config config, Connection conn, int pageNumber, int pageSize, Boolean isGroupBySql, String totalRowSql, StringBuilder findSql, Object... params) throws SQLException {
		throw new RuntimeException("You should implements this method in " + getClass().getName());
	}

	public boolean isTakeOverModelPaginate() {
		return false;
	}

	@SuppressWarnings("rawtypes")
	public Page takeOverModelPaginate(DbPro dbPro, Config config, Connection conn, Class<? extends Model> modelClass, int pageNumber, int pageSize, Boolean isGroupBySql, String totalRowSql, StringBuilder findSql, Object... params) throws Exception {
		throw new RuntimeException("You should implements this method in " + getClass().getName());
	}

	public void fillStatement(PreparedStatement pst, List<Object> params) throws SQLException {
		for (int i=0, size=params.size(); i<size; i++) {
			pst.setObject(i + 1, params.get(i));
		}
	}

	public void fillStatement(PreparedStatement pst, Object... params) throws SQLException {
		for (int i=0; i<params.length; i++) {
			pst.setObject(i + 1, params[i]);
		}
	}

	public String getDefaultPrimaryKey() {
		return "id";
	}

	public boolean isPrimaryKey(String colName, String[] pKeys) {
		for (String pKey : pKeys) {
			if (colName.equalsIgnoreCase(pKey)) {
				return true;
			}
		}
		return false;
	}

	public void trimPrimaryKeys(String[] pKeys) {
		for (int i=0; i<pKeys.length; i++) {
			pKeys[i] = pKeys[i].trim();
		}
	}

	protected static class Holder {

		private static final Pattern ORDER_BY_PATTERN = Pattern.compile(
			"order\\s+by\\s+[^,\\s]+(\\s+asc|\\s+desc)?(\\s*,\\s*[^,\\s]+(\\s+asc|\\s+desc)?)*",
			Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
	}

	public String replaceOrderBy(String sql) {
		return Holder.ORDER_BY_PATTERN.matcher(sql).replaceAll("");
	}

	protected void fillStatementHandleDateType(PreparedStatement pst, List<Object> params) throws SQLException {
		for (int i=0, size=params.size(); i<size; i++) {
			Object value = params.get(i);
			if (value instanceof java.util.Date) {
				if (value instanceof java.sql.Date) {
					pst.setDate(i + 1, (java.sql.Date)value);
				} else if (value instanceof java.sql.Timestamp) {
					pst.setTimestamp(i + 1, (java.sql.Timestamp)value);
				} else {

					java.util.Date d = (java.util.Date)value;
					pst.setTimestamp(i + 1, new java.sql.Timestamp(d.getTime()));
				}
			} else {
				pst.setObject(i + 1, value);
			}
		}
	}

	protected void fillStatementHandleDateType(PreparedStatement pst, Object... params) throws SQLException {
		for (int i=0; i<params.length; i++) {
			Object value = params[i];
			if (value instanceof java.util.Date) {
				if (value instanceof java.sql.Date) {
					pst.setDate(i + 1, (java.sql.Date)value);
				} else if (value instanceof java.sql.Timestamp) {
					pst.setTimestamp(i + 1, (java.sql.Timestamp)value);
				} else {

					java.util.Date d = (java.util.Date)value;
					pst.setTimestamp(i + 1, new java.sql.Timestamp(d.getTime()));
				}
			} else {
				pst.setObject(i + 1, value);
			}
		}
	}

	public String forPaginateTotalRow(String select, String sqlExceptSelect, Object ext) {
		return "select count(*) " + replaceOrderBy(sqlExceptSelect);
	}
}

