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

import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import work.ready.core.handler.RequestMethod;
import work.ready.core.json.Json;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.security.HttpAuth;
import work.ready.core.server.Constant;
import work.ready.core.server.Ready;
import work.ready.core.server.TrustAllTrustManager;
import work.ready.core.tools.define.LambdaFinal;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

public class HttpClient {

    private static final Log logger = LogFactory.getLog(HttpClient.class);
    public static final String HTTP_CLIENT_PROXY_PROPERTY = "ready.http.client.proxy";
    private static InetSocketAddress defaultProxy;
    private static String defaultUserAgent = "ReadyHttpClient";
    private static Authenticator defaultAuthenticator = Authenticator.getDefault();
    protected static Duration defaultTimeout = Duration.of(10, ChronoUnit.SECONDS);
    public static final String AUTHORIZATION_FIELD = "__" + Headers.AUTHORIZATION_STRING + "__";
    private java.net.http.HttpClient.Builder builder;
    private DynamicProxySelector proxySelector;
    private java.net.http.HttpClient client;
    protected List<RequestInterceptor> interceptors;
    protected String userAgent = defaultUserAgent;
    protected Duration timeout = defaultTimeout;

    private static final HttpClient instance = new HttpClient();

    protected HttpClient() {
        this(builder -> {});
    }

    protected HttpClient(Consumer<java.net.http.HttpClient.Builder> clientBuilder) {
        java.net.http.HttpClient.Builder builder = java.net.http.HttpClient.newBuilder();
        if(defaultProxy != null) {
            proxySelector = new DynamicProxySelector(defaultProxy);
            builder.proxy(proxySelector);
        }
        builder.followRedirects(java.net.http.HttpClient.Redirect.ALWAYS);
        if(defaultAuthenticator != null) {
            builder.authenticator(defaultAuthenticator);
        }

        builder.sslContext(getTrustAllSsl());
        clientBuilder.accept(builder);
        this.builder = builder;
        this.client = builder.build();
    }

    public static HttpClient getInstance() {
        instance.timeout = defaultTimeout;
        return instance;
    }

    public static HttpClient getInstance(boolean newInstance) {
        if(newInstance) {
            return new HttpClient();
        } else {
            return getInstance();
        }
    }

    public static HttpClient getInstance(Consumer<java.net.http.HttpClient.Builder> builder) {
         return new HttpClient(builder);
    }

