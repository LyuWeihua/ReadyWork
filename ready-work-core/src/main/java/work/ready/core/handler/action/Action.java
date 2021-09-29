/**
 * Copyright (c) 2011-2019, James Zhan 詹波 (jfinal@126.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package work.ready.core.handler.action;

import work.ready.core.aop.Interceptor;
import work.ready.core.handler.Controller;
import work.ready.core.handler.action.paramgetter.ParamProcessor;
import work.ready.core.handler.action.paramgetter.ParamProcessorBuilder;
import work.ready.core.handler.route.RequestMapping;

import java.lang.reflect.Method;

public class Action {

	private final Class<? extends Controller> controllerClass;
	private final String controllerKey;
	private final String actionKey;
	private final Method method;
	private final String methodName;
	private final Interceptor[] interceptors;
	RequestMapping.Produces produces;
	private final String viewPath;

	private final ParamProcessor parameterGetter;

	public Action(String controllerKey, String actionKey, Class<? extends Controller> controllerClass, Method method, String methodName, Interceptor[] interceptors, RequestMapping.Produces produces, String viewPath) {
		this.controllerKey = controllerKey;
		this.actionKey = actionKey;
		this.controllerClass = controllerClass;
		this.method = method;
		this.methodName = methodName;
		this.interceptors = interceptors;
		this.produces = produces;
		this.viewPath = viewPath;

		this.parameterGetter = ParamProcessorBuilder.getInstance().build(controllerClass, method);
	}

	public Class<? extends Controller> getControllerClass() {
		return controllerClass;
	}

	public String getControllerKey() {
		return controllerKey;
	}

	public String getActionKey() {
		return actionKey;
	}

	public Method getMethod() {
		return method;
	}

	public Interceptor[] getInterceptors() {
		return interceptors;
	}

	public RequestMapping.Produces getProduces() {
		return produces;
	}

	public String getViewPath() {
		return viewPath;
	}

	public String getMethodName() {
		return methodName;
	}

	public ParamProcessor getParameterGetter() {
		return parameterGetter;
	}
}

