/**
 *
 * Original work Copyright (c) 2011-2017, myaniu 玛雅牛 (myaniu@gmail.com).
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
package work.ready.core.database.datasource;

import work.ready.core.database.jdbc.hikari.HikariConfig;
import work.ready.core.database.jdbc.hikari.ReadyDataSource;
import work.ready.core.server.Ready;
import work.ready.core.tools.PathUtil;
import work.ready.core.tools.StrUtil;

import java.io.File;

public class HikariCp implements DataSourceProvider {

	private DataSourceConfig config = new DataSourceConfig();
	
	private ReadyDataSource ds;

	public HikariCp(DataSourceConfig config){
		this.config = config;
	}

	public HikariCp(String jdbcUrl, String username, String password) {
		config.setJdbcUrl(jdbcUrl);
		config.setUsername(username);
		config.setPassword(password);
	}

	public HikariCp(String jdbcUrl, String username, String password, String driverClass) {
		config.setJdbcUrl(jdbcUrl);
		config.setUsername(username);
		config.setPassword(password);
		config.setDriverClass(driverClass);
	}

	@Override
	public javax.sql.DataSource getDataSource() {
		synchronized (ReadyDataSource.class){
			if(ds == null){
				create();
			}
		}
		return ds;
	}

	@Override
	public boolean create() {
		HikariConfig hikariConfig = new HikariConfig();
		
		String[] jdbcSegment = config.getJdbcUrl().split(":[Hh]2:");
		if(jdbcSegment.length == 2){
			String firstSegment = jdbcSegment[0] + ":h2:";
			String secondSegment = jdbcSegment[1].toLowerCase();
			String path = null;
			if(secondSegment.startsWith("tcp:") || secondSegment.startsWith("ssl:")) {
				int index = secondSegment.indexOf("/", 7);  
				path = jdbcSegment[1].substring(index + 1);
				firstSegment += jdbcSegment[1].substring(0, index + 1);
			} else if (secondSegment.startsWith("mem:")){
			} else if(secondSegment.startsWith("file:")){
				path = jdbcSegment[1].substring(5);
				firstSegment += jdbcSegment[1].substring(0, 5);
			} else if(secondSegment.startsWith("zip:")){
				path = jdbcSegment[1].substring(4);
				firstSegment += jdbcSegment[1].substring(0, 4);
			} else {
				path = jdbcSegment[1];
			}
			if(path != null){
				if (PathUtil.isAbsolutePath(path)) {
					if (jdbcSegment[1].startsWith("@")){
						path = path.substring(1);
					}
					config.setJdbcUrl(firstSegment + path);
				} else {
					if(!path.startsWith(File.separator)){
						path = File.separator + path;
					}
					config.setJdbcUrl(firstSegment + Ready.root() + path);
				}
			}
		}
		hikariConfig.setJdbcUrl(config.getJdbcUrl());

		hikariConfig.setUsername(config.getUsername());
		hikariConfig.setPassword(config.getPassword());

		hikariConfig.setAutoCommit(config.isAutoCommit());
		hikariConfig.setReadOnly(config.isReadOnly());

		hikariConfig.setConnectionTimeout(config.getConnectionTimeout());
		hikariConfig.setIdleTimeout(config.getIdleTimeout());
		hikariConfig.setMaxLifetime(config.getMaxLifetime());
		hikariConfig.setMaximumPoolSize(config.getMaximumPoolSize());
		hikariConfig.setValidationTimeout(config.getValidationTimeout());

		hikariConfig.setDataSource(config.getDataSource());
		hikariConfig.setDataSourceClassName(config.getDataSourceClassName());
		hikariConfig.setDataSourceJNDI(config.getDataSourceJNDI());
		if(StrUtil.notBlank(config.getDriverClass())){
			hikariConfig.setDriverClassName(config.getDriverClass());
		}

		if(StrUtil.notBlank(config.getTransactionIsolation())){
			hikariConfig.setTransactionIsolation(config.getTransactionIsolation());
		}

		if(this.config.getLeakDetectionThreshold() != 0){
			hikariConfig.setLeakDetectionThreshold(config.getLeakDetectionThreshold());
		}

		if(StrUtil.notBlank(config.getCatalog())){
			hikariConfig.setCatalog(config.getCatalog());
		}

		if(StrUtil.notBlank(config.getConnectionTestQuery())){
			hikariConfig.setConnectionTestQuery(config.getConnectionTestQuery());
		}

		hikariConfig.setPoolName(config.getName());

		if(StrUtil.notBlank(config.getSchema())){
			hikariConfig.setSchema(config.getSchema());
		}

		if(StrUtil.notBlank(config.getConnectionInitSql())){
			hikariConfig.setConnectionInitSql(config.getConnectionInitSql());
		}

		if(config.getDataSourceProperties() != null){
			hikariConfig.setDataSourceProperties(config.getDataSourceProperties());
		}
		if(config.getJdbcUrl().toLowerCase().contains(":mysql:")){
			hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
			hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
			hikariConfig.addDataSourceProperty("prepStmtCacheSize", "256");
			hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
		}
		if(config.getJdbcUrl().toLowerCase().contains(":postgresql:")){
			if(config.isReadOnly()){
				hikariConfig.addDataSourceProperty("readOnly", "true");
			}
			hikariConfig.setConnectionTimeout(0);
			hikariConfig.addDataSourceProperty("prepareThreshold", "3");
			hikariConfig.addDataSourceProperty("preparedStatementCacheQueries", "128");
			hikariConfig.addDataSourceProperty("preparedStatementCacheSizeMiB", "4");
		}

		ds = new ReadyDataSource(hikariConfig);
		ds.setAttribute(ReadyDataSource.isEnabledTransaction, config.isEnabledTransaction());
		return true;
	}

	@Override
	public boolean destroy() {
		if (ds != null) {
			ds.close();
		}
		return true;
	}

}

