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

import work.ready.core.handler.HandlerManager;
import work.ready.core.handler.route.RouteManager;
import work.ready.core.handler.websocket.WebSocketManager;
import work.ready.core.render.RenderManager;
import work.ready.core.security.SecurityManager;
import work.ready.core.server.*;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class ApplicationContext {
    public final CoreContext coreContext;
    public final HandlerManager handlerManager;
    public final SecurityManager securityManager;
    public final RouteManager routeManager;
    public final RenderManager renderManager;
    public final WebSocketManager webSocketManager;
    public final List<Task> startupHook = new ArrayList<>();
    public final ShutdownHook shutdownHook;
    public final WebServer webServer;
    public final Application application;

    private BackgroundTaskExecutor backgroundTask;

    public ApplicationContext(CoreContext context, Application app) {
        application = app;
        coreContext = context;
        shutdownHook = new ShutdownHook(Ready.shutdownHook, application);

        ServiceLoader<ApplicationContextInitializer> appInitializerLoader = ServiceLoader.load(ApplicationContextInitializer.class, ApplicationContext.class.getClassLoader());
        ApplicationContextInitializer initializer = appInitializerLoader.findFirst().orElse(new DefaultApplicationInitializer());
        initializer.setContext(this);

        initializer.createBegin();

        handlerManager = initializer.createHandlerManager();

        securityManager = initializer.createSecurityManager();

        routeManager = initializer.createRouteManager();

        renderManager = initializer.createRenderManager();

        webSocketManager = initializer.createWebSocketManager();

        webServer = initializer.createWebServer();

        initializer.createEnd();

        if(!Ready.isMultiAppMode()){
            coreContext.beanManager.addSingletonObject(handlerManager);
            coreContext.beanManager.addSingletonObject(securityManager);
            coreContext.beanManager.addSingletonObject(routeManager);
            coreContext.beanManager.addSingletonObject(renderManager);
            coreContext.beanManager.addSingletonObject(webSocketManager);
            coreContext.beanManager.addSingletonObject(webServer);
            coreContext.beanManager.addSingletonObject(application);
            coreContext.beanManager.addSingletonObject(this);
        }

        startupHook.add(webServer::start);
        shutdownHook.add(ShutdownHook.STAGE_0, timeout -> webServer.shutdown());
        shutdownHook.add(ShutdownHook.STAGE_1, webServer::awaitRequestCompletion);
        shutdownHook.add(ShutdownHook.STAGE_9, timeout -> {webServer.awaitTermination();Ready.appDestroyed(application);});
    }

    public BackgroundTaskExecutor backgroundTask() {
        if (backgroundTask == null) {
            backgroundTask = new BackgroundTaskExecutor();
            startupHook.add(backgroundTask::start);
            shutdownHook.add(ShutdownHook.STAGE_2, timeoutInMs -> backgroundTask.shutdown());
            shutdownHook.add(ShutdownHook.STAGE_3, backgroundTask::awaitTermination);
        }
        return backgroundTask;
    }

    public void validate() {

    }
}
