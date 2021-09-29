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

import net.sf.jsqlparser.statement.create.table.CreateTable;
import org.apache.http.HttpHost;
import org.apache.ignite.*;
import org.apache.ignite.Ignite;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.CacheWriteSynchronizationMode;
import org.apache.ignite.cache.PartitionLossPolicy;
import org.apache.ignite.cache.affinity.Affinity;
import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.*;
import org.apache.ignite.internal.GridKernalContext;
import org.apache.ignite.internal.IgniteKernal;
import org.apache.ignite.internal.IgnitionEx;
import org.apache.ignite.internal.processors.affinity.AffinityTopologyVersion;
import org.apache.ignite.internal.processors.cache.GridCacheUtils;
import org.apache.ignite.internal.processors.query.QueryUtils;
import org.apache.ignite.internal.processors.query.h2.DistributedSqlConfiguration;
import org.apache.ignite.internal.processors.query.h2.FunctionsManager;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgniteExperimental;
import org.apache.ignite.lang.IgniteProductVersion;
import org.apache.ignite.lang.IgniteRunnable;
import org.apache.ignite.plugin.IgnitePlugin;
import org.apache.ignite.plugin.PluginNotFoundException;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.spi.checkpoint.cache.CacheCheckpointSpi;
import org.apache.ignite.spi.collision.priorityqueue.PriorityQueueCollisionSpi;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.jdbc.TcpDiscoveryJdbcIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.ignite.spi.failover.always.AlwaysFailoverSpi;
import org.apache.ignite.spi.tracing.TracingConfigurationManager;
import org.elasticsearch.client.RestClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import work.ready.cloud.ReadyCloud;
import work.ready.cloud.client.CloudClient;
import work.ready.cloud.client.clevercall.CircuitBreaker;
import work.ready.cloud.jdbc.ReadyJdbcDriver;
import work.ready.cloud.registry.*;
import work.ready.cloud.registry.ReadyRegistry;
import work.ready.cloud.registry.base.URL;
import work.ready.cloud.registry.base.URLParam;
import work.ready.cloud.transaction.DistributedTransactionManager;
import work.ready.cloud.transaction.TransactionManagerInitializer;
import work.ready.core.component.switcher.global.GlobalSwitcherService;
import work.ready.core.component.switcher.SwitcherUtil;
import work.ready.core.database.*;
import work.ready.core.database.Record;
import work.ready.core.database.datasource.DataSourceConfig;
import work.ready.core.database.datasource.HikariCp;
import work.ready.core.event.cloud.Event;
import work.ready.core.event.GeneralEvent;
import work.ready.core.exception.ClientException;
import work.ready.cloud.log.IgniteJavaLogger;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;
import work.ready.core.server.ServerConfig;
import work.ready.core.tools.*;
import work.ready.cloud.loadbalance.LoadBalancer;
import work.ready.core.server.Constant;
import work.ready.core.tools.define.ConcurrentHashSet;
import work.ready.core.tools.define.Kv;
import work.ready.core.tools.validator.Assert;

import javax.cache.CacheException;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.apache.ignite.events.EventType.*;

public class Cloud {
    private static final Log logger = LogFactory.getLog(Cloud.class);
    public final static String NODE_ID_PROPERTY = "ready.node.id";
    public final static String DATA_OLTP_NODES_PROPERTY = "ready.oltp.nodes";
    public final static String DATA_OLTP_FORCE_ACTIVE_PROPERTY = "ready.oltp.force_active";
    public final static String DATA_OLAP_NODES_PROPERTY = "ready.olap.nodes";

    public final static String OLTP_SERVICE_ID = "OLTP_SERVICE";
    public final static String OLAP_SERVICE_ID = "OLAP_SERVICE";

    public final static String CORE_DB_SCHEMA = "READY";
    public final static String CORE_DB_NAME = "READY_coreDB";
    public final static String PUBLIC_DB_NAME = "READY_publicDB";
    private final static String IGNITE_DB_PWD = "DoNotWantPeopleToUseThisPassword";
    public final static String WITHOUT_PERSISTENCE = "withoutPersistence";
    public final static String WITH_PERSISTENCE = "withPersistence";
    private final static String GENERAL_PROFILE = "*";

    private static Ignite ignite;
    private static ReliableMessage reliableMessage;
    private static DistributedTransactionManager transactionManager;
    private static CloudClient cloudClient;
    private static CloudConfig cloudConfig;
    private static ServerConfig serverConfig;
    private static Registry registry;
    private static LoadBalancer loadBalancer;
    private static final Set<URL> subscribedSet = new ConcurrentHashSet<>();
    private static final Map<String, List<URL>> serviceMap = new ConcurrentHashMap<>();
    private static final Map<String, Object> userAttributes = new ConcurrentHashMap<>();
    private static String bindIp;
    private static String publishIp;
    private static String consistentId;

    public enum NodeType {
        APPLICATION(Constant.NODE_TYPE_APPLICATION),
        APPLICATION_WITH_OLTP(Constant.NODE_TYPE_APPLICATION_WITH_OLTP),
        APPLICATION_WITH_OLAP(Constant.NODE_TYPE_APPLICATION_WITH_OLAP),
        COMPUTING_CPU(Constant.NODE_TYPE_COMPUTING_CPU),
        COMPUTING_GPU(Constant.NODE_TYPE_COMPUTING_GPU),
        COMPUTING_CPU_GPU(Constant.NODE_TYPE_COMPUTING_CPU_GPU),
        MEMORY_POOL(Constant.NODE_TYPE_MEMORY_POOL);

        String type;
        NodeType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    public enum NodeMode {
        REPLICATED(CacheMode.REPLICATED),
        PARTITIONED(CacheMode.PARTITIONED);

        private CacheMode mode;
        NodeMode(CacheMode mode){
            this.mode = mode;
        }

        public CacheMode getMode() {
            return mode;
        }
    }

    public Cloud(CloudConfig config) {
        cloudConfig = config;
        serverConfig = Ready.getMainApplicationConfig().getServer();

        if(cloudConfig.isTrustAllService()) {
            cloudClient = CloudClient.getTrustAllInstance(true);
        } else {
            cloudClient = CloudClient.getInstance(true);
        }

        igniteInit();
        
        ReadyJdbcDriver.register();
        
        new EventHandler().listen(this);
        
        reliableMessage = new ReliableMessage();
        reliableMessage.listen(this);
        
        advancedFeatureSupport();

        Ready.post(new GeneralEvent(Event.SERVICE_DISCOVER_REGISTRY_BEFORE_INIT, this));
        registry = new ReadyRegistry(this, new URL(Constant.PROTOCOL_DEFAULT, getPublishIp(), 0, "registry", Kv.create()));
        Ready.post(new GeneralEvent(Event.SERVICE_DISCOVER_REGISTRY_AFTER_INIT, this, registry));
        
        try {
            loadBalancer = cloudConfig.getLoadBalancer(cloudConfig.getLoadBalancer()).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            logger.error(e, "LoadBalancer initializing exception: ");
        }

        if(logger.isInfoEnabled()) {
            logger.info("Cloud instance is started");
        }
    }

    private void advancedFeatureSupport() {
        
        new work.ready.core.event.cloud.EventHandler().listen(this);
        
        SwitcherUtil.setSwitcherService(new GlobalSwitcherService(this)); 
        
        new work.ready.core.security.cloud.EventHandler().listen(this);
        
        new work.ready.core.component.cache.ignite.EventHandler().listen();
        
        new work.ready.core.handler.session.cloud.EventHandler().listen();
        
        if(cloudConfig.isEnabledDistributedTransaction()) {
            Ready.eventManager().addListener(event -> event.addName(Event.DATABASE_MANAGER_AFTER_CREATE), (event) -> {
                DatabaseManager manager = event.getObject();
                manager.addInitializer(Ready.beanManager().get(TransactionManagerInitializer.class));
            });
        }
        
        new work.ready.core.database.cloud.EventHandler().setCoreDbPwd(IGNITE_DB_PWD).listen(this);
    }

