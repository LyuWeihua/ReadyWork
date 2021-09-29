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

import io.undertow.websockets.core.*;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

final class WebSocketMessageListener extends AbstractReceiveListener {
    private static final Log logger = LogFactory.getLog(WebSocketMessageListener.class);
    private static final long MAX_TEXT_MESSAGE_SIZE = 10L * 1024 * 1024;     
    private final WebSocketManager manager;

    public WebSocketMessageListener(WebSocketManager manager){
        this.manager = manager;
    }

    @Override
    protected void onFullTextMessage(io.undertow.websockets.core.WebSocketChannel channel, BufferedTextMessage textMessage) {
        var wrapper = (WebSocketChannel) channel.getAttribute(WebSocketHandler.CHANNEL_KEY);
        logger.info("=== ws message handling begin ===");
        try {

            String data = textMessage.getData();
            logger.debug("[channel] message=%s", data);     

            Object message = wrapper.handler.fromClientMessage(data);
            wrapper.handler.listener.onMessage(wrapper, message);
        } catch (Throwable e) {
            WebSockets.sendClose(CloseMessage.UNEXPECTED_ERROR, e.getMessage(), channel, manager.getChannelCallback());
        } finally {
            logger.info("=== ws message handling end ===");
        }
    }

    @Override
    protected void onCloseMessage(CloseMessage message, io.undertow.websockets.core.WebSocketChannel channel) {
        var wrapper = (WebSocketChannel) channel.getAttribute(WebSocketHandler.CHANNEL_KEY);
        logger.info("=== ws close message handling begin ===");
        try {

            int code = message.getCode();
            String reason = message.getReason();
            logger.debug("[channel] reason=%s", reason);

            wrapper.handler.listener.onClose(wrapper, code, reason);
        } catch (Throwable e) {
        } finally {
            logger.info("=== ws close message handling end ===");
        }
    }

    @Override
    protected long getMaxTextBufferSize() {
        return MAX_TEXT_MESSAGE_SIZE;
    }
}
