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

package work.ready.core.handler.websocket;

import io.undertow.util.PathTemplateMatcher;
import work.ready.core.config.ConfigInjector;
import work.ready.core.event.Event;
import work.ready.core.event.GeneralEvent;
import work.ready.core.handler.RequestMethod;
import work.ready.core.handler.route.RouteManager;
import work.ready.core.handler.session.SessionRepository;
import work.ready.core.ioc.annotation.WebSocketListener;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.module.ApplicationContext;
import work.ready.core.module.Initializer;
import work.ready.core.server.Ready;
import work.ready.core.tools.ClassUtil;
import work.ready.core.tools.StrUtil;

import java.lang.reflect.ParameterizedType;
import java.util.*;

import static work.ready.core.handler.route.RouteManager.SLASH;

public final class WebSocketManager {
    private static final Log logger = LogFactory.getLog(WebSocketManager.class);
    private final ApplicationContext context;
    private final BeanMappers beanMappers = new BeanMappers();
    private final WebSocketHandler webSocketHandler;
    protected List<Initializer<WebSocketManager>> initializers = new ArrayList<>();

    public WebSocketManager(ApplicationContext context) {
        this.context = context;
        this.webSocketHandler = new WebSocketHandler(this);
        if(!Ready.isMultiAppMode()){
            Ready.beanManager().addSingletonObject(WebSocketContext.class, webSocketHandler.context);
        }
        Ready.post(new GeneralEvent(Event.WEB_SOCKET_MANAGER_CREATE, this));
    }

    public void addInitializer(Initializer<WebSocketManager> initializer) {
        this.initializers.add(initializer);
        initializers.sort(Comparator.comparing(Initializer::order));
    }

