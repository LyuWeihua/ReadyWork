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

package work.ready.core.database.datasource;

import work.ready.core.config.BaseConfig;
import work.ready.core.database.DatabaseManager;
import work.ready.core.tools.StrUtil;

import javax.sql.DataSource;
import java.util.Properties;

import static java.sql.Connection.*;

public class DataSourceConfig extends BaseConfig {

    public static final String TYPE_MYSQL = "mysql";
    public static final String TYPE_IGNITE = "ignite";
    public static final String TYPE_H2 = "h2";
    public static final String TYPE_ORACLE = "oracle";
    public static final String TYPE_SQLSERVER = "sqlserver";
    public static final String TYPE_SQLITE = "sqlite";
    public static final String TYPE_ANSISQL = "ansisql";
    public static final String TYPE_POSTGRESQL = "postgresql";

    private String name;

    private String type = TYPE_MYSQL;

    private boolean enabled = true;

    private String jdbcUrl;

    private String username;

    private String password;

    private boolean autoCommit = true;

    private long connectionTimeout = 30000;

    private long idleTimeout = 600000;

    private long maxLifetime = 1800000;

    private String connectionTestQuery = null;

    private int maximumPoolSize = 10;

    private String schema;

    private boolean readOnly = false;

    private String catalog = null;

    private String connectionInitSql = null;

    private String driverClass = "com.mysql.cj.jdbc.Driver";

    private DataSource dataSource;

    private String dataSourceClassName;

    private String dataSourceJndiName;

    private Properties dataSourceProperties;

    private boolean enabledTransaction = true;

    private String transactionIsolation = null;
    
    private Integer transactionLevel = DatabaseManager.DEFAULT_TRANSACTION_LEVEL;

    private long validationTimeout = 5000;

    private long leakDetectionThreshold = 0;

    private boolean cachePrepStmts = true;
    private int prepStmtCacheSize = 500;
    private int prepStmtCacheSqlLimit = 2048;

    private String sqlTemplatePath;
    private String sqlTemplate;

    private String shardingConfig;

    private boolean autoMapping = true;
    private String table; 
    private String ignoreTable; 

    private String dialectClass;
    private String databaseClass;

    public String getName() {
        return name;
    }

    public DataSourceConfig setName(String name) {
        this.name = name;
    	return this;
	}

    public String getType() {
        return type;
    }

    public DataSourceConfig setType(String type) {
        this.type = type;
    	return this;
	}

    public boolean isEnabled() {
        return enabled;
    }

    public DataSourceConfig setEnabled(boolean enabled) {
        this.enabled = enabled;
    	return this;
	}

    public final DataSourceConfig setDriverClass(String driverClass) {
        this.driverClass = driverClass;
    	return this;
	}

    public DataSource getDataSource()
    {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    public String getDataSourceClassName()
    {
        return dataSourceClassName;
    }

    public void setDataSourceClassName(String className)
    {
        this.dataSourceClassName = className;
    }

    public void addDataSourceProperty(String propertyName, Object value)
    {
        dataSourceProperties.put(propertyName, value);
    }

    public String getDataSourceJNDI()
    {
        return this.dataSourceJndiName;
    }

    public void setDataSourceJNDI(String jndiDataSource)
    {
        this.dataSourceJndiName = jndiDataSource;
    }

    public Properties getDataSourceProperties()
    {
        return dataSourceProperties;
    }

    public void setDataSourceProperties(Properties dsProperties)
    {
        dataSourceProperties.putAll(dsProperties);
    }

    public String getSchema()
    {
        return schema;
    }

    public final DataSourceConfig setSchema(String schema)
    {
        this.schema = schema;
        return this;
    }

    public final DataSourceConfig setUsername(String username) {
        this.username = username;
    	return this;
	}

    public final DataSourceConfig setPassword(String password) {
        this.password = password;
    	return this;
	}

    public final DataSourceConfig setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
    	return this;
	}

