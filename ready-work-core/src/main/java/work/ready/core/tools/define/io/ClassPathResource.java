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
package work.ready.core.tools.define.io;

import work.ready.core.tools.StrUtil;

import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Objects;
import java.util.StringJoiner;

public class ClassPathResource implements Resource {

	private final String path;

	private final ClassLoader classLoader;

	public ClassPathResource(String path) {
		this(path, null);
	}

	public ClassPathResource(String path, ClassLoader classLoader) {
		if (StrUtil.isBlank(path)) {
			throw new IllegalArgumentException("'name' must not be null or empty");
		}
		this.path = cleanPath(path);
		this.classLoader = (classLoader != null) ? classLoader : getClass().getClassLoader();
	}

	public String getPath() {
		return this.path;
	}

	public ClassLoader getClassLoader() {
		return this.classLoader;
	}

	@Override
	public URL toURL() throws FileNotFoundException {
		URL url = getURL();
		if (url == null) {
			throw new FileNotFoundException(String.format("ClassPathResource '%s' does not exist", this.path));
		}
		return url;
	}

	@Override
	public String getFileName() {
		String name = this.path;
		int index = name.lastIndexOf('/');
		return (index != -1) ? name.substring(index + 1) : name;
	}

	@Override
	public boolean exists() {
		return getURL() != null;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		ClassPathResource that = (ClassPathResource) other;
		return this.path.equals(that.path) && Objects.equals(this.classLoader, that.classLoader);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.path, this.classLoader);
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", ClassPathResource.class.getSimpleName() + "[", "]")
				.add("path='" + this.path + "'").toString();
	}

	private URL getURL() {
		ClassLoader cl = this.classLoader;
		return (cl != null) ? cl.getResource(this.path) : ClassLoader.getSystemResource(this.path);
	}

	private static String cleanPath(String name) {
		String path = name.replace('\\', '/').replaceAll("/+", "/").trim();
		return path.startsWith("/") ? path.substring(1) : path;
	}

}
