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
import io.undertow.server.protocol.http.HttpAttachments;
import io.undertow.util.*;
import work.ready.core.event.Event;
import work.ready.core.event.GeneralEvent;
import work.ready.core.handler.ContentType;
import work.ready.core.handler.cookie.Cookie;
import work.ready.core.handler.cookie.CookieItem;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;
import work.ready.core.tools.HttpUtil;
import work.ready.core.tools.StrUtil;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;

import static work.ready.core.tools.StrUtil.format;

public final class HttpResponse implements Response {
    private static final Log logger = LogFactory.getLog(HttpResponse.class);

    private final HttpServerExchange exchange;
    private ContentType contentType;
    private long contentLength;
    private boolean insideInclude;
    private Locale locale;
    private boolean responseDone;
    private boolean responseDoneEvent;
    private boolean ignoredFlushPerformed;
    private boolean treatAsCommitted;
    private boolean charsetSet;
    private Charset charset;
    private Supplier<Map<String, String>> trailerSupplier;

    public HttpResponse(HttpServerExchange exchange){
        this.contentLength = -1L;
        this.insideInclude = false;
        this.responseDone = false;
        this.responseDoneEvent = false;
        this.ignoredFlushPerformed = false;
        this.treatAsCommitted = false;
        this.charsetSet = false;
        this.exchange = exchange;
    }

    @Override
    public HttpServerExchange getExchange(){
        return exchange;
    }

    @Override
    public void send(String data){
        exchange.getResponseSender().send(data);
    }

    @Override
    public void send(String data, IoCallback ioCallback){
        exchange.getResponseSender().send(data, ioCallback);
    }

    @Override
    public void send(String data, Charset charset){
        exchange.getResponseSender().send(data, charset);
    }

    @Override
    public void send(String data, Charset charset, IoCallback ioCallback){
        exchange.getResponseSender().send(data, charset, ioCallback);
    }

    @Override
    public void send(ByteBuffer data){
        exchange.getResponseSender().send(data);
    }

    @Override
    public void send(ByteBuffer data, IoCallback ioCallback){
        exchange.getResponseSender().send(data, ioCallback);
    }

    @Override
    public void send(ByteBuffer[] data){
        exchange.getResponseSender().send(data);
    }

    @Override
    public void send(ByteBuffer[] data, IoCallback ioCallback){
        exchange.getResponseSender().send(data, ioCallback);
    }

    @Override
    public void transferFrom(FileChannel fileChannel, IoCallback ioCallback){
        exchange.getResponseSender().transferFrom(fileChannel, ioCallback);
    }

    @Override
    public OutputStream getOutputStream(){
        return exchange.getOutputStream();
    }

    @Override
    public BlockingHttpExchange startBlocking(){
        return exchange.startBlocking();
    }

    @Override
    public BlockingHttpExchange startBlocking(BlockingHttpExchange blockingExchange){
        return exchange.startBlocking(blockingExchange);
    }

    @Override
    public CookieItem newCookie(String name, String value){
        CookieItem cookie = new CookieItem(name);
        if (value == null) {
            cookie.setMaxAge(0);
            cookie.setValue("");
        } else {
            cookie.setValue(HttpUtil.uriComponent(value));     
        }
        return cookie;
    }

    @Override
    public CookieItem newCookie(Cookie c, String value){
        CookieItem cookie = new CookieItem(c.getName());
        if (value == null) {
            cookie.setMaxAge(0);
            cookie.setValue("");
        } else {
            if (c.getMaxAge() != null) cookie.setMaxAge(c.getMaxAge());
            cookie.setValue(HttpUtil.uriComponent(value));     
        }
        cookie.setDomain(c.getDomain());
        cookie.setPath(c.getPath());
        cookie.setSecure(c.isSecure());
        cookie.setHttpOnly(c.isHttpOnly());
        
        if (c.isSameSite()) cookie.setSameSiteMode("lax");
        return cookie;
    }

