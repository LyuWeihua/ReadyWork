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

import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.core.WebSockets;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.tools.define.ConcurrentHashSet;
import work.ready.core.tools.StopWatch;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketChannel implements Channel, Channel.Context {
    private static final Log logger = LogFactory.getLog(WebSocketChannel.class);
    final String id = UUID.randomUUID().toString();
    final Set<String> groups = new ConcurrentHashSet<>();
    final ChannelHandler handler;
    private final io.undertow.websockets.core.WebSocketChannel channel;
    private final Map<String, Object> context = new ConcurrentHashMap<>();
    private final WebSocketContext webSocketContext;
    private final ChannelCallback channelCallback;
    String path;
    String clientIP;

    WebSocketChannel(io.undertow.websockets.core.WebSocketChannel channel, WebSocketContext webSocketContext, ChannelHandler handler, ChannelCallback channelCallback) {
        this.channel = channel;
        this.channelCallback = channelCallback;
        this.webSocketContext = webSocketContext;
        this.handler = handler;
    }

    @Override
    public <T> void send(T message) {
        var watch = new StopWatch();
        String text = null;
        try {
            text = handler.toServerMessage(message);
            WebSockets.sendText(text, channel, channelCallback);
        } finally {
            long elapsed = watch.elapsed();
            logger.debug("send ws message, id=%s, text=%s, elapsed=%s", id, text, elapsed);     
        }
    }

    @Override
    public <T> void broadcast(T message) {
        broadcast(null, message);
    }

    @Override
    public <T> void broadcast(String group, T message) {
        var watch = new StopWatch();
        String text = null;
        try {
            if(group == null) {
                for(String eachGroup : groups) {
                    text = handler.toServerMessage(message);
                    for (Channel ch : webSocketContext.group(eachGroup)) {
                        
                        WebSockets.sendText(text, ((WebSocketChannel) ch).channel, ((WebSocketChannel) ch).channelCallback);
                    }
                }
            } else {
                if (groups.contains(group)) {
                    text = handler.toServerMessage(message);
                    for (Channel ch : webSocketContext.group(group)) {
                        
                        WebSockets.sendText(text, ((WebSocketChannel) ch).channel, ((WebSocketChannel) ch).channelCallback);
                    }
                }
            }
        } finally {
            long elapsed = watch.elapsed();
            logger.debug("send ws message, id=%s, text=%s, elapsed=%s", id, text, elapsed);     
        }
    }

    @Override
    public void close() {
        var watch = new StopWatch();
        try {
            WebSockets.sendClose(CloseMessage.NORMAL_CLOSURE, null, channel, channelCallback);
        } finally {
            long elapsed = watch.elapsed();
            logger.debug("close ws channel, id=%s, elapsed=%s", id, elapsed);
        }
    }

    @Override
    public void join(String group) {
        webSocketContext.join(this, group);
    }

    @Override
    public void leave(String group) {
        webSocketContext.leave(this, group);
    }

    @Override
    public Context context() {
        return this;
    }

    @Override
    public Object get(String key) {
        return context.get(key);
    }

    @Override
    public void put(String key, Object value) {
        if (value == null) context.remove(key);
        else context.put(key, value);
    }
}
