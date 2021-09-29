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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import work.ready.cloud.cluster.elasticsearch.Version;
import work.ready.core.tools.FileLock;
import work.ready.core.tools.define.io.ArchiveResource;
import work.ready.core.tools.define.io.Resource;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;
import work.ready.core.tools.FileUtil;

public final class ArchiveArtifact implements Artifact {

	private static final Log logger = LogFactory.getLog(ArchiveArtifact.class);

	private final Version version;

	private final Resource archiveResource;

	private Path destination;

	public ArchiveArtifact(Version version, Resource archiveResource) {
		this.version = Objects.requireNonNull(version, "'version' must not be null");
		this.archiveResource = Objects.requireNonNull(archiveResource, "'archiveResource' must not be null");
	}

	public ArchiveArtifact(Version version, Resource archiveResource, Path destination) {
		this.version = version;
		this.archiveResource = archiveResource;
		this.destination = destination;
	}

	public Version getVersion() {
		return this.version;
	}

	public Resource getArchiveResource() {
		return this.archiveResource;
	}

	public Path getDestination() {
		return this.destination;
	}

	public void setDestination(Path destination) {
		this.destination = destination;
	}

	@Override
	public Distribution getDistribution() throws IOException {
		Path destination = getRealDestination();

		Artifact artifact = new DefaultArtifact(this.version, destination);

		if (!Files.exists(destination.resolve(".exist"))) {
			Files.createDirectories(destination);
			Path lockFile = destination.resolve(".lock");
			try (FileLock fileLock = FileLock.of(lockFile)) {
				if (!fileLock.tryLock(30, TimeUnit.SECONDS)) {
					throw new IllegalStateException("File lock cannot be acquired for a file '" + lockFile + "'");
				}
				if (!Files.exists(destination.resolve(".exist"))) {
					logger.info("Extracts '%s' into '%s' directory", this.archiveResource, destination);
					ArchiveResource archiveResource = createArchiveResource();
					archiveResource.extract(destination);
					Distribution distribution = artifact.getDistribution();
					FileUtil.createIfNotExists(destination.resolve(".exist"));
					return distribution;
				}
			}
		}

		return artifact.getDistribution();
	}

	private ArchiveResource createArchiveResource() {
		if (this.archiveResource instanceof ArchiveResource) {
			return ((ArchiveResource) this.archiveResource);
		}
		return new ArchiveResource(this.archiveResource);
	}

	private Path getRealDestination() {
		Path destination = this.destination;
		if (destination == null) {
			destination = Ready.root().resolve("elasticsearch").toAbsolutePath();
		}
		return destination.resolve("artifact/" + this.version);
	}

}
