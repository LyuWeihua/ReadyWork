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
import work.ready.core.ioc.annotation.Scope;
import work.ready.core.ioc.annotation.ScopeType;
import work.ready.core.json.Json;
import work.ready.core.security.CurrentUser;
import work.ready.core.security.UserIdentity;
import work.ready.core.security.data.CallerInspector;
import work.ready.core.server.Ready;
import work.ready.core.tools.DateUtil;
import work.ready.core.tools.StrUtil;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;

import static work.ready.core.database.Db.NULL_PARAM_ARRAY;

@SuppressWarnings({"rawtypes", "unchecked"})
@Scope(ScopeType.prototype)
public abstract class Model<M extends Model> implements Serializable {

	private static final long serialVersionUID = -990334519496260591L;

	public static final int FILTER_BY_SAVE = 0;
	public static final int FILTER_BY_UPDATE = 1;
	protected static DatabaseManager manager = Ready.dbManager();

	private String configName;

	private Set<String> modifyFlag;

	private Map<String, Object> attrs = createAttrsMap();

	private Map<String, Object> createAttrsMap() {
		Config config = _getConfig();
		return config.containerFactory.getAttrsMap();
	}

	public M dao() {
		attrs = DaoContainerFactory.daoMap;
		modifyFlag = DaoContainerFactory.daoSet;
		return (M)this;
	}

