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

package work.ready.core.component.proxy;

import work.ready.core.aop.Interceptor;
import work.ready.core.module.CoreContext;

import java.lang.reflect.Method;

public class ProxyMethod {

	protected CoreContext context;
	private Long key;

	private Class<?> targetClass;
	private Class<?> proxyClass;
	private Method method;
	private Interceptor[] interceptors = null;

	public ProxyMethod(CoreContext context){
		this.context = context;
	}

	public void setKey(long key) {
		this.key = key;
	}

	public Long getKey() {
		return key;
	}

	public void setTargetClass(Class<?> targetClass) {
		this.targetClass = targetClass;
	}

	public Class<?> getTargetClass() {
		return targetClass;
	}

	public void setProxyClass(Class<?> proxyClass) {
		this.proxyClass = proxyClass;
	}

	public Class<?> getProxyClass() {
		return proxyClass;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public Method getMethod() {
		return method;
	}

	public Interceptor[] getInterceptors() {
		if (interceptors == null) {
			Interceptor[] ret = context.getInterceptorManager().buildServiceMethodInterceptor(targetClass, method);
			interceptors = ret;
		}
		return interceptors;
	}
}

