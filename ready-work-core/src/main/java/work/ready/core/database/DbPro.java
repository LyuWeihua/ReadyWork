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

import work.ready.core.component.cache.Cache;
import work.ready.core.database.transaction.LocalTransactionManager;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.tools.DateUtil;
import work.ready.core.tools.StrUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.function.Function;

import static work.ready.core.database.Db.NULL_PARAM_ARRAY;

@SuppressWarnings({"rawtypes", "unchecked"})
public class DbPro {

	private static final Log logger = LogFactory.getLog(DbPro.class);
	protected final Config config;
	protected final DatabaseManager manager;

	public DbPro(DatabaseManager manager, Config config) {
		this.config = config;
		this.manager = manager;
	}

	public Config getConfig() {
		return config;
	}

	public DbPro audit(String className, String methodName){
		manager.getAuditManager().audit(className, methodName);
		return this;
	}

	public DbPro skipAudit(){
		manager.getAuditManager().skipAudit();
		return this;
	}

	public Record record(){
		return new Record(config.containerFactory.getColumnsMap());
	}

	public Record record(Map<String, Object> columnMap) {
		return new Record(columnMap).setColumnsMap(config.containerFactory.getColumnsMap(), true);
	}

	public  <T> List<T> query(Config config, Connection conn, String sql, Object... params) throws SQLException {
		List result = new ArrayList();
		try (PreparedStatement pst = conn.prepareStatement(sql)) {
			config.dialect.fillStatement(pst, params);
			ResultSet rs = pst.executeQuery();
			int colAmount = rs.getMetaData().getColumnCount();
			if (colAmount > 1) {
				while (rs.next()) {
					Object[] temp = new Object[colAmount];
					for (int i = 0; i < colAmount; i++) {
						temp[i] = rs.getObject(i + 1);
					}
					result.add(temp);
				}
			} else if (colAmount == 1) {
				while (rs.next()) {
					result.add(rs.getObject(1));
				}
			}
			manager.close(rs);
			return result;
		}
	}

	public <T> List<T> query(String sql, Object... params) {
		Connection conn = null;
		try {
			conn = config.getConnection();
			return query(config, conn, sql, params);
		} catch (Exception e) {
			throw new DatabaseException(e);
		} finally {
			manager.close(conn);
		}
	}

	public <T> List<T> query(String sql) {		
		return query(sql, NULL_PARAM_ARRAY);
	}

	public <T> T queryFirst(String sql, Object... params) {
		List<T> result = query(sql, params);
		return (result.size() > 0 ? result.get(0) : null);
	}

	public <T> T queryFirst(String sql) {

		List<T> result = query(sql, NULL_PARAM_ARRAY);
		return (result.size() > 0 ? result.get(0) : null);
	}

	public <T> T queryColumn(String sql, Object... params) {
		List<T> result = query(sql, params);
		if (result.size() > 0) {
			T temp = result.get(0);
			if (temp instanceof Object[]) {
				throw new DatabaseException("Only ONE COLUMN can be queried.");
			}
			return temp;
		}
		return null;
	}

	public <T> T queryColumn(String sql) {
		return (T)queryColumn(sql, NULL_PARAM_ARRAY);
	}

	public String queryStr(String sql, Object... params) {
		Object s = queryColumn(sql, params);
		return s != null ? s.toString() : null;
	}

	public String queryStr(String sql) {
		return queryStr(sql, NULL_PARAM_ARRAY);
	}

	public Integer queryInt(String sql, Object... params) {
		Number n = queryNumber(sql, params);
		return n != null ? n.intValue() : null;
	}

	public Integer queryInt(String sql) {
		return queryInt(sql, NULL_PARAM_ARRAY);
	}

	public Long queryLong(String sql, Object... params) {
		Number n = queryNumber(sql, params);
		return n != null ? n.longValue() : null;
	}

	public Long queryLong(String sql) {
		return queryLong(sql, NULL_PARAM_ARRAY);
	}

	public Double queryDouble(String sql, Object... params) {
		Number n = queryNumber(sql, params);
		return n != null ? n.doubleValue() : null;
	}

	public Double queryDouble(String sql) {
		return queryDouble(sql, NULL_PARAM_ARRAY);
	}

	public Float queryFloat(String sql, Object... params) {
		Number n = queryNumber(sql, params);
		return n != null ? n.floatValue() : null;
	}

	public Float queryFloat(String sql) {
		return queryFloat(sql, NULL_PARAM_ARRAY);
	}

	public BigDecimal queryBigDecimal(String sql, Object... params) {
		Object n = queryColumn(sql, params);
		if (n instanceof BigDecimal) {
			return (BigDecimal)n;
		} else if (n != null) {
			return new BigDecimal(n.toString());
		} else {
			return null;
		}
	}

	public BigDecimal queryBigDecimal(String sql) {
		return queryBigDecimal(sql, NULL_PARAM_ARRAY);
	}

	public BigInteger queryBigInteger(String sql, Object... params) {
		Object n = queryColumn(sql, params);
		if (n instanceof BigInteger) {
			return (BigInteger)n;
		} else if (n != null) {
			return new BigInteger(n.toString());
		} else {
			return null;
		}
	}

