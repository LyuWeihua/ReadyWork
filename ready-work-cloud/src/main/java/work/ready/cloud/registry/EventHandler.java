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

package work.ready.cloud.registry;

import org.apache.ignite.IgniteMessaging;
import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.events.CacheEvent;
import org.apache.ignite.lang.IgniteBiPredicate;
import org.apache.ignite.lang.IgnitePredicate;
import org.apache.ignite.spi.discovery.tcp.internal.TcpDiscoveryNode;
import work.ready.cloud.ReadyCloud;
import work.ready.cloud.client.clevercall.CircuitBreaker;
import work.ready.cloud.client.clevercall.CircuitBreakerException;
import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.registry.base.URL;
import work.ready.cloud.registry.base.URLParam;
import work.ready.core.component.switcher.SwitcherUtil;
import work.ready.core.event.cloud.Event;
import work.ready.core.event.GeneralEvent;
import work.ready.core.ioc.annotation.Service;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Constant;
import work.ready.core.server.Ready;
import work.ready.core.server.WebServer;
import work.ready.core.tools.define.Kv;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

@Service
public class EventHandler {
    private static final Log logger = LogFactory.getLog(EventHandler.class);

    public void listen(Cloud cloud, ReadyRegistry registry){

        Ready.eventManager().addListener(this, "host_unreachable",
                setter -> setter.addName(Event.SERVICE_HOST_UNREACHABLE).setAsync(true));

        Ready.eventManager().addListener(this, "service_unstable",
                setter -> setter.addName(Event.SERVICE_UNSTABLE).setAsync(true));

        Ready.eventManager().addListener(this, "node_unavailable",
                setter -> setter.addName(Event.NODE_UNAVAILABLE).setAsync(true));

        Ready.eventManager().addListener(this, "webServerStartListener",
                setter -> setter.addName(Event.WEB_SERVER_STARTED).setAsync(true));

        Ready.eventManager().addListener(this, "webServerShutdownListener",
                setter -> setter.addName(Event.WEB_SERVER_BEFORE_SHUTDOWN).setAsync(false));

        Ready.eventManager().addListener(this, "readyForWorkListener",
                setter -> setter.addName(Event.READY_FOR_WORK).setAsync(false));

        IgnitePredicate<CacheEvent> eventListener;
        
        eventListener = new IgnitePredicate<CacheEvent>() {
            @Override
            public boolean apply(CacheEvent evt) {
                if(logger.isTraceEnabled()) {
                    logger.trace("Received cache event [cacheName=" + evt.cacheName() + ", evt=" + evt.name() + ", key=" + evt.key() +
                            ", oldVal=" + evt.oldValue() + ", newVal=" + evt.newValue());
                }

                return true; 
            }
        };

        localRegistryServiceEventListener(cloud, registry);
        
        remoteRegistryServiceEventListener(cloud, registry);
        
        remoteStabilityServiceEventListener(cloud, registry);
    }

    private void localRegistryServiceEventListener(Cloud cloud, ReadyRegistry registry) {
        Ready.eventManager().addListener(Event.REGISTRY_AVAILABLE_SERVICE_CHANGED, false, event -> {
            String serviceId = event.getObject();
            registry.availableCacheChanged(serviceId); 

            ClusterGroup group = Cloud.cluster().forRemotes();
            if(logger.isDebugEnabled()) {
                logger.debug("Local registry available service %s updated, try to notify remote registry nodes=%s ", serviceId, group.nodes());
            }
            if(!group.nodes().isEmpty()) {
                IgniteMessaging rmtMsg = Cloud.message(group);
                
                rmtMsg.send(Event.REGISTRY_AVAILABLE_SERVICE_CHANGED.toString(), serviceId);
            }
        });
    }

