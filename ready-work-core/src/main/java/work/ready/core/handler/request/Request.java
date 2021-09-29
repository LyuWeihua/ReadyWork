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
import io.undertow.util.HeaderMap;
import io.undertow.util.HttpString;
import work.ready.core.handler.ContentType;
import work.ready.core.handler.RequestMethod;
import work.ready.core.handler.cookie.Cookie;
import work.ready.core.handler.session.HttpSession;

import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

public interface Request {

    HttpServerExchange getExchange();

    SecurityContext getSecurityContext();

    String getAuthHeader();

    String getContextPath();  

    String getRequestURL();  

    StringBuffer getRequestUrl();  

    String getScheme();

    String getRequestScheme();  

    String getHostName();

    int getHostPort();

    String getServerName();  

    int getServerPort(); 

    String getPath();

    RequestMethod getMethod();

    String getClientIP();

    ContentType getContentType();

    int getContentLength();

    long getContentLengthLong();

    String getHeader(String name);

    HeaderMap setHeader(String name, String value);

    String getHeader(HttpString name);

    HeaderMap setHeader(HttpString name, String value);

    long getDateHeader(String name);

    Enumeration<String> getHeaders(String name);

    HttpRequest.IteratorEnumeration getHeaderNames();

    String getRequestURI();

    String getPathParameter(String name);

    Map<String, String> getPathParameters();

    String getParameter(String name);

    Map<String, String[]> getQueryParameters();

    Map<String, String[]> getFormParameters();

    Enumeration<String> getParameterNames();

    Map<String, String[]> getParameterMap();

    Map<String, String> getParameterMap(HttpRequest.parameterMap style);

    String[] getParameterValues(String name);

    String getQueryString();

    Object getAttribute(String name);

    Enumeration<String> getAttributeNames();

    HttpRequest setAttribute(String name, Object object);

    HttpRequest removeAttribute(String name);

    HttpRequest clearAttributes();

    Locale getLocale();

    Enumeration<Locale> getLocales();

    String getProtocol();

    String getRemoteAddr();

    String getRemoteHost();

    int getRemotePort();

    boolean isSecure();

    String getLocalName();

    String getLocalAddr();

    int getLocalPort();

    Map<String, UploadFile> getFiles();

    byte[] getBody();

    String getCookie(String name);

    String getCookie(Cookie cookie);

    Cookie[] getCookies();

    HttpSession getSession();

    Map<String, String> getTrailerFields();

    boolean isTrailerFieldsReady();
}