	public BigInteger queryBigInteger(String sql) {
		return queryBigInteger(sql, NULL_PARAM_ARRAY);
	}

	public byte[] queryBytes(String sql, Object... params) {
		return (byte[])queryColumn(sql, params);
	}

	public byte[] queryBytes(String sql) {
		return (byte[])queryColumn(sql, NULL_PARAM_ARRAY);
	}

	public java.util.Date queryDate(String sql, Object... params) {
		Object d = queryColumn(sql, params);

		if (d instanceof Temporal) {
			if (d instanceof LocalDateTime) {
				return DateUtil.toDate((LocalDateTime)d);
			}
			if (d instanceof LocalDate) {
				return DateUtil.toDate((LocalDate)d);
			}
			if (d instanceof LocalTime) {
				return DateUtil.toDate((LocalTime)d);
			}
		}

		return (java.util.Date)d;
	}

	public java.util.Date queryDate(String sql) {
		return queryDate(sql, NULL_PARAM_ARRAY);
	}

	public LocalDateTime queryLocalDateTime(String sql, Object... params) {
		Object d = queryColumn(sql, params);

		if (d instanceof LocalDateTime) {
			return (LocalDateTime)d;
		}
		if (d instanceof LocalDate) {
			return ((LocalDate)d).atStartOfDay();
		}
		if (d instanceof LocalTime) {
			return LocalDateTime.of(LocalDate.now(), (LocalTime)d);
		}
		if (d instanceof java.util.Date) {
			return DateUtil.toLocalDateTime((java.util.Date)d);
		}

		return (LocalDateTime)d;
	}

	public LocalDateTime queryLocalDateTime(String sql) {
		return queryLocalDateTime(sql, NULL_PARAM_ARRAY);
	}

	public java.sql.Time queryTime(String sql, Object... params) {
		return (java.sql.Time)queryColumn(sql, params);
	}

	public java.sql.Time queryTime(String sql) {
		return (java.sql.Time)queryColumn(sql, NULL_PARAM_ARRAY);
	}

	public java.sql.Timestamp queryTimestamp(String sql, Object... params) {
		return (java.sql.Timestamp)queryColumn(sql, params);
	}

	public java.sql.Timestamp queryTimestamp(String sql) {
		return (java.sql.Timestamp)queryColumn(sql, NULL_PARAM_ARRAY);
	}

	public Boolean queryBoolean(String sql, Object... params) {
		return (Boolean)queryColumn(sql, params);
	}

	public Boolean queryBoolean(String sql) {
		return (Boolean)queryColumn(sql, NULL_PARAM_ARRAY);
	}

	public Short queryShort(String sql, Object... params) {
		Number n = queryNumber(sql, params);
		return n != null ? n.shortValue() : null;
	}

	public Short queryShort(String sql) {
		return queryShort(sql, NULL_PARAM_ARRAY);
	}

	public Byte queryByte(String sql, Object... params) {
		Number n = queryNumber(sql, params);
		return n != null ? n.byteValue() : null;
	}

	public Byte queryByte(String sql) {
		return queryByte(sql, NULL_PARAM_ARRAY);
	}

	public Number queryNumber(String sql, Object... params) {
		return (Number)queryColumn(sql, params);
	}

	public Number queryNumber(String sql) {
		return (Number)queryColumn(sql, NULL_PARAM_ARRAY);
	}

	protected int update(Config config, Connection conn, String sql, Object... params) throws SQLException {
		try (PreparedStatement pst = conn.prepareStatement(sql)) {
			config.dialect.fillStatement(pst, params);
			return pst.executeUpdate();
		}
	}

	public int update(String sql, Object... params) {
		Connection conn = null;
		try {
			conn = config.getConnection();
			return update(config, conn, sql, params);
		} catch (Exception e) {
			throw new DatabaseException(e);
		} finally {
			manager.close(conn);
		}
	}

	public int update(String sql) {
		return update(sql, NULL_PARAM_ARRAY);
	}

	protected List<Record> find(Config config, Connection conn, String sql, Object... params) throws SQLException {
		try (PreparedStatement pst = conn.prepareStatement(sql)) {
			config.dialect.fillStatement(pst, params);
			ResultSet rs = pst.executeQuery();
			List<Record> result = config.dialect.buildRecordList(config, rs);    
			manager.close(rs);
			return result;
		}
	}

	public List<Record> find(String sql, Object... params) {
		Connection conn = null;
		try {
			conn = config.getConnection();
			return find(config, conn, sql, params);
		} catch (Exception e) {
			throw new DatabaseException(e);
		} finally {
			manager.close(conn);
		}
	}

	public Record findTop1(String findSql, Object... params){
		List<Record> result = findTop(1, findSql, params);
		return result.size() > 0 ? result.get(0) : null;
	}

	public List<Record> findTop(int size, String findSql, Object... params) {
		Connection conn = null;
		try {
			conn = config.getConnection();
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(findSql);
			String sql = config.dialect.forPaginate(1, size, stringBuilder);
			return find(config, conn, sql, params);
		} catch (Exception e) {
			throw new DatabaseException(e);
		} finally {
			manager.close(conn);
		}
	}

