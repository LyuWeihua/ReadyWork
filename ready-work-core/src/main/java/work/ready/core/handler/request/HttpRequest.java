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

package work.ready.core.handler.request;

import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.protocol.http.HttpAttachments;
import io.undertow.util.*;
import work.ready.core.handler.cookie.Cookie;
import work.ready.core.exception.BadRequestException;
import work.ready.core.handler.ContentType;
import work.ready.core.handler.RequestMethod;
import work.ready.core.handler.cookie.CookieItem;
import work.ready.core.handler.session.HttpSession;
import work.ready.core.handler.session.SessionManager;
import work.ready.core.module.ApplicationContext;
import work.ready.core.server.Constant;
import work.ready.core.server.Ready;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;

import static io.undertow.util.Headers.AUTHORIZATION;
import static java.net.URLDecoder.decode;
import static work.ready.core.tools.StrUtil.format;

public final class HttpRequest implements Request {

    final Map<String, String[]> queryParameters = new HashMap<>();
    final Map<String, String[]> formParameters = new HashMap<>();
    final Map<String, UploadFile> files = new HashMap<>();
    private Map<String, Object> attributes = new HashMap<>();
    private boolean attributeEvent;

    private final ApplicationContext context;
    private final HttpServerExchange exchange;
    private final SessionManager sessionManager;
    private final boolean enableSession;
    public HttpSession session;

    RequestMethod method;
    String clientIP;
    String scheme;
    String hostName;
    int port;
    String requestURL;
    ContentType contentType;
    byte[] body;
    Map<String, String> cookies;

    public HttpRequest(HttpServerExchange exchange, ApplicationContext context) {
        this.exchange = exchange;
        this.context = context;
        this.attributeEvent = false;
        this.enableSession = Ready.getApplicationConfig(context.application.getName()).isEnableSession();
        this.sessionManager = context.webServer.getRequestHandler().getSessionManager();
    }

    @Override
    public HttpServerExchange getExchange(){
        return exchange;
    }

    @Override
    public SecurityContext getSecurityContext(){
        return this.exchange.getSecurityContext();
    }

    @Override
    public String getAuthHeader() {
        HeaderValues values = exchange.getRequestHeaders().get(AUTHORIZATION);
        return values == null ? null : values.peekFirst();
    }

    @Override
    public String getContextPath() { return null; }  

    @Override
    public String getRequestURL() {
        return requestURL;
    }

    @Override
    public StringBuffer getRequestUrl() {
        return new StringBuffer(this.exchange.getRequestURL());
    }

    @Override
    public String getScheme() {
        return scheme;
    }

    @Override
    public String getRequestScheme() {
        return this.exchange.getRequestScheme();
    }

    @Override
    public String getHostName() {
        return hostName;
    }

    @Override
    public String getServerName() {
        return this.exchange.getHostName();
    }

    @Override
    public int getHostPort(){
        return port;
    }

    @Override
    public int getServerPort() { return this.exchange.getHostPort(); }

    @Override
    public String getPath() {
        return exchange.getRequestPath();
    }

    @Override
    public String getClientIP() {
        return clientIP;
    }

    @Override
    public RequestMethod getMethod() {
        return method;
    }

    @Override
    public ContentType getContentType() {
        return contentType;
    }

    @Override
    public int getContentLength() {
        long length = getContentLengthLong();
        if(length > Integer.MAX_VALUE) {
            return -1;
        }
        return (int)length;
    }

    @Override
    public long getContentLengthLong() {
        final String contentLength = getHeader(Headers.CONTENT_LENGTH);
        if (contentLength == null || contentLength.isEmpty()) {
            return -1;
        }
        return Long.parseLong(contentLength);
    }

    public boolean isJsonRequest(){
        return contentType != null && "application/json".equals(contentType.getMediaType().toLowerCase());
    }

    @Override
    public HttpSession getSession() {
        return getSession(false);
    }

    public HttpSession getSession(boolean create) {
        if (!enableSession) {
            throw new Error("Session must be configured");
        }
        session = sessionManager.getSession(exchange);
        if(create && session == null){
            session = sessionManager.createSession(exchange);
        }
        return session;
    }

