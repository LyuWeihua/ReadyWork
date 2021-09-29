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
package work.ready.cloud.cluster.elasticsearch;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import work.ready.cloud.cluster.elasticsearch.artifact.*;
import work.ready.core.server.Constant;
import work.ready.core.tools.define.io.Resource;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.module.ShutdownHook;
import work.ready.core.server.Ready;
import work.ready.core.tools.FileUtil;
import work.ready.core.tools.StrUtil;

public final class ReadyElasticSearchFactory implements ElasticSearchFactory {

	private static final AtomicLong NUMBER = new AtomicLong();

	private final Map<String, Object> environmentVariables = new LinkedHashMap<>();

	private final List<String> jvmOptions = new ArrayList<>();

	private final Map<String, Object> systemProperties = new LinkedHashMap<>();

	private final Map<String, Object> configProperties = new LinkedHashMap<>();

	private boolean rootAllowed = false;

	private boolean daemon = true;

	private boolean registerShutdownHook = true;

	private Log logger;

	private Duration timeout;

	private boolean useInternalJvm = true; 

	private Path javaHome;

	private String name;

	private String clusterName = "READY_OLAP_CLUSTER";

	private String defaultVersion = "7.10.0"; 

	private List<String> downloadUrls;

	private Artifact artifact;

	private Resource config;

	private Path workingDirectory;

	private Integer port; 

	private Integer tcpPort;  

	private String address;

	private String bindAddress;

	private String publishAddress;

	private List<String> seedHosts;

	private List<String> initialMasterNodes;

	public String getName() {
		return this.name;
	}

	public ReadyElasticSearchFactory setName(String name) {
		this.name = name;
		return this;
	}

