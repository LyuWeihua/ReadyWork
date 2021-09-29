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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.undertow.util.StatusCodes;
import work.ready.core.component.i18n.I18n;
import work.ready.core.handler.ContentType;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;
import work.ready.core.service.result.Result;
import work.ready.core.service.status.Status;

import java.util.*;

public class XmlRender extends Render {

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

	protected String xmlText;
	protected String[] attrs;

	public XmlRender() {

	}

	@SuppressWarnings("serial")
	public XmlRender(final String key, final Object value) {
		if (key == null) {
			throw new IllegalArgumentException("The parameter key can not be null.");
		}
		try {
			this.xmlText = Ready.config().getXmlMapper().writeValueAsString(new HashMap<String, Object>(){{put(key, value);}});
		} catch (JsonProcessingException e){
			throw new RenderException(e);
		}
	}

	public XmlRender(String[] attrs) {
		if (attrs == null) {
			throw new IllegalArgumentException("The parameter attrs can not be null.");
		}
		this.attrs = attrs;
	}

	public XmlRender(String xmlText) {
		if (xmlText == null) {

			this.xmlText = "null";
		} else {
			this.xmlText = xmlText;
		}
	}

	public XmlRender(Object object) {
		try {
			if(object instanceof Result) {
				if (((Result<?>) object).isSuccess()) {
					object = ((Result<?>) object).getResult();
				} else {
					object = ((Result<?>) object).getError();
				}
			}
			if (object instanceof Status) {
				this.statusCode = ((Status) object).getHttpCode();
				this.xmlText = ((Status) object).toXml();
			} else {
				this.xmlText = object instanceof String ? (String)object : Ready.config().getXmlMapper().writeValueAsString(object);
			}
		} catch (JsonProcessingException e) {
			throw new RenderException(e);
		}
	}

	public void render() {
		if (xmlText == null) {
			buildXmlText();
		}

		try {
			response.setContentType(ContentType.TEXT_XML);
			response.setStatus(statusCode);
			logger.info("response: %s", xmlText);
			response.send(xmlText);
		} catch (Exception e) {
			throw new RenderException(e);
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	protected void buildXmlText() {
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
		try {
			this.xmlText = Ready.config().getXmlMapper().writeValueAsString(map);
		} catch (JsonProcessingException e){
			throw new RenderException(e);
		}
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

	public String getXmlText() {
		return xmlText;
	}

}

