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

package work.ready.core.module;

import work.ready.core.event.Event;
import work.ready.core.event.GeneralEvent;
import work.ready.core.handler.HandlerManager;
import work.ready.core.handler.route.RouteManager;
import work.ready.core.handler.websocket.WebSocketManager;
import work.ready.core.render.RenderManager;
import work.ready.core.security.SecurityManager;
import work.ready.core.server.Ready;
import work.ready.core.server.WebServer;

public class DefaultApplicationInitializer implements ApplicationContextInitializer {

    private ApplicationContext context;

    @Override
    public void setContext(ApplicationContext context){
        this.context = context;
    }

    @Override
    public ApplicationContext getContext() {
        return this.context;
    }

    @Override
    public void createBegin(){
        Ready.post(new GeneralEvent(Event.APP_INITIALIZER_CREATE_BEGIN, this));
    }

    @Override
    public HandlerManager createHandlerManager() {
        GeneralEvent beforeEvent = new GeneralEvent(Event.HANDLER_MANAGER_BEFORE_CREATE, this);
        Ready.post(beforeEvent);
        HandlerManager handlerManager = beforeEvent.getObject() instanceof HandlerManager ? beforeEvent.getObject() : new HandlerManager(context);
        GeneralEvent afterEvent = new GeneralEvent(Event.HANDLER_MANAGER_AFTER_CREATE, this, handlerManager);
        Ready.post(afterEvent);
        handlerManager = afterEvent.getObject() instanceof HandlerManager ? afterEvent.getObject() : handlerManager;
        return handlerManager;
    }

    @Override
    public SecurityManager createSecurityManager() {
        GeneralEvent beforeEvent = new GeneralEvent(Event.SECURITY_MANAGER_BEFORE_CREATE, this);
        Ready.post(beforeEvent);
        SecurityManager securityManager = beforeEvent.getObject() instanceof SecurityManager ? beforeEvent.getObject() : new SecurityManager(context);
        GeneralEvent afterEvent = new GeneralEvent(Event.SECURITY_MANAGER_AFTER_CREATE, this, securityManager);
        Ready.post(afterEvent);
        securityManager = afterEvent.getObject() instanceof SecurityManager ? afterEvent.getObject() : securityManager;
        return securityManager;
    }

    @Override
    public RouteManager createRouteManager() {
        GeneralEvent beforeEvent = new GeneralEvent(Event.ROUTE_MANAGER_BEFORE_CREATE, this);
        Ready.post(beforeEvent);
        RouteManager routeManager = beforeEvent.getObject() instanceof RouteManager ? beforeEvent.getObject() : new RouteManager(context);
        GeneralEvent afterEvent = new GeneralEvent(Event.ROUTE_MANAGER_AFTER_CREATE, this, routeManager);
        Ready.post(afterEvent);
        routeManager = afterEvent.getObject() instanceof RouteManager ? afterEvent.getObject() : routeManager;
        return routeManager;
    }

    @Override
    public RenderManager createRenderManager() {
        GeneralEvent beforeEvent = new GeneralEvent(Event.RENDER_MANAGER_BEFORE_CREATE, this);
        Ready.post(beforeEvent);
        RenderManager renderManager = beforeEvent.getObject() instanceof RenderManager ? beforeEvent.getObject() : new RenderManager(context);
        GeneralEvent afterEvent = new GeneralEvent(Event.RENDER_MANAGER_AFTER_CREATE, this, renderManager);
        Ready.post(afterEvent);
        renderManager = afterEvent.getObject() instanceof RenderManager ? afterEvent.getObject() : renderManager;
        return renderManager;
    }

    @Override
    public WebSocketManager createWebSocketManager() {
        GeneralEvent beforeEvent = new GeneralEvent(Event.WEB_SOCKET_MANAGER_BEFORE_CREATE, this);
        Ready.post(beforeEvent);
        WebSocketManager webSocketManager = beforeEvent.getObject() instanceof WebSocketManager ? beforeEvent.getObject() : new WebSocketManager(context);
        GeneralEvent afterEvent = new GeneralEvent(Event.WEB_SOCKET_MANAGER_AFTER_CREATE, this, webSocketManager);
        Ready.post(afterEvent);
        webSocketManager = afterEvent.getObject() instanceof WebSocketManager ? afterEvent.getObject() : webSocketManager;
        return webSocketManager;
    }

    @Override
    public WebServer createWebServer() {
        GeneralEvent beforeEvent = new GeneralEvent(Event.WEB_SERVER_BEFORE_CREATE, this);
        Ready.post(beforeEvent);
        WebServer webServer = beforeEvent.getObject() instanceof WebServer ? beforeEvent.getObject() : new WebServer(context);
        GeneralEvent afterEvent = new GeneralEvent(Event.WEB_SERVER_AFTER_CREATE, this, webServer);
        Ready.post(afterEvent);
        webServer = afterEvent.getObject() instanceof WebServer ? afterEvent.getObject() : webServer;
        return webServer;
    }

    @Override
    public void createEnd(){
        Ready.post(new GeneralEvent(Event.APP_INITIALIZER_CREATE_END, this));
    }

}
