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

package work.ready.core.handler.response;

import io.undertow.io.IoCallback;
import io.undertow.server.BlockingHttpExchange;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.ResponseCommitListener;
import io.undertow.util.HttpString;
import work.ready.core.handler.ContentType;
import work.ready.core.handler.cookie.Cookie;
import work.ready.core.handler.cookie.CookieItem;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

public interface Response {

    HttpServerExchange getExchange();

    void send(String data);

    void send(String data, IoCallback ioCallback);

    void send(String data, Charset charset);

    void send(String data, Charset charset, IoCallback ioCallback);

    void send(ByteBuffer data);

    void send(ByteBuffer data, IoCallback ioCallback);

    void send(ByteBuffer[] data);

    void send(ByteBuffer[] data, IoCallback ioCallback);

    void transferFrom(FileChannel fileChannel, IoCallback ioCallback);

    OutputStream getOutputStream();

    BlockingHttpExchange startBlocking();

    BlockingHttpExchange startBlocking(BlockingHttpExchange blockingExchange);

    CookieItem newCookie(String name, String value);

    CookieItem newCookie(Cookie c, String value);

    Response addCookie(Cookie cookie);

    void addResponseCommitListener(ResponseCommitListener listener);

    int getStatus();

    Response setStatus(int status);

    Response setStatus(int sc, String sm);

    boolean containsHeader(String name);

    String getHeader(String name);

    Collection<String> getHeaders(String name);

    Collection<String> getHeaderNames();

    Response setHeader(String name, String value);

    Response setHeader(HttpString name, String value);

    Response addHeader(String name, String value);

    Response addHeader(HttpString name, String value);

    Response setIntHeader(String name, int value);

    Response addIntHeader(String name, int value);

    Response setDateHeader(String name, long date);

    Response addDateHeader(String name, long date);

    Charset getCharacterEncoding();

    Response setCharacterEncoding(Charset charset);

    String getContentType();

    Response setContentType(ContentType contentType);

    Response setContentType(String type);

    Response setContentLength(int len);

    Response setContentLengthLong(long len);

    long getContentLength();

    boolean isCommitted();

    Response reset();

    Response setLocale(Locale loc);

    Locale getLocale();

    Response setTrailerFields(Supplier<Map<String, String>> supplier);

    Supplier<Map<String, String>> getTrailerFields();

}