    private void remoteRegistryServiceEventListener(Cloud cloud, ReadyRegistry registry) {
        
        IgniteBiPredicate<UUID, String> messageListener = new IgniteBiPredicate<>() {
            @Override
            public boolean apply(UUID nodeId, String serviceId) {
                if(logger.isTraceEnabled()) {
                    logger.trace("node received REGISTRY_AVAILABLE_SERVICE_CHANGED message [msg=" + serviceId + ", from remote=" + nodeId + "]");
                }
                registry.availableCacheChanged(serviceId); 
                if(logger.isDebugEnabled()) {
                    logger.debug("Local registry available service %s cache updated", serviceId);
                }
                return true; 
            }
        };
        IgniteMessaging rmtMsg = Cloud.message();
        rmtMsg.localListen(Event.REGISTRY_AVAILABLE_SERVICE_CHANGED.toString(), messageListener);
    }

    public void host_unreachable(GeneralEvent event) {  
        var service = (CircuitBreaker)event.get("CircuitBreaker");
        if(logger.isWarnEnabled()) {
            logger.warn(service.getUrl() + " application host is unreachable, temporarily remove this service from discovery list");
        }
        adjustUnstableService(service.getUrl(), 100);
        
    }

    public void service_unstable(GeneralEvent event) { 
        var service = (CircuitBreaker)event.get("CircuitBreaker");
        int unstableLevel = 30;
        if(service != null) {
            if(event.get("Exception") instanceof CircuitBreakerException) {

            } else if(event.get("Exception") instanceof TimeoutException) {
                unstableLevel = 30;
                if(service.isOpen()) { 
                    unstableLevel = CircuitBreaker.MAX_UNSTABLE_LEVEL;
                }
            }
        } else {
            HeartbeatManager manager = event.getSender();
            String status = (String)event.get("STATUS");
            if(HeartbeatManager.UNSTABLE_FAILURE.equals(status)) {
                unstableLevel = CircuitBreaker.MAX_UNSTABLE_LEVEL;  
            } else if(HeartbeatManager.UNSTABLE_UNHEALTH.equals(status)){
                unstableLevel = 20;     
            } else if(HeartbeatManager.UNSTABLE_RECOVERY.equals(status)){
                unstableLevel = -5;    
            }
        }
        URL url = event.getObject();
        if(unstableLevel > 0) {
            if (logger.isWarnEnabled()) {
                logger.warn("%s service is unstable, degrade this service.", url);
            }
        } else {
            if (logger.isWarnEnabled()) {
                logger.warn("%s service is recovering, upgrade this service.", url);
            }
        }
        adjustUnstableService(url, unstableLevel);
    }

    private void adjustUnstableService(URL url, int unstableLevel) {
        int currentLevel = Cloud.getRegistry().getStabilityLevel(url);
        unstableLevel = currentLevel + unstableLevel;
        Cloud.getRegistry().setStabilityLevel(url, unstableLevel);
        ClusterGroup group = Cloud.cluster().forRemotes();
        if(unstableLevel >= currentLevel) {
            if (logger.isDebugEnabled()) {
                logger.debug("%s service is unstable [%s], try to notify remote registry nodes=%s ", url, unstableLevel, group.nodes());
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("%s service is recovering [%s], try to notify remote registry nodes=%s ", url, unstableLevel, group.nodes());
            }
            if(unstableLevel <= CircuitBreaker.MIN_UNSTABLE_LEVEL) {
                CircuitBreaker.tryHalfOpen(url); 
            }
        }
        if(!group.nodes().isEmpty()) {
            IgniteMessaging rmtMsg = Cloud.message(group);
            
            rmtMsg.send(Event.SERVICE_STABILITY_CHANGED.toString(), url.getUri());
        }
    }

