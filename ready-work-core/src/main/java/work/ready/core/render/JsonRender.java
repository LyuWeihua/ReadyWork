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
import work.ready.core.component.i18n.I18n;
import work.ready.core.handler.ContentType;
import work.ready.core.json.Json;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.service.result.Result;
import work.ready.core.service.status.Status;

import java.util.*;

public class JsonRender extends Render {

	private static final Log logger = LogFactory.getLog(TextRender.class);
	private int statusCode = StatusCodes.OK;

	protected static final Set<String> excludedAttrs = new HashSet<String>() {
		private static final long serialVersionUID = 9186138395157680676L;
		{
			add(I18n.localeParamName);
		}
	};

	public synchronized static void addExcludedAttrs(String... attrs) {
		if (attrs != null) {
			for (String attr : attrs) {
				excludedAttrs.add(attr);
			}
		}
	}

	public synchronized static void removeExcludedAttrs(String... attrs) {
		if (attrs != null) {
			for (String attr : attrs) {
				excludedAttrs.remove(attr);
			}
		}
	}

	public synchronized static void clearExcludedAttrs() {
		excludedAttrs.clear();
	}

	protected static final ContentType contentType = ContentType.APPLICATION_JSON;
	protected static final ContentType contentTypeForIE = ContentType.TEXT_HTML;
	protected boolean forIE = false;

	public JsonRender forIE() {
		forIE = true;
		return this;
	}

	protected String jsonText;
	protected String[] attrs;

	public JsonRender() {

	}

	@SuppressWarnings("serial")
	public JsonRender(final String key, final Object value) {
		if (key == null) {
			throw new IllegalArgumentException("The parameter key can not be null.");
		}
		this.jsonText = Json.getJson().toJson(new HashMap<String, Object>(){{put(key, value);}});
	}

	public JsonRender(String[] attrs) {
		if (attrs == null) {
			throw new IllegalArgumentException("The parameter attrs can not be null.");
		}
		this.attrs = attrs;
	}

	public JsonRender(String jsonText) {
		if (jsonText == null) {

			this.jsonText = "null";
		} else {
			this.jsonText = jsonText;
		}
	}

	public JsonRender(Object object) {
		if(object instanceof Result) {
			if (((Result<?>) object).isSuccess()) {
				object = ((Result<?>) object).getResult();
			} else {
				object = ((Result<?>) object).getError();
			}
		}
		if(object instanceof Status){
			this.statusCode = ((Status) object).getHttpCode();
			this.jsonText = ((Status) object).toString();
		} else {
			this.jsonText = object instanceof String ? (String)object : Json.getJson().toJson(object);
		}
	}

	public void render() {
		if (jsonText == null) {
			buildJsonText();
		}

		try {
			response.setContentType(forIE ? contentTypeForIE : contentType);
			response.setStatus(statusCode);
			logger.info("response: %s", jsonText);
			response.send(jsonText);
		} catch (Exception e) {
			throw new RenderException(e);
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	protected void buildJsonText() {

		Map map = new HashMap();
		if (attrs != null) {
			for (String key : attrs) {
				map.put(key, request.getAttribute(key));
			}
		}
		else {
			for (Enumeration<String> attrs = request.getAttributeNames(); attrs.hasMoreElements();) {
				String key = attrs.nextElement();
				if (excludedAttrs.contains(key)) {
					continue;
				}

				Object value = request.getAttribute(key);
				map.put(key, value);
			}
		}

		this.jsonText = Json.getJson().toJson(map);
	}

	public void setStatusCode(int statusCode){
		this.statusCode = statusCode;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public String[] getAttrs() {
		return attrs;
	}

	public String getJsonText() {
		return jsonText;
	}

	public Boolean getForIE() {
		return forIE;
	}
}