    public static SSLContext getTrustAllSsl() {
        try {
            TrustManager[] trustManagers = {new TrustAllTrustManager()};
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, trustManagers, null);
            return sslContext;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException("Unable to create and initialise the SSLContext", e);
        }
    }

    public HttpClient addProxy(String host, int port) {
        if(proxySelector == null) {
            proxySelector = new DynamicProxySelector();
            builder.proxy(proxySelector);
            this.client = builder.build();
        }
        proxySelector.addProxy(InetSocketAddress.createUnresolved(host, port));
        return this;
    }

    public static void setDefaultProxy(String host, int port) {
        defaultProxy = InetSocketAddress.createUnresolved(host, port);
    }

    public static void setDefaultProxy(String hostAndPort) {
        if(Ready.getProperty(HTTP_CLIENT_PROXY_PROPERTY) != null) {
            hostAndPort = Ready.getProperty(HTTP_CLIENT_PROXY_PROPERTY);
        }
        String[] proxyArray = StrUtil.split(hostAndPort, ":");
        if(proxyArray.length == 2 && StrUtil.isNumbers(proxyArray[1])) {
            defaultProxy = InetSocketAddress.createUnresolved(proxyArray[0], Integer.parseInt(proxyArray[1]));
        }
    }

    public static InetSocketAddress getDefaultProxy() {
        if(defaultProxy == null) setDefaultProxy(null);
        return defaultProxy;
    }

    public static Authenticator getDefaultAuthenticator() {
        return defaultAuthenticator;
    }

    public static void setDefaultAuthenticator(Authenticator authenticator) {
        defaultAuthenticator = authenticator;
    }

    public static void setDefaultTimeout(Duration timeout) {
        defaultTimeout = timeout;
    }

    public static void setDefaultUserAgent(String userAgent) {
        defaultUserAgent = userAgent;
    }

    public HttpClient withTimeout(long millis) {
        this.timeout = millis > 0 ? Duration.ofMillis(millis) : defaultTimeout;
        return this;
    }

    public HttpClient withUserAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    public HttpClient setAuthenticator(Authenticator authenticator) {
        builder.authenticator(authenticator);
        this.client = builder.build();
        return this;
    }

    public HttpClient setSslContext(SSLContext sslContext) {
        builder.sslContext(sslContext);
        this.client = builder.build();
        return this;
    }

    public SSLContext getSslContext() {
        return this.client.sslContext();
    }

    public String fetch(String url) {
        return fetch(url, null);
    }

    public String fetch(String url, String authUser, String authPwd) {
        if(authUser != null && authPwd != null) {
            return fetch(url, HttpAuth.getBasicAuthHeader(authUser, authPwd));
        } else {
            return fetch(url, null);
        }
    }

    public String fetch(String url, String authorization) { 
        try {
            URI uri = new URI(url);
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri).timeout(timeout);
            if(authorization != null) {
                requestBuilder.header(Headers.AUTHORIZATION_STRING, authorization);
            }
            requestBuilder.header(Headers.USER_AGENT_STRING, userAgent);
            HttpResponse<String> response = send(requestBuilder);
            if (response.statusCode() == StatusCodes.OK) {
                return response.body();
            } else {
                logger.error("failed to fetch url: %s, statusCode: %s, body: ", url, response.statusCode(), response.body());
                return null;
            }
        } catch (URISyntaxException | IOException | InterruptedException e) {
            logger.error(e, "failed to fetch url: %s", url);
        }
        return null;
    }

    public String jsonGet(String url, Map<String, Object> formData) {
        return jsonRequest(url, RequestMethod.GET, formData);
    }

    public String jsonDelete(String url, Map<String, Object> formData) {
        return jsonRequest(url, RequestMethod.DELETE, formData);
    }

    public String jsonPost(String url, Map<String, Object> formData) {
        return jsonRequest(url, RequestMethod.POST, formData);
    }

    public String jsonPut(String url, Map<String, Object> formData) {
        return jsonRequest(url, RequestMethod.PUT, formData);
    }

    public String jsonRequest(String url, RequestMethod method, Map<String, Object> formData) {
        try {
            if(method == null) {
                method = RequestMethod.GET;
            }
            HttpRequest.BodyPublisher publisher;
            if(formData == null) {
                formData = new HashMap<>();
            }
            if(!RequestMethod.hasBody(method)) {
                url = HttpUtil.buildUrlWithQueryString(url, formData);
                publisher = HttpRequest.BodyPublishers.noBody();
            } else {
                publisher = HttpRequest.BodyPublishers.ofString(Json.getJson().toJson(formData));
            }
            URI uri = new URI(url);
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri).timeout(timeout);
            requestBuilder.header(Headers.CONTENT_TYPE_STRING, "application/json");
            requestBuilder.header(Headers.ACCEPT_STRING, "application/json");
            if(formData.get(AUTHORIZATION_FIELD) != null) {
                requestBuilder.header(Headers.AUTHORIZATION_STRING, formData.remove(AUTHORIZATION_FIELD).toString());
            }
            requestBuilder.header(Headers.USER_AGENT_STRING, userAgent);
            requestBuilder.method(method.name(), publisher);
            HttpResponse<String> response = send(requestBuilder);
            if (response.statusCode() == StatusCodes.OK) {
                return response.body();
            } else {
                logger.error("failed to request url: %s, statusCode: %s, body: ", url, response.statusCode(), response.body());
                return null;
            }
        } catch (URISyntaxException | IOException | InterruptedException e) {
            logger.error(e, "failed to request url: %s", url);
        }
        return null;
    }

    public HttpResponse<String> send(final URI uri) throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder(uri).timeout(timeout));
    }

    public HttpResponse<String> send(HttpRequest.Builder requestBuilder) throws IOException, InterruptedException {
        return send(requestBuilder, HttpResponse.BodyHandlers.ofString());
    }

    public <T> HttpResponse<T> send(HttpRequest.Builder requestBuilder, HttpResponse.BodyHandler<T> handler) throws IOException, InterruptedException {
        return sendBase(requestBuilder, handler);
    }

    public void getAsync(final String url, Map<String, Object> formData, Consumer<HttpResponse<String>> action, Function<Throwable, HttpResponse<String>> exceptionally) {
        sendAsync(url, RequestMethod.GET, formData, action, exceptionally);
    }

    public void getAsync(final String url, Map<String, Object> formData, Consumer<HttpResponse<String>> action) {
        sendAsync(url, RequestMethod.GET, formData, action, (e)->null);
    }

    public void getAsync(final String url, Consumer<HttpResponse<String>> action, Function<Throwable, HttpResponse<String>> exceptionally) {
        sendAsync(url, RequestMethod.GET, null, action, exceptionally);
    }

    public void getAsync(final String url, Consumer<HttpResponse<String>> action) {
        sendAsync(url, RequestMethod.GET, null, action, (e)->null);
    }

    public HttpResponse<String> postAsync(String url, Map<String, Object> formData, String fileField, Path filePath) {
        Map<String, Path> uploadFiles = new HashMap<>();
        if(fileField != null && filePath != null) {
            uploadFiles.put(fileField, filePath);
        }
        return postAsync(url, formData, uploadFiles);
    }

    public HttpResponse<String> postAsync(String url, Map<String, Object> formData, Map<String, Path> uploadFiles) {
        LambdaFinal<HttpResponse<String>> var = new LambdaFinal<>();
        postAsync(url, formData, uploadFiles, var::set, (e)->null);
        return var.get();
    }

    public void postAsync(String url, Map<String, Object> formData, Map<String, Path> uploadFiles, Consumer<HttpResponse<String>> action, Function<Throwable, HttpResponse<String>> exceptionally) {
        if(formData == null) {
            formData = new LinkedHashMap<>();
        }
        if(uploadFiles != null) {
            formData.putAll(uploadFiles);
        }
        try {
            String boundary = UUID.randomUUID().toString().replaceAll("-", "");
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .header("Content-Type", "multipart/form-data;boundary=" + boundary)
                    .POST(MultipartBodyPublisher.ofMultipart(formData, boundary))
                    .uri(URI.create(url));
            if(formData.get(AUTHORIZATION_FIELD) != null) {
                requestBuilder.header(Headers.AUTHORIZATION_STRING, formData.remove(AUTHORIZATION_FIELD).toString());
            }
            requestBuilder.header(Headers.USER_AGENT_STRING, userAgent);
            sendAsyncBase(requestBuilder, HttpResponse.BodyHandlers.ofString())
                    .exceptionally(exceptionally)
                    .thenAccept(action).join();
        } catch (Exception e) {
            exceptionally.apply(e);
        }
    }

    public void postAsync(final String url, Map<String, Object> formData, Consumer<HttpResponse<String>> action, Function<Throwable, HttpResponse<String>> exceptionally) {
        sendAsync(url, RequestMethod.POST, formData, action, exceptionally);
    }

    public void postAsync(final String url, Map<String, Object> formData, Consumer<HttpResponse<String>> action) {
        sendAsync(url, RequestMethod.POST, formData, action, (e)->null);
    }

    public void postAsync(final String url, Consumer<HttpResponse<String>> action) {
        sendAsync(url, RequestMethod.POST, null, action, (e)->null);
    }

    public void putAsync(final String url, Map<String, Object> formData, Consumer<HttpResponse<String>> action, Function<Throwable, HttpResponse<String>> exceptionally) {
        sendAsync(url, RequestMethod.PUT, formData, action, exceptionally);
    }

    public void putAsync(final String url, Map<String, Object> formData, Consumer<HttpResponse<String>> action) {
        sendAsync(url, RequestMethod.PUT, formData, action, (e)->null);
    }

    public void putAsync(final String url, Consumer<HttpResponse<String>> action) {
        sendAsync(url, RequestMethod.PUT, null, action, (e)->null);
    }

    public void deleteAsync(final String url, Map<String, Object> formData, Consumer<HttpResponse<String>> action, Function<Throwable, HttpResponse<String>> exceptionally) {
        sendAsync(url, RequestMethod.DELETE, formData, action, exceptionally);
    }

    public void deleteAsync(final String url, Map<String, Object> formData, Consumer<HttpResponse<String>> action) {
        sendAsync(url, RequestMethod.DELETE, formData, action, (e)->null);
    }

    public void deleteAsync(final String url, Consumer<HttpResponse<String>> action) {
        sendAsync(url, RequestMethod.DELETE, null, action, (e)->null);
    }

    public void sendAsync(String url, RequestMethod method, Map<String, Object> formData, Consumer<HttpResponse<String>> action, Function<Throwable, HttpResponse<String>> exceptionally) {
        if(method == null) {
            method = RequestMethod.GET;
        }
        HttpRequest.BodyPublisher publisher;
        var builder = HttpRequest.newBuilder().timeout(timeout);
        if(formData == null) {
            formData = new HashMap<>();
        }
        if(!RequestMethod.hasBody(method)) {
            url = HttpUtil.buildUrlWithQueryString(url, formData);
            publisher = HttpRequest.BodyPublishers.noBody();
        } else {
            publisher = FormBodyPublisher.ofForm(formData);
            builder.header(Headers.CONTENT_TYPE_STRING, "application/x-www-form-urlencoded");
        }
        if(formData.get(AUTHORIZATION_FIELD) != null) {
            builder.header(Headers.AUTHORIZATION_STRING, formData.remove(AUTHORIZATION_FIELD).toString());
        }
        builder.header(Headers.USER_AGENT_STRING, userAgent);
        builder.method(method.name(), publisher);
        try {
            sendAsync(builder.uri(new URI(url)), action, exceptionally);
        } catch (Exception e) {
            exceptionally.apply(e);
        }
    }

    public void sendAsync(HttpRequest.Builder requestBuilder, Consumer<HttpResponse<String>> action) {
        sendAsync(requestBuilder, HttpResponse.BodyHandlers.ofString(), action, (e)->null);
    }

    public void sendAsync(HttpRequest.Builder requestBuilder, Consumer<HttpResponse<String>> action, Function<Throwable, HttpResponse<String>> exceptionally) {
        sendAsync(requestBuilder, HttpResponse.BodyHandlers.ofString(), action, exceptionally);
    }

    public <T> void sendAsync(HttpRequest.Builder requestBuilder, HttpResponse.BodyHandler<T> handler, Consumer<HttpResponse<T>> action) {
        sendAsync(requestBuilder, handler, action, (e)->null);
    }

    public <T> void sendAsync(HttpRequest.Builder requestBuilder, HttpResponse.BodyHandler<T> handler, Consumer<HttpResponse<T>> action, Function<Throwable, HttpResponse<T>> exceptionally) {
        sendAsyncBase(requestBuilder, handler)
                
                .exceptionally(exceptionally)
                .thenAccept(action).join();
    }

    public CompletableFuture<HttpResponse<String>> sendAsync(HttpRequest.Builder requestBuilder) {
        return sendAsync(requestBuilder, HttpResponse.BodyHandlers.ofString());
    }

    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest.Builder requestBuilder, HttpResponse.BodyHandler<T> handler) {
        return sendAsyncBase(requestBuilder, handler)
                
                .exceptionally(e -> null);
    }

    public void sendAsync(List<HttpRequest.Builder> requests, Consumer<HttpResponse<String>> action) {
        sendAsync(requests, HttpResponse.BodyHandlers.ofString(), action, (e)->null);
    }

    public void sendAsync(List<HttpRequest.Builder> requests, Consumer<HttpResponse<String>> action, Function<Throwable, HttpResponse<String>> exceptionally) {
        sendAsync(requests, HttpResponse.BodyHandlers.ofString(), action, exceptionally);
    }

    public <T> void sendAsync(List<HttpRequest.Builder> requests, HttpResponse.BodyHandler<T> handler, Consumer<HttpResponse<T>> action, Function<Throwable, HttpResponse<T>> exceptionally) {
        CompletableFuture.allOf(requests.stream()
                .map(req -> sendAsyncBase(req, handler)
                        
                        .exceptionally(exceptionally)
                        .thenAccept(action))
                .toArray(CompletableFuture[]::new))
                .join();
    }

    private <T> HttpResponse<T> sendBase(HttpRequest.Builder requestBuilder, HttpResponse.BodyHandler<T> responseBodyHandler) throws IOException, InterruptedException {
        if(interceptors != null) {
            interceptors.forEach(i->i.intercept(requestBuilder));
        }
        return client.send(requestBuilder.build(), responseBodyHandler);
    }

    private <T> CompletableFuture<HttpResponse<T>> sendAsyncBase(HttpRequest.Builder requestBuilder, HttpResponse.BodyHandler<T> responseBodyHandler) {
        if(interceptors != null) {
            interceptors.forEach(i->i.intercept(requestBuilder));
        }
        return client.sendAsync(requestBuilder.build(), responseBodyHandler);
    }

    public void addInterceptor(RequestInterceptor interceptor) {
        if(interceptors == null) {
            interceptors = new CopyOnWriteArrayList<>();
        }
        interceptors.add(interceptor);
    }

    @FunctionalInterface
    public interface RequestInterceptor {
        void intercept(HttpRequest.Builder requestBuilder);
    }

    public Path fileDownload(String url, Path filePath) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(url));
        try {
            HttpResponse<Path> response = sendBase(
                    requestBuilder, HttpResponse.BodyHandlers.ofFile(filePath));
            return response.body();
        } catch (Exception e) {
            return null;
        }
    }

    public Path fileDownloadToPath(String url, Path directory) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(url));
        try {
            HttpResponse<Path> response = sendBase(
                    requestBuilder, HttpResponse.BodyHandlers
                            .ofFileDownload(directory, CREATE, WRITE));
            return response.body();
        } catch (Exception e) {
            return null;
        }
    }

    public static String getFormDataString(Map<String, Object> params) {
        return FormBodyPublisher.getString(params);
    }

    public static HttpRequest.BodyPublisher getFormBodyPublisher(Map<String, Object> formData) {
        return FormBodyPublisher.ofForm(formData);
    }

    public static HttpRequest.BodyPublisher getNoBodyPublisher() {
        return HttpRequest.BodyPublishers.noBody();
    }

    public static HttpRequest.BodyPublisher getStringBodyPublisher(String body) {
        return HttpRequest.BodyPublishers.ofString(body);
    }

    public static HttpRequest.BodyPublisher getFileBodyPublisher(Path path) throws FileNotFoundException {
        return HttpRequest.BodyPublishers.ofFile(path);
    }

    private static class FormBodyPublisher {
        public static HttpRequest.BodyPublisher ofForm(Map<String, Object> data) {
            return HttpRequest.BodyPublishers.ofString(getString(data));
        }
        public static String getString(Map<String, Object> data) {
            StringBuilder body = new StringBuilder();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (body.length() > 0) {
                    body.append("&");
                }
                body.append(encode(entry.getKey()))
                        .append("=")
                        .append(encode(entry.getValue()).replaceAll("\\+", "%20"));
            }
            return body.toString();
        }
        private static String encode(Object obj) {
            return URLEncoder.encode(obj.toString(), Constant.DEFAULT_CHARSET);
        }
    }

    private static class MultipartBodyPublisher {

        private static final String LINE_SEPARATOR = System.lineSeparator();

        public static HttpRequest.BodyPublisher ofMultipart(
                Map<String, Object> data, String boundary) throws IOException {

            final byte[] separator = ("--" + boundary + LINE_SEPARATOR
                    + "Content-Disposition: form-data; name=").getBytes(Constant.DEFAULT_CHARSET);
            final List<byte[]> body = new ArrayList<>();

            for (String dataKey : data.keySet()) {
                body.add(separator);

                Object dataValue = data.get(dataKey);

                if (dataValue instanceof Path) {
                    Path path = (Path) dataValue;

                    String mimeType = fetchMimeType(path);

                    body.add(("\"" + dataKey + "\"; filename=\"" + path.getFileName()
                            + "\"" + LINE_SEPARATOR + "Content-Type: "
                            + mimeType + LINE_SEPARATOR + LINE_SEPARATOR)
                            .getBytes(Constant.DEFAULT_CHARSET));

                    body.add(Files.readAllBytes(path));

                    body.add(LINE_SEPARATOR.getBytes(Constant.DEFAULT_CHARSET));
                } else {
                    body.add(("\"" + dataKey + "\""
                            + LINE_SEPARATOR + LINE_SEPARATOR + dataValue + LINE_SEPARATOR)
                            .getBytes(Constant.DEFAULT_CHARSET));
                }
            }

            body.add(("--" + boundary + "--").getBytes(Constant.DEFAULT_CHARSET));

            return HttpRequest.BodyPublishers.ofByteArrays(body);
        }

        private static String fetchMimeType(Path filenamePath) throws IOException {
            String mimeType = Files.probeContentType(filenamePath);

            if (mimeType == null) {
                throw new IOException("Mime type could not be fetched");
            }

            return mimeType;
        }
    }

    private static class DynamicProxySelector extends ProxySelector {
        private static final List<Proxy> NO_PROXY_LIST = List.of(Proxy.NO_PROXY);
        final List<Proxy> list = new CopyOnWriteArrayList<>();
        private AtomicInteger idx = new AtomicInteger((int)(Math.random()*10));

        DynamicProxySelector(){ }
        DynamicProxySelector(InetSocketAddress address) {
            addProxy(address);
        }

        DynamicProxySelector addProxy(InetSocketAddress address) {
            if(address != null) {
                list.add(new Proxy(Proxy.Type.HTTP, address));
            }
            return this;
        }

        @Override
        public void connectFailed(URI uri, SocketAddress sa, IOException e) {
            
        }

        @Override
        public synchronized List<Proxy> select(URI uri) {
            String scheme = uri.getScheme().toLowerCase();
            if (scheme.equals("http") || scheme.equals("https")) {
                if(list.size() > 1) {
                    return doSelect();
                } else {
                    return list;
                }
            } else {
                return NO_PROXY_LIST;
            }
        }

        private List<Proxy> doSelect() {
            int index = getNextPositive();
            return List.of(list.get(index % list.size()));
        }

        private int getNextPositive() {
            return getPositive(idx.incrementAndGet());
        }
        private int getPositive(int originValue){
            return 0x7fffffff & originValue;
        }
    }
}
