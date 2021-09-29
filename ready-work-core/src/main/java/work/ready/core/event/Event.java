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

package work.ready.core.event;

public class Event {

    public static final String READY_WORK_SERVER_START = "READY_WORK_SERVER_START";

    public static final String READY_WORK_TIME_INIT = "READY_WORK_TIME_INIT";

    public static final String CORE_INITIALIZER_CREATE_BEGIN = "CORE_INITIALIZER_CREATE_BEGIN";

    public static final String CORE_INITIALIZER_CREATE_END = "CORE_INITIALIZER_CREATE_END";

    public static final String CORE_INITIALIZER_INIT_BEGIN = "CORE_INITIALIZER_INIT_BEGIN";

    public static final String CORE_INITIALIZER_INIT_END = "CORE_INITIALIZER_INIT_END";

    public static final String CORE_PLUGIN_LOADED = "CORE_PLUGIN_LOADED";

    public static final String CLASS_SCANNER_CREATE = "CLASS_SCANNER_CREATE";

    public static final String APM_MANAGER_BEFORE_CREATE = "APM_MANAGER_BEFORE_CREATE";

    public static final String APM_MANAGER_CREATE = "APM_MANAGER_CREATE";

    public static final String APM_MANAGER_AFTER_CREATE = "APM_MANAGER_AFTER_CREATE";

    public static final String APM_MANAGER_AFTER_INIT = "APM_MANAGER_AFTER_INIT";

    public static final String BEAN_MANAGER_BEFORE_CREATE = "BEAN_MANAGER_BEFORE_CREATE";

    public static final String BEAN_MANAGER_CREATE = "BEAN_MANAGER_CREATE";

    public static final String BEAN_MANAGER_AFTER_CREATE = "BEAN_MANAGER_AFTER_CREATE";

    public static final String BEAN_MANAGER_AFTER_INIT = "BEAN_MANAGER_AFTER_INIT";

    public static final String PROXY_MANAGER_BEFORE_CREATE = "PROXY_MANAGER_BEFORE_CREATE";

    public static final String PROXY_MANAGER_CREATE = "PROXY_MANAGER_CREATE";

    public static final String PROXY_MANAGER_AFTER_CREATE = "PROXY_MANAGER_AFTER_CREATE";

    public static final String PROXY_MANAGER_AFTER_INIT = "PROXY_MANAGER_AFTER_INIT";

    public static final String INTERCEPTOR_MANAGER_BEFORE_CREATE = "INTERCEPTOR_MANAGER_BEFORE_CREATE";

    public static final String INTERCEPTOR_MANAGER_CREATE = "INTERCEPTOR_MANAGER_CREATE";

    public static final String INTERCEPTOR_MANAGER_AFTER_CREATE = "INTERCEPTOR_MANAGER_AFTER_CREATE";

    public static final String INTERCEPTOR_MANAGER_AFTER_INIT = "INTERCEPTOR_MANAGER_AFTER_INIT";

    public static final String EVENT_MANAGER_AFTER_INIT = "EVENT_MANAGER_AFTER_INIT";

    public static final String CACHE_MANAGER_BEFORE_CREATE = "CACHE_MANAGER_BEFORE_CREATE";

    public static final String CACHE_MANAGER_CREATE = "CACHE_MANAGER_CREATE";

    public static final String CACHE_MANAGER_AFTER_CREATE = "CACHE_MANAGER_AFTER_CREATE";

    public static final String CACHE_MANAGER_AFTER_INIT = "CACHE_MANAGER_AFTER_INIT";

    public static final String DATABASE_MANAGER_BEFORE_CREATE = "DATABASE_MANAGER_BEFORE_CREATE";

    public static final String DATABASE_MANAGER_CREATE = "DATABASE_MANAGER_CREATE";

    public static final String DATABASE_MANAGER_AFTER_CREATE = "DATABASE_MANAGER_AFTER_CREATE";

    public static final String DATABASE_MANAGER_AFTER_INIT = "DATABASE_MANAGER_AFTER_INIT";

    public static final String APP_INITIALIZER_CREATE_BEGIN = "APP_INITIALIZER_CREATE_BEGIN";

    public static final String APP_INITIALIZER_CREATE_END = "APP_INITIALIZER_CREATE_END";

    public static final String APP_FRAMEWORK_INIT_BEGIN = "APP_FRAMEWORK_INIT_BEGIN";

    public static final String APP_FRAMEWORK_INIT_END = "APP_FRAMEWORK_INIT_END";

    public static final String APP_STARTED = "APP_STARTED";

    public static final String MODULE_FRAMEWORK_INIT_BEGIN = "MODULE_FRAMEWORK_INIT_BEGIN";