    private void remoteStabilityServiceEventListener(Cloud cloud, ReadyRegistry registry) {
        
        IgniteBiPredicate<UUID, String> messageListener = new IgniteBiPredicate<>() {
            @Override
            public boolean apply(UUID nodeId, String uri) {
                if(logger.isTraceEnabled()) {
                    logger.trace("node received SERVICE_STABILITY_CHANGED message [msg=" + uri + ", from remote=" + nodeId + "]");
                }
                registry.stabilityCacheChanged(uri); 
                if(logger.isDebugEnabled()) {
                    logger.debug("Local unstable service %s cache updated", uri);
                }
                return true; 
            }
        };
        IgniteMessaging rmtMsg = Cloud.message();
        rmtMsg.localListen(Event.SERVICE_STABILITY_CHANGED.toString(), messageListener);
    }

    public void node_unavailable(GeneralEvent event){
        ClusterNode clusterNode = (ClusterNode)event.get("node");
        if(logger.isWarnEnabled()) {
            logger.warn("Cloud node " + clusterNode.consistentId() + " is unavailable, remove all services on this node from discovery list");
        }
        if(clusterNode instanceof TcpDiscoveryNode) {
            TcpDiscoveryNode node = (TcpDiscoveryNode)clusterNode;
            String nodeId = node.consistentId().toString();
            int port = node.discoveryPort();
            List<String> host = new ArrayList<>(node.hostNames());
            try {
                Thread.sleep(100); 
            } catch (InterruptedException e){}
            Cloud.getRegistry().unavailableNode(nodeId);
        }
    }

    public void webServerStartListener(GeneralEvent event) {
        if(ReadyCloud.isReady()){
            WebServer webServer = event.getSender();
            webServer.getApplication().applicationConfig().getServer().validate();
            Boolean isHealthCheck = webServer.getApplication().applicationConfig().getServer().isHealthCheck();
            String healthCheckPath = webServer.getApplication().applicationConfig().getServer().getHealthCheckPath();
            String crucialCheckPath = webServer.getApplication().applicationConfig().getServer().getCrucialCheckPath();
            if(crucialCheckPath == null) crucialCheckPath = healthCheckPath;
            var healthCheckParam = Kv.by(URLParam.healthCheck.getName(), isHealthCheck.toString()).set(URLParam.healthCheckPath.getName(), healthCheckPath).set(URLParam.crucialCheckPath.getName(), crucialCheckPath);
            boolean httpChecker = false;
            if(webServer.getHttpPort() > 0){
                Cloud.getRegistry().register(ReadyCloud.getNodeType().getType(), webServer.getApplication(), webServer.getApplication().getName(), Constant.PROTOCOL_HTTP, webServer.getHttpPort(),
                        healthCheckParam);
                Cloud.getRegistry().register(ReadyCloud.getNodeType().getType(), webServer.getApplication(), webServer.getApplication().getName(), Constant.PROTOCOL_WS, webServer.getHttpPort(), null);
                httpChecker = true;
            }
            if(webServer.getHttpsPort() > 0){
                Cloud.getRegistry().register(ReadyCloud.getNodeType().getType(), webServer.getApplication(), webServer.getApplication().getName(), Constant.PROTOCOL_HTTPS, webServer.getHttpsPort(),
                        httpChecker ? null : healthCheckParam);
                Cloud.getRegistry().register(ReadyCloud.getNodeType().getType(), webServer.getApplication(), webServer.getApplication().getName(), Constant.PROTOCOL_WSS, webServer.getHttpsPort(), null);
            }
        }
    }

    public void webServerShutdownListener(GeneralEvent event) {
        
        if(ReadyCloud.isReady()){
            Cloud.getRegistry().unregister(((WebServer)event.getSender()).getApplication());
        }
    }

    public void readyForWorkListener(GeneralEvent event) {
        if(ReadyCloud.isReady()){
            if (logger.isInfoEnabled()) {
                logger.info("Turn on the APPLICATION_READY_SWITCHER switcher, all the services registered are ready for work.");
            }
            
            SwitcherUtil.setValue(Constant.APPLICATION_READY_SWITCHER, true);

            if (Cloud.cluster().forOldest().node().isLocal()) {   
                Cloud.getRegistry().startHeartbeat();
            }
        }
    }

}
