/**
 *
 * Original work Copyright (C) 2015 Red Hat, inc.
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
package work.ready.core.security.cors;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.util.*;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

import java.util.Arrays;
import java.util.Collection;

public class CorsUtil {
    private static final Log logger = LogFactory.getLog(CorsUtil.class);

    public static final HttpString ORIGIN = new HttpString("Origin");
    public static final HttpString ACCESS_CONTROL_REQUEST_METHOD = new HttpString("Access-Control-Request-Method");
    public static final HttpString ACCESS_CONTROL_REQUEST_HEADERS = new HttpString("Access-Control-Request-Headers");

    public static final HttpString ACCESS_CONTROL_ALLOW_ORIGIN = new HttpString("Access-Control-Allow-Origin");
    public static final HttpString ACCESS_CONTROL_ALLOW_CREDENTIALS = new HttpString("Access-Control-Allow-Credentials");
    public static final HttpString ACCESS_CONTROL_EXPOSE_HEADERS = new HttpString("Access-Control-Expose-Headers");
    public static final HttpString ACCESS_CONTROL_MAX_AGE = new HttpString("Access-Control-Max-Age");
    public static final HttpString ACCESS_CONTROL_ALLOW_METHODS = new HttpString("Access-Control-Allow-Methods");
    public static final HttpString ACCESS_CONTROL_ALLOW_HEADERS = new HttpString("Access-Control-Allow-Headers");

    public static boolean requestMatched(HeaderMap headers) {
        return headers.contains(ORIGIN)
                || headers.contains(ACCESS_CONTROL_REQUEST_HEADERS)
                || headers.contains(ACCESS_CONTROL_REQUEST_METHOD);
    }

    public static String matchOrigin(HttpServerExchange exchange, Collection<String> allowedOrigins) throws Exception {
        HeaderMap headers = exchange.getRequestHeaders();
        String[] origins = headers.get(Headers.ORIGIN).toArray();
        if(logger.isTraceEnabled()) logger.trace("origins from the request header = " + Arrays.toString(origins) + " allowedOrigins = " + allowedOrigins);
        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            if(allowedOrigins.contains("*")) return "*";
            for (String allowedOrigin : allowedOrigins) {
                for (String origin : origins) {
                    if (allowedOrigin.equalsIgnoreCase(sanitizeDefaultPort(origin))) {
                        return allowedOrigin;
                    }
                }
            }
        }
        String allowedOrigin = defaultOrigin(exchange);
        if(logger.isTraceEnabled()) logger.trace("allowedOrigin from the exchange = " + allowedOrigin);
        for (String origin : origins) {
            if (allowedOrigin.equalsIgnoreCase(sanitizeDefaultPort(origin))) {
                return allowedOrigin;
            }
        }
        logger.debug("Request rejected due to HOST/ORIGIN mis-match.");
        ResponseCodeHandler.HANDLE_403.handleRequest(exchange);
        return null;
    }

    public static String defaultOrigin(HttpServerExchange exchange) {
        String host = NetworkUtils.formatPossibleIpv6Address(exchange.getHostName());
        String protocol = exchange.getRequestScheme();
        int port = exchange.getHostPort();
        
        StringBuilder allowedOrigin = new StringBuilder(256);
        allowedOrigin.append(protocol).append("://").append(host);
        if (!isDefaultPort(port, protocol)) {
            allowedOrigin.append(':').append(port);
        }
        return allowedOrigin.toString();
    }

    private static boolean isDefaultPort(int port, String protocol) {
        return (("http".equals(protocol) && 80 == port) || ("https".equals(protocol) && 443 == port));
    }

    public static String sanitizeDefaultPort(String url) {
        int afterSchemeIndex = url.indexOf("://");
        if(afterSchemeIndex < 0) {
            return url;
        }
        String scheme = url.substring(0, afterSchemeIndex);
        int fromIndex = scheme.length() + 3;
        
        int ipv6StartIndex = url.indexOf('[', fromIndex);
        if (ipv6StartIndex > 0) {
            fromIndex = url.indexOf(']', ipv6StartIndex);
        }
        int portIndex = url.indexOf(':', fromIndex);
        if(portIndex >= 0) {
            int port = Integer.parseInt(url.substring(portIndex + 1));
            if(isDefaultPort(port, scheme)) {
                return url.substring(0, portIndex);
            }
        }
        return url;
    }

    public static boolean isCorsRequest(HttpServerExchange exchange) {
        return Methods.OPTIONS.equals(exchange.getRequestMethod()) && requestMatched(exchange.getRequestHeaders());
    }
}
