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
package work.ready.core.handler.route;

import work.ready.core.aop.InterceptorConfig;
import work.ready.core.config.BaseConfig;
import work.ready.core.handler.RequestMethod;
import work.ready.core.handler.Controller;
import work.ready.core.aop.Interceptor;
import work.ready.core.server.Ready;
import work.ready.core.tools.StrUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RouteConfig extends BaseConfig {

	private List<RouteConfig> routesList = new ArrayList<RouteConfig>();
	private Set<String> controllerKeySet = new HashSet<String>();

	static final boolean DEFAULT_MAPPING_SUPER_CLASS = false;	
	Boolean mappingSuperClass = null;

	private String baseViewPath = null;
	private RequestMapping.Produces produces = RequestMapping.Produces.Json;
	private List<String> host = new ArrayList<>();
	private List<String> subHost = new ArrayList<>();
	private List<Route> routeItemList = new ArrayList<Route>();
	private List<Interceptor> injectInterceptors = new ArrayList<Interceptor>();

	private boolean clearAfterMapping = false;

	public RouteConfig setMappingSuperClass(boolean mappingSuperClass) {
		this.mappingSuperClass = mappingSuperClass;
		return this;
	}

	public boolean getMappingSuperClass() {
		return mappingSuperClass != null ? mappingSuperClass : DEFAULT_MAPPING_SUPER_CLASS;
	}

	public RouteConfig setProduces(RequestMapping.Produces produces){
		this.produces = produces;
		return this;
	}

	public RequestMapping.Produces getProduces(){
		return this.produces;
	}

	public RouteConfig add(RouteConfig routes) {

		if (routes.mappingSuperClass == null) {
			routes.mappingSuperClass = this.mappingSuperClass;
		}

		routesList.add(routes);
		return this;
	}

	RouteConfig add(int index, RouteConfig routes){
		if (routes.mappingSuperClass == null) {
			routes.mappingSuperClass = this.mappingSuperClass;
		}

		routesList.add(index, routes);
		return this;
	}

	RouteConfig add(String controllerKey, Class<? extends Controller> controllerClass, String viewPath) {
		routeItemList.add(new Route(controllerKey, controllerClass, viewPath));
		return this;
	}

	RouteConfig add(String controllerKey, Class<? extends Controller> controllerClass) {
		String viewPath = controllerKey.replaceAll("[\\{\\}]",""); 
		return add(controllerKey, controllerClass, viewPath);
	}

	public RouteConfig addRoute(String urlPath, Class<? extends Controller> controllerClass){
		String viewPath = urlPath.replaceAll("[\\{\\}]",""); 
		return addRoute(urlPath, controllerClass, null, null,viewPath);
	}

	public RouteConfig addRoute(String urlPath, Class<? extends Controller> controllerClass, String viewPath){
		return addRoute(urlPath, controllerClass, null, null, viewPath);
	}

	public RouteConfig addRoute(String urlPath, Class<? extends Controller> controllerClass, String controllerMethod, RequestMethod[] requestMethods){
		return addRoute(urlPath, controllerClass, controllerMethod, requestMethods,null);
	}

	public RouteConfig addRoute(String urlPath, Class<? extends Controller> controllerClass,  String controllerMethod, RequestMethod[] requestMethods, String viewPath){
		if(requestMethods == null || requestMethods.length == 0) requestMethods = new RequestMethod[]{RequestMethod.GET, RequestMethod.POST};
		routeItemList.add(new Route(urlPath, controllerClass, controllerMethod, requestMethods, viewPath));
		return this;
	}

	public RouteConfig addInterceptor(Interceptor interceptor) {
		Ready.beanManager().inject(interceptor);
		injectInterceptors.add(interceptor);
		return this;
	}

	public RouteConfig setHost(String... host){
		if(host == null) return this;
		for(int i = 0; i < host.length; i ++){
			if(StrUtil.isBlank(host[i])) throw new RuntimeException("Host name on controller is empty");
			if(!StrUtil.isHostName(host[i], true) && !host[i].equals("*")){
				throw new RuntimeException("Host name '"+ host[i] +"' on controller is invalid");
			}
			this.host.add(host[i]);
		}
		return this;
	}

	public List<String> getHost(){
		return this.host;
	}

	public RouteConfig setSubHost(String... subHost){
		if(subHost == null) return this;
		for(int i = 0; i < subHost.length; i ++){
			if(StrUtil.isBlank(subHost[i])) throw new RuntimeException("subHost name on controller is empty");
			String forCheck = subHost[i];
			if(forCheck.startsWith("*")) forCheck = forCheck.substring(1);
			if(forCheck.endsWith("*")) forCheck = forCheck.substring(0,forCheck.length() - 1);
			if(!StrUtil.isHostName(forCheck, true) && !subHost[i].equals("*")){
				throw new RuntimeException("subHost name '"+ subHost[i] +"' on controller is invalid");
			}
			this.subHost.add(subHost[i]);
		}
		return this;
	}

	public List<String> getSubHost(){
		return this.subHost;
	}

	public RouteConfig setBaseViewPath(String baseViewPath) {
		if (StrUtil.isBlank(baseViewPath)) {
			throw new IllegalArgumentException("baseViewPath can not be blank");
		}

		baseViewPath = baseViewPath.trim();
		if (! baseViewPath.startsWith("/")) {			
			baseViewPath = "/" + baseViewPath;
		}
		if (baseViewPath.endsWith("/")) {				
			baseViewPath = baseViewPath.substring(0, baseViewPath.length() - 1);
		}

		this.baseViewPath = baseViewPath;
		return this;
	}

	public String getBaseViewPath() {
		return baseViewPath;
	}

	public List<Route> getRouteItemList() {
		return routeItemList;
	}

	public Interceptor[] getInterceptors() {
		return injectInterceptors.size() > 0 ?
				injectInterceptors.toArray(new Interceptor[injectInterceptors.size()]) :
				InterceptorConfig.NULL_INTERS;
	}

	public List<RouteConfig> getRoutesList() {
		return routesList;
	}

	public Set<String> getControllerKeySet() {
		return controllerKeySet;
	}

	public void setClearAfterMapping(boolean clearAfterMapping) {
		this.clearAfterMapping = clearAfterMapping;
	}

	public void clear() {
		if (clearAfterMapping) {
			routesList = null;
			controllerKeySet = null;
			baseViewPath = null;
			routeItemList = null;
			injectInterceptors = null;
		}
	}

	class Route {

		private String controllerKey;
		private Class<? extends Controller> controllerClass;
		private String urlPath; 
		private RequestMethod[] requestMethods; 
		private String controllerMethod; 
		private String viewPath;
		private RequestMapping.Produces produces;

		Route(String controllerKey, Class<? extends Controller> controllerClass, RequestMapping.Produces produces) {
			if (StrUtil.isBlank(controllerKey)) {
				throw new IllegalArgumentException("controllerKey can not be blank");
			}
			if (controllerClass == null) {
				throw new IllegalArgumentException("controllerClass can not be null");
			}
			if (produces != null) {
				this.produces = produces;
			}
			this.controllerKey = processControllerKey(controllerKey);
			this.controllerClass = controllerClass;
		}

		Route(String controllerKey, Class<? extends Controller> controllerClass, String viewPath) {
			if (StrUtil.isBlank(controllerKey)) {
				throw new IllegalArgumentException("controllerKey can not be blank");
			}
			if (controllerClass == null) {
				throw new IllegalArgumentException("controllerClass can not be null");
			}
			if (StrUtil.isBlank(viewPath)) {
				viewPath = "/";
			} else {
				this.produces = RequestMapping.Produces.General;
			}

			this.controllerKey = processControllerKey(controllerKey);
			this.controllerClass = controllerClass;
			this.viewPath = processViewPath(viewPath);
		}

		Route(String urlPath, Class<? extends Controller> controllerClass, String controllerMethod, RequestMethod[] requestMethods, String viewPath) {
			if (StrUtil.isBlank(urlPath)) {
				throw new IllegalArgumentException("urlPath can not be blank");
			}
			if (requestMethods == null){
				throw new IllegalArgumentException("requestMethod can not be null");
			}
			if (controllerClass == null) {
				throw new IllegalArgumentException("controllerClass can not be null");
			}
			if (StrUtil.isBlank(viewPath)) {
				viewPath = "/";
			} else {
				this.produces = RequestMapping.Produces.General;
			}
			this.urlPath = urlPath;
			this.requestMethods = requestMethods;
			this.controllerMethod = controllerMethod;
			this.controllerClass = controllerClass;
			this.viewPath = processViewPath(viewPath);
		}

		private String processControllerKey(String controllerKey) {
			controllerKey = controllerKey.trim();
			if (!controllerKey.startsWith("/")) {
				controllerKey = "/" + controllerKey;
			}
			if (controllerKeySet.contains(controllerKey)) {
				throw new IllegalArgumentException("controllerKey already exists: " + controllerKey);
			}
			controllerKeySet.add(controllerKey);
			return controllerKey;
		}

		private String processViewPath(String viewPath) {
			viewPath = viewPath.trim();
			if (!viewPath.startsWith("/")) {			
				viewPath = "/" + viewPath;
			}
			if (!viewPath.endsWith("/")) {				
				viewPath = viewPath + "/";
			}
			return viewPath;
		}

		String getControllerKey() {
			return controllerKey;
		}

		Class<? extends Controller> getControllerClass() {
			return controllerClass;
		}

		String getControllerMethod() { return controllerMethod; }

		String getUrlPath() { return urlPath; }

		RequestMethod[] getRequestMethods() { return requestMethods; }

		String getFinalViewPath(String baseViewPath) {
			return baseViewPath != null ? baseViewPath + viewPath : viewPath;
		}

		RequestMapping.Produces getProduces() { return produces; }
	}
}

