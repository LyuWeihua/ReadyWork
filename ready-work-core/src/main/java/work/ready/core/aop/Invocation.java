/**
 *
 * Original work (c) 2011-2019, James Zhan 詹波 (jfinal@126.com).
 * Modified work Copyright (c) 2020 WeiHua Lyu [ready.work]
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

package work.ready.core.aop;

import work.ready.core.handler.action.Action;
import work.ready.core.handler.Controller;
import work.ready.core.component.proxy.Callback;
import work.ready.core.component.proxy.ProxyMethod;
import work.ready.core.server.Ready;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static work.ready.core.security.data.DataSecurityInspector.auditClassKey;
import static work.ready.core.security.data.DataSecurityInspector.auditMethodKey;
import static work.ready.core.tools.ClassUtil.getMethodSignature;

@SuppressWarnings("unchecked")
public class Invocation {

	private static final Object[] NULL_ARGS = new Object[0];

	private Action action;
	private Object target;
	private Method method;
	private Object[] args;
	private Callback callback;
	private Interceptor[] interceptors;
	private Object returnValue;

	private int index = 0;

	public Invocation(Object target, Long proxyMethodKey, Callback callback, Object... args) {
		this.action = null;
		this.target = target;

		ProxyMethod proxyMethod = Ready.proxyManager().getProxyMethodCache().get(proxyMethodKey);
		this.method = proxyMethod.getMethod();
		this.interceptors = proxyMethod.getInterceptors();

		this.callback = callback;
		this.args = args;
	}

	public Invocation(Object target, Long proxyMethodKey, Callback callback) {
		this(target, proxyMethodKey, callback, NULL_ARGS);
	}

	public Invocation(Object target, Method method, Interceptor[] interceptors, Callback callback, Object[] args) {
		this.action = null;
		this.target = target;

		this.method = method;
		this.interceptors = interceptors;

		this.callback = callback;
		this.args = args;
	}

	protected Invocation() {
		this.action = null;
	}

	public Invocation(Action action, Controller controller) {
		this.action = action;
		this.interceptors = action.getInterceptors();
		this.target = controller;

		controller.setAttr(auditClassKey, controller.getClass().getCanonicalName());
		controller.setAttr(auditMethodKey, getMethodSignature(action.getMethod()));

		this.args = action.getParameterGetter().get(action, controller);
	}

	public Object invoke() {
		if (index < interceptors.length) {
			try {
				interceptors[index++].intercept(this);
			} catch (InvocationTargetException e) {
				Throwable t = e.getTargetException();
				if (t == null) {t = e;}
				throw t instanceof RuntimeException ? (RuntimeException)t : new RuntimeException(t);
			} catch (RuntimeException e) {
				throw e;
			}
			catch (Throwable t) {
				throw new RuntimeException(t);
			}
		}
		else if (index++ == interceptors.length) {	
			try {

				if (action != null) {
					returnValue = action.getMethod().invoke(target, args);
				}

				else {
					returnValue = callback.call(args);
				}
			}
			catch (InvocationTargetException e) {
				Throwable t = e.getTargetException();
				if (t == null) {t = e;}
				throw t instanceof RuntimeException ? (RuntimeException)t : new RuntimeException(t);
			}
			catch (RuntimeException e) {
				throw e;
			}
			catch (Throwable t) {
				throw new RuntimeException(t);
			}
		}
		return returnValue;
	}

	public Object getArg(int index) {
		if (index >= args.length)
			throw new ArrayIndexOutOfBoundsException();
		return args[index];
	}

	public void setArg(int index, Object value) {
		if (index >= args.length)
			throw new ArrayIndexOutOfBoundsException();
		args[index] = value;
	}

	public Object[] getArgs() {
		return args;
	}

	public <T> T getTarget() {
		return (T)target;
	}

	public Method getMethod() {
		if (action != null)
			return action.getMethod();
		return method;
	}

	public String getMethodName() {
		if (action != null)
			return action.getMethodName();
		return method.getName();
	}

	public <T> T getReturnValue() {
		return (T)returnValue;
	}

	public void setReturnValue(Object returnValue) {
		this.returnValue = returnValue;
	}

	public Controller getController() {
		if (action == null)
			throw new RuntimeException("This method can only be used for action interception");
		return (Controller)target;
	}

	public String getActionKey() {
		if (action == null)
			throw new RuntimeException("This method can only be used for action interception");
		return action.getActionKey();
	}

	public String getControllerKey() {
		if (action == null)
			throw new RuntimeException("This method can only be used for action interception");
		return action.getControllerKey();
	}

	public String getViewPath() {
		if (action == null)
			throw new RuntimeException("This method can only be used for action interception");
		return action.getViewPath();
	}

	public boolean isActionInvocation() {
		return action != null;
	}
}
