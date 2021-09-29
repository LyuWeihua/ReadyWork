/**
 *
 * Copyright (c) 2020 WeiHua Lyu [ready.work]
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
package work.ready.core.handler;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import io.undertow.util.PathTemplateMatcher;
import work.ready.core.config.ConfigInjector;
import work.ready.core.event.Event;
import work.ready.core.event.GeneralEvent;
import work.ready.core.ioc.annotation.HttpHandler;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.module.ApplicationContext;
import work.ready.core.module.Initializer;
import work.ready.core.server.Ready;
import work.ready.core.tools.ClassUtil;
import work.ready.core.tools.StrUtil;
import java.util.*;
import java.util.stream.Collectors;

import static io.undertow.util.PathTemplateMatch.ATTACHMENT_KEY;

public class HandlerManager {
	private static final Log logger = LogFactory.getLog(HandlerManager.class);
	protected final AttachmentKey<Integer> CHAIN_SEQ = AttachmentKey.create(Integer.class);
	protected final AttachmentKey<String> CHAIN_ID = AttachmentKey.create(String.class);
	private volatile boolean commonHandlerMerged = false;

	private Map<String, Object> serverModuleRegistry = new HashMap<>();
	
	protected final Map<String, BaseHandler> handlers = new HashMap<>();
	protected final Map<String, List<BaseHandler>> handlerListById = new LinkedHashMap<>();
	protected Map<String, Map<String, Map<String, PathTemplateMatcher<String>>>> matcherMap = new HashMap<>(2048,0.5F);
	protected final ServerModuleConfig config;
	protected List<Initializer<HandlerManager>> initializers = new ArrayList<>();
	protected MainHandler mainHandler;
	private final ApplicationContext context;

	public HandlerManager(ApplicationContext context) {
		this.context = context;
		config = Ready.getApplicationConfig(context.application.getName()).getServerModule();
		if(config != null) config.validate();
		Ready.post(new GeneralEvent(Event.HANDLER_MANAGER_CREATE, this));
	}

	public ServerModuleConfig getConfig(){ return config; }

	public void addInitializer(Initializer<HandlerManager> initializer) {
		this.initializers.add(initializer);
		initializers.sort(Comparator.comparing(Initializer::order));
	}

	public void startInit() {
		try {
			for (Initializer<HandlerManager> i : initializers) {
				i.startInit(this);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void endInit() {
		try {
			for (Initializer<HandlerManager> i : initializers) {
				i.endInit(this);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public HandlerManager addHandler(Class<?> clazz){
		Class<?> type = ClassUtil.getUserClass(clazz);
		if(handlers.containsKey(type.getCanonicalName())) return this;
		if(BaseHandler.class.isAssignableFrom(type)) {
			Ready.beanManager().addMapping(type, true);
			
			Object handlerObject = null;
			try {
				handlerObject = Ready.beanManager().get(type);
			} catch (Exception e) {
				throw new RuntimeException("Could not instantiate handler class: " + clazz.getCanonicalName());
			}
			return addHandler(handlerObject);
		} else {
			throw new IllegalArgumentException("Illegal handler, " + clazz.getCanonicalName() + " is a invalid handler.");
		}
	}

	public List<BaseHandler> getHandles(){
		return new ArrayList<>(handlers.values());
	}

	private HandlerManager addHandler(Object handlerObject){
		BaseHandler resolvedHandler = null;
		Class clazz = null;
		if (handlerObject instanceof BaseHandler) {
			resolvedHandler = (BaseHandler) handlerObject;
			resolvedHandler.setManager(this);
			resolvedHandler.setApplicationConfig(Ready.getApplicationConfig(context.application.getName()));
			clazz = ClassUtil.getUserClass(resolvedHandler.getClass());
			if(config != null && config.getModuleConfig(StrUtil.firstCharToLowerCase(clazz.getSimpleName())) != null) {
				resolvedHandler.setConfig(config.getModuleConfig(StrUtil.firstCharToLowerCase(clazz.getSimpleName())));
			}
		} else {
			throw new RuntimeException("Unsupported type of handler provided: " + handlerObject.getClass().getCanonicalName());
		}
		if(resolvedHandler != null && clazz != null) {
			registerServerModule(resolvedHandler);
			handlers.put(clazz.getCanonicalName(), resolvedHandler);
			handlerListById.put(clazz.getCanonicalName(), Collections.singletonList(resolvedHandler));
			resolvedHandler.initialize();
		}
		return this;
	}

	public synchronized HandlerManager removeHandler(Class<? extends BaseHandler> clazz){
		Class<?> type = ClassUtil.getUserClass(clazz);
		if(handlers.containsKey(type.getCanonicalName())){
			for(List<BaseHandler> handlerList : handlerListById.values()){
				handlerList.removeIf(each -> type.equals(ClassUtil.getUserClass(each.getClass())));
			}
			handlers.remove(type.getCanonicalName());
			Ready.beanManager().removeMapping(type);
		}
		return this;
	}

	public HandlerManager addHandler(String path, RequestMethod method, BaseHandler... handler){
		return addHandler(path, method, false, handler);
	}

	public HandlerManager addHandler(String path, RequestMethod method, boolean override, BaseHandler... handler){
		if(StrUtil.isBlank(path) || !path.startsWith("/")) throw new Error("Handle path is invalid");
		method = method == null ? RequestMethod.GET : method;
		return addHandler(new String[]{"*"}, new String[]{"*"}, new String[]{path}, new RequestMethod[]{method}, override, handler);
	}

	public HandlerManager addHandler(String path, RequestMethod[] method, BaseHandler... handler) {
		return addHandler(path, method, false, handler);
	}

	public HandlerManager addHandler(String path, RequestMethod[] method, boolean override, BaseHandler... handler) {
		if(StrUtil.isBlank(path) || !path.startsWith("/")) throw new Error("Handle path is invalid");
		return addHandler(new String[]{"*"}, new String[]{"*"}, new String[]{path}, method, override, handler);
	}

	public HandlerManager addHandler(String[] path, RequestMethod[] method, BaseHandler... handler) {
		return addHandler(path, method, false, handler);
	}

	public HandlerManager addHandler(String[] path, RequestMethod[] method, boolean override, BaseHandler... handler) {
		return addHandler(new String[]{"*"}, new String[]{"*"}, path, method, override, handler);
	}

	public HandlerManager addHandler(String[] host, String[] subHost, String[] path, RequestMethod[] method, BaseHandler... handler){
		return addHandler(host, subHost, path, method, false, handler);
	}

	public HandlerManager addHandler(String[] host, String[] subHost, String[] path, RequestMethod[] method, boolean override, BaseHandler... handler){
		if(handler == null || handler.length == 0) throw new Error("Handler is invalid");
		List<String> list = new ArrayList<>();
		for(int i=0; i<handler.length; i++) {
			if (!handlers.containsKey(ClassUtil.getUserClass(handler[i].getClass()).getCanonicalName())) addHandler(handler[i]);
			list.add(ClassUtil.getUserClass(handler[i].getClass()).getCanonicalName());
		}
		ServerModuleConfig.PathChain pathChain  = new ServerModuleConfig.PathChain();
		pathChain.setHost(new ArrayList<>(Arrays.asList(host))).setSubHost(new ArrayList<>(Arrays.asList(subHost))).setPath(new ArrayList<>(Arrays.asList(path)))
				.setMethod(Arrays.stream(method).map(Enum::name).collect(Collectors.toList()))
				.setHandlers(list).validate("ServerModuleConfig ");
		addPathChain(pathChain, override);
		return this;
	}

	public HandlerManager removeHandler(String urlPath){
		return removeHandler(urlPath, null, null, null);
	}

	public HandlerManager removeHandler(String urlPath, RequestMethod method){
		return removeHandler(urlPath, method, null, null);
	}

	public synchronized HandlerManager removeHandler(String urlPath, RequestMethod method, String Host, String subHost){
		for (String thisHost : matcherMap.keySet()) {
			if(StrUtil.notBlank(Host) && !thisHost.equals(Host)) continue;
			var subHostMap = matcherMap.get(thisHost);
			for (String thisSubHost : subHostMap.keySet()) {
				if(StrUtil.notBlank(subHost) && !thisSubHost.equals(subHost)) continue;
				var methodMap = subHostMap.get(thisSubHost);
				for (String thisMethod : methodMap.keySet()) {
					if(method != null && !thisMethod.equals(method.name())) continue;
					PathTemplateMatcher<String> pathTemplateMatcher = methodMap.get(thisMethod);
					pathTemplateMatcher.remove(urlPath);
				}
			}
		}
		return this;
	}

	public void addHandlerByConfig(ServerModuleConfig config) {
		if(config != null){
			try {
				for (var pathChain : config.getPathChain().values()) {
					for (String handler : pathChain.getHandlers()) {
						Class<?> handlerClass = ClassUtil.forName(handler, Ready.getClassLoader());
						addHandler(handlerClass);
					}
					if (pathChain.getHost() == null || pathChain.getHost().size() == 0) {
						pathChain.setHost(config.getHost());
						pathChain.setSubHost(config.getSubHost());
					}
					addPathChain(pathChain, false);
				}
			} catch (ClassNotFoundException e) {
				throw new RuntimeException("Loading server handler failed: ", e);
			}
		}
	}

	public void addHandlerByAnnotation(Class<?> handlerClass){
		if (!BaseHandler.class.isAssignableFrom(handlerClass)){
			if(logger.isErrorEnabled())
				logger.error("Class " + handlerClass.getCanonicalName() + " is an invalid Handler or ServerModule.");
			return;
		}
		addHandler(handlerClass);
		var handler = handlers.get(handlerClass.getCanonicalName());
		if(!handlerClass.isAnnotationPresent(HttpHandler.class)) return;
		HttpHandler annotation = handlerClass.getAnnotation(HttpHandler.class);
		RequestMethod[] method = annotation.method();
		String[] path = ConfigInjector.getStringValue(annotation.path());
		handler.setOrder(annotation.order());
		var pathChain = new ServerModuleConfig.PathChain();
		pathChain.setHost(Arrays.asList(ConfigInjector.getStringValue(annotation.host())));
		pathChain.setSubHost(Arrays.asList(ConfigInjector.getStringValue(annotation.subHost())));
		pathChain.setMethod(Arrays.stream(method).map(Enum::name).collect(Collectors.toList()));
		pathChain.setPath(Arrays.asList(path));
		pathChain.setHandlers(Collections.singletonList(handlerClass.getCanonicalName()));
		addPathChain(pathChain, false);
	}

	private synchronized void addPathChain(ServerModuleConfig.PathChain pathChain, boolean override) {
		List<String> hostList = pathChain.getHost();
		List<String> subHostList = pathChain.getSubHost();
		if(subHostList == null || subHostList.size() == 0){
			subHostList = new ArrayList<>(); subHostList.add("*");
		}
		if (hostList == null || hostList.size() == 0) {
			hostList = new ArrayList<>(); hostList.add("*");
			subHostList = new ArrayList<>(); subHostList.add("*"); 
		}
		List<String> methodList = pathChain.getMethod();
		List<String> pathList = pathChain.getPath();

		List<BaseHandler> handlers = getHandlersFromList(pathChain.getHandlers());
		handlers.sort(Comparator.comparingInt(BaseHandler::getOrder));
		if(handlers.size() > 0) {
			if(pathList.size() == 0 && hostList.contains("*") && subHostList.contains("*")){
				List<BaseHandler> tempList = new ArrayList<>(handlers);
				if (!override && handlerListById.containsKey("default")) {
					tempList.addAll(handlerListById.get("default")); 
					tempList.sort(Comparator.comparingInt(BaseHandler::getOrder));
				}
				handlerListById.put("default", tempList);
				hostList.remove("*"); subHostList.remove("*");
			}

			for (String host : hostList) {
				host = host.toLowerCase();
				Map<String, Map<String, PathTemplateMatcher<String>>> subHostMap = matcherMap.containsKey(host) ? matcherMap.get(host) : new LinkedHashMap<>();
				for (String subHost : subHostList) {
					subHost = subHost.toLowerCase();
					Map<String, PathTemplateMatcher<String>> methodMap = subHostMap.containsKey(subHost) ? subHostMap.get(subHost) : new LinkedHashMap<>();
					for (String requestMethod : methodList) {
						if(pathList.size()==0){
							String thisChainName  = host + "_" + subHost + "_" + requestMethod + "_" + "default";
							List<BaseHandler> tempList = new ArrayList<>(handlers);
							if (!override && handlerListById.containsKey(thisChainName)) {
								tempList.addAll(handlerListById.get(thisChainName)); 
								tempList.sort(Comparator.comparingInt(BaseHandler::getOrder));
							}
							handlerListById.put(thisChainName, tempList);
						} else {
							PathTemplateMatcher<String> pathTemplateMatcher = methodMap.containsKey(requestMethod)
									? methodMap.get(requestMethod)
									: new PathTemplateMatcher<>();
							for (String path : pathList) {
								String thisChainName = host + "_" + subHost + "_" + requestMethod + "_" + path;
								if(path.endsWith("**")) {	
									List<BaseHandler> tempList = new ArrayList<>(handlers);
									if (!override && handlerListById.containsKey(thisChainName)) {
										tempList.addAll(handlerListById.get(thisChainName)); 
										tempList.sort(Comparator.comparingInt(BaseHandler::getOrder));
									}
									handlerListById.put(thisChainName, tempList);
									commonHandlerMerged = false;
									path = path.substring(0, path.length() - 1); 
									thisChainName = host + "_" + subHost + "_" + requestMethod + "_" + path;
								}
								String oldPathId = pathTemplateMatcher.get(path);
								List<BaseHandler> tempList = new ArrayList<>(handlers);
								if (oldPathId != null) {
									if (!override) {
										tempList.addAll(handlerListById.get(oldPathId)); 
										tempList.sort(Comparator.comparingInt(BaseHandler::getOrder));
									}
									pathTemplateMatcher.remove(path);
								}
								handlerListById.put(thisChainName, tempList);
								pathTemplateMatcher.add(path, thisChainName);
							}
							methodMap.put(requestMethod, pathTemplateMatcher);
						}
					}
					subHostMap.put(subHost, methodMap);
					subHostMap = subHostMap.entrySet().stream()
							.sorted(Map.Entry.comparingByKey(Comparator.comparingInt(String::length).reversed()))
							.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
									(oldValue, newValue) -> oldValue, LinkedHashMap::new));
				}
				matcherMap.put(host, subHostMap);
				matcherMap = matcherMap.entrySet().stream()
						.sorted(Map.Entry.comparingByKey(Comparator.comparingInt(String::length).reversed()))
						.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
								(oldValue, newValue) -> oldValue, LinkedHashMap::new));
			}
		}
	}

	public void next(HttpServerExchange httpServerExchange) throws Exception {
		next(httpServerExchange, false);
	}

	public void next(HttpServerExchange httpServerExchange, boolean shouldDispatch) throws Exception {
		BaseHandler httpHandler = getNext(httpServerExchange);
		if (httpHandler != null) {
			if(httpHandler.isEnabled()) {
				if(shouldDispatch) {
					httpServerExchange.dispatch(httpHandler);
				} else {
					httpHandler.handleRequest(httpServerExchange);
				}
			} else {
				next(httpServerExchange, shouldDispatch);
			}
		} else {
			mainHandler.processRequest(httpServerExchange);
		}
	}

	public void next(HttpServerExchange httpServerExchange, BaseHandler next) throws Exception {
		next(httpServerExchange, next, false);
	}

	public void next(HttpServerExchange httpServerExchange, BaseHandler next, boolean shouldDispatch) throws Exception {
		if (next != null) {
			if(shouldDispatch) {
				httpServerExchange.dispatch(next);
			} else {
				next.handleRequest(httpServerExchange);
			}
		} else {
			next(httpServerExchange, shouldDispatch);
		}
	}

	public void next(HttpServerExchange httpServerExchange, String execName, Boolean returnToOrigFlow)
			throws Exception {
		String currentChainId = httpServerExchange.getAttachment(CHAIN_ID);
		Integer currentNextIndex = httpServerExchange.getAttachment(CHAIN_SEQ);

		httpServerExchange.putAttachment(CHAIN_ID, execName);
		httpServerExchange.putAttachment(CHAIN_SEQ, 0);

		next(httpServerExchange);

		if (returnToOrigFlow) {
			httpServerExchange.putAttachment(CHAIN_ID, currentChainId);
			httpServerExchange.putAttachment(CHAIN_SEQ, currentNextIndex);
			next(httpServerExchange);
		}
	}

	public BaseHandler getNext(HttpServerExchange httpServerExchange) {
		String chainId = httpServerExchange.getAttachment(CHAIN_ID);
		List<BaseHandler> handlersForId = handlerListById.get(chainId);
		Integer nextIndex = httpServerExchange.getAttachment(CHAIN_SEQ);
		
		if (handlersForId != null && nextIndex < handlersForId.size()) {
			httpServerExchange.putAttachment(CHAIN_SEQ, nextIndex + 1);
			return handlersForId.get(nextIndex);
		}
		return null;
	}

	public BaseHandler getNext(HttpServerExchange httpServerExchange, BaseHandler next) throws Exception {
		if (next != null) {
			return next;
		}
		return getNext(httpServerExchange);
	}

	public void start(HttpServerExchange httpServerExchange) throws Exception {
		if (!commonHandlerMerged) {
			mergeCommonHandlers();
		}
		boolean matched = match(httpServerExchange, false);
		if(!matched){
			httpServerExchange.putAttachment(CHAIN_ID, "default");
			httpServerExchange.putAttachment(CHAIN_SEQ, 0);
		}
		next(httpServerExchange);
	}

	private synchronized void mergeCommonHandlers() {
		if (!commonHandlerMerged) {
			List<String> commonChain = handlerListById.keySet().stream().filter(name -> name.endsWith("**")).collect(Collectors.toList());
			handlerListById.forEach((chainName, handlerList) -> {
				if (!chainName.endsWith("**")) {
					for (String common : commonChain) {
						if (chainName.startsWith(common.substring(0, common.length() - 2))) { 
							handlerListById.get(common).forEach((handler) -> {
								if (!handlerList.contains(handler)) {
									handlerList.add(handler);
									handlerList.sort(Comparator.comparingInt(BaseHandler::getOrder));
								}
							});
						}
					}
				}
			});
			commonHandlerMerged = true;
		}
	}

	private boolean match(HttpServerExchange httpServerExchange, boolean matchDefault){
		String hostName = httpServerExchange.getHostName().toLowerCase();
		String requestMethod = httpServerExchange.getRequestMethod().toString();
		boolean matched = false;
		for (String host : matcherMap.keySet()) {
			if (host.equals("*") || hostName.endsWith(host)) {
				var subHostMap = matcherMap.get(host);
				for (String subHost : subHostMap.keySet()) {
					if (!host.equals("*") && !subHost.equals("*") && !hostName.equals(host)) {
						String subDomain = hostName.substring(0, hostName.length() - host.length());
						if (subDomain.length() > 0 && subDomain.endsWith(".")) {
							subDomain = subDomain.substring(0, subDomain.length() - 1); 
							if (subHost.contains("*")) {
								if (subHost.startsWith("*")) {
									String forCheck = subHost.substring(1);
									if (subDomain.endsWith(forCheck)) matched = true;
								} else if (subHost.endsWith("*")) {
									String forCheck = subHost.substring(0, subHost.length() - 1);
									if (subDomain.startsWith(forCheck)) matched = true;
								} else if (subHost.startsWith("*") && subHost.endsWith("*")) {
									String forCheck = subHost.substring(1, subHost.length() - 2);
									if (subDomain.contains(forCheck)) matched = true;
								}
							} else {
								if (subDomain.equals(subHost)) matched = true;
							}
						}
					} else {
						matched = true;
					}
					if (matched) {
						matched = false; 
						if(matchDefault){
							String defaultId = host + "_" + subHost + "_" + requestMethod + "_" + "default";
							if(handlerListById.containsKey(defaultId)){
								httpServerExchange.putAttachment(CHAIN_ID, defaultId);
								httpServerExchange.putAttachment(CHAIN_SEQ, 0);
								matched = true; break;
							}
						} else {
							var methodMap = subHostMap.get(subHost);
							PathTemplateMatcher<String> pathTemplateMatcher = methodMap.get(requestMethod);
							if (pathTemplateMatcher != null) {
								PathTemplateMatcher.PathMatchResult<String> result = pathTemplateMatcher
										.match(httpServerExchange.getRequestPath());
								if (result != null) {

									httpServerExchange.putAttachment(ATTACHMENT_KEY,
											new io.undertow.util.PathTemplateMatch(result.getMatchedTemplate(), result.getParameters()));
									for (Map.Entry<String, String> entry : result.getParameters().entrySet()) {
										
										httpServerExchange.addPathParam(entry.getKey(), entry.getValue());
									}
									String id = result.getValue();
									httpServerExchange.putAttachment(CHAIN_ID, id);
									httpServerExchange.putAttachment(CHAIN_SEQ, 0);
									matched = true;
									break;
								}
							}
						}
					}
				}
				if(matched) break;
			}
		}
		if(!matched && !matchDefault) match(httpServerExchange, true);
		return matched;
	}

	private List<BaseHandler> getHandlersFromList(List<String> list) {
		List<BaseHandler> handlersFromList = new ArrayList<>();
		if (list != null) {
			for (String exec : list) {
				List<BaseHandler> handlerList = handlerListById.get(exec);
				if (handlerList == null)
					throw new RuntimeException("Unknown handler or chain: " + exec);

				for(BaseHandler handler : handlerList) {
					if(handler instanceof ServerModule) {
						
						if(((ServerModule)handler).isEnabled()) {
							handlersFromList.add(handler);
						}
					} else {
						handlersFromList.add(handler);
					}
				}
			}
		}
		return handlersFromList;
	}

	private void registerServerModule(Object handler) {
		if(handler instanceof ServerModule) {
			
			if(((ServerModule) handler).isEnabled()) {
				((ServerModule) handler).register();
			}
		}
	}

	public void registerModule(String moduleName, Map<String, Object> config) {
		
		if(config != null) {
			serverModuleRegistry.put(moduleName, config);
		} else {
			
			serverModuleRegistry.put(moduleName, new HashMap<String, Object>());
		}
	}

	public Map<String, Object> getRegistry() {
		return Collections.unmodifiableMap(serverModuleRegistry);
	}

}