    @Override
    public String getCookie(Cookie cookie) {
        return getCookie(cookie.getName());
    }

    @Override
    public String getCookie(String name) {
        return cookies == null ? null : cookies.get(name);
    }

    @Override
    public Cookie[] getCookies() {
        var it = this.exchange.requestCookies().iterator();
        List<Cookie> cookies = new ArrayList<>();
        while (it.hasNext()) {
            var cookie = it.next();
            try {
                Cookie c = new CookieItem(cookie.getName(), cookie.getValue());
                if (cookie.getDomain() != null) {
                    c.setDomain(cookie.getDomain());
                }

                c.setHttpOnly(cookie.isHttpOnly());
                if (cookie.getMaxAge() != null) {
                    c.setMaxAge(cookie.getMaxAge());
                }

                if (cookie.getPath() != null) {
                    c.setPath(cookie.getPath());
                }

                c.setSecure(cookie.isSecure());
                c.setVersion(cookie.getVersion());
                cookies.add(c);
            } catch (IllegalArgumentException e) {
            }
        }
        return cookies.toArray(new Cookie[0]);
    }

    @Override
    public long getDateHeader(String name) {
        String header = this.exchange.getRequestHeaders().getFirst(name);
        if (header == null) {
            return -1L;
        } else {
            Date date = DateUtils.parseDate(header);
            if (date == null) {
                throw new Error("header " + name + " cannot be converted to Date.");
            } else {
                return date.getTime();
            }
        }
    }

    @Override
    public String getHeader(String name) {
        return exchange.getRequestHeaders().getFirst(name);
    }

    @Override
    public HeaderMap setHeader(String name, String value) {
        return exchange.getRequestHeaders().put(new HttpString(name), value);
    }

    @Override
    public String getHeader(HttpString name) {
        return exchange.getRequestHeaders().getFirst(name);
    }

    @Override
    public HeaderMap setHeader(HttpString name, String value) {
        return exchange.getRequestHeaders().put(name, value);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        List<String> headers = this.exchange.getRequestHeaders().get(name);
        return (headers == null ? new IteratorEnumeration<>(Collections.emptyIterator()) : new IteratorEnumeration<>(headers.iterator()));
    }

    @Override
    public IteratorEnumeration<String> getHeaderNames() {
        final Set<String> headers = new HashSet<>();
        for (final HttpString i : exchange.getRequestHeaders().getHeaderNames()) {
            headers.add(i.toString());
        }
        return new IteratorEnumeration<>(headers.iterator());
    }

    @Override
    public String getRequestURI() {
        if(exchange.isHostIncludedInRequestURI()) {
            
            String uri = exchange.getRequestURI();
            int slashes =0;
            for(int i = 0; i < uri.length(); ++i) {
                if(uri.charAt(i) == '/') {
                    if(++slashes == 3) {
                        return uri.substring(i);
                    }
                }
            }
            return "/";
        } else {
            return exchange.getRequestURI();
        }
    }

    @Override
    public String getPathParameter(String name) {
        return exchange.getPathParameters().get(name) == null ? null : exchange.getPathParameters().get(name).peekFirst();
    }

    @Override
    public Map<String, String> getPathParameters() {
        Map<String, String> pathParameters = new HashMap<>();
        for (Map.Entry<String, Deque<String>> entry : exchange.getPathParameters().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().peekFirst();
            try {
                String paramName = decode(key, Constant.DEFAULT_CHARSET);
                String paramValue = decode(value, Constant.DEFAULT_CHARSET);
                pathParameters.put(paramName, paramValue);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException(format("failed to parse path param, name=%s, value=%s", key, value), "INVALID_HTTP_REQUEST", e);
            }
        }
        return pathParameters;
    }

    @Override
    public String getQueryString() {
        return this.exchange.getQueryString().isEmpty() ? null : this.exchange.getQueryString();
    }

    @Override
    public Map<String, String[]> getQueryParameters() {
        return queryParameters;
    }

    @Override
    public Map<String, String[]> getFormParameters() {
        return formParameters;
    }

