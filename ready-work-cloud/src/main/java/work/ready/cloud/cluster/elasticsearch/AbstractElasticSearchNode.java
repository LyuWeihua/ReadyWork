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

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.yaml.snakeyaml.Yaml;
import work.ready.cloud.cluster.Cloud;
import work.ready.core.tools.define.io.FileSystemResource;
import work.ready.core.tools.define.io.Resource;
import work.ready.core.tools.define.io.UrlResource;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;

abstract class AbstractElasticSearchNode implements ElasticSearchNode {

	protected final Log logger = LogFactory.getLog(getClass());

	private static final String JVM_EXTRA_OPTS = "JVM_EXTRA_OPTS";

	private static final ByteArrayInputStream EMPTY_STREAM = new ByteArrayInputStream(new byte[0]);

	static final String[] macFilesNeedExecutePermission = new String[]{
			"bin",
			"jdk.app/Contents/Home/bin",
            "jdk.app/Contents/Home/lib/jspawnhelper",
			"modules/x-pack-ml/platform/darwin-x86_64/controller.app/Contents/MacOS"
	};
	static final String[] linuxFilesNeedExecutePermission = new String[]{
			"bin",
			"jdk/bin",
            "jdk/lib/jspawnhelper",
			"modules/x-pack-ml/platform/linux-x86_64/bin"
	};

	private final Path workingDirectory;

	private final Map<String, Object> properties;

	private final Map<String, Object> systemProperties;

	private final Map<String, Object> environmentVariables;

	private final List<String> jvmOptions;

	private volatile Process process;

	private volatile long pid = -1;

	private volatile boolean sslEnabled;

	AbstractElasticSearchNode(Path workingDirectory, Map<String, Object> properties, List<String> jvmOptions,
							  Map<String, Object> systemProperties, Map<String, Object> environmentVariables) {
		this.workingDirectory = workingDirectory;
		this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
		this.jvmOptions = Collections.unmodifiableList(new ArrayList<>(jvmOptions));
		this.systemProperties = Collections.unmodifiableMap(new LinkedHashMap<>(systemProperties));
		this.environmentVariables = Collections.unmodifiableMap(new LinkedHashMap<>(environmentVariables));
	}

	@Override
	public final void start() throws IOException, InterruptedException {
		RunProcess runProcess = new RunProcess(this.workingDirectory);
		Map<String, Object> properties = loadProperties();
		properties.putAll(this.properties);
		Map<String, Object> systemProperties = new LinkedHashMap<>(this.systemProperties);
		Map<String, Object> environmentVariables = new LinkedHashMap<>(this.environmentVariables);
		configureSystemProperties(systemProperties);
		configureProperties(properties);
		Path configPath = Ready.root().resolve("elasticsearch").resolve(Cloud.getConsistentId()).resolve("config").toAbsolutePath();
		if(properties.size() > 0) {
			Path currentConfig = configPath.resolve("elasticsearch.yml");
			Files.deleteIfExists(currentConfig);
			Path configFile = Files.createFile(currentConfig);
			dumpProperties(properties, configFile);
		}
		environmentVariables.put("ES_PATH_CONF", configPath.toString());
		
		Path tmpPath = this.workingDirectory.resolve("temp");

		Files.createDirectories(tmpPath);
		tmpPath.toFile().setExecutable(true, false);
		tmpPath.toFile().setReadable(true, false);
		tmpPath.toFile().setWritable(true, false);
		environmentVariables.put("ES_TMPDIR", tmpPath);

		List<String> jvmOptions = new ArrayList<>(this.jvmOptions);
		for (Map.Entry<String, Object> entry : systemProperties.entrySet()) {
			Object value = entry.getValue();
			String name = entry.getKey();
			jvmOptions.add((value != null) ? String.format("-D%s=%s", name, value) : String.format("-D%s", name));
		}
		runProcess.getEnvironment().putAll(environmentVariables);
		runProcess.putEnvironment(JVM_EXTRA_OPTS, String.join(" ", jvmOptions));
		Process process = doStart(runProcess);
		this.process = process;
		this.pid = getPid(process);
		this.sslEnabled = isSslEnabled(properties) || isSslEnabled(systemProperties);
	}

	@SuppressWarnings("unchecked")
	private boolean isSslEnabled(Map<String, Object> properties) {
		if(properties.get("xpack.security.enabled") != null) {
			return Boolean.parseBoolean(Objects.toString(properties.get("xpack.security.enabled"), null));
		}
		
		return false;
	}

	@Override
	public final void stop() throws IOException, InterruptedException {
		Process process = this.process;
		if (process != null && process.isAlive()) {
			doStop(process, this.pid);
			if (!process.waitFor(3, TimeUnit.SECONDS)) {
				this.logger.warn("java.lang.Process.destroyForcibly() has been called for '%s'. The behavior of this "
						+ "method is undefined, hence Elasticsearch's node could be still alive", toString());
				if (!process.destroyForcibly().waitFor(1, TimeUnit.SECONDS)) {
					throw new IOException(String.format("'%s' is still alive.", toString()));
				}
			}
		}
	}

	@Override
	public final InputStream getInputStream() {
		Process process = this.process;
		return process != null ? process.getInputStream() : EMPTY_STREAM;
	}

	@Override
	public boolean isSslEnabled() {
		return this.sslEnabled;
	}

	@Override
	public final boolean isAlive() {
		Process process = this.process;
		return process != null && process.isAlive();
	}

	@Override
	public final String toString() {
		return String.format("%s[pid='%s', exitValue='%s']", getClass().getSimpleName(), this.pid, exitValue());
	}

	long getPid(Process process) throws IOException, InterruptedException {
		return process.pid();
	}

	abstract Process doStart(RunProcess runProcess) throws IOException, InterruptedException;

	abstract void doStop(Process process, long pid) throws IOException, InterruptedException;

	private String exitValue() {
		Process process = this.process;
		if (process == null) {
			return "does not exist";
		}
		return isAlive() ? "not exited" : String.valueOf(process.exitValue());
	}

	private Map<String, Object> loadProperties() throws IOException {
		try (InputStream is = getConfig().getInputStream()) {
			Yaml yaml = new Yaml();
			Map<String, Object> properties = yaml.load(is);
			return (properties != null) ? new LinkedHashMap<>(properties) : new LinkedHashMap<>(0);
		}
	}

	private Resource getConfig() throws IOException {
		Object url = this.environmentVariables.get("ES_PATH_CONF");
		if (url != null) {
			return new UrlResource(new URL(url.toString() + "/elasticsearch.yml"));
		}
		Path configPath = Ready.root().resolve("elasticsearch").resolve(Cloud.getConsistentId()).resolve("config").toAbsolutePath();
		return new FileSystemResource(configPath.resolve("elasticsearch.yml"));
	}

	private void dumpProperties(Map<String, Object> properties, Path file) throws IOException {
		try (BufferedWriter bw = Files.newBufferedWriter(file)) {
			Yaml yaml = new Yaml();
			yaml.dump(properties, bw);
		}
	}

	private void configureProperties(Map<String, Object> properties) throws IOException {
		configurePort(properties, "http.port");
		configurePort(properties, "transport.port");
	}

	private void configureSystemProperties(Map<String, Object> systemProperties) throws IOException {
		configurePort(systemProperties, "http.port");
		configurePort(systemProperties, "transport.port");
	}

	private void configurePort(Map<String, Object> properties, String name) throws IOException {
		if (!Objects.toString(properties.get(name), "").trim().equals("0")) {
			return;
		}
		try (ServerSocket ss = new ServerSocket(0)) {
			properties.put(name, ss.getLocalPort());
		}
	}
}
