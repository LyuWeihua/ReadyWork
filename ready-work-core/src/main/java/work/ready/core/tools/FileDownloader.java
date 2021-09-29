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

package work.ready.core.tools;

import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.tools.define.io.FileSystemResource;
import work.ready.core.tools.define.io.Resource;
import work.ready.core.tools.define.io.UrlResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public final class FileDownloader {

    private static final Log logger = LogFactory.getLog(FileDownloader.class);

    private final Duration readTimeout;

    private final Duration connectTimeout;

    private final Proxy proxy;

    public FileDownloader(Duration readTimeout, Duration connectTimeout, Proxy proxy) {
        this.readTimeout = readTimeout;
        this.connectTimeout = connectTimeout;
        this.proxy = proxy;
    }

    public Resource download(URL url, String fileName) throws IOException {
        return download(url, new DefaultProgressListener(url, fileName));
    }

    public Resource download(URL url, ProgressListener progressListener) throws IOException {
        URLConnection connection = connect(url);
        try (InputStream is = connection.getInputStream()) {
            long totalSize = connection.getContentLengthLong();
            Path tempFile = createTempFile(url);
            progressListener.start();
            try (OutputStream os = Files.newOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                long readBytes = 0;
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                    readBytes += read;
                    if (totalSize > 0 && readBytes > 0) {
                        progressListener.update(readBytes, totalSize);
                    }
                }
            }
            if (Thread.interrupted()) {
                throw new ClosedByInterruptException();
            }
            progressListener.finish();
            return new FileSystemResource(tempFile);
        }
    }

    private URLConnection connect(URL url) throws IOException {
        int maxRedirects = 10;
        URL target = url;
        for (; ; ) {
            URLConnection connection = connect(target, this.readTimeout, this.connectTimeout, this.proxy);
            if (connection instanceof HttpURLConnection) {
                HttpURLConnection httpConnection = (HttpURLConnection) connection;
                httpConnection.setInstanceFollowRedirects(false);
                int status = httpConnection.getResponseCode();
                if (status >= 300 && status <= 307 && status != 306 && status != 304) {
                    if (maxRedirects < 0) {
                        throw new IOException("Too many redirects for URL '" + url + "'");
                    }
                    String location = httpConnection.getHeaderField("Location");
                    if (location != null) {
                        httpConnection.disconnect();
                        maxRedirects--;
                        target = new URL(url, location);
                        continue;
                    }
                }
                if (status == HttpURLConnection.HTTP_OK) {
                    return connection;
                }
                throw new IOException("HTTP Status '" + status + "' is invalid for URL '" + url + "'");
            }
            return connection;
        }

    }

    private static URLConnection connect(URL url, Duration readTimeout, Duration connectTimeout,
                                         Proxy proxy) throws IOException {
        URLConnection connection = (proxy != null) ? url.openConnection(proxy) : url.openConnection();
        connection.setConnectTimeout(Math.toIntExact(connectTimeout.toMillis()));
        connection.setReadTimeout(Math.toIntExact(readTimeout.toMillis()));
        return connection;
    }

    private static Path createTempFile(URL url) throws IOException {
        String fileName = new UrlResource(url).getFileName();
        if (StrUtil.isBlank(fileName)) {
            throw new IllegalArgumentException(
                    String.format("There is no way to determine a file name from a '%s'", url));
        }
        Path tempFile = Files.createTempFile("", "-" + fileName);
        tempFile.toFile().deleteOnExit();
        return tempFile;
    }

    public interface ProgressListener {

        void start();

        void update(long readBytes, long totalBytes);

        void finish();

    }

    final class DefaultProgressListener implements ProgressListener {

        private static final long MB = 1024 * 1024;

        private final URL url;

        private final String fileName;

        private long lastPercent;

        DefaultProgressListener(URL url, String fileName) {
            this.url = url;
            this.fileName = fileName;
        }

        @Override
        public void start() {
            logger.info("Downloading '%s' from '%s'", this.fileName, this.url);
        }

        @Override
        public void update(long readBytes, long totalBytes) {
            long percent = readBytes * 100 / totalBytes;
            if (percent - this.lastPercent >= 10) {
                this.lastPercent = percent;
                logger.info("Downloaded %sMB / %sMB  %s%%", (readBytes / MB), (totalBytes / MB), percent);
            }
        }

        @Override
        public void finish() {
            logger.info("File '%s' is downloaded from '%s'", this.fileName, this.url);
        }

    }
}