    public static final String MODULE_FRAMEWORK_INIT_END = "MODULE_FRAMEWORK_INIT_END";

    public static final String ROUTE_MANAGER_BEFORE_CREATE = "ROUTE_MANAGER_BEFORE_CREATE";

    public static final String ROUTE_MANAGER_CREATE = "ROUTE_MANAGER_CREATE";

    public static final String ROUTE_MANAGER_AFTER_CREATE = "ROUTE_MANAGER_AFTER_CREATE";

    public static final String ROUTE_MANAGER_AFTER_INIT = "ROUTE_MANAGER_AFTER_INIT";

    public static final String MODULE_INIT_ROUTE = "MODULE_INIT_ROUTE";

    public static final String SECURITY_MANAGER_BEFORE_CREATE = "SECURITY_MANAGER_BEFORE_CREATE";

    public static final String SECURITY_MANAGER_CREATE = "SECURITY_MANAGER_CREATE";

    public static final String SECURITY_MANAGER_AFTER_CREATE = "SECURITY_MANAGER_AFTER_CREATE";

    public static final String SECURITY_MANAGER_AFTER_INIT = "SECURITY_MANAGER_AFTER_INIT";

    public static final String MODULE_INIT_SECURITY = "MODULE_INIT_SECURITY";

    public static final String HANDLER_MANAGER_BEFORE_CREATE = "HANDLER_MANAGER_BEFORE_CREATE";

    public static final String HANDLER_MANAGER_CREATE = "HANDLER_MANAGER_CREATE";

    public static final String HANDLER_MANAGER_AFTER_CREATE = "HANDLER_MANAGER_AFTER_CREATE";

    public static final String HANDLER_MANAGER_AFTER_INIT = "HANDLER_MANAGER_AFTER_INIT";

    public static final String MODULE_INIT_HANDLER = "MODULE_INIT_HANDLER";

    public static final String RENDER_MANAGER_BEFORE_CREATE = "RENDER_MANAGER_BEFORE_CREATE";

    public static final String RENDER_MANAGER_CREATE = "RENDER_MANAGER_CREATE";

    public static final String RENDER_MANAGER_AFTER_CREATE = "RENDER_MANAGER_AFTER_CREATE";

    public static final String RENDER_MANAGER_AFTER_INIT = "RENDER_MANAGER_AFTER_INIT";

    public static final String MODULE_INIT_RENDER = "MODULE_INIT_RENDER";

    public static final String SESSION_MANAGER_BEFORE_INIT = "SESSION_MANAGER_BEFORE_INIT";

    public static final String SESSION_MANAGER_AFTER_INIT = "SESSION_MANAGER_AFTER_INIT";

    public static final String WEB_SOCKET_MANAGER_BEFORE_CREATE = "WEB_SOCKET_MANAGER_BEFORE_CREATE";

    public static final String WEB_SOCKET_MANAGER_CREATE = "WEB_SOCKET_MANAGER_CREATE";

    public static final String WEB_SOCKET_MANAGER_AFTER_CREATE = "WEB_SOCKET_MANAGER_AFTER_CREATE";

    public static final String WEB_SOCKET_MANAGER_AFTER_INIT = "WEB_SOCKET_MANAGER_AFTER_INIT";

    public static final String MODULE_INIT_WEB_SOCKET = "MODULE_INIT_WEB_SOCKET";

    public static final String WEB_SERVER_BEFORE_CREATE = "WEB_SERVER_BEFORE_CREATE";

    public static final String WEB_SERVER_CREATE = "WEB_SERVER_CREATE";

    public static final String WEB_SERVER_AFTER_CREATE = "WEB_SERVER_AFTER_CREATE";

    public static final String WEB_SERVER_AFTER_INIT = "WEB_SERVER_AFTER_INIT";

    public static final String MODULE_INIT_WEB_SERVER = "MODULE_INIT_WEB_SERVER";

    public static final String WEB_SERVER_AFTER_HANDLER_INIT = "WEB_SERVER_AFTER_HANDLER_INIT";

    public static final String WEB_SERVER_STARTED = "WEB_SERVER_STARTED";

    public static final String READY_FOR_WORK = "READY_FOR_WORK";

    public static final String WEB_HTTP_RESPONSE_DONE = "WEB_HTTP_RESPONSE_DONE";

    public static final String WEB_SERVER_BEFORE_SHUTDOWN = "WEB_SERVER_BEFORE_SHUTDOWN";

    public static final String WEB_SERVER_AFTER_SHUTDOWN = "WEB_SERVER_AFTER_SHUTDOWN";

    public static final String LIMITER_CONFIG_CHANGED = "LIMITER_CONFIG_CHANGED";

    public static final String TEST_EVENT = "TEST_EVENT";

}
