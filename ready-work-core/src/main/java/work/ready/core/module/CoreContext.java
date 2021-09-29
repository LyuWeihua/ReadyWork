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

import work.ready.core.aop.InterceptorManager;
import work.ready.core.component.cache.CacheManager;
import work.ready.core.component.plugin.BaseCorePlugin;
import work.ready.core.component.proxy.ProxyManager;
import work.ready.core.database.*;
import work.ready.core.ioc.BeanManager;
import work.ready.core.server.Ready;

import java.util.*;

public class CoreContext {
    protected ProxyManager proxyManager;
    protected BeanManager beanManager;
    protected InterceptorManager interceptorManager;
    protected CacheManager cacheManager;
    protected DatabaseManager dbManager;
    protected Map<String, BaseCorePlugin> pluginMap;

    private CoreContextInitializer initializer;

    public CoreContext(Map<String, BaseCorePlugin> pluginMap) {
        ServiceLoader<CoreContextInitializer> coreInitializerServiceLoader =
                ServiceLoader.load(CoreContextInitializer.class, CoreContext.class.getClassLoader());
        initializer = coreInitializerServiceLoader.findFirst().orElse(new DefaultCoreInitializer());
        initializer.setContext(this);
        this.pluginMap = pluginMap;
    }

    private void create() {

        initializer.createBegin();

        beanManager = initializer.createBeanManager();

        proxyManager = initializer.createProxyManager();

        interceptorManager = initializer.createInterceptorManager();

        initializer.initBeans();
        initializer.initProxy();
        initializer.initEvents();
        initializer.initInterceptors();

        cacheManager = initializer.createCacheManager();

        dbManager = initializer.createDatabaseManager();

        initializer.createEnd();

        beanManager.addSingletonObject(beanManager);
        beanManager.addSingletonObject(proxyManager);
        beanManager.addSingletonObject(interceptorManager);
        beanManager.addSingletonObject(cacheManager);
        beanManager.addSingletonObject(dbManager);
        beanManager.addSingletonObject(this);
    }

    public void init(){

        this.create();

        initializer.initBegin();

        initializer.initCache();

        initializer.initDatabases();

        initializer.initEnd();

        Ready.shutdownHook.add(ShutdownHook.STAGE_7, timeout -> dbManager.destroy());
    }

    public ProxyManager getProxyManager() {
        return proxyManager;
    }

    public BeanManager getBeanManager() {
        return beanManager;
    }

    public InterceptorManager getInterceptorManager() {
        return interceptorManager;
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    public DatabaseManager getDbManager() {
        return dbManager;
    }

    public void validate() {

    }
}
