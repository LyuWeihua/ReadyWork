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

package work.ready.cloud.client;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.xnio.IoUtils;
import work.ready.cloud.ReadyCloud;
import work.ready.cloud.client.clevercall.CircuitBreaker;
import work.ready.cloud.client.oauth.Jwt;
import work.ready.cloud.client.oauth.TokenManager;
import work.ready.cloud.client.ssl.ClientTrustManager;
import work.ready.cloud.client.ssl.TLSConfig;
import work.ready.cloud.registry.base.URL;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Constant;
import work.ready.core.server.Ready;
import work.ready.core.service.result.Failure;
import work.ready.core.service.result.Result;
import work.ready.core.tools.HttpClient;
import work.ready.core.tools.NetUtil;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class CloudClient extends HttpClient {

    private static final Log logger = LogFactory.getLog(CloudClient.class);

    public static final String DEFAULT_USER_AGENT = "ReadyCloudClient";
    static final CloudClient instance;
    static final CloudClient trustAllInstance;
    static SSLContext sslContext;
    static final String KEY_STORE_PROPERTY = "javax.net.ssl.keyStore";
    static final String KEY_STORE_PASSWORD_PROPERTY = "javax.net.ssl.keyStorePassword";
    static final String TRUST_STORE_PROPERTY = "javax.net.ssl.trustStore";
    static final String TRUST_STORE_PASSWORD_PROPERTY = "javax.net.ssl.trustStorePassword";

    private TokenManager tokenManager = TokenManager.getInstance();
    static {
        trustAllInstance = getInstance(getTrustAllSsl());
        instance = getInstance(getDefaultSsl());
    }

    private CloudClient() {
        super();
    }

    private CloudClient(Consumer<java.net.http.HttpClient.Builder> clientBuilder) {
        super(clientBuilder);
        userAgent = DEFAULT_USER_AGENT;
    }

    public static CloudClient getInstance() {
        instance.timeout = defaultTimeout;
        return instance;
    }

    public static CloudClient getTrustAllInstance() {
        trustAllInstance.timeout = defaultTimeout;
        return trustAllInstance;
    }

    public static CloudClient getTrustAllInstance(boolean newInstance) {
        if(newInstance) {
            return getInstance(getTrustAllSsl());
        } else {
            return getTrustAllInstance();
        }
    }

    public static CloudClient getInstance(boolean newInstance) {
        if(newInstance) {
            return getInstance(getDefaultSsl());
        } else {
            return getInstance();
        }
    }

    public static CloudClient getInstance(Consumer<java.net.http.HttpClient.Builder> builder) {
        return new CloudClient(builder);
    }

    public static CloudClient getInstance(SSLContext sslContext) {
        return new CloudClient(builder -> {
            if (sslContext != null) { builder.sslContext(sslContext);}
        });
    }

    public static SSLContext getDefaultSsl() {
        if(sslContext == null) {
            try {
                sslContext = createSSLContext();
            } catch (Exception e) {
                logger.error(e,"Exception");
                throw new RuntimeException(e);
            }
        }
        return sslContext;
    }

    public CircuitBreaker getRequestService(URL url, String requestPath, HttpRequest.Builder requestBuilder) {
        return new CircuitBreaker(url, () -> callService(url, requestPath, requestBuilder));
    }

    public CircuitBreaker getRequestService(HttpRequest.Builder requestBuilder) {
        HttpRequest request = requestBuilder.build();
        return new CircuitBreaker(new URL(request.uri().getScheme(), request.uri().getHost(), request.uri().getPort(), request.uri().getPath()), () -> sendAsync(requestBuilder));
    }

    public CompletableFuture<HttpResponse<String>> callService(URL url, String requestPath, HttpRequest.Builder requestBuilder) {
        URI uri = null;
        try {
            uri = new URI(url.getRequestUri() + requestPath);
        } catch (URISyntaxException e){} 

        requestBuilder.uri(uri);
        return sendAsync(requestBuilder);
    }

    public void addAuthToken(HttpRequest.Builder requestBuilder, String token) {
        if(token != null && !token.startsWith("Bearer ")) {
            if(token.toUpperCase().startsWith("BEARER ")) {
                
                token = "Bearer " + token.substring(7);
            } else {
                token = "Bearer " + token;
            }
        }
        requestBuilder.header(Headers.AUTHORIZATION_STRING, token);
    }

    public void addAuthTokenTrace(HttpRequest.Builder requestBuilder, String token, String traceabilityId) {
        if(token != null && !token.startsWith("Bearer ")) {
            if(token.toUpperCase().startsWith("BEARER ")) {
                
                token = "Bearer " + token.substring(7);
            } else {
                token = "Bearer " + token;
            }
        }
        requestBuilder.header(Headers.AUTHORIZATION_STRING, token);
        requestBuilder.header(Constant.TRACEABILITY_ID_STRING, traceabilityId);
    }

    public void addAuthTokenTrace(HttpRequest.Builder requestBuilder, String token) {
        if(token != null && !token.startsWith("Bearer ")) {
            if(token.toUpperCase().startsWith("BEARER ")) {
                
                token = "Bearer " + token.substring(7);
            } else {
                token = "Bearer " + token;
            }
        }
        requestBuilder.header(Headers.AUTHORIZATION_STRING, token);
    }

    public Result addCcToken(HttpRequest.Builder requestBuilder) {
        Result<Jwt> result = tokenManager.getJwt(requestBuilder.build());
        if(result.isFailure()) { return Failure.of(result.getError()); }
        requestBuilder.header(Headers.AUTHORIZATION_STRING, "Bearer " + result.getResult().getJwt());
        return result;
    }

    public Result addCcTokenTrace(HttpRequest.Builder requestBuilder, String traceabilityId) {
        Result<Jwt> result = tokenManager.getJwt(requestBuilder.build());
        if(result.isFailure()) { return Failure.of(result.getError()); }
        requestBuilder.header(Headers.AUTHORIZATION_STRING, "Bearer " + result.getResult().getJwt());
        requestBuilder.header(Constant.TRACEABILITY_ID_STRING, traceabilityId);
        return result;
    }

    public Result propagateHeaders(HttpRequest.Builder requestBuilder, final HttpServerExchange exchange) {
        String token = exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION);
        String tid = exchange.getRequestHeaders().getFirst(Constant.TRACEABILITY_ID);
        String cid = exchange.getRequestHeaders().getFirst(Constant.CORRELATION_ID);
        return populateHeader(requestBuilder, token, cid, tid);
    }

    public Result populateHeader(HttpRequest.Builder requestBuilder, String authToken, String correlationId, String traceabilityId) {
        Result<Jwt> result = tokenManager.getJwt(requestBuilder.build());
        if(result.isFailure()) { return Failure.of(result.getError()); }
        
        if(authToken == null) {
            authToken = "Bearer " + result.getResult().getJwt();
        } else {
            requestBuilder.header(Constant.SCOPE_TOKEN_STRING, "Bearer " + result.getResult().getJwt());
        }
        requestBuilder.header(Constant.CORRELATION_ID_STRING, correlationId);
        if(traceabilityId != null) {
            addAuthTokenTrace(requestBuilder, authToken, traceabilityId);
        } else {
            addAuthToken(requestBuilder, authToken);
        }
        return result;
    }

    public Result populateHeader(HttpRequest.Builder requestBuilder, String authToken) {
        Result<Jwt> result = tokenManager.getJwt(requestBuilder.build());
        if(result.isFailure()) { return Failure.of(result.getError()); }
        
        if(authToken == null) {
            authToken = "Bearer " + result.getResult().getJwt();
        } else {
            requestBuilder.header(Constant.SCOPE_TOKEN_STRING, "Bearer " + result.getResult().getJwt());
        }
        addAuthToken(requestBuilder, authToken);
        return result;
    }

    private static KeyStore loadKeyStore(final String name, final char[] password) throws IOException {
        final InputStream stream = Ready.config().getInputStreamFromFile(name);
        if(stream == null) {
            throw new RuntimeException("Could not load keystore");
        }
        try {
            KeyStore loadedKeystore = KeyStore.getInstance("PKCS12");
            loadedKeystore.load(stream, password);

            return loadedKeystore;
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
            throw new IOException(String.format("Unable to load KeyStore %s", name), e);
        } finally {
            IoUtils.safeClose(stream);
        }
    }

    public static SSLContext createSSLContext() throws IOException {
        String defaultGroupKey = ReadyCloud.getConfig().getHttpClient().getTls().getDefaultGroupKey();
        return null == defaultGroupKey ? null : createSSLContext(defaultGroupKey);
    }

    @SuppressWarnings("unchecked")
    public static SSLContext createSSLContext(String trustedNamesGroupKey) throws IOException {
        SSLContext sslContext = null;
        KeyManager[] keyManagers = null;
        ClientConfig.TlsConfig config = ReadyCloud.getConfig().getHttpClient().getTls();
        if(config != null) {
            try {
                
                if (config.isLoadKeyStore()) {
                    String keyStoreName = System.getProperty(KEY_STORE_PROPERTY);
                    String keyStorePass = System.getProperty(KEY_STORE_PASSWORD_PROPERTY);
                    if (keyStoreName != null && keyStorePass != null) {
                        if(logger.isInfoEnabled()) logger.info("Loading key store from system property at " + keyStoreName);
                    } else {
                        keyStoreName = config.getKeyStore();
                        
                        keyStorePass = config.getKeyStorePass();
                        if(keyStorePass == null) {
                            keyStorePass = Ready.getMainApplicationConfig().getSecurity().getClientKeystorePass();
                        }
                        if(logger.isInfoEnabled()) logger.info("Loading key store from config at " + keyStoreName);
                    }
                    if (keyStoreName != null && keyStorePass != null) {
                        String keyPass = config.getKeyPass();
                        if(keyPass == null) {
                            keyPass = Ready.getMainApplicationConfig().getSecurity().getClientKeyPass();
                        }
                        KeyStore keyStore = NetUtil.loadKeyStore(keyStoreName, keyStorePass.toCharArray());
                        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                        keyManagerFactory.init(keyStore, keyPass.toCharArray());
                        keyManagers = keyManagerFactory.getKeyManagers();
                    }
                }
            } catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException e) {
                throw new IOException("Unable to initialise KeyManager[]", e);
            }

            TrustManager[] trustManagers = null;
            try {

                if (config.isLoadTrustStore()) {
                    String trustStoreName = System.getProperty(TRUST_STORE_PROPERTY);
                    String trustStorePass = System.getProperty(TRUST_STORE_PASSWORD_PROPERTY);
                    if (trustStoreName != null && trustStorePass != null) {
                        if(logger.isInfoEnabled()) logger.info("Loading trust store from system property at " + trustStoreName);
                    } else {
                        trustStoreName = config.getTrustStore();
                        trustStorePass = config.getTrustStorePass();
                        if(trustStorePass == null) {
                            trustStorePass = Ready.getMainApplicationConfig().getSecurity().getClientTruststorePass();
                        }
                        if(logger.isInfoEnabled()) logger.info("Loading trust store from config at " + trustStoreName);
                    }
                    if (trustStoreName != null && trustStorePass != null) {
                        KeyStore trustStore = NetUtil.loadTrustStore(trustStoreName, trustStorePass.toCharArray());
                        TLSConfig tlsConfig = TLSConfig.create(config, trustedNamesGroupKey);

                        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                        trustManagerFactory.init(trustStore);
                        
                        trustManagers = ClientTrustManager.decorate(trustManagerFactory.getTrustManagers(), tlsConfig);
                    }
                }
            } catch (NoSuchAlgorithmException | KeyStoreException e) {
                throw new IOException("Unable to initialise TrustManager[]", e);
            }

            try {
                sslContext = SSLContext.getInstance("TLSv1.2");
                sslContext.init(keyManagers, trustManagers, null);
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new IOException("Unable to create and initialise the SSLContext", e);
            }
        } else {
            logger.error("TLS configuration section is missing in config");
        }

        return sslContext;
    }

}