	public String getClusterName() {
		return clusterName;
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	public Path getWorkingDirectory() {
		return this.workingDirectory;
	}

	public ReadyElasticSearchFactory setWorkingDirectory(Path workingDirectory) {
		this.workingDirectory = workingDirectory;
		return this;
	}

	public String getDefaultVersion() {
		return defaultVersion;
	}

	public ReadyElasticSearchFactory setDefaultVersion(String defaultVersion) {
		this.defaultVersion = defaultVersion;
		return this;
	}

	public List<String> getDownloadUrls() {
		return downloadUrls;
	}

	public ReadyElasticSearchFactory setDownloadUrls(List<String> downloadUrls) {
		this.downloadUrls = downloadUrls;
		return this;
	}

	public Artifact getArtifact() {
		return this.artifact;
	}

	public ReadyElasticSearchFactory setArtifact(Artifact artifact) {
		this.artifact = artifact;
		return this;
	}

	public Path getJavaHome() {
		return this.javaHome;
	}

	public ReadyElasticSearchFactory setJavaHome(Path javaHome) {
		this.javaHome = javaHome;
		this.useInternalJvm = false;
		return this;
	}

	public Log getLogger() {
		return this.logger;
	}

	public ReadyElasticSearchFactory setLogger(Log logger) {
		this.logger = logger;
		return this;
	}

	public boolean isDaemon() {
		return this.daemon;
	}

	public ReadyElasticSearchFactory setDaemon(boolean daemon) {
		this.daemon = daemon;
		return this;
	}

	public boolean isRootAllowed() {
		return this.rootAllowed;
	}

	public ReadyElasticSearchFactory setRootAllowed(boolean rootAllowed) {
		this.rootAllowed = rootAllowed;
		return this;
	}

	public List<String> getJvmOptions() {
		return this.jvmOptions;
	}

	public Map<String, Object> getSystemProperties() {
		return this.systemProperties;
	}

	public Map<String, Object> getEnvironmentVariables() {
		return this.environmentVariables;
	}

	public Map<String, Object> getConfigProperties() {
		return this.configProperties;
	}

	public ReadyElasticSearchFactory setConfigProperty(String name, Object value) {
		Objects.requireNonNull(name, "'name' must not be null");
		this.configProperties.put(name, value);
		return this;
	}

	public ReadyElasticSearchFactory setConfigProperties(Map<String, Object> configProperties) {
		Objects.requireNonNull(configProperties, "'configProperties' must not be null");
		this.configProperties.putAll(configProperties);
		return this;
	}

	public ReadyElasticSearchFactory setSystemProperty(String name, Object value) {
		Objects.requireNonNull(name, "'name' must not be null");
		Objects.requireNonNull(value, "'value' must not be null");
		this.systemProperties.put(name, value);
		return this;
	}

	public ReadyElasticSearchFactory setSystemProperties(Map<String, Object> systemProperties) {
		Objects.requireNonNull(systemProperties, "'systemProperties' must not be null");
		this.systemProperties.putAll(systemProperties);
		return this;
	}

	public ReadyElasticSearchFactory setEnvironmentVariable(String name, Object value) {
		Objects.requireNonNull(name, "'name' must not be null");
		Objects.requireNonNull(value, "'value' must not be null");
		this.environmentVariables.put(name, value);
		return this;
	}

	public ReadyElasticSearchFactory setEnvironmentVariables(Map<String, Object> environmentVariables) {
		Objects.requireNonNull(environmentVariables, "'environmentVariables' must not be null");
		this.environmentVariables.putAll(environmentVariables);
		return this;
	}

	public ReadyElasticSearchFactory setJvmOptions(String... options) {
		Objects.requireNonNull(options, "'options' must not be null");
		this.jvmOptions.addAll(Arrays.asList(options));
		return this;
	}

	public Duration getTimeout() {
		return this.timeout;
	}

	public ReadyElasticSearchFactory setTimeout(Duration timeout) {
		this.timeout = timeout;
		return this;
	}

	public Integer getPort() {
		return this.port;
	}

	public ReadyElasticSearchFactory setPort(Integer port) {
		this.port = port;
		return this;
	}

	public Integer getTcpPort() {
		return this.tcpPort;
	}

	public ReadyElasticSearchFactory setTcpPort(Integer port) {
		this.tcpPort = port;
		return this;
	}

	public String getAddress() {
		return this.address;
	}

	public ReadyElasticSearchFactory setAddress(String address) {
		this.address = address;
		return this;
	}

	public String getBindAddress() {
		return bindAddress;
	}

	public void setBindAddress(String bindAddress) {
		this.bindAddress = bindAddress;
	}

	public String getPublishAddress() {
		return publishAddress;
	}

	public void setPublishAddress(String publishAddress) {
		this.publishAddress = publishAddress;
	}

	public List<String> getSeedHosts() {
		return seedHosts;
	}

	public void setSeedHosts(List<String> seedHosts) {
		this.seedHosts = seedHosts;
	}

	public void setSeedHosts(String seedHost) {
		if(this.seedHosts == null) this.seedHosts = new ArrayList<>();
		this.seedHosts.add(seedHost);
	}

	public List<String> getInitialMasterNodes() {
		return initialMasterNodes;
	}

	public void setInitialMasterNodes(List<String> initialMasterNodes) {
		this.initialMasterNodes = initialMasterNodes;
	}

	public void setInitialMasterNodes(String nodeName) {
		if(this.initialMasterNodes == null) this.initialMasterNodes = new ArrayList<>();
		this.initialMasterNodes.add(nodeName);
	}

	public Resource getConfig() {
		return this.config;
	}

	public ReadyElasticSearchFactory setConfig(Resource config) {
		this.config = config;
		return this;
	}

	public boolean isRegisterShutdownHook() {
		return this.registerShutdownHook;
	}

	public ReadyElasticSearchFactory setRegisterShutdownHook(boolean registerShutdownHook) {
		this.registerShutdownHook = registerShutdownHook;
		return this;
	}

	@Override
	public ElasticSearch create() {
		try {
			return doCreate();
		}
		catch (Exception ex) {
			throw new RuntimeException("ElasticSearch instance cannot be created", ex);
		}
	}

	private ElasticSearch doCreate() throws Exception {
		String name = getName();
		if (StrUtil.isBlank(name)) {
			name = "ES-" + NUMBER.incrementAndGet();
			setName(name);
		}
		Artifact artifact = getArtifact();
		if (artifact == null) {
			if(downloadUrls == null || downloadUrls.isEmpty()) {
				throw new RuntimeException("ElasticSearch Artifact didn't provide and download urls is empty");
			}
			artifact = new RemoteArtifact(Version.of(getOsType(), defaultVersion), (ver)->{
				List<URL> urlList = new ArrayList<>();
				for(String url : downloadUrls) {
					url = StrUtil.replace(url,"$version", ver.toString());
					url = StrUtil.replace(url,"$os", ver.getOs());
					urlList.add(new URL(url));
				}
				return urlList;
			});
		}
		Artifact.Distribution distribution = artifact.getDistribution();
		Version version = distribution.getVersion();
		Path workingDirectory = getWorkingDirectory();
		if (workingDirectory == null) {
			workingDirectory = Files.createTempDirectory("ES-" + version + "-");
			FileUtil.createIfNotExists(workingDirectory.resolve(".temp"));
		}
		if (Files.exists(workingDirectory) && !Files.isDirectory(workingDirectory)) {
			throw new IllegalArgumentException(workingDirectory + " is not a directory");
		}
		Path directory = distribution.getDirectory();
		if (!Files.exists(directory)) {
			throw new IllegalStateException(directory + " does not exist");
		}
		if (!Files.isDirectory(directory)) {
			throw new IllegalStateException(directory + " is not a directory");
		}
		Log logger = getLogger();
		if (logger == null) {
			logger = LogFactory.getLog(ElasticSearch.class);
		}
		Duration timeout = getTimeout();
		if (timeout == null || timeout.toMillis() <= 0) {
			timeout = Duration.ofSeconds(60);
		}
		ElasticSearchNode node = createNode(version, workingDirectory);
		ElasticSearchInstance instance = new ReadyElasticSearchInstance(name, getClusterName(), version, directory, workingDirectory,
				isDaemon(), logger, timeout, getConfig(), node);
		ReadyElasticSearch elasticSearch = new ReadyElasticSearch(name, version, instance);
		if (isRegisterShutdownHook()) {
			Ready.shutdownHook.add(ShutdownHook.STAGE_7, time -> elasticSearch.stop()); 
		}
		return elasticSearch;
	}

	private ElasticSearchNode createNode(Version version, Path workingDirectory) {
		Map<String, Object> systemProperties = new LinkedHashMap<>(getSystemProperties());
		systemProperties.keySet().removeIf(Objects::isNull);
		Map<String, Object> environmentVariables = new LinkedHashMap<>(getEnvironmentVariables());
		environmentVariables.keySet().removeIf(Objects::isNull);
		List<String> jvmOptions = new ArrayList<>(getJvmOptions());
		jvmOptions.removeIf(Objects::isNull);
		LinkedHashMap<String, Object> configProperties = new LinkedHashMap<>(getConfigProperties());
		configProperties.keySet().removeIf(Objects::isNull);
		if(!useInternalJvm) {
			Path javaHome = Optional.ofNullable(getJavaHome())
					.orElseGet(() -> Optional.ofNullable(System.getProperty("java.home")).map(Paths::get).orElse(null));
			if (javaHome != null) {
				environmentVariables.put("JAVA_HOME", javaHome);
			}
		}
		if (isRootAllowed()) {
			systemProperties.put("es.insecure.allow.root", true);
		}
		if(getName() != null) {
			configProperties.put("node.name", getName());
		}
		if (getClusterName() != null) {
			configProperties.put("cluster.name", getClusterName());
		}
		if (getAddress() != null) {
			configProperties.put("network.host", getAddress());
		}
		if (getBindAddress() != null) {
			configProperties.put("network.bind_host", getBindAddress());
		}
		if (getPublishAddress() != null) {
			configProperties.put("network.publish_host", getPublishAddress());
		}
		if (getPort() != null) {
			configProperties.put("http.port", getPort());
		}
		if (getTcpPort() != null) {
			configProperties.put("transport.port", getTcpPort());
		}
		if (getSeedHosts() != null && getSeedHosts().size() > 0) {
			configProperties.put("discovery.seed_hosts", getSeedHosts());
		}
		if (getInitialMasterNodes() != null && getInitialMasterNodes().size() > 0) {
			configProperties.put("cluster.initial_master_nodes", getInitialMasterNodes());
		}
		if (Constant.WINDOWS) {
			return new WindowsElasticSearchNode(version, workingDirectory, jvmOptions, systemProperties,
					environmentVariables, configProperties);
		} else if(Constant.MAC_OS_X) {
			return new MacElasticSearchNode(version, workingDirectory, jvmOptions, systemProperties, environmentVariables,
					configProperties);
		}
		return new LinuxElasticSearchNode(version, workingDirectory, jvmOptions, systemProperties, environmentVariables,
				configProperties);
	}

	public static String getOsType() {
		String osType;
		if(Constant.MAC_OS_X) {
			osType = "darwin";
		} else if(Constant.WINDOWS) {
			osType = "windows";
		} else {
			osType = "linux";
		}
		return osType;
	}

}
