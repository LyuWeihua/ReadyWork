/**
 *
 * Original work Copyright jfinal-event L.cm
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

package work.ready.core.event;

import work.ready.core.config.ConfigInjector;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;
import work.ready.core.tools.EvalUtil;
import work.ready.core.tools.define.CheckedConsumer;
import work.ready.core.tools.define.Kv;
import work.ready.core.tools.StrUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class ApplicationListenerAdapter implements ApplicationListener<Object> {
	private static final Log logger = LogFactory.getLog(ApplicationListenerAdapter.class);

	private final Object listener;
	private final Method method;
	private final int paramCount;
	private final Class<?> paramType;
	private final Class<?> targetClass;
	private final List<Class<?>> declaredEventClasses;
	private final String[] name;
	private final String condition;
	private final Function<Object, Boolean> filter;
	private final Object contextReference;
	private final CheckedConsumer<GeneralEvent, Exception> handler;
	private final int order;
	private final boolean async;
	private final boolean global;

	public ApplicationListenerAdapter(EventManager.ListenerSetter setter, CheckedConsumer<GeneralEvent, Exception> handler) {
		this.listener = null;
		this.method = null;
		this.paramCount = 1;
		this.paramType = GeneralEvent.class;
		this.targetClass = null;

		this.declaredEventClasses = ApplicationListenerAdapter.join(setter.value(), setter.events());
		this.name = setter.name();
		if(!this.declaredEventClasses.contains(GeneralEvent.class) && this.name.length > 0) {
			this.declaredEventClasses.add(GeneralEvent.class);
		}
		this.condition = ConfigInjector.getStringValue(setter.condition());
		this.filter = setter.filter();
		this.contextReference = setter.getContextReference();
		this.handler = handler;
		this.order = setter.order();
		this.async = setter.async();
		this.global = setter.global();
	}

	public ApplicationListenerAdapter(Object listener, Method method, EventManager.ListenerSetter setter) {
		this.listener = listener;
		this.method = method;
		this.paramCount = method.getParameterCount();
		this.paramType = this.paramCount > 0 ? method.getParameterTypes()[0] : null;
		this.targetClass = method.getDeclaringClass();

		this.declaredEventClasses = ApplicationListenerAdapter.join(setter.value(), setter.events());
		this.name = setter.name();
		if(!this.declaredEventClasses.contains(GeneralEvent.class) && this.name.length > 0) {
			this.declaredEventClasses.add(GeneralEvent.class);
		}
		this.condition = ConfigInjector.getStringValue(setter.condition());
		this.filter = setter.filter();
		this.contextReference = setter.getContextReference();
		this.handler = null;
		this.order = setter.order();
		this.async = setter.async();
		this.global = setter.global();
	}

	public ApplicationListenerAdapter(Object listener, Method method) {
		this.listener = listener;
		this.method = method;
		this.paramCount = method.getParameterCount();
		this.paramType = this.paramCount > 0 ? method.getParameterTypes()[0] : null;
		this.targetClass = method.getDeclaringClass();

		EventListener listenerAnnotation = method.getAnnotation(EventListener.class);
		
		this.declaredEventClasses = ApplicationListenerAdapter.join(listenerAnnotation.value(), listenerAnnotation.events());
		this.name = listenerAnnotation.name();
		if(!this.declaredEventClasses.contains(GeneralEvent.class) && this.name.length > 0) {
			this.declaredEventClasses.add(GeneralEvent.class);
		}
		this.condition = ConfigInjector.getStringValue(listenerAnnotation.condition());
		this.filter = null;
		this.contextReference = null;
		this.handler = null;
		this.order = listenerAnnotation.order();
		this.async = listenerAnnotation.async();
		this.global = listenerAnnotation.global();
	}

	public ApplicationListenerAdapter(Method method) {
		this.listener = null;
		this.method = method;
		this.paramCount = method.getParameterCount();
		this.paramType = this.paramCount > 0 ? method.getParameterTypes()[0] : null;
		this.targetClass = method.getDeclaringClass();

		EventListener listenerAnnotation = method.getAnnotation(EventListener.class);
		
		this.declaredEventClasses = ApplicationListenerAdapter.join(listenerAnnotation.value(), listenerAnnotation.events());
		this.name = listenerAnnotation.name();
		if(!this.declaredEventClasses.contains(GeneralEvent.class) && this.name.length > 0) {
			this.declaredEventClasses.add(GeneralEvent.class);
		}
		this.condition = ConfigInjector.getStringValue(listenerAnnotation.condition());
		this.filter = null;
		this.contextReference = null;
		this.handler = null;
		this.order = listenerAnnotation.order();
		this.async = listenerAnnotation.async();
		this.global = listenerAnnotation.global();
	}

	@Override
	public void onApplicationEvent(Object event) {
		if(filter != null) {
			if(!filter.apply(event)) return;
		}
		
		if(this.contextReference != null && (event instanceof GeneralEvent) && ((GeneralEvent)event).getContextReference() != null){
			if(!((GeneralEvent)event).getContextReference().equals(this.contextReference)) return;
		}
		
		if (StrUtil.notBlank(this.condition)) {
			boolean elPass = EvalUtil.eval(this.condition, Kv.by("event", event));
			if(logger.isDebugEnabled())
			logger.debug("method:[" + this.method + "]-condition:[" + this.condition + "]-result:[" + elPass + "]");
			if (!elPass) {
				return;
			}
		}
		try {
			Object bean = null;
			if(listener != null) {
				bean = listener;
			} else {
				if(targetClass == null && handler != null) {
					handler.accept((GeneralEvent)event);
					return;
				} else {
					bean = Ready.beanManager().get(this.targetClass);
				}
			}
			if(bean == null) {
				throw new RuntimeException("Event listener bean " + targetClass.getCanonicalName() + "(method: " + method.getName() + ") initialize failed.");
			}
			Object[] args;
			
			if (this.paramCount == 0) {
				args = new Object[0];
			} else {
				args = new Object[]{event};
			}
			this.method.invoke(bean, args);
		} catch (Exception e) {
			handleException(e);
		}
	}

	private void handleException(Exception e) {
		if (e instanceof IllegalAccessException || e instanceof IllegalArgumentException
			|| e instanceof NoSuchMethodException) {
			throw new IllegalArgumentException(e);
		} else if (e instanceof InvocationTargetException) {
			throw new RuntimeException(((InvocationTargetException) e).getTargetException());
		} else if (e instanceof RuntimeException) {
			throw (RuntimeException) e;
		} else {
			throw new RuntimeException(e);
		}
	}

	public Method getMethod() {
		return method;
	}

	public int getParamCount() {
		return paramCount;
	}

	public Class<?> getParamType() {
		return paramType;
	}

	public Class<?> getTargetClass() {
		return targetClass;
	}

	public List<Class<?>> getDeclaredEventClasses() {
		return declaredEventClasses;
	}

	public String[] getName(){ return name; }

	public String getCondition() {
		return condition;
	}

	public int getOrder() {
		return order;
	}

	public boolean isAsync() {
		return async;
	}

	public boolean isGlobal() {
		return global;
	}

	@Override
	public String toString() {
		return "@EventListener [" + method + "]";
	}

	private static List<Class<?>> join(Class<?>[] first, Class<?>[] second) {
		Class<?>[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return new ArrayList<>(Arrays.asList(result));
	}
}
