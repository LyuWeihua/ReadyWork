/**
 *
 * Original work Copyright core-ng
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
package work.ready.core.handler.websocket;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.PathTemplateMatcher;
import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.core.protocol.Handshake;
import io.undertow.websockets.core.protocol.version13.Hybi13Handshake;
import io.undertow.websockets.spi.AsyncWebSocketHttpServerExchange;
import org.xnio.IoUtils;
import work.ready.core.exception.BadRequestException;
import work.ready.core.exception.NotFoundException;
import work.ready.core.handler.RequestMethod;
import work.ready.core.handler.request.HttpRequest;
import work.ready.core.handler.session.HttpSession;
import work.ready.core.handler.session.SessionManager;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static io.undertow.util.PathTemplateMatch.ATTACHMENT_KEY;

public class WebSocketHandler {
    protected static final Log logger = LogFactory.getLog(WebSocketHandler.class);
    static final String CHANNEL_KEY = "CHANNEL";
    public final WebSocketContext context = new WebSocketContext();
    public final ChannelCallback channelCallback;

    private final Handshake handshake = new Hybi13Handshake();
    protected Map<String, Map<String, Map<RequestMethod, PathTemplateMatcher<ChannelHandler>>>> matcherMap = new HashMap<>(16,0.5F);

    private final Set<io.undertow.websockets.core.WebSocketChannel> channels = ConcurrentHashMap.newKeySet();
    private final ChannelCloseListener channelCloseListener = new ChannelCloseListener(context);

    private final WebSocketMessageListener messageListener;
    private SessionManager sessionManager;
    private WebSocketManager manager;

    public WebSocketHandler(WebSocketManager manager) {
        this.manager = manager;
        this.channelCallback = manager.getChannelCallback();
        this.messageListener = new WebSocketMessageListener(manager);
    }

    public void setSessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public boolean checkWebSocket(RequestMethod method, HeaderMap headers) {
        if (method == RequestMethod.GET && headers.getFirst(Headers.SEC_WEB_SOCKET_KEY) != null) {
            String version = headers.getFirst(Headers.SEC_WEB_SOCKET_VERSION);
            if ("13".equals(version)) return true;  
            throw new BadRequestException("only support web socket version 13, version=" + version, "INVALID_HTTP_REQUEST");
        }
        return false;
    }

    public void handle(HttpServerExchange exchange, HttpRequest request) {
        String path = exchange.getRequestPath();

        ChannelHandler handler = getHandler(request, path);
        if (handler == null) throw new NotFoundException("not found, path=" + path, "PATH_NOT_FOUND");

        if(sessionManager != null) {
            request.session = loadSession(request);
            if (request.session == null) {
                request.session = sessionManager.createSession(exchange);
            }
        }

        var webSocketExchange = new AsyncWebSocketHttpServerExchange(exchange, channels);
        exchange.upgradeChannel((connection, httpServerExchange) -> {
            io.undertow.websockets.core.WebSocketChannel channel = handshake.createChannel(webSocketExchange, connection, webSocketExchange.getBufferPool());
            try {
                var wrapper = new WebSocketChannel(channel, context, handler, channelCallback);
                wrapper.path = path;
                wrapper.clientIP = request.getClientIP();
                logger.info("channel %s", wrapper.id);
                channel.setAttribute(CHANNEL_KEY, wrapper);
                channel.addCloseTask(channelCloseListener);
                context.add(wrapper);

                handler.listener.onConnect(request, wrapper);
                logger.info("group %s", wrapper.groups.toArray()); 
                channel.getReceiveSetter().set(messageListener);
                channel.resumeReceives();

                channels.add(channel);
            } catch (Throwable e) {
                
                IoUtils.safeClose(connection);
            }
        });
        handshake.handshake(webSocketExchange);
    }

    public ChannelHandler getHandler(HttpRequest request, String requestPath) {
        ChannelHandler handler = null;
        String hostName = request.getHostName().toLowerCase();

        for (String host : matcherMap.keySet()) {
            if (host.equals("*") || hostName.endsWith(host)) {
                var subHostMap = matcherMap.get(host);
                for (String subHost : subHostMap.keySet()) {
                    boolean matched = false;
                    if (!host.equals("*") && !subHost.equals("*") && !hostName.equals(host)) {
                        String subDomain = hostName.substring(0, hostName.length() - host.length());
                        if (subDomain.length() > 0 && subDomain.endsWith(".")) {
                            subDomain = subDomain.substring(0, subDomain.length() - 1); 
                            if (subHost.contains("*")) {
                                if (subHost.startsWith("*")) {
                                    String forCheck = subHost.substring(1);
                                    if (subDomain.endsWith(forCheck)) matched = true;
                                } else if (subHost.endsWith("*")) {
                                    String forCheck = subHost.substring(0, subHost.length() - 1);
                                    if (subDomain.startsWith(forCheck)) matched = true;
                                } else if (subHost.startsWith("*") && subHost.endsWith("*")) {
                                    String forCheck = subHost.substring(1, subHost.length() - 2);
                                    if (subDomain.contains(forCheck)) matched = true;
                                }
                            } else {
                                if (subDomain.equals(subHost)) matched = true;
                            }
                        }
                    } else {
                        matched = true;
                    }
                    if (matched) {
                        var methodMap = subHostMap.get(subHost);
                        PathTemplateMatcher<ChannelHandler> pathTemplateMatcher = methodMap.get(request.getMethod());
                        if (pathTemplateMatcher != null) {
                            PathTemplateMatcher.PathMatchResult<ChannelHandler> result = pathTemplateMatcher
                                    .match(requestPath);
                            if (result != null) {
                                request.getExchange().putAttachment(ATTACHMENT_KEY,
                                        new io.undertow.util.PathTemplateMatch(result.getMatchedTemplate(), result.getParameters()));
                                for (Map.Entry<String, String> entry : result.getParameters().entrySet()) {
                                    request.getExchange().addPathParam(entry.getKey(), entry.getValue());
                                }
                                handler = result.getValue();
                                return handler;
                            }
                        }
                    }
                }
            }
        }
        return handler;
    }

    HttpSession loadSession(HttpRequest request) {
        HttpSession session = sessionManager.getSession(request.getExchange());
        if (session == null) return null;
        return new ReadOnlySession(session);
    }

    public void shutdown() {
        for (var channel : channels) {
            WebSockets.sendClose(CloseMessage.GOING_AWAY, "server is shutting down", channel, channelCallback);
        }
    }
}
