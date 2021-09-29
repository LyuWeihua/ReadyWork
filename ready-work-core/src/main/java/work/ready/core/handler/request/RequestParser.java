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

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import work.ready.core.exception.BadRequestException;
import work.ready.core.exception.MethodNotAllowedException;
import work.ready.core.handler.ContentType;
import work.ready.core.handler.RequestMethod;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.tools.CollectionUtil;
import work.ready.core.tools.StrUtil;

import java.io.IOException;
import java.util.*;

import static work.ready.core.tools.HttpUtil.decodeURIComponent;
import static work.ready.core.tools.StrUtil.format;
import static java.net.URLDecoder.decode;
import static java.nio.charset.StandardCharsets.UTF_8;

public final class RequestParser {
    private static final Log logger = LogFactory.getLog(RequestParser.class);
    private static final int MAX_URL_LENGTH = 1000;
    public static final ClientIPParser clientIPParser = new ClientIPParser();

    public void parse(HttpRequest request, HttpServerExchange exchange) throws Throwable {
        HeaderMap headers = exchange.getRequestHeaders();

        request.scheme = scheme(exchange.getRequestScheme(), headers.getFirst(Headers.X_FORWARDED_PROTO));
        request.hostName = hostName(exchange.getHostName(), headers.getFirst(Headers.X_FORWARDED_HOST));
        int requestPort = requestPort(headers.getFirst(Headers.HOST), request.scheme, exchange);
        request.port = port(requestPort, headers.getFirst(Headers.X_FORWARDED_PORT));

        String method = exchange.getRequestMethod().toString();

        request.requestURL = requestURL(request, exchange);

        logHeaders(headers);

        parseClientIP(request, exchange, headers.getFirst(Headers.X_FORWARDED_FOR)); 

        parseCookies(request, exchange);

        String userAgent = headers.getFirst(Headers.USER_AGENT);

        request.method = httpMethod(method);    

        parseQueryParams(request, exchange.getQueryParameters());

        if (RequestMethod.hasBody(request.method)) {
            String contentType = headers.getFirst(Headers.CONTENT_TYPE);
            request.contentType = contentType == null ? null : ContentType.parse(contentType);
            parseBody(request, exchange);
        }
    }

    String hostName(String hostName, String xForwardedHost) {
        if (StrUtil.isBlank(xForwardedHost)) return hostName;
        return xForwardedHost;
    }

    void parseCookies(HttpRequest request, HttpServerExchange exchange) {
        HeaderValues cookieHeaders = exchange.getRequestHeaders().get(Headers.COOKIE);
        if (cookieHeaders != null) {
            try {
                request.cookies = decodeCookies(exchange.requestCookies());
            } catch (IllegalArgumentException e) {
                logger.debug("[request:header] %s=%s", Headers.COOKIE, cookieHeaders);

                throw new BadRequestException("invalid cookie", "INVALID_COOKIE", e);
            }
        }
    }

    private void parseClientIP(HttpRequest request, HttpServerExchange exchange, String xForwardedFor) {
        String remoteAddress = exchange.getSourceAddress().getAddress().getHostAddress();
        logger.debug("[request] remoteAddress=%s", remoteAddress);
        request.clientIP = clientIPParser.parse(remoteAddress, xForwardedFor);
    }

    String scheme(String requestScheme, String xForwardedProto) {       
        logger.debug("[request] requestScheme=%s", requestScheme);
        return xForwardedProto != null ? xForwardedProto : requestScheme;
    }

    private void logHeaders(HeaderMap headers) {
        for (HeaderValues header : headers) {
            HttpString name = header.getHeaderName();
            if (!Headers.COOKIE.equals(name)) {
                logger.debug("[request:header] %s=%s", name, header);
            }
        }
    }

    Map<String, String> decodeCookies(Iterable<Cookie> cookies) {
        Map<String, String> cookieValues = new HashMap<>(16);
        var it = cookies.iterator();
        while (it.hasNext()) {
            var cookie = it.next();
            try {
                String cookieName = decodeURIComponent(cookie.getName());
                String cookieValue = decodeURIComponent(cookie.getValue());
                logger.debug("[request:cookie] %s=%s", cookieName, cookieValue);
                cookieValues.put(cookieName, cookieValue);
            } catch (IllegalArgumentException e) {
                
                logger.warn(e,"ignore invalid encoded cookie, name=%s, value=%s", cookie.getName(), cookie.getValue());
            }
        }
        return cookieValues;
    }

