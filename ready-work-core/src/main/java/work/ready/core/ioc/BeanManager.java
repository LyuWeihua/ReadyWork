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
package work.ready.core.ioc;

import work.ready.core.aop.Interceptor;
import work.ready.core.config.ConfigInjector;
import work.ready.core.config.annotation.ConfigurationProperties;
import work.ready.core.database.Model;
import work.ready.core.event.ApplicationListener;
import work.ready.core.event.Event;
import work.ready.core.event.GeneralEvent;
import work.ready.core.handler.HandlerManager;
import work.ready.core.handler.route.RouteManager;
import work.ready.core.handler.websocket.WebSocketManager;
import work.ready.core.ioc.annotation.*;
import work.ready.core.ioc.aware.*;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.module.*;
import work.ready.core.render.RenderManager;
import work.ready.core.security.CurrentUser;
import work.ready.core.security.UserIdentity;
import work.ready.core.security.SecurityManager;
import work.ready.core.server.Ready;
import work.ready.core.server.WebServer;
import work.ready.core.service.BusinessService;
import work.ready.core.database.ModelService;
import work.ready.core.tools.ClassUtil;
import work.ready.core.tools.define.ConcurrentMultiMap;
import work.ready.core.tools.define.Kv;
import work.ready.core.tools.StrUtil;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static work.ready.core.tools.StrUtil.format;

public class BeanManager {

	private static final Log logger = LogFactory.getLog(BeanManager.class);

	private final static Class[] DEFAULT_EXCLUDES_MAPPING_CLASSES = new Class[]{
			Object.class,
			String.class,
			Integer.class,
			Boolean.class,
			Byte.class,
			Long.class,
			Short.class,
			Float.class,
			Double.class,
			Character.class,
			List.class,
			Map.class,
			Set.class,
			int.class,
			boolean.class,
			byte.class,
			long.class,
			short.class,
			float.class,
			double.class,
			char.class,
			void.class,
			Void.class,
			BigDecimal.class,
			BigInteger.class,
			java.util.Date.class,
			java.sql.Date.class,
			java.sql.Timestamp.class,
			java.sql.Time.class,
			Serializable.class,
			ApplicationListener.class
	};

	protected ConcurrentHashMap<Class<?>, Object> singletonCache = new ConcurrentHashMap<>();

	protected ConcurrentMultiMap<Class<?>, Field> injectCache = new ConcurrentMultiMap<>();

	protected static final ThreadLocal<HashMap<Class<?>, Object>> singletonTl = ThreadLocal.withInitial(HashMap::new);
	protected static final ThreadLocal<HashMap<Class<?>, Object>> prototypeTl = ThreadLocal.withInitial(HashMap::new);

	protected static final ThreadLocal<CurrentUser<? extends UserIdentity>> currentUser = new InheritableThreadLocal<>();
	protected static final ThreadLocal<Application> currentApplication = new InheritableThreadLocal<>();

	protected ConcurrentHashMap<String, HashSet<Class<?>>> mapping = new ConcurrentHashMap<>(256, 0.25F);
	protected HashMap<String, Method> configurationMapping = new HashMap<>(128, 0.25F);

	protected List<BeanConfig> beanConfig = new ArrayList<>();
	protected List<Initializer<BeanManager>> initializers = new ArrayList<>();

	protected boolean singleton = true;

	private final CoreContext context;
	private final List<Object[]> beanDestroyHolder = new ArrayList<>();

	public BeanManager(CoreContext context){
		this.context = context;
		Ready.shutdownHook.add(ShutdownHook.STAGE_5, timeout -> beanDestroyHook());
		Ready.post(new GeneralEvent(Event.BEAN_MANAGER_CREATE, this));
	}

	public CurrentUser<? extends UserIdentity> getCurrentUser() {
		return BeanManager.currentUser.get();
	}

	public void setCurrentUser(CurrentUser<? extends UserIdentity> currentUser) {
		BeanManager.currentUser.set(currentUser);
	}

	public void setCurrentApplication(Application app) { BeanManager.currentApplication.set(app); }

	public Application getCurrentApplication() { return BeanManager.currentApplication.get(); }

