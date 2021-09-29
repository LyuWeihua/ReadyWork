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

import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketContext {
    private static final Log logger = LogFactory.getLog(WebSocketContext.class);
    private final Map<String, Map<String, Channel>> groups = new ConcurrentHashMap<>();
    private final Map<String, Channel> channels = new ConcurrentHashMap<>();

    public List<Channel> all() {
        return List.copyOf(channels.values());
    }

    public List<Channel> group(String name) {
        Map<String, Channel> channels = groups.get(name);
        if (channels == null) return List.of();
        return List.copyOf(channels.values());
    }

    void join(WebSocketChannel channel, String group) {
        logger.debug("join group, channel=%s, group=%s", channel.id, group);
        channel.groups.add(group);
        groups.computeIfAbsent(group, key -> new ConcurrentHashMap<>()).put(channel.id, channel);
    }

    void leave(WebSocketChannel channel, String group) {
        logger.debug("leave group, channel=%s, group=%s", channel.id, group);
        channel.groups.remove(group);
        Map<String, Channel> channels = groups.get(group);
        if (channels != null) channels.remove(channel.id);
    }

    void add(WebSocketChannel channel) {
        channels.put(channel.id, channel);
    }

    void remove(WebSocketChannel channel) {
        channels.remove(channel.id);
        for (String group : channel.groups) {
            groups.get(group).remove(channel.id);
        }
    }
}
