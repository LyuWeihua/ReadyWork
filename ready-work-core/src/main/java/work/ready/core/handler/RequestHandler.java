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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import io.undertow.util.HeaderMap;
import io.undertow.util.StatusCodes;
import work.ready.core.aop.Invocation;
import work.ready.core.event.Event;
import work.ready.core.event.GeneralEvent;
import work.ready.core.exception.ApiException;
import work.ready.core.exception.IllegalArgumentException;
import work.ready.core.handler.action.*;
import work.ready.core.handler.request.HttpRequest;
import work.ready.core.handler.request.RequestParser;
import work.ready.core.handler.response.HttpResponse;
import work.ready.core.handler.route.RequestMapping;
import work.ready.core.handler.session.*;
import work.ready.core.handler.websocket.WebSocketHandler;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.module.ApplicationContext;
import work.ready.core.render.ErrorRender;
import work.ready.core.render.Render;
import work.ready.core.render.RenderException;
import work.ready.core.render.RenderManager;
import work.ready.core.security.CurrentUser;
import work.ready.core.security.UserIdentity;
import work.ready.core.server.ApplicationConfig;
import work.ready.core.server.Ready;
import work.ready.core.component.i18n.I18n;
import work.ready.core.component.i18n.I18nServerModule;
import work.ready.core.component.i18n.Res;
import work.ready.core.service.result.Result;
import work.ready.core.service.status.Status;
import work.ready.core.tools.ClassUtil;
import work.ready.core.tools.StrUtil;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class RequestHandler extends BaseHandler {
    private static final Log logger = LogFactory.getLog(RequestHandler.class);

    private static final String INVALID_REQUEST_PATH = "ERROR10007";

    public static final AttachmentKey<CurrentUser<? extends UserIdentity>> currentUser = AttachmentKey.create(CurrentUser.class);
    public final RequestParser requestParser = new RequestParser();

    protected final ApplicationContext context;
    protected SessionManager sessionManager;

    protected final ControllerFactory controllerFactory;
    protected final RenderManager renderManager;
    protected final boolean devMode;
    protected final boolean singletonController;

    protected WebSocketHandler webSocketHandler;
    protected List<RequestInterceptor> interceptors;

    public RequestHandler addInterceptor(RequestInterceptor interceptor) {
        if(interceptors == null) {
            interceptors = new CopyOnWriteArrayList<>();
        }
        interceptors.add(interceptor);
        return this;
    }

    @FunctionalInterface
    public interface RequestInterceptor {
        void intercept(HttpServerExchange httpServerExchange);
    }

    public RequestHandler(ApplicationContext context) {
        this.context = context;
        setManager(context.handlerManager);
        setApplicationConfig(Ready.getApplicationConfig(context.application.getName()));
        webSocketHandler = context.webSocketManager.getWebSocketHandler();
        if(applicationConfig.isEnableSession()) {
            sessionConfig();
        }
        singletonController = applicationConfig.getServer().isSingletonController();

        this.controllerFactory = new ControllerFactory();
        this.controllerFactory.setInjectDependency(true);
        renderManager = context.renderManager;
        devMode = Ready.getBootstrapConfig().isDevMode();
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        if(interceptors != null) {
            interceptors.forEach(i->i.intercept(exchange));
        }

        handle(exchange);
    }

    private void handle(HttpServerExchange exchange) {
        HttpRequest request = new HttpRequest(exchange, context);
        try {
            requestParser.parse(request, exchange);

            CurrentUser<? extends UserIdentity> user = exchange.getAttachment(currentUser);
            if(user != null) {
                Ready.beanManager().setCurrentUser(user);
            }

            if(applicationConfig.isEnableSession()){
                request.session = sessionManager.getSession(exchange);  
                if (request.session == null) {
                    request.session = sessionManager.createSession(exchange);
                }
            }

            HeaderMap headers = exchange.getRequestHeaders();
            if (webSocketHandler != null && webSocketHandler.checkWebSocket(request.getMethod(), headers)) {
                webSocketHandler.handle(exchange, request);
                return;
            }

            HttpResponse response = new HttpResponse(exchange);
            String target = request.getRequestURI();
            Action action = context.routeManager.getAction(request, target);
            if (action == null) {
                if (logger.isWarnEnabled()) {
                    String qs = request.getQueryString();
                    logger.warn("404 Action Not Found: " + (qs == null ? target : target + "?" + qs));
                }
                renderManager.getRenderFactory().getErrorRender(new Status(INVALID_REQUEST_PATH, target)).setContext(request, response).render();
                return;
            }

            Controller controller = null;
            Res i18nRes = exchange.getAttachment(I18nServerModule.i18n);
            try {
                if(singletonController) {
                    controller = Ready.beanManager().get(action.getControllerClass());
                } else {
                    controller = controllerFactory.getController(action.getControllerClass());
                }
                controller._init_(action, request, response);
                controller.setRenderManager(renderManager);

                if(user != null) {
                    controller.setAttrs(user.getAttributes());
                }
                controller.setAttr(I18n.localeParamName, i18nRes);

                Result result = null;
                if (devMode) {
                    if (ActionReporter.isReportAfterInvocation(request)) {
                        result = (Result) new Invocation(action, controller).invoke();
                        ActionReporter.report(target, controller, action);
                    } else {
                        ActionReporter.report(target, controller, action);
                        result = (Result) new Invocation(action, controller).invoke();
                    }
                }
                else {
                    result = (Result) new Invocation(action, controller).invoke();
                }

                if(applicationConfig.isEnableSession()){
                    if(sessionManager.getSessionRepository().getSessionFlushMode().equals(SessionFlushMode.ON_SAVE)){
                        sessionManager.getSessionRepository().save(request.getSession());
                    }
                }

                Render render = controller.getRender();
                if (render == null) {
                    if(result.isSuccess()) {
                        if(result.getResult() instanceof Status) {
                            ((Status) result.getResult()).setI18n(i18nRes);
                        }
                    } else {
                        if(result.getError() != null) {
                            result.getError().setI18n(i18nRes);
                        }
                    }
                    if(RequestMapping.Produces.Json.equals(action.getProduces())) {
                        render = renderManager.getRenderFactory().getJsonRender(result);
                    } else if(RequestMapping.Produces.Xml.equals(action.getProduces())) {
                        render = renderManager.getRenderFactory().getXmlRender(result);
                    } else { 
                        render = renderManager.getRenderFactory().getDefaultRender(action.getViewPath() + action.getMethodName());
                    }
                }
                render.setContext(request, response, action.getViewPath()).render();
            }
            catch (RenderException e) {
                if (logger.isErrorEnabled()) {
                    String qs = request.getQueryString();
                    logger.error(e, qs == null ? target : target + "?" + qs);
                }
            }
            catch (Exception e) {
                handleException(target, request, response, action, i18nRes, e);
            } finally {
                controllerFactory.recycle(controller);
            }
        } catch (Throwable e) {
            if (logger.isErrorEnabled()) {
                logger.error(e, request.toString());
            }
            throw new RuntimeException("Unexpected runtime exception", e);
        }
    }

    private void handleException(String target, HttpRequest request, HttpResponse response, Action action, Res i18nRes, Exception e) throws JsonProcessingException {
        final String RUNTIME_EXCEPTION = "ERROR10010";
        final String GENERIC_EXCEPTION = "ERROR10014";
        if (logger.isErrorEnabled()) {
            String qs = request.getQueryString();
            String targetInfo = (qs == null ? target : target + "?" + qs);
            String sign = ClassUtil.getMethodSignature(action.getMethod());
            logger.error(e,sign + " : " + targetInfo);
        }
        if(e instanceof ApiException) { 
            Status status = ((ApiException) e).getStatus().setI18n(i18nRes);
            if (RequestMapping.Produces.Json.equals(action.getProduces())) {
                renderManager.getRenderFactory().getJsonRender(status).setContext(request, response).render();
            } else if (RequestMapping.Produces.Xml.equals(action.getProduces())) {
                renderManager.getRenderFactory().getXmlRender(status).setContext(request, response).render();
            } else {
                ((ErrorRender)renderManager.getRenderFactory().getErrorRender(status.getHttpCode()).setContext(request, response, action.getViewPath())).render(status.getDescription());
            }
        } else if(e instanceof IllegalArgumentException){ 
            if(StrUtil.isValidStatusCode(e.getMessage())){
                Status status = new Status(e.getMessage(), ((IllegalArgumentException) e).getArgs()).setI18n(i18nRes);
                if (RequestMapping.Produces.Json.equals(action.getProduces())) {
                    renderManager.getRenderFactory().getJsonRender(status).setContext(request, response).render();
                } else if (RequestMapping.Produces.Xml.equals(action.getProduces())) {
                    renderManager.getRenderFactory().getXmlRender(status).setContext(request, response).render();
                } else {
                    ((ErrorRender)renderManager.getRenderFactory().getErrorRender(status.getHttpCode()).setContext(request, response, action.getViewPath())).render(status.getDescription());
                }
            } else {
                Status status = new Status(GENERIC_EXCEPTION, e.getMessage()); 
                if (RequestMapping.Produces.Json.equals(action.getProduces())) {
                    renderManager.getRenderFactory().getJsonRender(status).setContext(request, response).render();
                } else if (RequestMapping.Produces.Xml.equals(action.getProduces())) {
                    renderManager.getRenderFactory().getXmlRender(status).setContext(request, response).render();
                } else {
                    ((ErrorRender)renderManager.getRenderFactory().getErrorRender(status.getHttpCode()).setContext(request, response, action.getViewPath())).render(e.getMessage());
                }
            }
        } else { 
            Status status = new Status(RUNTIME_EXCEPTION).setI18n(i18nRes);
            if(RequestMapping.Produces.Json.equals(action.getProduces())) {
                renderManager.getRenderFactory().getJsonRender(status).setContext(request, response).render();
            } else if(RequestMapping.Produces.Xml.equals(action.getProduces())) {
                ((ErrorRender)renderManager.getRenderFactory().getErrorRender(StatusCodes.INTERNAL_SERVER_ERROR).setContext(request, response)).render(Ready.config().getXmlMapper().writeValueAsString(status));
            } else {
                renderManager.getRenderFactory().getErrorRender(StatusCodes.INTERNAL_SERVER_ERROR).setContext(request, response, action.getViewPath()).render();
            }
        }
    }

    public WebSocketHandler getWebSocketHandler() {
        return webSocketHandler;
    }

    private void sessionConfig(){
        Ready.post(new GeneralEvent(Event.SESSION_MANAGER_BEFORE_INIT, this, applicationConfig));
        if(this.sessionManager == null) {
            CacheSessionRepository sessionRepository = new CacheSessionRepository(Ready.cacheManager().getCache());
            sessionRepository.setDefaultMaxInactiveInterval(applicationConfig.getSessionMaxInactiveInterval());
            this.sessionManager = new CacheSessionManager(
                    Ready.beanManager().get(SessionCookieConfig.class),
                    sessionRepository);
        }
        webSocketHandler.setSessionManager(this.sessionManager);
        Ready.post(new GeneralEvent(Event.SESSION_MANAGER_AFTER_INIT, this, applicationConfig));
    }

    public void setSessionManager(SessionManager manager) {
        sessionManager = manager;
    }

    public SessionManager getSessionManager(){
        return sessionManager;
    }

}