	private void beanDestroyHook(){
		Collections.reverse(beanDestroyHolder);
		beanDestroyHolder.forEach(kv -> {
			try {
				((Method) kv[0]).invoke(kv[1]);
			} catch (IllegalAccessException | InvocationTargetException e){
				logger.error(e,"Destroy method " + kv[1].getClass().getName() + "." + ((Method) kv[0]).getName() + " failed to execute while server shutting down");
			}
		});
	}

	public void addBeanConfig(BeanConfig beanConfig){
		this.beanConfig.add(beanConfig);
	}

	public void addInitializer(Initializer<BeanManager> initializer) {
		this.initializers.add(initializer);
		initializers.sort(Comparator.comparing(Initializer::order));
	}

	public void startInit() {
		try {
			for (Initializer<BeanManager> i : initializers) {
				i.startInit(this);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void endInit() {
		try {
			for (Initializer<BeanManager> i : initializers) {
				i.endInit(this);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void initAnnotationMapping(Class<?> clazz, Class<? extends Annotation> annotation){
		String named = null;
		if (Component.class.equals(annotation))
			named = ConfigInjector.getStringValue(clazz.getAnnotation(Component.class).value());
		else if (Service.class.equals(annotation))
			named = ConfigInjector.getStringValue(clazz.getAnnotation(Service.class).value());
		else if (Configuration.class.equals(annotation))
			named = ConfigInjector.getStringValue(clazz.getAnnotation(Configuration.class).value());
		else throw new RuntimeException("unsupported annotation type: " + annotation.getCanonicalName());
		if (StrUtil.notBlank(named)) {
			addMapping(named, clazz, false);
		} else {
			Class<?>[] interfaceClasses = clazz.getInterfaces();
			var superClassList = getSuperClass(clazz);
			superClassList.forEach(superClass -> addMapping((Class) superClass, clazz, false));
			if (interfaceClasses != null && interfaceClasses.length > 0) {
				for (Class interfaceClass : interfaceClasses) {
					if (!inExcludes(interfaceClass, DEFAULT_EXCLUDES_MAPPING_CLASSES)) {
						addMapping(interfaceClass, clazz, false);
					}
				}
			}
			addMapping(clazz, false);
		}
	}

	public void initConfigurationMapping(Class<?> clazz){
		Method[] methods = clazz.getMethods();
		for (Method method : methods) {
			Bean bean = method.getAnnotation(Bean.class);
			if (bean == null) {
				continue;
			}
			int parameterCount = method.getParameterCount();
			if (parameterCount > 0) {
				throw new RuntimeException("Bean configuration methods cannot have parameters : " + clazz.getCanonicalName() + " => " + method.toString());
			}
			Class<?> classType = method.getReturnType();
			String[] named = null;
			if (bean.name().length > 0) {
				named = ConfigInjector.getStringValue(bean.name());
			} else if (bean.value().length > 0) {
				named = ConfigInjector.getStringValue(bean.value());
			}
			if (named != null) {
				for (String name : named) {
					if (StrUtil.notBlank(name)) addMapping(name, classType, true);
				}
			}
			if (!inExcludes(classType, DEFAULT_EXCLUDES_MAPPING_CLASSES)) {
				if (mapping.containsKey(classType.getCanonicalName())) {
					mapping.get(classType.getCanonicalName()).clear();
				}
				addMapping(classType, true);
				addConfigurationMapping(classType.getCanonicalName(), method);
			}
		}
	}

	private  <T> List<Class<? super T>> getSuperClass(Class<T> clazz){
		List<Class<? super T>> classes = new ArrayList<>();
		Class<? super T> superclass = clazz.getSuperclass();
		while(superclass != null && !inExcludes(superclass, DEFAULT_EXCLUDES_MAPPING_CLASSES)){
			classes.add(superclass);
			superclass = superclass.getSuperclass();
		}
		return classes;
	}

	private boolean inExcludes(Class interfaceClass, Class[] excludes) {
		for (Class ex : excludes) {
			if (ex.equals(interfaceClass)) {
				return true;
			}
		}
		return false;
	}

	private <T> void targetClassVerify(Class<T> targetClass){
		if(Ready.getBootstrapConfig().isDevMode()){	
			if(!targetClass.equals(ClassUtil.getUserClass(targetClass))){
				throw new RuntimeException(targetClass.getCanonicalName() + " is not a valid class for IOC");
			}
			Class<?>[] specialClass = new Class[]{ApplicationContext.class, HandlerManager.class, SecurityManager.class,
					RouteManager.class, RenderManager.class, WebSocketManager.class, WebServer.class, Application.class};
			if(Ready.isMultiAppMode() && Arrays.asList(specialClass).contains(targetClass)){
				throw new RuntimeException(targetClass.getCanonicalName() + " is a special class, IOC is not supported it in Multi-App Mode, " +
						"please use Aware interface to get instance instead");
			}
		}
	}

	private <T> T awareEnhance(T bean) {
		if(bean instanceof Aware) {
			if (bean instanceof ApplicationAware) {
				((ApplicationAware) bean).setApplication(getCurrentApplication());
			}
			if (bean instanceof HandlerManagerAware) {
				if (getCurrentApplication() != null) {
					((HandlerManagerAware) bean).setHandlerManager(getCurrentApplication().handler());
				}
			}
			if (bean instanceof RouteManagerAware) {
				if (getCurrentApplication() != null) {
					((RouteManagerAware) bean).setRouteManager(getCurrentApplication().route());
				}
			}
			if (bean instanceof WebSocketManagerAware) {
				if (getCurrentApplication() != null) {
					((WebSocketManagerAware) bean).setWebSocketManager(getCurrentApplication().webSocket());
				}
			}
			if (bean instanceof DatabaseManagerAware) {
				((DatabaseManagerAware) bean).setDatabaseManager(Ready.dbManager());
			}
		}
		return bean;
	}

	public <T> T get(Class<T> targetClass, Class<? extends T> defaultImplement) {
		try {
			T instance = doGet(targetClass, false, null);
			if(instance == null) {
				instance = doGet(defaultImplement, true, null);
				if(instance != null) {
					addMapping(targetClass, defaultImplement, true);
				}
			}
			return instance;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	public <T> T get(Class<T> targetClass) {
		try {
			return doGet(targetClass, false, null);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	public <T> T get(Class<T> targetClass, boolean required, String qualifier, ScopeType scopeType){
		try {
			return doGet(targetClass, required, qualifier, scopeType);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	protected <T> T doGet(Class<T> targetClass, boolean required, String qualifier) throws ReflectiveOperationException {
		return doGet(targetClass, required, qualifier, null);
	}

	@SuppressWarnings("unchecked")
	protected <T> T doGet(Class<T> targetClass, boolean required, String qualifier, ScopeType scopeType) throws ReflectiveOperationException {

		targetClassVerify(targetClass);
		boolean isSingleton = singleton;

		targetClass = (Class<T>)getMappingClass(targetClass, required, qualifier);
		if(targetClass == null) {
			return null; 
		}
		if(scopeType == null) {
			Scope scope = targetClass.getAnnotation(Scope.class);

			if (scope == null) {
				Method beanMethod = configurationMapping.get(targetClass.getCanonicalName());
				if (beanMethod != null) {
					if (beanMethod.isAnnotationPresent(Scope.class))
						scope = beanMethod.getAnnotation(Scope.class);
				}
			}
			if (scope != null) {
				isSingleton = !ScopeType.prototype.equals(scope.value());
			}
		} else {
			isSingleton = !ScopeType.prototype.equals(scopeType);
		}

		if (isSingleton) {
			return awareEnhance(doGetSingleton(targetClass));
		} else {
			return awareEnhance(doGetPrototype(targetClass));
		}
	}

	@SuppressWarnings("unchecked")
	protected <T> T doGetSingleton(Class<T> targetClass) throws ReflectiveOperationException {
		Object ret = singletonCache.get(targetClass);
		if (ret != null) {
			return (T)ret;
		}

		HashMap<Class<?>, Object> map = singletonTl.get();
		int size = map.size();
		if (size > 0) {
			ret = map.get(targetClass);
			if (ret != null) {
				return (T)ret;
			}
		}

		synchronized (this) {
			try {
				ret = singletonCache.get(targetClass);
				if (ret == null) {
					ret = createObject(targetClass);
					map.put(targetClass, ret);
					doInject(targetClass, ret);
					Method beanMethod = configurationMapping.get(targetClass.getCanonicalName());
					if(beanMethod != null){
						initConfiguredBean(beanMethod, ret);
					}
					singletonCache.put(targetClass, ret);
				}
				return (T)ret;
			} finally {
				if (size == 0) {  
					singletonTl.remove();
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	protected <T> T doGetPrototype(Class<T> targetClass) throws ReflectiveOperationException {
		Object ret;

		HashMap<Class<?>, Object> map = prototypeTl.get();
		int size = map.size();
		if (size > 0) {
			ret = map.get(targetClass);
			if (ret != null) {
				throw new RuntimeException("Nested exception : Error creating bean with class '" + targetClass.getCanonicalName() + "': Requested bean is currently in creation: Is there an unresolvable circular reference?");
			}
		}

		try {
			ret = createObject(targetClass);
			map.put(targetClass, ret);
			doInject(targetClass, ret);
			Method beanMethod = configurationMapping.get(targetClass.getCanonicalName());
			if(beanMethod != null){
				initConfiguredBean(beanMethod, ret);
			}
			map.remove(targetClass);  
			return (T)ret;
		} finally {
			if (size == 0) { 
				map.clear();
			}
		}
	}

	public <T> T inject(T targetObject) {
		try {
			doInject(targetObject.getClass(), targetObject);
			return targetObject;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	public <T> T inject(Class<T> targetClass, T targetObject) {
		try {
			doInject(targetClass, targetObject);
			return targetObject;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	protected void doInject(Class<?> targetClass, Object targetObject) throws ReflectiveOperationException {
		targetClass = ClassUtil.getUserClass(targetClass);
		synchronized (targetClass) {
			Field[] fields;
			List<Field> fieldList = injectCache.get(targetClass);
			if (fieldList != null) {
				fields = fieldList.toArray(new Field[]{});
			} else {
				injectCache.putAll(targetClass, new ArrayList<>());  
				fields = targetClass.getDeclaredFields();
			}
			if (fields.length != 0) {
				for (Field field : fields) {
					if (Modifier.isFinal(field.getModifiers())) continue;
					Inject inject = field.getAnnotation(Inject.class);
					Autowired autowired = field.getAnnotation(Autowired.class);
					Value value = field.getAnnotation(Value.class);
					if (value != null) {
						try {
							if (fieldList == null)
								injectCache.put(targetClass, field); 
							doInjectConfigValue(targetObject, field, value);
						} catch (Exception e) {
							throw new RuntimeException("Try to get value from " + value.value() + " inject into field '" + field.toString() + "' failed: " + e.getMessage());
						}
						continue;
					}
					boolean required = true;
					if (inject == null && autowired == null) {
						continue;
					}
					if (fieldList == null)
						injectCache.put(targetClass, field); 
					Class<?> injectionType = null;
					String qualifierType = null;
					if (inject != null) {
						Class<?> fieldInjectedClass = inject.value();
						if (fieldInjectedClass != Void.class) {
							qualifierType = fieldInjectedClass.getCanonicalName();
						} else if (StrUtil.notBlank(inject.name())) {
							qualifierType = ConfigInjector.getStringValue(inject.name());
						}
						injectionType = field.getType();
						required = inject.required();
					} else {
						Qualifier qualifier = field.getAnnotation(Qualifier.class);
						if (qualifier != null && StrUtil.notBlank(qualifier.value())) {
							qualifierType = ConfigInjector.getStringValue(qualifier.value());
						}
						injectionType = field.getType();
						required = autowired.required();
					}

					Object fieldInjectedObject = doGet(injectionType, required, qualifierType);
					field.setAccessible(true);
					field.set(targetObject, fieldInjectedObject);
				}
			}
		}
		Class<?> c = targetClass.getSuperclass();
		if (c != work.ready.core.handler.Controller.class
				&& c != Interceptor.class
				&& c != ModelService.class
				&& c != BusinessService.class
				&& c != Object.class
				&& c != Model.class
				&& c != null
		) {
			doInject(c, targetObject);
		}
	}

	private void doInjectConfigValue(Object targetObject, Field field, Value configValue) throws Exception {
		String key = configValue.value();
		Class<?> fieldType = field.getType();
		Object fieldInjectedObject = null;
		if(Value.Source.VALUES.equals(configValue.source())) {
			String value = ConfigInjector.getStringValue(key);
			fieldInjectedObject = ClassUtil.typeCast(fieldType, value);
		} else if(Value.Source.APPLICATION.equals(configValue.source())){

			if(key.startsWith("${readyWork.")) {
				fieldInjectedObject = ConfigInjector.getConfigProperty(key, fieldType, Ready.getApplicationConfig(configValue.appName()));
			} else {
				fieldInjectedObject = ConfigInjector.getConfigProperty(key, fieldType, configValue.appName());
			}
		}
		if(fieldInjectedObject != null) {
			field.setAccessible(true);
			field.set(targetObject, fieldInjectedObject);
		}
	}

	protected Object createObject(Class<?> targetClass) throws ReflectiveOperationException {
		Method beanMethod = configurationMapping.get(targetClass.getCanonicalName());
		if(beanMethod != null) {
			Object bean;
			Class<?> configClass = beanMethod.getDeclaringClass();
			String named = ConfigInjector.getStringValue(configClass.getAnnotation(Configuration.class).value());
			bean = beanMethod.invoke(doGet(configClass, true, named));
			return bean;
		}
		if(targetClass.isAnnotationPresent(ConfigurationProperties.class)){
			ConfigurationProperties conf = targetClass.getAnnotation(ConfigurationProperties.class);
			String prefix = conf.prefix();
			String file = conf.file();
			Object target = null;
			if(StrUtil.notBlank(file) && StrUtil.notBlank(prefix)){
				target = ConfigInjector.getConfigBean(prefix, targetClass, file, conf.appName());
			} else if(StrUtil.notBlank(prefix)) {

				if(prefix.startsWith("${readyWork.")) {
					target = ConfigInjector.getConfigBean(prefix, targetClass, Ready.getApplicationConfig(conf.appName()));
				} else {
					target = ConfigInjector.getConfigBean(prefix, targetClass, null, conf.appName());
				}
			} else {
				throw new RuntimeException("Wrong configurationProperties annotation in class " + targetClass);
			}
			if(target == null) throw new RuntimeException("Create " + targetClass.getCanonicalName() + " type of config bean failed, by '" + prefix + "' configurationProperties annotation on class " + targetClass);
			return target;
		}

		Class<?> beanClass = (context.getProxyManager() != null && context.getInterceptorManager() != null) ? context.getProxyManager().get(targetClass) : targetClass;
		Object bean = null;
		List<BeanConfig> matchedConfig = new ArrayList<>();
		BeanConfig constructorConfig = null;
		for(BeanConfig config : beanConfig) {
			boolean matched = false;
			if(config.getClazz() != null && config.getClazz().equals(targetClass)){
				matched = true;
			} else {
				if(config.getAssignableFrom() != null && StrUtil.notBlank(config.getNameContains())){
					matched = (config.getAssignableFrom().isAssignableFrom(targetClass) && beanClass.getCanonicalName().contains(config.getNameContains()));
				} else if (config.getAssignableFrom() != null && config.getAssignableFrom().isAssignableFrom(targetClass)) {
					matched = true;
				} else if (StrUtil.notBlank(config.getNameContains()) && beanClass.getCanonicalName().contains(config.getNameContains())) {
					matched = true;
				}
			}
			if(matched) {
				if(config.getConstructorParams().size() > 0) {
					if(constructorConfig != null){
						throw new RuntimeException("Found " + targetClass.getCanonicalName() + " type of bean " + beanClass.getCanonicalName() + " has more than one constructor configs, please check bean configs.");
					}
					constructorConfig = config;
				} else {
					matchedConfig.add(config);
				}
			}
		}
		if(matchedConfig.size() > 0) {
			matchedConfig.sort(Comparator.comparingInt(c->{var b=(BeanConfig)c;return b.getInjectProperties().size();}).reversed());
		}
		if(constructorConfig != null) matchedConfig.add(0, constructorConfig);
		for(BeanConfig config : matchedConfig) {
			if(bean == null){
				try {
					bean = (config.getConstructorParams().size() > 0) ? construct(Kv.by(beanClass, config.getConstructorParams())) : ClassUtil.newInstance(beanClass);
				} catch (Exception e) {
					throw new RuntimeException("Create " + targetClass.getCanonicalName() + " type of bean " + beanClass.getCanonicalName() + " failed.");
				}
			} else {
				if (config.getInjectProperties().size() > 0) {
					try {
						bean = injectProperties(bean, config.getInjectProperties());
					} catch (Exception e) {
						throw new RuntimeException("Inject properties for " + targetClass.getCanonicalName() + " type of bean " + beanClass.getCanonicalName() + " failed.");
					}
				}
				if (StrUtil.notBlank(config.getInitMethod())) {
					try {
						Method initMethod = bean.getClass().getMethod(config.getInitMethod());
						initMethod.invoke(bean);
					} catch (NoSuchMethodException e) {
						throw new RuntimeException("InitMethod '" + config.getInitMethod() + "' has been set for " + targetClass.getCanonicalName() + ", which is not exist in " + bean.getClass().getCanonicalName() + ".");
					}
				}
				if(StrUtil.notBlank(config.getDestroyMethod())){
					try {
						Method destroyMethod = bean.getClass().getMethod(config.getDestroyMethod());
						beanDestroyHolder.add(new Object[]{destroyMethod, bean});
					} catch (NoSuchMethodException e) {
						throw new RuntimeException("DestroyMethod '" + config.getDestroyMethod() + "' has been set for " + targetClass.getCanonicalName() + ", which is not exist in " + bean.getClass().getCanonicalName() + ".");
					}
				}
			}
		}

		if(bean == null) {
			bean = ClassUtil.newInstance(beanClass);
		}
		if(bean instanceof DisposableBean) {
			Method destroyMethod = bean.getClass().getMethod("destroy");
			beanDestroyHolder.add(new Object[]{destroyMethod, bean});
		}
		return bean;
	}

	private void initConfiguredBean(Method beanMethod, Object bean) throws ReflectiveOperationException {
		if(beanMethod != null) {
			Bean beanAnnotation = beanMethod.getAnnotation(Bean.class);
			String initMethodName = ConfigInjector.getStringValue(beanAnnotation.initMethod());
			String destroyMethodName = ConfigInjector.getStringValue(beanAnnotation.destroyMethod());
			if (StrUtil.notBlank(initMethodName)) {
				try {
					Method initMethod = bean.getClass().getMethod(initMethodName);
					initMethod.invoke(bean);
				} catch (NoSuchMethodException e) {
					Class<?> configClass = beanMethod.getDeclaringClass();
					throw new RuntimeException("InitMethod '" + initMethodName + "' has been declared on " + beanMethod.getName() + " in " + configClass.getCanonicalName() + ", which is not exist in " + bean.getClass().getCanonicalName() + ".");
				}
			}
			if (StrUtil.notBlank(destroyMethodName)) {
				try {
					Method destroyMethod = bean.getClass().getMethod(destroyMethodName);
					beanDestroyHolder.add(new Object[]{destroyMethod, bean});
				} catch (NoSuchMethodException e) {
					Class<?> configClass = beanMethod.getDeclaringClass();
					throw new RuntimeException("DestroyMethod '" + destroyMethodName + "' has been declared on " + beanMethod.getName() + " in " + configClass.getCanonicalName() + ", which is not exist in " + bean.getClass().getCanonicalName() + ".");
				}
			}
			if(bean instanceof DisposableBean) {
				Method destroyMethod = bean.getClass().getMethod("destroy");
				beanDestroyHolder.add(new Object[]{destroyMethod, bean});
			}
		}
	}

	public BeanManager setSingleton(boolean singleton) {
		this.singleton = singleton;
		return this;
	}

	public boolean isSingleton() {
		return singleton;
	}

	public synchronized BeanManager addSingletonObject(Class<?> type, Object singletonObject) {
		if (type == null) {
			throw new IllegalArgumentException("type can not be null");
		}
		if (singletonObject == null) {
			throw new IllegalArgumentException("singletonObject can not be null");
		}

		if ( ! (type.isAssignableFrom(singletonObject.getClass())) ) {
			throw new IllegalArgumentException(singletonObject.getClass().getName() + " can not cast to " + type.getName());
		}

		addMapping(type, true);
		if (singletonCache.putIfAbsent(type, singletonObject) != null) {
			throw new RuntimeException("Singleton object already exists for type : " + type.getName());
		}

		return this;
	}

	public BeanManager addSingletonObject(Object singletonObject) {
		Class<?> type = ClassUtil.getUserClass(singletonObject.getClass());
		return addSingletonObject(type, singletonObject);
	}

	public synchronized <T> BeanManager addConfigurationMapping(String type, Method configBy){
		if (type == null || configBy == null) {
			throw new IllegalArgumentException("The parameter type and configBy can not be null");
		}

		if (configurationMapping.containsKey(type)) {
			throw new RuntimeException("Class already mapped : " + type);
		}
		configurationMapping.put(type, configBy);
		return this;
	}

	public synchronized <T> BeanManager addMapping(String from, Class<? extends T> to, boolean unique) {
		if (from == null || to == null) {
			throw new IllegalArgumentException("The parameter from and to can not be null");
		}

		if (mapping.containsKey(from)){
			if(unique && mapping.get(from).size() > 0){
				for(Class<?> clazz : mapping.get(from)) {
					if(clazz.equals(to)) return this;
				}
				throw new RuntimeException("Class '" + from + "' is already mapped to '" + mapping.get(from).toString() + "'.");
			} else {
				mapping.get(from).add(to);
			}
		} else {
			HashSet<Class<?>> classSet = new HashSet<>();
			classSet.add(to);
			mapping.put(from, classSet);
		}
		return this;
	}

	public synchronized <T> BeanManager addMapping(Class<T> type, T instance){
		if (instance == null) throw new IllegalArgumentException("instance must not be null");

		if (!isTypeOf(instance, type))
			throw new Error(format("Instance type does not match, type=%s, instanceType=%s", type.getTypeName(), instance.getClass().getCanonicalName()));
		addMapping(type.getCanonicalName(), ClassUtil.getUserClass(instance.getClass()), false);
		if (singletonCache.putIfAbsent(ClassUtil.getUserClass(instance.getClass()), instance) != null) {
			throw new RuntimeException("Singleton object already exists for type : " + type.getName());
		}
		return this;
	}

	public <T> BeanManager addMapping(Class<T> from, Class<? extends T> to, boolean unique) {
		if (from.isAssignableFrom(to)) {
			return addMapping(from.getCanonicalName(), to, unique);
		} else {
			throw new IllegalArgumentException("The parameter 'to' must be the subclass or implementation of the parameter 'from'");
		}
	}

	public <T> BeanManager addMapping(Class<T> clazz, boolean unique) {
		return addMapping(clazz.getCanonicalName(), clazz, unique);
	}

	public synchronized BeanManager removeMapping(String type) {
		if(mapping.containsKey(type)) {
			if(mapping.get(type).size() == 1) {
				mapping.get(type).forEach(c->singletonCache.remove(c));
			} else {
				mapping.remove(type);
			}
		}
		return this;
	}

	public synchronized BeanManager removeMapping(Class<?> clazz){
		mapping.remove(ClassUtil.getUserClass(clazz).getCanonicalName());
		singletonCache.remove(clazz);
		return this;
	}

	private boolean isTypeOf(Object instance, Type type) {
		if (type instanceof Class) return ((Class) type).isInstance(instance);
		if (type instanceof ParameterizedType) return isTypeOf(instance, ((ParameterizedType) type).getRawType());
		throw new Error("not supported type, type=" + type.getTypeName());
	}

	public Class<?> getMappingClass(Class<?> from, boolean required, String qualifier) {
		Class<?> ret = null;
		boolean moreThanOne = false;
		HashSet<Class<?>> classSet;
		if (StrUtil.notBlank(qualifier)) {
			classSet = mapping.get(qualifier);
			if(classSet != null) {
				Iterator<Class<?>> iterator = classSet.iterator();
				int counter = 0;
				while (iterator.hasNext()) {
					Class<?> next = iterator.next();
					if(from.equals(next)) {
						ret = next;
						counter = 1;
						break;
					}
					if(from.isAssignableFrom(next)) {
						ret = next;
						counter++;
					}
				}
				if(counter > 1) {
					ret = null;
					moreThanOne = true;
				}
			}
		} else {
			classSet = mapping.get(from.getCanonicalName());
			if(classSet != null) {
				Iterator<Class<?>> iterator = classSet.iterator();
				if (classSet.size() > 1) {
					moreThanOne = true;
					while (iterator.hasNext()) {
						Class<?> next = iterator.next();
						if (from.equals(next)) {
							ret = next;
							moreThanOne = false;
							break;
						}
					}
				} else {
					if (iterator.hasNext()) {
						ret = iterator.next();
					}
				}
			}
		}
		if(moreThanOne) {
			throw new RuntimeException("No qualifying bean of type '" + from + "' available: expected single matching bean but found " + classSet.size() + ": " + classSet.toString());
		}
		if(ret == null && StrUtil.isBlank(qualifier) && isInstantiable(from)){
			ret = from;
		}
		if(ret == null && required) {
			throw new RuntimeException("No qualifying bean " + (qualifier == null ? "" : "'" + qualifier + "' ") + "of type '" + from + "' available and it is required.");
		}
		return ret;
	}

	public boolean isInstantiable(Class<?> clz) {
		if(clz.isPrimitive() || Modifier.isAbstract( clz.getModifiers()) ||clz.isInterface()  || clz.isArray() || inExcludes(clz, DEFAULT_EXCLUDES_MAPPING_CLASSES)){
			return false;
		}
		return true;
	}

	public static Object construct(Object something) throws Exception {
		if (something instanceof String) {
			return Class.forName((String) something).getConstructor().newInstance();
		} else if(something instanceof Class){
			return ClassUtil.newInstance((Class)something);
		} else if (something instanceof Map) {

			for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) something).entrySet()) {
				if (entry.getValue() instanceof Map) {
					if(entry.getKey() instanceof String) {
						return constructByNamedParams(Class.forName((String)entry.getKey()), (Map) entry.getValue());
					}else if(entry.getKey() instanceof Class){
						return constructByNamedParams((Class)entry.getKey(), (Map) entry.getValue());
					}
				} else if (entry.getValue() instanceof List) {
					if(entry.getKey() instanceof String) {
						return constructByParameterizedConstructor(Class.forName((String)entry.getKey()), (List) entry.getValue());
					}else if(entry.getKey() instanceof Class) {
						return constructByParameterizedConstructor((Class)entry.getKey(), (List) entry.getValue());
					}
				}
			}
		}
		return null;
	}

	public static Object constructByNamedParams(Class clazz, Map params) throws Exception {
		Object obj = ClassUtil.newInstance(clazz);
		return injectProperties(obj, params);
	}

	public static Object injectProperties(Object object, Map params) throws Exception {
		Class clazz = object.getClass();
        if (params.size() > 0) {
            Set<Field> allFields = new HashSet<>();
            allFields.addAll(Arrays.asList(clazz.getFields()));
            allFields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            for (Field field : allFields){
                if(Modifier.isFinal(field.getModifiers())) continue;
                if(params.containsKey(field.getName())){
                    field.setAccessible(true);
                    field.set(object, params.get(field.getName()));
                }
            }
        }
		return object;
	}

	public static Object constructByParameterizedConstructor(Class clazz, List parameters) throws Exception {

		Object instance  = null;
		java.lang.reflect.Constructor[] allConstructors = clazz.getDeclaredConstructors();

		boolean hasDefaultConstructor = false;
		for (java.lang.reflect.Constructor ctor : allConstructors) {
			Class<?>[] pType  = ctor.getParameterTypes();
			if(pType.length > 0) {
				if(pType.length == parameters.size()) {

					boolean matched = true;
					Object[] params = new Object[pType.length];
					for (int j = 0; j < pType.length; j++) {

						Map<Object, Object> parameter = (Map)parameters.get(j);
						Iterator it = parameter.entrySet().iterator();
						if (it.hasNext()) {  
							Map.Entry<Object, Object> pair = (Map.Entry) it.next();
							if(pair.getKey() instanceof String) {
								String key = (String)pair.getKey();
								Object value = pair.getValue();
								if (pType[j].getName().equals(key)) {
									params[j] = value;
								} else {
									matched = false;
									break;
								}
							} else {
								Class<?> key = (Class<?>) pair.getKey();
								Object value = pair.getValue();
								if (pType[j].equals(key)) {
									params[j] = value;
								} else {
									matched = false;
									break;
								}
							}
						}
					}
					if(matched) {

						instance = ctor.newInstance(params);
						break;
					}
				}
			} else {
				hasDefaultConstructor = true;
			}
		}
		if(instance != null) {
			return instance;
		} else {
			if(hasDefaultConstructor) {
				return clazz.getDeclaredConstructor().newInstance();
			} else {

				throw new Exception("No instance can be created for class " + clazz);
			}
		}
	}
}

