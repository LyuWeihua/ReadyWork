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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

import work.ready.cloud.ReadyCloud;
import work.ready.cloud.cluster.Cloud;
import work.ready.core.server.Constant;
import work.ready.core.tools.define.CachedConsumer;
import work.ready.core.tools.define.CompositeConsumer;
import work.ready.core.tools.define.Kv;
import work.ready.core.tools.define.io.Resource;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;
import work.ready.core.tools.FileUtil;
import work.ready.core.tools.StrUtil;

class ReadyElasticSearchInstance implements ElasticSearchInstance {

	private static final Log log = LogFactory.getLog(ReadyElasticSearchInstance.class);

	private final String name;

	private final String clusterName;

	private final Version version;

	private final Path directory;

	private final Path workingDirectory;

	private final boolean daemon;

	private final Log logger;

	private final Duration timeout;

	private final ElasticSearchNode node;

	private final Resource config;

	private volatile InetAddress address;

	private volatile int port = -1;

	private volatile int sslPort = -1;

	private volatile int tcpPort = -1;

	ReadyElasticSearchInstance(String name, String clusterName, Version version, Path directory, Path workingDirectory, boolean daemon,
							   Log logger, Duration timeout, Resource config, ElasticSearchNode node) {
		this.name = name;
		this.clusterName = clusterName;
		this.version = version;
		this.directory = directory;
		this.workingDirectory = workingDirectory;
		this.daemon = daemon;
		this.logger = logger;
		this.timeout = timeout;
		this.config = config;
		this.node = node;
	}

	@Override
	public void start() throws InterruptedException, IOException {
		initialize();
		this.node.start();
		log.info("%s has been started", toString());
		TcpTransportStateWatcherConsumer tcpTransportWatcher = new TcpTransportStateWatcherConsumer();
		HttpTransportStateWatcherConsumer httpTransportWatcher = new HttpTransportStateWatcherConsumer(this.node.isSslEnabled());
		await(tcpTransportWatcher, httpTransportWatcher);
		int sslPort = httpTransportWatcher.getSslPort();
		int port = httpTransportWatcher.getPort();
		this.port = (port != -1) ? port : sslPort;
		this.sslPort = sslPort;
		this.tcpPort = tcpTransportWatcher.getPort();
		InetAddress address = tcpTransportWatcher.getAddress();
		this.address = (address != null) ? address : tcpTransportWatcher.getAddress();
		Cloud.getRegistry().register(Cloud.NodeType.APPLICATION_WITH_OLAP.getType(),
				Cloud.OLAP_SERVICE_ID, version.toString(),
				(port != -1) ? Constant.PROTOCOL_HTTP : Constant.PROTOCOL_HTTPS,
				this.port, Kv.by("clusterName", this.clusterName).set("tcpPort", String.valueOf(this.tcpPort)).set("address", this.address.getHostAddress()));
	}

	@Override
	public void stop() throws InterruptedException, IOException {
		if (this.node.isAlive()) {
			this.node.stop();
			log.info("%s has been stopped", toString());
		}
		try {
			if (Files.exists(this.workingDirectory.resolve(".temp"))) {
				FileUtil.delete(this.workingDirectory);
			}
		}
		catch (IOException ex) {
			log.error(ex, "Working Directory '" + this.workingDirectory + "' has not been deleted");
		}
	}

	@Override
	public InetAddress getAddress() {
		return this.address;
	}

	@Override
	public int getPort() {
		return this.port;
	}

	@Override
	public int getSslPort() {
		return this.sslPort;
	}

	@Override
	public int getTcpPort() {
		return this.tcpPort;
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", ReadyElasticSearchInstance.class.getSimpleName() + "[", "]")
				.add("name='" + this.name + "'").add("version='" + this.version + "'").add("node=" + this.node)
				.toString();
	}

	private void initialize() throws IOException {
		Files.createDirectories(this.workingDirectory);
		if(!Files.exists(this.workingDirectory.resolve(".exist"))) {
			FileUtil.copy(this.directory, this.workingDirectory, (path, attributes) -> {
				if (attributes.isDirectory()) {
					String name = path.getFileName().toString().toLowerCase(Locale.ENGLISH);
					return !name.equals("config");
				}
				return true;
			});
			FileUtil.createIfNotExists(this.workingDirectory.resolve(".exist"));
		}
		Path configPath = Ready.root().resolve("elasticsearch").resolve(Cloud.getConsistentId()).resolve("config").toAbsolutePath();
		FileUtil.copy(this.directory.resolve("config"), configPath, null);
		if (this.config != null) {
			try (InputStream is = this.config.getInputStream()) {
				Files.copy(is, configPath.resolve("elasticsearch.yml"),
						StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}

	private void await(StateWatcherConsumer... watcherConsumers) throws IOException, InterruptedException {
		CompositeConsumer<String> compositeConsumer = new CompositeConsumer<>();
		CachedConsumer<String> cachedConsumer = new CachedConsumer<>(30);
		compositeConsumer.add(this.logger::info);
		compositeConsumer.add(cachedConsumer);
		for (StateWatcherConsumer watcherConsumer : watcherConsumers) {
			compositeConsumer.add(watcherConsumer);
		}
		Thread thread = new Thread(() -> {
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(this.node.getInputStream(), StandardCharsets.UTF_8))) {
				try {
					reader.lines().map(line->line.replace("%", "%%")).forEach(compositeConsumer);
				}
				catch (UncheckedIOException ex) {
					if (!ex.getMessage().contains("Stream closed")) {
						throw ex;
					}
				}
			}
			catch (IOException ex) {
				throw new UncheckedIOException("Stream cannot be closed", ex);
			}
		});
		thread.setName(this.name);
		thread.setDaemon(this.daemon);
		thread.setUncaughtExceptionHandler((t, ex) -> log.error("Exception in thread " + t, ex));
		thread.start();
		long start = System.nanoTime();
		long rem = this.timeout.toNanos();
		while (rem > 0 && this.node.isAlive() && !isReady(watcherConsumers)) {
			Thread.sleep(Math.min(TimeUnit.NANOSECONDS.toMillis(rem) + 1, 100));
			rem = this.timeout.toNanos() - (System.nanoTime() - start);
		}
		if (!this.node.isAlive()) {
			thread.join(100);
			List<String> lines = new ArrayList<>(cachedConsumer.get());
			Collections.reverse(lines);
			throw new IOException(String.format("'%s' is not alive. Please see logs for more details%n\t%s", this.node,
					String.join(String.format("%n\t"), lines)));
		}
		if (rem <= 0) {
			throw new IllegalStateException(
					toString() + " couldn't be started within " + this.timeout.toMillis() + "ms");
		}
		for (StateWatcherConsumer watcherConsumer : watcherConsumers) {
			compositeConsumer.remove(watcherConsumer);
		}
		compositeConsumer.remove(cachedConsumer);
	}

	private static boolean isReady(StateWatcher... stateWatchers) {
		for (StateWatcher stateWatcher : stateWatchers) {
			if (!stateWatcher.isReady()) {
				return false;
			}
		}
		return true;
	}

}
