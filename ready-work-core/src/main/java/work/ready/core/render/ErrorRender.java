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
import work.ready.core.module.Version;
import work.ready.core.service.status.Status;

public class ErrorRender extends Render {

	protected static final String version = "<center><a href='https://ready.work/' target='_blank'><b>Powered by Ready.Work " + Version.CURRENT + "</b></a></center>";

	protected static final byte[] html404 = ("<html><head><title>404 Not Found</title></head><body bgcolor='white'><center><h1>404 Not Found</h1></center><hr>" + version + "</body></html>").getBytes();
	protected static final byte[] html500 = ("<html><head><title>500 Internal Server Error</title></head><body bgcolor='white'><center><h1>500 Internal Server Error</h1></center><hr>" + version + "</body></html>").getBytes();

	protected static final byte[] html400 = ("<html><head><title>400 Bad Request</title></head><body bgcolor='white'><center><h1>400 Bad Request</h1></center><hr>" + version + "</body></html>").getBytes();
	protected static final byte[] html401 = ("<html><head><title>401 Unauthorized</title></head><body bgcolor='white'><center><h1>401 Unauthorized</h1></center><hr>" + version + "</body></html>").getBytes();
	protected static final byte[] html403 = ("<html><head><title>403 Forbidden</title></head><body bgcolor='white'><center><h1>403 Forbidden</h1></center><hr>" + version + "</body></html>").getBytes();
	protected final RenderManager renderManager;
	protected int errorCode;
	protected String customizedHtml = null;
	protected ContentType contentType;

	public ErrorRender(RenderManager renderManager, int errorCode, String view) {
		this.renderManager = renderManager;
		this.errorCode = errorCode;
		this.view = view;
		this.contentType = ContentType.TEXT_HTML;
	}

	public ErrorRender(RenderManager renderManager, Status status){
		this.renderManager = renderManager;
		this.errorCode = status.getHttpCode();
		this.customizedHtml = status.toString();
		this.contentType = ContentType.APPLICATION_JSON;
	}

	public void render() {
		response.setStatus(errorCode);

		String view = getView();
		if (view != null) {
			renderManager.getRenderFactory().getRender(view).setContext(request, response).render();
			return;
		}

		try {
			response.setContentType(this.contentType);
			response.send(new String(getErrorHtml()));
		} catch (Exception e) {
			throw new RenderException(e);
		}
	}

	public void render(String customizedHtml){
		this.customizedHtml = customizedHtml;
		render();
	}

	public byte[] getErrorHtml() {
		if(customizedHtml != null) return customizedHtml.getBytes();
		int errorCode = getErrorCode();
		if (errorCode == StatusCodes.NOT_FOUND)
			return html404;
		if (errorCode == StatusCodes.INTERNAL_SERVER_ERROR)
			return html500;
		if (errorCode == StatusCodes.BAD_REQUEST)
			return html400;
		if (errorCode == StatusCodes.UNAUTHORIZED)
			return html401;
		if (errorCode == StatusCodes.FORBIDDEN)
			return html403;
		return ("<html><head><title>" + errorCode + " Error</title></head><body bgcolor='white'><center><h1>" + errorCode + " Error</h1></center><hr>" + version + "</body></html>").getBytes();
	}

	public int getErrorCode() {
		return errorCode;
	}
}

