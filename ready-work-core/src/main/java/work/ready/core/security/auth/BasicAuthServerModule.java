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

package work.ready.core.security.auth;

import io.undertow.Handlers;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import work.ready.core.handler.BaseHandler;
import work.ready.core.handler.ServerModule;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Constant;
import work.ready.core.server.Ready;
import work.ready.core.tools.HttpUtil;

import java.util.Map;
import java.util.Optional;

public class BasicAuthServerModule extends ServerModule {
    private static final Log logger = LogFactory.getLog(BasicAuthServerModule.class);
    private static final String CONFIG_NAME = "basic-auth";
    private static final String MISSING_AUTH_TOKEN = "ERROR10002";
    private static final String INVALID_BASIC_HEADER = "ERROR10046";
    private static final String INVALID_USERNAME_OR_PASSWORD = "ERROR10047";

    private BasicAuthConfig authConfig;
    private volatile BaseHandler next;

    public BasicAuthServerModule() {
        setOrder(3);
        if(logger.isInfoEnabled()) logger.info("BasicAuthServerModule is loaded.");
    }

    public void initialize() {
        authConfig = (BasicAuthConfig) Ready.config().convertItemToObject(config, BasicAuthConfig.class);
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        String auth = exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION);
        if(auth == null || auth.trim().length() == 0) {
            exception(exchange, MISSING_AUTH_TOKEN);
            return;
        } else {
            
            String basic = auth.substring(0, 5);
            if("BASIC".equalsIgnoreCase(basic)) {
                String credentials = auth.substring(6);
                int pos = credentials.indexOf(':');
                if (pos == -1) {
                    credentials = new String(HttpUtil.decodeBase64(credentials), Constant.DEFAULT_CHARSET);
                }
                pos = credentials.indexOf(':');
                if (pos != -1) {
                    String username = credentials.substring(0, pos);
                    String password = credentials.substring(pos + 1);
                    Optional<Map<String, Object>> result = authConfig.getUsers().stream()
                            .filter(user -> user.get("username").equals(username) && user.get("password").equals(password))
                            .findFirst();
                    if(result.isEmpty()) {
                        exception(exchange, INVALID_USERNAME_OR_PASSWORD);
                        return;
                    }
                } else {
                    exception(exchange, INVALID_BASIC_HEADER, auth);
                    return;
                }
            } else {
                exception(exchange, INVALID_BASIC_HEADER, auth);
                return;
            }
            manager.next(exchange, next);
        }
    }

    @Override
    public BaseHandler getNext() {
        return next;
    }

    @Override
    public ServerModule setNext(final BaseHandler next) {
        Handlers.handlerNotNull(next);
        this.next = next;
        return this;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void register() {
        manager.registerModule(BasicAuthServerModule.class.getName(), Ready.config().getMapConfigNoCache(CONFIG_NAME));
    }
}
