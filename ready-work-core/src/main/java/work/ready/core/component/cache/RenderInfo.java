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

package work.ready.core.component.cache;

import work.ready.core.render.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class RenderInfo implements Serializable {

	private static final long serialVersionUID = -7299875545092102194L;

	protected String view;
	protected Integer renderType;
	protected Map<String, Object> otherParam = null;

	public RenderInfo(Render render) {
		if (render == null) {
			throw new IllegalArgumentException("Render can not be null.");
		}
		view = render.getView();
		if (render instanceof TemplateRender) {
			renderType = RenderType.TEMPLATE_RENDER;
		} else if (render instanceof XmlRender) {
			XmlRender xr = (XmlRender)render;
			renderType = RenderType.XML_RENDER;
			otherParam = new HashMap<String, Object>();
			otherParam.put("xmlText", xr.getXmlText());
			otherParam.put("attrs", xr.getAttrs());
			otherParam.put("statusCode", xr.getStatusCode());
		} else if(render instanceof JsonRender) {
			JsonRender jr = (JsonRender)render;
			renderType = RenderType.JSON_RENDER;
			otherParam = new HashMap<String, Object>();
			otherParam.put("jsonText", jr.getJsonText());
			otherParam.put("attrs", jr.getAttrs());
			otherParam.put("statusCode", jr.getStatusCode());
			otherParam.put("forIE", jr.getForIE());
		}
		else
			throw new IllegalArgumentException("CacheInterceptor can not support the render of the type : " + render.getClass().getName());
	}

	public Render createRender(RenderManager renderManager) {
		switch (renderType) {
		case RenderType.TEMPLATE_RENDER:
			return new TemplateRender(renderManager, view);
		case RenderType.XML_RENDER:
			XmlRender xr;
			if (otherParam.get("xmlText") != null) {
				xr = new XmlRender((String)otherParam.get("xmlText"));
			} else if (otherParam.get("attrs") != null) {
				xr = new XmlRender((String[])otherParam.get("attrs"));
			} else {
				xr = new XmlRender();
			}
			if (otherParam.get("statusCode") != null){
				xr.setStatusCode((int) otherParam.get("statusCode"));
			}
			return xr;
		case RenderType.JSON_RENDER:
			JsonRender jr;
			if (otherParam.get("jsonText") != null) {
				jr = new JsonRender((String)otherParam.get("jsonText"));
			} else if (otherParam.get("attrs") != null) {
				jr = new JsonRender((String[])otherParam.get("attrs"));
			} else {
				jr = new JsonRender();
			}
			if (otherParam.get("statusCode") != null){
				jr.setStatusCode((int) otherParam.get("statusCode"));
			}
			if (Boolean.TRUE.equals(otherParam.get("forIE"))) {
				jr.forIE();
			}
			return jr;
		default :
			throw new IllegalArgumentException("CacheInterceptor can not support the renderType of the value : " + renderType);
		}
	}
}
