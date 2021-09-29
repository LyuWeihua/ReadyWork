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

import org.apache.ignite.configuration.DataStorageConfiguration;
import work.ready.cloud.client.ClientConfig;
import work.ready.cloud.config.ConfigClientConfig;
import work.ready.cloud.config.ConfigServerConfig;
import work.ready.cloud.transaction.loadbalance.DtxOptimizedLoadBalancer;
import work.ready.cloud.loadbalance.LoadBalancer;
import work.ready.cloud.loadbalance.LocalFirstLoadBalancer;
import work.ready.cloud.loadbalance.RoundRobinLoadBalancer;
import work.ready.cloud.registry.RegistryConfig;
import work.ready.cloud.transaction.TransactionConfig;
import work.ready.core.config.BaseConfig;
import work.ready.core.database.DatabaseManager;
import work.ready.core.server.Ready;
import work.ready.core.server.ServerConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.ignite.configuration.DataStorageConfiguration.DFLT_WAL_ARCHIVE_MAX_SIZE;

public class CloudConfig extends BaseConfig {

    private String group = "228.10.10.88";  
    private String clusterId = "default";
    private String publishIp;  
    private List<String> ipFinder; 
    private String ipFinderPort; 
    private boolean jdbcIpFinder = false;
    private String jdbcIpFinderDatasource = DatabaseManager.MAIN_CONFIG_NAME;
    private Integer communicationPort = 16655;
    private Integer multicastPort = 16666; 
    private Integer discoveryPort = 16677;
    private Integer tcpClientPort = 16688;
    private Integer thinClientPort = 16699;
    private boolean preferIPv4 = true;
    private String nodeId;
    private String dataOltpNodes;

    private String dataOlapNodes;
    private boolean olapNodeAsComputeNode = false;  
    private String olapClusterName = "READY_OLAP_CLUSTER";
    private String elasticSearchVersion = "7.10.0";  
    
    private List<String> elasticSearchDownloadUrls;
    private Map<String, Object> elasticSearchSettings;

    private boolean dataNodeAutoJoin = true;

    private boolean autoRebalance = true;
    private long rebalanceTimeOut = 30000;

    private long defaultDataRegionInitialMemorySize = Math.min(
            DataStorageConfiguration.DFLT_DATA_REGION_MAX_SIZE, DataStorageConfiguration.DFLT_DATA_REGION_INITIAL_SIZE);
    private long defaultDataRegionMaxMemorySize = DataStorageConfiguration.DFLT_DATA_REGION_MAX_SIZE;

    private long dataRegionWithoutPersistenceInitialMemorySize = defaultDataRegionInitialMemorySize;
    private long dataRegionWithoutPersistenceMaxMemorySize = defaultDataRegionMaxMemorySize;

    private long dataRegionWithPersistenceInitialMemorySize = defaultDataRegionInitialMemorySize;
    private long dataRegionWithPersistenceMaxMemorySize = defaultDataRegionMaxMemorySize;

    private boolean diskPageCompression = false;
    private int diskPageCompressionLevel = 0;
    private long maxWalArchiveSize = DFLT_WAL_ARCHIVE_MAX_SIZE;
    private boolean walPageCompression = false;
    private int walPageCompressionLevel = 0;

    private boolean peerClassLoading = false;

    private boolean cacheCheckpoint = false;
    private long metricsLogFrequency = 0;
    private boolean allowH2DB = false;

    private boolean enabledDistributedTransaction = true;
    private String dbUser = "root";
    private String dbPassword = "123456";

    private boolean reliableMessageLogger = false;
    private int reliableMessageTimeout = 5000;

    public int getReliableMessageTimeout() {
        return reliableMessageTimeout;
    }

    public void setReliableMessageTimeout(int reliableMessageTimeout) {
        this.reliableMessageTimeout = reliableMessageTimeout;
    }

    public boolean isReliableMessageLogger() {
        return reliableMessageLogger;
    }

    public void setReliableMessageLogger(boolean reliableMessageLogger) {
        this.reliableMessageLogger = reliableMessageLogger;
    }

    private TransactionConfig transaction = new TransactionConfig();

    private ConfigClientConfig configClient = new ConfigClientConfig();

    private ConfigServerConfig configServer = new ConfigServerConfig();

    private RegistryConfig registry = new RegistryConfig();

    private String loadBalancer = RoundRobinLoadBalancer.name;
    
    private transient Map<String, Class<? extends LoadBalancer>> supportedLoadBalancer = new HashMap<>(Map.of(
            RoundRobinLoadBalancer.name, RoundRobinLoadBalancer.class,
            LocalFirstLoadBalancer.name, LocalFirstLoadBalancer.class
    ));

