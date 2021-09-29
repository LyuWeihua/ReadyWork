/**
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
 */
package work.ready.core.database.query;

import work.ready.core.database.handlers.*;
import work.ready.core.tools.CollectionUtil;

import javax.sql.DataSource;
import java.math.BigInteger;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdvancedQueryRunner {

    private final DataSource dataSource;
    private final QueryRunner queryRunner;

    public AdvancedQueryRunner(DataSource dataSource) {
        this.dataSource = dataSource;
        this.queryRunner = new QueryRunner(dataSource);
    }

    public AdvancedQueryRunner(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
        this.dataSource = queryRunner.getDataSource();
    }

    public int insert(String sql, Object[] params) throws SQLException {
        int affectedRows = 0;
        if (params == null) {
            affectedRows = queryRunner.update(sql);
        } else {
            affectedRows = queryRunner.update(sql, params);
        }
        return affectedRows;
    }

    public int insertForKeys(String sql, Object[] params) throws SQLException {
        int key = 0;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ParameterMetaData pmd = stmt.getParameterMetaData();
            if (params.length < pmd.getParameterCount()) {
                throw new SQLException("number of parameter doesn't match: " + params.length + " < " + pmd.getParameterCount());
            }
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            stmt.executeUpdate();
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                key = rs.getInt(1);
            }
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (stmt != null) {
                stmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
        return key;
    }

    private ScalarHandler scalarHandler = new ScalarHandler() {
        @Override
        public Object handle(ResultSet rs) throws SQLException {
            Object obj = super.handle(rs);
            if (obj instanceof BigInteger) {
                return ((BigInteger) obj).longValue();
            }
            return obj;
        }
    };

    public long count(String sql, Object... params) throws SQLException {
        Number num = 0;
        if (params == null) {
            num = (Number) queryRunner.query(sql, scalarHandler);
        } else {
            num = (Number) queryRunner.query(sql, scalarHandler, params);
        }
        return (num != null) ? num.longValue() : -1;
    }

    public int update(String sql) throws SQLException {
        return update(sql, null);
    }

    public int update(String sql, Object param) throws SQLException {
        return update(sql, new Object[] { param });
    }

    public int update(String sql, Object[] params) throws SQLException {
        int affectedRows = 0;
        if (params == null) {
            affectedRows = queryRunner.update(sql);
        } else {
            affectedRows = queryRunner.update(sql, params);
        }
        return affectedRows;
    }

    public int[] batchUpdate(String sql, Object[][] params) throws SQLException {
        int[] affectedRows = new int[0];
        affectedRows = queryRunner.batch(sql, params);
        return affectedRows;
    }

    public List<Map<String, Object>> find(String sql) throws SQLException {
        return find(sql, null);
    }

    public List<Map<String, Object>> find(String sql, Object param) throws SQLException {
        return find(sql, new Object[] { param });
    }

    public List<Map<String, Object>> findPage(String sql, int page, int count, Object... params) throws SQLException {
        sql = sql + " LIMIT ?,?";
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        if (params == null) {
            list = (List<Map<String, Object>>) queryRunner.query(sql, new MapListHandler(), new Integer[] { page, count });
        } else {
            list = (List<Map<String, Object>>) queryRunner.query(sql, new MapListHandler(), CollectionUtil.appendArray(
                    params, new Integer[] { page, count }));
        }
        return list;
    }

    public List<Map<String, Object>> find(String sql, Object[] params) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        if (params == null) {
            list = (List<Map<String, Object>>) queryRunner.query(sql, new MapListHandler());
        } else {
            list = (List<Map<String, Object>>) queryRunner.query(sql, new MapListHandler(), params);
        }
        return list;
    }

    public <T> List<T> find(Class<T> entityClass, String sql) throws SQLException {
        return find(entityClass, sql, null);
    }

    public <T> List<T> find(Class<T> entityClass, String sql, Object param) throws SQLException {
        return find(entityClass, sql, new Object[] { param });
    }

    public <T> List<T> find(Class<T> entityClass, String sql, Object[] params) throws SQLException {
        List<T> list = new ArrayList<T>();
        if (params == null) {
            list = (List<T>) queryRunner.query(sql, new BeanListHandler<>(entityClass));
        } else {
            list = (List<T>) queryRunner.query(sql, new BeanListHandler<>(entityClass), params);
        }
        return list;
    }

    public <T> T findFirst(Class<T> entityClass, String sql) throws SQLException {
        return findFirst(entityClass, sql, null);
    }

    public <T> T findFirst(Class<T> entityClass, String sql, Object param) throws SQLException {
        return findFirst(entityClass, sql, new Object[] { param });
    }

    public <T> T findFirst(Class<T> entityClass, String sql, Object[] params) throws SQLException {
        Object object = null;
        if (params == null) {
            object = queryRunner.query(sql, new BeanHandler<>(entityClass));
        } else {
            object = queryRunner.query(sql, new BeanHandler<>(entityClass), params);
        }
        return (T) object;
    }

    public Map<String, Object> findFirst(String sql) throws SQLException {
        return findFirst(sql, null);
    }

    public Map<String, Object> findFirst(String sql, Object param) throws SQLException {
        return findFirst(sql, new Object[] { param });
    }

    public Map<String, Object> findFirst(String sql, Object[] params) throws SQLException {
        Map<String, Object> map = null;
        if (params == null) {
            map = (Map<String, Object>) queryRunner.query(sql, new MapHandler());
        } else {
            map = (Map<String, Object>) queryRunner.query(sql, new MapHandler(), params);
        }
        return map;
    }

    public Object findBy(String sql, String columnName) throws SQLException {
        return findBy(sql, columnName, null);
    }

    public Object findBy(String sql, String columnName, Object param) throws SQLException {
        return findBy(sql, columnName, new Object[] { param });
    }

    public Object findBy(String sql, String columnName, Object[] params) throws SQLException {
        Object object = null;
        if (params == null) {
            object = queryRunner.query(sql, new ScalarHandler<>(columnName));
        } else {
            object = queryRunner.query(sql, new ScalarHandler<>(columnName), params);
        }
        return object;
    }

    public Object findBy(String sql, int columnIndex) throws SQLException {
        return findBy(sql, columnIndex, null);
    }

    public Object findBy(String sql, int columnIndex, Object param) throws SQLException {
        return findBy(sql, columnIndex, new Object[] { param });
    }

    public Object findBy(String sql, int columnIndex, Object[] params) throws SQLException {
        Object object = null;
        if (params == null) {
            object = queryRunner.query(sql, new ScalarHandler<>(columnIndex));
        } else {
            object = queryRunner.query(sql, new ScalarHandler<>(columnIndex), params);
        }
        return object;
    }

    public <T> List<T> findPage(Class<T> beanClass, String sql, int page, int pageSize, Object... params) throws SQLException {
        if (page <= 1) {
            page = 0;
        }
        return query(beanClass, sql + " LIMIT ?,?", CollectionUtil.appendArray(params, new Integer[] { page, pageSize }));
    }

    public <T> List<T> query(Class<T> beanClass, String sql, Object... params) throws SQLException {
        return (List<T>) queryRunner.query(sql, isPrimitive(beanClass) ? columnListHandler : new BeanListHandler<>(
                beanClass), params);
    }

    private List<Class<?>> PrimitiveClasses = new ArrayList<Class<?>>() {
        {
            add(Long.class);
            add(Integer.class);
            add(String.class);
            add(java.util.Date.class);
            add(java.sql.Date.class);
            add(java.sql.Timestamp.class);
        }
    };

    private final static ColumnListHandler columnListHandler = new ColumnListHandler<>() {
        @Override
        protected Object handleRow(ResultSet rs) throws SQLException {
            Object obj = super.handleRow(rs);
            if (obj instanceof BigInteger) {
                return ((BigInteger) obj).longValue();
            }
            return obj;
        }
    };

    private boolean isPrimitive(Class<?> cls) {
        return cls.isPrimitive() || PrimitiveClasses.contains(cls);
    }

}
