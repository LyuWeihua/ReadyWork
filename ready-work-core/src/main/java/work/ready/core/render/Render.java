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

import work.ready.core.handler.request.HttpRequest;
import work.ready.core.handler.response.HttpResponse;
import work.ready.core.server.Ready;

public abstract class Render {

	protected String view;
	protected HttpRequest request;
	protected HttpResponse response;
	protected RenderManager renderManager;

	public Render setRenderManager(RenderManager manager){
		this.renderManager = manager;
		return this;
	}

	public RenderManager getRenderManager(){
		return renderManager;
	}

	public Render setContext(HttpRequest request, HttpResponse response) {
		this.request = request;
		this.response = response;
		return this;
	}

	public Render setContext(HttpRequest request, HttpResponse response, String viewPath) {
		this.request = request;
		this.response = response;
		if (view != null && view.length() > 0 && view.charAt(0) != '/') {
			view = viewPath + view;
		}
		return this;
	}

	public String getView() {
		return view;
	}

	public void setView(String view) {
		this.view = view;
	}

	public abstract void render();
}
