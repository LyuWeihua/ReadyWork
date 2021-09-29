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

package work.ready.cloud.cluster;

import org.apache.ignite.IgniteMessaging;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cluster.BaselineNode;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.events.DiscoveryEvent;
import org.apache.ignite.events.Event;
import org.apache.ignite.lang.IgniteBiPredicate;
import org.apache.ignite.lang.IgnitePredicate;
import work.ready.cloud.ReadyCloud;
import work.ready.cloud.cluster.elasticsearch.ElasticSearch;
import work.ready.cloud.cluster.elasticsearch.ReadyElasticSearchFactory;
import work.ready.cloud.cluster.elasticsearch.Version;
import work.ready.cloud.cluster.elasticsearch.artifact.ArchiveArtifact;
import work.ready.cloud.registry.base.URL;
import work.ready.core.module.ShutdownHook;
import work.ready.core.server.Constant;
import work.ready.core.tools.define.io.FileSystemResource;
import work.ready.core.tools.define.io.Resource;
import work.ready.core.event.GeneralEvent;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;
import work.ready.core.tools.StrUtil;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.ignite.events.EventType.*;
import static work.ready.core.event.cloud.Event.*;

public class EventHandler {
    private static final Log logger = LogFactory.getLog(EventHandler.class);

    private DelayQueue<DelayedTask> eventQueue = new DelayQueue<>();
    private DelayQueue<DelayedTask> messageQueue = new DelayQueue<>();
    private List<Integer> nodeLeftEvent = new ArrayList<>(
            Arrays.asList(EVT_NODE_LEFT,EVT_NODE_FAILED,EVT_NODE_SEGMENTED,EVT_CACHE_NODES_LEFT));
    private List<Integer> nodeJoinEvent = new ArrayList<>(Arrays.asList(EVT_NODE_JOINED, EVT_CLIENT_NODE_RECONNECTED));

    private void eventHandledListener(Cloud cloud) {
        
        IgniteBiPredicate<UUID, String> messageListener = new IgniteBiPredicate<>() {
            @Override
            public boolean apply(UUID nodeId, String msg) {
                logger.info("Youngest node received message [msg=" + msg.replace("%", "%%") + ", from Oldest=" + nodeId.toString().replace("%", "%%") + "]");
                Ready.post(new GeneralEvent(MESSAGE_FROM_OLDEST).put("EventHandled", new DelayedTask(msg, 1000 * 60L)));
                return true; 
            }
        };
        IgniteMessaging rmtMsg = Cloud.message(Cloud.cluster().forYoungest());
        rmtMsg.localListen("EventHandled", messageListener);
    }

    public void listen(Cloud cloud) {
        ExecutorService pool = Executors.newCachedThreadPool(new CloudThreadFactory());
        
        Ready.shutdownHook.add(ShutdownHook.STAGE_6, (inMs)->pool.shutdown());
        IgnitePredicate<Event> eventListener;

        Ready.eventManager().addListener(this, "messageFromOldest",
                (setter -> setter.addName(MESSAGE_FROM_OLDEST).setAsync(true)));
        Ready.eventManager().addListener(this, "elasticSearchInit",
                (setter -> setter.addName(READY_WORK_CLOUD_AFTER_INIT).setAsync(false)));
        eventHandledListener(cloud);

        eventListener = new IgnitePredicate<Event>() {
            @Override
            public boolean apply(Event evt) {
                if(logger.isDebugEnabled()) {                               
                    logger.debug("Received node event: " + evt.toString().replace("%", "%%"));
                }
                pool.submit(() -> {
                    if(nodeLeftEvent.contains(evt.type())){
                        handleNodeLeft(cloud, evt);
                    } else
                    if(nodeJoinEvent.contains(evt.type())){
                        handleNodeJoin(cloud, evt);
                    }
                });
                return true; 
            }
        };
        Cloud.events(Cloud.cluster().forServers()).localListen(eventListener,
                EVT_NODE_JOINED, EVT_NODE_LEFT, EVT_NODE_FAILED, EVT_NODE_SEGMENTED, EVT_CLIENT_NODE_DISCONNECTED, EVT_CLIENT_NODE_RECONNECTED,
                EVT_CACHE_NODES_LEFT);

        if(!Cloud.cluster().state().active()) {
            cloudActivate(cloud);
        }

        afterCloudActive(cloud);
        eventsHandleEnsure();
    }