    @Override
    public HttpResponse addCookie(Cookie cookie) {
        if (!this.insideInclude) {
            this.exchange.setResponseCookie(cookie);
            logger.debug("[response:cookie] name=%s, value=%s, domain=%s, path=%s, secure=%s, httpOnly=%s, maxAge=%s"
                    , cookie.getName(), cookie.getValue(), cookie.getDomain(), cookie.getPath(), cookie.isSecure(), cookie.isHttpOnly(), cookie.getMaxAge());
        }
        return this;
    }

    @Override
    public void addResponseCommitListener(ResponseCommitListener listener) {
        this.exchange.addResponseCommitListener(listener);
    }

    @Override
    public boolean containsHeader(String name) {
        return this.exchange.getResponseHeaders().contains(name);
    }

    public void redirect(String location, int redirectStatus) {
        if (this.responseStarted()) {
            throw new RuntimeException("Response already commited");
        } else {
            if (redirectStatus != StatusCodes.SEE_OTHER
                    && redirectStatus != StatusCodes.FOUND
                    && redirectStatus != StatusCodes.MOVED_PERMANENTLY
                    && redirectStatus != StatusCodes.PERMANENT_REDIRECT
                    && redirectStatus != StatusCodes.TEMPORARY_REDIRECT)
                throw new Error(format("invalid redirect status, status=%s", redirectStatus));
            this.setStatus(redirectStatus);
            if (URLUtils.isAbsoluteUrl(location)) {
                this.exchange.getResponseHeaders().put(Headers.LOCATION, location);
            } else {
                String realPath;
                String current;
                if (location.startsWith("/")) {
                    realPath = location;
                } else {
                    current = this.exchange.getRelativePath();
                    int lastSlash = current.lastIndexOf("/");
                    if (lastSlash != -1) {
                        current = current.substring(0, lastSlash + 1);
                    }

                    realPath = CanonicalPathUtils.canonicalize(current + location);
                }

                current = this.exchange.getRequestScheme() + "://" + this.exchange.getHostAndPort() + realPath;
                this.exchange.getResponseHeaders().put(Headers.LOCATION, current);
            }

            this.responseDone();
        }
    }

    @Override
    public HttpResponse setDateHeader(String name, long date) {
        this.setHeader(name, DateUtils.toDateString(new Date(date)));
        return this;
    }

    @Override
    public HttpResponse addDateHeader(String name, long date) {
        this.addHeader(name, DateUtils.toDateString(new Date(date)));
        return this;
    }

    @Override
    public HttpResponse setHeader(String name, String value) {
        if (name == null) {
            throw new RuntimeException("Header name was null");
        } else {
            this.setHeader(HttpString.tryFromString(name), value);
        }
        return this;
    }

    @Override
    public HttpResponse setHeader(HttpString name, String value) {
        if (name == null) {
            throw new RuntimeException("Header name was null");
        } else if (!this.insideInclude && !this.ignoredFlushPerformed) {
            if (name.equals(Headers.CONTENT_TYPE)) {
                if(!this.exchange.getResponseHeaders().contains(Headers.CONTENT_TYPE))
                    this.setContentType(value);
            } else {
                this.exchange.getResponseHeaders().put(name, value);
            }
        }
        return this;
    }

    @Override
    public HttpResponse addHeader(String name, String value) {
        if (name == null) {
            throw new RuntimeException("Header name was null");
        } else {
            this.addHeader(HttpString.tryFromString(name), value);
        }
        return this;
    }

    @Override
    public HttpResponse addHeader(HttpString name, String value) {
        if (name == null) {
            throw new RuntimeException("Header name was null");
        } else if (!this.insideInclude && !this.ignoredFlushPerformed && !this.treatAsCommitted) {
            if (name.equals(Headers.CONTENT_TYPE)) {
                if(!this.exchange.getResponseHeaders().contains(Headers.CONTENT_TYPE))
                    this.setContentType(value);
            } else {
                this.exchange.getResponseHeaders().add(name, value);
            }
        }
        return this;
    }