    private boolean autoDiscover;

    private ClientConfig httpClient = new ClientConfig();

    private boolean trustAllService = true;

    public String getPublishIp() {
        return publishIp;
    }

    public CloudConfig setPublishIp(String publishIp) {
        this.publishIp = publishIp;
        return this;
    }

    public List<String> getIpFinder() {
        return ipFinder;
    }

    public CloudConfig setIpFinder(List<String> ipFinder) {
        this.ipFinder = ipFinder;
        return this;
    }

    public CloudConfig addIpFinder(String ipFinder) {
        if(this.ipFinder == null) this.ipFinder = new ArrayList<>();
        this.ipFinder.add(ipFinder);
        return this;
    }

    public String getIpFinderPort() {
        return ipFinderPort;
    }

    public CloudConfig setIpFinderPort(String ipFinderPort) {
        this.ipFinderPort = ipFinderPort;
        return this;
    }

    public boolean isJdbcIpFinder() {
        return jdbcIpFinder;
    }

    public void setJdbcIpFinder(boolean jdbcIpFinder) {
        this.jdbcIpFinder = jdbcIpFinder;
    }

    public String getJdbcIpFinderDatasource() {
        return jdbcIpFinderDatasource;
    }

    public void setJdbcIpFinderDatasource(String jdbcIpFinderDatasource) {
        this.jdbcIpFinderDatasource = jdbcIpFinderDatasource;
    }

    public Integer getDiscoveryPort() {
        return discoveryPort;
    }

    public CloudConfig setDiscoveryPort(Integer discoveryPort) {
        this.discoveryPort = discoveryPort;
        return this;
    }

    public Integer getMulticastPort() {
        return multicastPort;
    }

    public CloudConfig setMulticastPort(Integer multicastPort) {
        this.multicastPort = multicastPort;
        return this;
    }

    public Integer getCommunicationPort() {
        return communicationPort;
    }

    public CloudConfig setCommunicationPort(Integer communicationPort) {
        this.communicationPort = communicationPort;
        return this;
    }

    public Integer getTcpClientPort() {
        return tcpClientPort;
    }

    public CloudConfig setTcpClientPort(Integer tcpClientPort) {
        this.tcpClientPort = tcpClientPort;
        return this;
    }

    public Integer getThinClientPort() {
        return thinClientPort;
    }

    public CloudConfig setThinClientPort(Integer thinClientPort) {
        this.thinClientPort = thinClientPort;
        return this;
    }

    public boolean isPreferIPv4() {
        return preferIPv4;
    }