    @Override
    public String getParameter(String name) {
        String[] value = queryParameters.get(name);
        if(value == null) {
            value = formParameters.get(name);
        }
        return value == null ? null : value[0];
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> result = getQueryParameters();
        if(RequestMethod.hasBody(method)) {
            if(result.size() == 0){
                result = getFormParameters();
            } else {
                Map<String, String[]> formParams = getFormParameters();
                Iterator it = formParams.entrySet().iterator();
                while (it.hasNext()){
                    Map.Entry<String, String[]> entry = (Map.Entry)it.next();
                    if(result.containsKey(entry.getKey())){
                        String[] a1 = result.get(entry.getKey());
                        String[] a2 = entry.getValue();
                        String[] a3 = new String[a1.length + a2.length];
                        System.arraycopy(a1, 0, a3, 0, a1.length);
                        System.arraycopy(a2, 0, a3, a1.length, a2.length);
                        result.put(entry.getKey(), a3);
                    }else{
                        result.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
        return result;
    }

    public enum parameterMap{
        queryOnly, formOnly, formPrioritized, queryPrioritized
    }
    @Override
    public Map<String, String> getParameterMap(parameterMap style) {
        Map<String, String[]> query = getQueryParameters();
        Map<String, String> resultMap = new HashMap<>();
        if(parameterMap.queryOnly.equals(style) || !RequestMethod.hasBody(method)){
            query.forEach((key,val)-> resultMap.put(key, (val != null) ? val[0] : null));
            return resultMap;
        }
        if(RequestMethod.hasBody(method)) {
            if(parameterMap.formOnly.equals(style)){
                Map<String, String[]> form = getFormParameters();
                form.forEach((key,val)-> resultMap.put(key, (val != null) ? val[0] : null));
                return resultMap;
            }
            if(parameterMap.queryPrioritized.equals(style)){
                Map<String, String[]> form = getFormParameters();
                query.forEach((key,val)-> resultMap.put(key, (val != null) ? val[0] : null));
                form.forEach((key,val)-> resultMap.putIfAbsent(key, (val != null) ? val[0] : null));
                return resultMap;
            }
            if(parameterMap.formPrioritized.equals(style)){
                Map<String, String[]> form = getFormParameters();
                form.forEach((key,val)-> resultMap.put(key, (val != null) ? val[0] : null));
                query.forEach((key,val)-> resultMap.putIfAbsent(key, (val != null) ? val[0] : null));
                return resultMap;
            }
        }
        return resultMap;
    }

    @Override
    public String[] getParameterValues(String name){
        String[] queryParams = queryParameters.get(name) != null ? queryParameters.get(name) : new String[]{};
        String[] formParams = formParameters.get(name) != null ? formParameters.get(name) : new String[]{};
        String[] result = new String[queryParams.length + formParams.length];
        System.arraycopy(queryParams, 0, result, 0, queryParams.length);
        System.arraycopy(formParams, 0, result, queryParams.length, formParams.length);
        return result;
    }

    @Override
    public Enumeration<String> getParameterNames(){
        Set<String> parameterNames = new HashSet<>(queryParameters.keySet());
        if(RequestMethod.hasBody(method)) {
            parameterNames.addAll(formParameters.keySet());
        }
        return new IteratorEnumeration<>(parameterNames.iterator());
    }

    @Override
    public Object getAttribute(String name) {
        return this.attributes == null ? null : this.attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return this.attributes == null ? new IteratorEnumeration<>(Collections.emptyIterator()) : new IteratorEnumeration<>(this.attributes.keySet().iterator());
    }

    public boolean isAttributeEvent() {
        return attributeEvent;
    }

    public HttpRequest setAttributeEvent(boolean attributeEvent) {
        this.attributeEvent = attributeEvent;
        return this;
    }

    @Override
    public synchronized HttpRequest setAttribute(String name, Object object) {
        if (object == null) {
            this.removeAttribute(name);
        } else {
            if (this.attributes == null) {
                this.attributes = new HashMap<>();
            }

            Object existing = this.attributes.put(name, object);
            if(attributeEvent) {
                if (existing != null) {
                    Ready.post(new RequestAttributeEvent(this, RequestAttributeEvent.Action.Replaced, name, existing));
                } else {
                    Ready.post(new RequestAttributeEvent(this, RequestAttributeEvent.Action.Added, name, object));
                }
            }
        }
        return this;
    }

    @Override
    public synchronized HttpRequest removeAttribute(String name) {
        if (this.attributes != null) {
            Object exiting = this.attributes.remove(name);
            if(attributeEvent) {
                Ready.post(new RequestAttributeEvent(this, RequestAttributeEvent.Action.Removed, name, exiting));
            }
        }
        return this;
    }

    @Override
    public synchronized HttpRequest clearAttributes() {
        if (this.attributes != null) {
            this.attributes.clear();
        }
        if(attributeEvent) {
            Ready.post(new RequestAttributeEvent(this, RequestAttributeEvent.Action.Cleared, null, null));
        }
        return this;
    }

    @Override
    public Locale getLocale() {
        return getLocales().nextElement();
    }

    @Override
    public Enumeration<Locale> getLocales() {
        final List<String> acceptLanguage = exchange.getRequestHeaders().get(Headers.ACCEPT_LANGUAGE);
        List<Locale> ret = LocaleUtils.getLocalesFromHeader(acceptLanguage);
        if(ret.isEmpty()) {
            return new IteratorEnumeration<>(Collections.singletonList(Locale.getDefault()).iterator());
        }
        return new IteratorEnumeration<>(ret.iterator());
    }

    @Override
    public Map<String, UploadFile> getFiles() {
        return files;
    }

    @Override
    public byte[] getBody() {
        return body;
    }

    @Override
    public String getProtocol() {
        return this.exchange.getProtocol().toString();
    }

    @Override
    public String getRemoteAddr() {
        InetSocketAddress sourceAddress = this.exchange.getSourceAddress();
        if (sourceAddress == null) {
            return "";
        } else {
            InetAddress address = sourceAddress.getAddress();
            return address == null ? sourceAddress.getHostString() : address.getHostAddress();
        }
    }

    @Override
    public String getRemoteHost() {
        InetSocketAddress sourceAddress = this.exchange.getSourceAddress();
        return sourceAddress == null ? "" : sourceAddress.getHostString();
    }

    @Override
    public int getRemotePort() {
        return this.exchange.getSourceAddress().getPort();
    }

    @Override
    public boolean isSecure() {
        return this.exchange.isSecure();
    }

    @Override
    public String getLocalName() {
        return this.exchange.getDestinationAddress().getHostString();
    }

    @Override
    public String getLocalAddr() {
        InetSocketAddress destinationAddress = this.exchange.getDestinationAddress();
        if (destinationAddress == null) {
            return "";
        } else {
            InetAddress address = destinationAddress.getAddress();
            return address == null ? destinationAddress.getHostString() : address.getHostAddress();
        }
    }

    @Override
    public int getLocalPort() {
        return this.exchange.getDestinationAddress().getPort();
    }

    @Override
    public Map<String, String> getTrailerFields() {
        HeaderMap trailers = exchange.getAttachment(HttpAttachments.REQUEST_TRAILERS);
        if(trailers == null) {
            return Collections.emptyMap();
        }
        Map<String, String> ret = new HashMap<>();
        for(HeaderValues entry : trailers) {
            ret.put(entry.getHeaderName().toString().toLowerCase(Locale.ENGLISH), entry.getFirst());
        }
        return ret;
    }

    @Override
    public boolean isTrailerFieldsReady() {
        if(exchange.isRequestComplete()) {
            return true;
        }
        return !exchange.getConnection().isRequestTrailerFieldsSupported();
    }

    public static class IteratorEnumeration<E> implements Enumeration<E> {
        private Iterator<? extends E> iterator;

        public IteratorEnumeration() {
        }

        IteratorEnumeration(Iterator<? extends E> iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean hasMoreElements() {
            return this.iterator.hasNext();
        }

        @Override
        public E nextElement() {
            return this.iterator.next();
        }

        public Iterator<? extends E> getIterator() {
            return this.iterator;
        }

        public void setIterator(Iterator<? extends E> iterator) {
            this.iterator = iterator;
        }
    }
}
