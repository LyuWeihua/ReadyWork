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

import work.ready.core.config.ConfigInjector;
import work.ready.core.database.datasource.DataSourceConfig;
import work.ready.core.tools.StrUtil;

import java.sql.*;
import java.util.*;

public class TableManager {

    private DatabaseManager manager;

    private final Map<Class<? extends Model<?>>, Table> modelToTableMap = new HashMap<Class<? extends Model<?>>, Table>(512, 0.5F);
    private final Map<String, Table> nameToTableMap = new HashMap<>(512, 0.5F);

    private JavaType javaType = new JavaType();

    void setManager(DatabaseManager manager){
        this.manager = manager;
    }

    public void putTable(Table table) {
        if (modelToTableMap.containsKey(table.getModelClass())) {
            throw new RuntimeException("Model mapping already exists : " + table.getModelClass().getName());
        }
        nameToTableMap.put(table.getDatasource() + "_" + table.getName(), table);
        modelToTableMap.put(table.getModelClass(), table);
    }

    @SuppressWarnings("rawtypes")
    public Table getTable(Class<? extends Model> modelClass) {
        return modelToTableMap.get(modelClass);
    }
    public Table getTable(String dataSource, String tableName) {
        return nameToTableMap.get(dataSource + "_" + tableName);
    }

    public TableManager addMapping(String tableName, String primaryKey, Class<? extends Model<?>> modelClass, Config config) {
        buildTable(new Table(tableName, primaryKey, modelClass), config);
        return this;
    }

    public TableManager addMapping(String tableName, String primaryKey, Class<? extends Model<?>> modelClass) {
        buildTable(new Table(tableName, primaryKey, modelClass), manager.config);
        return this;
    }

    public TableManager addMapping(String tableName, Class<? extends Model<?>> modelClass, Config config) {
        buildTable(new Table(tableName, modelClass), config);
        return this;
    }

    public TableManager addMapping(String tableName, Class<? extends Model<?>> modelClass) {
        buildTable(new Table(tableName, modelClass), manager.config);
        return this;
    }

    private void buildTable(Table table, Config config) {
        Table temp = null;
        Connection conn = null;
        try {
            conn = config.dataSource.getConnection();
            temp = table;
            doBuild(table, conn, config);
            putTable(table);
            manager.addModelToConfigMapping(table.getModelClass(), config);
        } catch (Exception e) {
            if (temp != null) {
                System.err.println("Can not create Table object, maybe the table " + temp.getName() + " is not exists in datasource: " + config.getName());
            }
            throw new DatabaseException(e);
        }
        finally {
            manager.close(conn);
        }
    }

    void buildTable(List<Table> tableList, Config config) {
        Table temp = null;
        Connection conn = null;
        try {
            conn = config.dataSource.getConnection();
            for (Table table : tableList) {
                temp = table;
                doBuild(table, conn, config);
                putTable(table);
                manager.addModelToConfigMapping(table.getModelClass(), config);
            }
        } catch (Exception e) {
            if (temp != null) {
                System.err.println("Can not create Table object, please make sure the table " + temp.getName() + " is exist.");
            }
            throw new DatabaseException(e);
        }
        finally {
            manager.close(conn);
        }
    }

    @SuppressWarnings("unchecked")
    private void doBuild(Table table, Connection conn, Config config) throws SQLException {
        table.setDatasource(config.getName());
        table.setColumnTypeMap(config.containerFactory.getAttrsMap());
        if (table.getPrimaryKey() == null) {
            table.setPrimaryKey(config.dialect.getDefaultPrimaryKey());
        }

        String sql = config.dialect.forTableBuilderDoBuild(table.getName());
        Statement stm = conn.createStatement();
        ResultSet rs = stm.executeQuery(sql);
        ResultSetMetaData rsmd = rs.getMetaData();

        for (int i=1; i<=rsmd.getColumnCount(); i++) {
            String colName = rsmd.getColumnName(i);
            String colClassName = rsmd.getColumnClassName(i);

            Class<?> clazz = javaType.getType(colClassName);
            if (clazz != null) {
                table.setColumnType(colName, clazz);
            }
            else {
                int type = rsmd.getColumnType(i);
                if (type == Types.BINARY || type == Types.VARBINARY || type == Types.BLOB) {
                    table.setColumnType(colName, byte[].class);
                }
                else if (type == Types.CLOB || type == Types.NCLOB) {
                    table.setColumnType(colName, String.class);
                }
                
                else if (type == Types.TIMESTAMP) {
                    table.setColumnType(colName, java.sql.Timestamp.class);
                }

                else if (type == Types.DATE) {
                    table.setColumnType(colName, java.sql.Date.class);
                }
                
                else if (type == Types.OTHER) {
                    table.setColumnType(colName, Object.class);
                }
                else {
                    table.setColumnType(colName, String.class);
                }

            }
        }

        rs.close();
        stm.close();
    }

    public static Table createTableInfoByAnnotation(Class<? extends Model<?>> clazz) {
        var tableAnnotation = clazz.getAnnotation(work.ready.core.database.annotation.Table.class);
        Table tableInfo = new Table();
        tableInfo.setModelClass(clazz);
        tableInfo.setPrimaryKey(ConfigInjector.getStringValue(tableAnnotation.primaryKey()));
        tableInfo.setName(ConfigInjector.getStringValue(tableAnnotation.tableName()));
        return tableInfo;
    }

    public static List<Table> getMatchedTables(DataSourceConfig dataSourceConfig, List<Table> tableList) {

        Set<String> configTables = StrUtil.notBlank(dataSourceConfig.getTable())
                ? StrUtil.splitToSet(dataSourceConfig.getTable(),",|;")
                : null;

        Set<String> ignoreTables = StrUtil.notBlank(dataSourceConfig.getIgnoreTable())
                ? StrUtil.splitToSet(dataSourceConfig.getIgnoreTable(),",|;")
                : null;

        List<Table> matchedList = new ArrayList<>();

        for (Table tableInfo : tableList) {

            if (tableInfo.getDatasource() != null) {
                continue;
            }

            if (configTables != null && !configTables.contains(tableInfo.getName())) {
                continue;
            }

            if (ignoreTables != null && ignoreTables.contains(tableInfo.getName())) {
                continue;
            }

            tableInfo.setDatasource(dataSourceConfig.getName());
            matchedList.add(tableInfo);
        }

        return matchedList;
    }
}
