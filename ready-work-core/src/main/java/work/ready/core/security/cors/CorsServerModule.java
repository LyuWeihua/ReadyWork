
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

package work.ready.core.security.cors;

import io.undertow.Handlers;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import work.ready.core.config.Config;
import work.ready.core.handler.BaseHandler;
import work.ready.core.handler.ServerModule;

import java.util.Collection;

import static io.undertow.server.handlers.ResponseCodeHandler.HANDLE_200;

public class CorsServerModule extends ServerModule {

    private CorsConfig corsConfig;
    private Collection<String> allowedOrigins;
    private Collection<String> allowedMethods;

    private volatile BaseHandler next;
    
    private static final long ONE_HOUR_IN_SECONDS = 60 * 60;

    public CorsServerModule() {
        setOrder(0); 
        
    }

    @Override
    public void initialize(){
        if(config != null)
        corsConfig = (CorsConfig) Config.convertItemToObject(config, CorsConfig.class);
        if(corsConfig != null) {
            setEnabled(corsConfig.isEnabled());
            allowedOrigins = corsConfig.getAllowedOrigins();
            allowedMethods = corsConfig.getAllowedMethods();
        }
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        HeaderMap headers = exchange.getRequestHeaders();
        if (CorsUtil.requestMatched(headers)) {
            if (CorsUtil.isCorsRequest(exchange)) {
                handleCorsRequest(exchange);
                return;
            }
            setCorsResponseHeaders(exchange);
        }
        manager.next(exchange, next);
    }

    private void handleCorsRequest(HttpServerExchange exchange) throws Exception {
        setCorsResponseHeaders(exchange);
        HANDLE_200.handleRequest(exchange);
    }

    private void setCorsResponseHeaders(HttpServerExchange exchange) throws Exception {
        HeaderMap headers = exchange.getRequestHeaders();
        if (headers.contains(Headers.ORIGIN)) {
            if(CorsUtil.matchOrigin(exchange, allowedOrigins) != null) {
                exchange.getResponseHeaders().addAll(CorsUtil.ACCESS_CONTROL_ALLOW_ORIGIN, headers.get(Headers.ORIGIN));
                exchange.getResponseHeaders().add(Headers.VARY, Headers.ORIGIN_STRING);
            }
        }
        exchange.getResponseHeaders().addAll(CorsUtil.ACCESS_CONTROL_ALLOW_METHODS, allowedMethods);
        HeaderValues requestedHeaders = headers.get(CorsUtil.ACCESS_CONTROL_REQUEST_HEADERS);
        if (requestedHeaders != null && !requestedHeaders.isEmpty()) {
            exchange.getResponseHeaders().addAll(CorsUtil.ACCESS_CONTROL_ALLOW_HEADERS, requestedHeaders);
        } else {
            exchange.getResponseHeaders().add(CorsUtil.ACCESS_CONTROL_ALLOW_HEADERS, Headers.CONTENT_TYPE_STRING);
            exchange.getResponseHeaders().add(CorsUtil.ACCESS_CONTROL_ALLOW_HEADERS, Headers.WWW_AUTHENTICATE_STRING);
            exchange.getResponseHeaders().add(CorsUtil.ACCESS_CONTROL_ALLOW_HEADERS, Headers.AUTHORIZATION_STRING);
        }
        exchange.getResponseHeaders().add(CorsUtil.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        exchange.getResponseHeaders().add(CorsUtil.ACCESS_CONTROL_MAX_AGE, ONE_HOUR_IN_SECONDS);
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
    public void register() {
        manager.registerModule(CorsServerModule.class.getName(), config);
    }

}
