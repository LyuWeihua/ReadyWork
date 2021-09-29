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

package work.ready.core.handler.request;

import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import org.xnio.ChannelListener;
import org.xnio.IoUtils;
import org.xnio.channels.StreamSourceChannel;
import work.ready.core.handler.RequestHandler;

import java.nio.ByteBuffer;

import static work.ready.core.tools.StrUtil.format;

public final class RequestBodyReader implements ChannelListener<StreamSourceChannel> {
    static final AttachmentKey<RequestBody> REQUEST_BODY = AttachmentKey.create(RequestBody.class);

    private final HttpServerExchange exchange;
    private final RequestHandler handler;
    private final int contentLength;
    private boolean complete;
    private byte[] body;
    private int position = 0;

    public RequestBodyReader(HttpServerExchange exchange, RequestHandler handler) {
        this.exchange = exchange;
        this.handler = handler;
        contentLength = (int) exchange.getRequestContentLength();
        if (contentLength >= 0) body = new byte[contentLength];
    }

    @Override
    public void handleEvent(StreamSourceChannel channel) {
        read(channel);
        if (complete) {
            exchange.dispatch(handler);
        }
    }

    public void read(StreamSourceChannel channel) {
        try (PooledByteBuffer poolItem = exchange.getConnection().getByteBufferPool().allocate()) {
            ByteBuffer buffer = poolItem.getBuffer();
            int bytesRead;
            while (true) {
                buffer.clear();
                bytesRead = channel.read(buffer);
                if (bytesRead <= 0) break;
                buffer.flip();
                ensureCapacity(bytesRead);
                buffer.get(body, position, bytesRead);
                position += bytesRead;
            }
            if (bytesRead == -1) {
                if (contentLength >= 0 && position < body.length) {
                    throw new Error(format("body ends prematurely, expected=%s, actual=%s", contentLength, position));
                } else if (body == null) {
                    body = new byte[0]; 
                }
                complete = true;
                exchange.putAttachment(REQUEST_BODY, new RequestBody(body, null));
            }
        } catch (Throwable e) { 
            IoUtils.safeClose(channel);
            complete = true;
            exchange.putAttachment(REQUEST_BODY, new RequestBody(null, e));
        }
    }

    private void ensureCapacity(int bytesRead) {
        if (contentLength >= 0) {
            if (bytesRead + position > contentLength) throw new Error("body exceeds expected content length, expected=" + contentLength);
        } else {
            if (body == null) { 
                body = new byte[bytesRead];
            } else {
                int newLength = position + bytesRead;   
                byte[] bytes = new byte[newLength];     
                System.arraycopy(body, 0, bytes, 0, position);
                body = bytes;
            }
        }
    }

    public boolean complete() {
        return complete;
    }

    public static class RequestBody {
        private final byte[] body;
        private final Throwable exception;

        RequestBody(byte[] body, Throwable exception) {
            this.body = body;
            this.exception = exception;
        }

        public byte[] body() throws Throwable {
            if (exception != null) throw exception;
            return this.body;
        }
    }
}