	public M copy() {
		M m = null;
		try {
			m = (M) _getUserClass().getConstructor().newInstance();
			if(!this.attrs.getClass().equals(DaoContainerFactory.daoMap.getClass())) {
				m.put(_getAttrs());
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return m;
	}

	public M copyModel() {
		M m = null;
		try {
			m = (M) _getUserClass().getConstructor().newInstance();
			Table table = _getTable(true);
			Set<String> attrKeys = table.getColumnTypeMap().keySet();
			for (String attrKey : attrKeys) {
				Object o = this.get(attrKey);
				if (o != null) {
					m.set(attrKey, o);
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return m;
	}

	protected void filter(int filterBy) {

	}

	protected Map<String, Object> _getAttrs() {
		return attrs;
	}

	public Map<String, Object> getAttrs() {return new LinkedHashMap(attrs);}

	public Map<String, Object> getData(boolean camelCase) {
		Map<String, Object> dataMap = getAttrs();
		if(manager.getDataSecurityInspector() != null){
			CallerInspector callerInspector = new CallerInspector();
			dataMap = manager.getDataSecurityInspector().outputExamine(callerInspector.getCallerClassName(), callerInspector.getCallerMethodName(), _getTable().getDatasource(), _getTable().getName(), dataMap);
		}
		if(camelCase){
			Map<String, Object> camelCaseMap = new LinkedHashMap<>();
			dataMap.forEach((key, val)->camelCaseMap.put(StrUtil.toCamelCase(key), val));
			return camelCaseMap;
		} else {
			return dataMap;
		}
	}

	public Set<Entry<String, Object>> _getAttrsEntrySet() {
		return attrs.entrySet();
	}

	public String[] _getAttrNames() {
		Set<String> attrNameSet = attrs.keySet();
		return attrNameSet.toArray(new String[attrNameSet.size()]);
	}

	public Object[] _getAttrValues() {
		java.util.Collection<Object> attrValueCollection = attrs.values();
		return attrValueCollection.toArray(new Object[attrValueCollection.size()]);
	}

	public M _setAttrs(M model) {
		return (M)_setAttrs(model._getAttrs());
	}

	public M _setAttrs(Map<String, Object> attrs) {
		for (Entry<String, Object> e : attrs.entrySet()) {
			set(e.getKey(), e.getValue());
		}
		return (M)this;
	}

	protected Set<String> _getModifyFlag() {
		if (modifyFlag == null) {
			Config config = _getConfig();
			modifyFlag = config.containerFactory.getModifyFlagSet();
		}
		return modifyFlag;
	}

	protected Config _getConfig() {
		if (configName != null) {
			return manager.getConfig(configName);
		}
		return manager.getConfig(_getUserClass());
	}

	public <T> T _getIdValue() {
		return get(_getPrimaryKey());
	}

	public <T> T[] _getIdValues(Class<T> clazz) {
		String[] pkeys = _getPrimaryKeys();
		T[] values = (T[]) Array.newInstance(clazz, pkeys.length);

		int i = 0;
		for (String key : pkeys) {
			values[i++] = get(key);
		}
		return values;
	}

	private transient Table table;

	public Table _getTable(boolean validateNull) {
		if (table == null) {
			table = manager.tableManager.getTable(_getUserClass());
			if (table == null && validateNull) {
				throw new RuntimeException(
						String.format("class %s can not mapping to database table. "
								, _getUserClass().getName()));
			}
		}
		return table;
	}

	protected Table _getTable() {
		return _getTable(false);
	}

	public String _getTableName() {
		return _getTable(true).getName();
	}

	public String _getPrimaryKey() {
		return _getPrimaryKeys()[0];
	}

	private transient String[] primaryKeys;

	public String[] _getPrimaryKeys() {
		if (primaryKeys != null) {
			return primaryKeys;
		}
		primaryKeys = _getTable(true).getPrimaryKey();

		if (primaryKeys == null) {
			throw new RuntimeException(String.format("primaryKeys == null in [%s]", getClass()));
		}
		return primaryKeys;
	}

	private transient Class<?> primaryType;

	protected Class<?> _getPrimaryType() {
		if (primaryType == null) {
			primaryType = _getTable(true).getColumnType(_getPrimaryKey());
		}
		return primaryType;
	}

	protected boolean _hasColumn(String columnLabel) {
		return _getTable(true).hasColumnLabel(columnLabel);
	}

	public M preventXssAttack() {
		String[] attrNames = _getAttrNames();
		for (String attrName : attrNames) {
			Object value = get(attrName);
			if (value == null || !(value instanceof String)) {
				continue;
			}

			set(attrName, StrUtil.escapeHTMLTags((String) value));
		}
		return (M) this;
	}

	public M preventXssAttack(String... ignoreAttrs) {
		String[] attrNames = _getAttrNames();
		for (String attrName : attrNames) {
			Object value = get(attrName);
			if (value == null || !(value instanceof String)) {
				continue;
			}

			boolean isIgnoreAttr = false;
			for (String ignoreAttr : ignoreAttrs) {
				if (attrName.equals(ignoreAttr)) {
					isIgnoreAttr = true;
					break;
				}
			}

			if (isIgnoreAttr) {
				continue;
			} else {
				set(attrName, StrUtil.escapeHTMLTags((String) value));
			}
		}

		return (M) this;
	}

	protected Class<? extends Model> _getUserClass() {
		Class c = getClass();
		return c.getName().indexOf("$$EnhancerBy") == -1 ? c : c.getSuperclass();
	}

	protected M _use(String configName) {
		if (attrs == DaoContainerFactory.daoMap) {
			throw new RuntimeException("dao 只允许调用查询方法");
		}

		this.configName = configName;
		return (M)this;
	}

	public M set(String attr, Object value) {
		Table table = _getTable();	
		if (table != null && !table.hasColumnLabel(attr)) {
			throw new DatabaseException("The attribute name does not exist: \"" + attr + "\"");
		}

		attrs.put(attr, value);
		_getModifyFlag().add(attr);	
		return (M)this;
	}

	public M set(Column attr, Object value) {
		return set(attr.get(), value);
	}

	public M put(String key, Object value) {

		attrs.put(key, value);
		return (M)this;
	}

	public M setOrPut(String attrOrNot, Object value) {
		Table table = _getTable();
		if (table != null && table.hasColumnLabel(attrOrNot)) {
			_getModifyFlag().add(attrOrNot);	
		}

		attrs.put(attrOrNot, value);
		return (M)this;
	}

	public M _setOrPut(Map<String, Object> map) {
		for (Entry<String, Object> e : map.entrySet()) {
			setOrPut(e.getKey(), e.getValue());
		}
		return (M)this;
	}

	public M _setOrPut(Model model) {
		return (M)_setOrPut(model._getAttrs());
	}

	public M put(Map<String, Object> map) {
		attrs.putAll(map);
		return (M)this;
	}

	public M put(Model model) {
		attrs.putAll(model._getAttrs());
		return (M)this;
	}

	public M put(Record record) {
		attrs.putAll(record.getColumns());
		return (M)this;
	}

	public Record toRecord() {
		return new Record(manager.config.containerFactory.getColumnsMap()).setColumns(_getAttrs());
	}

	public <T> T get(String attr) {
		return (T)(attrs.get(attr));
	}
	public <T> T get(Column field) { return get(field.get()); }

	public <T> T get(String attr, Object defaultValue) {
		Object result = attrs.get(attr);
		return (T)(result != null ? result : defaultValue);
	}
	public <T> T get(Column field, Object defaultValue) { return get(field.get(), defaultValue); }

	public String getStr(String attr) {

		Object s = attrs.get(attr);
		return s != null ? s.toString() : null;
	}
	public String getStr(Column field) { return getStr(field.get()); }

	public Integer getInt(String attr) {
		Number n = (Number)attrs.get(attr);
		return n != null ? n.intValue() : null;
	}
	public Integer getInt(Column field) { return getInt(field.get()); }

	public Long getLong(String attr) {
		Number n = (Number)attrs.get(attr);
		return n != null ? n.longValue() : null;
	}
	public Long getLong(Column field) { return getLong(field.get()); }

	public java.math.BigInteger getBigInteger(String attr) {
		return (java.math.BigInteger)attrs.get(attr);
	}
	public java.math.BigInteger getBigInteger(Column field){ return getBigInteger(field.get()); }

	public java.util.Date getDate(String attr) {
		Object ret = attrs.get(attr);

		if (ret instanceof Temporal) {
			if (ret instanceof LocalDateTime) {
				return DateUtil.toDate((LocalDateTime)ret);
			}
			if (ret instanceof LocalDate) {
				return DateUtil.toDate((LocalDate)ret);
			}
			if (ret instanceof LocalTime) {
				return DateUtil.toDate((LocalTime)ret);
			}
		}

		return (java.util.Date)ret;
	}
	public java.util.Date getDate(Column field) { return getDate(field.get()); }

	public LocalDateTime getLocalDateTime(String attr) {
		Object ret = attrs.get(attr);

		if (ret instanceof LocalDateTime) {
			return (LocalDateTime)ret;
		}
		if (ret instanceof LocalDate) {
			return ((LocalDate)ret).atStartOfDay();
		}
		if (ret instanceof LocalTime) {
			return LocalDateTime.of(LocalDate.now(), (LocalTime)ret);
		}
		if (ret instanceof java.util.Date) {
			return DateUtil.toLocalDateTime((java.util.Date)ret);
		}

		return (LocalDateTime)ret;
	}
	public LocalDateTime getLocalDateTime(Column field) { return getLocalDateTime(field.get()); }

	public java.sql.Time getTime(String attr) {
		return (java.sql.Time)attrs.get(attr);
	}
	public java.sql.Time getTime(Column field) { return getTime(field.get()); }

	public java.sql.Timestamp getTimestamp(String attr) {
		return (java.sql.Timestamp)attrs.get(attr);
	}
	public java.sql.Timestamp getTimestamp(Column field) { return getTimestamp(field.get()); }

	public Double getDouble(String attr) {
		Number n = (Number)attrs.get(attr);
		return n != null ? n.doubleValue() : null;
	}
	public Double getDouble(Column field) { return getDouble(field.get()); }

	public Float getFloat(String attr) {
		Number n = (Number)attrs.get(attr);
		return n != null ? n.floatValue() : null;
	}
	public Float getFloat(Column field) { return getFloat(field.get()); }

	public Short getShort(String attr) {
		Number n = (Number)attrs.get(attr);
		return n != null ? n.shortValue() : null;
	}
	public Short getShort(Column field) { return getShort(field.get()); }

	public Byte getByte(String attr) {
		Number n = (Number)attrs.get(attr);
		return n != null ? n.byteValue() : null;
	}
	public Byte getByte(Column field) { return getByte(field.get()); }

	public Boolean getBoolean(String attr) {
		return (Boolean)attrs.get(attr);
	}
	public Boolean getBoolean(Column field) { return getBoolean(field.get()); }

	public BigDecimal getBigDecimal(String attr) {
		Object n = attrs.get(attr);
		if (n instanceof BigDecimal) {
			return (BigDecimal)n;
		} else if (n != null) {
			return new BigDecimal(n.toString());
		} else {
			return null;
		}
	}
	public java.math.BigDecimal getBigDecimal(Column field){ return getBigDecimal(field.get()); }

	public byte[] getBytes(String attr) {
		return (byte[])attrs.get(attr);
	}
	public byte[] getBytes(Column field) { return getBytes(field.get()); }

	public Number getNumber(String attr) {
		return (Number)attrs.get(attr);
	}
	public Number getNumber(Column field) { return getNumber(field.get()); }

	public Page<M> paginate(int pageNumber, int pageSize, String select, String sqlExceptSelect, Object... params) {
		return doPaginate(pageNumber, pageSize, null, select, sqlExceptSelect, params);
	}

	public Page<M> paginate(int pageNumber, int pageSize, String select, String sqlExceptSelect) {
		return doPaginate(pageNumber, pageSize, null, select, sqlExceptSelect, NULL_PARAM_ARRAY);
	}

	public Page<M> paginate(int pageNumber, int pageSize, boolean isGroupBySql, String select, String sqlExceptSelect, Object... params) {
		return doPaginate(pageNumber, pageSize, isGroupBySql, select, sqlExceptSelect, params);
	}

	private Page<M> doPaginate(int pageNumber, int pageSize, Boolean isGroupBySql, String select, String sqlExceptSelect, Object... params) {
		Config config = _getConfig();
		Connection conn = null;
		try {
			conn = config.getConnection();
			String totalRowSql = config.dialect.forPaginateTotalRow(select, sqlExceptSelect, this);
			StringBuilder findSql = new StringBuilder();
			findSql.append(select).append(' ').append(sqlExceptSelect);
			return doPaginateByFullSql(config, conn, pageNumber, pageSize, isGroupBySql, totalRowSql, findSql, params);
		} catch (Exception e) {
			throw new DatabaseException(e);
		} finally {
			manager.close(conn);
		}
	}

	private Page<M> doPaginateByFullSql(Config config, Connection conn, int pageNumber, int pageSize, Boolean isGroupBySql, String totalRowSql, StringBuilder findSql, Object... params) throws Exception {
		if (pageNumber < 1 || pageSize < 1) {
			throw new DatabaseException("pageNumber and pageSize must be greater than 0");
		}

		if (config.dialect.isTakeOverModelPaginate()) {
			return config.dialect.takeOverModelPaginate(manager.db.use(), config, conn, _getUserClass(), pageNumber, pageSize, isGroupBySql, totalRowSql, findSql, params);
		}

		List result = manager.db.use().query(config, conn, totalRowSql, params);
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
			return new Page<M>(new ArrayList<M>(0), pageNumber, pageSize, 0, 0);	
		}

		int totalPage = (int) (totalRow / pageSize);
		if (totalRow % pageSize != 0) {
			totalPage++;
		}

		if (pageNumber > totalPage) {
			return new Page<M>(new ArrayList<M>(0), pageNumber, pageSize, totalPage, (int)totalRow);
		}

		String sql = config.dialect.forPaginate(pageNumber, pageSize, findSql);
		List<M> list = find(config, conn, sql, params);
		return new Page<M>(list, pageNumber, pageSize, totalPage, (int)totalRow);
	}

	private Page<M> doPaginateByFullSql(int pageNumber, int pageSize, Boolean isGroupBySql, String totalRowSql, String findSql, Object... params) {
		Config config = _getConfig();
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

	public Page<M> paginateByFullSql(int pageNumber, int pageSize, String totalRowSql, String findSql, Object... params) {
		return doPaginateByFullSql(pageNumber, pageSize, null, totalRowSql, findSql, params);
	}

	public Page<M> paginateByFullSql(int pageNumber, int pageSize, boolean isGroupBySql, String totalRowSql, String findSql, Object... params) {
		return doPaginateByFullSql(pageNumber, pageSize, isGroupBySql, totalRowSql, findSql, params);
	}

	public M audit(String className, String methodName){
		manager.getAuditManager().audit(className, methodName);
		return (M)this;
	}

	M skipAudit(){
		manager.getAuditManager().skipAudit();
		return (M)this;
	}

	private boolean _save() {
		filter(FILTER_BY_SAVE);

		Config config = _getConfig();
		Table table = _getTable();

		StringBuilder sql = new StringBuilder();
		List<Object> params = new ArrayList<Object>();
		config.dialect.forModelSave(table, attrs, sql, params);

		Connection conn = null;
		PreparedStatement pst = null;
		int result = 0;
		try {
			conn = config.getConnection();
			if (config.dialect.isOracle()) {
				pst = conn.prepareStatement(sql.toString(), table.getPrimaryKey());
			} else {
				pst = conn.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS);
			}
			config.dialect.fillStatement(pst, params);
			result = pst.executeUpdate();
			config.dialect.getModelGeneratedKey(this, pst, table);
			_getModifyFlag().clear();
			return result >= 1;
		} catch (Exception e) {
			throw new DatabaseException(e);
		} finally {
			manager.close(pst, conn);
		}
	}

	public boolean save() {

		if (_hasColumn(manager.getModelConfig().getCreateTimeColumn()) && get(manager.getModelConfig().getCreateTimeColumn()) == null) {
			set(manager.getModelConfig().getCreateTimeColumn(), Ready.now());
		}
		if (_hasColumn(manager.getModelConfig().getCreateUserColumn()) && get(manager.getModelConfig().getCreateUserColumn()) == null) {
			CurrentUser<? extends UserIdentity> currentUser = Ready.beanManager().getCurrentUser();
			if(currentUser != null) {
				set(manager.getModelConfig().getCreateUserColumn(), currentUser.getOnlineUser().getUserId());
			}
		}

		if(null == get(_getPrimaryKey()) && _getPrimaryKeys().length == 1){
			if(String.class == _getPrimaryType()){
				set(_getPrimaryKey(), StrUtil.getRandomUUID());
			} else if(Long.class == _getPrimaryType() || java.math.BigInteger.class == _getPrimaryType()){
				set(_getPrimaryKey(), Ready.getId());
			}
		}

		return _save();
	}

	public boolean saveOrUpdate() {
		if (null == _getIdValue()) {
			return this.save();
		}
		return this.update();
	}

	public M findById(Object idValue) {
		if (idValue == null) {
			return null;
		}
		return _findById(idValue);
	}

	public M findByIds(Object... idValues) {
		if (idValues == null) {
			return null;
		}
		if (idValues.length != _getPrimaryKeys().length) {
			throw new IllegalArgumentException("idValues.length != _getPrimaryKeys().length");
		}
		return _findByIds(idValues);
	}

	public boolean update() {
		if (_hasColumn(manager.getModelConfig().getUpdateTimeColumn())) {
			set(manager.getModelConfig().getUpdateTimeColumn(), Ready.now());
		}

		if (_hasColumn(manager.getModelConfig().getUpdateUserColumn())) {
			CurrentUser<? extends UserIdentity> currentUser = Ready.beanManager().getCurrentUser();
			if(currentUser != null) {
				set(manager.getModelConfig().getUpdateUserColumn(), currentUser.getOnlineUser().getUserId());
			}
		}

		return _update();
	}

	private Object[] getPrimaryVal(){
		Table table = _getTable();
		String[] pKeys = table.getPrimaryKey();
		Object[] ids = new Object[pKeys.length];
		for (int i=0; i<pKeys.length; i++) {
			ids[i] = attrs.get(pKeys[i]);
			if (ids[i] == null) {
				throw new DatabaseException("Primary key " + pKeys[i] + " can not be null");
			}
		}
		return ids;
	}

	public boolean delete() {
		Object[] ids = getPrimaryVal();
		return deleteById(_getTable(), ids);
	}

	public boolean deleteById(Object idValue) {
		if (idValue == null) {
			throw new IllegalArgumentException("idValue can not be null");
		}
		return deleteById(_getTable(), idValue);
	}

	public boolean deleteByIds(Object... idValues) {
		Table table = _getTable();
		if (idValues == null || idValues.length != table.getPrimaryKey().length) {
			throw new IllegalArgumentException("Primary key number must equals id value number and can not be null");
		}
		return deleteById(table, idValues);
	}

	private boolean deleteById(Table table, Object... idValues) {
		Config config = _getConfig();
		Connection conn = null;
		try {
			String sql = config.dialect.forModelDeleteById(table);
			conn = config.getConnection();
			PreparedStatement pst = conn.prepareStatement(sql);
			config.dialect.fillStatement(pst, idValues);
			int result = pst.executeUpdate();
			return result >= 1;
		} catch (Exception e) {
			throw new DatabaseException(e);
		} finally {
			manager.close(conn);
		}
	}

	private boolean _update() {
		filter(FILTER_BY_UPDATE);

		if (_getModifyFlag().isEmpty()) {
			return false;
		}

		Table table = _getTable();
		String[] pKeys = table.getPrimaryKey();
		for (String pKey : pKeys) {
			Object id = attrs.get(pKey);
			if (id == null) {
				throw new DatabaseException("You can't update model without Primary Key, " + pKey + " can not be null.");
			}
		}

		Config config = _getConfig();
		StringBuilder sql = new StringBuilder();
		List<Object> params = new ArrayList<Object>();
		config.dialect.forModelUpdate(table, attrs, _getModifyFlag(), sql, params);

		if (params.size() <= 1) {	
			return false;
		}

		Connection conn = null;
		try {
			conn = config.getConnection();
			PreparedStatement pst = conn.prepareStatement(sql.toString());
			config.dialect.fillStatement(pst, params);
			int result = pst.executeUpdate();

			if (result >= 1) {
				_getModifyFlag().clear();
				return true;
			}
			return false;
		} catch (Exception e) {
			throw new DatabaseException(e);
		} finally {
			manager.close(conn);
		}
	}

	private List<M> find(Config config, Connection conn, String sql, Object... params) throws Exception {
		try (PreparedStatement pst = conn.prepareStatement(sql)) {
			config.dialect.fillStatement(pst, params);
			ResultSet rs = pst.executeQuery();
			List<M> result = config.dialect.buildModelList(rs, _getUserClass());	
			manager.close(rs);
			return result;
		}
	}

	protected List<M> find(Config config, String sql, Object... params) {
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

	public M findTop1(String findSql, Object... params) {
		List<M> result = findTop(1, findSql, params);
		return result.size() > 0 ? result.get(0) : null;
	}

	public List<M> findTop(int size, String findSql, Object... params) {
		Config config = _getConfig();
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

	public List<M> find(String sql, Object... params) {
		return find(_getConfig(), sql, params);
	}

	public List<M> find(String sql) {
		return find(sql, NULL_PARAM_ARRAY);
	}

	public List<M> findAll() {
		Config config = _getConfig();
		String sql = config.dialect.forFindAll(_getTable().getName());
		return find(config, sql, NULL_PARAM_ARRAY);
	}

	public M findFirst(String sql, Object... params) {
		List<M> result = find(sql, params);
		return result.size() > 0 ? result.get(0) : null;
	}

	public M findFirst(String sql) {
		return findFirst(sql, NULL_PARAM_ARRAY);
	}

	private M _findById(Object idValue) {
		return findByIdLoadColumns(new Object[]{idValue}, "*");
	}

	private M _findByIds(Object... idValues) {
		return findByIdLoadColumns(idValues, "*");
	}

	public M findByIdLoadColumns(Object idValue, String columns) {
		return findByIdLoadColumns(new Object[]{idValue}, columns);
	}

	public M findByIdLoadColumns(Object[] idValues, String columns) {
		Table table = _getTable();
		if (table.getPrimaryKey().length != idValues.length) {
			throw new IllegalArgumentException("id values error, need " + table.getPrimaryKey().length + " id value");
		}
		Config config = _getConfig();
		String sql = config.dialect.forModelFindById(table, columns);
		List<M> result = find(config, sql, idValues);
		return result.size() > 0 ? result.get(0) : null;
	}

	public M remove(String attr) {
		attrs.remove(attr);
		_getModifyFlag().remove(attr);
		return (M)this;
	}
	public M remove(Column field) { return remove(field.get()); }

	public M remove(String... attrs) {
		if (attrs != null) {
			for (String a : attrs) {
				this.attrs.remove(a);
				this._getModifyFlag().remove(a);
			}
		}
		return (M)this;
	}
	public M remove(Column... fields) {
		if (fields != null) {
			for (Column a : fields) {
				this.attrs.remove(a.get());
				this._getModifyFlag().remove(a.get());
			}
		}
		return (M)this;
	}

	public M removeNullValueAttrs() {
		for (Iterator<Entry<String, Object>> it = attrs.entrySet().iterator(); it.hasNext();) {
			Entry<String, Object> e = it.next();
			if (e.getValue() == null) {
				it.remove();
				_getModifyFlag().remove(e.getKey());
			}
		}
		return (M)this;
	}

	public M keep(String... attrs) {
		if (attrs != null && attrs.length > 0) {
			Config config = _getConfig();
			Map<String, Object> newAttrs = config.containerFactory.getAttrsMap();	
			Set<String> newModifyFlag = config.containerFactory.getModifyFlagSet();	
			for (String a : attrs) {
				if (this.attrs.containsKey(a)) {    
					newAttrs.put(a, this.attrs.get(a));
				}
				if (this._getModifyFlag().contains(a)) {
					newModifyFlag.add(a);
				}
			}
			this.attrs = newAttrs;
			this.modifyFlag = newModifyFlag;
		}
		else {
			this.attrs.clear();
			this._getModifyFlag().clear();
		}
		return (M)this;
	}

	public M keep(String attr) {
		if (attrs.containsKey(attr)) {	
			Object keepIt = attrs.get(attr);
			boolean keepFlag = _getModifyFlag().contains(attr);
			attrs.clear();
			_getModifyFlag().clear();
			attrs.put(attr, keepIt);
			if (keepFlag) {
				_getModifyFlag().add(attr);
			}
		}
		else {
			attrs.clear();
			_getModifyFlag().clear();
		}
		return (M)this;
	}

	public M clear() {
		attrs.clear();
		_getModifyFlag().clear();
		return (M)this;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		boolean first = true;
		for (Entry<String, Object> e : attrs.entrySet()) {
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}
			Object value = e.getValue();
			if (value != null) {
				value = value.toString();
			}
			sb.append(e.getKey()).append(':').append(value);
		}
		sb.append('}');
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Model)) {
			return false;
		}
		if (o == this) {
			return true;
		}
		Model mo = (Model)o;
		if (getClass() != mo.getClass()) {
			return false;
		}
		return attrs.equals(mo.attrs);
	}

	@Override
	public int hashCode() {

		return attrs.hashCode();
	}

	public List<M> findByCache(String cacheName, Object key, String sql, Object... params) {
		Config config = _getConfig();
		Cache cache = config.getCache();
		List<M> result = cache.get(cacheName, key);
		if (result == null) {
			result = find(config, sql, params);
			cache.put(cacheName, key, result);
		}
		return result;
	}

	public List<M> findByCache(String cacheName, Object key, String sql) {
		return findByCache(cacheName, key, sql, NULL_PARAM_ARRAY);
	}

	public M findFirstByCache(String cacheName, Object key, String sql, Object... params) {
		Cache cache = _getConfig().getCache();
		M result = cache.get(cacheName, key);
		if (result == null) {
			result = findFirst(sql, params);
			cache.put(cacheName, key, result);
		}
		return result;
	}

	public M findFirstByCache(String cacheName, Object key, String sql) {
		return findFirstByCache(cacheName, key, sql, NULL_PARAM_ARRAY);
	}

	public Page<M> paginateByCache(String cacheName, Object key, int pageNumber, int pageSize, String select, String sqlExceptSelect, Object... params) {
		return doPaginateByCache(cacheName, key, pageNumber, pageSize, null, select, sqlExceptSelect, params);
	}

	public Page<M> paginateByCache(String cacheName, Object key, int pageNumber, int pageSize, String select, String sqlExceptSelect) {
		return doPaginateByCache(cacheName, key, pageNumber, pageSize, null, select, sqlExceptSelect, NULL_PARAM_ARRAY);
	}

	public Page<M> paginateByCache(String cacheName, Object key, int pageNumber, int pageSize, boolean isGroupBySql, String select, String sqlExceptSelect, Object... params) {
		return doPaginateByCache(cacheName, key, pageNumber, pageSize, isGroupBySql, select, sqlExceptSelect, params);
	}

	private Page<M> doPaginateByCache(String cacheName, Object key, int pageNumber, int pageSize, Boolean isGroupBySql, String select, String sqlExceptSelect, Object... params) {
		Cache cache = _getConfig().getCache();
		Page<M> result = cache.get(cacheName, key);
		if (result == null) {
			result = doPaginate(pageNumber, pageSize, isGroupBySql, select, sqlExceptSelect, params);
			cache.put(cacheName, key, result);
		}
		return result;
	}

	public String toJson() {
		return Json.getJson().toJson(attrs);
	}

	public String getSql(String key) {
		return _getConfig().getSqlKit().getSql(key);
	}

	public SqlParam getSqlParam(String key, Map data) {
		return _getConfig().getSqlKit().getSqlParam(key, data);
	}

	public SqlParam getSqlParam(String key, Object... params) {
		return _getConfig().getSqlKit().getSqlParam(key, params);
	}

	public SqlParam getSqlParam(String key, Model model) {
		return getSqlParam(key, model.attrs);
	}

	public SqlParam getSqlParamByString(String content, Map data) {
		return _getConfig().getSqlKit().getSqlParamByString(content, data);
	}

	public SqlParam getSqlParamByString(String content, Object... params) {
		return _getConfig().getSqlKit().getSqlParamByString(content, params);
	}

	public SqlParam getSqlParamByString(String content, Model model) {
		return getSqlParamByString(content, model.attrs);
	}

	public List<M> find(SqlParam sqlParam) {
		return find(sqlParam.getSql(), sqlParam.getParam());
	}

	public M findFirst(SqlParam sqlParam) {
		return findFirst(sqlParam.getSql(), sqlParam.getParam());
	}

	public Page<M> paginate(int pageNumber, int pageSize, SqlParam sqlParam) {
		String[] sqls = PageSqlKit.parsePageSql(sqlParam.getSql());
		return doPaginate(pageNumber, pageSize, null, sqls[0], sqls[1], sqlParam.getParam());
	}

	public Page<M> paginate(int pageNumber, int pageSize, boolean isGroupBySql, SqlParam sqlParam) {
		String[] sqls = PageSqlKit.parsePageSql(sqlParam.getSql());
		return doPaginate(pageNumber, pageSize, isGroupBySql, sqls[0], sqls[1], sqlParam.getParam());
	}

	public void each(Function<M, Boolean> func, String sql, Object... params) {
		Config config = _getConfig();
		Connection conn = null;
		try {
			conn = config.getConnection();

			try (PreparedStatement pst = conn.prepareStatement(sql)) {
				config.dialect.fillStatement(pst, params);
				ResultSet rs = pst.executeQuery();
				config.dialect.eachModel(rs, _getUserClass(), func);
				manager.close(rs);
			}

		} catch (Exception e) {
			throw new DatabaseException(e);
		} finally {
			manager.close(conn);
		}
	}

	public DaoTemplate<M> template(String key, Map data) {
		return new DaoTemplate(this, key, data);
	}

	public DaoTemplate<M> template(String key, Object... params) {
		return new DaoTemplate(this, key, params);
	}

	public DaoTemplate<M> template(String key, Model model) {
		return template(key, model.attrs);
	}

	public DaoTemplate<M> templateByString(String content, Map data) {
		return new DaoTemplate(true, this, content, data);
	}

	public DaoTemplate<M> templateByString(String content, Object... params) {
		return new DaoTemplate(true, this, content, params);
	}

	public DaoTemplate<M> templateByString(String content, Model model) {
		return templateByString(content, model.attrs);
	}

	public interface Column{
		String get();
	}

	public interface Status {
		int NORMAL = 1;
		int INVALID = 0;
	}
}

