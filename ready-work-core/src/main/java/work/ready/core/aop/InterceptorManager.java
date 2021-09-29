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

import work.ready.core.aop.annotation.Before;
import work.ready.core.aop.annotation.Clear;
import work.ready.core.aop.annotation.GlobalInterceptor;
import work.ready.core.config.ConfigInjector;
import work.ready.core.event.Event;
import work.ready.core.event.GeneralEvent;
import work.ready.core.handler.Controller;
import work.ready.core.ioc.BeanManager;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.module.CoreContext;
import work.ready.core.module.Initializer;
import work.ready.core.server.Ready;
import work.ready.core.tools.EvalUtil;
import work.ready.core.tools.define.Kv;
import work.ready.core.tools.StrUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InterceptorManager {
	private static final Log logger = LogFactory.getLog(InterceptorManager.class);
	private InterceptorConfig config;
	private final CoreContext context;
	protected List<Initializer<InterceptorManager>> initializers = new ArrayList<>();

	private final ConcurrentHashMap<Class<?>, Interceptor[]> serviceClassInterceptors = new ConcurrentHashMap<Class<?>, Interceptor[]>(32, 0.5F);

	public InterceptorManager(CoreContext context) {
		this.context = context;
		config = context.getBeanManager().get(InterceptorConfig.class);
		Ready.post(new GeneralEvent(Event.INTERCEPTOR_MANAGER_CREATE, this));
	}

	public void addInitializer(Initializer<InterceptorManager> initializer) {
		this.initializers.add(initializer);
		initializers.sort(Comparator.comparing(Initializer::order));
	}

	public void startInit() {
		try {
			for (Initializer<InterceptorManager> i : initializers) {
				i.startInit(this);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void endInit() {
		try {
			for (Initializer<InterceptorManager> i : initializers) {
				i.endInit(this);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void addAopComponent(AopComponent component){
		config.addAopComponent(component);
	}

	public List<AopComponent> getAopComponents(){
		return config.getAopComponents();
	}

	public Interceptor[] createControllerInterceptor(Class<? extends Controller> controllerClass) {
		return createInterceptor(getClassAnnotations(controllerClass));
	}

	public Interceptor[] createServiceInterceptor(Class<?> serviceClass) {
		Interceptor[] result = serviceClassInterceptors.get(serviceClass);
		if (result == null) {
			result = createInterceptor(getClassAnnotations(serviceClass));
			serviceClassInterceptors.put(serviceClass, result);
		}
		return result;
	}

	private Annotation[] getClassAnnotations(Class<?> clazz){
		List<Annotation> annotationList = new ArrayList<>();
		annotationList.addAll(Arrays.asList(clazz.getAnnotation(Before.class)));
		for(AopComponent component : getAopComponents()){
			if(clazz.isAnnotationPresent(component.getAnnotation())){
				annotationList.add(clazz.getAnnotation(component.getAnnotation()));
			}
		}
		return annotationList.toArray(new Annotation[annotationList.size()]);
	}

	private Annotation[] getMethodAnnotations(Method method){
		List<Annotation> annotationList = new ArrayList<>();
		annotationList.addAll(Arrays.asList(method.getAnnotation(Before.class)));
		for(AopComponent component : getAopComponents()){
			if(method.isAnnotationPresent(component.getAnnotation())){
				annotationList.add(method.getAnnotation(component.getAnnotation()));
			}
		}
		return annotationList.toArray(new Annotation[annotationList.size()]);
	}

	public Interceptor[] buildControllerActionInterceptor(Interceptor[] routesInterceptors, Interceptor[] classInterceptors, Class<? extends Controller> controllerClass, Method method) {
		return doBuild(config.getGlobalActionInterceptors(), routesInterceptors, classInterceptors, controllerClass, method);
	}

	public Interceptor[] buildServiceMethodInterceptor(Class<?> serviceClass, Method method) {
		return doBuild(config.getGlobalServiceInterceptors(), config.NULL_INTERS, createServiceInterceptor(serviceClass), serviceClass, method);
	}

	private Interceptor[] doBuild(Interceptor[] globalInterceptors, Interceptor[] routesInterceptors, Interceptor[] classInterceptors, Class<?> targetClass, Method method) {
		Interceptor[] methodInterceptors = createInterceptor(getMethodAnnotations(method));

		Class<? extends Interceptor>[] clearInterceptorsOnMethod;
		Clear clearOnMethod = method.getAnnotation(Clear.class);
		if (clearOnMethod != null) {
			clearInterceptorsOnMethod = clearOnMethod.value();
			if (clearInterceptorsOnMethod.length == 0) {	
				return methodInterceptors;
			}
		} else {
			clearInterceptorsOnMethod = null;
		}

		Class<? extends Interceptor>[] clearIntersOnClass;
		Clear clearOnClass = targetClass.getAnnotation(Clear.class);
		if (clearOnClass != null) {
			clearIntersOnClass = clearOnClass.value();
			if (clearIntersOnClass.length == 0) {	
				globalInterceptors = InterceptorConfig.NULL_INTERS;
				routesInterceptors = InterceptorConfig.NULL_INTERS;
			}
		} else {
			clearIntersOnClass = null;
		}

		ArrayList<Interceptor> result = new ArrayList<Interceptor>(globalInterceptors.length + routesInterceptors.length + classInterceptors.length + methodInterceptors.length);
		for (Interceptor inter : globalInterceptors) {

			var annotation  = inter.getClass().getAnnotation(GlobalInterceptor.class);
			if(annotation == null) continue;
			String match = ConfigInjector.getStringValue(annotation.match());
			if (StrUtil.notBlank(match)) {
				boolean elPass = EvalUtil.eval(match, Kv.by("name", targetClass.getCanonicalName()));
				if(logger.isDebugEnabled())
					logger.debug("GlobalInterceptor [" + inter.getClass().getCanonicalName() + "] on " + targetClass.getCanonicalName() + ": match=[" + match + "] result=[" + elPass + "]");
				if (elPass) {
					result.add(inter);
				}
			}
		}
		for (Interceptor inter : routesInterceptors) {
			result.add(inter);
		}
		if (clearIntersOnClass != null && clearIntersOnClass.length > 0) {
			removeInterceptor(result, clearIntersOnClass);
		}
		for (Interceptor inter : classInterceptors) {
			result.add(inter);
		}
		if (clearInterceptorsOnMethod != null && clearInterceptorsOnMethod.length > 0) {
			removeInterceptor(result, clearInterceptorsOnMethod);
		}
		for (Interceptor inter : methodInterceptors) {
			result.add(inter);
		}
		return result.toArray(new Interceptor[result.size()]);
	}

	private void removeInterceptor(ArrayList<Interceptor> target, Class<? extends Interceptor>[] clearInterceptors) {
		for (Iterator<Interceptor> it = target.iterator(); it.hasNext();) {
			Interceptor currentInterceptor = it.next();
			if (currentInterceptor != null) {
				Class<? extends Interceptor> currentInterceptorClass = currentInterceptor.getClass();
				for (Class<? extends Interceptor> ci : clearInterceptors) {
					if (currentInterceptorClass == ci) {
						it.remove();
						break;
					}
				}
			} else {
				it.remove();
			}
		}
	}

	public Interceptor[] createInterceptor(Annotation... annotations) {
		if (annotations == null || annotations.length == 0) {
			return InterceptorConfig.NULL_INTERS;
		}
		List<Class<? extends Interceptor>> interceptorList = new ArrayList<>();
		for(int i = 0; i < annotations.length; i++){
			if(annotations[i] == null) continue;
			if(annotations[i].annotationType().equals(Before.class)){
				var classArray = ((Before)annotations[i]).value();
				if(classArray != null) interceptorList.addAll(Arrays.asList(classArray));
			}
			for(AopComponent component : getAopComponents()){
				if(annotations[i].annotationType().equals(component.getAnnotation())){
					interceptorList.add(component.getInterceptorClass());
				}
			}
		}
		return createInterceptor(interceptorList.toArray(new Class[interceptorList.size()]));
	}

	public Interceptor[] createInterceptor(Class<? extends Interceptor>[] interceptorClasses) {
		if (interceptorClasses == null || interceptorClasses.length == 0) {
			return InterceptorConfig.NULL_INTERS;
		}

		Interceptor[] result = new Interceptor[interceptorClasses.length];
		try {
			for (int i=0; i<result.length; i++) {
				result[i] = config.getSingletonMap(interceptorClasses[i]);
				if (result[i] == null) {
					result[i] = (Interceptor)interceptorClasses[i].getDeclaredConstructor().newInstance();
					for(AopComponent component : getAopComponents()){
						if(interceptorClasses[i].equals(component.getInterceptorClass())){
							BeanManager.injectProperties(result[i], component.getInjectProperties());
						}
					}
					context.getBeanManager().inject(result[i]);
					config.setSingletonMap(interceptorClasses[i], result[i]);
				}
			}
			return result;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void addGlobalActionInterceptor(Interceptor... interceptors) {
		config.addGlobalActionInterceptor(interceptors);
	}

	public void addGlobalServiceInterceptor(Interceptor... interceptors) {
		config.addGlobalServiceInterceptor(interceptors);
	}

	public java.util.List<Class<?>> getGlobalServiceInterceptorClasses() {
		ArrayList<Class<?>> ret;
		if(config != null) {
			ret = new ArrayList<>(config.getGlobalServiceInterceptors().length + 3);
			for (Interceptor i : config.getGlobalServiceInterceptors()) {
				ret.add(i.getClass());
			}
		} else {
			ret = new ArrayList<>();
		}
		return ret;
	}
}

