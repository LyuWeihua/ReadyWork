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

import java.nio.file.Path;
import java.util.Objects;
import java.util.StringJoiner;

public final class DefaultDistribution implements Artifact.Distribution {

	private final Version version;

	private final Path directory;

	public DefaultDistribution(Version version, Path directory) {
		this.version = Objects.requireNonNull(version, "'version' must not be null");
		this.directory = Objects.requireNonNull(directory, "'directory' must not be null");
	}

	@Override
	public Path getDirectory() {
		return this.directory;
	}

	@Override
	public Version getVersion() {
		return this.version;
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", DefaultDistribution.class.getSimpleName() + "[", "]")
				.add("version=" + this.version)
				.add("directory=" + this.directory)
				.toString();
	}

}
