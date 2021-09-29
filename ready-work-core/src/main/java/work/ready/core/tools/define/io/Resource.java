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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;

public interface Resource {

	String getFileName();

	boolean exists();

	URL toURL() throws IOException;

	default InputStream getInputStream() throws IOException {
		return toURL().openStream();
	}

	default URI toURI() throws IOException {
		URL url = toURL();
		try {
			return url.toURI();
		}
		catch (URISyntaxException ex) {
			throw new IOException("URL '" + url + "' is not formatted strictly according to RFC2396", ex);
		}
	}

	default Path toPath() throws IOException, FileSystemNotFoundException {
		return Paths.get(toURI());
	}

	default File toFile() throws IOException {
		return new File(toURI());
	}

	default byte[] getBytes() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (InputStream is = getInputStream()) {
			byte[] buffer = new byte[8192];
			int read;
			while ((read = is.read(buffer)) != -1) {
				out.write(buffer, 0, read);
			}
			return out.toByteArray();
		}
	}

}
