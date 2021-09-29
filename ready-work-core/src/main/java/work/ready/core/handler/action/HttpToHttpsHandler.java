/**
 * Copyright (c) 2011-2019, James Zhan 詹波 (jfinal@126.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package work.ready.core.handler.action;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import work.ready.core.handler.BaseHandler;

public class HttpToHttpsHandler extends BaseHandler {

	protected HttpHandler next;

	protected String httpsPrefix;
	protected int statusCode = StatusCodes.FOUND;

	public HttpToHttpsHandler(HttpHandler next) {
		setOrder(-1);
		this.next = next;
	}

	public void handleRequest(HttpServerExchange exchange) throws Exception {
		String scheme = exchange.getRequestScheme();
		if ("http".equals(scheme)) {
			String httpsUrl = buildRedirectHttpsUrl(exchange);

			exchange.setStatusCode(statusCode);
			exchange.getResponseHeaders().put(Headers.LOCATION, httpsUrl);
			exchange.getResponseHeaders().put(Headers.CONNECTION, "close");

			exchange.endExchange();
		} else {
			next.handleRequest(exchange);
		}
	}

	protected String buildRedirectHttpsUrl(HttpServerExchange exchange) {
		if (httpsPrefix == null) {
			buildUrlPrefix(exchange);
		}

		String uri = exchange.getRequestURI();			
		String queryString = exchange.getQueryString();
		if (queryString != null && queryString.length() > 0) {
			StringBuilder ret = new StringBuilder(httpsPrefix.length() + uri.length() + 1 + queryString.length());
			ret.append(httpsPrefix).append(uri).append('?').append(queryString);
			return ret.toString();
		} else {
			StringBuilder ret = new StringBuilder(httpsPrefix.length() + uri.length());
			ret.append(httpsPrefix).append(uri);
			return ret.toString();
		}
	}

	protected void buildUrlPrefix(HttpServerExchange exchange) {
		String ret = "https://" + exchange.getHostName();

		if (!applicationConfig.getServer().getHttpsPort().equals(443)) {
			ret = ret + ":" + applicationConfig.getServer().getHttpsPort();
		}

		this.httpsPrefix = ret;

	}
}

