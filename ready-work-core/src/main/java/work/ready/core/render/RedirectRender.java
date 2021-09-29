/**
 *
 * Original work Copyright (c) 2011-2019, James Zhan 詹波 (jfinal@126.com).
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
package work.ready.core.render;

import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

public class RedirectRender extends Render {

	protected String url;
	protected boolean withQueryString;

	public RedirectRender(String url) {
		this.url = url;
		this.withQueryString = false;
	}

	public RedirectRender(String url, boolean withQueryString) {
		this.url = url;
		this.withQueryString =  withQueryString;
	}

	public String buildFinalUrl() {
		String result;

		String contextPath = request.getContextPath();
		if (contextPath != null && (url.indexOf("://") == -1 || url.indexOf("://") > 5)) {
			result = contextPath + url;
		} else {
			result = url;
		}

		if (withQueryString) {
			String queryString = request.getQueryString();
			if (queryString != null) {
				if (result.indexOf('?') == -1) {
					result = result + "?" + queryString;
				} else {
					result = result + "&" + queryString;
				}
			}
		}

		if (!result.startsWith("http")) {	
			if ("https".equals(request.getScheme())) {
				String serverName = request.getServerName();
				int port = request.getServerPort();
				if (port != 443) {
					serverName = serverName + ":" + port;
				}

				if (result.charAt(0) != '/') {
					result = "https://" + serverName + "/" + result;
				} else {
					result = "https://" + serverName + result;
				}
			}
		}

		return result;
	}

	public void render() {
		String finalUrl = buildFinalUrl();

		try {

			response.setHeader(Headers.LOCATION, finalUrl);
			response.setStatus(StatusCodes.FOUND);
			response.responseDone();
		} catch (Exception e) {
			throw new RenderException(e);
		}
	}
}