    public void startInit() {
        try {
            for (Initializer<WebSocketManager> i : initializers) {
                i.startInit(this);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void endInit() {
        try {
            for (Initializer<WebSocketManager> i : initializers) {
                i.endInit(this);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void addListenerByAnnotation(Class<?> clazz){
        WebSocketListener annotation = clazz.getAnnotation(WebSocketListener.class);
        if(annotation == null) return;
        if (!ChannelListener.class.isAssignableFrom(clazz)){
            if(logger.isErrorEnabled())
                logger.error("Class " + clazz.getCanonicalName() + " marked as ChannelListener, but not extended from ChannelListener.class.");
            return;
        }
        Class<?> messageType = (Class<?>)((ParameterizedType)clazz.getGenericInterfaces()[0]).getActualTypeArguments()[0];
        String[] host = ConfigInjector.getStringValue(annotation.host());
        String[] subHost = ConfigInjector.getStringValue(annotation.subHost());
        RequestMethod[] requestMethods = annotation.method();
        String[] urls = ConfigInjector.getStringValue(annotation.value());

        var handler = getHandler(messageType, messageType, (ChannelListener<?>)Ready.beanManager().get(clazz));
        addListener(host, subHost, urls, requestMethods, handler);
    }

    public WebSocketHandler getWebSocketHandler(){
        return webSocketHandler;
    }

    public WebSocketContext getWebSocketContext() { return webSocketHandler.context; }

    public ChannelCallback getChannelCallback(){
        return Ready.beanManager().get(ChannelCallback.class);
    }

    private ChannelHandler getHandler(Class<?> clientMessageClass, Class<?> serverMessageClass, ChannelListener<?> listener){
        logger.info("webSocket, clientMessageClass=%s, serverMessageClass=%s, listener=%s",
                clientMessageClass.getCanonicalName(), serverMessageClass.getCanonicalName(), listener.getClass().getCanonicalName());

        BeanMapper<?> clientMessageMapper = null;
        if (!String.class.equals(clientMessageClass)) {
            clientMessageMapper = beanMappers.register(clientMessageClass);
        }
        BeanMapper<?> serverMessageMapper = null;
        if (!String.class.equals(serverMessageClass)) {
            serverMessageMapper = beanMappers.register(serverMessageClass);
        }
        return new ChannelHandler(clientMessageMapper, serverMessageClass, serverMessageMapper, listener);
    }

    public void addListener(String path, Class<? extends ChannelListener<String>> listener) {
        addListener(path, String.class, String.class, listener);
    }

    public <T, V> void addListener(String path, Class<T> clientMessageClass, Class<V> serverMessageClass, Class<? extends ChannelListener<T>> listener) {
        addListener(null, null, new String[]{path}, null,clientMessageClass, serverMessageClass, listener);
    }

    public <T, V> void addListener(String[] host, String[] subHost, String[] path, RequestMethod[] method, Class<T> clientMessageClass, Class<V> serverMessageClass, Class<? extends ChannelListener<T>> listener) {
        var handler = getHandler(clientMessageClass, serverMessageClass, Ready.beanManager().get(listener));
        addListener(host, subHost, path, method, handler);
    }

    public synchronized void addListener(String[] host, String[] subHost, String[] path, RequestMethod[] method, ChannelHandler handler) {
        Class<?> listenerClass = ClassUtil.getUserClass(handler.listener.getClass());
        if (listenerClass.isSynthetic())
            throw new Error("Listener class must not be anonymous class or lambda, listenerClass=" + handler.listener.getClass().getCanonicalName());

        if (path == null || path.length == 0)
            throw new IllegalArgumentException(listenerClass.getCanonicalName() + ": The path of WebSocketListener can not be blank.");
        if(host == null || host.length == 0) { host = new String[] {"*"}; subHost = new String[] {"*"}; }
        if(subHost == null || subHost.length == 0) subHost = new String[] {"*"};
        if(method == null || method.length == 0) method = new RequestMethod[] {RequestMethod.GET, RequestMethod.POST};

        for(int i = 0; i < host.length; i++){
            String hostName = host[i].trim().toLowerCase(); verifyHost(hostName);
            Map<String, Map<RequestMethod, PathTemplateMatcher<ChannelHandler>>> subHostMap = webSocketHandler.matcherMap.containsKey(hostName) ? webSocketHandler.matcherMap.get(hostName) : new HashMap<>();
            for(int j = 0; j < subHost.length; j++){
                String subHostName = subHost[j].trim().toLowerCase(); verifySubHost(subHostName);
                Map<RequestMethod, PathTemplateMatcher<ChannelHandler>> methodMap = subHostMap.containsKey(subHostName) ? subHostMap.get(subHostName) : new HashMap<>();
                for(int m = 0; m < method.length; m++) {
                    RequestMethod requestMethod = method[m];
                    PathTemplateMatcher<ChannelHandler> pathTemplateMatcher = methodMap.containsKey(requestMethod)
                            ? methodMap.get(requestMethod)
                            : new PathTemplateMatcher<>();
                    for (int k = 0; k < path.length; k++) {
                        String pathName = path[k].trim();
                        context.routeManager.checkReservedPath(pathName);
                        if (StrUtil.isBlank(pathName))
                            throw new IllegalArgumentException(listenerClass.getCanonicalName() + ": The path of WebSocketListener can not be blank.");

                        if (!pathName.startsWith(SLASH))
                            pathName = SLASH + pathName;
                        pathName = pathName.replaceAll(SLASH + "+", SLASH);
                        if (pathTemplateMatcher.get(pathName) == null) {
                            pathTemplateMatcher.add(pathName, handler);
                            if(logger.isInfoEnabled())
                                logger.info("The webSocket url path %s:%s is mapped to %s", requestMethod, pathName, listenerClass.getName());
                        } else {
                            throw new RuntimeException(listenerClass.getCanonicalName() + ":The path '" + pathName + "' for WebSocketListener is already in use.");
                        }
                    }
                    methodMap.put(requestMethod, pathTemplateMatcher);
                }
                subHostMap.put(subHostName, methodMap);
            }
            webSocketHandler.matcherMap.put(hostName, subHostMap);
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
}
