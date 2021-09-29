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

import io.undertow.util.PathTemplateMatcher;
import work.ready.core.aop.Interceptor;
import work.ready.core.aop.InterceptorManager;
import work.ready.core.config.ConfigInjector;
import work.ready.core.event.Event;
import work.ready.core.event.GeneralEvent;
import work.ready.core.handler.RequestMethod;
import work.ready.core.handler.action.Action;
import work.ready.core.handler.Controller;
import work.ready.core.handler.request.HttpRequest;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.module.ApplicationContext;
import work.ready.core.module.Initializer;
import work.ready.core.server.Ready;
import work.ready.core.service.result.Result;
import work.ready.core.tools.ClassUtil;
import work.ready.core.tools.StrUtil;
import work.ready.core.tools.define.SyncWriteMap;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import static io.undertow.util.PathTemplateMatch.ATTACHMENT_KEY;

public class RouteManager {
    private static final Log logger = LogFactory.getLog(RouteManager.class);
    private final ApplicationContext context;
    public static final String SLASH = "/";
    protected List<Initializer<RouteManager>> initializers = new ArrayList<>();
    protected RouteConfig routes = new RouteConfig();;
    protected RouteConfig routeConfig;
    protected Map<String, Map<String, Map<RequestMethod, PathTemplateMatcher<Action>>>> matcherMap = new SyncWriteMap<>(2048,0.5F);

    public RouteManager(ApplicationContext context) {
        this.context = context;
        routeConfig = Ready.config().getConfig(RouteConfig.class, context.application.getName());
        routeConfig.setMappingSuperClass(true);
        Ready.post(new GeneralEvent(Event.ROUTE_MANAGER_CREATE, this));
    }

    public RouteConfig getConfig(){
        return routeConfig;
    }

    public RouteConfig getAllRoutes(){
        return routes;
    }

    public void addInitializer(Initializer<RouteManager> initializer) {
        this.initializers.add(initializer);
        initializers.sort(Comparator.comparing(Initializer::order));
    }

