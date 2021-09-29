/**
 *
 * Original work Copyright (c) 2016 Network New Technologies Inc.
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
package work.ready.core.handler;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import work.ready.core.ioc.annotation.Scope;
import work.ready.core.ioc.annotation.ScopeType;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.ApplicationConfig;
import work.ready.core.service.status.Status;

import java.util.HashMap;
import java.util.Map;

@Scope(ScopeType.prototype)
public abstract class BaseHandler implements HttpHandler {
    private static final Log logger = LogFactory.getLog(BaseHandler.class);
    String ERROR_NOT_DEFINED = "ERROR10042";

    protected HandlerManager manager;
    protected ApplicationConfig applicationConfig;
    protected Map<String, Object> config = new HashMap<>();
    private boolean enabled = true;

    private int order = 100;

    public int getOrder() {
        return order;
    }

    public BaseHandler setOrder(int order) {
        this.order = order;
        return this;
    }

    BaseHandler setConfig(Map<String, Object> moduleConfig){
        this.config = moduleConfig;
        return this;
    }

    BaseHandler setManager(HandlerManager manager) {
        this.manager = manager;
        return this;
    }

    BaseHandler setApplicationConfig(ApplicationConfig config) {
        this.applicationConfig = config;
        return this;
    }

    public void initialize() {

    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled){
        this.enabled = enabled;
    }

    public void exception(HttpServerExchange exchange, String code, final Object... args) {
        Status status = new Status(code, args);
        if(status.getHttpCode() == 0) {
            
            status = new Status(ERROR_NOT_DEFINED, code);
        }
        exception(exchange, status);
    }

    public void exception(HttpServerExchange exchange, Status status) {
        exchange.setStatusCode(status.getHttpCode());
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        status.setDescription(status.getDescription().replaceAll("\\\\", "\\\\\\\\"));
        exchange.getResponseSender().send(status.toString());
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        logger.error(status.toString() + " at " + elements[2].getClassName() + "." + elements[2].getMethodName() + "(" + elements[2].getFileName() + ":" + elements[2].getLineNumber() + ")");
    }

}
