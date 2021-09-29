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

package work.ready.core.module;

import work.ready.core.aop.Interceptor;
import work.ready.core.aop.InterceptorManager;
import work.ready.core.aop.annotation.GlobalInterceptor;
import work.ready.core.component.cache.CacheManager;
import work.ready.core.component.proxy.ProxyManager;
import work.ready.core.database.*;
import work.ready.core.database.datasource.DataSourceConfig;
import work.ready.core.event.Event;
import work.ready.core.event.EventListener;
import work.ready.core.event.GeneralEvent;
import work.ready.core.ioc.BeanManager;
import work.ready.core.ioc.annotation.Component;
import work.ready.core.ioc.annotation.Configuration;
import work.ready.core.ioc.annotation.Service;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;
import work.ready.core.tools.StrUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static work.ready.core.database.DatabaseManager.MAIN_CONFIG_NAME;

public class DefaultCoreInitializer implements CoreContextInitializer {

    private static final Log logger = LogFactory.getLog(DefaultCoreInitializer.class);
    private CoreContext context;

    @Override
    public void setContext(CoreContext context){
        this.context = context;
    }

    @Override
    public CoreContext getContext(){
        return this.context;
    }

    @Override
    public void createBegin() {
        Ready.post(new GeneralEvent(Event.CORE_INITIALIZER_CREATE_BEGIN, this));

        context.pluginMap.forEach((name,plugin) -> {
            logger.info("plugin " + name + " is starting");
            plugin.start();
            logger.info("plugin " + name + " started");
        });
    }

    @Override
    public BeanManager createBeanManager() {
        GeneralEvent beforeEvent = new GeneralEvent(Event.BEAN_MANAGER_BEFORE_CREATE, this);
        Ready.post(beforeEvent);
        BeanManager beanManager = beforeEvent.getObject() instanceof BeanManager ? beforeEvent.getObject() : new BeanManager(context);
        GeneralEvent afterEvent = new GeneralEvent(Event.BEAN_MANAGER_AFTER_CREATE, this, beanManager);
        Ready.post(afterEvent);
        beanManager = afterEvent.getObject() instanceof BeanManager ? afterEvent.getObject() : beanManager;
        return beanManager;
    }

    @Override
    public ProxyManager createProxyManager() {
        GeneralEvent beforeEvent = new GeneralEvent(Event.PROXY_MANAGER_BEFORE_CREATE, this);
        Ready.post(beforeEvent);
        ProxyManager proxyManager = beforeEvent.getObject() instanceof ProxyManager ? beforeEvent.getObject() : new ProxyManager(context);
        GeneralEvent afterEvent = new GeneralEvent(Event.PROXY_MANAGER_AFTER_CREATE, this, proxyManager);
        Ready.post(afterEvent);
        proxyManager = afterEvent.getObject() instanceof ProxyManager ? afterEvent.getObject() : proxyManager;
        return proxyManager;
    }

    @Override
    public InterceptorManager createInterceptorManager() {
        GeneralEvent beforeEvent = new GeneralEvent(Event.INTERCEPTOR_MANAGER_BEFORE_CREATE, this);
        Ready.post(beforeEvent);
        InterceptorManager interceptorManager = beforeEvent.getObject() instanceof InterceptorManager ? beforeEvent.getObject() : new InterceptorManager(context);
        GeneralEvent afterEvent = new GeneralEvent(Event.INTERCEPTOR_MANAGER_AFTER_CREATE, this, interceptorManager);
        Ready.post(afterEvent);
        interceptorManager = afterEvent.getObject() instanceof InterceptorManager ? afterEvent.getObject() : interceptorManager;
        return interceptorManager;
    }

    @Override
    public CacheManager createCacheManager() {
        GeneralEvent beforeEvent = new GeneralEvent(Event.CACHE_MANAGER_BEFORE_CREATE, this);
        Ready.post(beforeEvent);
        CacheManager cacheManager = beforeEvent.getObject() instanceof CacheManager ? beforeEvent.getObject() : new CacheManager(context);
        GeneralEvent afterEvent = new GeneralEvent(Event.CACHE_MANAGER_AFTER_CREATE, this, cacheManager);
        Ready.post(afterEvent);
        cacheManager = afterEvent.getObject() instanceof CacheManager ? afterEvent.getObject() : cacheManager;
        return cacheManager;
    }

