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

import io.undertow.util.StatusCodes;
import work.ready.core.handler.ContentType;
import work.ready.core.handler.response.Response;
import work.ready.core.template.Engine;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class TemplateRender extends Render {

	protected Engine engine;
	protected String contentSecurityPolicy;

	public TemplateRender(RenderManager renderManager, String view) {
		setRenderManager(renderManager);
		this.engine = renderManager.getEngine();
		this.view = view;
	}

	public ContentType getContentType() {
		return ContentType.TEXT_HTML;
	}

	public void render() {

		response.setContentType(getContentType());
		response.setStatus(StatusCodes.OK);

		appendSecurityHeaders(response);

		Map<Object, Object> data = new HashMap<Object, Object>();
		for (Enumeration<String> attrs = request.getAttributeNames(); attrs.hasMoreElements();) {
			String attrName = attrs.nextElement();
			data.put(attrName, request.getAttribute(attrName));
		}

		try {
			response.startBlocking();
			OutputStream os = response.getOutputStream();
			engine.getTemplate(view).render(data, os);
			os.flush();

		} catch (RuntimeException e) {	
			Throwable cause = e.getCause();
			if (cause instanceof IOException) {	
				String name = cause.getClass().getSimpleName();
				if ("ClientAbortException".equals(name) || "EofException".equals(name)) {
					return ;
				}
			}

			throw e;
		} catch (Exception e) {
			throw new RenderException(e);
		}
	}

	protected void appendSecurityHeaders(Response response) {
		response.setHeader("Strict-Transport-Security", "max-age=31536000");
		if (contentSecurityPolicy != null) {
			response.setHeader("Content-Security-Policy", contentSecurityPolicy);
		}
		response.setHeader("X-XSS-Protection", "1; mode=block");       
		response.setHeader("X-Content-Type-Options", "nosniff");
	}

	public String toString() {
		return view;
	}
}

