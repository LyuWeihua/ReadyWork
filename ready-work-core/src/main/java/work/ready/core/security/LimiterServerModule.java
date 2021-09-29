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

package work.ready.core.security;

import io.undertow.Handlers;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import work.ready.core.handler.BaseHandler;
import work.ready.core.handler.ServerModule;
import work.ready.core.handler.request.RequestParser;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.security.access.limiter.Limiter;
import work.ready.core.security.access.limiter.limit.Limit;
import work.ready.core.security.access.limiter.limit.LimitBuilder;
import work.ready.core.server.Ready;
import work.ready.core.service.status.Status;

import java.net.InetAddress;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class LimiterServerModule extends ServerModule {

    private static final Log logger = LogFactory.getLog(LimiterServerModule.class);
    private final SecurityManager securityManager;
    private final LimiterConfig limiterConfig;

    private volatile BaseHandler next;

    private String TOO_MANY_REQUESTS = "ERROR10999";
    private String ACCESS_DENIED = "ERROR10998";

    public LimiterServerModule(SecurityManager securityManager){
        this.securityManager = securityManager;
        this.limiterConfig = securityManager.getConfig().getLimiter();
        setOrder(1);  
        if(logger.isInfoEnabled()) logger.info("LimiterServerModule is loaded.");
    }

    @Override
    public void initialize(){

    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        
        exchange.getResponseHeaders().put(Headers.DATE,
                DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.ofInstant(Ready.now().toInstant(), ZoneOffset.UTC)));

        Map<String, String> headValues = null;
        InetAddress ip = exchange.getSourceAddress().getAddress();
        boolean skip = (limiterConfig.isEnableSkipLocalIp() && AccessControl.isLocal(ip));
        if(!skip) {
            if (securityManager.getAccessControl() != null && securityManager.getAccessControl().haveRules()) {
                if (securityManager.pathShouldHandle(exchange.getRequestPath(), SecurityManager.IpControl)) {
                    boolean result = true;
                    if (securityManager.getAccessControl().haveIpRules()) { 
                        
                        result = securityManager.getAccessControl().validateIp(ip);
                    }
                    if (result && securityManager.getAccessControl().haveHeadRules()) {
                        headValues = getHeadValues(exchange);
                        result = securityManager.getAccessControl().validateHead(headValues);
                    }
                    if (!result) {
                        Status status = new Status(ACCESS_DENIED);
                        logger.debug(status.toString());
                        exchange.setStatusCode(securityManager.getAccessControl().getDenyResponseCode());
                        exchange.getResponseSender().send(status.toString());
                        exchange.endExchange();
                        return;
                    }
                }
            }

            if (limiterConfig.isEnableIpBasedLimiter()) {
                if (securityManager.pathShouldHandle(exchange.getRequestPath(), SecurityManager.FrequencyLimiter)) {
                    int duration = limiterConfig.getDurationOfLimitForIp();
                    Limit<String> ipLimit = LimitBuilder.of("perIp")
                            .to(limiterConfig.getLimitCapacityForIp())
                            .per(Duration.ofSeconds(duration))
                            .build();

                    Limiter<String> limiter = securityManager.getLimiter("ipLimiter", ipLimit);
                    boolean result = limiter.tryCall(ip.getHostAddress());
                    
                    if (result && limiterConfig.extraLimitList != null) {
                        if (headValues == null) headValues = getHeadValues(exchange);
                        for (Limit<String> limit : limiterConfig.extraLimitList) {
                            if (headValues.containsKey(limit.getName())) {
                                boolean match = false;
                                for (String keyword : limiterConfig.extraLimitParam.get(limit.getName())) {
                                    if (headValues.get(limit.getName()).contains(keyword)) {
                                        match = true;
                                        break;
                                    }
                                }
                                if (match) {
                                    limiter = securityManager.getLimiter("headerLimiter", limit);
                                    result = limiter.tryCall(ip + limit.getName()); 
                                    if (!result) {
                                        duration = (int) limit.getExpiration().getSeconds();
                                        break;
                                    }
                                }
                            } else {
                                logger.debug("Limiter missed ExtraLimit config type: " + limit.getName() + ", for this request, there are options: " + headValues.keySet().toString());
                            }
                        }
                    }

                    if (!result) {
                        Status status = new Status(TOO_MANY_REQUESTS);
                        logger.debug(status.toString());
                        exchange.setStatusCode(StatusCodes.TOO_MANY_REQUESTS);

                        exchange.getResponseHeaders().add(Headers.RETRY_AFTER, duration);
                        exchange.getResponseSender().send(status.toString());
                        exchange.endExchange();
                        return;
                    }
                }
            }

            if (limiterConfig.isEnableDownloadRateLimiter()) {
                if (securityManager.pathShouldHandle(exchange.getRequestPath(), SecurityManager.RateLimiter)) {
                    exchange.addResponseWrapper(securityManager.getRateLimiter());
                }
            }
            if (limiterConfig.isEnableConcurrentRequestLimiter()) {
                if (securityManager.pathShouldHandle(exchange.getRequestPath(), SecurityManager.ConcurrentLimiter)) {
                    securityManager.getRequestLimiter().handleRequest(exchange, next);
                    return;
                }
            }
        }
        manager.next(exchange, next);
    }

    private Map<String, String> getHeadValues(HttpServerExchange exchange){
        Map<String, String> requestValues = new HashMap<>();
        requestValues.put("Method", exchange.getRequestMethod().toString());
        requestValues.put("Query", exchange.getQueryString());
        requestValues.put("Url", exchange.getRequestURL());
        exchange.getRequestHeaders().getHeaderNames().forEach(name->{
            requestValues.put(name.toString(), Optional.ofNullable(exchange.getRequestHeaders().get(name).peekFirst()).orElse(""));
        });
        return requestValues;
    }

    private String parseClientIP(HttpServerExchange exchange, String xForwardedFor) {
        String remoteAddress = exchange.getSourceAddress().getAddress().getHostAddress();
        logger.debug("[request] remoteAddress=%s", remoteAddress);
        return RequestParser.clientIPParser.parse(remoteAddress, xForwardedFor);
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
        manager.registerModule(LimiterServerModule.class.getName(), config);
    }
}