    public static DbPro getCoreDb(){
        return Ready.dbManager().getDb().use(Cloud.CORE_DB_NAME);
    }

    public static DbPro getDb(){
        return Ready.dbManager().getDb().use(Cloud.PUBLIC_DB_NAME);
    }

    public static DatasourceAgent addMapping(String tableName, String primaryKey, Class<? extends Model<?>> modelClass) {
        return Ready.dbManager().getDatasourceAgent(Cloud.PUBLIC_DB_NAME).addMapping(tableName, primaryKey, modelClass);
    }

    public static DatasourceAgent addMapping(String tableName, Class<? extends Model<?>> modelClass) {
        return Ready.dbManager().getDatasourceAgent(Cloud.PUBLIC_DB_NAME).addMapping(tableName, modelClass);
    }

    public static void addDbUser(String username, String password) {
        int result = 0;
        try {
            result = getCoreDb().update("CREATE USER \"" + username + "\" WITH PASSWORD '" + password + "';");
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void updateDbUserPwd(String username, String password) {
        int result = 0;
        try {
            
            result = getCoreDb().update("ALTER USER \"" + username + "\" WITH PASSWORD '" + password + "';");
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void deleteDbUser(String username) {
        int result = 0;
        try {
            result = getCoreDb().update("DROP USER " + username + ";");
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static RestClient restClient() {
        return restClient(null);
    }
    public static RestClient restClient(String clusterName) {
        return RestClient.builder(discoverAllOlapNodes(clusterName)).build();
    }

    private static HttpHost[] discoverAllOlapNodes(String clusterName) {
        List<URL> nodeList = discoverAll(Cloud.NodeType.APPLICATION_WITH_OLAP.getType(), Constant.PROTOCOL_DEFAULT, Cloud.OLAP_SERVICE_ID, Ready.getBootstrapConfig().getActiveProfile());
        HttpHost[] httpHostArray = new HttpHost[0];
        if (nodeList != null && nodeList.size() > 0) {
            httpHostArray = nodeList.stream()
                    .filter(url -> clusterName == null || url.getParameter("clusterName").equals(clusterName))
                    .map(url -> new HttpHost(url.getHost(), url.getPort(), url.getProtocol())).toArray(HttpHost[]::new);
        }
        if (httpHostArray.length > 0) {
            return httpHostArray;
        } else {
            throw new RuntimeException(String.format("Failed to discover %s, with " + (clusterName != null ? "clusterName: " + clusterName + "," : "") + " profile: %s, please make sure that there are OLAP nodes alive.", Cloud.OLAP_SERVICE_ID, Ready.getBootstrapConfig().getActiveProfile()));
        }
    }

    public static LoadBalancer getLoadBalancer() {
        return loadBalancer;
    }

    public static void setLoadBalancer(LoadBalancer lb) {
        loadBalancer = Assert.notNull(lb, "LoadBalancer can not be null.");
    }

    public static CompletableFuture<HttpResponse<String>> callService(String protocol, String serviceId, String profile, String requestPath, HttpRequest.Builder requestBuilder) {
        try {
            URL url = discover(protocol, serviceId, profile, null);
            if (url == null) {
                logger.error("Failed to discover service with serviceId: %s, with profile: %s", serviceId, profile);
                throw new ClientException(String.format("Failed to discover service with serviceId: %s, with profile: %s", serviceId, profile));
            }
            return cloudClient.callService(url, requestPath, requestBuilder);
        } catch (Exception e) {
            logger.error(e,"Failed to call service: %s", serviceId);
            throw new RuntimeException("Failed to call service: " + serviceId, e);
        }
    }

    public static CircuitBreaker getService(String protocol, String project, String projectVersion, String serviceId, String serviceVersion, String profile, String requestPath, HttpRequest.Builder requestBuilder) {
        try {
            URL url = discover(NodeType.APPLICATION.getType(), protocol, project, projectVersion, serviceId, serviceVersion, profile, null);
            if (url == null) {
                logger.error("Failed to discover service with serviceId: %s, with profile: %s", serviceId, profile);
                throw new ClientException(String.format("Failed to discover service with serviceId: %s, with profile: %s", serviceId, profile));
            }
            return cloudClient.getRequestService(url, requestPath, requestBuilder);
        } catch (Exception e) {
            logger.error(e,"Failed to call service: %s", serviceId);
            throw new RuntimeException("Failed to call service: " + serviceId, e);
        }
    }

    public static CircuitBreaker getService(HttpRequest.Builder requestBuilder) {
        return cloudClient.getRequestService(requestBuilder);
    }

    public static Registry getRegistry() {
        return registry;
    }

    public static CloudClient getCloudClient() {
        return cloudClient;
    }

    public static String discoverConfigNode(String protocol, String serviceId) {
        URL url = loadBalancer.select(serviceId, doDiscover(Constant.NODE_TYPE_CONFIG, protocol, null, null, serviceId, null, Ready.getBootstrapConfig().getActiveProfile()), registry::getStabilityLevel,null);
        if (url != null) {
            logger.debug("The final url after load balance = %s.", url.getRequestUri());
            
            return url.getRequestUri();
        } else {
            logger.debug("The service: %s cannot be found from discovery.", serviceId);
            return null;
        }
    }

    public static List<URI> discoverAllConfigNodes(String protocol, String serviceId, String profile) {
        return doDiscover(Constant.NODE_TYPE_CONFIG, protocol, null, null, serviceId, null, profile).stream()
                .map(Cloud::toUri)
                .collect(Collectors.toList());
    }

    public static URL discover(String protocol, String serviceId, String profile, String requestKey) {
        return discover(null, protocol, null, null, serviceId, null, profile, requestKey);
    }

    public static URL discover(String nodeType, String protocol, String project, String projectVersion, String serviceId, String serviceVersion, String profile, String requestKey) {
        URL url = loadBalancer.select(serviceId, doDiscover(nodeType, protocol, project, projectVersion, serviceId, serviceVersion, profile), registry::getStabilityLevel, requestKey);
        if (url != null) {
            logger.debug("The final url after load balance = %s.", url);
            return url;
        } else {
            logger.debug("The service: %s cannot be found from discovery.", serviceId);
            return null;
        }
    }

    public static List<URL> discoverAll(String protocol, String serviceId, String profile) {
        
        return doDiscover(null, protocol, null, null, serviceId, null, profile);
    }

    public static List<URL> discoverAll(String nodeType, String protocol, String serviceId, String profile) {
        return doDiscover(nodeType, protocol, null, null, serviceId, null, profile);
    }

    private static List<URL> doDiscover(String nodeType, String protocol, String project, String projectVersion, String serviceId, String serviceVersion, String profile) {
        if(logger.isDebugEnabled())
            logger.debug("nodeType = " + nodeType + ", protocol = " + protocol + ", project = " + project + ", projectVersion = " + projectVersion + ", serviceId = " + serviceId + ", serviceVersion = " + serviceVersion + ", profile = " + profile);
        
        List<URL> urls = serviceMap.get(serviceId);
        if(logger.isDebugEnabled()) logger.debug("cached serviceId " + serviceId + " urls = " + urls);
        if((urls == null) || (urls.isEmpty())) {
            URL subscribeUrl = URL.valueOf(protocol + "://0.0.0.0:0/" + serviceId);
            if(StrUtil.notBlank(nodeType)) {
                subscribeUrl.addParameter(URLParam.nodeType.getName(), nodeType);
            }
            if(StrUtil.notBlank(project)) {
                subscribeUrl.addParameter(URLParam.project.getName(), project);
            }
            if(StrUtil.notBlank(projectVersion)) {
                subscribeUrl.addParameter(URLParam.projectVersion.getName(), projectVersion);
            }
            if(StrUtil.notBlank(serviceVersion)) {
                subscribeUrl.addParameter(URLParam.serviceVersion.getName(), serviceVersion);
            }
            if(StrUtil.notBlank(profile)) {
                
                subscribeUrl.addParameter(URLParam.environment.getName(), profile);
            }
            if(logger.isDebugEnabled()) logger.debug("subscribeUrl = " + subscribeUrl.getIdentity());
            
            if(!subscribedSet.contains(subscribeUrl)) {
                registry.subscribe(subscribeUrl, new RegistryNotifyListener("DiscoverCache", serviceId));
                subscribedSet.add(subscribeUrl);
            }
            urls = registry.discover(subscribeUrl);
            if(logger.isDebugEnabled()) logger.debug("discovered urls = " + urls);
        }
        
        if(profile == null) {return urls;}
        
        if(urls != null) {
            return urls.stream()
                    .filter(url -> url.getParameter(URLParam.environment.getName()) != null
                            && (url.getParameter(URLParam.environment.getName()).equals(profile) || url.getParameter(URLParam.environment.getName()).equals(GENERAL_PROFILE)))
                    .collect(Collectors.toList());
        }
        return urls;
    }

    private static URI toUri(URL url) {
        URI uri = null;
        try {
            uri = new URI(url.getProtocol(), null, url.getHost(), url.getPort(), null, null, null);
        } catch (URISyntaxException e) {
            logger.error(e,"URISyntaxException");
        }
        return uri;
    }

    static class RegistryNotifyListener implements NotifyListener {
        private final String serviceId;
        private String name;

        RegistryNotifyListener(String listenerName, String serviceId) {
            this.name = listenerName;
            this.serviceId = serviceId;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void setName(String name) {
            this.name = name;
        }

        @Override
        public void notify(URL registryUrl, List<URL> urls) {
            logger.debug("registry is: %s", registryUrl);
            logger.debug(name + " received notification: %s update urls: %s", serviceId, urls.toString());
            if(StrUtil.notBlank(serviceId)) {
                serviceMap.put(serviceId, urls == null ? new ArrayList<>() : urls);
            }
        }
    }

    public static void setAttribute(String name, Object value) { 
        userAttributes.put(name, value);
    }

    public static  <T> T getAttribute(String name) {
        return (T)userAttributes.get(name);
    }

    public static Map<String, Object> getAttributes() {
        return userAttributes;
    }

    public static void createSnapshot() {
        
        ignite.snapshot().createSnapshot("snapshot_" + DateUtil.format(Ready.localDateTime(), "yyyyMMdd_HHmmss")).get();
    }

    public static String getBindIp() {
        if(bindIp != null) {
            return bindIp;
        }
        String ipAddress = null;
        if(serverConfig.getIp() != null && !NetUtil.ANYHOST_V4.equals(serverConfig.getIp())) {
            ipAddress = serverConfig.getIp();
        }
        if(ipAddress == null) {
            ArrayList<String> ipList = cluster().localNode().attribute("TcpCommunicationSpi.comm.tcp.addrs");
            if(ipList != null && ipList.size() > 0) {
                for(String ip : ipList) {
                    if(NetUtil.isIPv4AddressExcept127(ip)){
                        ipAddress = ip;
                        break;
                    }
                }
            }
        }
        if(ipAddress == null) {
            ipAddress = cluster().localNode().addresses().stream().
                    filter(NetUtil::isIPv4AddressExcept127).findFirst().orElseGet(()->{
                InetAddress inetAddress = NetUtil.getLocalAddress();
                return inetAddress.getHostAddress();
            });
        }
        bindIp = ipAddress;
        return bindIp;
    }

    public static String getPublishIp() {
        if(publishIp != null) return publishIp;

        String ipAddress = Ready.getProperty(Constant.READY_HOST_IP_PROPERTY);
        if(ipAddress != null){
            logger.info("Publish IP from READY_HOST_IP is " + ipAddress);
        }
        if(ipAddress == null) {
            ipAddress = cloudConfig.getPublishIp();
            if(ipAddress != null) logger.info("Registry IP from PublishIp is " + ipAddress);
        }
        if(ipAddress == null) {
            ipAddress = getBindIp();
            logger.info("Could not find Publish IP from READY_HOST_IP, so Publish IP is same as bind IP " + ipAddress);
        }
        publishIp = ipAddress;
        return publishIp;
    }

    public static String getNodeId() {
        return getConsistentId();
    }

    public static String getConsistentId() {
        if(consistentId == null) {
            consistentId = Ready.getProperty(NODE_ID_PROPERTY);
            if (consistentId == null) consistentId = cloudConfig.getNodeId();
            if (consistentId == null) { 
                StackTraceElement[] stackTraceElements = new Throwable().getStackTrace();
                for (StackTraceElement stackTraceElement : stackTraceElements) {
                    if ("main".equals(stackTraceElement.getMethodName())) {
                        String mainClass = stackTraceElement.getClassName();
                        consistentId = mainClass.substring(mainClass.lastIndexOf('.') + 1);
                        break;
                    }
                }
            }
        }
        return consistentId;
    }

    public static String getNodeName() {
        return Ready.getBootstrapConfig().getProject() + ":" + getNodeId() + ":" + cluster().localNode().attribute("TcpCommunicationSpi.comm.tcp.port");
    }

    public static String getNodeNameWithIp() {
        return getBindIp() + ":" + cluster().localNode().attribute("TcpCommunicationSpi.comm.tcp.port") + "/" + Ready.getBootstrapConfig().getProject() + "/" + getNodeId();
    }

    public static UUID getInternalNodeId() {
        return cluster().localNode().id();
    }

    public static ReliableMessage reliableMessage(){
        return reliableMessage;
    }

    public static DistributedTransactionManager getTransactionManager() {
        if(transactionManager == null) {
            if(ReadyCloud.getConfig().isEnabledDistributedTransaction()) {
                transactionManager = Ready.dbManager().getTransactionManager();
            } else {
                throw new RuntimeException("DistributedTransactionManager is not available due to isEnabledDistributedTransaction = false in cloud config.");
            }
        }
        return transactionManager;
    }

    public static final String READY_CLOUD_DATA_OLTP_NODE = "READY_CLOUD_DATA_OLTP_NODE";
    public static final String READY_CLOUD_DATA_OLAP_NODE = "READY_CLOUD_DATA_OLAP_NODE";
    public static final String READY_CLOUD_APPLICATION_NODE = "READY_CLOUD_APPLICATION_NODE";
    public static final String READY_CLOUD_CLIENT_NODE = "READY_CLOUD_CLIENT_NODE";
    private void igniteInit() {
        
        System.setProperty("IGNITE_QUIET", "false");
        System.setProperty("IGNITE_HOME", Ready.root().toAbsolutePath().toString());
        
        System.setProperty("IGNITE_CONSOLE_APPENDER", "true");
        System.setProperty("IGNITE_NO_SHUTDOWN_HOOK", "true");
        System.setProperty("IGNITE_NO_ASCII", "true");
        if(cloudConfig.isPreferIPv4() || "true".equals(System.getProperty("java.net.preferIPv4Stack"))) {

        }
        System.setProperty("org.apache.ignite.update.notifier.enabled", "false");
        System.setProperty("h2.serializeJavaObject", "false"); 

        IgniteConfiguration config = new IgniteConfiguration();

        config.setUserAttributes(getAttributes());

        config.setGridLogger(new IgniteJavaLogger());
        
        List<Integer> includeEventTypes = new ArrayList<>(List.of(EVT_NODE_JOINED, EVT_NODE_LEFT, EVT_NODE_FAILED, EVT_CLIENT_NODE_DISCONNECTED, EVT_CLIENT_NODE_RECONNECTED,
                EVT_CLUSTER_ACTIVATED, EVT_CLUSTER_DEACTIVATED,
                EVT_CACHE_NODES_LEFT, EVT_TASK_STARTED, EVT_TASK_FINISHED, EVT_TASK_FAILED, EVT_TASK_TIMEDOUT, EVT_TASK_SESSION_ATTR_SET, EVT_TASK_REDUCED
                ));
        config.setIncludeEventTypes(includeEventTypes.stream().mapToInt(Integer::valueOf).toArray());
        config.setPeerClassLoadingEnabled(cloudConfig.isPeerClassLoading());
        config.getSqlConfiguration().setSqlSchemas(CORE_DB_SCHEMA);
        config.setWorkDirectory(Ready.root().resolve("database").toAbsolutePath().toString());

        if(ReadyCloud.getNodeType().equals(NodeType.APPLICATION)) {
            System.setProperty(READY_CLOUD_APPLICATION_NODE, "true");
        }

        PriorityQueueCollisionSpi colSpi = new PriorityQueueCollisionSpi();

        config.setCollisionSpi(colSpi);
        AlwaysFailoverSpi failSpi = new AlwaysFailoverSpi();
        config.setFailoverSpi(failSpi);

        if(cloudConfig.isCacheCheckpoint()) {
            includeEventTypes.add(EVT_CACHE_OBJECT_REMOVED);
            includeEventTypes.add(EVT_CACHE_OBJECT_EXPIRED);
            config.setIncludeEventTypes(includeEventTypes.stream().mapToInt(Integer::valueOf).toArray());
            
            CacheConfiguration<String, byte[]> checkpointCache = new CacheConfiguration<>();
            checkpointCache.setName("CHECKPOINT_CACHE");
            CacheCheckpointSpi checkpointSpi = new CacheCheckpointSpi();
            checkpointSpi.setCacheName(checkpointCache.getName());
            config.setCheckpointSpi(checkpointSpi);
        }

        config.setMetricsLogFrequency(cloudConfig.getMetricsLogFrequency());
        config.setConsistentId(getConsistentId()); 
        DataStorageConfiguration dataStorageCfg = new DataStorageConfiguration();

        dataStorageCfg.getDefaultDataRegionConfiguration().setInitialSize(cloudConfig.getDefaultDataRegionInitialMemorySize());
        dataStorageCfg.getDefaultDataRegionConfiguration().setMaxSize(cloudConfig.getDefaultDataRegionMaxMemorySize());
        dataStorageCfg.setMaxWalArchiveSize(cloudConfig.getMaxWalArchiveSize());
        dataStorageCfg.setPageSize(4096 * 2); 
        if(cloudConfig.isWalPageCompression()) {
            dataStorageCfg.setWalPageCompression(DiskPageCompression.LZ4);
            dataStorageCfg.setWalCompactionLevel(cloudConfig.getWalPageCompressionLevel());
        }
        if(ReadyCloud.getNodeType().equals(NodeType.APPLICATION_WITH_OLTP)) {
            System.setProperty(READY_CLOUD_DATA_OLTP_NODE, "true");

            DataRegionConfiguration dataRegionCfg = dataStorageCfg.getDefaultDataRegionConfiguration();
            
            dataRegionCfg.setPersistenceEnabled(false); 
            dataStorageCfg.setDefaultDataRegionConfiguration(dataRegionCfg);

            DataRegionConfiguration dataRegionWithPersistence = new DataRegionConfiguration();
            dataRegionWithPersistence.setName(WITH_PERSISTENCE);
            dataRegionWithPersistence.setPersistenceEnabled(true);
            dataRegionWithPersistence.setInitialSize(cloudConfig.getDataRegionWithPersistenceInitialMemorySize());
            dataRegionWithPersistence.setMaxSize(cloudConfig.getDataRegionWithPersistenceMaxMemorySize());
            dataStorageCfg.setDataRegionConfigurations(dataRegionWithPersistence);

            config.setAuthenticationEnabled(true); 
        }
        
        DataRegionConfiguration dataRegionWithoutPersistence = new DataRegionConfiguration();
        dataRegionWithoutPersistence.setName(WITHOUT_PERSISTENCE);
        dataRegionWithoutPersistence.setPersistenceEnabled(false);
        dataRegionWithoutPersistence.setInitialSize(cloudConfig.getDataRegionWithoutPersistenceInitialMemorySize());
        dataRegionWithoutPersistence.setMaxSize(cloudConfig.getDataRegionWithoutPersistenceMaxMemorySize());

        if(dataStorageCfg.getDataRegionConfigurations() != null) {
            DataRegionConfiguration[] array = CollectionUtil.appendArray(dataStorageCfg.getDataRegionConfigurations(), dataRegionWithoutPersistence);
            dataStorageCfg.setDataRegionConfigurations(array);
        } else {
            dataStorageCfg.setDataRegionConfigurations(dataRegionWithoutPersistence);
        }
        config.setDataStorageConfiguration(dataStorageCfg);

        if(ReadyCloud.getNodeType().equals(NodeType.APPLICATION_WITH_OLAP)) {
            System.setProperty(READY_CLOUD_DATA_OLAP_NODE, "true");
            if(!cloudConfig.isOlapNodeAsComputeNode()) {
                config.setClientMode(true);
            }
        }

        AtomicConfiguration atomicCfg = new AtomicConfiguration();
        
        atomicCfg.setBackups(1);
        
        atomicCfg.setAtomicSequenceReserveSize(3000);
        config.setAtomicConfiguration(atomicCfg);

        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
        TcpCommunicationSpi commSpi = new TcpCommunicationSpi();
        commSpi.setMessageQueueLimit(10000*100); 
        commSpi.setSlowClientQueueLimit(1000);

        TcpDiscoveryMulticastIpFinder tcMP = new TcpDiscoveryMulticastIpFinder();

        if(cloudConfig.getTcpClientPort() != null) { 
            ConnectorConfiguration connectorConfig = new ConnectorConfiguration();
            connectorConfig.setPort(cloudConfig.getTcpClientPort());
            config.setConnectorConfiguration(connectorConfig);
        }

        ClientConnectorConfiguration clientConfig = null;
        clientConfig = new ClientConnectorConfiguration();
        clientConfig.setPort(cloudConfig.getThinClientPort()); 
        
        config.setClientConnectorConfiguration(clientConfig);

        TcpDiscoveryJdbcIpFinder jdbcIpFinder = null;
        if(cloudConfig.isJdbcIpFinder()) {
            DataSourceConfig hikariConfig = Ready.getMainApplicationConfig().getDatabase().getDataSource(cloudConfig.getJdbcIpFinderDatasource());
            jdbcIpFinder = new TcpDiscoveryJdbcIpFinder(() -> "ready_jdbc_ip_finder");
            jdbcIpFinder.setDataSource(new HikariCp(hikariConfig).getDataSource());
        }

        TcpDiscoveryVmIpFinder ipFinder = null;
        ArrayList<String> addresses = new ArrayList<>();
        if(cloudConfig.getIpFinder() != null && cloudConfig.getIpFinder().size() > 0) {

            addresses = cloudConfig.getIpFinder().stream().map(ip -> ip + ":" + (StrUtil.notBlank(cloudConfig.getIpFinderPort()) ? cloudConfig.getIpFinderPort() : cloudConfig.getDiscoveryPort())).collect(Collectors.toCollection(ArrayList::new));
        }
        if(StrUtil.notBlank(cloudConfig.getGroup()))
            tcMP.setMulticastGroup(cloudConfig.getGroup());
        if(serverConfig.getIp() != null && !NetUtil.ANYHOST_V4.equals(serverConfig.getIp())) {
            commSpi.setLocalAddress(serverConfig.getIp());
            discoverySpi.setLocalAddress(serverConfig.getIp());
            addresses.add(serverConfig.getIp() + ":" + cloudConfig.getDiscoveryPort());
        }
        if(addresses.size() > 0) {
            tcMP.setAddresses(addresses);
        }
        if(cloudConfig.getMulticastPort() != null)  
            tcMP.setMulticastPort(cloudConfig.getMulticastPort());

        if(cloudConfig.getDiscoveryPort() != null)  
            discoverySpi.setLocalPort(cloudConfig.getDiscoveryPort());
        if(cloudConfig.getCommunicationPort() != null)  
            commSpi.setLocalPort(cloudConfig.getCommunicationPort());
        if(serverConfig.isDynamicPort()){
            int portRange = serverConfig.getMaxPort() - serverConfig.getMinPort();
            portRange = serverConfig.getMinPort() < 1024 ? serverConfig.getMaxPort() - 1024 : portRange;
            if(portRange > 100) portRange = 100; 
            discoverySpi.setLocalPort(serverConfig.getMinPort() < 1024 ? 1024 : serverConfig.getMinPort());
            discoverySpi.setLocalPortRange(portRange);
            commSpi.setLocalPort(serverConfig.getMinPort() < 1024 ? 1025 : serverConfig.getMinPort() + 1);
            commSpi.setLocalPortRange(portRange - 1);
        }

        discoverySpi.setIpFinder(jdbcIpFinder != null ? jdbcIpFinder : (ipFinder != null ? ipFinder : tcMP));  
        discoverySpi.setNetworkTimeout(5000); 
        discoverySpi.setSocketTimeout(2000);  
        discoverySpi.setAckTimeout(2000); 
        config.setDiscoverySpi(discoverySpi);

        commSpi.setConnectTimeout(1000);  
        config.setCommunicationSpi(commSpi);

        config.setRebalanceThreadPoolSize(1);
        
        config.setRebalanceBatchSize(2 * 1024 * 1024);
        config.setRebalanceThrottle(100);

        try {
            ignite = IgnitionEx.start(config);
        }catch (IgniteCheckedException e) {
            throw U.convertException(e);
        }
        try {
            Method removeFunctions = FunctionsManager.class.getDeclaredMethod("removeFunctions", Set.class);
            DistributedSqlConfiguration.DFLT_DISABLED_FUNCS.remove("LOCK_MODE");
            removeFunctions.setAccessible(true);
            removeFunctions.invoke(FunctionsManager.class, DistributedSqlConfiguration.DFLT_DISABLED_FUNCS);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) { }

        LogFactory.setConsoleLevel(Optional.ofNullable(Ready.getBootstrapConfig().getLog().getConsoleLogLevel()).orElse("ALL"));
    }

    public static String getCoordinator() {
        if(ignite.configuration().getDiscoverySpi() instanceof TcpDiscoverySpi) {
            TcpDiscoverySpi tds = (TcpDiscoverySpi) ignite.configuration().getDiscoverySpi();
            UUID uuid = tds.getCoordinator();
            ClusterNode node = ignite.cluster().nodes().stream().filter(n -> n.id().equals(uuid)).findFirst().get();
            
            return String.format("nodeId [%s], address [%s]", uuid.toString(), node.addresses().toString());
        }
        return null;
    }

    public static boolean isRebalanced(String cacheName){
        AffinityTopologyVersion topVer0 = new AffinityTopologyVersion(0);
        return ((IgniteKernal)ignite).context().cache().context().cacheContext(GridCacheUtils.cacheId(cacheName)).topology().rebalanceFinished(topVer0);
    }

    public static void createTable(Class<? extends Model> modelClass, TableConfig config) {
        getDb().update(getDDL(modelClass, config));
    }

    public static void dropTable(String tableName) {
        getDb().update("DROP TABLE IF EXISTS PUBLIC." + tableName);
    }

    public static String getDDL(Class<? extends Model> modelClass, TableConfig config) {
        return getDDL(modelClass, config, false);
    }

    public static String getDDL(Class<? extends Model> modelClass, TableConfig config, boolean withDropTable) {
        Method[] ignoreMethods = Model.class.getMethods();
        Method[] potentialMethods = modelClass.getMethods();
        Set<Method> methods = new LinkedHashSet<>(Arrays.asList(potentialMethods));
        methods.removeAll(Arrays.asList(ignoreMethods));
        Map<String, Class<?>> potentialFieldMap = new LinkedHashMap<>();
        Map<String, Class<?>> finalFieldMap = new LinkedHashMap<>();
        for(Method method : methods) {
            if(method.getName().startsWith("set") && method.getParameterCount() == 1) {
                
                if(ClassUtil.isSimpleType(method.getParameterTypes()[0]) || java.util.UUID.class.equals(method.getParameterTypes()[0]) || byte.class.equals(method.getParameterTypes()[0].getComponentType())){
                    potentialFieldMap.put(method.getName().substring(3), method.getParameterTypes()[0]);
                }
            }
        }
        for(Method method : methods) {
            if(method.getParameterCount() == 0) {
                if (method.getName().startsWith("get")) {
                    
                    if (ClassUtil.isSimpleType(method.getReturnType()) || java.util.UUID.class.equals(method.getParameterTypes()[0]) || byte.class.equals(method.getReturnType().getComponentType())) {
                        String name = method.getName().substring(3);
                        if (potentialFieldMap.containsKey(name) && potentialFieldMap.get(name).equals(method.getReturnType())) {
                            finalFieldMap.put(StrUtil.firstCharToLowerCase(name), method.getReturnType());
                        }
                    }
                } else if (method.getName().startsWith("is")) {
                    if(Boolean.class.equals(method.getReturnType()) || boolean.class.equals(method.getReturnType())) {
                        String name = method.getName().substring(2);
                        if (potentialFieldMap.containsKey(name) && potentialFieldMap.get(name).equals(method.getReturnType())) {
                            finalFieldMap.put(StrUtil.firstCharToLowerCase(name), method.getReturnType());
                        }
                    }
                }
            }
        }
        StringBuilder sql = new StringBuilder();
        String schemaName = config.schema;
        String tableName =  config.getTableName() == null ? modelClass.getSimpleName() : config.getTableName();
        if(withDropTable) {
            sql.append("DROP TABLE IF EXISTS ").append(schemaName).append(".").append(tableName).append(";");
        }
        sql.append("CREATE TABLE IF NOT EXISTS ").append(schemaName).append(".").append(tableName).append('(');
        finalFieldMap.forEach((field, type)->{
            sql.append(field).append(" ").append(CloudDb.typeMap.get(type)).append(",");
        });
        sql.append(" PRIMARY KEY (");
        if(config.getKeyField() == null || config.getKeyField().isEmpty()) {
            throw new RuntimeException("Key field cannot be empty, all fields: " + finalFieldMap.keySet());
        } else {
            if(finalFieldMap.keySet().containsAll(config.getKeyField())){
                sql.append(StrUtil.join(config.getKeyField(), ","));
            } else {
                throw new RuntimeException("Please check the key fields: " + config.getKeyField() + ", all fields: " + finalFieldMap.keySet());
            }
        }
        sql.append("))");
        Map<String, String> withParam = new HashMap<>();
        if(config.getTableName() != null) {
            withParam.put(TableConfig.CACHE_NAME, config.getTableName());
        }
        if(config.getMode() != null) {
            withParam.put(TableConfig.MODE, config.getMode().toString());
        }
        if(config.getTemplate() != null) {
            withParam.put(TableConfig.TEMPLATE, config.getTemplate());
        }
        if(config.getBackups() != null) {
            withParam.put(TableConfig.BACKUPS, config.getBackups().toString());
        }
        if(config.getAtomicity() != null) {
            withParam.put(TableConfig.ATOMICITY, config.getAtomicity());
        }
        if(config.getWriteSynchronizationMode() != null) {
            withParam.put(TableConfig.WRITE_SYNCHRONIZATION_MODE, config.getWriteSynchronizationMode());
        }
        if(config.getCacheGroup() != null) {
            withParam.put(TableConfig.CACHE_GROUP, config.getCacheGroup());
        }
        if(config.getAffinityKey() != null) {
            withParam.put(TableConfig.AFFINITY_KEY, config.getAffinityKey());
        }
        if(config.getDataRegion() != null) {
            withParam.put(TableConfig.DATA_REGION, config.getDataRegion());
        }
        verifyTableConfig(schemaName, tableName, withParam);
        if(withParam.size() > 0){
            sql.append(" WITH \"");
            for(var entry : withParam.entrySet()){
                sql.append(entry.getKey()).append("=").append(entry.getValue()).append(",");
            }
            sql.deleteCharAt(sql.length() - 1); 
            sql.append("\";");
        }
        if(config.getIndexField() != null){
            StringBuilder idxSql = new StringBuilder("CREATE INDEX IF NOT EXISTS ");
            for(var index : config.getIndexField()) {
                idxSql.append(schemaName).append(".");
                if(index.getName() == null) {
                    idxSql.append(QueryUtils.indexName(tableName, index));
                } else {
                    idxSql.append(index.getName());
                }
                idxSql.append(" ON ");
                idxSql.append(schemaName).append(".");
                idxSql.append(tableName);
                idxSql.append("(");
                for (Map.Entry<String, Boolean> entry : index.getFields().entrySet()) {
                    if(!finalFieldMap.keySet().contains(entry.getKey())) throw new RuntimeException("Please check the index field: " + entry.getKey() + ", all fields: " + finalFieldMap.keySet());
                    idxSql.append(entry.getKey()).append(entry.getValue() ? " ASC" : " DESC");
                    idxSql.append(",");
                }
                idxSql.deleteCharAt(idxSql.length() - 1); 
                idxSql.append(");");
            }
            sql.append(idxSql);
        }
        return sql.toString();
    }

    public boolean verifyCreateTableDDL(CreateTable statement) {
        String schemaName = statement.getTable().getSchemaName() == null ? "PUBLIC" : statement.getTable().getSchemaName();
        String tableName = statement.getTable().getName();
        List<String> tableOptions = statement.getTableOptionsStrings();
        Map<String, String> withParam = new HashMap<>();
        int optIndex = -1;
        if(tableOptions != null && tableOptions.size() > 1) {
            for(int i = 0; i < tableOptions.size(); i++) {
                if("with".equalsIgnoreCase(tableOptions.get(i))){
                    optIndex = i;
                }
            }
            if(optIndex != -1) {
                String config = tableOptions.get(optIndex + 1);
                if (config.startsWith("\"") || config.startsWith("'")) {
                    config = config.substring(1);
                }
                if (config.endsWith("\"") || config.endsWith("'")) {
                    config = config.substring(0, config.length() - 1);
                }
                String[] items = StrUtil.split(config, ',');
                for (String item : items) {
                    String[] keyVal = StrUtil.split(item, "=");
                    if (keyVal.length == 2) {
                        withParam.put(keyVal[0].toUpperCase(), keyVal[1]);
                    }
                }
            }
        }
        verifyTableConfig(schemaName, tableName, withParam);
        if(withParam.size() > 0) {
            boolean isAppend = false;
            if(optIndex == -1) {
                if(tableOptions == null) {
                    tableOptions = new ArrayList<>();
                }
                optIndex = tableOptions.size();
                isAppend = true;
            }
            if(!isAppend) tableOptions.remove(optIndex);
            tableOptions.add(optIndex, "WITH");
            StringBuilder sb = new StringBuilder();
            for(var entry : withParam.entrySet()){
                if(sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(entry.getKey()).append("=").append(entry.getValue());
            }
            if(!isAppend) tableOptions.remove(optIndex + 1);
            tableOptions.add(optIndex + 1, '"' + sb.toString() + '"');
            statement.setTableOptionsStrings(tableOptions);
            return true;
        }
        return false;
    }

    @Deprecated
    public static String verifyCreateTableDDL(String sql) {
        String tidySql = StrUtil.replaceWhitespace(sql, ' ', true, true);
        String tidySqlUpperCase = tidySql.toUpperCase();
        boolean isCreateTable = tidySqlUpperCase.startsWith("CREATE TABLE ");
        StringBuilder newSql = null;
        if(isCreateTable) {
            String tableNameStr = tidySql.substring(13, tidySql.indexOf("(", 13)).trim();
            String tableName = tableNameStr.substring(tableNameStr.lastIndexOf(" ") + 1).replace("\"","");
            String schemaName = "PUBLIC";
            if(tableName.indexOf(".") > 0) {
                String[] names = StrUtil.split(tableName, '.');
                schemaName = names[0];
                tableName = names[1];
            }
            int with = tidySqlUpperCase.indexOf(" WITH ");
            Map<String, String> withParam = new HashMap<>();
            if(with > 0) { 
                int first = tidySql.indexOf('"', with + 6);
                int second = tidySql.indexOf('"', first + 1);
                if(first > 0 && second > 0) {
                    String config = tidySql.substring(first + 1, second);
                    String[] items = StrUtil.split(config, ',');
                    for(String item : items){
                        String[] keyVal = StrUtil.split(item, "=");
                        if(keyVal.length == 2) withParam.put(keyVal[0].toUpperCase(), keyVal[1]);
                    }
                    verifyTableConfig(schemaName, tableName, withParam);
                }
            } else { 
                verifyTableConfig(schemaName, tableName, withParam);
            }
            if(withParam.size() > 0) {
                if(with > 0) {
                    newSql = new StringBuilder(tidySql.substring(0, with));
                } else {
                    newSql = new StringBuilder((tidySql.endsWith(";")) ? tidySql.substring(0, tidySql.length() - 1) : tidySql);
                }
                newSql.append(" WITH \"");
                for(var entry : withParam.entrySet()){
                    newSql.append(entry.getKey()).append("=").append(entry.getValue()).append(",");
                }
                newSql.deleteCharAt(newSql.length() - 1); 
                newSql.append("\";");
            }
        }
        return newSql == null ? null : newSql.toString();
    }

    private static void verifyTableConfig(String schemaName, String tableName, Map<String, String> withParam) {
        List<String> problems = new ArrayList<>();
        if(withParam.get(TableConfig.MODE) != null) {
            if(!CloudDb.allowedTableMode.contains(withParam.get(TableConfig.MODE))) {
                problems.add("unknown table mode " + withParam.get(TableConfig.MODE) + ", options are " + CloudDb.allowedTableMode);
            } else {
                withParam.put(TableConfig.TEMPLATE, withParam.get(TableConfig.MODE));
            }
            withParam.remove(TableConfig.MODE);
        } else {
            withParam.putIfAbsent(TableConfig.TEMPLATE, CloudDb.TableMode.REPLICATED_MEMORY.name()); 
        }
        String cacheName = tableName;
        if(withParam.get(TableConfig.CACHE_NAME) != null) {
            cacheName = withParam.get(TableConfig.CACHE_NAME);
        }
        List<Record> sysView = getCoreDb().find("SELECT TABLE_NAME, SCHEMA_NAME, CACHE_NAME, CACHE_ID, AFFINITY_KEY_COLUMN, KEY_ALIAS, VALUE_ALIAS, KEY_TYPE_NAME, VALUE_TYPE_NAME, IS_INDEX_REBUILD_IN_PROGRESS FROM SYS.TABLES WHERE CACHE_NAME=?;", cacheName);
        if(sysView.size() > 0) {
            if(!sysView.get(0).getStr("SCHEMA_NAME").equals(schemaName) || !sysView.get(0).getStr("TABLE_NAME").equals(tableName.toUpperCase())){
                problems.add("The name '" + cacheName + "' is already in use by " + sysView.get(0).getStr("SCHEMA_NAME") + "." + sysView.get(0).getStr("TABLE_NAME") + ", please change to another name.");
            }
        }
        withParam.put(TableConfig.CACHE_NAME, cacheName);

        withParam.putIfAbsent(TableConfig.ATOMICITY, "TRANSACTIONAL_SNAPSHOT");

        if(!problems.isEmpty()) {
            throw new RuntimeException("CREATE TABLE " + tableName + " [ " + String.join(" | ", problems) + " ]");
        }
    }

    @Deprecated
    private void verifyCacheConfig(CacheConfiguration configuration) {
        if(ReadyCloud.getNodeMode().equals(NodeMode.REPLICATED)){
            if(configuration.getCacheMode().equals(CacheMode.PARTITIONED)){
                if(GridCacheUtils.isPersistentCache(configuration, configuration().getDataStorageConfiguration()))
                throw new RuntimeException("this node is declared to work in REPLICATED mode, but PARTITIONED persistence cache configuration appears");
            }
        } else if(ReadyCloud.getNodeMode().equals(NodeMode.PARTITIONED)) {
            if(configuration.getCacheMode().equals(CacheMode.REPLICATED)){
                
            }
        }
        
    }

    public static <K,V> IgniteCache<K, V> expire(IgniteCache<K, V> cache, long expireTime) {
        ExpiryPolicy plc = new CreatedExpiryPolicy(new Duration(TimeUnit.MILLISECONDS,expireTime));
        return cache.withExpiryPolicy(plc);
    }

    public static <K,V> void putWithExpiration(IgniteCache<K, V> cache, K key, V value, long expireTime) {
        ExpiryPolicy plc = new CreatedExpiryPolicy(new Duration(TimeUnit.MILLISECONDS,expireTime));
        IgniteCache<K,V> cache0 = cache.withExpiryPolicy(plc);
        cache0.getAndPut(key, value);
    }

    public <K, V> CacheConfiguration<K, V> newCacheConfig(String cacheName) {
        return newCacheConfig(cacheName, false, false, 0, false);
    }

    public <K, V> CacheConfiguration<K, V> newCacheConfig(String cacheName, boolean partitioned, boolean transactional, int backups, boolean persistence) {
        CacheConfiguration<K, V> config = new CacheConfiguration<>();
        config.setName(cacheName);
        config.setEventsDisabled(true);
        config.setAtomicityMode(transactional ? CacheAtomicityMode.TRANSACTIONAL : CacheAtomicityMode.ATOMIC);
        if(partitioned) {
            config.setCacheMode(CacheMode.PARTITIONED);
            config.setBackups(backups);
        } else {
            config.setCacheMode(CacheMode.REPLICATED);
        }
        config.setSqlEscapeAll(false); 
        config.setDataRegionName(persistence ? Cloud.WITH_PERSISTENCE : Cloud.WITHOUT_PERSISTENCE);
        if(persistence && cloudConfig.isDiskPageCompression()) {
            
            config.setDiskPageCompression(DiskPageCompression.LZ4);
            
            config.setDiskPageCompressionLevel(cloudConfig.getDiskPageCompressionLevel());
        }
        config.setWriteSynchronizationMode(CacheWriteSynchronizationMode.PRIMARY_SYNC);
        config.setPartitionLossPolicy(PartitionLossPolicy.READ_WRITE_SAFE);  
        return config;
    }

    public <K, V> IgniteCache<K, V> createCache(String cacheName) throws CacheException {
        return ignite.createCache(newCacheConfig(cacheName, false, false, 0, false));
    }

    public <K, V> IgniteCache<K, V> getOrCreateCache(String cacheName) throws CacheException {
        return ignite.getOrCreateCache(newCacheConfig(cacheName, false, false, 0, false));
    }

    public <K, V> IgniteCache<K, V> createCache(String cacheName, boolean transactional, int backups) throws CacheException {
        return ignite.createCache(newCacheConfig(cacheName, false, transactional, backups, false));
    }

    public <K, V> IgniteCache<K, V> getOrCreateCache(String cacheName, boolean transactional, int backups) throws CacheException {
        return ignite.getOrCreateCache(newCacheConfig(cacheName, false, transactional, backups, false));
    }

    public <K, V> IgniteCache<K, V> createCache(String cacheName, boolean partitioned, boolean transactional, int backups) throws CacheException {
        return ignite.createCache(newCacheConfig(cacheName, partitioned, transactional, backups, false));
    }

    public <K, V> IgniteCache<K, V> getOrCreateCache(String cacheName, boolean partitioned, boolean transactional, int backups) throws CacheException {
        return ignite.getOrCreateCache(newCacheConfig(cacheName, partitioned, transactional, backups, false));
    }

    public <K, V> IgniteCache<K, V> createCache(String cacheName, boolean partitioned, boolean transactional, int backups, boolean persistence) throws CacheException {
        return ignite.createCache(newCacheConfig(cacheName, partitioned, transactional, backups, persistence));
    }

    public <K, V> IgniteCache<K, V> getOrCreateCache(String cacheName, boolean partitioned, boolean transactional, int backups, boolean persistence) throws CacheException {
        return ignite.getOrCreateCache(newCacheConfig(cacheName, partitioned, transactional, backups, persistence));
    }

    public GridKernalContext context() {
        return ((IgniteKernal)ignite).context();
    }

    static class shutdownNode implements IgniteRunnable {
        @IgniteInstanceResource
        private transient Ignite ignite;

        @Override
        public void run() {
            new Thread() {
                @Override public void run() {
                    ignite.close();
                }
            }.start();
        }
    }

    public void shutdownAllNodes(){

        ignite.compute(ignite.cluster().forRemotes()).broadcast(new shutdownNode());
    }

    public boolean stop(){
        return IgnitionEx.stop(false, null);
    }

    public static String name() {
        return ignite.name();
    }

    public static IgniteLogger log() {
        return ignite.log();
    }

    public static IgniteConfiguration configuration() {
        return ignite.configuration();
    }

    public static IgniteCluster cluster() {
        return ignite.cluster();
    }

    public static IgniteCompute compute() {
        return ignite.compute();
    }

    public static IgniteCompute compute(ClusterGroup grp) {
        return ignite.compute(grp);
    }

    public static IgniteMessaging message() {
        return ignite.message();
    }

    public static IgniteMessaging message(ClusterGroup grp) {
        return ignite.message(grp);
    }

    public static IgniteEvents events() {
        return ignite.events();
    }

    public static IgniteEvents events(ClusterGroup grp) {
        return ignite.events(grp);
    }

    public static IgniteServices services() {
        return ignite.services();
    }

    public static IgniteServices services(ClusterGroup grp) {
        return ignite.services(grp);
    }

    public static ExecutorService executorService() {
        return ignite.executorService();
    }

    public static ExecutorService executorService(ClusterGroup grp) {
        return ignite.executorService(grp);
    }

    public static IgniteProductVersion version() {
        return ignite.version();
    }

    public static IgniteScheduler scheduler() {
        return ignite.scheduler();
    }

    public static <K, V> IgniteCache<K, V> createCache(CacheConfiguration<K, V> cacheCfg) throws CacheException {
        return ignite.createCache(cacheCfg);
    }

    public static Collection<IgniteCache> createCaches(Collection<CacheConfiguration> cacheCfgs) throws CacheException {
        return ignite.createCaches(cacheCfgs);
    }

    public static <K, V> IgniteCache<K, V> getOrCreateCache(CacheConfiguration<K, V> cacheCfg) throws CacheException {
        return ignite.getOrCreateCache(cacheCfg);
    }

    public static Collection<IgniteCache> getOrCreateCaches(Collection<CacheConfiguration> cacheCfgs) throws CacheException {
        return ignite.getOrCreateCaches(cacheCfgs);
    }

    public static <K, V> void addCacheConfiguration(CacheConfiguration<K, V> cacheCfg) throws CacheException {
        ignite.addCacheConfiguration(cacheCfg);
    }

    public static <K, V> IgniteCache<K, V> createCache(CacheConfiguration<K, V> cacheCfg, NearCacheConfiguration<K, V> nearCfg) throws CacheException {
        return ignite.createCache(cacheCfg, nearCfg);
    }

    public static <K, V> IgniteCache<K, V> getOrCreateCache(CacheConfiguration<K, V> cacheCfg, NearCacheConfiguration<K, V> nearCfg) throws CacheException {
        return ignite.getOrCreateCache(cacheCfg, nearCfg);
    }

    public static <K, V> IgniteCache<K, V> createNearCache(String cacheName, NearCacheConfiguration<K, V> nearCfg) throws CacheException {
        return ignite.createNearCache(cacheName, nearCfg);
    }

    public static <K, V> IgniteCache<K, V> getOrCreateNearCache(String cacheName, NearCacheConfiguration<K, V> nearCfg) throws CacheException {
        return ignite.getOrCreateNearCache(cacheName, nearCfg);
    }

    public static void destroyCache(String cacheName) throws CacheException {
        ignite.destroyCache(cacheName);
    }

    public static void destroyCaches(Collection<String> cacheNames) throws CacheException {
        ignite.destroyCaches(cacheNames);
    }

    public static <K, V> IgniteCache<K, V> cache(String name) throws CacheException {
        return ignite.cache(name);
    }

    public static Collection<String> cacheNames() {
        return ignite.cacheNames();
    }

    public static IgniteTransactions transactions() {
        return ignite.transactions();
    }

    public static <K, V> IgniteDataStreamer<K, V> dataStreamer(String cacheName) throws IllegalStateException {
        return ignite.dataStreamer(cacheName);
    }

    public static IgniteAtomicSequence atomicSequence(String name, long initVal, boolean create) throws IgniteException {
        return ignite.atomicSequence(name, initVal, create);
    }

    public static IgniteAtomicSequence atomicSequence(String name, AtomicConfiguration cfg, long initVal, boolean create) throws IgniteException {
        return ignite.atomicSequence(name, cfg, initVal, create);
    }

    public static IgniteAtomicLong atomicLong(String name, long initVal, boolean create) throws IgniteException {
        return ignite.atomicLong(name, initVal, create);
    }

    public static IgniteAtomicLong atomicLong(String name, AtomicConfiguration cfg, long initVal, boolean create) throws IgniteException {
        return ignite.atomicLong(name, cfg, initVal, create);
    }

    public static <T> IgniteAtomicReference<T> atomicReference(String name, @Nullable T initVal, boolean create) throws IgniteException {
        return ignite.atomicReference(name, initVal, create);
    }

    public static <T> IgniteAtomicReference<T> atomicReference(String name, AtomicConfiguration cfg, @Nullable T initVal, boolean create) throws IgniteException {
        return ignite.atomicReference(name, cfg, initVal, create);
    }

    public static <T, S> IgniteAtomicStamped<T, S> atomicStamped(String name, @Nullable T initVal, @Nullable S initStamp, boolean create) throws IgniteException {
        return ignite.atomicStamped(name, initVal, initStamp, create);
    }

    public static <T, S> IgniteAtomicStamped<T, S> atomicStamped(String name, AtomicConfiguration cfg, @Nullable T initVal, @Nullable S initStamp, boolean create) throws IgniteException {
        return ignite.atomicStamped(name, cfg, initVal, initStamp, create);
    }

    public static IgniteCountDownLatch countDownLatch(String name, int cnt, boolean autoDel, boolean create) throws IgniteException {
        return ignite.countDownLatch(name, cnt, autoDel, create);
    }

    public static IgniteSemaphore semaphore(String name, int cnt, boolean failoverSafe, boolean create) throws IgniteException {
        return ignite.semaphore(name, cnt, failoverSafe, create);
    }

    public static IgniteLock reentrantLock(String name, boolean failoverSafe, boolean fair, boolean create) throws IgniteException {
        return ignite.reentrantLock(name, failoverSafe, fair, create);
    }

    public static <T> IgniteQueue<T> queue(String name, int cap, @Nullable CollectionConfiguration cfg) throws IgniteException {
        return ignite.queue(name, cap, cfg);
    }

    public static <T> IgniteSet<T> set(String name, @Nullable CollectionConfiguration cfg) throws IgniteException {
        return ignite.set(name, cfg);
    }

    public static <T extends IgnitePlugin> T plugin(String name) throws PluginNotFoundException {
        return ignite.plugin(name);
    }

    public static IgniteBinary binary() {
        return ignite.binary();
    }

    public static void close() throws IgniteException {
        ignite.close();
    }

    public static <K> Affinity<K> affinity(String cacheName) {
        return ignite.affinity(cacheName);
    }

    @Deprecated
    public static boolean active() {
        return ignite.active();
    }

    @Deprecated
    public static void active(boolean active) {
        ignite.active(active);
    }

    public static void resetLostPartitions(Collection<String> cacheNames) {
        ignite.resetLostPartitions(cacheNames);
    }

    @Deprecated
    public static Collection<MemoryMetrics> memoryMetrics() {
        return ignite.memoryMetrics();
    }

    @Deprecated
    public static @Nullable MemoryMetrics memoryMetrics(String memPlcName) {
        return ignite.memoryMetrics(memPlcName);
    }

    @Deprecated
    public static PersistenceMetrics persistentStoreMetrics() {
        return ignite.persistentStoreMetrics();
    }

    public static Collection<DataRegionMetrics> dataRegionMetrics() {
        return ignite.dataRegionMetrics();
    }

    public static @Nullable DataRegionMetrics dataRegionMetrics(String memPlcName) {
        return ignite.dataRegionMetrics(memPlcName);
    }

    public static DataStorageMetrics dataStorageMetrics() {
        return ignite.dataStorageMetrics();
    }

    public static IgniteEncryption encryption() {
        return ignite.encryption();
    }

    public static IgniteSnapshot snapshot() {
        return ignite.snapshot();
    }

    @IgniteExperimental
    public static @NotNull TracingConfigurationManager tracingConfiguration() {
        return ignite.tracingConfiguration();
    }
}
