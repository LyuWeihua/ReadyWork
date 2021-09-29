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

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import org.h2.tools.Server;
import work.ready.core.component.cache.Cache;
import work.ready.core.database.cleverorm.AutoCodeGenerator;
import work.ready.core.component.proxy.JavaCoder;
import work.ready.core.database.annotation.Auto;
import work.ready.core.database.cleverorm.AutoCodeSchemaConfig;
import work.ready.core.database.datasource.DataSourceConfig;
import work.ready.core.database.datasource.DataSourceProvider;
import work.ready.core.database.datasource.H2serverConfig;
import work.ready.core.database.datasource.HikariCp;
import work.ready.core.database.dialect.*;
import work.ready.core.database.jdbc.common.SqlExecuteHandler;
import work.ready.core.database.jdbc.event.JdbcEventListenerManager;
import work.ready.core.database.jdbc.event.JdbcListener;
import work.ready.core.database.jdbc.event.SqlHandlerListener;
import work.ready.core.database.marshaller.ModelDeserializer;
import work.ready.core.database.marshaller.ModelSerializer;
import work.ready.core.database.marshaller.RecordDeserializer;
import work.ready.core.database.marshaller.RecordSerializer;
import work.ready.core.database.transaction.LocalTransactionManager;
import work.ready.core.database.transaction.TransactionManager;
import work.ready.core.event.Event;
import work.ready.core.event.GeneralEvent;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.module.CoreContext;
import work.ready.core.module.Initializer;
import work.ready.core.security.data.DataSecurityInspector;
import work.ready.core.server.Ready;
import work.ready.core.tools.PathUtil;
import work.ready.core.tools.ReadyThreadFactory;
import work.ready.core.tools.StrUtil;
import work.ready.core.tools.define.ConcurrentMultiMap;