    @Override
    public HttpResponse setIntHeader(String name, int value) {
        this.setHeader(name, Integer.toString(value));
        return this;
    }

    @Override
    public HttpResponse addIntHeader(String name, int value) {
        this.addHeader(name, Integer.toString(value));
        return this;
    }

    @Override
    public HttpResponse setStatus(int sc) {
        if (!this.insideInclude && !this.treatAsCommitted) {
            if (!this.responseStarted()) {
                this.exchange.setStatusCode(sc);
            }
        }
        return this;
    }

    @Override
    public HttpResponse setStatus(int sc, String sm) {
        this.setStatus(sc);
        if (!this.insideInclude) {
            this.exchange.setReasonPhrase(sm);
        }
        return this;
    }

    @Override
    public int getStatus() {
        return this.exchange.getStatusCode();
    }

    @Override
    public String getHeader(String name) {
        return this.exchange.getResponseHeaders().getFirst(name);
    }

    @Override
    public Collection<String> getHeaders(String name) {
        HeaderValues headers = this.exchange.getResponseHeaders().get(name);
        return headers == null ? Collections.emptySet() : new ArrayList<>(headers);
    }

    @Override
    public Collection<String> getHeaderNames() {
        final Set<String> headers = new HashSet<>();
        for (final HttpString i : exchange.getResponseHeaders().getHeaderNames()) {
            headers.add(i.toString());
        }
        return headers;
    }

    @Override
    public Charset getCharacterEncoding() {
        if (this.charset != null) {
            return this.charset;
        } else {
            return StandardCharsets.UTF_8;
        }
    }

    @Override
    public String getContentType() {
        String contentType = null;
        if (this.contentType != null) {
            contentType = this.charsetSet ? this.contentType.getMediaType() + ";charset=" + this.getCharacterEncoding() : this.contentType.toString();
        }
        return contentType;
    }

