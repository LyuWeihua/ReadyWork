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
import work.ready.core.database.datasource.DataSourceProvider;
import work.ready.core.database.dialect.Dialect;
import work.ready.core.database.jdbc.hikari.ReadyDataSource;
import work.ready.core.database.sql.SqlKit;
import work.ready.core.template.Engine;
import work.ready.core.template.source.TemplateSource;
import work.ready.core.tools.StrUtil;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class DatasourceAgent {

	private DatabaseManager manager;
	protected DataSourceProvider dataSourceProvider = null;
	protected Boolean devMode = null;
	private boolean initialized = false;

	protected Config config = null;

	protected List<Table> tableList = new ArrayList<>();

	DatasourceAgent(String configName, javax.sql.DataSource dataSource, int transactionLevel) {
		if (StrUtil.isBlank(configName)) {
			throw new IllegalArgumentException("configName can not be blank");
		}
		if (dataSource == null) {
			throw new IllegalArgumentException("dataSource can not be null");
		}
		this.config = new Config(configName, dataSource, transactionLevel);
	}

	DatasourceAgent(javax.sql.DataSource dataSource) {
		this(DatabaseManager.MAIN_CONFIG_NAME, dataSource);
	}

	DatasourceAgent(String configName, javax.sql.DataSource dataSource) {
		this(configName, dataSource, DatabaseManager.DEFAULT_TRANSACTION_LEVEL);
	}

	DatasourceAgent(javax.sql.DataSource dataSource, int transactionLevel) {
		this(DatabaseManager.MAIN_CONFIG_NAME, dataSource, transactionLevel);
	}

	DatasourceAgent(String configName, DataSourceProvider dataSourceProvider, int transactionLevel) {
		if (StrUtil.isBlank(configName)) {
			throw new IllegalArgumentException("configName can not be blank");
		}
		if (dataSourceProvider == null) {
			throw new IllegalArgumentException("dataSourceProvider can not be null");
		}
		this.dataSourceProvider = dataSourceProvider;
		this.config = new Config(configName, null, transactionLevel);
	}

	DatasourceAgent(DataSourceProvider dataSourceProvider) {
		this(DatabaseManager.MAIN_CONFIG_NAME, dataSourceProvider);
	}

	DatasourceAgent(String configName, DataSourceProvider dataSourceProvider) {
		this(configName, dataSourceProvider, DatabaseManager.DEFAULT_TRANSACTION_LEVEL);
	}

	DatasourceAgent(DataSourceProvider dataSourceProvider, int transactionLevel) {
		this(DatabaseManager.MAIN_CONFIG_NAME, dataSourceProvider, transactionLevel);
	}

	DatasourceAgent(Config config) {
		if (config == null) {
			throw new IllegalArgumentException("Config can not be null");
		}
		this.config = config;
	}

	void setManager(DatabaseManager manager){
		this.manager = manager;
	}

	public DatasourceAgent addMapping(String tableName, String primaryKey, Class<? extends Model<?>> modelClass) {
		if(initialized){
			manager.tableManager.addMapping(tableName, primaryKey, modelClass, config);
		} else {
			tableList.add(new Table(tableName, primaryKey, modelClass));
		}
		return this;
	}

	public DatasourceAgent addMapping(String tableName, Class<? extends Model<?>> modelClass) {
		if(initialized){
			manager.tableManager.addMapping(tableName, modelClass, config);
		} else {
			tableList.add(new Table(tableName, modelClass));
		}
		return this;
	}

	public DatasourceAgent addSqlTemplate(String sqlTemplate) {
		config.sqlKit.addSqlTemplate(sqlTemplate);
		return this;
	}

	public DatasourceAgent addSqlTemplate(TemplateSource sqlTemplate) {
		config.sqlKit.addSqlTemplate(sqlTemplate);
		return this;
	}

	public DatasourceAgent setBaseSqlTemplatePath(String baseSqlTemplatePath) {
		config.sqlKit.setBaseSqlTemplatePath(baseSqlTemplatePath);
		return this;
	}

	public SqlKit getSqlKit() {
		return config.sqlKit;
	}

	public Engine getEngine() {
		return getSqlKit().getEngine();
	}

	public DatasourceAgent setTransactionLevel(int transactionLevel) {
		config.setTransactionLevel(transactionLevel);
		return this;
	}

	public DatasourceAgent setType(String type) {
		config.type = type;
		return this;
	}

	public DatasourceAgent setCache(Cache cache) {
		if (cache == null) {
			throw new IllegalArgumentException("cache can not be null");
		}
		config.cache = cache;
		return this;
	}

	public DatasourceAgent setDevMode(boolean devMode) {
		this.devMode = devMode;
		config.setDevMode(devMode);
		return this;
	}

	public Boolean getDevMode() {
		return devMode;
	}

	public DatasourceAgent setDialect(Dialect dialect) {
		if (dialect == null) {
			throw new IllegalArgumentException("dialect can not be null");
		}
		config.dialect = dialect;
		if (config.transactionLevel == Connection.TRANSACTION_REPEATABLE_READ && dialect.isOracle()) {
			
			config.transactionLevel = Connection.TRANSACTION_READ_COMMITTED;
		}
		return this;
	}

	public DatasourceAgent setContainerFactory(ContainerFactory containerFactory) {
		if (containerFactory == null) {
			throw new IllegalArgumentException("containerFactory can not be null");
		}
		config.containerFactory = containerFactory;
		return this;
	}

	public void setPrimaryKey(String tableName, String primaryKey) {
		for (Table table : tableList) {
			if (table.getName().equalsIgnoreCase(tableName.trim())) {
				table.setPrimaryKey(primaryKey);
			}
		}
	}

	boolean init() {
		if(initialized) return false;
		if (config.dataSource == null && dataSourceProvider != null) {
			config.dataSource = dataSourceProvider.getDataSource();
		}
		if (config.dataSource == null) {
			throw new RuntimeException("database initialize error: DataSource is null");
		}
		if(config.cache == null) config.cache = manager.getDbCache();
		config.sqlKit.parseSqlTemplate();
		if(tableList.size() > 0)
			manager.tableManager.buildTable(tableList, config);
		initialized = true;
		return initialized;
	}

	boolean destroy() {
		if(config.getDataSource() instanceof ReadyDataSource) {
			((ReadyDataSource) config.getDataSource()).close();
		}
		return true;
	}

	public Config getConfig() {
		return config;
	}
}

