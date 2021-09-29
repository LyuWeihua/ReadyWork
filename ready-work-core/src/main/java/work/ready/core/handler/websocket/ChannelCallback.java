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

import io.undertow.websockets.core.WebSocketCallback;
import io.undertow.websockets.core.WebSocketChannel;

import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

public class ChannelCallback implements WebSocketCallback<Void> {
    private static final Log logger = LogFactory.getLog(ChannelCallback.class);

    @Override
    public void complete(WebSocketChannel channel, Void context) {
    }

    @Override
    public void onError(WebSocketChannel channel, Void context, Throwable exception) {
        logger.warn(exception, exception.getMessage());
    }
}
