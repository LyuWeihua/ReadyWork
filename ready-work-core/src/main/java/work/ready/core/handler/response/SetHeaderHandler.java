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

import io.undertow.attribute.ExchangeAttribute;
import io.undertow.attribute.ExchangeAttributes;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.util.HttpString;
import work.ready.core.tools.validator.Assert;

import java.util.HashMap;
import java.util.Map;

public class SetHeaderHandler implements HttpHandler {

    private final Map<HttpString, ExchangeAttribute> headerMap = new HashMap<>();
    private final HttpHandler next;

    public SetHeaderHandler(final String header, final String value) {
        this.next = ResponseCodeHandler.HANDLE_404;
        addHeader(header, value);
    }

    public SetHeaderHandler(final HttpHandler next, final String header, final String value) {
        this.next = Assert.notNull(next, "next handler cannot be null");
        addHeader(header, value);
    }

    public SetHeaderHandler(final HttpHandler next, Map<String, String> headers) {
        Assert.notEmpty(headers, "headers cannot be null or empty");
        this.next = Assert.notNull(next, "next handler cannot be null");
        headers.forEach(this::addHeader);
    }

    public void addHeader(final String header, final String value) {
        Assert.notEmpty(value, "value cannot be null or empty");
        Assert.notEmpty(header, "header cannot be null or empty");
        headerMap.put(new HttpString(header), ExchangeAttributes.constant(value));
    }

    public Map<HttpString, ExchangeAttribute> getHeaderMap() {
        return headerMap;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        headerMap.forEach((header, value)-> exchange.getResponseHeaders().put(header, value.readAttribute(exchange)));
        next.handleRequest(exchange);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        headerMap.forEach((header, value)-> sb.append("set( header='").append(header.toString()).append("', value='").append(value.toString()).append("' )\n"));
        return sb.toString();
    }

}
