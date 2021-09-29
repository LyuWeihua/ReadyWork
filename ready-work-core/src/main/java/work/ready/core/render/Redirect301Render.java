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

public class Redirect301Render extends RedirectRender {

	public Redirect301Render(String url) {
		super(url);
	}

	public Redirect301Render(String url, boolean withQueryString) {
		super(url, withQueryString);
	}

	public void render() {

		String finalUrl = buildFinalUrl();
		response.addHeader(Headers.LOCATION, finalUrl);
		response.setStatus(StatusCodes.MOVED_PERMANENTLY);
		response.responseDone();
	}
}