    private void cloudActivate(Cloud cloud) {
        String data_oltp_nodes = null;
        boolean forceActive = false;
        if(ReadyCloud.getNodeType().equals(Cloud.NodeType.APPLICATION_WITH_OLTP)) {
            data_oltp_nodes = Ready.getProperty(Cloud.DATA_OLTP_NODES_PROPERTY);
            if (data_oltp_nodes == null) {
                data_oltp_nodes = ReadyCloud.getConfig().getDataOltpNodes();
            }
        }
        List<String> definedNodes = StrUtil.isBlank(data_oltp_nodes) ? null : new ArrayList<>(Arrays.asList(StrUtil.split(data_oltp_nodes, ",")));
        if(definedNodes == null) {
            if(logger.isWarnEnabled()) {
                logger.warn("Baseline Topology is waiting for activation.");  
            }
        } else {
            String active = Ready.getProperty(Cloud.DATA_OLTP_FORCE_ACTIVE_PROPERTY);
            if(active != null && "true".equals(active.toLowerCase())) {
                forceActive = true;
            }
            if (logger.isWarnEnabled()) {
                logger.warn("Baseline Topology is waiting for other nodes to join: ");  
            }
        }
        try {
            boolean isActive = false;
            StringBuilder sb = new StringBuilder();
            do {
                isActive = Cloud.cluster().state().active();
                if (!isActive) {
                    List<String> onlineNodes = Cloud.cluster().nodes().stream().map(node->node.consistentId().toString()).collect(Collectors.toList());
                    sb.delete(0, sb.length());
                    sb.append("Current online nodes: ");
                    sb.append(Cloud.cluster().nodes().stream().map(node -> node.consistentId() + "" + node.addresses().toString().replace("%","%%")).collect(Collectors.toList()));
                    if(Cloud.cluster().currentBaselineTopology() == null || forceActive) {
                        if(definedNodes == null) {
                            sb.append(", waiting for other nodes.");
                        } else {
                            if (onlineNodes.containsAll(definedNodes)) {
                                List<ClusterNode> dataNodes = Cloud.cluster().nodes().stream().filter(node -> definedNodes.contains(node.consistentId().toString())).collect(Collectors.toList());
                                for (var node : dataNodes) {
                                    if (node.attribute(Cloud.READY_CLOUD_DATA_OLTP_NODE) == null) {
                                        cloud.shutdownAllNodes();
                                        throw new RuntimeException(node.consistentId() + "" + node.addresses() + " is not a valid DATA_OLTP node");
                                    }
                                }
                                System.err.println("==active==");
                                sb.append(", all nodes are online, the Baseline Topology is going to be active.");
                                Cloud.cluster().active(true);
                                if (Cloud.cluster().currentBaselineTopology() == null) {  
                                    
                                }
                            } else {
                                sb.append(", waiting for nodes: ");
                                sb.append(definedNodes.stream().filter(node -> !onlineNodes.contains(node)).collect(Collectors.toList()));
                            }
                        }
                    } else {
                        List<String> existTopology = Cloud.cluster().currentBaselineTopology().stream().map(node->node.consistentId().toString()).collect(Collectors.toList());
                        sb.append(", waiting for nodes: ");
                        sb.append(existTopology.stream().filter(node -> !onlineNodes.contains(node)).collect(Collectors.toList()));
                    }
                    logger.warn(sb.toString());
                    Thread.sleep(1000);
                }
            } while (!isActive);
        } catch (InterruptedException e) {
            throw new RuntimeException("InterruptedException while waiting for nodes to join the cluster.");
        }

        if(logger.isWarnEnabled()) {
            logger.warn("Cloud Baseline Topology is active !");
        }
    }