	public List<Record> find(String sql) {
		return find(sql, NULL_PARAM_ARRAY);
	}

	public List<Record> findAll(String tableName) {
		String sql = config.dialect.forFindAll(tableName);
		return find(sql, NULL_PARAM_ARRAY);
	}

	public Record findFirst(String sql, Object... params) {
		List<Record> result = find(sql, params);
		return result.size() > 0 ? result.get(0) : null;
	}

	public Record findFirst(String sql) {
		return findFirst(sql, NULL_PARAM_ARRAY);
	}

	public Record findById(String tableName, Object idValue) {
		return findByIds(tableName, config.dialect.getDefaultPrimaryKey(), idValue);
	}

	public Record findById(String tableName, String primaryKey, Object idValue) {
		return findByIds(tableName, primaryKey, idValue);
	}

	public Record findByIds(String tableName, String primaryKey, Object... idValues) {
		String[] pKeys = primaryKey.split(",");
		if (pKeys.length != idValues.length) {
			throw new IllegalArgumentException("primary key number must equals id value number");
		}
		String sql = config.dialect.forDbFindById(tableName, pKeys);
		List<Record> result = find(sql, idValues);
		return result.size() > 0 ? result.get(0) : null;
	}

	public boolean deleteById(String tableName, Object idValue) {
		return deleteByIds(tableName, config.dialect.getDefaultPrimaryKey(), idValue);
	}

	public boolean deleteById(String tableName, String primaryKey, Object idValue) {
		return deleteByIds(tableName, primaryKey, idValue);
	}

	public boolean deleteByIds(String tableName, String primaryKey, Object... idValues) {
		String[] pKeys = primaryKey.split(",");
		if (pKeys.length != idValues.length) {
			throw new IllegalArgumentException("primary key number must equals id value number");
		}
		String sql = config.dialect.forDbDeleteById(tableName, pKeys);
		return update(sql, idValues) >= 1;
	}

	public boolean delete(String tableName, String primaryKey, Record record) {
		String[] pKeys = primaryKey.split(",");
		if (pKeys.length <= 1) {
			Object t = record.get(primaryKey);	
			return deleteByIds(tableName, primaryKey, t);
		}

		config.dialect.trimPrimaryKeys(pKeys);
		Object[] idValue = new Object[pKeys.length];
		for (int i=0; i<pKeys.length; i++) {
			idValue[i] = record.get(pKeys[i]);
			if (idValue[i] == null) {
				throw new IllegalArgumentException("The value of primary key \"" + pKeys[i] + "\" can not be null in record object");
			}
		}
		return deleteByIds(tableName, primaryKey, idValue);
	}

	public boolean delete(String tableName, Record record) {
		String defaultPrimaryKey = config.dialect.getDefaultPrimaryKey();
		Object t = record.get(defaultPrimaryKey);	
		return deleteByIds(tableName, defaultPrimaryKey, t);
	}

	public int delete(String sql, Object... params) {
		return update(sql, params);
	}

	public int delete(String sql) {
		return update(sql);
	}

	public Page<Record> paginate(int pageNumber, int pageSize, String select, String sqlExceptSelect, Object... params) {
		return doPaginate(pageNumber, pageSize, null, select, sqlExceptSelect, params);
	}

	public Page<Record> paginate(int pageNumber, int pageSize, String select, String sqlExceptSelect) {
		return doPaginate(pageNumber, pageSize, null, select, sqlExceptSelect, NULL_PARAM_ARRAY);
	}

	public Page<Record> paginate(int pageNumber, int pageSize, boolean isGroupBySql, String select, String sqlExceptSelect, Object... params) {
		return doPaginate(pageNumber, pageSize, isGroupBySql, select, sqlExceptSelect, params);
	}

	protected Page<Record> doPaginate(int pageNumber, int pageSize, Boolean isGroupBySql, String select, String sqlExceptSelect, Object... params) {
		Connection conn = null;
		try {
			conn = config.getConnection();
			String totalRowSql = config.dialect.forPaginateTotalRow(select, sqlExceptSelect, null);
			StringBuilder findSql = new StringBuilder();
			findSql.append(select).append(' ').append(sqlExceptSelect);
			return doPaginateByFullSql(config, conn, pageNumber, pageSize, isGroupBySql, totalRowSql, findSql, params);
		} catch (Exception e) {
			throw new DatabaseException(e);
		} finally {
			manager.close(conn);
		}
	}

