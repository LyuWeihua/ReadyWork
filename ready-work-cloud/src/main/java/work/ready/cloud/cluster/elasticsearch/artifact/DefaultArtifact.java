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

import work.ready.cloud.cluster.elasticsearch.Version;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class DefaultArtifact implements Artifact {

	private final Version version;

	private final Path directory;

	public DefaultArtifact(Version version, Path directory) {
		this.version = Objects.requireNonNull(version, "'version' must not be null");
		this.directory = Objects.requireNonNull(directory, "'directory' must not be null");
	}

	public Version getVersion() {
		return this.version;
	}

	public Path getDirectory() {
		return this.directory;
	}

	@Override
	public Distribution getDistribution() throws IOException {
		return new DefaultDistribution(this.version, findElasticSearchHome(this.directory));
	}

	private static Path findElasticSearchHome(Path directory) throws IOException {
		try (Stream<Path> stream = Files.find(directory, 1, DefaultArtifact::isElasticSearchHome)) {
			Set<Path> directories = stream.collect(Collectors.toSet());
			if (directories.isEmpty()) {
				throw new IllegalStateException(
						String.format("'%s' does not have the ElasticSearch files", directory));
			}
			if (directories.size() > 1) {
				throw new IllegalStateException(String.format(
						"Impossible to determine the ElasticSearch directory. There are '%s' candidates  '%s'",
						directories.size(), directories));
			}
			return directories.iterator().next();
		}
	}

	private static boolean isElasticSearchHome(Path path, BasicFileAttributes attributes) {
		if (attributes.isDirectory()) {
			return Files.isDirectory(path.resolve("bin")) && Files.isDirectory(path.resolve("lib")) && Files
					.isDirectory(path.resolve("config")) && Files.isRegularFile(
					path.resolve("config").resolve("elasticsearch.yml"));
		}
		return false;
	}

}
