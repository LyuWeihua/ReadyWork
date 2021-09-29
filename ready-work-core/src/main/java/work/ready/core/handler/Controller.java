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
package work.ready.core.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import work.ready.core.config.Config;
import work.ready.core.database.Model;
import work.ready.core.database.Record;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.render.captcha.CaptchaRender;
import work.ready.core.exception.ApiException;
import work.ready.core.handler.action.Action;
import work.ready.core.handler.request.HttpRequest;
import work.ready.core.handler.request.UploadFile;
import work.ready.core.handler.response.HttpResponse;
import work.ready.core.handler.cookie.Cookie;
import work.ready.core.handler.cookie.CookieItem;
import work.ready.core.handler.session.HttpSession;
import work.ready.core.render.JsonRender;
import work.ready.core.render.Render;
import work.ready.core.render.RenderManager;
import work.ready.core.component.i18n.I18n;
import work.ready.core.component.i18n.Res;
import work.ready.core.server.Ready;
import work.ready.core.service.status.Status;
import work.ready.core.tools.define.Kv;
import work.ready.core.tools.StrUtil;
import work.ready.core.tools.converter.TypeConverter;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.*;
import java.util.Map.Entry;

import static work.ready.core.security.data.DataSecurityInspector.auditClassKey;
import static work.ready.core.security.data.DataSecurityInspector.auditMethodKey;

