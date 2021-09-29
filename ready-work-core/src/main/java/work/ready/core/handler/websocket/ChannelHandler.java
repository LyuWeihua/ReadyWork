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

import work.ready.core.exception.BadRequestException;

import java.io.UncheckedIOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static work.ready.core.tools.StrUtil.format;

public class ChannelHandler {
    final ChannelListener<Object> listener;

    private final BeanMapper<Object> clientMessageMapper;
    private final Class<?> serverMessageClass;
    private final BeanMapper<Object> serverMessageMapper;

    @SuppressWarnings("unchecked")
    public ChannelHandler(BeanMapper<?> clientMessageMapper, Class<?> serverMessageClass, BeanMapper<?> serverMessageMapper, ChannelListener<?> listener) {
        this.clientMessageMapper = (BeanMapper<Object>) clientMessageMapper;
        this.serverMessageClass = serverMessageClass;
        this.serverMessageMapper = (BeanMapper<Object>) serverMessageMapper;
        this.listener = (ChannelListener<Object>) listener;
    }

    String toServerMessage(Object message) {
        if (message == null) throw new Error("message must not be null");
        if (!serverMessageClass.equals(message.getClass())) {
            throw new Error(format("message class does not match, expected=%s, actual=%s", serverMessageClass.getCanonicalName(), message.getClass().getCanonicalName()));
        }
        if (this.serverMessageMapper == null) return (String) message;
        return new String(this.serverMessageMapper.toJSON(message), UTF_8);
    }

    Object fromClientMessage(String message) {
        try {
            if (clientMessageMapper == null) return message;
            return clientMessageMapper.fromJSON(message.getBytes(UTF_8));
        } catch (UncheckedIOException e) {  
            throw new BadRequestException(e.getMessage(), "INVALID_WS_MESSAGE", e);
        }
    }
}