	protected Page<Record> doPaginateByFullSql(Config config, Connection conn, int pageNumber, int pageSize, Boolean isGroupBySql, String totalRowSql, StringBuilder findSql, Object... params) throws SQLException {
		if (pageNumber < 1 || pageSize < 1) {
			throw new DatabaseException("pageNumber and pageSize must be greater than 0");
		}

		if (config.dialect.isTakeOverDbPaginate()) {
			return config.dialect.takeOverDbPaginate(this, config, conn, pageNumber, pageSize, isGroupBySql, totalRowSql, findSql, params);
		}

		List result = query(config, conn, totalRowSql, params);
		int size = result.size();
		if (isGroupBySql == null) {
			isGroupBySql = size > 1;
		}

		long totalRow;
		if (isGroupBySql) {
			totalRow = size;
		} else {
			totalRow = (size > 0) ? ((Number)result.get(0)).longValue() : 0;
		}
		if (totalRow == 0) {
			return new Page<Record>(new ArrayList<Record>(0), pageNumber, pageSize, 0, 0);
		}

		int totalPage = (int) (totalRow / pageSize);
		if (totalRow % pageSize != 0) {
			totalPage++;
		}

		if (pageNumber > totalPage) {
			return new Page<Record>(new ArrayList<Record>(0), pageNumber, pageSize, totalPage, (int)totalRow);
		}

		String sql = config.dialect.forPaginate(pageNumber, pageSize, findSql);
		List<Record> list = find(config, conn, sql, params);
		return new Page<Record>(list, pageNumber, pageSize, totalPage, (int)totalRow);
	}

	protected Page<Record> paginate(Config config, Connection conn, int pageNumber, int pageSize, String select, String sqlExceptSelect, Object... params) throws SQLException {
		String totalRowSql = config.dialect.forPaginateTotalRow(select, sqlExceptSelect, null);
		StringBuilder findSql = new StringBuilder();
		findSql.append(select).append(' ').append(sqlExceptSelect);
		return doPaginateByFullSql(config, conn, pageNumber, pageSize, null, totalRowSql, findSql, params);
	}

	protected Page<Record> doPaginateByFullSql(int pageNumber, int pageSize, Boolean isGroupBySql, String totalRowSql, String findSql, Object... params) {
		Connection conn = null;
		try {
			conn = config.getConnection();
			StringBuilder findSqlBuf = new StringBuilder().append(findSql);
			return doPaginateByFullSql(config, conn, pageNumber, pageSize, isGroupBySql, totalRowSql, findSqlBuf, params);
		} catch (Exception e) {
			throw new DatabaseException(e);
		} finally {
			manager.close(conn);
		}
	}

	public Page<Record> paginateByFullSql(int pageNumber, int pageSize, String totalRowSql, String findSql, Object... params) {
		return doPaginateByFullSql(pageNumber, pageSize, null, totalRowSql, findSql, params);
	}

	public Page<Record> paginateByFullSql(int pageNumber, int pageSize, boolean isGroupBySql, String totalRowSql, String findSql, Object... params) {
		return doPaginateByFullSql(pageNumber, pageSize, isGroupBySql, totalRowSql, findSql, params);
	}

