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
import work.ready.core.database.dialect.Dialect;
import work.ready.core.database.sql.SqlKit;
import work.ready.core.tools.StrUtil;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class Config {

	String name;
	DataSource dataSource;
	String type;
	Dialect dialect;
	boolean devMode;
	int transactionLevel;
	ContainerFactory containerFactory;
	Cache cache;

	SqlKit sqlKit;

	Config(String name, DataSource dataSource, int transactionLevel) {
		init(name, dataSource, null, false, transactionLevel, ContainerFactory.defaultContainerFactory);
	}

	public Config(String name, DataSource dataSource, Dialect dialect, boolean devMode, int transactionLevel, ContainerFactory containerFactory) {
		if (dataSource == null) {
			throw new IllegalArgumentException("DataSource can not be null");
		}
		init(name, dataSource, dialect, devMode, transactionLevel, containerFactory);
	}

	private void init(String name, DataSource dataSource, Dialect dialect, boolean devMode, int transactionLevel, ContainerFactory containerFactory) {
		if (StrUtil.isBlank(name)) {
			throw new IllegalArgumentException("Config name can not be blank");
		}
		if (containerFactory == null) {
			throw new IllegalArgumentException("ContainerFactory can not be null");
		}

		this.name = name.trim();
		this.dataSource = dataSource;
		this.dialect = dialect;
		this.devMode = devMode;
		this.setTransactionLevel(transactionLevel);
		this.containerFactory = containerFactory;

		this.sqlKit = new SqlKit(this.name, this.devMode);
	}

	public Config(String name, DataSource dataSource) {
		this(name, dataSource, null, false, DatabaseManager.DEFAULT_TRANSACTION_LEVEL, ContainerFactory.defaultContainerFactory);
	}

	void setDevMode(boolean devMode) {
		this.devMode = devMode;
		this.sqlKit.setDevMode(devMode);
	}

	void setTransactionLevel(int transactionLevel) {
		int t = transactionLevel;
		if (t != 0 && t != 1  && t != 2  && t != 4  && t != 8) {
			throw new IllegalArgumentException("The transactionLevel only be 0, 1, 2, 4, 8");
		}
		this.transactionLevel = transactionLevel;
	}

	public String getName() {
		return name;
	}

	public SqlKit getSqlKit() {
		return sqlKit;
	}

	public Dialect getDialect() {
		return dialect;
	}

	public String getType() {
		return type;
	}

	public Cache getCache() {
		return cache;
	}

	public int getTransactionLevel() {
		return transactionLevel;
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public ContainerFactory getContainerFactory() {
		return containerFactory;
	}

	public boolean isDevMode() {
		return devMode;
	}

	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

}

