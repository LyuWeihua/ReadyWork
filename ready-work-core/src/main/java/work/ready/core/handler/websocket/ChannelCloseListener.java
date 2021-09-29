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

import static work.ready.core.handler.websocket.WebSocketHandler.CHANNEL_KEY;

class ChannelCloseListener implements org.xnio.ChannelListener<io.undertow.websockets.core.WebSocketChannel> {
    private final WebSocketContext context;

    ChannelCloseListener(WebSocketContext context) {
        this.context = context;
    }

    @Override
    public void handleEvent(io.undertow.websockets.core.WebSocketChannel channel) {
        var wrapper = (WebSocketChannel) channel.getAttribute(CHANNEL_KEY);
        remove(wrapper);
    }

    void remove(WebSocketChannel wrapper) {
        context.remove(wrapper);
    }
}
