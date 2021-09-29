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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

public class ArchiveResource implements Resource {

	private final Resource resource;

	public ArchiveResource(Resource resource) {
		this.resource = Objects.requireNonNull(resource, "'resource' must not be null");
	}

	public void forEach(ArchiveEntryCallback callback) throws IOException {
		Objects.requireNonNull(callback, "'callback' must not be null");
		try (ArchiveInputStream is = getInputStream()) {
			ArchiveEntry entry;
			while ((entry = is.getNextEntry()) != null) {
				callback.accept(entry, is);
			}
		}
	}

	public void extract(Path destination) throws IOException {
		Objects.requireNonNull(destination, "'destination' must not be null");
		forEach((entry, stream) -> {
			if (entry.isDirectory()) {
				Path directory = destination.resolve(entry.getName());
				Files.createDirectories(directory);
			}
			else {
				Path file = destination.resolve(entry.getName());
				Path directory = file.getParent();
				if (directory != null && !Files.exists(directory)) {
					Files.createDirectories(directory);
				}
				Files.copy(stream, file, StandardCopyOption.REPLACE_EXISTING);
			}
		});

	}

	public Resource getResource() {
		return this.resource;
	}

	@Override
	public String getFileName() {
		return this.resource.getFileName();
	}

	@Override
	public boolean exists() {
		return this.resource.exists();
	}

	@Override
	public URL toURL() throws IOException {
		return this.resource.toURL();
	}

	@Override
	public ArchiveInputStream getInputStream() throws IOException {
		Resource resource = this.resource;
		ArchiveInputStreamFactory archiveInputStreamFactory = createArchiveInputStreamFactory(resource);
		InputStream is = resource.getInputStream();
		try {
			return archiveInputStreamFactory.create(is);
		}
		catch (Exception ex) {
			try {
				is.close();
			}
			catch (Exception swallow) {
				ex.addSuppressed(swallow);
			}
			throw new IOException(ex);
		}
	}

	@Override
	public byte[] getBytes() throws IOException {
		throw new UnsupportedOperationException("Archive Resource does not support `getBytes`");
	}

	@Override
	public URI toURI() throws IOException {
		return this.resource.toURI();
	}

	@Override
	public Path toPath() throws IOException, FileSystemNotFoundException {
		return this.resource.toPath();
	}

	@Override
	public File toFile() throws IOException {
		return this.resource.toFile();
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || getClass() != other.getClass()) {
			return false;
		}

		ArchiveResource that = (ArchiveResource) other;
		return this.resource.equals(that.resource);
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", ArchiveResource.class.getSimpleName() + "[", "]")
				.add("resource=" + this.resource).toString();
	}

	@Override
	public int hashCode() {
		return this.resource.hashCode();
	}

	protected ArchiveInputStreamFactory createArchiveInputStreamFactory(Resource resource) {
		return ArchiveStreams.create(resource);
	}

	public interface ArchiveEntryCallback {

		void accept(ArchiveEntry entry, ArchiveInputStream stream) throws IOException;

	}

	@FunctionalInterface
	protected interface ArchiveInputStreamFactory {

		ArchiveInputStream create(InputStream is) throws ArchiveException, CompressorException;

	}

	private static final class ArchiveStreams {

		private static final Map<String, ArchiveInputStreamFactory> STREAMS;

		static {
			Map<String, ArchiveInputStreamFactory> streams = new LinkedHashMap<>();
			streams.put(".tar.gz", create(ArchiveStreamFactory.TAR, CompressorStreamFactory.GZIP));
			streams.put(".tar.bz2", create(ArchiveStreamFactory.TAR, CompressorStreamFactory.BZIP2));
			streams.put(".tgz", create(ArchiveStreamFactory.TAR, CompressorStreamFactory.GZIP));
			streams.put(".tbz2", create(ArchiveStreamFactory.TAR, CompressorStreamFactory.BZIP2));
			streams.put(".7z", create(ArchiveStreamFactory.SEVEN_Z));
			streams.put(".a", create(ArchiveStreamFactory.AR));
			streams.put(".ar", create(ArchiveStreamFactory.AR));
			streams.put(".arj", create(ArchiveStreamFactory.ARJ));
			streams.put(".cpio", create(ArchiveStreamFactory.CPIO));
			streams.put(".dump", create(ArchiveStreamFactory.DUMP));
			streams.put(".jar", create(ArchiveStreamFactory.JAR));
			streams.put(".tar", create(ArchiveStreamFactory.TAR));
			streams.put(".zip", create(ArchiveStreamFactory.ZIP));
			streams.put(".zipx", create(ArchiveStreamFactory.ZIP));
			STREAMS = Collections.unmodifiableMap(streams);
		}

		static ArchiveInputStreamFactory create(Resource resource) {
			String name = Objects.toString(resource.getFileName(), "");
			for (Map.Entry<String, ArchiveInputStreamFactory> entry : STREAMS.entrySet()) {
				if (name.endsWith(entry.getKey())) {
					return entry.getValue();
				}
			}
			throw new IllegalArgumentException("Archive Type for '" + resource + "' cannot be determined");
		}

		private static ArchiveInputStreamFactory create(String archiveType, String compressorType) {
			return is -> {
				ArchiveStreamFactory af = new ArchiveStreamFactory();
				CompressorStreamFactory csf = new CompressorStreamFactory();
				return af.createArchiveInputStream(archiveType, csf.createCompressorInputStream(compressorType, is));
			};
		}

		private static ArchiveInputStreamFactory create(String archiveType) {
			return is -> {
				ArchiveStreamFactory af = new ArchiveStreamFactory();
				return af.createArchiveInputStream(archiveType, is);
			};
		}

	}

}