	protected boolean save(Config config, Connection conn, String tableName, String primaryKey, Record record) throws SQLException {
		String[] pKeys = primaryKey.split(",");
		List<Object> params = new ArrayList<Object>();
		StringBuilder sql = new StringBuilder();
		config.dialect.forDbSave(tableName, pKeys, record, sql, params);

		try (PreparedStatement pst =
				config.dialect.isOracle() ?
				conn.prepareStatement(sql.toString(), pKeys) :
				conn.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS)) {
			config.dialect.fillStatement(pst, params);
			int result = pst.executeUpdate();
			config.dialect.getRecordGeneratedKey(pst, record, pKeys);
			return result >= 1;
		}
	}

	public boolean save(String tableName, String primaryKey, Record record) {
		Connection conn = null;
		try {
			conn = config.getConnection();
			return save(config, conn, tableName, primaryKey, record);
		} catch (Exception e) {
			throw new DatabaseException(e);
		} finally {
			manager.close(conn);
		}
	}

	public boolean save(String tableName, Record record) {
		return save(tableName, config.dialect.getDefaultPrimaryKey(), record);
	}

	protected boolean update(Config config, Connection conn, String tableName, String primaryKey, Record record) throws SQLException {
		String[] pKeys = primaryKey.split(",");
		Object[] ids = new Object[pKeys.length];

		for (int i=0; i<pKeys.length; i++) {
			ids[i] = record.get(pKeys[i].trim());	
			if (ids[i] == null) {
				throw new DatabaseException("You can't update record without Primary Key, " + pKeys[i] + " can not be null.");
			}
		}

		StringBuilder sql = new StringBuilder();
		List<Object> params = new ArrayList<Object>();
		config.dialect.forDbUpdate(tableName, pKeys, ids, record, sql, params);

		if (params.size() <= 1) {	
			return false;
		}

		return update(config, conn, sql.toString(), params.toArray()) >= 1;
	}

	public boolean update(String tableName, String primaryKey, Record record) {
		Connection conn = null;
		try {
			conn = config.getConnection();
			return update(config, conn, tableName, primaryKey, record);
		} catch (Exception e) {
			throw new DatabaseException(e);
		} finally {
			manager.close(conn);
		}
	}

	public boolean update(String tableName, Record record) {
		return update(tableName, config.dialect.getDefaultPrimaryKey(), record);
	}

	public Object execute(DbCallback callback) {
		return execute(config, callback);
	}

	protected Object execute(Config config, DbCallback callback) {
		Connection conn = null;
		try {
			conn = config.getConnection();
			return callback.call(conn);
		} catch (Exception e) {
			throw new DatabaseException(e);
		} finally {
			manager.close(conn);
		}
	}

	protected boolean transaction(Atom atom) {
		var transactionManager = manager.getTransactionManager();
		if(!(transactionManager instanceof LocalTransactionManager)) {
			throw new RuntimeException("It is not supported by " + transactionManager.getClass().getSimpleName() + ", LocalTransactionManager is required.");
		}
		LocalTransactionManager txManager = (LocalTransactionManager)transactionManager;
		if(txManager.inLocalTransaction()) {
			try {
				boolean result = atom.run();
				if (result) {
					return true;
				}
				throw new NestedTransactionHelpException("Notice the outer transaction that the nested transaction return false");	
			} catch (SQLException e) {
				throw new DatabaseException(e);
			}
		}

		txManager.startTransaction();
		Long transactionId = txManager.getTransactionId();
		HashMap<String, Connection> connections = null;
		try {
			boolean result = atom.run();
			connections = txManager.endTransaction();
			if(connections != null) {
				if (result) {
					for (var entry : connections.entrySet()) {
						entry.getValue().commit();
						logger.debug("finish transaction %s, final commit for %s.", transactionId, entry.getKey());
					}
				} else {
					for (var entry : connections.entrySet()) {
						entry.getValue().rollback();
					}
				}
			}
			return result;
		} catch (NestedTransactionHelpException e) {
			if(connections == null) {
				connections = txManager.endTransaction();
			}
			if(connections != null) {
				for (var entry : connections.entrySet()) {
					try {
						entry.getValue().rollback();
						logger.warn("failed transaction %s, rollback for %s.", transactionId, entry.getKey());
					} catch (Exception e1) {
						logger.error(e1, "failed transaction %s, %s rollback exception", transactionId, entry.getKey());
					}
				}
			}
			return false;
		} catch (Throwable t) {
			if(connections == null) {
				connections = txManager.endTransaction();
			}
			if(connections != null) {
				for (var entry : connections.entrySet()) {
					try {
						entry.getValue().rollback();
						logger.warn("failed transaction %s, rollback for %s.", transactionId, entry.getKey());
					} catch (Exception e1) {
						logger.error(e1, "failed transaction %s, %s rollback exception", transactionId, entry.getKey());
					}
				}
			}
			throw t instanceof RuntimeException ? (RuntimeException)t : new DatabaseException(t);
		} finally {
			if(connections != null) {
				for (var entry : connections.entrySet()) {
					try {
						entry.getValue().close();
					} catch (Throwable t) {
						logger.error(t,"%s transaction exception, close connection failed", entry.getKey());	
					}
				}
			}
		}
	}

	public Future<Boolean> asyncTransaction(Atom atom) {
		FutureTask<Boolean> task = new FutureTask<>(() -> transaction(atom));
		Thread thread = new Thread(task);
		thread.setDaemon(true);
		thread.start();
		return task;
	}

	public List<Record> findByCache(String cacheName, Object key, String sql, Object... params) {
		Cache cache = config.getCache();
		List<Record> result = cache.get(cacheName, key);
		if (result == null) {
			result = find(sql, params);
			cache.put(cacheName, key, result);
		}
		return result;
	}

	public List<Record> findByCache(String cacheName, Object key, String sql) {
		return findByCache(cacheName, key, sql, NULL_PARAM_ARRAY);
	}

	public Record findFirstByCache(String cacheName, Object key, String sql, Object... params) {
		Cache cache = config.getCache();
		Record result = cache.get(cacheName, key);
		if (result == null) {
			result = findFirst(sql, params);
			cache.put(cacheName, key, result);
		}
		return result;
	}

	public Record findFirstByCache(String cacheName, Object key, String sql) {
		return findFirstByCache(cacheName, key, sql, NULL_PARAM_ARRAY);
	}

	public Page<Record> paginateByCache(String cacheName, Object key, int pageNumber, int pageSize, String select, String sqlExceptSelect, Object... params) {
		return doPaginateByCache(cacheName, key, pageNumber, pageSize, null, select, sqlExceptSelect, params);
	}

	public Page<Record> paginateByCache(String cacheName, Object key, int pageNumber, int pageSize, String select, String sqlExceptSelect) {
		return doPaginateByCache(cacheName, key, pageNumber, pageSize, null, select, sqlExceptSelect, NULL_PARAM_ARRAY);
	}

	public Page<Record> paginateByCache(String cacheName, Object key, int pageNumber, int pageSize, boolean isGroupBySql, String select, String sqlExceptSelect, Object... params) {
		return doPaginateByCache(cacheName, key, pageNumber, pageSize, isGroupBySql, select, sqlExceptSelect, params);
	}

	protected Page<Record> doPaginateByCache(String cacheName, Object key, int pageNumber, int pageSize, Boolean isGroupBySql, String select, String sqlExceptSelect, Object... params) {
		Cache cache = config.getCache();
		Page<Record> result = cache.get(cacheName, key);
		if (result == null) {
			result = doPaginate(pageNumber, pageSize, isGroupBySql, select, sqlExceptSelect, params);
			cache.put(cacheName, key, result);
		}
		return result;
	}

	protected int[] batch(Config config, Connection conn, String sql, Object[][] params, int batchSize) throws SQLException {
		if (params == null || params.length == 0) {
			return new int[0];
		}
		if (batchSize < 1) {
			throw new IllegalArgumentException("The batchSize must be greater than 0.");
		}
		boolean isInTransaction = manager.getTransactionManager().inLocalTransaction();
		int counter = 0;
		int pointer = 0;
		int[] result = new int[params.length];
		try (PreparedStatement pst = conn.prepareStatement(sql)) {
			for (int i = 0; i < params.length; i++) {
				for (int j = 0; j < params[i].length; j++) {
					Object value = params[i][j];
					if (value instanceof java.util.Date) {
						if (value instanceof java.sql.Date) {
							pst.setDate(j + 1, (java.sql.Date) value);
						} else if (value instanceof java.sql.Timestamp) {
							pst.setTimestamp(j + 1, (java.sql.Timestamp) value);
						} else {

							java.util.Date d = (java.util.Date) value;
							pst.setTimestamp(j + 1, new java.sql.Timestamp(d.getTime()));
						}
					} else {
						pst.setObject(j + 1, value);
					}
				}
				pst.addBatch();
				if (++counter >= batchSize) {
					counter = 0;
					int[] r = pst.executeBatch();
					if (!isInTransaction)
						conn.commit();
					for (int k = 0; k < r.length; k++)
						result[pointer++] = r[k];
				}
			}
			if (counter != 0) {
				int[] r = pst.executeBatch();
				if (!isInTransaction)
					conn.commit();
				for (int k = 0; k < r.length; k++)
					result[pointer++] = r[k];
			}
			return result;
		}
	}

	public int[] batch(String sql, Object[][] params, int batchSize) {
		Connection conn = null;
		Boolean autoCommit = null;
		try {
			conn = config.getConnection();
			autoCommit = conn.getAutoCommit();
			conn.setAutoCommit(false);
			return batch(config, conn, sql, params, batchSize);
		} catch (Exception e) {
			throw new DatabaseException(e);
		} finally {
			if (autoCommit != null) {
				try {
					conn.setAutoCommit(autoCommit);
				} catch (Exception e) {
					logger.error(e, "Transaction exception");
				}
			}
			manager.close(conn);
		}
	}

	protected int[] batch(Config config, Connection conn, String sql, String columns, List list, int batchSize) throws SQLException {
		if (list == null || list.size() == 0) {
			return new int[0];
		}
		Object element = list.get(0);
		if (!(element instanceof Record) && !(element instanceof Model)) {
			throw new IllegalArgumentException("The element in list must be Model or Record.");
		}
		if (batchSize < 1) {
			throw new IllegalArgumentException("The batchSize must be greater than 0.");
		}
		boolean isModel = element instanceof Model;

		String[] columnArray = columns.split(",");
		for (int i=0; i<columnArray.length; i++) {
			columnArray[i] = columnArray[i].trim();
		}
		boolean isInTransaction = manager.getTransactionManager().inLocalTransaction();
		int counter = 0;
		int pointer = 0;
		int size = list.size();
		int[] result = new int[size];
		try (PreparedStatement pst = conn.prepareStatement(sql)) {
			for (int i = 0; i < size; i++) {
				Map map = isModel ? ((Model) list.get(i))._getAttrs() : ((Record) list.get(i)).getColumns();
				for (int j = 0; j < columnArray.length; j++) {
					Object value = map.get(columnArray[j]);
					if (value instanceof java.util.Date) {
						if (value instanceof java.sql.Date) {
							pst.setDate(j + 1, (java.sql.Date) value);
						} else if (value instanceof java.sql.Timestamp) {
							pst.setTimestamp(j + 1, (java.sql.Timestamp) value);
						} else {

							java.util.Date d = (java.util.Date) value;
							pst.setTimestamp(j + 1, new java.sql.Timestamp(d.getTime()));
						}
					} else {
						pst.setObject(j + 1, value);
					}
				}
				pst.addBatch();
				if (++counter >= batchSize) {
					counter = 0;
					int[] r = pst.executeBatch();
					if (!isInTransaction)
						conn.commit();
					for (int k = 0; k < r.length; k++)
						result[pointer++] = r[k];
				}
			}
			if (counter != 0) {
				int[] r = pst.executeBatch();
				if (!isInTransaction)
					conn.commit();
				for (int k = 0; k < r.length; k++)
					result[pointer++] = r[k];
			}
			return result;
		}
	}

	public int[] batch(String sql, String columns, List modelOrRecordList, int batchSize) {
		Connection conn = null;
		Boolean autoCommit = null;
		try {
			conn = config.getConnection();
			autoCommit = conn.getAutoCommit();
			conn.setAutoCommit(false);
			return batch(config, conn, sql, columns, modelOrRecordList, batchSize);
		} catch (Exception e) {
			throw new DatabaseException(e);
		} finally {
			if (autoCommit != null) {
				try {
					conn.setAutoCommit(autoCommit);
				} catch (Exception e) {
					logger.error(e, "Transaction exception");
				}
			}
			manager.close(conn);
		}
	}

	protected int[] batch(Config config, Connection conn, List<String> sqlList, int batchSize) throws SQLException {
		if (sqlList == null || sqlList.size() == 0) {
			return new int[0];
		}
		if (batchSize < 1) {
			throw new IllegalArgumentException("The batchSize must be greater than 0.");
		}
		boolean isInTransaction = manager.getTransactionManager().inLocalTransaction();
		int counter = 0;
		int pointer = 0;
		int size = sqlList.size();
		int[] result = new int[size];
		try (Statement st = conn.createStatement()) {
			for (int i = 0; i < size; i++) {
				st.addBatch(sqlList.get(i));
				if (++counter >= batchSize) {
					counter = 0;
					int[] r = st.executeBatch();
					if (!isInTransaction) {
						conn.commit();
					}
					for (int k = 0; k < r.length; k++) {
						result[pointer++] = r[k];
					}
				}
			}
			if (counter != 0) {
				int[] r = st.executeBatch();
				if (!isInTransaction) {
					conn.commit();
				}
				for (int k = 0; k < r.length; k++) {
					result[pointer++] = r[k];
				}
			}
			return result;
		}
	}

    public int[] batch(List<String> sqlList, int batchSize) {
		Connection conn = null;
		Boolean autoCommit = null;
		try {
			conn = config.getConnection();
			autoCommit = conn.getAutoCommit();
			conn.setAutoCommit(false);
			return batch(config, conn, sqlList, batchSize);
		} catch (Exception e) {
			throw new DatabaseException(e);
		} finally {
			if (autoCommit != null) {
				try {
					conn.setAutoCommit(autoCommit);
				} catch (Exception e) {
					logger.error(e, "Transaction exception");
				}
			}
			manager.close(conn);
		}
    }

    public int[] batchSave(List<? extends Model> modelList, Table table, int batchSize) {
    	if (modelList == null || modelList.size() == 0) {
			return new int[0];
		}

    	Model model = modelList.get(0);
    	Map<String, Object> attrs = model._getAttrs();
    	int index = 0;
    	StringBuilder columns = new StringBuilder();

		for (Entry<String, Object> e : attrs.entrySet()) {
			if (config.dialect.isOracle()) {	
				Object value = e.getValue();
				if (value instanceof String && ((String)value).endsWith(".nextval")) {
					continue ;
				}
			}

			if (index++ > 0) {
				columns.append(',');
			}
			columns.append(e.getKey());
		}

    	StringBuilder sql = new StringBuilder();
    	List<Object> paramsNoUse = new ArrayList<Object>();
    	config.dialect.forModelSave(table, attrs, sql, paramsNoUse);
    	return batch(sql.toString(), columns.toString(), modelList, batchSize);
    }

    public int[] batchSave(String tableName, List<Record> recordList, int batchSize) {
    	if (recordList == null || recordList.size() == 0) {
			return new int[0];
		}

    	Record record = recordList.get(0);
    	Map<String, Object> cols = record.getColumns();
    	int index = 0;
    	StringBuilder columns = new StringBuilder();

		for (Entry<String, Object> e : cols.entrySet()) {
			if (config.dialect.isOracle()) {	
				Object value = e.getValue();
				if (value instanceof String && ((String)value).endsWith(".nextval")) {
					continue ;
				}
			}

			if (index++ > 0) {
				columns.append(',');
			}
			columns.append(e.getKey());
		}

    	String[] pKeysNoUse = new String[0];
    	StringBuilder sql = new StringBuilder();
    	List<Object> paramsNoUse = new ArrayList<Object>();
    	config.dialect.forDbSave(tableName, pKeysNoUse, record, sql, paramsNoUse);
    	return batch(sql.toString(), columns.toString(), recordList, batchSize);
    }

    public int[] batchUpdate(List<? extends Model> modelList, Table table, int batchSize) {
    	if (modelList == null || modelList.size() == 0) {
			return new int[0];
		}

    	Model model = modelList.get(0);
    	String[] pKeys = table.getPrimaryKey();
    	Map<String, Object> attrs = model._getAttrs();
    	List<String> attrNames = new ArrayList<String>();

    	for (Entry<String, Object> e : attrs.entrySet()) {
    		String attr = e.getKey();
    		if (config.dialect.isPrimaryKey(attr, pKeys) == false && table.hasColumnLabel(attr)) {
				attrNames.add(attr);
			}
    	}
    	for (String pKey : pKeys) {
			attrNames.add(pKey);
		}
    	String columns = StrUtil.join(attrNames.toArray(new String[attrNames.size()]), ",");

    	Set<String> modifyFlag = attrs.keySet();

    	StringBuilder sql = new StringBuilder();
    	List<Object> paramsNoUse = new ArrayList<Object>();
    	config.dialect.forModelUpdate(table, attrs, modifyFlag, sql, paramsNoUse);
    	return batch(sql.toString(), columns, modelList, batchSize);
    }

    public int[] batchUpdate(String tableName, String primaryKey, List<Record> recordList, int batchSize) {
    	if (recordList == null || recordList.size() == 0) {
			return new int[0];
		}

    	String[] pKeys = primaryKey.split(",");
    	config.dialect.trimPrimaryKeys(pKeys);

    	Record record = recordList.get(0);
    	Map<String, Object> cols = record.getColumns();
    	List<String> colNames = new ArrayList<String>();

    	for (Entry<String, Object> e : cols.entrySet()) {
    		String col = e.getKey();
    		if (!config.dialect.isPrimaryKey(col, pKeys)) {
				colNames.add(col);
			}
    	}
    	for (String pKey : pKeys) {
			colNames.add(pKey);
		}
    	String columns = StrUtil.join(colNames.toArray(new String[colNames.size()]), ",");

    	Object[] idsNoUse = new Object[pKeys.length];
    	StringBuilder sql = new StringBuilder();
    	List<Object> paramsNoUse = new ArrayList<Object>();
    	config.dialect.forDbUpdate(tableName, pKeys, idsNoUse, record, sql, paramsNoUse);
    	return batch(sql.toString(), columns, recordList, batchSize);
    }

    public int[] batchUpdate(String tableName, List<Record> recordList, int batchSize) {
    	return batchUpdate(tableName, config.dialect.getDefaultPrimaryKey(),recordList, batchSize);
    }

    public String getSql(String key) {
    	return config.getSqlKit().getSql(key);
    }

    public SqlParam getSqlParam(String key, Record record) {
    	return getSqlParam(key, record.getColumns());
    }

    public SqlParam getSqlParam(String key, Model model) {
    	return getSqlParam(key, model._getAttrs());
    }

    public SqlParam getSqlParam(String key, Map data) {
    	return config.getSqlKit().getSqlParam(key, data);
    }

    public SqlParam getSqlParam(String key, Object... params) {
    	return config.getSqlKit().getSqlParam(key, params);
    }

	public SqlParam getSqlParamByString(String content, Map data) {
		return config.getSqlKit().getSqlParamByString(content, data);
	}

	public SqlParam getSqlParamByString(String content, Object... params) {
		return config.getSqlKit().getSqlParamByString(content, params);
	}

    public List<Record> find(SqlParam sqlParam) {
    	return find(sqlParam.getSql(), sqlParam.getParam());
    }

    public Record findFirst(SqlParam sqlParam) {
    	return findFirst(sqlParam.getSql(), sqlParam.getParam());
    }

    public int update(SqlParam sqlParam) {
    	return update(sqlParam.getSql(), sqlParam.getParam());
    }

    public Page<Record> paginate(int pageNumber, int pageSize, SqlParam sqlParam) {
    	String[] sqls = PageSqlKit.parsePageSql(sqlParam.getSql());
    	return doPaginate(pageNumber, pageSize, null, sqls[0], sqls[1], sqlParam.getParam());
    }

	public Page<Record> paginate(int pageNumber, int pageSize, boolean isGroupBySql, SqlParam sqlParam) {
		String[] sqls = PageSqlKit.parsePageSql(sqlParam.getSql());
		return doPaginate(pageNumber, pageSize, isGroupBySql, sqls[0], sqls[1], sqlParam.getParam());
	}

	public void each(Function<Record, Boolean> func, String sql, Object... params) {
		Connection conn = null;
		try {
			conn = config.getConnection();

			try (PreparedStatement pst = conn.prepareStatement(sql)) {
				config.dialect.fillStatement(pst, params);
				ResultSet rs = pst.executeQuery();
				config.dialect.eachRecord(config, rs, func);
				manager.close(rs);
			}

		} catch (Exception e) {
			throw new DatabaseException(e);
		} finally {
			manager.close(conn);
		}
	}

	public DbTemplate template(String key, Map data) {
		return new DbTemplate(this, key, data);
	}

	public DbTemplate template(String key, Object... params) {
		return new DbTemplate(this, key, params);
	}

	public DbTemplate templateByString(String content, Map data) {
		return new DbTemplate(true, this, content, data);
	}

	public DbTemplate templateByString(String content, Object... params) {
		return new DbTemplate(true, this, content, params);
	}

	public List<String> getAllTables(boolean withSchema) {
		try {
			return getAllTables(config.getConnection(), withSchema);
		} catch (Exception e) {
			throw new DatabaseException(e);
		}
	}

	public List<String> getAllTables(Connection conn, boolean withSchema) {
		try {
			ResultSet resultSet = conn.getMetaData().getTables(conn.getCatalog(), null, null, new String[]{"TABLE"});
			ArrayList<String> tables = new ArrayList<>();
			while (resultSet.next()) {
				String name = resultSet.getString("TABLE_NAME");
				String schema = resultSet.getString("TABLE_SCHEM");
				tables.add(withSchema ? schema + "." + name : name);
			}
			return tables;
		} catch (Exception e) {
			throw new DatabaseException(e);
		} finally {
			manager.close(conn);
		}
	}

	public boolean tableExists(String tableName) {
		try {
			return tableExists(config.getConnection(), tableName);
		} catch (Exception e) {
			throw new DatabaseException(e);
		}
	}

	public boolean tableExists(Connection conn, String tableName) {
		try {
			ResultSet resultSet = conn.getMetaData().getTables(conn.getCatalog(), null, tableName.toUpperCase(), new String[]{"TABLE"});
			return resultSet.next();
		} catch (Exception e) {
			throw new DatabaseException(e);
		} finally {
			manager.close(conn);
		}
	}
}

