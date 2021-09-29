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
package work.ready.cloud.cluster.elasticsearch.artifact;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import work.ready.cloud.cluster.elasticsearch.Version;
import work.ready.core.tools.FileDownloader;
import work.ready.core.tools.FileLock;
import work.ready.core.tools.define.io.ArchiveResource;
import work.ready.core.tools.define.io.Resource;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;
import work.ready.core.tools.FileUtil;

public final class RemoteArtifact implements Artifact {

	private static final Log logger = LogFactory.getLog(RemoteArtifact.class);

	private final Version version;

	private Duration readTimeout;

	private Duration connectTimeout;

	private Proxy proxy;

	private UrlProvider urlProvider;

	private Path destination;

	public RemoteArtifact(Version version, UrlProvider urlProvider) {
		this(version, urlProvider, null, null, null, null);
	}

	public RemoteArtifact(Version version, UrlProvider urlProvider, Path destination, Duration readTimeout,
						  Duration connectTimeout, Proxy proxy) {
		this.version = Objects.requireNonNull(version, "'version' must not be null");
		this.readTimeout = (readTimeout != null) ? readTimeout : Duration.ofSeconds(30);
		this.connectTimeout = (connectTimeout != null) ? connectTimeout : Duration.ofSeconds(10);
		this.proxy = proxy;
		this.urlProvider = urlProvider;
		this.destination = destination;
	}

	public Version getVersion() {
		return this.version;
	}

	public Proxy getProxy() {
		return this.proxy;
	}

	public void setProxy(Proxy proxy) {
		this.proxy = Objects.requireNonNull(proxy, "'proxy' must not be null");
	}

	public UrlProvider getUrlProvider() {
		return this.urlProvider;
	}

	public void setUrlProvider(UrlProvider urlProvider) {
		this.urlProvider = Objects.requireNonNull(urlProvider, "'urlFactory' must not be null");
	}

	public Path getDestination() {
		return this.destination;
	}

	public void setDestination(Path destination) {
		this.destination = destination;
	}

	public Duration getReadTimeout() {
		return this.readTimeout;
	}

	public void setReadTimeout(Duration readTimeout) {
		this.readTimeout = Objects.requireNonNull(readTimeout, "'readTimeout' must not be null");
	}

	public Duration getConnectTimeout() {
		return this.connectTimeout;
	}

	public void setConnectTimeout(Duration connectTimeout) {
		this.connectTimeout = Objects.requireNonNull(connectTimeout, "'connectTimeout' must not be null");
	}

	@Override
	public Distribution getDistribution() throws IOException {
		Path destination = getRealDestination();
		Artifact artifact = new DefaultArtifact(this.version, destination);
		if (!Files.exists(destination.resolve(".exist"))) {
			Files.createDirectories(destination);
			Path lockFile = destination.resolve(".lock");
			try (FileLock fileLock = FileLock.of(lockFile)) {
				logger.info("Acquires a lock to the file '%s' ...", lockFile);
				if (!fileLock.tryLock(2, TimeUnit.MINUTES)) {
					throw new IllegalStateException("File lock cannot be acquired for a file '" + lockFile + "'");
				}
				logger.info("The lock to the file '%s' was acquired", lockFile);
				if (!Files.exists(destination.resolve(".exist"))) {
					Resource resource = download();
					logger.info("Extracts '%s' into '%s' directory", resource, destination);
					ArchiveResource archiveResource = new ArchiveResource(resource);
					archiveResource.extract(destination);
					Distribution distribution = artifact.getDistribution();
					FileUtil.createIfNotExists(destination.resolve(".exist"));
					return distribution;
				}
			}
		}
		return artifact.getDistribution();
	}

	private Path getRealDestination() {
		Path destination = this.destination;
		if (destination == null) {
			destination = Ready.root().resolve("elasticsearch").toAbsolutePath();
		}
		return destination.resolve("artifact/" + this.version);
	}

	private Resource download() throws IOException {
		List<Exception> exceptions = new ArrayList<>();
		List<URL> urls = this.urlProvider.getUrl(this.version);
		FileDownloader downloader = new FileDownloader(this.readTimeout, this.connectTimeout, this.proxy);
		for (URL url : urls) {
			try {
				return downloader.download(url, "ElasticSearch " + this.version);
			}
			catch (ClosedByInterruptException ex) {
				throw ex;
			}
			catch (Exception ex) {
				exceptions.add(ex);
			}
		}
		IOException ex = new IOException("ElasticSearch cannot be downloaded from " + urls);
		exceptions.forEach(ex::addSuppressed);
		throw ex;
	}

}
