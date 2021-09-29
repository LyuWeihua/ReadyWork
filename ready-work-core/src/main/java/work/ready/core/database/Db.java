/**
 *
 * Original work Copyright (c) 2011-2019, James Zhan 詹波 (jfinal@126.com).
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

package work.ready.core.database;

import work.ready.core.tools.define.SyncWriteMap;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.Function;

@SuppressWarnings("rawtypes")
public class Db {

	private DatabaseManager manager;
	static final Object[] NULL_PARAM_ARRAY = new Object[0];
	private DbPro MAIN = null;
	private final Map<String, DbPro> map = new SyncWriteMap<>(16, 0.25F);

	Db setManager(DatabaseManager manager){
		this.manager = manager;
		return this;
	}

	void init(String configName) {
		MAIN = new DbPro(manager, manager.getConfig(configName));
		map.put(configName, MAIN);
	}

    synchronized void removeDbProByConfig(String configName) {
    	if (MAIN != null && MAIN.config.getName().equals(configName)) {
    		MAIN = null;
    	}
    	map.remove(configName);
    }

    public synchronized DbPro use(String configName) {
		DbPro result = map.get(configName);
		if (result == null) {
			Config config = manager.getConfig(configName);
			if (config == null) {
				throw new IllegalArgumentException("Data source config '" + configName + "' not found");
			}
			result = new DbPro(manager, config);
			map.put(configName, result);
		}
		return result;
	}

	public DbPro use() {
		return MAIN;
	}

	public Record record() {
    	return MAIN.record();
	}

	public Record record(Map<String, Object> columnMap) { return MAIN.record(columnMap); }

	public DbPro audit(String className, String methodName){
		return MAIN.audit(className, methodName);
	}

	public <T> List<T> query(Config config, Connection conn, String sql, Object... params) throws SQLException {
		return MAIN.query(config, conn, sql, params);
	}

	public <T> List<T> query(String sql, Object... params) {
		return MAIN.query(sql, params);
	}

	public <T> List<T> query(String sql) {
		return MAIN.query(sql);
	}

	public <T> T queryFirst(String sql, Object... params) {
		return MAIN.queryFirst(sql, params);
	}

	public <T> T queryFirst(String sql) {
		return MAIN.queryFirst(sql);
	}

	public <T> T queryColumn(String sql, Object... params) {
		return MAIN.queryColumn(sql, params);
	}

	public <T> T queryColumn(String sql) {
		return MAIN.queryColumn(sql);
	}

	public String queryStr(String sql, Object... params) {
		return MAIN.queryStr(sql, params);
	}

	public String queryStr(String sql) {
		return MAIN.queryStr(sql);
	}

	public Integer queryInt(String sql, Object... params) {
		return MAIN.queryInt(sql, params);
	}

	public Integer queryInt(String sql) {
		return MAIN.queryInt(sql);
	}

	public Long queryLong(String sql, Object... params) {
		return MAIN.queryLong(sql, params);
	}

	public Long queryLong(String sql) {
		return MAIN.queryLong(sql);
	}

	public Double queryDouble(String sql, Object... params) {
		return MAIN.queryDouble(sql, params);
	}

	public Double queryDouble(String sql) {
		return MAIN.queryDouble(sql);
	}

	public Float queryFloat(String sql, Object... params) {
		return MAIN.queryFloat(sql, params);
	}

	public Float queryFloat(String sql) {
		return MAIN.queryFloat(sql);
	}

	public java.math.BigDecimal queryBigDecimal(String sql, Object... params) {
		return MAIN.queryBigDecimal(sql, params);
	}

	public java.math.BigDecimal queryBigDecimal(String sql) {
		return MAIN.queryBigDecimal(sql);
	}

	public java.math.BigInteger queryBigInteger(String sql, Object... params) {
		return MAIN.queryBigInteger(sql, params);
	}

	public java.math.BigInteger queryBigInteger(String sql) {
		return MAIN.queryBigInteger(sql);
	}

	public byte[] queryBytes(String sql, Object... params) {
		return MAIN.queryBytes(sql, params);
	}

	public byte[] queryBytes(String sql) {
		return MAIN.queryBytes(sql);
	}

	public java.util.Date queryDate(String sql, Object... params) {
		return MAIN.queryDate(sql, params);
	}

	public java.util.Date queryDate(String sql) {
		return MAIN.queryDate(sql);
	}

	public LocalDateTime queryLocalDateTime(String sql, Object... params) {
		return MAIN.queryLocalDateTime(sql, params);
	}

	public LocalDateTime queryLocalDateTime(String sql) {
		return MAIN.queryLocalDateTime(sql);
	}

	public java.sql.Time queryTime(String sql, Object... params) {
		return MAIN.queryTime(sql, params);
	}

	public java.sql.Time queryTime(String sql) {
		return MAIN.queryTime(sql);
	}

	public java.sql.Timestamp queryTimestamp(String sql, Object... params) {
		return MAIN.queryTimestamp(sql, params);
	}

	public java.sql.Timestamp queryTimestamp(String sql) {
		return MAIN.queryTimestamp(sql);
	}

	public Boolean queryBoolean(String sql, Object... params) {
		return MAIN.queryBoolean(sql, params);
	}

	public Boolean queryBoolean(String sql) {
		return MAIN.queryBoolean(sql);
	}

	public Short queryShort(String sql, Object... params) {
		return MAIN.queryShort(sql, params);
	}

	public Short queryShort(String sql) {
		return MAIN.queryShort(sql);
	}

	public Byte queryByte(String sql, Object... params) {
		return MAIN.queryByte(sql, params);
	}

	public Byte queryByte(String sql) {
		return MAIN.queryByte(sql);
	}

	public Number queryNumber(String sql, Object... params) {
		return MAIN.queryNumber(sql, params);
	}

	public Number queryNumber(String sql) {
		return MAIN.queryNumber(sql);
	}

	int update(Config config, Connection conn, String sql, Object... params) throws SQLException {
		return MAIN.update(config, conn, sql, params);
	}

	public int update(String sql, Object... params) {
		return MAIN.update(sql, params);
	}

	public int update(String sql) {
		return MAIN.update(sql);
	}

	List<Record> find(Config config, Connection conn, String sql, Object... params) throws SQLException {
		return MAIN.find(config, conn, sql, params);
	}

	public List<Record> find(String sql, Object... params) {
		return MAIN.find(sql, params);
	}

	public Record findTop1(String findSql, Object... params){ return MAIN.findTop1(findSql, params); }

	public List<Record> findTop(int size, String findSql, Object... params){ return MAIN.findTop(size, findSql, params); }

	public List<Record> find(String sql) {
		return MAIN.find(sql);
	}

	public List<Record> findAll(String tableName) {
		return MAIN.findAll(tableName);
	}

	public Record findFirst(String sql, Object... params) {
		return MAIN.findFirst(sql, params);
	}

	public Record findFirst(String sql) {
		return MAIN.findFirst(sql);
	}

	public Record findById(String tableName, Object idValue) {
		return MAIN.findById(tableName, idValue);
	}

	public Record findById(String tableName, String primaryKey, Object idValue) {
		return MAIN.findById(tableName, primaryKey, idValue);
	}

	public Record findByIds(String tableName, String primaryKey, Object... idValues) {
		return MAIN.findByIds(tableName, primaryKey, idValues);
	}

	public boolean deleteById(String tableName, Object idValue) {
		return MAIN.deleteById(tableName, idValue);
	}

	public boolean deleteById(String tableName, String primaryKey, Object idValue) {
		return MAIN.deleteById(tableName, primaryKey, idValue);
	}

	public boolean deleteByIds(String tableName, String primaryKey, Object... idValues) {
		return MAIN.deleteByIds(tableName, primaryKey, idValues);
	}

	public boolean delete(String tableName, String primaryKey, Record record) {
		return MAIN.delete(tableName, primaryKey, record);
	}

	public boolean delete(String tableName, Record record) {
		return MAIN.delete(tableName, record);
	}

	public int delete(String sql, Object... params) {
		return MAIN.delete(sql, params);
	}

	public int delete(String sql) {
		return MAIN.delete(sql);
	}

	Page<Record> paginate(Config config, Connection conn, int pageNumber, int pageSize, String select, String sqlExceptSelect, Object... params) throws SQLException {
		return MAIN.paginate(config, conn, pageNumber, pageSize, select, sqlExceptSelect, params);
	}

	public Page<Record> paginate(int pageNumber, int pageSize, String select, String sqlExceptSelect, Object... params) {
		return MAIN.paginate(pageNumber, pageSize, select, sqlExceptSelect, params);
	}

	public Page<Record> paginate(int pageNumber, int pageSize, boolean isGroupBySql, String select, String sqlExceptSelect, Object... params) {
		return MAIN.paginate(pageNumber, pageSize, isGroupBySql, select, sqlExceptSelect, params);
	}

	public Page<Record> paginate(int pageNumber, int pageSize, String select, String sqlExceptSelect) {
		return MAIN.paginate(pageNumber, pageSize, select, sqlExceptSelect);
	}

	public Page<Record> paginateByFullSql(int pageNumber, int pageSize, String totalRowSql, String findSql, Object... params) {
		return MAIN.paginateByFullSql(pageNumber, pageSize, totalRowSql, findSql, params);
	}

	public Page<Record> paginateByFullSql(int pageNumber, int pageSize, boolean isGroupBySql, String totalRowSql, String findSql, Object... params) {
		return MAIN.paginateByFullSql(pageNumber, pageSize, isGroupBySql, totalRowSql, findSql, params);
	}

	boolean save(Config config, Connection conn, String tableName, String primaryKey, Record record) throws SQLException {
		return MAIN.save(config, conn, tableName, primaryKey, record);
	}

	public boolean save(String tableName, String primaryKey, Record record) {
		return MAIN.save(tableName, primaryKey, record);
	}

	public boolean save(String tableName, Record record) {
		return MAIN.save(tableName, record);
	}

	boolean update(Config config, Connection conn, String tableName, String primaryKey, Record record) throws SQLException {
		return MAIN.update(config, conn, tableName, primaryKey, record);
	}

	public boolean update(String tableName, String primaryKey, Record record) {
		return MAIN.update(tableName, primaryKey, record);
	}

	public boolean update(String tableName, Record record) {
		return MAIN.update(tableName, record);
	}

	public Object execute(DbCallback callback) {
		return MAIN.execute(callback);
	}

	Object execute(Config config, DbCallback callback) {
		return MAIN.execute(config, callback);
	}

	public boolean transaction(Atom atom) {
		return MAIN.transaction(atom);
	}

	public Future<Boolean> asyncTransaction(Atom atom) {
		return MAIN.asyncTransaction(atom);
	}

	public List<Record> findByCache(String cacheName, Object key, String sql, Object... params) {
		return MAIN.findByCache(cacheName, key, sql, params);
	}

	public List<Record> findByCache(String cacheName, Object key, String sql) {
		return MAIN.findByCache(cacheName, key, sql);
	}

	public Record findFirstByCache(String cacheName, Object key, String sql, Object... params) {
		return MAIN.findFirstByCache(cacheName, key, sql, params);
	}

	public Record findFirstByCache(String cacheName, Object key, String sql) {
		return MAIN.findFirstByCache(cacheName, key, sql);
	}

	public Page<Record> paginateByCache(String cacheName, Object key, int pageNumber, int pageSize, String select, String sqlExceptSelect, Object... params) {
		return MAIN.paginateByCache(cacheName, key, pageNumber, pageSize, select, sqlExceptSelect, params);
	}

	public Page<Record> paginateByCache(String cacheName, Object key, int pageNumber, int pageSize, boolean isGroupBySql, String select, String sqlExceptSelect, Object... params) {
		return MAIN.paginateByCache(cacheName, key, pageNumber, pageSize, isGroupBySql, select, sqlExceptSelect, params);
	}

	public Page<Record> paginateByCache(String cacheName, Object key, int pageNumber, int pageSize, String select, String sqlExceptSelect) {
		return MAIN.paginateByCache(cacheName, key, pageNumber, pageSize, select, sqlExceptSelect);
	}

    public int[] batch(String sql, Object[][] params, int batchSize) {
    	return MAIN.batch(sql, params, batchSize);
    }

	public int[] batch(String sql, String columns, List modelOrRecordList, int batchSize) {
		if (modelOrRecordList == null || modelOrRecordList.size() == 0) return new int[0];
		if(Model.class.isAssignableFrom(modelOrRecordList.get(0).getClass())) {
			String config = manager.getConfig((Class<? extends Model>) modelOrRecordList.get(0).getClass()).getName();
			return use(config).batch(sql, columns, modelOrRecordList, batchSize);
		} else {
			return MAIN.batch(sql, columns, modelOrRecordList, batchSize);
		}
	}

    public int[] batch(List<String> sqlList, int batchSize) {
    	return MAIN.batch(sqlList, batchSize);
    }

    public int[] batchSave(List<? extends Model> modelList, int batchSize) {
		if (modelList == null || modelList.size() == 0) return new int[0];
		String config = manager.getConfig(modelList.get(0).getClass()).getName();
    	return use(config).batchSave(modelList, manager.tableManager.getTable(modelList.get(0).getClass()), batchSize);
    }

    public int[] batchSave(String tableName, List<Record> recordList, int batchSize) {
    	return MAIN.batchSave(tableName, recordList, batchSize);
    }

    public int[] batchUpdate(List<? extends Model> modelList, int batchSize) {
		if (modelList == null || modelList.size() == 0) return new int[0];
		String config = manager.getConfig(modelList.get(0).getClass()).getName();
    	return use(config).batchUpdate(modelList, manager.tableManager.getTable(modelList.get(0).getClass()), batchSize);
    }

    public int[] batchUpdate(String tableName, String primaryKey, List<Record> recordList, int batchSize) {
    	return MAIN.batchUpdate(tableName, primaryKey, recordList, batchSize);
    }

    public int[] batchUpdate(String tableName, List<Record> recordList, int batchSize) {
    	return MAIN.batchUpdate(tableName, recordList, batchSize);
    }

    public String getSql(String key) {
    	return MAIN.getSql(key);
    }

    public SqlParam getSqlParam(String key, Record record) {
    	return MAIN.getSqlParam(key, record);
    }

    public SqlParam getSqlParam(String key, Model model) {
		String config = manager.getConfig(model.getClass()).getName();
    	return use(config).getSqlParam(key, model);
    }

    public SqlParam getSqlParam(String key, Map data) {
    	return MAIN.getSqlParam(key, data);
    }

    public SqlParam getSqlParam(String key, Object... params) {
    	return MAIN.getSqlParam(key, params);
    }

	public SqlParam getSqlParamByString(String content, Map data) {
		return MAIN.getSqlParamByString(content, data);
	}

	public SqlParam getSqlParamByString(String content, Object... params) {
		return MAIN.getSqlParamByString(content, params);
	}

    public List<Record> find(SqlParam sqlParam) {
    	return MAIN.find(sqlParam);
    }

    public Record findFirst(SqlParam sqlParam) {
    	return MAIN.findFirst(sqlParam);
    }

    public int update(SqlParam sqlParam) {
    	return MAIN.update(sqlParam);
    }

    public Page<Record> paginate(int pageNumber, int pageSize, SqlParam sqlParam) {
    	return MAIN.paginate(pageNumber, pageSize, sqlParam);
    }

	public Page<Record> paginate(int pageNumber, int pageSize, boolean isGroupBySql, SqlParam sqlParam) {
		return MAIN.paginate(pageNumber, pageSize, isGroupBySql, sqlParam);
	}

	public void each(Function<Record, Boolean> func, String sql, Object... params) {
		MAIN.each(func, sql, params);
	}

	public DbTemplate template(String key, Map data) {
		return MAIN.template(key, data);
	}

	public DbTemplate template(String key, Object... params) {
		return MAIN.template(key, params);
	}

	public DbTemplate templateByString(String content, Map data) {
		return MAIN.templateByString(content, data);
	}

	public DbTemplate templateByString(String content, Object... params) {
		return MAIN.templateByString(content, params);
	}

	public List<String> getAllTables(boolean withSchema) {
		return MAIN.getAllTables(withSchema);
	}

	public List<String> getAllTables(Connection conn, boolean withSchema) {
		return MAIN.getAllTables(conn, withSchema);
	}

	public boolean tableExists(String tableName) {
		return MAIN.tableExists(tableName);
	}

	public boolean tableExists(Connection conn, String tableName) {
		return MAIN.tableExists(conn, tableName);
	}
}

