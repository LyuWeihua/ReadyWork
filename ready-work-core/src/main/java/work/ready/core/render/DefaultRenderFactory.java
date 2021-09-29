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

import work.ready.core.render.captcha.CaptchaRender;
import work.ready.core.handler.ContentType;
import work.ready.core.service.status.Status;

import java.io.File;

public class DefaultRenderFactory implements RenderFactory {

	protected RenderManager renderManager;

	public DefaultRenderFactory(){
	}

	public void setRenderManager(RenderManager renderManager) {
		this.renderManager = renderManager;
	}

	public RenderManager getRenderManager() {
		return renderManager;
	}

	public Render getRender(String view) {
		return getTemplateRender(view);
	}

	public Render getTemplateRender(String view) {
		return new TemplateRender(renderManager, view + renderManager.viewFileExt);
	}

	public Render getJsonRender() {
		return new JsonRender();
	}

	public Render getJsonRender(String key, Object value) {
		return new JsonRender(key, value);
	}

	public Render getJsonRender(String[] attrs) {
		return new JsonRender(attrs);
	}

	public Render getJsonRender(String jsonText) {
		return new JsonRender(jsonText);
	}

	public Render getJsonRender(Object object) {
		return new JsonRender(object);
	}

	public Render getXmlRender(String xml) {
		return new XmlRender(xml);
	}

	public Render getXmlRender(Object object) {
		return new XmlRender(object);
	}

	public Render getTextRender(String text) {
		return new TextRender(text);
	}

	public Render getTextRender(String text, String contentType) {
		return new TextRender(text, contentType);
	}

	public Render getTextRender(String text, ContentType contentType) {
		return new TextRender(text, contentType);
	}

	public Render getDefaultRender(String view) {
		return getRender(view);
	}

	public ErrorRender getErrorRender(int errorCode, String view) {
		return new ErrorRender(renderManager, errorCode, view);
	}

	public ErrorRender getErrorRender(int errorCode) {
		return new ErrorRender(renderManager, errorCode, renderManager.getErrorView(errorCode));
	}

	public ErrorRender getErrorRender(Status status) {
		return new ErrorRender(renderManager, status);
	}

	public Render getFileRender(String fileName) {
		return new FileRender(renderManager, fileName);
	}

	public Render getFileRender(String fileName, String downloadFileName) {
		return new FileRender(renderManager, fileName, downloadFileName);
	}

	public Render getFileRender(File file) {
		return new FileRender(renderManager, file);
	}

	public Render getFileRender(File file, String downloadFileName) {
		return new FileRender(renderManager, file, downloadFileName);
	}

	public Render getRedirectRender(String url) {
		return new RedirectRender(url);
	}

	public Render getRedirectRender(String url, boolean withQueryString) {
		return new RedirectRender(url, withQueryString);
	}

	public Render getRedirect301Render(String url) {
		return new Redirect301Render(url);
	}

	public Render getRedirect301Render(String url, boolean withQueryString) {
		return new Redirect301Render(url, withQueryString);
	}

	public Render getNullRender() {
		return new NullRender();
	}

	public Render getJavascriptRender(String jsText) {
		return new JavascriptRender(jsText);
	}

	public Render getHtmlRender(String htmlText) {
		return new HtmlRender(htmlText);
	}

	public Render getCaptchaRender() {
		return new CaptchaRender();
	}

	public Render getQrCodeRender(String content, int width, int height) {
		return new QrCodeRender(content, width, height);
	}

	public Render getQrCodeRender(String content, int width, int height, char errorCorrectionLevel) {
		return new QrCodeRender(content, width, height, errorCorrectionLevel);
	}

}