    private void afterCloudActive(Cloud cloud){
        if(Cloud.cluster().currentBaselineTopology() != null) {
            Cloud.cluster().currentBaselineTopology().stream().filter(node->node.attribute(Cloud.READY_CLOUD_DATA_OLTP_NODE) != null).findFirst().ifPresent((node)-> {
                if(node.consistentId().equals(Cloud.cluster().localNode().consistentId())) {
                    
                    Cloud.cluster().baselineAutoAdjustEnabled(ReadyCloud.getConfig().isAutoRebalance());
                    Cloud.cluster().baselineAutoAdjustTimeout(ReadyCloud.getConfig().getRebalanceTimeOut());
                    Cloud.addCacheConfiguration(cloud.newCacheConfig(CloudDb.TableMode.REPLICATED_PERSISTENCE.name(), false, true, 0, true).setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL_SNAPSHOT));
                    Cloud.addCacheConfiguration(cloud.newCacheConfig(CloudDb.TableMode.PARTITIONED_PERSISTENCE.name(), true, true, 1, true).setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL_SNAPSHOT));
                }
            });
        }
        if(Cloud.cluster().forOldest().node().equals(Cloud.cluster().localNode())) {
            Cloud.addCacheConfiguration(cloud.newCacheConfig(CloudDb.TableMode.REPLICATED_MEMORY.name(), false, true, 0, false).setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL_SNAPSHOT));
            Cloud.addCacheConfiguration(cloud.newCacheConfig(CloudDb.TableMode.PARTITIONED_MEMORY.name(), true, true, 1, false).setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL_SNAPSHOT));
        }
        if(ReadyCloud.getConfig().isDataNodeAutoJoin()) {
            if (Cloud.cluster().currentBaselineTopology() != null && Cloud.cluster().localNode().attribute(Cloud.READY_CLOUD_DATA_OLTP_NODE) != null) {
                if (Cloud.cluster().currentBaselineTopology().stream().map(BaselineNode::consistentId).noneMatch(id->id.equals(Cloud.cluster().localNode().consistentId()))) {
                    Cloud.cluster().currentBaselineTopology().add(Cloud.cluster().localNode());
                    if (logger.isWarnEnabled()) {
                        logger.warn(Cloud.cluster().localNode().consistentId() + "" + Cloud.cluster().localNode().addresses() + " is automatically joined Baseline Topology.");
                    }
                    if(Cloud.cluster().isBaselineAutoAdjustEnabled()) {
                        if (logger.isWarnEnabled()) {
                            logger.warn(Cloud.cluster().localNode().consistentId() + "" + Cloud.cluster().localNode().addresses() + " is going to handle data in " + ReadyCloud.getConfig().getRebalanceTimeOut() / 1000 + " seconds.");
                        }
                    } else {
                        if (logger.isWarnEnabled()) {
                            logger.warn(Cloud.cluster().localNode().consistentId() + "" + Cloud.cluster().localNode().addresses() + " joined Baseline Topology, but not going to store persistence data until rebalanced.");
                        }
                    }
                }
            }
        }
    }

    public void messageFromOldest(GeneralEvent event){
        messageQueue.add((DelayedTask)event.get("EventHandled"));
    }

    public void elasticSearchInit(GeneralEvent event) {
        Cloud cloud = event.getObject();
        if(Cloud.cluster().currentBaselineTopology() != null) {
            if(ReadyCloud.getNodeType().equals(Cloud.NodeType.APPLICATION_WITH_OLAP)) {
                String data_olap_nodes = null;
                data_olap_nodes = Ready.getProperty(Cloud.DATA_OLAP_NODES_PROPERTY);
                if (data_olap_nodes == null){
                    data_olap_nodes = ReadyCloud.getConfig().getDataOlapNodes();
                }
                List<String> definedNodes = StrUtil.isBlank(data_olap_nodes) ? null : new ArrayList<>(Arrays.asList(StrUtil.split(data_olap_nodes, ",")));

                ReadyElasticSearchFactory elasticSearchFactory = new ReadyElasticSearchFactory();
                if(ReadyCloud.getConfig().getElasticSearchSettings() != null) {
                    elasticSearchFactory.setConfigProperties(ReadyCloud.getConfig().getElasticSearchSettings());
                }
                elasticSearchFactory.setWorkingDirectory(Ready.root().resolve("elasticsearch").resolve("workspace").toAbsolutePath());
                Path dataPath = Ready.root().resolve("elasticsearch").resolve(Cloud.getConsistentId()).resolve("data").toAbsolutePath();
                Path logsPath = Ready.root().resolve("elasticsearch").resolve(Cloud.getConsistentId()).resolve("logs").toAbsolutePath();
                elasticSearchFactory.setConfigProperty("path.data", dataPath.toString());
                elasticSearchFactory.setConfigProperty("path.logs", logsPath.toString());
                elasticSearchFactory.setClusterName(ReadyCloud.getConfig().getOlapClusterName());
                elasticSearchFactory.setDefaultVersion(ReadyCloud.getConfig().getElasticSearchVersion()); 
                elasticSearchFactory.setDownloadUrls(ReadyCloud.getConfig().getElasticSearchDownloadUrls());
                elasticSearchFactory.setName(Cloud.getConsistentId());
                
                elasticSearchFactory.setAddress(Cloud.getBindIp());
                String ipAddress = Cloud.getPublishIp();
                if(ipAddress != null) {
                    elasticSearchFactory.setPublishAddress(ipAddress);
                }
                elasticSearchFactory.setInitialMasterNodes(definedNodes);
                List<URL> nodeList = Cloud.discoverAll(Cloud.NodeType.APPLICATION_WITH_OLAP.getType(), Constant.PROTOCOL_DEFAULT, Cloud.OLAP_SERVICE_ID, Ready.getBootstrapConfig().getActiveProfile());
                if(nodeList != null && nodeList.size() > 0) {
                    List<String> seedList = nodeList.stream().filter(url->url.getParameters().get("clusterName").equals(elasticSearchFactory.getClusterName()))
                            .map(url -> url.getParameters().get("address") + ":" + url.getParameters().get("tcpPort"))
                            .collect(Collectors.toList());
                    elasticSearchFactory.setSeedHosts(seedList);
                } else {
                    elasticSearchFactory.setInitialMasterNodes(elasticSearchFactory.getName());
                }
                if(Files.exists(Ready.config().getMainConfigPath())) {
                    String osType = ReadyElasticSearchFactory.getOsType();
                    Pattern pattern = Pattern.compile("elasticsearch-([\\d\\.]+)-" + osType + "-x86_64.tar.gz");
                    try {
                        Files.walkFileTree(Ready.config().getMainConfigPath(), new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {    
                                Matcher matcher = pattern.matcher(file.getFileName().toString());
                                if(matcher.matches()) {
                                    boolean newVersion = true;
                                    if(elasticSearchFactory.getArtifact() != null) {
                                        if(((ArchiveArtifact) elasticSearchFactory.getArtifact()).getVersion().compareTo(Version.of(osType, matcher.group(1))) >= 0){
                                            newVersion = false;
                                        }
                                    }
                                    if(newVersion) {
                                        Resource resource = new FileSystemResource(file);
                                        elasticSearchFactory.setArtifact(new ArchiveArtifact(Version.of(osType, matcher.group(1)), resource));
                                        
                                        if(matcher.group(1).equals(ReadyCloud.getConfig().getElasticSearchVersion())) {
                                            return FileVisitResult.TERMINATE;
                                        }
                                    }
                                }
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } catch (IOException e) {}
                }
                ElasticSearch elasticSearch = elasticSearchFactory.create();
                elasticSearch.start();
            }
        }
    }

    private void eventsHandleEnsure(){

        Executors.newSingleThreadExecutor().execute(() -> {
            DelayedTask task = null;
            while (true) {
                try {
                    task = eventQueue.take();
                    if(messageQueue.contains(task)){
                        messageQueue.remove(task);
                        if(logger.isInfoEnabled()) {
                            logger.info("Youngest node ensured that oldest node already handled event: " + task.getName().replace("%","%%"));
                        }
                    } else {
                        if(logger.isWarnEnabled()) {
                            logger.warn("Oldest node seems missed event: " + task.getName().replace("%","%%") + ", youngest node is trying to handle it.");
                        }
                        
                        DiscoveryEvent event = (DiscoveryEvent)task.getObject();
                        if(nodeLeftEvent.contains(event.type())) {
                            Ready.post(new GeneralEvent(NODE_UNAVAILABLE).put("node", event.eventNode()));
                        }
                    }
                } catch (Exception e) {
                    logger.info(e, e.getMessage());
                }
            }
        });

        Executors.newSingleThreadExecutor().execute(() -> {
            DelayedTask task = null;
            while (true) {
                try {
                    task = messageQueue.take();
                    if(logger.isWarnEnabled()) {
                        logger.warn("Received message from Oldest node: " + task.getName().replace("%","%%") + " for insuring, but youngest node doesn't know such event");
                    }
                } catch (Exception e) {
                    logger.info(e, e.getMessage());
                }
            }
        });

    }

    private void handleNodeJoin(Cloud cloud, Event evt) {
        if(Cloud.cluster().forOldest().node().isLocal()) {
            System.out.println("Oldest handles a node join event: " + evt.toString().replace("%","%%"));
        } else if(Cloud.cluster().forYoungest().node().isLocal()) {
            System.out.println("Youngest received a node join event: " + evt.toString().replace("%","%%"));
        }
    }

    private void handleNodeLeft(Cloud cloud, Event evt) {
        if(Cloud.cluster().forOldest().node().isLocal()){
            if(logger.isInfoEnabled()) {
                logger.info("Oldest node handles a node left event: " + evt.toString().replace("%", "%%"));
            }
            if(evt instanceof DiscoveryEvent) {
                DiscoveryEvent event = (DiscoveryEvent)evt;
                Ready.post(new GeneralEvent(NODE_UNAVAILABLE).put("node", event.eventNode()));

                if(Cloud.cluster().nodes().size() > 1) { 
                    
                    IgniteMessaging rmtMsg = Cloud.message(Cloud.cluster().forYoungest());
                    rmtMsg.send("EventHandled", event.shortDisplay());
                }
            }

            Cloud.getRegistry().startHeartbeat();

        } else if(Cloud.cluster().forYoungest().node().isLocal()){
            if(logger.isInfoEnabled()) {
                logger.info("Youngest node received a node left event: " + evt.toString().replace("%", "%%"));
            }
            if(evt instanceof DiscoveryEvent) {
                DiscoveryEvent event = (DiscoveryEvent)evt;
                eventQueue.add(new DelayedTask(event.shortDisplay(), event,1000 * 5L));
                if(logger.isInfoEnabled()) {
                    logger.info("Youngest node received the same event as the oldest node received: " + event.eventNode().consistentId() + " left, try to ensure that the oldest node handles this event properly.");
                }
            }
        }
    }

}
