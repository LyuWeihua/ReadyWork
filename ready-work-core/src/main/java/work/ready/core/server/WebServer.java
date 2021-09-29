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
package work.ready.core.server;

import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.encoding.ContentEncodingRepository;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.server.handlers.encoding.GzipEncodingProvider;
import io.undertow.util.Headers;
import org.xnio.Options;
import org.xnio.Sequence;
import org.xnio.SslClientAuthMode;
import work.ready.core.event.Event;
import work.ready.core.event.GeneralEvent;
import work.ready.core.handler.GZipPredicate;
import work.ready.core.handler.MainHandler;
import work.ready.core.handler.action.HttpDisableHandler;
import work.ready.core.handler.action.HttpToHttpsHandler;
import work.ready.core.handler.RequestHandler;
import work.ready.core.handler.response.SetHeaderHandler;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.module.Application;
import work.ready.core.module.ApplicationContext;
import work.ready.core.module.Initializer;
import work.ready.core.security.SecurityConfig;
import work.ready.core.tools.NetUtil;
import work.ready.core.tools.StopWatch;

import javax.net.ssl.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class WebServer {
    static {
        System.setProperty("org.jboss.logging.provider", "slf4j");  
    }
    private static final Log logger = LogFactory.getLog(WebServer.class);

    private RequestHandler requestHandler;
    private MainHandler mainHandler;
    private GracefulShutdownHandler shutdownHandler;
    private Integer httpPort;
    private Integer httpsPort;
    
    private Set<Integer> usedPorts;

    private Undertow server;
    private final ApplicationContext context;

    private ServerConfig serverConfig;
    private SecurityConfig securityConfig;
    private ApplicationConfig applicationConfig;
    protected List<Initializer<WebServer>> initializers = new ArrayList<>();

    public WebServer(ApplicationContext context) {
        this.context = context;
        Ready.post(new GeneralEvent(Event.WEB_SERVER_CREATE, this));
    }

    public void addInitializer(Initializer<WebServer> initializer) {
        this.initializers.add(initializer);
        initializers.sort(Comparator.comparing(Initializer::order));
    }

    public void startInit() {
        try {
            for (Initializer<WebServer> i : initializers) {
                i.startInit(this);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void endInit() {
        try {
            for (Initializer<WebServer> i : initializers) {
                i.endInit(this);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void initHandlers() {
        applicationConfig = Ready.getApplicationConfig(context.application.getName());
        serverConfig = applicationConfig.getServer();
        securityConfig = applicationConfig.getSecurity();
        shutdownHandler = new GracefulShutdownHandler();
        requestHandler = new RequestHandler(context);
        mainHandler = new MainHandler(context);
        Ready.post(new GeneralEvent(Event.WEB_SERVER_AFTER_HANDLER_INIT, this));
    }

    public void start() {

        var watch = new StopWatch();
        try {

            initHandlers();

            if (!serverConfig.isEnableHttp() && !serverConfig.isEnableHttps()) {
                throw new Error("Unable to start the server as both http and https are disabled");
            }
            if (serverConfig.isDynamicPort()) {
                if (serverConfig.getMinPort() > serverConfig.getMaxPort()) {
                    String errMessage = "No ports available to bind to - the minPort is larger than the maxPort in ServerConfig";
                    logger.error(errMessage);
                    throw new Error(errMessage);
                }

                int capacity = serverConfig.getMaxPort() - serverConfig.getMinPort();
                usedPorts = new HashSet<>(capacity);
                while(usedPorts.size() < capacity) {
                    int randomPort = ThreadLocalRandom.current().nextInt(serverConfig.getMinPort(), serverConfig.getMaxPort());
                    
                    if(usedPorts.contains(randomPort) || usedPorts.contains(randomPort + 1)) continue;
                    boolean b = bind(randomPort, randomPort + 1);
                    if (b) {
                        usedPorts = null;
                        break;
                    } else {
                        usedPorts.add(randomPort);usedPorts.add(randomPort + 1);
                    }
                }
            } else {
                httpPort = serverConfig.isEnableHttp() ? serverConfig.getHttpPort() : null;
                httpsPort = serverConfig.isEnableHttps() ? serverConfig.getHttpsPort() : null;
                if(httpPort == null && httpsPort == null) throw new Error("Unable to start the server as httpPort/httpsPort are incorrect");
                bind(-1, -1);
            }
        } finally {
            logger.info("Ready for work, httpPort=%s, httpsPort=%s, gzip=%s, elapsed=%ss", httpPort, httpsPort, serverConfig.isEnableGzip(), watch.elapsedSeconds());
        }
    }

    private boolean bind(int httpPort, int httpsPort) {
        String serverIp = serverConfig.getIp();
        try {
            Undertow.Builder builder = Undertow.builder();

            if (serverConfig.isEnableHttps()) {
                httpsPort = httpsPort < 0 ? this.httpsPort : httpsPort;
                SSLContext sslContext = createSSLContext(); 
                builder.addHttpsListener(httpsPort, serverIp, sslContext);
            } else {
                httpsPort = -1;
            }
            if (serverConfig.isEnableHttp()) {
                httpPort = httpPort < 0 ? this.httpPort : httpPort;
                builder.addHttpListener(httpPort, serverIp);
            } else {
                httpPort = -1;
            }

            builder.setServerOption(UndertowOptions.ENABLE_HTTP2, true);

            if (securityConfig.isEnableTwoWayTls()) {
                builder.setSocketOption(Options.SSL_CLIENT_AUTH_MODE, SslClientAuthMode.REQUIRED);
            }

            serverConfigEnsure();

            server = builder.setBufferSize(serverConfig.getBufferSize()).setIoThreads(serverConfig.getIoThreads())

                    .setSocketOption(Options.BACKLOG, serverConfig.getBacklog())
                    .setServerOption(UndertowOptions.ENABLE_RFC6265_COOKIE_VALIDATION, Boolean.TRUE)
                    .setServerOption(UndertowOptions.ALWAYS_SET_KEEP_ALIVE, false)

                    .setServerOption(UndertowOptions.ALWAYS_SET_DATE, serverConfig.isAlwaysSetDate())
                    .setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, false)
                    
                    .setServerOption(UndertowOptions.ALLOW_UNESCAPED_CHARACTERS_IN_URL, serverConfig.isAllowUnescapedCharactersInUrl())

                    .setServerOption(UndertowOptions.NO_REQUEST_TIMEOUT, 620 * 1000)     
                    .setServerOption(UndertowOptions.SHUTDOWN_TIMEOUT, 10 * 1000)        
                    .setServerOption(UndertowOptions.MAX_ENTITY_SIZE, serverConfig.getMaxEntitySize())

                    .setSocketOption(Options.SSL_ENABLED_PROTOCOLS, Sequence.of("TLSv1.2"))
                    .setHandler(new SetHeaderHandler(handler(), Map.of(Headers.SERVER_STRING, serverConfig.getServerString(), "Application", context.application.getName())))
                    .setWorkerThreads(serverConfig.getWorkerThreads()).build();

            server.start();
        } catch (Exception e) {
            if (!serverConfig.isDynamicPort() || usedPorts.size() >= (serverConfig.getMaxPort() - serverConfig.getMinPort())) {
                String triedPortsMessage = serverConfig.isDynamicPort() ? serverConfig.getMinPort() + " to " + (serverConfig.getMaxPort()) : this.httpPort + (this.httpsPort == null ? "" : " and " + this.httpsPort);
                String errMessage = "No ports available to bind to. Tried: " + triedPortsMessage;
                logger.error(errMessage);
                throw new RuntimeException(errMessage, e);
            }
            String failedBindMsg = "Failed to bind";
            String tryingMsg = "";
            if(serverConfig.isEnableHttp()){
                failedBindMsg += " http to port " + httpPort + ",";
                tryingMsg += " trying http on port " + ++httpPort + " ...";
            }
            if(serverConfig.isEnableHttps()){
                failedBindMsg += " https to port " + httpsPort + ",";
                tryingMsg += " trying https on port " + ++httpsPort + " ...";
            }
            failedBindMsg += tryingMsg;
            if (logger.isInfoEnabled())
                logger.info(failedBindMsg);
            return false;
        }

        this.httpPort = httpPort;
        this.httpsPort = httpsPort;

        if (serverConfig.isEnableHttp()) {
            if (logger.isInfoEnabled()) {
                logger.info("Http Server started on ip:" + serverIp + " Port:" + httpPort);
            } else {
                System.out.println("Http Server started on ip:" + serverIp + " Port:" + httpPort);
            }
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("Http port disabled.");
            } else {
                System.out.println("Http port disabled.");
            }
        }
        if (serverConfig.isEnableHttps()) {
            if (logger.isInfoEnabled()) {
                logger.info("Https Server started on ip:" + serverIp + " Port:" + httpsPort);
            } else {
                System.out.println("Https Server started on ip:" + serverIp + " Port:" + httpsPort);
            }
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("Https port disabled.");
            } else {
                System.out.println("Https port disabled.");
            }
        }

        Ready.post(new GeneralEvent(Event.WEB_SERVER_STARTED, this));
        return true;
    }

    private void serverConfigEnsure() {
        ServerConfigEnsure.confirm(serverConfig);
    }

    public Application getApplication(){
        return context.application;
    }

    public Integer getHttpPort() {
        return httpPort;
    }

    public Integer getHttpsPort() {
        return httpsPort;
    }

    public RequestHandler getRequestHandler() {
        return requestHandler;
    }

    public GracefulShutdownHandler getShutdownHandler() {
        return shutdownHandler;
    }

    private HttpHandler handler() {
        HttpHandler handler = mainHandler;
        if (serverConfig.isEnableGzip()) {
            
            handler = new EncodingHandler(handler, new ContentEncodingRepository()
                .addEncodingHandler("gzip", new GzipEncodingProvider(serverConfig.getGzipLevel()), 100, new GZipPredicate()));
        }

        if (serverConfig.isEnableHttps()) {
            if (serverConfig.isHttpToHttps()) {
                handler = new HttpToHttpsHandler(handler);
            } else {
                if (!serverConfig.isEnableHttp()) {
                    handler = new HttpDisableHandler(handler);
                }
            }
        } else {
            if (serverConfig.isHttpToHttps()) {
                System.err.println("http redirect to https needs ssl support");
            }
        }

        return handler;
    }

    public void shutdown() {
        if (server != null) {
            logger.info("shutting down http server");
            Ready.post(new GeneralEvent(Event.WEB_SERVER_BEFORE_SHUTDOWN, this));
            shutdownHandler.shutdown();
            if (requestHandler.getWebSocketHandler() != null) {
                requestHandler.getWebSocketHandler().shutdown();
            }
        }
    }

    public void awaitRequestCompletion(long timeoutInMs) throws InterruptedException {
        if (server != null) {
            boolean success = shutdownHandler.awaitTermination(timeoutInMs);
            if (!success) {
                logger.warn("Failed to stop: failed to wait active http requests to complete");
                server.getWorker().shutdownNow();
            } else {
                logger.info("active http requests completed");
            }
        }
    }

    public void awaitTermination() {
        if (server != null) {
            server.stop();
            logger.info("http server stopped");
            Ready.post(new GeneralEvent(Event.WEB_SERVER_AFTER_SHUTDOWN, this));
        }
    }

    int getIoThreadCount(){
        return server.getWorker().getIoThreadCount();
    }

    protected KeyStore loadKeyStore() {
        String name = securityConfig.getServerKeystoreName();
        String pass = securityConfig.getServerKeystorePass();
        return NetUtil.loadKeyStore(name, pass.toCharArray());
    }

    protected KeyStore loadTrustStore() {
        String name = securityConfig.getServerTruststoreName();
        String pass = securityConfig.getServerTruststorePass();
        return NetUtil.loadTrustStore(name, pass.toCharArray());
    }

    private TrustManager[] buildTrustManagers(final KeyStore trustStore) {
        TrustManager[] trustManagers = null;
        if (trustStore != null) {
            try {
                TrustManagerFactory trustManagerFactory = TrustManagerFactory
                        .getInstance(KeyManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(trustStore);
                trustManagers = trustManagerFactory.getTrustManagers();
            } catch (NoSuchAlgorithmException | KeyStoreException e) {
                logger.error(e,"Unable to initialise TrustManager[]");
                throw new RuntimeException("Unable to initialise TrustManager[]", e);
            }
        } else {
            
            trustManagers = new X509TrustManager[]{new TrustAllTrustManager()};
        }
        return trustManagers;
    }

    private KeyManager[] buildKeyManagers(final KeyStore keyStore, char[] keyPass) {
        KeyManager[] keyManagers;
        try {
            KeyManagerFactory keyManagerFactory = KeyManagerFactory
                    .getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyPass);
            keyManagers = keyManagerFactory.getKeyManagers();
        } catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException e) {
            logger.error(e,"Unable to initialise KeyManager[]");
            throw new RuntimeException("Unable to initialise KeyManager[]", e);
        }
        return keyManagers;
    }

    private SSLContext createSSLContext() throws RuntimeException {

        try {
            String keyPass = securityConfig.getServerKeyPass();
            KeyManager[] keyManagers = buildKeyManagers(loadKeyStore(), keyPass.toCharArray());
            TrustManager[] trustManagers;
            if (securityConfig.isEnableTwoWayTls()) {
                trustManagers = buildTrustManagers(loadTrustStore());
            } else {
                trustManagers = buildTrustManagers(null);
            }

            SSLContext sslContext;
            sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(keyManagers, trustManagers, null);
            return sslContext;
        } catch (Exception e) {
            logger.error(e,"Unable to create SSLContext");
            throw new RuntimeException("Unable to create SSLContext", e);
        }
    }
}
