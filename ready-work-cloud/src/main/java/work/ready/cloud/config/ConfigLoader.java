/**
 *
 * Original work copyright (c) 2016 Network New Technologies Inc.
 * Modified work Copyright (c) 2020 WeiHua Lyu [ready.work]
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
package work.ready.cloud.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.util.Headers;
import work.ready.cloud.ReadyCloud;
import work.ready.cloud.client.CloudClient;
import work.ready.core.config.Config;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.module.Application;
import work.ready.core.security.HttpAuth;
import work.ready.core.server.Constant;
import work.ready.core.server.Ready;
import work.ready.core.tools.StrUtil;
import work.ready.core.tools.define.LambdaFinal;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static work.ready.core.server.Ready.getBootstrapConfig;

public class ConfigLoader {
    private static final Log logger = LogFactory.getLog(ConfigLoader.class);

    public static final String CONFIG_SERVER_CONFIG_PATH = "/config-server/config";
    public static final String CONFIG_SERVER_CERT_PATH = "/config-server/cert";
    public static final String CONFIG_SERVER_FILE_PATH = "/config-server/file";
    public static final String AUTHORIZATION = "ready.config_server_authorization";

    private Path targetConfigsDirectory;
    private String configServerUri;
    private String authorization = Ready.getProperty(AUTHORIZATION);

    private final static ObjectMapper mapper = Ready.config().getYamlMapper();
    
    private static final CloudClient client;
    static {
        String trustStoreFile = ReadyCloud.getConfig().getHttpClient().getTls().getTrustStore();
        String trustStorePassword = ReadyCloud.getConfig().getHttpClient().getTls().getTrustStorePass();
        if (StrUtil.notBlank(trustStoreFile) && StrUtil.notBlank(trustStorePassword)) {
            client = CloudClient.getInstance(createTlsContext());
        } else {
            client = CloudClient.getTrustAllInstance();
        }
    }

    public ConfigLoader(String configServer, String clientId, String clientSecret){
        configServerUri = configServer == null ? null : configServer.endsWith("/") ? configServer.substring(0, configServer.length() - 1) : configServer;
        if(authorization == null && clientId != null && clientSecret != null)
            authorization = HttpAuth.getBasicAuthHeader(clientId, clientSecret);
    }

    public void fetchGlobalFiles(){
        fetchFiles(null);
    }

    public void fetchFiles(Class<? extends Application> app) {
        if (StrUtil.notBlank(configServerUri)) {
            logger.info("Fetching %s files from config server", app == null ? "global" : Application.getApplicationName(app));
            targetConfigsDirectory = locateConfigRepository();

            String configPath = getConfigServerPath(app);

            fetchingFiles(configPath, CONFIG_SERVER_CERT_PATH);

            fetchingFiles(configPath, CONFIG_SERVER_FILE_PATH);

        } else {
            logger.warn("config server is not provided, using local configs");
        }
    }

    public void fetchGlobalConfigs(){
        fetchConfigs(null);
    }

    public void fetchConfigs(Class<? extends Application> app) {
        if (StrUtil.notBlank(configServerUri)) {
            logger.info("Fetching %s configs from config server", app == null ? "global" : Application.getApplicationName(app));
            targetConfigsDirectory = locateConfigRepository();

            String configPath = getConfigServerPath(app);

            fetchingConfigs(configPath);
        } else {
            logger.warn("config server is not provided, using local configs");
        }
    }

    private Path locateConfigRepository() {
        String value = Ready.getProperty(Constant.READY_WORK_CONFIG_DIR_PROPERTY);
        Path path = null;
        if (value != null) {
            path = Paths.get(value).toAbsolutePath();
            if (!Files.exists(path)) {
                try {
                    Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxr-xr-x");
                    FileAttribute<Set<PosixFilePermission>> attrs = PosixFilePermissions.asFileAttribute(perms);
                    path = Files.createDirectories(path, attrs);
                } catch (Exception e) {
                    throw new RuntimeException("found -DREADY_configPath as config path, but could not create it, path=" + path);
                }
            }
        }
        value = getBootstrapConfig().getConfigPath();
        if (value != null) {
            if (value.startsWith("@")) {
                value = value.substring(1);
            } else {
                value = Ready.root() + File.separator + value;
            }
            path = Paths.get(value).toAbsolutePath();
            if (!Files.exists(path)) {
                try {
                    Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxr-xr-x");
                    FileAttribute<Set<PosixFilePermission>> attrs = PosixFilePermissions.asFileAttribute(perms);
                    path = Files.createDirectories(path, attrs);
                } catch (Exception e){
                    throw new RuntimeException("found configPath in bootstrap config, but could not create it, path=" + path);
                }
            }
        }
        if (Ready.rootExist()) {
            path = Ready.root().resolve(Constant.DEFAULT_CONFIG_FILE_DIR).toAbsolutePath();
            if (!Files.exists(path)) {
                try {
                    Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxr-xr-x");
                    FileAttribute<Set<PosixFilePermission>> attrs = PosixFilePermissions.asFileAttribute(perms);
                    path = Files.createDirectories(path, attrs);
                } catch (Exception e){
                    throw new RuntimeException("try to locate config path, but could not create it, path=" + path);
                }
            }
        }
        if(path == null) {
            throw new RuntimeException("Can not locate config directory");
        }
        if(!Files.isWritable(path)){
            throw new RuntimeException("found config directory, but it's not writable, path=" + path);
        }
        return path;
    }

    private void fetchingConfigs(String configPath) {
        
        String configServerConfigsPath = CONFIG_SERVER_CONFIG_PATH + configPath;
        
        Map<String, Object> applicationConfigs = getApplicationConfigs(configServerConfigsPath);

        applicationConfigs.put("activeProfile", Ready.getBootstrapConfig().getActiveProfile());
        logger.debug("application configs received from Config Server: %s", applicationConfigs);

        try {
            
            applicationConfigs = Ready.config().getYamlMapper().readValue(Ready.config().getJsonMapper().writeValueAsString(applicationConfigs), new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            logger.warn(e,"Json parse exception during loading configs from config server: ");
        }

        Ready.config().mergeIntoConfigCache(Constant.VALUES_CONFIG_NAME, applicationConfigs);
    }

    private void fetchingFiles(String configPath, String contextRoot) {
        
        String configServerFilesPath = contextRoot + configPath;
        
        Map<String, Object> applicationFiles = getApplicationConfigs(configServerFilesPath);
        logger.debug("%s files fetched from config sever.", applicationFiles.size());
        logger.debug("loadFiles: %s", applicationFiles);
        try {
            Path filePath = targetConfigsDirectory;
            if (!Files.exists(filePath)) {
                Files.createDirectories(filePath);
                logger.info("target configs directory created: %s", targetConfigsDirectory);
            }
            Base64.Decoder decoder = Base64.getMimeDecoder();
            for (String fileName : applicationFiles.keySet()) {
                filePath=Paths.get(targetConfigsDirectory+"/"+fileName);
                Files.write(filePath, decoder.decode(applicationFiles.get(fileName).toString().getBytes()));
            }
        }  catch (IOException e) {
            logger.error(e,"Exception while creating %s dir or creating files:", targetConfigsDirectory);
        }
    }

    public static String getConfigServerHealth(String host) {
        String result = null;
        try {
            HttpResponse<String> response = client.withTimeout(1000).send(
                    HttpRequest.newBuilder(new URI(host).resolve(ConfigServerModule.healthCheck)).
                            header(Headers.USER_AGENT_STRING, CloudClient.DEFAULT_USER_AGENT));
            if (response.statusCode() == 200) {
                result = response.body();
            }
        } catch (Exception e) {
            
        }
        return result;
    }

    private Map<String, Object> getApplicationConfigs(String configServerPath) {
        LambdaFinal<Map<String, Object>> configs = new LambdaFinal<>(new HashMap<>());

        logger.debug("Calling Config Server endpoint: %s%s", configServerUri, configServerPath);

        client.withTimeout(5000)
                .getAsync(configServerUri + configServerPath,
                        authorization == null ? null : new HashMap<>(Map.of(CloudClient.AUTHORIZATION_FIELD, authorization)),
                        (response)->{
                            int statusCode = response.statusCode();
                            String body = response.body();
                            if (statusCode >= 300 || StrUtil.isBlank(body)) {
                                throw new RuntimeException("Failed to fetch configs from config server: " + statusCode + " : " + body);
                            } else {
                                try {
                                    Map<String, Object> responseMap = mapper.readValue(body, new TypeReference<Map<String, Object>>() {});
                                    configs.set((Map<String, Object>) responseMap.get("configProperties"));
                                } catch (JsonProcessingException e) {
                                    throw new RuntimeException("Failed to parse configs which fetched from config server : " + body);
                                }
                            }
                        },
                        (e)-> {
                            logger.error(e,"Call config server failed: ");
                            return null;
                        });

        return configs.get();
    }

    private String getConfigServerPath(Class<? extends Application> appClass) {
        StringBuilder configPath = new StringBuilder();
        configPath.append("/").append(Ready.getBootstrapConfig().getProject());
        configPath.append("/").append(Ready.getBootstrapConfig().getVersion());
        if(appClass != null) {
            configPath.append("/").append(Application.getApplicationName(appClass));
            configPath.append("/").append(Application.getApplicationVersion(appClass));
        }
        configPath.append("/").append(Ready.getBootstrapConfig().getActiveProfile());
        logger.debug("configPath: %s", configPath);
        return configPath.toString();
    }

    private static KeyStore loadTrustStore(){
        String trustStoreFile = ReadyCloud.getConfig().getHttpClient().getTls().getTrustStore();
        String trustStorePassword = ReadyCloud.getConfig().getHttpClient().getTls().getTrustStorePass();

        try (InputStream stream = Ready.config().getInputStreamFromFile(trustStoreFile)) {
            if (stream == null) {
                String message = "Unable to load truststore '" + trustStoreFile + "', please provide the correct truststore to enable TLS connection.";
                if (logger.isErrorEnabled()) {
                    logger.error(message);
                }
                throw new RuntimeException(message);
            }
            KeyStore loadedKeystore = KeyStore.getInstance("PKCS12");
            loadedKeystore.load(stream, trustStorePassword != null ? trustStorePassword.toCharArray() : null);
            return loadedKeystore;
        } catch (Exception e) {
            logger.error(e,"Unable to load truststore: " + trustStoreFile);
            throw new RuntimeException("Unable to load truststore: " + trustStoreFile, e);
        }
    }

    private static TrustManager[] buildTrustManagers(final KeyStore trustStore) {
        TrustManager[] trustManagers = null;
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory
                    .getInstance(KeyManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);
            trustManagers = trustManagerFactory.getTrustManagers();
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            logger.error(e,"Unable to initialise TrustManager[]");
            throw new RuntimeException("Unable to initialise TrustManager[]", e);
        }
        return trustManagers;
    }

    private static SSLContext createTlsContext() throws RuntimeException {
        SSLContext sslContext = null;
        try {
            TrustManager[] trustManagers = buildTrustManagers(loadTrustStore());
            sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, trustManagers, null);
        } catch (Exception e) {
            logger.error(e,"Unable to create SSLContext");
            throw new RuntimeException("Unable to create SSLContext", e);
        }
        return sslContext;
    }
}