import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DatabaseManager {
	private static final Log logger = LogFactory.getLog(DatabaseManager.class);
	protected final static CCJSqlParserManager sqlParserManager = new CCJSqlParserManager();
	protected final static com.github.benmanes.caffeine.cache.Cache<String, net.sf.jsqlparser.statement.Statement>
			sqlStatementCache = Caffeine.newBuilder().build();
	protected final Map<String, List<SqlExecuteHandler>> sqlExecuteHandlers = new HashMap<>();
	private final CoreContext context;
	protected final DatabaseConfig databaseConfig;
	protected final Map<String, DatasourceAgent> datasourceAgentMap = new HashMap<>();
	protected final TableManager tableManager;
	protected final ConcurrentMultiMap<String, DbChangeListener> dbChangeListenerMap = new ConcurrentMultiMap<>();
	protected List<Consumer<DbChangeEvent>> dbChangeEventFilter = new ArrayList<>();
	protected ExecutorService pool = Executors.newCachedThreadPool(new ReadyThreadFactory("DbChangeEvent"));
	protected DataSecurityInspector dataSecurityInspector;
	protected List<Initializer<DatabaseManager>> initializers = new ArrayList<>();
	protected TransactionManager transactionManager;
	protected DbAuditManager auditManager;
	protected SqlDebugger sqlDebugger;
	protected Db db = null;

	protected Server h2tcpServer;
	protected Server h2webServer;
	
	Config config = null;

	private final Map<Class<? extends Model>, Config> modelToConfig = new HashMap<>(512, 0.5F);
	private final Map<String, Config> configNameToConfig = new HashMap<>(32, 0.25F);

	public static final String MAIN_CONFIG_NAME = "main";
	public static final int DEFAULT_TRANSACTION_LEVEL = Connection.TRANSACTION_REPEATABLE_READ;

	public DatabaseManager(CoreContext context) {
		this.context = context;
		databaseConfig = Ready.getMainApplicationConfig().getDatabase();

		db = context.getBeanManager().get(Db.class);
		db.setManager(this);

		tableManager = context.getBeanManager().get(TableManager.class);
		tableManager.setManager(this);

		Ready.post(new GeneralEvent(Event.DATABASE_MANAGER_CREATE, this));

		advancedDbFeatureSupport();
		serializerSupport();
	}

	private void advancedDbFeatureSupport(){
		AutoCodeGenerator autoCodeGenerator = new AutoCodeGenerator(this);
		context.getProxyManager().addAutoCoder(new JavaCoder().setAnnotation(Auto.class)
				.setAssignableFrom(ModelService.class)
				.setGenerator(autoCodeGenerator));
		new AutoCodeSchemaConfig(autoCodeGenerator).listen();
	}

	private void serializerSupport(){
		SimpleModule simpleModule = new SimpleModule();

		List<Class<Model>> modelList = Ready.classScanner().scanSubClass(Model.class, true);
		RecordDeserializer recordDeserializer = new RecordDeserializer();
		RecordSerializer recordSerializer = new RecordSerializer();
		simpleModule.addDeserializer(Record.class, recordDeserializer);
		simpleModule.addSerializer(Record.class, recordSerializer);
		ModelSerializer modelSerializer = new ModelSerializer();
		for (Class<?> m : modelList) {
			simpleModule.addDeserializer(m, new ModelDeserializer(m));
			simpleModule.addSerializer(m, modelSerializer);
		}
		Ready.config().getJsonMapper().registerModule(simpleModule);
		Ready.config().getXmlMapper().registerModule(simpleModule);
	}

	public void addInitializer(Initializer<DatabaseManager> initializer) {
		this.initializers.add(initializer);
		initializers.sort(Comparator.comparing(Initializer::order));
	}

	public void startInit() {
		try {
			for (Initializer<DatabaseManager> i : initializers) {
				i.startInit(this);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		if(sqlDebugger == null) {
			sqlDebugger = Ready.beanManager().get(SqlDebugger.class, DefaultSqlDebugger.class);
		}
		if(auditManager == null) {
			auditManager = Ready.beanManager().get(DbAuditManager.class);
		}
		JdbcEventListenerManager.addListener(Ready.beanManager().get(SqlHandlerListener.class));
		if(transactionManager == null) {
			transactionManager = Ready.beanManager().get(LocalTransactionManager.class);
		}
	}

	public void endInit() {
		try {
			for (Initializer<DatabaseManager> i : initializers) {
				i.endInit(this);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void startH2Server(H2serverConfig h2){
		if(h2 != null){
			try {
				List<String> args = new ArrayList<>();
				args.add("-tcp");
				if(h2.getTcpPort() != null){
					args.add("-tcpPort"); args.add(h2.getTcpPort().toString());
					if(h2.isTcpAllowOthers()) args.add("-tcpAllowOthers");
					h2tcpServer = Server.createTcpServer(args.toArray(new String[args.size()]));
					h2tcpServer.start();
				}
				if(h2.getWebPort() != null) {
					args.add("-web");
					args.add("-webPort"); args.add(h2.getWebPort().toString());
					if(h2.isWebAllowOthers()) args.add("-webAllowOthers");
					h2webServer = Server.createWebServer(args.toArray(new String[args.size()]));
					h2webServer.start();
				}
			} catch (SQLException e){
				if(logger.isWarnEnabled())
					logger.warn("H2 server start failed: " + e.getMessage());
			}
		}
	}

	public DatabaseManager setTransactionManager(TransactionManager txManager) {
		transactionManager = txManager;
		return this;
	}

	public <T extends TransactionManager> T getTransactionManager() {
		return (T)transactionManager;
	}

	public DbAuditManager getAuditManager() {
		return auditManager;
	}

	public DatabaseManager setAuditManager(DbAuditManager auditManager) {
		this.auditManager = auditManager;
		return this;
	}

	public SqlDebugger getSqlDebugger() {
		return sqlDebugger;
	}

	public DatabaseManager setSqlDebugger(SqlDebugger sqlDebugger) {
		this.sqlDebugger = sqlDebugger;
		return this;
	}

	public DatasourceAgent getDatasourceAgent(String name){
		return datasourceAgentMap.get(name);
	}

	public void getDatasourceAgent(BiConsumer<String, DatasourceAgent> apply){
		datasourceAgentMap.forEach(apply);
	}

	public DatabaseConfig getDatabaseConfig(){
		return databaseConfig;
	}

	public DatabaseManager addSqlExecuteHandlers(SqlExecuteHandler handler) {
		sqlExecuteHandlers.computeIfAbsent("*", ds->new ArrayList<>()).add(handler);
		return this;
	}

	public DatabaseManager addSqlExecuteHandlers(String datasource, SqlExecuteHandler handler) {
		sqlExecuteHandlers.computeIfAbsent(datasource, ds->new ArrayList<>()).add(handler);
		return this;
	}

	public List<SqlExecuteHandler> getSqlExecuteHandlers(String datasource) {
		List<SqlExecuteHandler> list = new ArrayList<>();
		if(sqlExecuteHandlers.containsKey("*")) {
			list.addAll(sqlExecuteHandlers.get("*"));
		}
		if(sqlExecuteHandlers.containsKey(datasource)) {
			list.addAll(sqlExecuteHandlers.get(datasource));
		}
		return Collections.unmodifiableList(list);
	}

	public DatabaseManager addJdbcEventListener(JdbcListener listener){
		JdbcEventListenerManager.addListener(listener);
		return this;
	}

	public DatabaseManager removeJdbcEventListener(JdbcListener listener){
		JdbcEventListenerManager.removeListener(listener);
		return this;
	}

	public DatabaseManager addDbChangeListener(String table, DbChangeListener listener){
		dbChangeListenerMap.put(table.toLowerCase(), listener);
		return this;
	}

	public DatabaseManager removeDbChangeListener(String table, DbChangeListener listener){
		dbChangeListenerMap.get(table.toLowerCase()).remove(listener);
		return this;
	}

	public DatabaseManager setDbChangeEventFilter(int idx, Consumer<DbChangeEvent> eventFilter){
		dbChangeEventFilter.add(idx, eventFilter);
		return this;
	}

	public DatabaseManager setDbChangeEventFilter(Consumer<DbChangeEvent> eventFilter){
		dbChangeEventFilter.add(eventFilter);
		return this;
	}

	public void dbChangeNotify(String dbSource, String table, DbChangeType type, Object... id){
		dbChangeNotify(new DbChangeEvent(dbSource, table.toLowerCase(), type, id));
	}

	public void dbChangeNotify(DbChangeEvent event){
		if(event == null || event.isSkip()) return;
		for(Consumer<DbChangeEvent> filter : dbChangeEventFilter){
			filter.accept(event);
		}
		if(event.isSkip()) return;
		String table = event.getTable();
		switch (event.getType()){
			case INSERTED:
				Optional.ofNullable(dbChangeListenerMap.get(table)).ifPresent(list-> list.forEach(listener->{
					pool.submit(()->listener.onInserted(event));
				}));
				break;
			case UPDATED:
				Optional.ofNullable(dbChangeListenerMap.get(table)).ifPresent(list-> list.forEach(listener->{
					pool.submit(()->listener.onUpdated(event));
				}));
				break;
			case DELETED:
				Optional.ofNullable(dbChangeListenerMap.get(table)).ifPresent(list-> list.forEach(listener->{
					pool.submit(()->listener.onDeleted(event));
				}));
				break;
			case REPLACED:
				Optional.ofNullable(dbChangeListenerMap.get(table)).ifPresent(list-> list.forEach(listener->{
					pool.submit(()->listener.onReplaced(event));
				}));
				break;
			case MERGED:
				Optional.ofNullable(dbChangeListenerMap.get(table)).ifPresent(list-> list.forEach(listener->{
					pool.submit(()->listener.onMerged(event));
				}));
				break;
			default:
				break;
		}
	}

	public boolean destroy() {
		if(h2webServer != null) h2webServer.stop();
		if(h2tcpServer != null) h2tcpServer.stop();
		pool.shutdown();
		datasourceAgentMap.values().forEach(db->{
			removeConfig(db.config.getName());
			db.destroy();
		});
		return true;
	}

	public DatabaseManager register(DatasourceAgent datasourceAgent, boolean canBeMainDB) {
		datasourceAgent.setManager(this);
		if(datasourceAgent.init())
			addConfig(datasourceAgent.config, canBeMainDB);
		return this;
	}

	public DatabaseManager register(DatasourceAgent datasourceAgent) {
		return register(datasourceAgent, true);
	}

	public void setDataSecurityInspector(DataSecurityInspector dataSecurityInspector) { this.dataSecurityInspector = dataSecurityInspector; }

	public DataSecurityInspector getDataSecurityInspector(){ return dataSecurityInspector; }

	public Db getDb(){
		return db;
	}

	public ModelConfig getModelConfig(){
		return databaseConfig.getModelConfig();
	}

	public Cache getDbCache(){
		return context.getCacheManager().getDbCache();
	}

	private void addConfig(Config config, boolean canBeMainDB) {
		if (config == null) {
			throw new IllegalArgumentException("Config can not be null");
		}
		if (configNameToConfig.containsKey(config.getName())) {
			throw new IllegalArgumentException("Config already exists: " + config.getName());
		}

		configNameToConfig.put(config.getName(), config);

		if(!canBeMainDB) return;

		if (MAIN_CONFIG_NAME.equals(config.getName())) {
			this.config = config;
			db.init(this.config.getName());
		}

		if (this.config == null) {
			this.config = config;
			db.init(this.config.getName());
		}
	}

	public Config removeConfig(String configName) {
		if (this.config != null && this.config.getName().equals(configName)) {
			
			this.config = null;
		}

		db.removeDbProByConfig(configName);
		return configNameToConfig.remove(configName);
	}

	void addModelToConfigMapping(Class<? extends Model> modelClass, Config config) {
		modelToConfig.put(modelClass, config);
	}

	public Table getTable(Class<? extends Model> modelClass) {
		return tableManager.getTable(modelClass);
	}

	public Table getTable(String dataSource, String tableName) {
		return tableManager.getTable(dataSource, tableName);
	}

	public Config getConfig() {
		return config;
	}

	public Config getConfig(String configName) {
		return configNameToConfig.get(configName);
	}

	public void getConfig(BiConsumer<String, Config> apply) {
		configNameToConfig.forEach(apply);
	}

	public Config getConfig(Class<? extends Model> modelClass) {
		return modelToConfig.get(modelClass);
	}

	public <M> M createModel(Class<M> modelClass){
		return context.getBeanManager().get(modelClass);
	}

	public net.sf.jsqlparser.statement.Statement sqlParser(String className, String methodName, String sql) {
		try {
			return sqlParser(sql);
		} catch (JSQLParserException e) {
			throw new RuntimeException("sql parser exception: " + sql + ", on " + className + "." + methodName, e);
		}
	}

	public net.sf.jsqlparser.statement.Statement sqlParser(String sql) throws JSQLParserException {
		net.sf.jsqlparser.statement.Statement statement = sqlStatementCache.getIfPresent(sql);
		if(statement == null) {
			synchronized (sqlStatementCache) {
				statement = sqlStatementCache.getIfPresent(sql);
				if(statement == null) {
					statement = sqlParser(sql, true);
					sqlStatementCache.put(sql, statement);
				}
			}
		}
		return statement;
	}

	public net.sf.jsqlparser.statement.Statement sqlParser(String sql, boolean withoutCache) throws JSQLParserException {
		if(withoutCache) {
			Reader reader = new StringReader(sql);
			return sqlParserManager.parse(reader);
		} else {
			return sqlParser(sql);
		}
	}

	public DatasourceAgent assignAgent(DataSourceConfig config) {

		DatasourceAgent datasourceAgent;
		config.validate();
		String name = config.getName();
		DataSourceProvider dataSourceProvider = new HikariCp(config);
		String clazzName = config.getDatabaseClass();
		if (StrUtil.isBlank(clazzName)) {
			datasourceAgent = StrUtil.notBlank(name)
					? new DatasourceAgent(name, dataSourceProvider)
					: new DatasourceAgent(dataSourceProvider);
		} else {
			try {
				Class<DatasourceAgent> clazz = (Class<DatasourceAgent>) Class.forName(clazzName, false, Ready.getClassLoader());
				if (StrUtil.notBlank(name)) {
					Constructor constructor = clazz.getConstructor(String.class, DataSourceProvider.class);
					datasourceAgent = (DatasourceAgent) constructor.newInstance(name, dataSourceProvider);
				} else {
					Constructor constructor = clazz.getConstructor(DataSourceProvider.class);
					datasourceAgent = (DatasourceAgent) constructor.newInstance(dataSourceProvider);
				}
			} catch (Exception e) {
				throw new RuntimeException("Database class " + clazzName + " load failed");
			}
		}
		datasourceAgent.setManager(this);
		datasourceAgent.setType(config.getType());

		if (config.getTransactionLevel() != null) {
			datasourceAgent.setTransactionLevel(config.getTransactionLevel());
		}

		Cache dbCache = getDbCache();
		if (dbCache != null) {
			datasourceAgent.setCache(dbCache);
		}

		configSqlTemplate(datasourceAgent, config);
		configDialect(datasourceAgent, config);

		datasourceAgentMap.putIfAbsent(name, datasourceAgent);;
		return datasourceAgent;
	}

	private void configSqlTemplate(DatasourceAgent datasourceAgent, DataSourceConfig datasourceConfig) {
		String sqlTemplatePath = datasourceConfig.getSqlTemplatePath();
		if (StrUtil.notBlank(sqlTemplatePath)) {
			if(!PathUtil.isAbsolutePath(sqlTemplatePath)){
				if(!sqlTemplatePath.startsWith("/")) sqlTemplatePath = "/" + sqlTemplatePath;
				sqlTemplatePath = Ready.root() + sqlTemplatePath;
			} else{
				if(sqlTemplatePath.startsWith("@")) sqlTemplatePath = sqlTemplatePath.substring(1);
			}
			datasourceAgent.setBaseSqlTemplatePath(sqlTemplatePath);
		} else {
			datasourceAgent.setBaseSqlTemplatePath(null);
		}

		String sqlTemplateString = datasourceConfig.getSqlTemplate();
		if (sqlTemplateString != null) {
			String[] sqlTemplateFiles = sqlTemplateString.split(",");
			for (String sql : sqlTemplateFiles) {
				datasourceAgent.addSqlTemplate(sql);
			}
		}
	}

	private void configDialect(DatasourceAgent datasourceAgent, DataSourceConfig datasourceConfig) {

		if (datasourceConfig.getDialectClass() != null) {
			try {
				Class<Dialect> clazz = (Class<Dialect>) Class.forName(datasourceConfig.getDialectClass(), false, Ready.getClassLoader());
				Dialect dialect = clazz.getDeclaredConstructor().newInstance();
				datasourceAgent.setDialect(dialect);
			} catch (Exception e) {
				throw new RuntimeException("Dialect class " + datasourceConfig.getDialectClass() +" load failed");
			}
			return;
		}

		switch (datasourceConfig.getType()) {
			case DataSourceConfig.TYPE_IGNITE:
				datasourceAgent.setDialect(new IgniteDialect());
				break;
			case DataSourceConfig.TYPE_MYSQL:
				datasourceAgent.setDialect(new MysqlDialect());
				break;
			case DataSourceConfig.TYPE_H2:
				if (datasourceAgent.config.containerFactory.getClass().equals(ContainerFactory.defaultContainerFactory.getClass())) {
					datasourceAgent.setContainerFactory(new CaseInsensitiveContainerFactory());
				}
				datasourceAgent.setDialect(new H2Dialect());
				break;
			case DataSourceConfig.TYPE_ORACLE:
				if (datasourceAgent.config.containerFactory.getClass().equals(ContainerFactory.defaultContainerFactory.getClass())) {
					datasourceAgent.setContainerFactory(new CaseInsensitiveContainerFactory());
				}
				datasourceAgent.setDialect(new OracleDialect());
				break;
			case DataSourceConfig.TYPE_SQLSERVER:
				datasourceAgent.setDialect(new SqlServerDialect());
				break;
			case DataSourceConfig.TYPE_SQLITE:
				datasourceAgent.setDialect(new Sqlite3Dialect());
				break;
			case DataSourceConfig.TYPE_ANSISQL:
				datasourceAgent.setDialect(new AnsiSqlDialect());
				break;
			case DataSourceConfig.TYPE_POSTGRESQL:
				datasourceAgent.setDialect(new PostgreSqlDialect());
				break;
			default:
				throw new RuntimeException("Unknown database type '" + datasourceConfig.getType() + "', supported database types are : mysql, h2, orcale, sqlserver, sqlite, ansisql and postgresql. ");
		}
	}

	public void closeQuietly(final ResultSet rs) {
		try {
			close(rs);
		} catch (final SQLException e) { 
			
		}
	}

	public void close(final ResultSet rs) throws SQLException {
		if (rs != null) {
			rs.close();
		}
	}

	public void closeQuietly(final Statement stmt) {
		try {
			close(stmt);
		} catch (final SQLException e) { 
			
		}
	}

	public void close(final Statement stmt) throws SQLException {
		if (stmt != null) {
			stmt.close();
		}
	}

	public void close(final Statement st, final Connection conn) {
		if (st != null) {try {st.close();} catch (SQLException e) {logger.error(e, "Statement close exception");}}
		
		if (!transactionManager.inLocalTransaction()) {
			if (conn != null) {try {conn.close();}
			catch (SQLException e) {throw new DatabaseException(e);}}
		}
	}

	public void closeQuietly(final Connection conn) {
		try {
			close(conn);
		} catch (final DatabaseException e) { 
			
		}
	}

	public void close(final Connection conn) {
		if (!transactionManager.inLocalTransaction()) {        
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					throw new DatabaseException(e);
				}
			}
		}
	}
}

