/**
 *
 * Copyright (c) 2020 WeiHua Lyu [ready.work]
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

package work.ready.core.database.dialect;

import work.ready.core.database.Record;
import work.ready.core.database.Table;
import work.ready.core.database.builder.TimestampProcessedModelBuilder;
import work.ready.core.database.builder.TimestampProcessedRecordBuilder;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class H2Dialect extends Dialect {

    public H2Dialect(){
        this.modelBuilder = new TimestampProcessedModelBuilder();
        this.recordBuilder = new TimestampProcessedRecordBuilder();
        this.recordBuilder.setModelBuilder(modelBuilder);
    }

    @Override
    public String forTableBuilderDoBuild(String tableName) {
        return "select * from " + tableName + " where rownum < 1";
    }

    @Override
    public String forPaginate(int pageNumber, int pageSize, StringBuilder findSql) {
        int start = (pageNumber - 1) * pageSize;
        int end = pageNumber * pageSize;
        StringBuilder ret = new StringBuilder();
        ret.append("select * from ( select row_.*, rownum rownum_ from (  ");
        ret.append(findSql);
        ret.append(" ) row_ where rownum <= ").append(end).append(") table_alias");
        ret.append(" where table_alias.rownum_ > ").append(start);
        return ret.toString();
    }

    @Override
    public String forModelFindById(Table table, String columns) {
        StringBuilder sql = new StringBuilder("select ").append(columns).append(" from ");
        sql.append(table.getName());
        sql.append(" where ");
        String[] pKeys = table.getPrimaryKey();
        for (int i=0; i<pKeys.length; i++) {
            if (i > 0) {
                sql.append(" and ");
            }
            sql.append(pKeys[i]).append(" = ?");
        }
        return sql.toString();
    }

    @Override
    public String forModelDeleteById(Table table) {
        String[] pKeys = table.getPrimaryKey();
        StringBuilder sql = new StringBuilder(45);
        sql.append("delete from ");
        sql.append(table.getName());
        sql.append(" where ");
        for (int i=0; i<pKeys.length; i++) {
            if (i > 0) {
                sql.append(" and ");
            }
            sql.append(pKeys[i]).append(" = ?");
        }
        return sql.toString();
    }

    @Override
    public void forModelSave(Table table, Map<String, Object> attrs, StringBuilder sql, List<Object> params) {

        sql.append("insert into ").append(table.getName()).append("(");
        StringBuilder temp = new StringBuilder(") values(");
        String[] pKeys = table.getPrimaryKey();
        int count = 0;
        for (Entry<String, Object> e: attrs.entrySet()) {
            String colName = e.getKey();
            if (table.hasColumnLabel(colName)) {
                if (count++ > 0) {
                    sql.append(", ");
                    temp.append(", ");
                }
                sql.append(colName);
                Object value = e.getValue();
                if (value instanceof String && isPrimaryKey(colName, pKeys) && ((String)value).endsWith(".nextval")) {
                    temp.append(value);
                } else {
                    temp.append("?");
                    params.add(value);
                }
            }
        }
        sql.append(temp.toString()).append(")");
    }

    @Override
    public void forModelUpdate(Table table, Map<String, Object> attrs, Set<String> modifyFlag, StringBuilder sql,
                               List<Object> params) {

        sql.append("update ").append(table.getName()).append(" set ");
        String[] pKeys = table.getPrimaryKey();
        for (Entry<String, Object> e : attrs.entrySet()) {
            String colName = e.getKey();
            if (modifyFlag.contains(colName) && !isPrimaryKey(colName, pKeys) && table.hasColumnLabel(colName)) {
                if (params.size() > 0) {
                    sql.append(", ");
                }
                sql.append(colName).append(" = ? ");
                params.add(e.getValue());
            }
        }
        sql.append(" where ");
        for (int i=0; i<pKeys.length; i++) {
            if (i > 0) {
                sql.append(" and ");
            }
            sql.append(pKeys[i]).append(" = ?");
            params.add(attrs.get(pKeys[i]));
        }
    }

    @Override
    public String forDbFindById(String tableName, String[] pKeys) {
        tableName = tableName.trim();
        trimPrimaryKeys(pKeys);

        StringBuilder sql = new StringBuilder("select * from ").append(tableName).append(" where ");
        for (int i=0; i<pKeys.length; i++) {
            if (i > 0) {
                sql.append(" and ");
            }
            sql.append(pKeys[i]).append(" = ?");
        }
        return sql.toString();
    }

    @Override
    public String forDbDeleteById(String tableName, String[] pKeys) {
        tableName = tableName.trim();
        trimPrimaryKeys(pKeys);

        StringBuilder sql = new StringBuilder("delete from ").append(tableName).append(" where ");
        for (int i=0; i<pKeys.length; i++) {
            if (i > 0) {
                sql.append(" and ");
            }
            sql.append(pKeys[i]).append(" = ?");
        }
        return sql.toString();
    }

    @Override
    public void forDbSave(String tableName, String[] pKeys, Record record, StringBuilder sql, List<Object> params) {
        tableName = tableName.trim();
        trimPrimaryKeys(pKeys);

        sql.append("insert into ");
        sql.append(tableName).append("(");
        StringBuilder temp = new StringBuilder();
        temp.append(") values(");

        int count = 0;
        for (Entry<String, Object> e: record.getColumns().entrySet()) {
            String colName = e.getKey();
            if (count++ > 0) {
                sql.append(", ");
                temp.append(", ");
            }
            sql.append(colName);

            Object value = e.getValue();
            if (value instanceof String && isPrimaryKey(colName, pKeys) && ((String)value).endsWith(".nextval")) {
                temp.append(value);
            } else {
                temp.append("?");
                params.add(value);
            }
        }
        sql.append(temp.toString()).append(")");
    }

    @Override
    public void forDbUpdate(String tableName, String[] pKeys, Object[] ids, Record record, StringBuilder sql,
                            List<Object> params) {
        tableName = tableName.trim();
        trimPrimaryKeys(pKeys);

        sql.append("update ").append(tableName).append(" set ");
        for (Entry<String, Object> e: record.getColumns().entrySet()) {
            String colName = e.getKey();
            if (!isPrimaryKey(colName, pKeys)) {
                if (params.size() > 0) {
                    sql.append(", ");
                }
                sql.append(colName).append(" = ? ");
                params.add(e.getValue());
            }
        }
        sql.append(" where ");
        for (int i=0; i<pKeys.length; i++) {
            if (i > 0) {
                sql.append(" and ");
            }
            sql.append(pKeys[i]).append(" = ?");
            params.add(ids[i]);
        }

    }

    public boolean isH2() {
        return true;
    }

    @Override
    public void fillStatement(PreparedStatement pst, List<Object> params) throws SQLException {
        for (int i=0, size=params.size(); i<size; i++) {
            Object value = params.get(i);
            if (value instanceof java.sql.Date) {
                pst.setDate(i + 1, (java.sql.Date)value);
            } else if (value instanceof java.sql.Timestamp) {
                pst.setTimestamp(i + 1, (java.sql.Timestamp)value);
            } else {
                pst.setObject(i + 1, value);
            }
        }
    }

    @Override
    public void fillStatement(PreparedStatement pst, Object... params) throws SQLException {
        for (int i=0; i<params.length; i++) {
            Object value = params[i];
            if (value instanceof java.sql.Date) {
                pst.setDate(i + 1, (java.sql.Date)value);
            } else if (value instanceof java.sql.Timestamp) {
                pst.setTimestamp(i + 1, (java.sql.Timestamp)value);
            } else {
                pst.setObject(i + 1, value);
            }
        }
    }

    @Override
    public String getDefaultPrimaryKey() {
        return "ID";
    }
}