@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class Controller {
	private static final Log logger = LogFactory.getLog(Controller.class);
	protected static final String PARAM_PARSE_EXCEPTION = "ERROR10150";
	private Action action;

	private HttpRequest request;
	private HttpResponse response;

	private String rawData;

	private Render render;

	private RenderManager renderManager;

	void _init_(Action action, HttpRequest request, HttpResponse response) {
		this.action = action;
		this.request = request;
		this.response = response;
		render = null;
		rawData = null;
	}

	protected void _clear_() {
		action = null;
		request = null;
		response = null;
		render = null;
		rawData = null;
	}

	public String getRawData() {
		if (rawData == null) {
			rawData = StrUtil.trimToEmpty(new String(Optional.ofNullable(request.getBody()).orElse(new byte[0]), StandardCharsets.UTF_8));
		}
		return rawData;
	}

	public String getControllerKey() {
		return action.getControllerKey();
	}

	public String getViewPath() {
		return action.getViewPath();
	}

	public void setHttpRequest(HttpRequest request) {
		this.request = request;
	}

	public void setHttpResponse(HttpResponse response) {
		this.response = response;
	}

	public Controller setAttr(String name, Object value) {
		request.setAttribute(name, value);
		return this;
	}

	public Controller removeAttr(String name) {
		request.removeAttribute(name);
		return this;
	}

	public Controller setAttrs(Map<String, Object> attrMap) {
		if(attrMap != null)
		for (Entry<String, Object> entry : attrMap.entrySet())
			request.setAttribute(entry.getKey(), entry.getValue());
		return this;
	}

	public Map<String, Object> inputAudit(Map<String, Object> inputMap){
		return inputAudit(null, null, inputMap);
	}

	public Map<String, Object> inputAudit(Map<String, Object> inputMap, Class<? extends Model> model){
		return inputAudit(Ready.dbManager().getTable(model).getDatasource(), Ready.dbManager().getTable(model).getDatasource(), inputMap);
	}

	private Map<String, Object> inputAudit(String datasource, String table, Map<String, Object> inputMap){
		if(Ready.dbManager().getDataSecurityInspector() != null) {
			return Ready.dbManager().getDataSecurityInspector().inputExamine(getAttr(auditClassKey), getAttr(auditMethodKey), datasource, table, inputMap);
		}
		return inputMap;
	}

	public <T extends Model> T inputAudit(T input){
		if(Ready.dbManager().getDataSecurityInspector() != null) {
			return Ready.dbManager().getDataSecurityInspector().inputExamine(getAttr(auditClassKey), getAttr(auditMethodKey), input);
		}
		return input;
	}

	public Map<String, Object> outputAudit(Map<String, Object> outputMap){
		return outputAudit(null, null, outputMap);
	}

	public Map<String, Object> outputAudit(Map<String, Object> outputMap, Class<? extends Model> model){
		return outputAudit(Ready.dbManager().getTable(model).getDatasource(), Ready.dbManager().getTable(model).getName(), outputMap);
	}

	private Map<String, Object> outputAudit(String datasource, String table, Map<String, Object> outputMap){
		if(Ready.dbManager().getDataSecurityInspector() != null) {
			return Ready.dbManager().getDataSecurityInspector().outputExamine(getAttr(auditClassKey), getAttr(auditMethodKey), datasource, table, outputMap);
		}
		return outputMap;
	}

	public Record outputAudit(Record output) {
		return outputAudit(null, null, output);
	}

	public Record outputAudit(Record output, Class<? extends Model> model) {
		return outputAudit(Ready.dbManager().getTable(model).getDatasource(), Ready.dbManager().getTable(model).getName(), output);
	}

	private Record outputAudit(String datasource, String table, Record output){
		if(Ready.dbManager().getDataSecurityInspector() != null) {
			return Ready.dbManager().getDataSecurityInspector().outputExamine(getAttr(auditClassKey), getAttr(auditMethodKey), datasource, table, output);
		}
		return output;
	}

	public <T extends Model> T outputAudit(T output){
		if(Ready.dbManager().getDataSecurityInspector() != null) {
			return Ready.dbManager().getDataSecurityInspector().outputExamine(getAttr(auditClassKey), getAttr(auditMethodKey), output);
		}
		return output;
	}

	public String getParam(String name) {
		String result = request.getParameter(name);
		return result != null && result.length() != 0 ? result : null;
	}

	public String getParam(String name, String defaultValue) {
		String result = request.getParameter(name);
		return result != null && !"".equals(result) ? result : defaultValue;
	}

	public Map<String, String> getParamMap() {
		return request.getParameterMap(HttpRequest.parameterMap.formPrioritized);
	}

	public Map<String, String[]> getParamMaps() {
		return request.getParameterMap();
	}

	public Enumeration<String> getParamNames() {
		return request.getParameterNames();
	}

	public String[] getParamValues(String name) {
		return request.getParameterValues(name);
	}

	public Integer[] getParamValuesToInt(String name) {
		String[] values = request.getParameterValues(name);
		if (values == null || values.length == 0) {
			return null;
		}
		Integer[] result = new Integer[values.length];
		for (int i=0; i<result.length; i++) {
			result[i] = StrUtil.isBlank(values[i]) ? null : Integer.parseInt(values[i]);
		}
		return result;
	}

	public Long[] getParamValuesToLong(String name) {
		String[] values = request.getParameterValues(name);
		if (values == null || values.length == 0) {
			return null;
		}
		Long[] result = new Long[values.length];
		for (int i=0; i<result.length; i++) {
			result[i] = StrUtil.isBlank(values[i]) ? null : Long.parseLong(values[i]);
		}
		return result;
	}

	public Enumeration<String> getAttrNames() {
		return request.getAttributeNames();
	}

	public <T> T getAttr(String name) {
		return (T)request.getAttribute(name);
	}

	public <T> T getAttr(String name, T defaultValue) {
		T result = (T)request.getAttribute(name);
		return result != null ? result : defaultValue;
	}

	public String getAttrForStr(String name) {
		return (String)request.getAttribute(name);
	}

	public Integer getAttrForInt(String name) {
		return (Integer)request.getAttribute(name);
	}

	public String getHeader(String name) {
		return request.getHeader(name);
	}

	public Res getI18n(){
		return getAttr(I18n.localeParamName);
	}

	private Integer toInt(String value, Integer defaultValue) {
		try {
			if (StrUtil.isBlank(value))
				return defaultValue;
			value = value.trim();
			if (value.startsWith("N") || value.startsWith("n"))
				return -Integer.parseInt(value.substring(1));
			return Integer.parseInt(value);
		}
		catch (Exception e) {
			throw new ApiException(new Status(PARAM_PARSE_EXCEPTION, value, Integer.class));
		}
	}

	public Integer getParamToInt(String name) {
		return toInt(request.getParameter(name), null);
	}

	public Integer getParamToInt(String name, Integer defaultValue) {
		return toInt(request.getParameter(name), defaultValue);
	}

	private Long toLong(String value, Long defaultValue) {
		try {
			if (StrUtil.isBlank(value))
				return defaultValue;
			value = value.trim();
			if (value.startsWith("N") || value.startsWith("n"))
				return -Long.parseLong(value.substring(1));
			return Long.parseLong(value);
		}
		catch (Exception e) {
			throw new ApiException(new Status(PARAM_PARSE_EXCEPTION, value, Long.class));
		}
	}

	public Long getParamToLong(String name) {
		return toLong(request.getParameter(name), null);
	}

	public Long getParamToLong(String name, Long defaultValue) {
		return toLong(request.getParameter(name), defaultValue);
	}

	private Boolean toBoolean(String value, Boolean defaultValue) {
		if (StrUtil.isBlank(value))
			return defaultValue;
		value = value.trim().toLowerCase();
		if ("1".equals(value) || "true".equals(value))
			return Boolean.TRUE;
		else if ("0".equals(value) || "false".equals(value))
			return Boolean.FALSE;
		throw new ApiException(new Status(PARAM_PARSE_EXCEPTION, value, Boolean.class));
	}

	public Boolean getParamToBoolean(String name) {
		return toBoolean(request.getParameter(name), null);
	}

	public Boolean getParamToBoolean(String name, Boolean defaultValue) {
		return toBoolean(request.getParameter(name), defaultValue);
	}

	private Date toDate(String value, Date defaultValue) {
		try {
			if (StrUtil.isBlank(value))
				return defaultValue;

			return (Date) TypeConverter.getInstance().convert(Date.class, value);

		} catch (Exception e) {
			throw new ApiException(new Status(PARAM_PARSE_EXCEPTION, value, Date.class));
		}
	}

	public Date getParamToDate(String name) {
		return toDate(request.getParameter(name), null);
	}

	public Date getParamToDate(String name, Date defaultValue) {
		return toDate(request.getParameter(name), defaultValue);
	}

	public HttpRequest getRequest() {
		return request;
	}

	public HttpResponse getResponse() {
		return response;
	}

	public void setRenderManager(RenderManager renderManager) { this.renderManager = renderManager; }

	public RenderManager getRenderManager() { return renderManager; }

	public HttpSession getSession() {
		return request.getSession();
	}

	public HttpSession getSession(boolean create) {
		return request.getSession(create);
	}

	public <T> T getSessionAttr(String key) {
		HttpSession session = request.getSession(false);
		return session != null ? (T)session.getAttribute(key) : null;
	}

	public <T> T getSessionAttr(String key, T defaultValue) {
		T result = getSessionAttr(key);
		return result != null ? result : defaultValue;
	}

	public Controller setSessionAttr(String key, Object value) {
		request.getSession(true).setAttribute(key, value);
		return this;
	}

	public Controller removeSessionAttr(String key) {
		HttpSession session = request.getSession(false);
		if (session != null)
			session.removeAttribute(key);
		return this;
	}

	public String getCookie(String name, String defaultValue) {
		Cookie cookie = getCookieObject(name);
		return cookie != null ? cookie.getValue() : defaultValue;
	}

	public String getCookie(String name) {
		return getCookie(name, null);
	}

	public Integer getCookieToInt(String name) {
		String result = getCookie(name);
		return result != null ? Integer.parseInt(result) : null;
	}

	public Integer getCookieToInt(String name, Integer defaultValue) {
		String result = getCookie(name);
		return result != null ? Integer.parseInt(result) : defaultValue;
	}

	public Long getCookieToLong(String name) {
		String result = getCookie(name);
		return result != null ? Long.parseLong(result) : null;
	}

	public Long getCookieToLong(String name, Long defaultValue) {
		String result = getCookie(name);
		return result != null ? Long.parseLong(result) : defaultValue;
	}

	public Cookie getCookieObject(String name) {
		Cookie[] cookies = request.getCookies();
		if (cookies != null)
			for (Cookie cookie : cookies)
				if (cookie.getName().equals(name))
					return cookie;
		return null;
	}

	public Cookie[] getCookieObjects() {
		Cookie[] result = request.getCookies();
		return result != null ? result : new Cookie[0];
	}

	public Controller setCookie(String name, String value, int maxAgeInSeconds, boolean isHttpOnly) {
		return doSetCookie(name, value, maxAgeInSeconds, null, null, isHttpOnly);
	}

	public Controller setCookie(String name, String value, int maxAgeInSeconds) {
		return doSetCookie(name, value, maxAgeInSeconds, null, null, null);
	}

	public Controller setCookie(Cookie cookie) {
		response.addCookie(cookie);
		return this;
	}

	public Controller setCookie(String name, String value, int maxAgeInSeconds, String path, boolean isHttpOnly) {
		return doSetCookie(name, value, maxAgeInSeconds, path, null, isHttpOnly);
	}

	public Controller setCookie(String name, String value, int maxAgeInSeconds, String path) {
		return doSetCookie(name, value, maxAgeInSeconds, path, null, null);
	}

	public Controller setCookie(String name, String value, int maxAgeInSeconds, String path, String domain, boolean isHttpOnly) {
		return doSetCookie(name, value, maxAgeInSeconds, path, domain, isHttpOnly);
	}

	public Controller removeCookie(String name) {
		return doSetCookie(name, null, 0, null, null, null);
	}

	public Controller removeCookie(String name, String path) {
		return doSetCookie(name, null, 0, path, null, null);
	}

	public Controller removeCookie(String name, String path, String domain) {
		return doSetCookie(name, null, 0, path, domain, null);
	}

	private Controller doSetCookie(String name, String value, int maxAgeInSeconds, String path, String domain, Boolean isHttpOnly) {
		Cookie cookie = new CookieItem(name, value);
		cookie.setMaxAge(maxAgeInSeconds);

		if (path == null) {
			path = "/";
		}
		cookie.setPath(path);

		if (domain != null) {
			cookie.setDomain(domain);
		}
		if (isHttpOnly != null) {
			cookie.setHttpOnly(isHttpOnly);
		}
		response.addCookie(cookie);
		return this;
	}

	public Map<String, String> getPathParam(){
		return request.getPathParameters();
	}

	public String getPathParam(String paramName) {
		return request.getPathParameter(paramName);
	}

	public String getPathParam(String paramName, String defaultValue) {
		String result = getPathParam(paramName);
		return result != null && !"".equals(result) ? result : defaultValue;
	}

	public Integer getPathParamToInt(String paramName) {
		return toInt(getPathParam(paramName), null);
	}

	public Integer getPathParamToInt(String paramName, Integer defaultValue) {
		return toInt(getPathParam(paramName), defaultValue);
	}

	public Long getPathParamToLong(String paramName) {
		return toLong(getPathParam(paramName), null);
	}

	public Long getPathParamToLong(String paramName, Long defaultValue) {
		return toLong(getPathParam(paramName), defaultValue);
	}

	public Boolean getPathParamToBoolean(String paramName) {
		return toBoolean(getPathParam(paramName), null);
	}

	public Boolean getPathParamToBoolean(String paramName, Boolean defaultValue) {
		return toBoolean(getPathParam(paramName), defaultValue);
	}

	public Date getPathParamToDate(String paramName) {
		return toDate(getPathParam(paramName), null);
	}

	public Date getPathParamToDate(String paramName, Date defaultValue) {
		return toDate(getPathParam(paramName), defaultValue);
	}

	public <T> T getModel(Class<T> modelClass) {
		try{
			if(getRequest().isJsonRequest()){
				return Ready.config().getJsonMapper().readValue(getRawData(), modelClass);
			} else {
				Map<String, String> formMap = getRequest().getParameterMap(HttpRequest.parameterMap.formPrioritized);
				return Ready.config().getJsonMapper().convertValue(formMap, modelClass);
			}
		} catch (Exception e){
			logger.error(e.getMessage());
			return null;
		}
	}

	public <T> T getBean(TypeReference<T> reference) {
		try{
			if(getRequest().isJsonRequest()){
				return Ready.config().getJsonMapper().readValue(getRawData(), reference);
			} else {
				Map<String, String> formMap = getRequest().getParameterMap(HttpRequest.parameterMap.formPrioritized);
				return Ready.config().getJsonMapper().convertValue(formMap, reference);
			}
		} catch (Exception e){
			logger.error(e.getMessage());
			return null;
		}
	}

	public <T> T getBean(Class<T> beanClass) {
		try{
			if(getRequest().isJsonRequest()){
				return Ready.config().getJsonMapper().readValue(getRawData(), beanClass);
			} else {
				Map<String, String> formMap = getRequest().getParameterMap(HttpRequest.parameterMap.formPrioritized);
				return Ready.config().getJsonMapper().convertValue(formMap, beanClass);
			}
		} catch (Exception e){
			logger.error(e.getMessage());
			return null;
		}
	}

	public Kv getKv() {
		Kv kv = new Kv();
		Map<String, String[]> paraMap = request.getParameterMap();
		for (Entry<String, String[]> entry : paraMap.entrySet()) {
			String[] values = entry.getValue();
			String value = (values != null && values.length > 0) ? values[0] : null;
			kv.put(entry.getKey(), "".equals(value) ? null : value);
		}
		return kv;
	}

	public Map<String, UploadFile> getFiles(){
		return request.getFiles();
	}

	public UploadFile getFile(String parameterName){
		return getFiles().get(parameterName);
	}

	public Controller keepParam() {
		Map<String, String[]> map = request.getParameterMap();
		for (Entry<String, String[]> e: map.entrySet()) {
			String[] values = e.getValue();
			if (values.length == 1)
				request.setAttribute(e.getKey(), values[0]);
			else
				request.setAttribute(e.getKey(), values);
		}
		return this;
	}

	public Controller keepParam(String... names) {
		for (String name : names) {
			String[] values = request.getParameterValues(name);
			if (values != null) {
				if (values.length == 1)
					request.setAttribute(name, values[0]);
				else
					request.setAttribute(name, values);
			}
		}
		return this;
	}

	public Controller keepParam(Class type, String name) {
		String[] values = request.getParameterValues(name);
		if (values != null) {
			if (values.length == 1)
				try {request.setAttribute(name, TypeConverter.getInstance().convert(type, values[0]));} catch (ParseException e) {
						logger.error(e.getMessage());
					}
			else
				request.setAttribute(name, values);
		}
		return this;
	}

	public Controller keepParam(Class type, String... names) {
		if (type == String.class)
			return keepParam(names);

		if (names != null)
			for (String name : names)
				keepParam(type, name);
		return this;
	}

	public Controller keepModel(Class<? extends Model> modelClass) {
		if (modelClass != null) {
			Object model = getModel(modelClass);
			request.setAttribute(StrUtil.firstCharToLowerCase(modelClass.getSimpleName()), model);
		} else {
			keepParam();
		}
		return this;
	}

	public Controller keepBean(Class<?> beanClass) {
		if (beanClass != null) {
			Object bean = getBean(beanClass);
			request.setAttribute(StrUtil.firstCharToLowerCase(beanClass.getSimpleName()), bean);
		} else {
			keepParam();
		}
		return this;
	}

	public boolean isParamBlank(String paramName) {
		return StrUtil.isBlank(request.getParameter(paramName));
	}

	public boolean isPathParamBlank(String paramName) {
		return StrUtil.isBlank(getPathParam(paramName));
	}

	public boolean isParamExists(String paramName) {
		return request.getParameterMap().containsKey(paramName);
	}

	public boolean isPathParamExists(String paramName) {
		return getPathParam(paramName) != null;
	}

	public Render getRender() {
		return render;
	}

	public void render(Render render) {
		this.render = render;
	}

	public void render(String view) {
		render = renderManager.getRenderFactory().getRender(view);
	}

	public String renderToString(String template, Map data) {
		if (template.charAt(0) != '/') {
			template = action.getViewPath() + template;
		}
		return renderManager.getEngine().getTemplate(template).renderToString(data);
	}

	public void renderTemplate(String template) {
		render = renderManager.getRenderFactory().getTemplateRender(template);
	}

	public void renderJson(String key, Object value) {
		render = renderManager.getRenderFactory().getJsonRender(key, value);
	}

	public void renderJson() {
		render = renderManager.getRenderFactory().getJsonRender();
	}

	public void renderJson(String[] attrs) {
		render = renderManager.getRenderFactory().getJsonRender(attrs);
	}

	public void renderJson(String jsonText) {
		render = renderManager.getRenderFactory().getJsonRender(jsonText);
	}

	public void renderJson(Object object) {
		render = object instanceof JsonRender ? (JsonRender)object : renderManager.getRenderFactory().getJsonRender(object);
	}

	public void renderText(String text) {
		render = renderManager.getRenderFactory().getTextRender(text);
	}

	public void renderText(String text, String contentType) {
		render = renderManager.getRenderFactory().getTextRender(text, contentType);
	}

	public void renderText(String text, ContentType contentType) {
		render = renderManager.getRenderFactory().getTextRender(text, contentType);
	}

	public void renderFile(String fileName) {
		render = renderManager.getRenderFactory().getFileRender(fileName);
	}

	public void renderFile(String fileName, String downloadFileName) {
		render = renderManager.getRenderFactory().getFileRender(fileName, downloadFileName);
	}

	public void renderFile(File file) {
		render = renderManager.getRenderFactory().getFileRender(file);
	}

	public void renderFile(File file, String downloadFileName) {
		render = renderManager.getRenderFactory().getFileRender(file, downloadFileName);
	}

	public void redirect(String url) {
		render = renderManager.getRenderFactory().getRedirectRender(url);
	}

	public void redirect(String url, boolean withQueryString) {
		render = renderManager.getRenderFactory().getRedirectRender(url, withQueryString);
	}

	public void render(String view, int status) {
		render = renderManager.getRenderFactory().getRender(view);
		response.setStatus(status);
	}

	public void redirect301(String url) {
		render = renderManager.getRenderFactory().getRedirect301Render(url);
	}

	public void redirect301(String url, boolean withQueryString) {
		render = renderManager.getRenderFactory().getRedirect301Render(url, withQueryString);
	}

	public void renderNull() {
		render = renderManager.getRenderFactory().getNullRender();
	}

	public void renderJavascript(String javascriptText) {
		render = renderManager.getRenderFactory().getJavascriptRender(javascriptText);
	}

	public void renderHtml(String htmlText) {
		render = renderManager.getRenderFactory().getHtmlRender(htmlText);
	}

	public void renderXml(String view) {
		render = renderManager.getRenderFactory().getXmlRender(view);
	}

	public void renderCaptcha() {
		render = renderManager.getRenderFactory().getCaptchaRender();
	}

	public void renderQrCode(String content, int width, int height) {
		render = renderManager.getRenderFactory().getQrCodeRender(content, width, height);
	}

	public void renderQrCode(String content, int width, int height, char errorCorrectionLevel) {
		render = renderManager.getRenderFactory().getQrCodeRender(content, width, height, errorCorrectionLevel);
	}

	public boolean validateCaptcha(String paramName) {
		return CaptchaRender.validate(this, getParam(paramName));
	}
}

