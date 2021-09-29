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

package work.ready.core.apm;

import work.ready.core.aop.transformer.TransformerManager;
import work.ready.core.aop.transformer.enhance.Interceptor;
import work.ready.core.apm.common.JvmInfoTask;
import work.ready.core.apm.model.CollectorConfig;
import work.ready.core.apm.reporter.ReporterManager;
import work.ready.core.config.Config;
import work.ready.core.event.Event;
import work.ready.core.event.GeneralEvent;
import work.ready.core.ioc.BeanManager;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.module.Initializer;
import work.ready.core.module.ShutdownHook;
import work.ready.core.server.Ready;
import work.ready.core.tools.ClassUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static work.ready.core.tools.ClassUtil.getDefaultClassLoader;

public class ApmManager {
    private static final Log logger = LogFactory.getLog(ApmManager.class);
    protected List<Initializer<ApmManager>> initializers = new ArrayList<>();
    private static final ConcurrentHashMap<Class<? extends CollectorConfig>, CollectorConfig> configMap = new ConcurrentHashMap<>();
    private static final List<ShutdownHook.Shutdown> shutdowns = new ArrayList<>();
    private static ApmManager instance;
    private boolean initialized = false;

    private ApmManager() {
        Ready.post(new GeneralEvent(Event.APM_MANAGER_CREATE, this));
    }

    public static ApmManager getInstance() {
        if(instance == null) {
            instance = createApmManager();
            instance.initApm();
        }
        return instance;
    }

    private static ApmManager createApmManager() {
        GeneralEvent beforeEvent = new GeneralEvent(Event.APM_MANAGER_BEFORE_CREATE);
        Ready.post(beforeEvent);
        ApmManager apmManager = beforeEvent.getObject() instanceof BeanManager ? beforeEvent.getObject() : new ApmManager();
        GeneralEvent afterEvent = new GeneralEvent(Event.APM_MANAGER_AFTER_CREATE, null, apmManager);
        Ready.post(afterEvent);
        apmManager = afterEvent.getObject() instanceof ApmManager ? afterEvent.getObject() : apmManager;
        return apmManager;
    }

    private void initApm() {
        if(initialized) { return; }
        startInit();
        endInit();
        Ready.post(new GeneralEvent(Event.APM_MANAGER_AFTER_INIT, this));
    }

    public void addInitializer(Initializer<ApmManager> initializer) {
        this.initializers.add(initializer);
        initializers.sort(Comparator.comparing(Initializer::order));
    }

    private void startInit() {
        try {
            for (Initializer<ApmManager> i : initializers) {
                i.startInit(this);
            }

            this.initialize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void endInit() {
        try {
            for (Initializer<ApmManager> i : initializers) {
                i.endInit(this);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ApmConfig getConfig() {
        return Ready.getBootstrapConfig().getApm();
    }

    public static <T extends CollectorConfig> T getConfig(Class<T> collectorConfig) {
        if(!configMap.containsKey(collectorConfig)) {
            var config = (CollectorConfig)ClassUtil.newInstance(collectorConfig);
            registryConfig(config);
        }
        return (T)configMap.get(collectorConfig);
    }

    private static void registryConfig(CollectorConfig config) {
        logger.debug("register config: " + config.getCollectorName());
        Object collectorConfig = getConfig().getCollector().get(config.getCollectorName());
        if(collectorConfig != null) {
            if(collectorConfig instanceof Map) {
                config = (CollectorConfig)Config.convertItemToObject(collectorConfig, config.getClass());
            } else {
                logger.debug("Wrong config for apm collector: " + config.getCollectorName());
            }
        }
        configMap.put(config.getClass(), config);
    }

    private void initialize() {
        if(initialized) { return; }
        try {
            if(!getConfig().isEnabled()) return;
            logger.info("initialize APM ......");
            Ready.shutdownHook.add(ShutdownHook.STAGE_1, (inMs)-> {
                shutdowns.forEach(shutdown -> {
                    try {
                        shutdown.execute(inMs);
                    } catch (Throwable e) {
                        logger.warn(e,"Failed to stop: failed to shutdown, method=%s", shutdown);
                    }
                });
                logger.info("shutdown all APM tasks");
            });

            List<CollectorConfig> list = new ArrayList<>();
            for(Map.Entry<String, Object> entry : getConfig().getCollector().entrySet()) {
                if(entry.getValue() instanceof Map) {
                    String configClass = ((Map)entry.getValue()).get("config").toString();
                    try {
                        Class<?> configClazz = Class.forName(configClass, false, getDefaultClassLoader());
                        if(CollectorConfig.class.isAssignableFrom(configClazz)) {
                            list.add(getConfig((Class<? extends CollectorConfig>)configClazz));
                        } else {
                            logger.warn("Invalid CollectorConfig: %s", configClass);
                        }
                    } catch (ClassNotFoundException e) {
                        logger.warn(e, "Failed to load CollectorConfig: %s", configClass);
                    }
                }
            }
            list.sort(Comparator.comparingInt(CollectorConfig::getOrder));
            List<String> interceptors = new ArrayList<>();
            list.forEach(config->{
                if(config.isEnabled()) {
                    config.active();
                    config.getCollectorClasses().forEach(
                            clazz -> {
                                if (Interceptor.class.isAssignableFrom(clazz)) {
                                    interceptors.add(clazz.getCanonicalName());
                                } else {
                                    try {
                                        clazz.getDeclaredConstructor().newInstance();
                                    } catch (Exception e) {
                                        logger.warn(e, "Failed to initialize APM collector " + clazz.getCanonicalName() + ": ");
                                    }
                                }
                            });
                }
            });
            TransformerManager.getInstance().attach(interceptors);

            ReporterManager.init();
            JvmInfoTask.start();

            initialized = true;
        } catch (Throwable e) {
            logger.error(e, "APM initialization failed!");
            throw new RuntimeException("APM initialization failed", e);
        }
    }

    public static void addShutdown(ShutdownHook.Shutdown shutdown) {
        shutdowns.add(shutdown);
    }

}
