/**
 *
 * Original work copyright (c) 2016 Network New Technologies Inc.
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
package work.ready.cloud.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.Handlers;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import work.ready.cloud.config.source.LocalFileSystemSource;
import work.ready.cloud.config.source.ConfigFileSource;
import work.ready.core.event.cloud.Event;
import work.ready.core.event.GeneralEvent;
import work.ready.core.handler.BaseHandler;
import work.ready.core.handler.ContentType;
import work.ready.core.handler.ServerModule;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Constant;
import work.ready.core.server.Ready;
import work.ready.core.service.status.Status;

import java.util.Deque;
import java.util.Map;

import static work.ready.cloud.config.source.ConfigFileSource.*;

public class ConfigServerServerModule extends ServerModule {
    private static final Log logger = LogFactory.getLog(ConfigServerServerModule.class);

    private volatile BaseHandler next;
    private static final ObjectMapper mapper = Ready.config().getJsonMapper();

    private String INVALID_AUTH_TOKEN = "ERROR10000";

    public ConfigServerServerModule() {
        setOrder(5);
        if(logger.isInfoEnabled()) logger.info("ConfigServerServerModule is loaded.");
    }

    @Override
    public void initialize() {
        Ready.post(new GeneralEvent(Event.CONFIG_SERVER_MODULE_SERVICE_INIT, this));
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {

        if(ConfigServerModule.healthCheck.equals(exchange.getRequestPath())){
            exchange.setStatusCode(StatusCodes.OK);
            exchange.getResponseSender().send(Constant.DEFAULT_HEALTH_RESPONSE);
            exchange.endExchange();
            return;
        }

        ConfigFileSource backend = new LocalFileSystemSource();
        
        final String authorization = exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION);
        String clientToken = backend.login(authorization);
        if(clientToken == null) {
            Status status = new Status(INVALID_AUTH_TOKEN);
            logger.debug(status.toString());
            exchange.setStatusCode(StatusCodes.UNAUTHORIZED);
            exchange.getResponseSender().send(status.toString());
            exchange.endExchange();
            return;
        }

        Map<String, Deque<String>> parameters = exchange.getPathParameters();
        Application application = new Application();
        String resourceType = parameters.get(RESOURCE_TYPE).getFirst();
        application.setProjectName(parameters.get(PROJECT_NAME).getFirst());
        application.setProjectVersion(parameters.get(PROJECT_VERSION).getFirst());
        
        if(parameters.get(APPLICATION_NAME) != null && parameters.get(APPLICATION_VERSION) != null) {
            application.setApplicationName(parameters.get(APPLICATION_NAME).getFirst());
            application.setApplicationVersion(parameters.get(APPLICATION_VERSION).getFirst());
        }
        application.setProfile(parameters.get(ENVIRONMENT).getFirst());

        ApplicationConfigs applicationConfigs = null;

        if(CERT.equals(resourceType)) {
            logger.debug("Application Certs requested for: %s", application);
            
            applicationConfigs = backend.getApplicationCertificates(clientToken, application);
        } else
        if(FILE.equals(resourceType)) {
            logger.debug("Application Files requested for: %s", application);
            
            applicationConfigs = backend.getApplicationFiles(clientToken, application); 
        } else
        if(CONFIG.equals(resourceType)) {
            logger.debug("Application Configs requested for: %s", application);
            
            applicationConfigs = backend.getApplicationConfigs(clientToken, application); 
        } else {
            logger.error("unsupported resource type of " + resourceType);
        }

        if (applicationConfigs != null && applicationConfigs.getConfigProperties()!= null) {
            exchange.setStatusCode(StatusCodes.OK);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
            exchange.getResponseSender().send(mapper.writeValueAsString(applicationConfigs));
        } else {
            logger.error("Could not read configs from the backend");
            exchange.getResponseSender().send(mapper.writeValueAsString(applicationConfigs));
            Status status = new Status(String.valueOf(StatusCodes.INTERNAL_SERVER_ERROR));
            String errorResp = mapper.writeValueAsString(status);
            exchange.setStatusCode(status.getHttpCode());
            exchange.getResponseSender().send(errorResp);
        }
        exchange.endExchange();
    }

    @Override
    public BaseHandler getNext() {
        return next;
    }

    @Override
    public ServerModule setNext(final BaseHandler next) {
        Handlers.handlerNotNull(next);
        this.next = next;
        return this;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void register() {
        manager.registerModule(ConfigServerServerModule.class.getName(), config);
    }
}
