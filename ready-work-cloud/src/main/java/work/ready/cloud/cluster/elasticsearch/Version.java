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

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Version implements Comparable<Version> {

	private static final Pattern VERSION_PATTERN = Pattern.compile("^([0-9]+)(\\.([0-9]+))(\\.([0-9]+))?.*$");

	private final String os;

	private final String version;

	private final int major;

	private final int minor;

	private final Integer patch;

	private Version(int major, int minor, Integer patch, String os, String version) {
		this.major = major;
		this.minor = minor;
		this.patch = patch;
		this.os = os;
		this.version = version;
	}

	public static Version of(String os, String version) throws IllegalArgumentException, NullPointerException {
		Objects.requireNonNull(version, "'version' must not be null");
		Matcher matcher = VERSION_PATTERN.matcher(version.trim());
		if (matcher.find()) {
			int major = Integer.parseInt(matcher.group(1));
			int minor = Integer.parseInt(matcher.group(3));
			Integer patch = (matcher.group(5) != null) ? Integer.parseInt(matcher.group(5)) : null;
			return new Version(major, minor, patch, os, matcher.group(0));
		}
		throw new IllegalArgumentException("Version '" + version + "' is invalid");
	}

	public String getOs() {
		return os;
	}

	public int getMajor() {
		return this.major;
	}

	public int getMinor() {
		return this.minor;
	}

	public Optional<Integer> getPatch() {
		return Optional.ofNullable(this.patch);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		Version v = (Version) other;
		return this.version.equals(v.version);
	}

	@Override
	public int hashCode() {
		return this.version.hashCode();
	}

	@Override
	public int compareTo(Version other) {
		Objects.requireNonNull(other, "'other' must not be null");
		int major = Integer.compare(getMajor(), other.getMajor());
		if (major == 0) {
			int min = Integer.compare(getMinor(), other.getMinor());
			if (min == 0) {
				int patch = Integer.compare(getPatch().orElse(-1), other.getPatch().orElse(-1));
				if (patch == 0) {
					return this.version.compareTo(other.version);
				}
				return patch;
			}
			return min;
		}
		return major;
	}

	@Override
	public String toString() {
		return this.version;
	}

}