    public final DataSourceConfig setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    	return this;
	}

    public final DataSourceConfig setConnectionTimeout(long connectionTimeoutMs) {
        this.connectionTimeout = connectionTimeoutMs;
    	return this;
	}

    public final DataSourceConfig setIdleTimeout(long idleTimeoutMs) {
        this.idleTimeout = idleTimeoutMs;
    	return this;
	}

    public final DataSourceConfig setMaxLifetime(long maxLifetimeMs) {
        this.maxLifetime = maxLifetimeMs;
    	return this;
	}

    public final DataSourceConfig setMaximumPoolSize(int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    	return this;
	}

    public final DataSourceConfig setConnectionInitSql(String connectionInitSql) {
        this.connectionInitSql = connectionInitSql;
    	return this;
	}

    public final DataSourceConfig setConnectionTestQuery(String connectionTestQuery) {
        this.connectionTestQuery = connectionTestQuery;
    	return this;
	}

    public final DataSourceConfig setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    	return this;
	}

    public final DataSourceConfig setCatalog(String catalog) {
        this.catalog = catalog;
    	return this;
	}

    public void setEnabledTransaction(boolean enabledTransaction) {
        this.enabledTransaction = enabledTransaction;
    }

    public final DataSourceConfig setTransactionIsolation(String isolationLevel) {
        this.transactionIsolation = isolationLevel;
    	return this;
	}

    public final DataSourceConfig setValidationTimeout(long validationTimeoutMs) {
        this.validationTimeout = validationTimeoutMs;
    	return this;
	}

    public final DataSourceConfig setLeakDetectionThreshold(long leakDetectionThresholdMs) {
        this.leakDetectionThreshold = leakDetectionThresholdMs;
    	return this;
	}

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isAutoCommit() {
        return autoCommit;
    }

    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    public long getIdleTimeout() {
        return idleTimeout;
    }

    public long getMaxLifetime() {
        return maxLifetime;
    }

    public String getConnectionTestQuery() {
        return connectionTestQuery;
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public String getCatalog() {
        return catalog;
    }

    public String getConnectionInitSql() {
        return connectionInitSql;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public boolean isEnabledTransaction() {
        return enabledTransaction;
    }

    public String getTransactionIsolation() {
        return transactionIsolation;
    }

    public long getValidationTimeout() {
        return validationTimeout;
    }

    public long getLeakDetectionThreshold() {
        return leakDetectionThreshold;
    }

    public boolean isCachePrepStmts() {
        return cachePrepStmts;
    }

    public DataSourceConfig setCachePrepStmts(boolean cachePrepStmts) {
        this.cachePrepStmts = cachePrepStmts;
    	return this;
	}

    public int getPrepStmtCacheSize() {
        return prepStmtCacheSize;
    }

    public DataSourceConfig setPrepStmtCacheSize(int prepStmtCacheSize) {
        this.prepStmtCacheSize = prepStmtCacheSize;
    	return this;
	}

    public int getPrepStmtCacheSqlLimit() {
        return prepStmtCacheSqlLimit;
    }

    public DataSourceConfig setPrepStmtCacheSqlLimit(int prepStmtCacheSqlLimit) {
        this.prepStmtCacheSqlLimit = prepStmtCacheSqlLimit;
    	return this;
	}

    public String getSqlTemplatePath() {
        return sqlTemplatePath;
    }

    public DataSourceConfig setSqlTemplatePath(String sqlTemplatePath) {
        this.sqlTemplatePath = sqlTemplatePath;
    	return this;
	}

    public String getSqlTemplate() {
        return sqlTemplate;
    }

    public DataSourceConfig setSqlTemplate(String sqlTemplate) {
        this.sqlTemplate = sqlTemplate;
    	return this;
	}

    public String getShardingConfig() {
        return shardingConfig;
    }

    public DataSourceConfig setShardingConfig(String shardingConfig) {
        this.shardingConfig = shardingConfig;
    	return this;
	}

    public String getTable() {
        return table;
    }

    public DataSourceConfig setTable(String table) {
        this.table = table;
    	return this;
	}

    public String getIgnoreTable() {
        return ignoreTable;
    }

    public DataSourceConfig setIgnoreTable(String ignoreTable) {
        this.ignoreTable = ignoreTable;
    	return this;
	}

    public String getDialectClass() {
        return dialectClass;
    }

    public DataSourceConfig setDialectClass(String dialectClass) {
        this.dialectClass = dialectClass;
    	return this;
	}

    public String getDatabaseClass() {
        return databaseClass;
    }

    public DataSourceConfig setDatabaseClass(String databaseClass) {
        this.databaseClass = databaseClass;
    	return this;
	}

    public Integer getTransactionLevel() {
        return transactionLevel;
    }

    public DataSourceConfig setTransactionLevel(Integer transactionLevel) {
        this.transactionLevel = transactionLevel;
    	return this;
	}

    public boolean isAutoMapping() {
        return autoMapping;
    }

    public DataSourceConfig setAutoMapping(boolean autoMapping) {
        this.autoMapping = autoMapping;
    	return this;
	}

	@Override
    public void validate() {
        if(StrUtil.notBlank(transactionIsolation)){
            switch (transactionIsolation) {
                case "TRANSACTION_READ_UNCOMMITTED":
                    setTransactionLevel(TRANSACTION_READ_UNCOMMITTED);
                    break;
                case "TRANSACTION_READ_COMMITTED":
                    setTransactionLevel(TRANSACTION_READ_COMMITTED);
                    break;
                case "TRANSACTION_REPEATABLE_READ":
                    setTransactionLevel(TRANSACTION_REPEATABLE_READ);
                    break;
                case "TRANSACTION_SERIALIZABLE":
                    setTransactionLevel(TRANSACTION_SERIALIZABLE);
                    break;
                default:
                    transactionIsolation = null;
            }
        }
        if(transactionIsolation == null && transactionLevel != null) {
            switch (transactionLevel) {
                case TRANSACTION_READ_UNCOMMITTED :
                    setTransactionIsolation("TRANSACTION_READ_UNCOMMITTED");
                    break;
                case TRANSACTION_READ_COMMITTED :
                    setTransactionIsolation("TRANSACTION_READ_COMMITTED");
                    break;
                case TRANSACTION_REPEATABLE_READ :
                    setTransactionIsolation("TRANSACTION_REPEATABLE_READ");
                    break;
                case TRANSACTION_SERIALIZABLE :
                    setTransactionIsolation("TRANSACTION_SERIALIZABLE");
                    break;
                default:
                    throw new RuntimeException("invalid transactionIsolation or transactionLevel");
            }
        }
    }
}
