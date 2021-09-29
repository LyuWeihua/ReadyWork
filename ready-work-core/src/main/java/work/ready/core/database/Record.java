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

import work.ready.core.ioc.annotation.Scope;
import work.ready.core.ioc.annotation.ScopeType;
import work.ready.core.security.data.CallerInspector;
import work.ready.core.server.Ready;
import work.ready.core.tools.DateUtil;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.Temporal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static work.ready.core.tools.StrUtil.toCamelCase;

@Scope(ScopeType.prototype)
public class Record implements Serializable {

	private static final long serialVersionUID = 905784513600884082L;

	private Map<String, Object> columns;

	public Record(Map<String, Object> columnMap){
		this.columns = columnMap;
	}

	Record setColumnsMap(Map<String, Object> columnMap) {
		this.columns = columnMap;
		return this;
	}

	Record setColumnsMap(Map<String, Object> columnMap, boolean keepOldData) {
		if(keepOldData){
			Map<String, Object> oldData = columns;
			this.columns = columnMap;
			this.columns.putAll(oldData);
		} else {
			this.columns = columnMap;
		}
		return this;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> getColumns() {
		if (columns == null) {
			columns = ContainerFactory.defaultContainerFactory.getColumnsMap();
		}
		return columns;
	}

	public Map<String, Object> getData(boolean camelCase) {
		Map<String, Object> dataMap = new LinkedHashMap<>(getColumns());
		if(Ready.dbManager().getDataSecurityInspector() != null){
			CallerInspector callerInspector = new CallerInspector();
			dataMap = Ready.dbManager().getDataSecurityInspector().outputExamine(callerInspector.getCallerClassName(), callerInspector.getCallerMethodName(), null, null, dataMap);
		}
		if(camelCase){
			Map<String, Object> camelCaseMap = new LinkedHashMap<>();
			dataMap.forEach((key, val)->camelCaseMap.put(toCamelCase(key), val));
			return camelCaseMap;
		} else {
			return dataMap;
		}
	}

	public Record setColumns(Map<String, Object> columns) {
		this.getColumns().putAll(columns);
		return this;
	}

	public Record setColumns(Record record) {
		getColumns().putAll(record.getColumns());
		return this;
	}

	public Record setColumns(Model<?> model) {
		getColumns().putAll(model._getAttrs());
		return this;
	}

	public Record remove(String column) {
		getColumns().remove(column);
		return this;
	}

	public Record remove(String... columns) {
		if (columns != null)
			for (String c : columns)
				this.getColumns().remove(c);
		return this;
	}

	public Record removeNullValueColumns() {
		for (java.util.Iterator<Entry<String, Object>> it = getColumns().entrySet().iterator(); it.hasNext();) {
			Entry<String, Object> e = it.next();
			if (e.getValue() == null) {
				it.remove();
			}
		}
		return this;
	}

	public Record keep(String... columns) {
		if (columns != null && columns.length > 0) {
			Map<String, Object> newColumns = new HashMap<String, Object>(columns.length);	
			for (String c : columns)
				if (this.getColumns().containsKey(c))	
					newColumns.put(c, this.getColumns().get(c));

			this.getColumns().clear();
			this.getColumns().putAll(newColumns);
		}
		else
			this.getColumns().clear();
		return this;
	}

	public Record keep(String column) {
		if (getColumns().containsKey(column)) {	
			Object keepIt = getColumns().get(column);
			getColumns().clear();
			getColumns().put(column, keepIt);
		}
		else
			getColumns().clear();
		return this;
	}

	public Record clear() {
		getColumns().clear();
		return this;
	}

	public Record set(String column, Object value) {
		getColumns().put(column, value);
		return this;
	}

	@SuppressWarnings("unchecked")
	public <T> T get(String column) {
		return (T)getColumns().get(column);
	}

	@SuppressWarnings("unchecked")
	public <T> T get(String column, Object defaultValue) {
		Object result = getColumns().get(column);
		return (T)(result != null ? result : defaultValue);
	}

	public Object getObject(String column) {
		return getColumns().get(column);
	}

	public Object getObject(String column, Object defaultValue) {
		Object result = getColumns().get(column);
		return result != null ? result : defaultValue;
	}

	public String getStr(String column) {

		Object s = getColumns().get(column);
		return s != null ? s.toString() : null;
	}

	public Integer getInt(String column) {
		Number n = getNumber(column);
		return n != null ? n.intValue() : null;
	}

	public Long getLong(String column) {
		Number n = getNumber(column);
		return n != null ? n.longValue() : null;
	}

	public java.math.BigInteger getBigInteger(String column) {
		return (java.math.BigInteger)getColumns().get(column);
	}

	public java.util.Date getDate(String column) {
		Object ret = getColumns().get(column);

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

	public LocalDateTime getLocalDateTime(String column) {
		Object ret = getColumns().get(column);

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

	public java.sql.Time getTime(String column) {
		return (java.sql.Time)getColumns().get(column);
	}

	public java.sql.Timestamp getTimestamp(String column) {
		return (java.sql.Timestamp)getColumns().get(column);
	}

	public Double getDouble(String column) {
		Number n = getNumber(column);
		return n != null ? n.doubleValue() : null;
	}

	public Float getFloat(String column) {
		Number n = getNumber(column);
		return n != null ? n.floatValue() : null;
	}

	public Short getShort(String column) {
		Number n = getNumber(column);
		return n != null ? n.shortValue() : null;
	}

	public Byte getByte(String column) {
		Number n = getNumber(column);
		return n != null ? n.byteValue() : null;
	}

	public Boolean getBoolean(String column) {
		return (Boolean)getColumns().get(column);
	}

	public BigDecimal getBigDecimal(String column) {
		Object n = getColumns().get(column);
		if (n instanceof BigDecimal) {
			return (BigDecimal)n;
		} else if (n != null) {
			return new BigDecimal(n.toString());
		} else {
			return null;
		}
	}

	public byte[] getBytes(String column) {
		return (byte[])getColumns().get(column);
	}

	public Number getNumber(String column) {
		return (Number)getColumns().get(column);
	}

	public String toString() {
		if (columns == null) {
			return "{}";
		}
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		boolean first = true;
		for (Entry<String, Object> e : getColumns().entrySet()) {
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

	public boolean equals(Object o) {
		if (!(o instanceof Record))
			return false;
		if (o == this)
			return true;
		return getColumns().equals(((Record)o).getColumns());
	}

	public int hashCode() {
		return getColumns().hashCode();
	}

	public String[] getColumnNames() {
		Set<String> attrNameSet = getColumns().keySet();
		return attrNameSet.toArray(new String[attrNameSet.size()]);
	}

	public Object[] getColumnValues() {
		java.util.Collection<Object> attrValueCollection = getColumns().values();
		return attrValueCollection.toArray(new Object[attrValueCollection.size()]);
	}

	public String toJson() {
		return work.ready.core.json.Json.getJson().toJson(getColumns());
	}
}

