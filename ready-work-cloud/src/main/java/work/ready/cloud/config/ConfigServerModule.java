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

package work.ready.cloud.config;

import work.ready.cloud.ReadyCloud;
import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.config.source.ConfigFileSource;
import work.ready.cloud.registry.base.URLParam;
import work.ready.core.event.cloud.Event;
import work.ready.core.event.GeneralEvent;
import work.ready.core.handler.RequestMethod;
import work.ready.core.module.AppModule;
import work.ready.core.server.Constant;
import work.ready.core.server.Ready;
import work.ready.core.server.WebServer;
import work.ready.core.tools.define.Kv;

public class ConfigServerModule extends AppModule {

    public static final String name = "CONFIG_SERVER";
    public static final String version = "1.0.0.000000";
    public static final String healthCheck = "/config-server/health-check";

    @Override
    protected void initialize() {
        ensureDependsAvailable();
        if(!ReadyCloud.getConfig().getConfigServer().isEnabled()) {
            return;
        }
        Ready.post(new GeneralEvent(Event.CONFIG_SERVER_MODULE_BEFORE_INIT, this));
        handler().addHandler(new String[]{"/config-server/{"+ConfigFileSource.RESOURCE_TYPE+"}/{"+ ConfigFileSource.PROJECT_NAME+"}/{"+ConfigFileSource.PROJECT_VERSION+"}/{"+ConfigFileSource.APPLICATION_NAME+"}/{"+ConfigFileSource.APPLICATION_VERSION+"}/{"+ConfigFileSource.ENVIRONMENT+"}",
                                          "/config-server/{"+ConfigFileSource.RESOURCE_TYPE+"}/{"+ ConfigFileSource.PROJECT_NAME+"}/{"+ConfigFileSource.PROJECT_VERSION+"}/{"+ConfigFileSource.ENVIRONMENT+"}",
                                          healthCheck},
                new RequestMethod[]{RequestMethod.GET}, bean().get(ConfigServerServerModule.class));

        Ready.eventManager().addListener(this,"webServerStartListener", (setter)->{
            setter.addName(Event.WEB_SERVER_STARTED);
            setter.setAsync(true);
        });

        Ready.post(new GeneralEvent(Event.CONFIG_SERVER_MODULE_AFTER_INIT, this));
    }

    private void ensureDependsAvailable(){
        if(!ReadyCloud.isReady()){
            throw new RuntimeException("ConfigServerModule depends on Registry, please start server with cloud mode.");
        }
    }

    public void webServerStartListener(GeneralEvent event){ 
        WebServer webServer = event.getSender();
        if(ReadyCloud.isReady() && context.application.equals(webServer.getApplication())) {
            boolean isHealthCheck = ReadyCloud.getConfig().getConfigServer().isHealthCheck();
            if(webServer.getHttpPort() > 0){
                Cloud.getRegistry().register(Constant.NODE_TYPE_CONFIG, name, version, Constant.PROTOCOL_HTTP, webServer.getHttpPort(), Kv.by(URLParam.healthCheckPath.getName(), healthCheck).set(URLParam.healthCheck.getName(), String.valueOf(isHealthCheck)));
            } else {
                Cloud.getRegistry().register(Constant.NODE_TYPE_CONFIG, name, version, Constant.PROTOCOL_HTTPS, webServer.getHttpsPort(), Kv.by(URLParam.healthCheckPath.getName(), healthCheck).set(URLParam.healthCheck.getName(), String.valueOf(isHealthCheck)));
            }

            Ready.post(new GeneralEvent(Event.CONFIG_SERVER_MODULE_REGISTERED, this));
        }
    }
}