    void parseQueryParams(HttpRequest request, Map<String, Deque<String>> params) {
        for (Map.Entry<String, Deque<String>> entry : params.entrySet()) {
            String key = entry.getKey();

            ArrayList<String> valueList = new ArrayList<>(entry.getValue());
            for(int i = 0; i < valueList.size(); i++){
                valueList.set(i, decode(valueList.get(i), UTF_8));
            }

            try {
                String paramName = decode(key, UTF_8);
                
                logger.debug("[request:query] %s=%s", paramName, valueList);
                request.queryParameters.put(paramName, valueList.toArray(new String[0]));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException(format("failed to parse query param, name=%s, value=%s", key, valueList), "INVALID_HTTP_REQUEST", e);
            }
        }
    }

    RequestMethod httpMethod(String method) {
        try {
            return RequestMethod.valueOf(method);
        } catch (IllegalArgumentException e) {
            throw new MethodNotAllowedException("method not allowed, method=" + method, e);
        }
    }

    void parseBody(HttpRequest request, HttpServerExchange exchange) throws Throwable {
        var body = exchange.getAttachment(RequestBodyReader.REQUEST_BODY);
        if (body != null) {
            request.body = body.body();
            logger.debug("[request] body=%s", request.body);
        } else {
            parseForm(request, exchange);
        }
    }

    private void parseForm(HttpRequest request, HttpServerExchange exchange) throws IOException {
        FormData formData = exchange.getAttachment(FormDataParser.FORM_DATA);
        if (formData == null) return;

        for (String name : formData) {

            FormData.FormValue value = formData.getFirst(name);
            Deque<FormData.FormValue> valueDeque = formData.get(name);

            if (value.isFileItem()) {
                String fileName = value.getFileName();
                if (!StrUtil.isBlank(fileName)) {    
                    FormData.FileItem item = value.getFileItem();
                    logger.debug("[request:file] %s=%s, size=%s", name, fileName, item.getFileSize());
                    request.files.put(name, new UploadFile(item, fileName, value.getHeaders().getFirst(Headers.CONTENT_TYPE)));
                }
            } else {
                Iterator it = valueDeque.iterator();
                ArrayList<String> valueList = new ArrayList();
                while (it.hasNext()){
                    value = (FormData.FormValue)it.next();
                    valueList.add(value.getValue());
                }
                logger.debug("[request:form] %s=%s", name, valueList);
                request.formParameters.put(name, valueList.toArray(new String[valueList.size()]));
            }
        }
    }

    int port(int requestPort, String xForwardedPort) {
        if (xForwardedPort != null) {
            int index = xForwardedPort.indexOf(',');
            if (index > 0)
                return Integer.parseInt(xForwardedPort.substring(0, index));
            else
                return Integer.parseInt(xForwardedPort);
        }
        return requestPort;
    }

    int requestPort(String host, String scheme, HttpServerExchange exchange) {    
        if (host != null) {     
            int colonIndex;
            if (host.startsWith("[")) { 
                colonIndex = host.indexOf(':', host.indexOf(']'));
            } else {
                colonIndex = host.indexOf(':');
            }
            if (colonIndex > 0 && colonIndex + 1 < host.length()) { 
                return Integer.parseInt(host.substring(colonIndex + 1));
            }
            
            if ("https".equals(scheme)) return 443;
            if ("http".equals(scheme)) return 80;
        }
        return exchange.getDestinationAddress().getPort();
    }

    String requestURL(HttpRequest request, HttpServerExchange exchange) {
        var builder = new StringBuilder(128);

        if (exchange.isHostIncludedInRequestURI()) {    
            builder.append(exchange.getRequestURI());
        } else {
            String scheme = request.scheme;
            int port = request.port;

            builder.append(scheme)
                   .append("://")
                   .append(exchange.getHostName());

            if (!(port == 80 && "http".equals(scheme)) && !(port == 443 && "https".equals(scheme))) {
                builder.append(':').append(port);
            }

            builder.append(exchange.getRequestURI());
        }

        String queryString = exchange.getQueryString();
        if (!queryString.isEmpty()) builder.append('?').append(queryString);

        String requestURL = builder.toString();
        if (requestURL.length() > MAX_URL_LENGTH) throw new BadRequestException(format("requestURL is too long, requestURL=%s...(truncated)", requestURL.substring(0, 50)), "INVALID_HTTP_REQUEST");
        return requestURL;
    }
}
