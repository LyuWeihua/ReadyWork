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
import work.ready.core.component.proxy.ProxyManager;
import work.ready.core.database.DatabaseManager;
import work.ready.core.ioc.BeanManager;

public interface CoreContextInitializer {

    void setContext(CoreContext context);

    CoreContext getContext();

    void createBegin();

    ProxyManager createProxyManager();

    BeanManager createBeanManager();

    InterceptorManager createInterceptorManager();

    CacheManager createCacheManager();

    DatabaseManager createDatabaseManager();

    void createEnd();

    void initBegin();

    void initProxy();

    void initEvents();

    void initBeans();

    void initInterceptors();

    void initCache();

    void initDatabases();

    void initEnd();
}
