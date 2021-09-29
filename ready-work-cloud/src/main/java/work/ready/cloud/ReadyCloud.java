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

package work.ready.cloud;

import io.undertow.util.Headers;
import work.ready.cloud.client.clevercall.CleverCallModule;
import work.ready.cloud.jdbc.oltp.JdbcUserOperationHandler;
import work.ready.cloud.registry.HealthClient;
import work.ready.core.aop.transformer.TransformerManager;
import work.ready.core.aop.transformer.match.InterceptorPoint;
import work.ready.core.apm.ApmManager;
import work.ready.core.apm.collector.http.client.HttpClientConfig;
import work.ready.core.apm.collector.http.server.WebServerConfig;
import work.ready.core.component.plugin.BaseCorePlugin;
import work.ready.core.event.cloud.Event;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.tools.HttpClient;
import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.cluster.CloudConfig;
import work.ready.cloud.config.ConfigLoader;
import work.ready.cloud.config.ConfigServerModule;
import work.ready.core.config.ConfigInjector;
import work.ready.core.event.GeneralEvent;
import work.ready.core.module.Application;
import work.ready.core.module.CoreContext;
import work.ready.core.module.ShutdownHook;
import work.ready.core.server.Constant;
import work.ready.core.server.Ready;
import work.ready.core.tools.StrUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map;

public class ReadyCloud extends BaseCorePlugin {

    private static final Log logger = LogFactory.getLog(ReadyCloud.class);
    private static final String name = "ReadyCloud";
    private static final String version = "0.6.6.210518";

    private static Cloud instance;
    private static CloudConfig config;
    private static Cloud.NodeType nodeType;
    private static Cloud.NodeMode nodeMode;
    private static boolean ready = false;
    private String configServerUri;
    private boolean configFetched = false;

    public static boolean isReady() {
        return ready;
    }

    public static Cloud getInstance() {
        return instance;
    }

    public static CloudConfig getConfig() {
        return config;
    }

    public static Cloud.NodeType getNodeType() {
        return nodeType;
    }