    public void setPreferIPv4(boolean preferIPv4) {
        this.preferIPv4 = preferIPv4;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public CloudConfig setNodeId(String nodeId) {
        this.nodeId = nodeId;
        return this;
    }

    public String getDataOltpNodes() {
        return dataOltpNodes;
    }

    public CloudConfig setDataOltpNodes(String dataOltpNodes) {
        this.dataOltpNodes = dataOltpNodes;
        return this;
    }

    public String getDataOlapNodes() {
        return dataOlapNodes;
    }

    public CloudConfig setDataOlapNodes(String dataOlapNodes) {
        this.dataOlapNodes = dataOlapNodes;
        return this;
    }

    public boolean isOlapNodeAsComputeNode() {
        return olapNodeAsComputeNode;
    }

    public CloudConfig setOlapNodeAsComputeNode(boolean olapNodeAsComputeNode) {
        this.olapNodeAsComputeNode = olapNodeAsComputeNode;
        return this;
    }

    public String getOlapClusterName() {
        return olapClusterName;
    }

    public CloudConfig setOlapClusterName(String olapClusterName) {
        this.olapClusterName = olapClusterName;
        return this;
    }

    public String getElasticSearchVersion() {
        return elasticSearchVersion;
    }

    public CloudConfig setElasticSearchVersion(String elasticSearchVersion) {
        this.elasticSearchVersion = elasticSearchVersion;
        return this;
    }

    public List<String> getElasticSearchDownloadUrls() {
        if(elasticSearchDownloadUrls == null) {
            elasticSearchDownloadUrls = List.of("https://mirrors.huaweicloud.com/elasticsearch/$version/elasticsearch-$version-$os-x86_64.tar.gz", "https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-$version-$os-x86_64.tar.gz");
        }
        return elasticSearchDownloadUrls;
    }

    public CloudConfig setElasticSearchDownloadUrls(List<String> elasticSearchDownloadUrls) {
        this.elasticSearchDownloadUrls = elasticSearchDownloadUrls;
        return this;
    }

    public CloudConfig setElasticSearchDownloadUrls(String elasticSearchDownloadUrl) {
        if(this.elasticSearchDownloadUrls == null) {
            this.elasticSearchDownloadUrls = new ArrayList<>();
        }
        this.elasticSearchDownloadUrls.add(elasticSearchDownloadUrl);
        return this;
    }

    public Map<String, Object> getElasticSearchSettings() {
        return elasticSearchSettings;
    }

    public CloudConfig setElasticSearchSettings(Map<String, Object> elasticSearchSettings) {
        this.elasticSearchSettings = elasticSearchSettings;
        return this;
    }

    public CloudConfig addElasticSearchSettings(String key, Object value) {
        if(this.elasticSearchSettings == null) {
            this.elasticSearchSettings = new HashMap<>();
        }
        this.elasticSearchSettings.put(key, value);
        return this;
    }

    public boolean isDataNodeAutoJoin() {
        return dataNodeAutoJoin;
    }

    public CloudConfig setDataNodeAutoJoin(boolean dataNodeAutoJoin) {
        this.dataNodeAutoJoin = dataNodeAutoJoin;
        return this;
    }

    public boolean isAutoRebalance() {
        return autoRebalance;
    }

    public CloudConfig setAutoRebalance(boolean autoRebalance) {
        this.autoRebalance = autoRebalance;
        return this;
    }

    public long getRebalanceTimeOut() {
        return rebalanceTimeOut;
    }

    public CloudConfig setRebalanceTimeOut(long rebalanceTimeOut) {
        this.rebalanceTimeOut = rebalanceTimeOut;
        return this;
    }

    public long getDefaultDataRegionInitialMemorySize() {
        return defaultDataRegionInitialMemorySize;
    }

    public CloudConfig setDefaultDataRegionInitialMemorySize(long defaultDataRegionInitialMemorySize) {
        this.defaultDataRegionInitialMemorySize = defaultDataRegionInitialMemorySize;
        return this;
    }

    public long getDefaultDataRegionMaxMemorySize() {
        return defaultDataRegionMaxMemorySize;
    }

    public CloudConfig setDefaultDataRegionMaxMemorySize(long defaultDataRegionMaxMemorySize) {
        this.defaultDataRegionMaxMemorySize = defaultDataRegionMaxMemorySize;
        return this;
    }

    public long getDataRegionWithoutPersistenceInitialMemorySize() {
        return dataRegionWithoutPersistenceInitialMemorySize;
    }

    public CloudConfig setDataRegionWithoutPersistenceInitialMemorySize(long dataRegionWithoutPersistenceInitialMemorySize) {
        this.dataRegionWithoutPersistenceInitialMemorySize = dataRegionWithoutPersistenceInitialMemorySize;
        return this;
    }

    public long getDataRegionWithoutPersistenceMaxMemorySize() {
        return dataRegionWithoutPersistenceMaxMemorySize;
    }

    public CloudConfig setDataRegionWithoutPersistenceMaxMemorySize(long dataRegionWithoutPersistenceMaxMemorySize) {
        this.dataRegionWithoutPersistenceMaxMemorySize = dataRegionWithoutPersistenceMaxMemorySize;
        return this;
    }

    public long getDataRegionWithPersistenceInitialMemorySize() {
        return dataRegionWithPersistenceInitialMemorySize;
    }

    public CloudConfig setDataRegionWithPersistenceInitialMemorySize(long dataRegionWithPersistenceInitialMemorySize) {
        this.dataRegionWithPersistenceInitialMemorySize = dataRegionWithPersistenceInitialMemorySize;
        return this;
    }

    public long getDataRegionWithPersistenceMaxMemorySize() {
        return dataRegionWithPersistenceMaxMemorySize;
    }

    public CloudConfig setDataRegionWithPersistenceMaxMemorySize(long dataRegionWithPersistenceMaxMemorySize) {
        this.dataRegionWithPersistenceMaxMemorySize = dataRegionWithPersistenceMaxMemorySize;
        return this;
    }

    public boolean isDiskPageCompression() {
        return diskPageCompression;
    }

    public CloudConfig setDiskPageCompression(boolean diskPageCompression) {
        this.diskPageCompression = diskPageCompression;
        return this;
    }

    public int getDiskPageCompressionLevel() {
        return diskPageCompressionLevel;
    }

    public CloudConfig setDiskPageCompressionLevel(int diskPageCompressionLevel) {
        this.diskPageCompressionLevel = diskPageCompressionLevel;
        return this;
    }

    public boolean isWalPageCompression() {
        return walPageCompression;
    }

    public CloudConfig setWalPageCompression(boolean walPageCompression) {
        this.walPageCompression = walPageCompression;
        return this;
    }

    public int getWalPageCompressionLevel() {
        return walPageCompressionLevel;
    }

    public CloudConfig setWalPageCompressionLevel(int walPageCompressionLevel) {
        this.walPageCompressionLevel = walPageCompressionLevel;
        return this;
    }

    public long getMaxWalArchiveSize() {
        return maxWalArchiveSize;
    }

    public CloudConfig setMaxWalArchiveSize(long maxWalArchiveSize) {
        this.maxWalArchiveSize = maxWalArchiveSize;
        return this;
    }

    public boolean isPeerClassLoading() {
        return peerClassLoading;
    }

    public CloudConfig setPeerClassLoading(boolean peerClassLoading) {
        this.peerClassLoading = peerClassLoading;
        return this;
    }

    public boolean isCacheCheckpoint() {
        return cacheCheckpoint;
    }

    public CloudConfig setCacheCheckpoint(boolean cacheCheckpoint) {
        this.cacheCheckpoint = cacheCheckpoint;
        return this;
    }

    public long getMetricsLogFrequency() {
        return metricsLogFrequency;
    }

    public CloudConfig setMetricsLogFrequency(long metricsLogFrequency) {
        this.metricsLogFrequency = metricsLogFrequency;
        return this;
    }

    public boolean isAllowH2DB() {
        return allowH2DB;
    }

    public CloudConfig setAllowH2DB(boolean allowH2DB) {
        this.allowH2DB = allowH2DB;
        return this;
    }

    public String getLoadBalancer() {
        return loadBalancer;
    }

    public void setLoadBalancer(String loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    public void addLoadBalancer(String name, Class<? extends LoadBalancer> loadBalancer) {
        this.supportedLoadBalancer.put(name, loadBalancer);
    }

    public Class<? extends LoadBalancer> getLoadBalancer(String name) {
        return this.supportedLoadBalancer.get(name);
    }

    public boolean isEnabledDistributedTransaction() {
        return enabledDistributedTransaction;
    }

    public void setEnabledDistributedTransaction(boolean enabledDistributedTransaction) {
        this.enabledDistributedTransaction = enabledDistributedTransaction;
    }

    public String getDbUser() {
        return dbUser;
    }

    public CloudConfig setDbUser(String dbUser) {
        this.dbUser = dbUser;
        return this;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public CloudConfig setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
        return this;
    }

    public TransactionConfig getTransaction() {
        return transaction;
    }

    public void setTransaction(TransactionConfig transaction) {
        this.transaction = transaction;
    }

    public ConfigClientConfig getConfigClient() {
        return configClient;
    }

    public void setConfigClient(ConfigClientConfig configClient) {
        this.configClient = configClient;
    }

    public RegistryConfig getRegistry() {
        return registry;
    }

    public void setRegistry(RegistryConfig registry) {
        this.registry = registry;
    }

    public ConfigServerConfig getConfigServer() {
        return configServer;
    }

    public void setConfigServer(ConfigServerConfig configServer) {
        this.configServer = configServer;
    }

    public ClientConfig getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(ClientConfig httpClient) {
        this.httpClient = httpClient;
    }

    public boolean isTrustAllService() {
        return trustAllService;
    }

    public void setTrustAllService(boolean trustAllService) {
        this.trustAllService = trustAllService;
    }

    @Override
    public void validate() {
        if(getLoadBalancer(loadBalancer) == null) {
            throw new RuntimeException("invalid loadBalancer '" + loadBalancer + "', supported loadBalancer: " + supportedLoadBalancer.keySet());
        }
        ServerConfig server = Ready.getBootstrapConfig().getServer();
        int minimalPort = 2; 
        if(server.isEnableHttp()) minimalPort++;
        if(server.isEnableHttps()) minimalPort++;
        if(server.isDynamicPort() ){
            if(server.getMaxPort() > 65534) throw new RuntimeException("the max port should < 65535");
            if(server.getMaxPort() < 1025) throw new RuntimeException("the max port should > 1024, due to discoveryPort and communicationPort must > 1023");
            int portRange = server.getMaxPort() - server.getMinPort() < 1024 ? 1024 : server.getMinPort();
            if(portRange < minimalPort) throw new RuntimeException("there is not enough dynamic ports for usage.");
        } else {
            if(getDiscoveryPort() != null && getDiscoveryPort() < 1024)
                throw new RuntimeException("the discoveryPort port should > 1023");
            if(getCommunicationPort() != null && getCommunicationPort() < 1024)
                throw new RuntimeException("the communicationPort port should > 1023");
            if(getDiscoveryPort() != null && getDiscoveryPort() > 65534)
                throw new RuntimeException("the discoveryPort port should < 65534");
            if(getCommunicationPort() != null && getCommunicationPort() > 65534)
                throw new RuntimeException("the communicationPort port should < 65534");
        }
    }

}