    @Override
    public HttpResponse setCharacterEncoding(Charset charset) {
        if (!this.insideInclude && !this.responseStarted() && !this.isCommitted()) {
            this.charsetSet = charset != null;
            this.charset = charset;
            if (this.contentType != null) {
                this.exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, this.getContentType());
            }
        }
        return this;
    }

    @Override
    public HttpResponse setContentLength(int len) {
        this.setContentLengthLong(len);
        return this;
    }

    @Override
    public HttpResponse setContentLengthLong(long len) {
        if (!this.insideInclude && !this.responseStarted()) {
            if (len >= 0L) {
                this.exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, Long.toString(len));
            } else {
                this.exchange.getResponseHeaders().remove(Headers.CONTENT_LENGTH);
            }
            this.contentLength = len;
        }
        return this;
    }

    boolean isIgnoredFlushPerformed() {
        return this.ignoredFlushPerformed;
    }

    HttpResponse setIgnoredFlushPerformed(boolean ignoredFlushPerformed) {
        this.ignoredFlushPerformed = ignoredFlushPerformed;
        return this;
    }

    private boolean responseStarted() {
        return this.exchange.isResponseStarted() || this.ignoredFlushPerformed || this.treatAsCommitted;
    }

    @Override
    public HttpResponse setContentType(ContentType contentType) {
        if (contentType != null && !this.insideInclude && !this.responseStarted()) {
            this.contentType = contentType;
            boolean useCharset = false;
            if (contentType.getCharset() != null && !this.isCommitted()) {
                this.charset = contentType.getCharset();
                this.charsetSet = true;
                useCharset = true;
            }

            if (!useCharset && this.charsetSet) {
                if (contentType.getCharset() == null) {
                    this.exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, contentType.toString() + "; charset=" + this.charset);
                } else {
                    this.exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, contentType.getMediaType() + "; charset=" + this.charset);
                }
            } else {
                this.exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, contentType.toString());
            }
        }
        return this;
    }

    @Override
    public HttpResponse setContentType(String type) {
        if(StrUtil.notBlank(type)) {
            ContentType ct = ContentType.parse(type);
            setContentType(ct);
        }
        return this;
    }

    @Override
    public boolean isCommitted() {
        return this.responseStarted();
    }

    @Override
    public HttpResponse reset() {
        exchange.getResponseHeaders().clear();
        exchange.setStatusCode(StatusCodes.OK);
        treatAsCommitted = false;
        return this;
    }

    @Override
    public HttpResponse setLocale(Locale loc) {
        if (!this.insideInclude && !this.responseStarted()) {
            this.locale = loc;
            this.exchange.getResponseHeaders().put(Headers.CONTENT_LANGUAGE, loc.getLanguage() + "-" + loc.getCountry());
        }
        return this;
    }

    @Override
    public Locale getLocale() {
        return this.locale != null ? this.locale : Locale.getDefault();
    }

    public boolean isResponseDoneEvent() {
        return responseDoneEvent;
    }

    public HttpResponse setResponseDoneEvent(boolean responseDoneEvent) {
        this.responseDoneEvent = responseDoneEvent;
        return this;
    }

    public void responseDone() {
        if (!this.responseDone && !this.treatAsCommitted) {
            this.responseDone = true;

            try {
                this.exchange.endExchange();
            } finally {
                if(responseDoneEvent)
                Ready.post(new GeneralEvent(Event.WEB_HTTP_RESPONSE_DONE, this, this.exchange));  
            }
        }
    }

    public boolean isInsideInclude() {
        return this.insideInclude;
    }

    public HttpResponse setInsideInclude(boolean insideInclude) {
        this.insideInclude = insideInclude;
        return this;
    }

    private String toAbsolute(String location) {
        if (location == null) {
            return location;
        }

        boolean leadingSlash = location.startsWith("/");

        if (leadingSlash || !hasScheme(location)) {
            return RedirectBuilder.redirect(exchange, location, false);
        } else {
            return location;
        }
    }

    private boolean hasScheme(String uri) {
        int len = uri.length();
        for (int i = 0; i < len; i++) {
            char c = uri.charAt(i);
            if (c == ':') {
                return i > 0;
            } else if (!Character.isLetterOrDigit(c) &&
                    (c != '+' && c != '-' && c != '.')) {
                return false;
            }
        }
        return false;
    }

    @Override
    public long getContentLength() {
        return this.contentLength;
    }

    private static String escapeHtml(String msg) {
        return msg.replace("<", "&lt;").replace(">", "&gt;").replace("&", "&amp;");
    }

    public boolean isTreatAsCommitted() {
        return this.treatAsCommitted;
    }

    @Override
    public HttpResponse setTrailerFields(Supplier<Map<String, String>> supplier) {
        if (this.exchange.isResponseStarted()) {
            throw new RuntimeException("Response already commited");
        } else if (this.exchange.getProtocol() == Protocols.HTTP_1_0) {
            throw new RuntimeException("Trailers not supported for this request due to HTTP/1.0 request");
        } else if (this.exchange.getProtocol() == Protocols.HTTP_1_1 && this.exchange.getResponseHeaders().contains(Headers.CONTENT_LENGTH)) {
            throw new RuntimeException("Trailers not supported for this request due to not chunked");
        }
        this.trailerSupplier = supplier;
        this.exchange.putAttachment(HttpAttachments.RESPONSE_TRAILER_SUPPLIER, () -> {
            HeaderMap trailers = new HeaderMap();
            Map<String, String> map = supplier.get();
            for(Map.Entry<String, String> e : map.entrySet()) {
                trailers.put(new HttpString(e.getKey()), e.getValue());
            }
            return trailers;
        });
        return this;
    }

    @Override
    public Supplier<Map<String, String>> getTrailerFields() {
        return this.trailerSupplier;
    }

}
