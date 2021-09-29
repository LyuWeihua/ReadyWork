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

import work.ready.core.config.Config;
import work.ready.core.database.DatabaseManager;
import work.ready.core.database.datasource.DataSourceConfig;
import work.ready.core.database.dialect.*;
import work.ready.core.database.jdbc.hikari.HikariConfig;
import work.ready.core.database.jdbc.hikari.ReadyDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Generator {

	private static Map<String, DataSource> dataSourceMap = new HashMap<>();
	protected Dialect dialect = null;
	protected MetaBuilder metaBuilder;
	protected BaseModelGenerator baseModelGenerator;
	protected ModelGenerator modelGenerator;
	protected ServiceInterfaceGenerator interfaceGenerator;
	protected ServiceImplGenerator implGenerator;

	protected DataDictionaryGenerator dataDictionaryGenerator;
	protected boolean generateDataDictionary = false;

	public Generator(DataSource dataSource, MetaBuilder metaBuilder, String baseModelPackageName, String baseModelOutputDir, String modelPackageName, String modelOutputDir) {
		this(dataSource, metaBuilder, new BaseModelGenerator(baseModelPackageName, baseModelOutputDir), new ModelGenerator(modelPackageName, baseModelPackageName, modelOutputDir));
	}

	public Generator(DataSource dataSource, MetaBuilder metaBuilder, String baseModelPackageName, String baseModelOutputDir) {
		this(dataSource, metaBuilder, new BaseModelGenerator(baseModelPackageName, baseModelOutputDir));
	}

	public Generator(DataSource dataSource, MetaBuilder metaBuilder, BaseModelGenerator baseModelGenerator) {
		if (dataSource == null) {
			throw new IllegalArgumentException("dataSource can not be null.");
		}
		if (baseModelGenerator == null) {
			throw new IllegalArgumentException("baseModelGenerator can not be null.");
		}
		this.metaBuilder = metaBuilder;
		this.baseModelGenerator = baseModelGenerator;
		this.modelGenerator = null;
		this.interfaceGenerator = null;
		this.implGenerator = null;
		this.dataDictionaryGenerator = null;
	}

	public Generator(DataSource dataSource, MetaBuilder metaBuilder, BaseModelGenerator baseModelGenerator, ModelGenerator modelGenerator) {
		if (dataSource == null) {
			throw new IllegalArgumentException("dataSource can not be null.");
		}
		if (baseModelGenerator == null) {
			throw new IllegalArgumentException("baseModelGenerator can not be null.");
		}
		if (modelGenerator == null) {
			throw new IllegalArgumentException("modelGenerator can not be null.");
		}
		this.metaBuilder = metaBuilder;
		this.baseModelGenerator = baseModelGenerator;
		this.modelGenerator = modelGenerator;
		this.interfaceGenerator = null;
		this.implGenerator = null;
		this.dataDictionaryGenerator = new DataDictionaryGenerator(dataSource, modelGenerator.modelOutputDir);
	}

	public Generator(DataSource dataSource, MetaBuilder metaBuilder, BaseModelGenerator baseModelGenerator, ModelGenerator modelGenerator, ServiceInterfaceGenerator interfaceGenerator, ServiceImplGenerator implGenerator) {
		if (dataSource == null) {
			throw new IllegalArgumentException("dataSource can not be null.");
		}
		if (baseModelGenerator == null) {
			throw new IllegalArgumentException("baseModelGenerator can not be null.");
		}
		if (modelGenerator == null) {
			throw new IllegalArgumentException("modelGenerator can not be null.");
		}
		this.metaBuilder = metaBuilder;
		this.baseModelGenerator = baseModelGenerator;
		this.modelGenerator = modelGenerator;
		this.interfaceGenerator = interfaceGenerator;
		this.implGenerator = implGenerator;
		this.dataDictionaryGenerator = new DataDictionaryGenerator(dataSource, modelGenerator.modelOutputDir);
	}

	public Generator(String datasource, String modelPackage, String servicePackage) {
		if (datasource == null) {
			throw new IllegalArgumentException("dataSource can not be null.");
		}
		if (modelPackage == null) {
			throw new IllegalArgumentException("modelPackage can not be null.");
		}
		if (servicePackage == null) {
			throw new IllegalArgumentException("servicePackage can not be null.");
		}
		String baseModelPackage = modelPackage + ".base";
		this.metaBuilder = getMetaBuilder(datasource);
		this.baseModelGenerator = new BaseModelGenerator(baseModelPackage);
		this.modelGenerator = new ModelGenerator(modelPackage, baseModelPackage);
		this.interfaceGenerator = new ServiceInterfaceGenerator(servicePackage, modelPackage);
		this.implGenerator = new ServiceImplGenerator(servicePackage, modelPackage);
		this.dataDictionaryGenerator = new DataDictionaryGenerator(getDatasource(datasource), modelGenerator.modelOutputDir);
	}

	public void setMetaBuilder(MetaBuilder metaBuilder) {
		if (metaBuilder != null) {
			this.metaBuilder = metaBuilder;
		}
	}

	public void setGenerateRemarks(boolean generateRemarks) {
		if (metaBuilder != null) {
			metaBuilder.setGenerateRemarks(generateRemarks);
		}
	}

	public void setTypeMapping(TypeMapping typeMapping) {
		this.metaBuilder.setTypeMapping(typeMapping);
	}

	public void addTypeMapping(Class<?> from, Class<?> to) {
		this.metaBuilder.typeMapping.addMapping(from, to);
	}

	public void addTypeMapping(String from, String to) {
		this.metaBuilder.typeMapping.addMapping(from, to);
	}

	public void setInterfaceGenerator(ServiceInterfaceGenerator interfaceGenerator) {
		if (interfaceGenerator != null) {
			this.interfaceGenerator = interfaceGenerator;
		}
	}

	public void setInterfaceTemplate(String template) {
		if(interfaceGenerator != null) {
			interfaceGenerator.setTemplate(template);
		}
	}

	public void setImplGenerator(ServiceImplGenerator implGenerator) {
		if (implGenerator != null) {
			this.implGenerator = implGenerator;
		}
	}

	public void setImplTemplate(String template) {
		if(implGenerator != null) {
			implGenerator.setTemplate(template);
		}
	}

	public void setDataDictionaryGenerator(DataDictionaryGenerator dataDictionaryGenerator) {
		if (dataDictionaryGenerator != null) {
			this.dataDictionaryGenerator = dataDictionaryGenerator;
		}
	}

	public void setDialect(Dialect dialect) {
		this.dialect = dialect;
	}

	public void setBaseModelTemplate(String baseModelTemplate) {
		baseModelGenerator.setTemplate(baseModelTemplate);
	}

	public void setGenerateChainSetter(boolean generateChainSetter) {
		baseModelGenerator.setGenerateChainSetter(generateChainSetter);
	}

	public void setRemovedTableNamePrefixes(String... removedTableNamePrefixes) {
		metaBuilder.setRemovedTableNamePrefixes(removedTableNamePrefixes);
	}

	public void addExcludedTable(String... excludedTables) {
		metaBuilder.addExcludedTable(excludedTables);
	}

	public void setModelTemplate(String modelTemplate) {
		if (modelGenerator != null) {
			modelGenerator.setTemplate(modelTemplate);
		}
	}

	public void setGenerateDaoInModel(boolean generateDaoInModel) {
		if (modelGenerator != null) {
			modelGenerator.setGenerateDaoInModel(generateDaoInModel);
		}
	}

	public void setGenerateDataDictionary(boolean generateDataDictionary) {
		this.generateDataDictionary = generateDataDictionary;
	}

	public void setDataDictionaryOutputDir(String dataDictionaryOutputDir) {
		if (this.dataDictionaryGenerator != null) {
			this.dataDictionaryGenerator.setDataDictionaryOutputDir(dataDictionaryOutputDir);
		}
	}

	public void setDataDictionaryFileName(String dataDictionaryFileName) {
		if (dataDictionaryGenerator != null) {
			dataDictionaryGenerator.setDataDictionaryFileName(dataDictionaryFileName);
		}
	}

	public void generate() {
		if (dialect != null) {
			metaBuilder.setDialect(dialect);
		}

		long start = System.currentTimeMillis();
		List<TableMeta> tableMetas = metaBuilder.build();
		if (tableMetas.size() == 0) {
			System.out.println("TableMeta 数量为 0，不生成任何文件");
			return ;
		}

		baseModelGenerator.generate(tableMetas);

		if (modelGenerator != null) {
			modelGenerator.generate(tableMetas);
		}

		if(interfaceGenerator != null) {
			interfaceGenerator.generate(tableMetas);
		}

		if(implGenerator != null) {
			implGenerator.generate(tableMetas);
		}

		if (dataDictionaryGenerator != null && generateDataDictionary) {
			dataDictionaryGenerator.generate(tableMetas);
		}

		long usedTime = (System.currentTimeMillis() - start) / 1000;
		System.out.println("Generate complete in " + usedTime + " seconds.");
	}

	public static DataSource getMainDatasource() {
		return getDatasource(DatabaseManager.MAIN_CONFIG_NAME);
	}

	public static DataSource getDatasource(String datasourceName) {
		if(!dataSourceMap.containsKey(datasourceName)) {
			DataSourceConfig datasourceConfig = new Config().getApplicationConfig().getDatabase().getDataSource(datasourceName);
			HikariConfig config = new HikariConfig();
			config.setJdbcUrl(datasourceConfig.getJdbcUrl());
			config.setUsername(datasourceConfig.getUsername());
			config.setPassword(datasourceConfig.getPassword());
			config.setDriverClassName(datasourceConfig.getDriverClass());
			dataSourceMap.put(datasourceName, new ReadyDataSource(config));
		}
		return dataSourceMap.get(datasourceName);
	}

	public static MetaBuilder getMainMetaBuilder() {
		return getMetaBuilder(DatabaseManager.MAIN_CONFIG_NAME);
	}

	public static MetaBuilder getMetaBuilder(String datasourceName) {
		DataSourceConfig datasourceConfig = new Config().getApplicationConfig().getDatabase().getDataSource(datasourceName);
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(datasourceConfig.getJdbcUrl());
		config.setUsername(datasourceConfig.getUsername());
		config.setPassword(datasourceConfig.getPassword());
		config.setDriverClassName(datasourceConfig.getDriverClass());

		Dialect dialect = null;
		switch (datasourceConfig.getType()) {
			case DataSourceConfig.TYPE_MYSQL:
				dialect = new MysqlDialect();
				break;
			case DataSourceConfig.TYPE_H2:
				dialect = new H2Dialect();
				break;
			case DataSourceConfig.TYPE_IGNITE:
				dialect = new IgniteDialect();
				break;
			case DataSourceConfig.TYPE_ORACLE:
				dialect = new OracleDialect();
				break;
			case DataSourceConfig.TYPE_SQLSERVER:
				dialect = new SqlServerDialect();
				break;
			case DataSourceConfig.TYPE_SQLITE:
				dialect = new Sqlite3Dialect();
				break;
			case DataSourceConfig.TYPE_ANSISQL:
				dialect = new AnsiSqlDialect();
				break;
			case DataSourceConfig.TYPE_POSTGRESQL:
				dialect = new PostgreSqlDialect();
				break;
			default:
				throw new RuntimeException("invalid datasource type " + datasourceConfig.getType() + ", supported types: mysql, ignite, oracle, sqlserver, sqlite, ansisql and postgresql.");
		}
		MetaBuilder metaBuilder = new MetaBuilder(getDatasource(datasourceName), dialect);
		metaBuilder.setGenerateRemarks(true);

		return metaBuilder;
	}

	@SuppressWarnings("serial")
	public static Map<String, String> getterTypeMap = new HashMap<String, String>() {{
		put("java.lang.String", "getStr");
		put("java.lang.Integer", "getInt");
		put("java.lang.Long", "getLong");
		put("java.lang.Double", "getDouble");
		put("java.lang.Float", "getFloat");
		put("java.lang.Short", "getShort");
		put("java.lang.Byte", "getByte");

		put("java.util.Date", "getDate");
		put("java.time.LocalDateTime", "getLocalDateTime");
	}};
}