    public void startInit() {
        try {
            for (Initializer<RouteManager> i : initializers) {
                i.startInit(this);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void endInit() {
        try {
            for (Initializer<RouteManager> i : initializers) {
                i.endInit(this);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public RouteManager add(RouteConfig routeConfig){
        for (RouteConfig routes : getRoutesList(routeConfig)) {
            buildActionMapping(routes);
            this.routes.add(routes);
        }
        return this;
    }

    public RouteManager remove(String urlPath){
        return remove(urlPath, null, null, null);
    }

    public RouteManager remove(String urlPath, RequestMethod method){
        return remove(urlPath, method, null, null);
    }

    public synchronized RouteManager remove(String urlPath, RequestMethod method, String Host, String subHost){
        for (String thisHost : matcherMap.keySet()) {
            if(StrUtil.notBlank(Host) && !thisHost.equals(Host)) continue;
            var subHostMap = matcherMap.get(thisHost);
            for (String thisSubHost : subHostMap.keySet()) {
                if(StrUtil.notBlank(subHost) && !thisSubHost.equals(subHost)) continue;
                var methodMap = subHostMap.get(thisSubHost);
                for (RequestMethod thisMethod : methodMap.keySet()) {
                    if(method != null && !thisMethod.equals(method)) continue;
                    PathTemplateMatcher<Action> pathTemplateMatcher = methodMap.get(thisMethod);
                    pathTemplateMatcher.remove(urlPath);
                }
            }
        }
        return this;
    }

    public void addMappingByAnnotation(Class<?> clazz) {
        RequestMapping mapping = clazz.getAnnotation(RequestMapping.class);
        if (mapping == null) return;

        if(!Controller.class.isAssignableFrom(clazz)){
            if(logger.isErrorEnabled())
                logger.error("Class " + clazz.getCanonicalName() + " marked as Controller, but not extended from Controller.class.");
            return;
        }

        String[] values = ConfigInjector.getStringValue(mapping.value());
        if (values == null) return;

        String viewPath = ConfigInjector.getStringValue(mapping.viewPath());
        String[] host = ConfigInjector.getStringValue(mapping.host());
        String[] subHost = ConfigInjector.getStringValue(mapping.subHost());

        RouteConfig thisConfig = new RouteConfig();
        thisConfig.setHost(host);
        thisConfig.setSubHost(subHost);
        if (StrUtil.notBlank(viewPath)) {
            for(String value : values)
                thisConfig.add(value, (Class<Controller>)clazz, viewPath);
        } else {
            for(String value : values)
                thisConfig.add(value, (Class<Controller>)clazz);
        }
        buildActionMapping(thisConfig);
        routes.add(thisConfig);
    }

    protected synchronized void buildActionMapping(RouteConfig routes) {
        for (RouteConfig.Route route : routes.getRouteItemList()) {
            Class<? extends Controller> controllerClass = route.getControllerClass();

            boolean declaredMethods = routes.getMappingSuperClass()
                    ? controllerClass.getSuperclass() == Controller.class
                    : true;

            Method[] methods = (declaredMethods ? controllerClass.getDeclaredMethods() : controllerClass.getMethods());
            for (Method method : methods) {
                if (declaredMethods) {
                    if (!Modifier.isPublic(method.getModifiers()))
                        continue;
                } else {
                    Class<?> dc = method.getDeclaringClass();
                    if (dc == Controller.class || dc == Object.class)
                        continue;
                }
                if(!void.class.equals(method.getReturnType()) && !Result.class.equals(method.getReturnType())){
                    throw new RuntimeException("Return type of methods for controller can only be void or Result type: " + method.getReturnType().getCanonicalName() + " " + ClassUtil.getMethodSignature(method));
                }

                if(route.getControllerKey() == null && StrUtil.notBlank(route.getUrlPath())){
                    if(StrUtil.notBlank(route.getControllerMethod()) && (!method.getName().equals(route.getControllerMethod()) || method.getParameterCount() != 0)) continue;
                    mappingWithoutAnnotation(controllerClass, method, routes, route);
                } else {
                    mappingByAnnotation(controllerClass, method, routes, route);
                }
            }
        }
    }

    private void mappingWithoutAnnotation(Class<? extends Controller> controllerClass, Method method, RouteConfig routes, RouteConfig.Route route){

        List<String> hostList = routes.getHost();
        List<String> subHostList = routes.getSubHost();
        if (hostList.size() == 0) {
            hostList = this.routeConfig.getHost();
            subHostList = this.routeConfig.getSubHost();
        }
        if (hostList.size() == 0) {
            hostList.add("*");
            subHostList.clear(); 
            subHostList.add("*");
        }

        InterceptorManager interceptorManager = context.coreContext.getInterceptorManager();
        Interceptor[] controllerInterceptors = interceptorManager.createControllerInterceptor(controllerClass);
        Interceptor[] actionInterceptors = interceptorManager.buildControllerActionInterceptor(routes.getInterceptors(), controllerInterceptors, controllerClass, method);

        String methodName = method.getName();
        RequestMethod[] requestMethods = route.getRequestMethods();
        RequestMapping.Produces produces = route.getProduces();
        if(produces == null) produces = routes.getProduces();

        String controllerKey = route.getUrlPath();
        String baseViewPath = StrUtil.notBlank(routes.getBaseViewPath()) ? routes.getBaseViewPath() : this.routeConfig.getBaseViewPath();
        String viewPath = route.getFinalViewPath(baseViewPath);

        for (String host : hostList) {
            host = host.trim().toLowerCase(); verifyHost(host);
            Map<String, Map<RequestMethod, PathTemplateMatcher<Action>>> subHostMap = matcherMap.containsKey(host) ? matcherMap.get(host) : new HashMap<>();
            for (String subHost : subHostList) {
                subHost = subHost.trim().toLowerCase(); verifySubHost(subHost);
                Map<RequestMethod, PathTemplateMatcher<Action>> methodMap = subHostMap.containsKey(subHost) ? subHostMap.get(subHost) : new HashMap<>();
                for (RequestMethod requestMethod : requestMethods) {
                    PathTemplateMatcher<Action> pathTemplateMatcher = methodMap.containsKey(requestMethod)
                            ? methodMap.get(requestMethod)
                            : new PathTemplateMatcher<>();

                    String actionKey;
                    if(StrUtil.notBlank(route.getControllerMethod())){
                        actionKey = controllerKey;
                    }else {
                        if (methodName.equals("index")) {
                            actionKey = controllerKey;
                        } else {
                            actionKey = controllerKey.equals(SLASH) ? SLASH + methodName : controllerKey + SLASH + methodName;
                        }
                    }
                    actionKey = actionKey.replaceAll(SLASH + "+", SLASH);
                    checkReservedPath(actionKey);
                    Action action = new Action(controllerKey, actionKey, controllerClass, method, methodName, actionInterceptors, produces, viewPath);

                    Action mapped = pathTemplateMatcher.get(actionKey);
                    if (mapped != null) {
                        pathTemplateMatcher.remove(actionKey);
                        if(logger.isWarnEnabled())
                            logger.warn("The url path %s is already mapped to %s.%s by annotation, this is going to override it, mapping to %s.%s",
                                    actionKey, mapped.getControllerClass().getName(), mapped.getMethodName(),
                                                controllerClass.getName(), method.getName());
                    }
                    pathTemplateMatcher.add(actionKey, action);

                    methodMap.put(requestMethod, pathTemplateMatcher);
                    if(logger.isInfoEnabled())
                        logger.info("The url path %s%s:%s is mapped to %s.%s", !host.equals("*") ? subHost + "." + host + ":" : "", requestMethod, actionKey, controllerClass.getName(), method.getName());
                }
                subHostMap.put(subHost, methodMap);
            }
            matcherMap.put(host, subHostMap);
        }
    }

    private void mappingByAnnotation(Class<? extends Controller> controllerClass, Method method, RouteConfig routes, RouteConfig.Route route){

        RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
        if (requestMapping == null){
            return;
        }

        InterceptorManager interceptorManager = context.coreContext.getInterceptorManager();
        Interceptor[] controllerInterceptors = interceptorManager.createControllerInterceptor(controllerClass);
        Interceptor[] actionInterceptors = interceptorManager.buildControllerActionInterceptor(routes.getInterceptors(), controllerInterceptors, controllerClass, method);

        String methodName = method.getName();
        String controllerKey = route.getControllerKey();

        List<String> hostList = routes.getHost();
        List<String> subHostList = routes.getSubHost();
        if (hostList.size() == 0) {
            hostList = this.routeConfig.getHost();
            subHostList = this.routeConfig.getSubHost();
        }
        if (hostList.size() == 0) {
            hostList.add("*");
            subHostList.clear(); 
            subHostList.add("*");
        }

        RequestMethod[] requestMethods = requestMapping.method();
        String[] actionKeys = ConfigInjector.getStringValue(requestMapping.value());
        RequestMapping.Produces produces = requestMapping.produces();

        String baseViewPath = StrUtil.notBlank(routes.getBaseViewPath()) ? routes.getBaseViewPath() : this.routeConfig.getBaseViewPath();
        String viewPath = ConfigInjector.getStringValue(requestMapping.viewPath());
        viewPath = StrUtil.isBlank(viewPath) ? route.getFinalViewPath(baseViewPath) :
                (StrUtil.isBlank(baseViewPath) ? viewPath : baseViewPath + viewPath);

        for (String host : hostList) {
            host = host.trim().toLowerCase(); verifyHost(host);
            Map<String, Map<RequestMethod, PathTemplateMatcher<Action>>> subHostMap = matcherMap.containsKey(host) ? matcherMap.get(host) : new HashMap<>();
            for (String subHost : subHostList) {
                subHost = subHost.trim().toLowerCase(); verifySubHost(subHost);
                Map<RequestMethod, PathTemplateMatcher<Action>> methodMap = subHostMap.containsKey(subHost) ? subHostMap.get(subHost) : new HashMap<>();
                for (RequestMethod requestMethod : requestMethods) {
                    PathTemplateMatcher<Action> pathTemplateMatcher = methodMap.containsKey(requestMethod)
                            ? methodMap.get(requestMethod)
                            : new PathTemplateMatcher<>();

                    if (actionKeys.length > 0) {
                        for (String actionKey : actionKeys) {

                            if (StrUtil.isBlank(actionKey))
                                throw new IllegalArgumentException(controllerClass.getName() + "." + methodName + "(): The path of RequestMapping can not be blank.");

                            if (!actionKey.startsWith(SLASH))
                                actionKey = SLASH + actionKey;

                            actionKey = controllerKey + actionKey;
                            actionKey = actionKey.replaceAll(SLASH + "+", SLASH);
                            checkReservedPath(actionKey);

                            Action action = new Action(controllerKey, actionKey, controllerClass, method, methodName, actionInterceptors, produces, viewPath);
                            if (pathTemplateMatcher.get(actionKey) == null) {
                                pathTemplateMatcher.add(actionKey, action);
                                if(logger.isInfoEnabled())
                                    logger.info("The url path %s%s:%s is mapped to %s.%s", !host.equals("*") ? subHost + "." + host + ":" : "", requestMethod, actionKey, controllerClass.getName(), method.getName());
                            } else {
                                throw new RuntimeException(buildMsg(actionKey, controllerClass, method));
                            }
                        }
                    } else {
                        String actionKey;
                        if (methodName.equals("index")) {
                            actionKey = controllerKey;
                        } else {
                            actionKey = controllerKey.equals(SLASH) ? SLASH + methodName : controllerKey + SLASH + methodName;
                        }
                        actionKey = actionKey.replaceAll(SLASH + "+", SLASH);
                        checkReservedPath(actionKey);

                        Action action = new Action(controllerKey, actionKey, controllerClass, method, methodName, actionInterceptors, produces, viewPath);
                        if (pathTemplateMatcher.get(actionKey) == null) {
                            pathTemplateMatcher.add(actionKey, action);
                            if(logger.isInfoEnabled())
                                logger.info("The url path %s%s:%s is mapped to %s.%s", !host.equals("*") ? subHost + "." + host + ":" : "", requestMethod, actionKey, controllerClass.getName(), method.getName());
                        } else {
                            throw new RuntimeException(buildMsg(actionKey, controllerClass, method));
                        }
                    }
                    methodMap.put(requestMethod, pathTemplateMatcher);
                }
                subHostMap.put(subHost, methodMap);
            }
            matcherMap.put(host, subHostMap);
        }

    }

    public static void verifyHost(String host){
        if(StrUtil.isBlank(host)) throw new RuntimeException("host name is empty");
        if(!StrUtil.isHostName(host, true) && !host.equals("*")){
            throw new RuntimeException("host name '"+ host +"' is invalid");
        }
    }

    public static void verifySubHost(String subHost){
        if(StrUtil.isBlank(subHost)) throw new RuntimeException("subHost name is empty");
        String forCheck = subHost;
        if(forCheck.startsWith("*")) forCheck = forCheck.substring(1);
        if(forCheck.endsWith("*")) forCheck = forCheck.substring(0,forCheck.length() - 1);
        if(!StrUtil.isHostName(forCheck, true) && !subHost.equals("*")){
            throw new RuntimeException("subHost name '"+ subHost +"' is invalid");
        }
    }

    private List<RouteConfig> getRoutesList(RouteConfig routes) {
        List<RouteConfig> routesList = routes.getRoutesList();
        List<RouteConfig> ret = new ArrayList<RouteConfig>(routesList.size() + 1);
        ret.add(routes);
        ret.addAll(routesList);
        return ret;
    }

    public Action getAction(HttpRequest request, String requestPath) {
        Action action = null;
        String hostName = request.getHostName().toLowerCase();

        for (String host : matcherMap.keySet()) {
            if (host.equals("*") || hostName.endsWith(host)) {
                var subHostMap = matcherMap.get(host);
                for (String subHost : subHostMap.keySet()) {
                    boolean matched = false;
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
                        var methodMap = subHostMap.get(subHost);
                        PathTemplateMatcher<Action> pathTemplateMatcher = methodMap.get(request.getMethod());
                        if (pathTemplateMatcher != null) {
                            PathTemplateMatcher.PathMatchResult<Action> result = pathTemplateMatcher
                                    .match(requestPath);
                            if (result != null) {
                                request.getExchange().putAttachment(ATTACHMENT_KEY,
                                        new io.undertow.util.PathTemplateMatch(result.getMatchedTemplate(), result.getParameters()));
                                for (Map.Entry<String, String> entry : result.getParameters().entrySet()) {
                                    request.getExchange().addPathParam(entry.getKey(), entry.getValue());
                                }
                                action = result.getValue();
                                return action;
                            }
                        }
                    }
                }
            }
        }
        return action;
    }

    protected String buildMsg(String actionKey, Class<? extends Controller> controllerClass, Method method) {
        StringBuilder sb = new StringBuilder("The action \"")
                .append(controllerClass.getName()).append(".")
                .append(method.getName()).append("()\" can not be mapped, ")
                .append("actionKey \"").append(actionKey).append("\" is already in use.");

        String msg = sb.toString();
        System.err.println("\nException: " + msg);
        return msg;
    }

    public void checkReservedPath(String path) {
        
        var reserved = List.of(context.application.applicationConfig().getServer().getHealthCheckPath()); 
        
        if(reserved.contains(path)) {
            throw new Error(path + " is a reserved path");
        }
    }
}