    public static Cloud.NodeMode getNodeMode() {
        return nodeMode;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public void bootstrap() {
        config = ConfigInjector.getConfigBean("${readyCloud}", CloudConfig.class, Constant.BOOTSTRAP_CONFIG_NAME, null);
        if(config == null) {
            config = ConfigInjector.getConfigBean("${readyCloud}", CloudConfig.class, Constant.BOOTSTRAP_CONFIG_NAME + "-" + Ready.getBootstrapConfig().getActiveProfile(), null);
        }
        if(config == null) {
            config = new CloudConfig();
        }

        configServerUri = Ready.getProperty(Constant.READY_WORK_CONFIG_SERVER_PROPERTY);
        if(StrUtil.notBlank(configServerUri) || (config.getConfigClient().isEnabled()) && !isConfigServer()) {
            if (StrUtil.isBlank(configServerUri)) {
                configServerUri = config.getConfigClient().getConfigServerUri();
            }
            
            if(StrUtil.notBlank(configServerUri)) {
                for(String eachServer : StrUtil.split(configServerUri, ';')) {
                    eachServer = eachServer.trim();
                    if (StrUtil.notBlank(eachServer))
                    {
                        if(Constant.DEFAULT_HEALTH_RESPONSE.equals(ConfigLoader.getConfigServerHealth(eachServer))){
                            loadFiles(eachServer);
                            Ready.config().clear();
                            Ready.config().getMapConfig(Constant.VALUES_CONFIG_NAME);
                            loadConfigs(eachServer);
                            configFetched = true;
                            Ready.post(new GeneralEvent(Event.CONFIG_LOADED_FROM_CONFIG_SERVER, this).put("server", eachServer));
                            break;
                        } else {
                            logger.error("Call config server %s failed.", eachServer);
                        }
                    }
                }
            }
        }
    }

    private boolean isConfigServer() {
        for(var appClass : Ready.getAppClass()) {
            if(Application.moduleExists(appClass, ConfigServerModule.class)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void config() {
        config = ConfigInjector.getConfigBean("${readyCloud}", CloudConfig.class, null, Ready.getMainApp().getName());
        if(config == null) {
            config = new CloudConfig();
        }

        var mainApp = Ready.getMainApp();
        Ready.getApp(appList-> appList.forEach(app->{
                if(app instanceof ReadyCloudConfig && !app.equals(mainApp)) {
                    ((ReadyCloudConfig) app).cloudConfig(config);
                }
            }));
        if(mainApp instanceof ReadyCloudConfig) {
            ((ReadyCloudConfig) mainApp).cloudConfig(config);
        } else { 
            try {
                Method configMethod = mainApp.getClass().getDeclaredMethod("cloudConfig", CloudConfig.class);
                configMethod.setAccessible(true);
                configMethod.invoke(mainApp, config);
            } catch (NoSuchMethodException e) { 
            } catch (IllegalAccessException e) {
                logger.error("The cloudConfig(CloudConfig config) method of %s is not accessible !", mainApp.getClass().getSimpleName());
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                logger.error("Exception caused by cloudConfig(CloudConfig config) method of %s !", mainApp.getClass().getSimpleName());
                throw new RuntimeException(e);
            }
        }

        config.validate();
        var annotation = mainApp.getClass().getAnnotation(EnableCloud.class);
        nodeType = annotation.nodeType();
        nodeMode = annotation.nodeMode();

        if(ApmManager.getConfig().isEnabled()) {
            ApmManager.getConfig(HttpClientConfig.class).setHeaderExclude(Headers.USER_AGENT_STRING, HealthClient.HEALTH_CLIENT_HEADER);
            ApmManager.getConfig(WebServerConfig.class).setHeaderExclude(Headers.USER_AGENT_STRING, HealthClient.HEALTH_CLIENT_HEADER);
        }

        if(config.getHttpClient().getProxy() != null) {
            HttpClient.setDefaultProxy(config.getHttpClient().getProxy());
        }
        if(config.getHttpClient().getTimeout() > 0) {
            HttpClient.setDefaultTimeout(Duration.ofMillis(config.getHttpClient().getTimeout()));
        }
        if(config.getHttpClient().getKeepAliveTimeout() > 0) {
            System.setProperty("jdk.httpclient.keepalive.timeout", String.valueOf(config.getHttpClient().getKeepAliveTimeout()));
        }
        if(config.getHttpClient().getConnectionPoolSize() > 0) {
            System.setProperty("jdk.httpclient.connectionPoolSize", String.valueOf(config.getHttpClient().getConnectionPoolSize()));
        }

        TransformerManager.getInstance().attach(Map.of("JdbcUserOperationExceptionHandler",
                new InterceptorPoint().setInterceptor(JdbcUserOperationHandler.class.getCanonicalName())
                        .setTypeInclude("named", "org.apache.ignite.internal.processors.authentication.IgniteAuthenticationProcessor")
                        .setMethodInclude("named", "checkUserOperation")));
    }

    @Override
    public void start() {
        startNode();
    }

    @Override
    public void initEnd(CoreContext context) {
        Ready.getMainApp().registerModule(CleverCallModule.class);
        Ready.shutdownHook.add(ShutdownHook.STAGE_7, timeout -> Cloud.close()); 
    }

    @Override
    public void stop() {
        if(instance != null) { 
            Cloud.getRegistry().stopHeartbeat();
        }
    }

    @Override
    public void destroy() {
        
    }

    private void startNode() {
        Ready.post(new GeneralEvent(Event.READY_WORK_CLOUD_BEFORE_INIT, this));

        instance = new Cloud(config);

        if(config.getConfigClient().isEnabled()) {
            
            if (!configFetched) {
                configServerUri = Cloud.discoverConfigNode(Constant.PROTOCOL_DEFAULT, ConfigServerModule.name);
                if (configServerUri != null) {
                    if (Constant.DEFAULT_HEALTH_RESPONSE.equals(ConfigLoader.getConfigServerHealth(configServerUri))) {
                        loadFiles(configServerUri);
                        loadConfigs(configServerUri);
                        configFetched = true;
                        Ready.post(new GeneralEvent(Event.CONFIG_LOADED_FROM_CONFIG_SERVER, this).put("server", configServerUri));
                    } else {
                        logger.error("Call config server %s failed.", configServerUri);
                    }
                }
            }
        }
        ready = true;
        Ready.post(new GeneralEvent(Event.READY_WORK_CLOUD_AFTER_INIT, this, instance));
    }

    private void loadFiles(String configServerUri) {
        String clientId = config.getConfigClient().getClientId();
        String clientSecret = config.getConfigClient().getClientSecret();

        ConfigLoader loader = new ConfigLoader(configServerUri, clientId, clientSecret);
        try {
            loader.fetchGlobalFiles();
            for (Class<? extends Application> app : Ready.getAppClass()) {
                loader.fetchFiles(app);
            }
        } finally {
            Ready.config().reLocateConfigRepository();  
        }
    }

    private void loadConfigs(String configServerUri) {
        String clientId = config.getConfigClient().getClientId();
        String clientSecret = config.getConfigClient().getClientSecret();

        ConfigLoader loader = new ConfigLoader(configServerUri, clientId, clientSecret);
        loader.fetchGlobalConfigs();
        for (Class<? extends Application> app : Ready.getAppClass()) {
            loader.fetchConfigs(app);
        }
    }
}