    @Override
    public DatabaseManager createDatabaseManager() {
        GeneralEvent beforeEvent = new GeneralEvent(Event.DATABASE_MANAGER_BEFORE_CREATE, this);
        Ready.post(beforeEvent);
        DatabaseManager databaseManager = beforeEvent.getObject() instanceof DatabaseManager ? beforeEvent.getObject() : new DatabaseManager(context);
        GeneralEvent afterEvent = new GeneralEvent(Event.DATABASE_MANAGER_AFTER_CREATE, this, databaseManager);
        Ready.post(afterEvent);
        databaseManager = afterEvent.getObject() instanceof DatabaseManager ? afterEvent.getObject() : databaseManager;
        return databaseManager;
    }

    @Override
    public void createEnd() {

        Ready.post(new GeneralEvent(Event.CORE_INITIALIZER_CREATE_END, this));
    }

    @Override
    public void initBegin() {
        Ready.post(new GeneralEvent(Event.CORE_INITIALIZER_INIT_BEGIN, this));
        context.pluginMap.forEach((name,plugin) -> {
            logger.info("plugin " + name + " initializing");
            plugin.initBegin(context);
        });
        Ready.getMainApp().coreInitBegin();
    }

    @Override
    public void initBeans() {
        context.beanManager.startInit();
        List<Class<?>> components = Ready.classScanner().scanClassByAnnotation(Component.class, true);
        components.forEach(clazz->context.beanManager.initAnnotationMapping(clazz, Component.class));
        List<Class<?>> services = Ready.classScanner().scanClassByAnnotation(Service.class, true);
        services.forEach(clazz->context.beanManager.initAnnotationMapping(clazz, Service.class));
        List<Class<?>> configurations = Ready.classScanner().scanClassByAnnotation(Configuration.class, true);
        configurations.forEach(clazz->{
            context.beanManager.initAnnotationMapping(clazz, Configuration.class);
            context.beanManager.initConfigurationMapping(clazz);
        });
        context.beanManager.endInit();
        Ready.post(new GeneralEvent(Event.BEAN_MANAGER_AFTER_INIT, this, context.beanManager));
    }

    @Override
    public void initProxy() {
        context.proxyManager.startInit();
        context.proxyManager.getProxyGenerator().setPrintGeneratedClassToLogger(Ready.getBootstrapConfig().isPrintGeneratedClassToLogger());
        context.proxyManager.endInit();
        Ready.post(new GeneralEvent(Event.PROXY_MANAGER_AFTER_INIT, this, context.proxyManager));
    }

    @Override
    public void initEvents(){
        Ready.eventManager().startInit();
        List<Class<?>> listeners = Ready.classScanner().scanClassByMethodAnnotation(EventListener.class, true);
        listeners.forEach(clazz -> Ready.eventManager().addListener(clazz));
        Ready.eventManager().endInit();
        Ready.post(new GeneralEvent(Event.EVENT_MANAGER_AFTER_INIT, this, Ready.eventManager()));
    }

