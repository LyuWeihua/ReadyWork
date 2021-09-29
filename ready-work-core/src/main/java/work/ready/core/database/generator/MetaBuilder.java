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

package work.ready.core.database.generator;

import work.ready.core.database.dialect.Dialect;
import work.ready.core.tools.StrUtil;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.function.Predicate;

public class MetaBuilder {

	protected DataSource dataSource;
	protected Dialect dialect;
	protected Set<String> excludedTables = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

	protected Predicate<String> tableSkip = null;

	protected Connection conn = null;
	protected DatabaseMetaData dbMeta = null;

	protected String[] removedTableNamePrefixes = null;

	protected TypeMapping typeMapping = new TypeMapping();

	protected boolean generateRemarks = false;

	public MetaBuilder(DataSource dataSource, Dialect dialect) {
		if (dataSource == null) {
			throw new IllegalArgumentException("dataSource can not be null.");
		}
		if (dialect == null) {
			throw new IllegalArgumentException("dialect can not be null.");
		}
		this.dialect = dialect;
		this.dataSource = dataSource;
	}

	public void setGenerateRemarks(boolean generateRemarks) {
		this.generateRemarks = generateRemarks;
	}

	public void setDialect(Dialect dialect) {
		if (dialect != null) {
			this.dialect = dialect;
		}
	}

	public void addExcludedTable(String... excludedTables) {
		if (excludedTables != null) {
			for (String table : excludedTables) {
				this.excludedTables.add(table.trim());
			}
		}
	}

	public void setRemovedTableNamePrefixes(String... removedTableNamePrefixes) {
		this.removedTableNamePrefixes = removedTableNamePrefixes;
	}

	public void setTypeMapping(TypeMapping typeMapping) {
		if (typeMapping != null) {
			this.typeMapping = typeMapping;
		}
	}