    @Override
    public void initInterceptors() {
        context.interceptorManager.startInit();
        List<Class<?>> interceptors = Ready.classScanner().scanClassByAnnotation(GlobalInterceptor.class, true);
        interceptors.forEach(clazz->{
            boolean isForAction = ((GlobalInterceptor)clazz.getAnnotation(GlobalInterceptor.class)).forAction();
            if(Interceptor.class.isAssignableFrom(clazz)){
                try {
                    Interceptor interceptor = (Interceptor)clazz.getDeclaredConstructor().newInstance();
                    if (isForAction) {
                        context.interceptorManager.addGlobalActionInterceptor(interceptor);
                    } else {
                        context.interceptorManager.addGlobalServiceInterceptor(interceptor);
                    }
                } catch (Exception e){
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException(clazz.getCanonicalName() + " is a invalid interceptor, a interceptor should implement from Interceptor.class.");
            }
        });
        context.interceptorManager.endInit();
        Ready.post(new GeneralEvent(Event.INTERCEPTOR_MANAGER_AFTER_INIT, this, context.interceptorManager));
    }

    @Override
    public void initCache() {
        context.cacheManager.startInit();
        context.cacheManager.endInit();
        Ready.post(new GeneralEvent(Event.CACHE_MANAGER_AFTER_INIT, this, context.cacheManager));
    }

    @Override
    public void initDatabases() {
        DatabaseConfig databaseConfig = Ready.getMainApplicationConfig().getDatabase();
        context.dbManager.startInit();
        if(databaseConfig != null){
            if(databaseConfig.getH2server() != null && databaseConfig.getH2server().isEnabled()) {
                context.dbManager.startH2Server(databaseConfig.getH2server());
            }
            if(databaseConfig.getDataSource() != null && databaseConfig.getDataSource().size() > 0){
                databaseConfig.getDataSource().entrySet().stream()
                        .peek(entry-> entry.getValue().setName(entry.getKey()))
                        .sorted(Comparator.comparing(entry->entry.getValue().getTable(), Comparator.nullsLast(String::compareTo)))
                        .forEach(entry -> {
                            if(entry.getValue().isEnabled()) {
                                context.dbManager.assignAgent(entry.getValue());
                            }
                        });

                if(context.dbManager.getDatasourceAgent(MAIN_CONFIG_NAME) != null) {
                    context.dbManager.register(context.dbManager.getDatasourceAgent(MAIN_CONFIG_NAME));
                }
                context.dbManager.getDatasourceAgent((name, agent)->{
                    if(!name.equals(MAIN_CONFIG_NAME)) {
                        context.dbManager.register(agent);
                    }
                });
            }
            tableMapping();
        }
        context.dbManager.endInit();
        Ready.post(new GeneralEvent(Event.DATABASE_MANAGER_AFTER_INIT, this, context.dbManager));
    }

    private void tableMapping() {
        DatabaseConfig databaseConfig = Ready.getMainApplicationConfig().getDatabase();
        if(databaseConfig != null && databaseConfig.getDataSource() != null && databaseConfig.getDataSource().size() > 0){
            List<Class<?>> tables = Ready.classScanner().scanClassByAnnotation(work.ready.core.database.annotation.Table.class, true);
            List<Table> tableList = new ArrayList<>();
            tables.forEach(clazz->{
                Class<? extends Model<?>> modelClass = (Class<? extends Model<?>>)clazz;
                Table tableInfo = TableManager.createTableInfoByAnnotation(modelClass);
                tableList.add(tableInfo);
            });
            if(tableList.size() > 0) {
                databaseConfig.getDataSource().entrySet().stream()
                        .peek(entry -> entry.getValue().setName(entry.getKey()))
                        .filter(entry -> entry.getValue().isEnabled())
                        .sorted(Comparator.comparing(entry -> ((Map.Entry<String, DataSourceConfig>)entry).getValue().getTable(), Comparator.nullsLast(String::compareTo))
                                .thenComparing(entry -> !((Map.Entry<String, DataSourceConfig>)entry).getValue().getName().equalsIgnoreCase(DatabaseManager.MAIN_CONFIG_NAME)))
                        .forEach(entry -> {
                            if (entry.getValue().isAutoMapping()) {
                                List<Table> matchedTables = TableManager.getMatchedTables(entry.getValue(), tableList);
                                if (matchedTables.size() > 0) {
                                    var agent = context.dbManager.getDatasourceAgent(entry.getKey());
                                    for (Table table : matchedTables) {
                                        if (StrUtil.notBlank(table.getPrimaryKey())) {
                                            agent.addMapping(table.getName(), StrUtil.join(table.getPrimaryKey(), ","), table.getModelClass());
                                        } else {
                                            agent.addMapping(table.getName(), table.getModelClass());
                                        }
                                    }
                                }
                            }
                        });
            }
        }
    }

    @Override
    public void initEnd(){
        context.pluginMap.forEach((name,plugin) -> {
            plugin.initEnd(context);
            logger.info("plugin " + name + " loaded");
            Ready.shutdownHook.add(ShutdownHook.STAGE_1, timeout -> plugin.stop());
            Ready.shutdownHook.add(ShutdownHook.STAGE_6, timeout -> plugin.destroy());
            Ready.post(new GeneralEvent(Event.CORE_PLUGIN_LOADED, this, plugin));
        });
        Ready.getMainApp().coreInitEnd();
        Ready.post(new GeneralEvent(Event.CORE_INITIALIZER_INIT_END, this));
    }

}