	public List<TableMeta> build() {
		System.out.println("Build TableMeta ...");
		try {
			conn = dataSource.getConnection();
			dbMeta = conn.getMetaData();

			List<TableMeta> ret = new ArrayList<TableMeta>();
			buildTableNames(ret);
			for (TableMeta tableMeta : ret) {
				buildPrimaryKey(tableMeta);
				buildColumnMetas(tableMeta);
			}
			removeNoPrimaryKeyTable(ret);
			return ret;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		finally {
			if (conn != null) {
				try {conn.close();} catch (SQLException e) {throw new RuntimeException(e);}
			}
		}
	}

	protected void removeNoPrimaryKeyTable(List<TableMeta> ret) {
		for (java.util.Iterator<TableMeta> it = ret.iterator(); it.hasNext();) {
			TableMeta tm = it.next();
			if (StrUtil.isBlank(tm.primaryKey)) {
				it.remove();
				System.err.println("Skip table " + tm.name + " because there is no primary key");
			}
		}
	}

	protected boolean isSkipTable(String tableName) {
		return false;
	}

	public MetaBuilder skip(Predicate<String> tableSkip) {
		this.tableSkip = tableSkip;
		return this;
	}

	protected String buildModelName(String tableName) {

		if (removedTableNamePrefixes != null) {
			for (String prefix : removedTableNamePrefixes) {
				if (tableName.startsWith(prefix)) {
					tableName = tableName.replaceFirst(prefix, "");
					break;
				}
			}
		}

		if (dialect.isOracle()) {
			tableName = tableName.toLowerCase();
		}

		return StrUtil.firstCharToUpperCase(StrUtil.toCamelCase(tableName));
	}

	protected String buildBaseModelName(String modelName) {
		return "Base" + modelName;
	}

	protected ResultSet getTablesResultSet() throws SQLException {
		String schemaPattern = dialect.isOracle() ? dbMeta.getUserName() : null;

		return dbMeta.getTables(conn.getCatalog(), schemaPattern, null, new String[]{"TABLE"});	
	}

	protected void buildTableNames(List<TableMeta> ret) throws SQLException {
		ResultSet rs = getTablesResultSet();
		while (rs.next()) {
			String tableName = rs.getString("TABLE_NAME");

			if (excludedTables.contains(tableName)) {
				System.out.println("Skip table :" + tableName);
				continue ;
			}
			if (isSkipTable(tableName)) {
				System.out.println("Skip table :" + tableName);
				continue ;
			}

			if (tableSkip != null && tableSkip.test(tableName)) {
				System.out.println("Skip table :" + tableName);
				continue ;
			}

			TableMeta tableMeta = new TableMeta();
			tableMeta.name = tableName;
			tableMeta.remarks = rs.getString("REMARKS");

			tableMeta.modelName = buildModelName(tableName);
			tableMeta.baseModelName = buildBaseModelName(tableMeta.modelName);
			ret.add(tableMeta);
		}
		rs.close();
	}

	protected void buildPrimaryKey(TableMeta tableMeta) throws SQLException {
		ResultSet rs = dbMeta.getPrimaryKeys(conn.getCatalog(), null, tableMeta.name);

		String primaryKey = "";
		int index = 0;
		while (rs.next()) {
			String cn = rs.getString("COLUMN_NAME");

			if (primaryKey.equals(cn)) {
				continue ;
			}

			if (index++ > 0) {
				primaryKey += ",";
			}
			primaryKey += cn;
		}

		tableMeta.primaryKey = primaryKey;
		rs.close();
	}

	protected void buildColumnMetas(TableMeta tableMeta) throws SQLException {
		String sql = dialect.forTableBuilderDoBuild(tableMeta.name);
		Statement stm = conn.createStatement();
		ResultSet rs = stm.executeQuery(sql);
		ResultSetMetaData rsmd = rs.getMetaData();
		int columnCount = rsmd.getColumnCount();

		Map<String, ColumnMeta> columnMetaMap = new HashMap<>();
		if (generateRemarks) {
			ResultSet colMetaRs = null;
			try {
				colMetaRs = dbMeta.getColumns(conn.getCatalog(), null, tableMeta.name, null);
				while (colMetaRs.next()) {
					ColumnMeta columnMeta = new ColumnMeta();
					columnMeta.name = colMetaRs.getString("COLUMN_NAME");
					columnMeta.remarks = colMetaRs.getString("REMARKS");
					columnMetaMap.put(columnMeta.name, columnMeta);
				}
			} catch (Exception e) {
				System.out.println("无法生成 REMARKS");
			} finally {
				if (colMetaRs != null) {
					colMetaRs.close();
				}
			}
		}

		for (int i=1; i<=columnCount; i++) {
			ColumnMeta cm = new ColumnMeta();
			cm.name = rsmd.getColumnName(i);

			String typeStr = null;
			if (dialect.isKeepByteAndShort()) {
				int type = rsmd.getColumnType(i);
				if (type == Types.TINYINT) {
					typeStr = "java.lang.Byte";
				} else if (type == Types.SMALLINT) {
					typeStr = "java.lang.Short";
				}
			}

			if (typeStr == null) {
				String colClassName = rsmd.getColumnClassName(i);
				typeStr = typeMapping.getType(colClassName);
			}

			if (typeStr == null) {
				int type = rsmd.getColumnType(i);
				if (type == Types.BINARY || type == Types.VARBINARY || type == Types.LONGVARBINARY || type == Types.BLOB) {
					typeStr = "byte[]";
				} else if (type == Types.CLOB || type == Types.NCLOB) {
					typeStr = "java.lang.String";
				}

				else if (type == Types.TIMESTAMP || type == Types.DATE) {
					typeStr = "java.util.Date";
				}

				else if (type == Types.OTHER) {
					typeStr = "java.lang.Object";
				} else {
					typeStr = "java.lang.String";
				}
			}

			typeStr = handleJavaType(typeStr, rsmd, i);

			cm.javaType = typeStr;

			cm.attrName = buildAttrName(cm.name);

			if (generateRemarks && columnMetaMap.containsKey(cm.name)) {
				cm.remarks = columnMetaMap.get(cm.name).remarks;
			}

			tableMeta.columnMetas.add(cm);
		}

		rs.close();
		stm.close();
	}

	protected String handleJavaType(String typeStr, ResultSetMetaData rsmd, int column) throws SQLException {

		if ( ! dialect.isOracle() ) {
			return typeStr;
		}

		if ("java.math.BigDecimal".equals(typeStr)) {
			int scale = rsmd.getScale(column);			
			int precision = rsmd.getPrecision(column);	
			if (scale == 0) {
				if (precision <= 9) {
					typeStr = "java.lang.Integer";
				} else if (precision <= 18) {
					typeStr = "java.lang.Long";
				} else {
					typeStr = "java.math.BigDecimal";
				}
			} else {

				typeStr = "java.math.BigDecimal";
			}
		}

		if ("java.math.BigInteger".equals(typeStr)) {
			boolean isSigned = rsmd.isSigned(column);
			if (isSigned) {
				typeStr = "java.lang.Long";
			} else {
				typeStr = "java.math.BigInteger";
			}
		}

		return typeStr;
	}

	protected String buildAttrName(String colName) {
		if (dialect.isOracle() || StrUtil.isUpperCase(colName)) {
			colName = colName.toLowerCase();
		}
		return StrUtil.toCamelCase(colName);
	}
}

