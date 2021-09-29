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

package work.ready.core.config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import work.ready.core.component.decrypt.AESDecryptor;
import work.ready.core.component.decrypt.Decryptor;
import work.ready.core.ioc.annotation.ScopeType;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.ApplicationConfig;
import work.ready.core.server.Constant;
import work.ready.core.server.Ready;
import work.ready.core.tools.StrUtil;

import static java.nio.charset.StandardCharsets.UTF_8;
import static work.ready.core.component.cache.BaseCache.unwrapNull;
import static work.ready.core.component.cache.BaseCache.wrapNull;

public class Config {
    private static final Log logger = LogFactory.getLog(Config.class);

    private List<String> CONFIG_PATH = new CopyOnWriteArrayList<>();
    private Path mainConfigPath;

    static final String CONFIG_EXT_YAML = ".yaml";
    static final String CONFIG_EXT_YML = ".yml";
    static final String[] configExtensionsOrdered = {CONFIG_EXT_YML, CONFIG_EXT_YAML};
    public static final Pattern CRYPT_PATTERN = Pattern.compile("^CRYPT:([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$");

    private long cacheExpirationTime = 0L;

    private Map<String, String> argMap;

    private final Map<String, Object> configCache = new ConcurrentHashMap<>(10, 0.9f, 1);
    private final Map<String, BaseConfig> configs = new HashMap<>();

    private final static ObjectMapper jsonMapper = new ObjectMapper();
    
    private final static ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    
    private static ObjectMapper xmlMapper;
    
    private static Decryptor decryptor;

    static {
        
        ServiceLoader<Decryptor> decryptors = ServiceLoader.load(Decryptor.class);
        decryptor = decryptors.findFirst().orElse(new AESDecryptor());
        yamlMapper.registerModule(new JavaTimeModule());
        yamlMapper.registerModule(new DecryptModule(decryptor));
        yamlMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        jsonMapper.registerModule(new JavaTimeModule());
        jsonMapper.registerModule(new DecryptModule(decryptor));
        jsonMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
        jsonMapper.registerModule(simpleModule);
    }

    public Config() {
        locateConfigRepository();
    }

    public ObjectMapper getYamlMapper() {
        return yamlMapper;
    }

    public ObjectMapper getJsonMapper() {
        return jsonMapper;
    }

    public ObjectMapper getXmlMapper(){
        if(xmlMapper == null) {
            xmlMapper = new XmlMapper();
            xmlMapper.registerModule(new DecryptModule(decryptor));
            xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            xmlMapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
            xmlMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            xmlMapper.setPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE);
            xmlMapper.enable(MapperFeature.USE_STD_BEAN_NAMING);
        }
        return xmlMapper;
    }

    public void clear() {
        configCache.clear();
    }

    public void resetApplicationConfig() {
        String prefix = Constant.APPLICATION_CONFIG_NAME + "-MergedObject";
        configCache.entrySet().removeIf(stringObjectEntry -> stringObjectEntry.getKey().startsWith(prefix));
    }

    public void putInConfigCache(String configName, Object config) {
        configCache.put(configName, config);
    }

    public synchronized void mergeIntoConfigCache(String configName, Map<String, Object> config){
        if(unwrapNull(configCache.get(configName)) != null) {
            ConfigMerge.mergeConfigMap((Map) configCache.get(configName), config);
        } else {
            configCache.put(configName, config);
        }
    }

    public Path getMainConfigPath() {
        return mainConfigPath;
    }

    public void addConfigPath(String path) {
        if(Files.isDirectory(Path.of(path))) {
            CONFIG_PATH.add(path);
        } else {
            logger.warn("config path is not valid, ignored path=%s", path);
        }
    }

    private void locateConfigRepository() {
        mainConfigPath = locateConfigRepository(false);
        if(mainConfigPath != null) {
            CONFIG_PATH.clear();
            CONFIG_PATH.add(mainConfigPath.toString());
        }
    }

    public void reLocateConfigRepository() {
        mainConfigPath = locateConfigRepository(true);
        if(mainConfigPath != null) {
            CONFIG_PATH.clear();
            CONFIG_PATH.add(mainConfigPath.toString());
        }
    }

    private Path locateConfigRepository(boolean selfInitialized) {
        String value = Ready.getProperty(Constant.READY_WORK_CONFIG_DIR_PROPERTY);
        if (value != null) {
            Path path = Paths.get(value).toAbsolutePath();
            if (Files.isDirectory(path)) {
                logger.info("found -D%s as config path, path=%s", Constant.READY_WORK_CONFIG_DIR_PROPERTY, path);
                return path;
            } else {
                logger.warn("found -D%s as config path, but could not access, path=%s", Constant.READY_WORK_CONFIG_DIR_PROPERTY, path);
            }
        }
        if(selfInitialized) {
            value = getBootstrapConfig().getConfigPath();
            if (value != null) {
                if (value.startsWith("@")) {
                    value = value.substring(1);
                } else {
                    value = Ready.root().resolve(value).toAbsolutePath().toString();
                }
                Path path = Paths.get(value).toAbsolutePath();
                if (Files.isDirectory(path)) {
                    logger.info("found configPath in bootstrap config as config path, path=%s", path);
                    return path;
                } else {
                    logger.warn("found configPath in bootstrap config, but could not access, path=%s", path);
                }
            }

            if (Ready.rootExist()) {
                value = Ready.root().resolve(Constant.DEFAULT_CONFIG_FILE_DIR).toAbsolutePath().toString();
                Path path = Paths.get(value).toAbsolutePath();
                if (Files.isDirectory(path)) {
                    logger.info("found config directory in root path, path=%s", path);
                    return path;
                } else {
                    logger.warn("try to locate config directory in root path, but could not access, path=%s", path);
                }
            }
        }
        return null;
    }

    public void parseCmdArgs(String[] args) {
        if (args == null || args.length == 0) {
            return;
        }

        for (String arg : args) {
            int indexOf = arg.indexOf("=");
            if (arg.startsWith("--") && indexOf > 0) {
                String key = arg.substring(2, indexOf);
                String value = arg.substring(indexOf + 1);
                setCmdArg(key, value);
            }
        }
    }

    public void setCmdArg(String key, Object value) {
        if (argMap == null) {
            argMap = new HashMap<>();
        }
        argMap.put(key, value.toString());
    }

    public String getCmdArg(String key) {
        if (argMap == null) return null;
        return argMap.get(key);
    }

    public Map<String, String> getCmdArgs() {
        return argMap;
    }

    public String getStringFromFile(String filename, String path) {
        Object content = configCache.get(filename);
        if (content == null) {
            synchronized (Config.class) {
                content = configCache.get(filename);
                if (content == null) {
                    content = loadStringFromFile(filename, path);
                    configCache.put(filename, wrapNull(content));
                }
            }
        }
        return (String)unwrapNull(content);
    }

    public ApplicationConfig getBootstrapConfig(){
        String cacheName = Constant.BOOTSTRAP_CONFIG_NAME + "-MergedObject";
        Object config = configCache.get(cacheName);
        if(config == null) {
            synchronized (Config.class) {
                config = configCache.get(cacheName);
                if(config == null) {
                    Map<String, Object> bootstrapConfig = getBootstrapConfigMap();
                    if (bootstrapConfig == null || bootstrapConfig.get("readyWork") == null) {
                        if(bootstrapConfig != null && !bootstrapConfig.isEmpty()) logger.warn("readyWork section is missing in the bootstrap config");
                        config = new ApplicationConfig(); 
                        
                        configCache.put(Constant.BOOTSTRAP_CONFIG_NAME + "-original", new ApplicationConfig());
                    } else {
                        config = convertItemToObject(bootstrapConfig.get("readyWork"), ApplicationConfig.class);
                        ((ApplicationConfig)config).validate();
                        
                        configCache.put(Constant.BOOTSTRAP_CONFIG_NAME + "-original", convertItemToObject(bootstrapConfig.get("readyWork"), ApplicationConfig.class));
                    }
                    configCache.put(cacheName, wrapNull(config));
                }
            }
        }
        return (ApplicationConfig)unwrapNull(config);
    }

    public ApplicationConfig getApplicationConfig(String appName){
        String cacheName = Constant.APPLICATION_CONFIG_NAME + "-MergedObject" + "-" + appName;
        Object config = configCache.get(cacheName);
        if(config == null) {
            synchronized (Config.class) {
                config = configCache.get(cacheName);
                if(config == null) {
                    Map<String, Object> applicationConfig = getApplicationConfigMap(appName);
                    if(applicationConfig == null || applicationConfig.get("readyWork") == null) {
                        if(applicationConfig != null && !applicationConfig.isEmpty()) logger.warn("readyWork section is missing in the '%s' application config", appName);
                        config = new ApplicationConfig(); 
                    } else {
                        config = convertItemToObject(applicationConfig.get("readyWork"), ApplicationConfig.class);
                        ((ApplicationConfig)config).validate();
                    }
                    
                    Object original = configCache.get(Constant.BOOTSTRAP_CONFIG_NAME + "-original");
                    if(original != null) {
                        ConfigMerge.compareAndMergeConfig(config, original, getBootstrapConfig());
                    }
                    configCache.put(cacheName, wrapNull(config));
                }
            }
        }
        return (ApplicationConfig)unwrapNull(config);
    }

    protected Map<String, Object> getBootstrapConfigMap(){
        String cacheName = Constant.BOOTSTRAP_CONFIG_NAME + "-MergedMap";
        Object bootstrapConfig = configCache.get(cacheName);
        if(bootstrapConfig == null) {
            synchronized (Config.class) {
                bootstrapConfig = configCache.get(cacheName);
                if(bootstrapConfig == null) {
                    if(getMapConfig(Constant.BOOTSTRAP_CONFIG_NAME) == null) {
                        bootstrapConfig = new LinkedHashMap<String, Object>();
                    } else {
                        bootstrapConfig = new LinkedHashMap<String, Object>();
                        ConfigMerge.deepCopy((Map<String, Object>)bootstrapConfig, getMapConfig(Constant.BOOTSTRAP_CONFIG_NAME));
                    }
                    String activeProfile = getActiveProfile((Map<String, Object>)bootstrapConfig);
                    if (StrUtil.notBlank(activeProfile)) {
                        Map<String, Object> profileConfig = getMapConfig(Constant.BOOTSTRAP_CONFIG_NAME + "-" + activeProfile);
                        if (profileConfig != null && profileConfig.get("readyWork") != null && Map.class.isAssignableFrom(profileConfig.get("readyWork").getClass())) {
                            Map<String, Object> readyConfig = (Map<String, Object>) profileConfig.get("readyWork");
                            if (readyConfig.get("profiles") != null && !activeProfile.equals(readyConfig.get("profiles"))) {
                                if(logger.isWarnEnabled())
                                logger.warn(Constant.BOOTSTRAP_CONFIG_NAME + "-" + activeProfile + " config with promiscuous profiles: " + readyConfig.get("profiles") + ".");
                            }
                            readyConfig.remove("profiles");
                            readyConfig.remove("activeProfile"); 
                        }
                        ConfigMerge.mergeConfigMap((Map<String, Object>)bootstrapConfig, profileConfig);
                    }
                    configCache.put(cacheName, wrapNull(bootstrapConfig));
                }
            }
        }
        return (Map<String, Object>)unwrapNull(bootstrapConfig);
    }

    protected Map<String, Object> getApplicationConfigMap(String appName) {
        if(StrUtil.isBlank(appName)) return getApplicationConfigMap();
        String cacheName = Constant.APPLICATION_CONFIG_NAME + "-MergedMap" + "-" + appName;
        Object extendedAppConfig = configCache.get(cacheName);
        if(extendedAppConfig == null) {
            synchronized (Config.class) {
                extendedAppConfig = configCache.get(cacheName);
                if (extendedAppConfig == null) {
                    extendedAppConfig = new LinkedHashMap<>();
                    ConfigMerge.deepCopy((Map<String, Object>)extendedAppConfig, getApplicationConfigMap());
                    String activeProfile = getActiveProfile((Map<String, Object>)extendedAppConfig);
                    Map<String, Object> appConfig = null;
                    if(getMapConfig(appName) != null) {
                        appConfig = new LinkedHashMap<>();
                        ConfigMerge.deepCopy(appConfig, getMapConfig(appName));
                    }
                    if (appConfig != null && appConfig.get("readyWork") != null && Map.class.isAssignableFrom(appConfig.get("readyWork").getClass())) {
                        Map<String, Object> readyConfig = (Map<String, Object>) appConfig.get("readyWork");
                        if(StrUtil.notBlank(activeProfile)) {
                            if (readyConfig.get("profiles") != null && !activeProfile.equals(readyConfig.get("profiles"))) {
                                if (logger.isWarnEnabled())
                                    logger.warn(appName + " config with promiscuous profiles: " + readyConfig.get("profiles") + ".");
                            }
                            readyConfig.remove("profiles");
                            readyConfig.remove("activeProfile"); 
                        } else {
                            activeProfile = getActiveProfile(appConfig);
                        }
                    }
                    if (StrUtil.notBlank(activeProfile)) {
                        Map<String, Object> profileConfig = getMapConfig(appName + "-" + activeProfile);
                        if (profileConfig != null && profileConfig.get("readyWork") != null && Map.class.isAssignableFrom(profileConfig.get("readyWork").getClass())) {
                            Map<String, Object> readyConfig = (Map<String, Object>) profileConfig.get("readyWork");
                            if (readyConfig.get("profiles") != null && !activeProfile.equals(readyConfig.get("profiles"))) {
                                if(logger.isWarnEnabled())
                                    logger.warn(appName + "-" + activeProfile + " config with promiscuous profiles: " + readyConfig.get("profiles") + ".");
                            }
                            readyConfig.remove("profiles");
                            readyConfig.remove("activeProfile"); 
                        }
                        if(profileConfig != null){
                            if(appConfig != null) {
                                ConfigMerge.mergeConfigMap(appConfig, profileConfig);
                            } else {
                                appConfig = profileConfig;
                            }
                        }
                    }
                    if(appConfig != null)
                    ConfigMerge.mergeConfigMap((Map<String, Object>)extendedAppConfig, appConfig);
                    configCache.put(cacheName, wrapNull(extendedAppConfig));
                }
            }
        }
        return (Map<String, Object>)unwrapNull(extendedAppConfig);
    }

    public ApplicationConfig getApplicationConfig(){
        String cacheName = Constant.APPLICATION_CONFIG_NAME + "-MergedObject";
        Object config = configCache.get(cacheName);
        if(config == null) {
            synchronized (Config.class) {
                config = configCache.get(cacheName);
                if(config == null) {
                    Map<String, Object> applicationConfig = getApplicationConfigMap();
                    if(applicationConfig == null || applicationConfig.get("readyWork") == null) {
                        if(applicationConfig != null && !applicationConfig.isEmpty()) logger.warn("readyWork section is missing in the application config");
                        config = new ApplicationConfig(); 
                    } else {
                        config = convertItemToObject(applicationConfig.get("readyWork"), ApplicationConfig.class);
                        ((ApplicationConfig)config).validate();
                    }
                    
                    Object original = configCache.get(Constant.BOOTSTRAP_CONFIG_NAME + "-original");
                    if(original != null) {
                        ConfigMerge.compareAndMergeConfig(config, original, getBootstrapConfig());
                    }
                    configCache.put(cacheName, wrapNull(config));
                }
            }
        }
        return (ApplicationConfig)unwrapNull(config);
    }

    private Map<String, Object> getApplicationConfigMap(){
        String cacheName = Constant.APPLICATION_CONFIG_NAME + "-MergedMap";
        Object applicationConfig = configCache.get(cacheName);
        if(applicationConfig == null) {
            synchronized (Config.class) {
                applicationConfig = configCache.get(cacheName);
                if(applicationConfig == null) {
                    Map<String, Object> bootstrapConfig = getBootstrapConfigMap();
                    String activeProfile = getActiveProfile(bootstrapConfig);
                    if(getMapConfig(Constant.APPLICATION_CONFIG_NAME) != null) {
                        applicationConfig = new LinkedHashMap<>();
                        ConfigMerge.deepCopy((Map<String, Object>)applicationConfig, getMapConfig(Constant.APPLICATION_CONFIG_NAME));
                    }
                    if(StrUtil.isBlank(activeProfile) && applicationConfig != null) activeProfile = getActiveProfile((Map<String, Object>)applicationConfig);
                    if (StrUtil.notBlank(activeProfile)) {
                        Map<String, Object> profileConfig = getMapConfig(Constant.APPLICATION_CONFIG_NAME + "-" + activeProfile);
                        if (profileConfig != null && profileConfig.get("readyWork") != null && Map.class.isAssignableFrom(profileConfig.get("readyWork").getClass())) {
                            Map<String, Object> readyConfig = (Map<String, Object>) profileConfig.get("readyWork");
                            if (readyConfig.get("profiles") != null && !activeProfile.equals(readyConfig.get("profiles"))) {
                                if(logger.isWarnEnabled())
                                logger.warn(Constant.APPLICATION_CONFIG_NAME + "-" + activeProfile + " config with promiscuous profiles: " + readyConfig.get("profiles") + ".");
                            }
                            readyConfig.remove("profiles");
                            readyConfig.remove("activeProfile"); 
                        }
                        if(profileConfig != null) {
                            if(applicationConfig != null)
                                ConfigMerge.mergeConfigMap((Map<String, Object>)applicationConfig, profileConfig);
                            else applicationConfig = profileConfig;
                        }
                    }
                    if (applicationConfig != null) {
                        if(bootstrapConfig != null)
                            ConfigMerge.mergeConfigMap((Map<String, Object>)applicationConfig, bootstrapConfig, true);
                        configCache.put(cacheName, wrapNull(applicationConfig));
                    } else {
                        if(bootstrapConfig != null) {
                            applicationConfig = new LinkedHashMap<>();
                            ConfigMerge.deepCopy((Map<String, Object>)applicationConfig, bootstrapConfig);
                        }
                        configCache.put(cacheName, wrapNull(applicationConfig));
                    }
                }
            }
        }
        return (Map<String, Object>)unwrapNull(applicationConfig);
    }

    private String getActiveProfile(Map<String, Object> config){
        String activeProfile = null;
        if(config != null && config.get("readyWork") != null && Map.class.isAssignableFrom(config.get("readyWork").getClass())) {
            Map<String, Object> readyConfig = (Map<String, Object>) config.get("readyWork");
            if (readyConfig.get("profiles") != null && Map.class.isAssignableFrom(readyConfig.get("profiles").getClass())) {
                Map<String, Object> profiles = (Map<String, Object>) readyConfig.get("profiles");
                activeProfile = profiles.get("active") != null ? profiles.get("active").toString() : null;
                readyConfig.put("activeProfile", activeProfile);
            }
            readyConfig.remove("profiles");
            activeProfile = (String)readyConfig.get("activeProfile");
        }
        return activeProfile;
    }

    public Map<String, Object> getStatusConfig(){
        String cacheName = Constant.STATUS_CONFIG_NAME + "-MergedMap";
        Object statusConfig = configCache.get(cacheName);
        if(statusConfig == null) {
            synchronized (Config.class) {
                statusConfig = configCache.get(cacheName);
                if(statusConfig == null) {
                    statusConfig = getMapConfig(Constant.STATUS_CONFIG_NAME);
                    if(statusConfig == null) {
                        statusConfig = new HashMap<>();
                    }
                    configCache.put(cacheName, statusConfig);
                }
            }
        }
        return (Map<String, Object>)statusConfig;
    }

    public void addStatusConfig(String extStatusConfig) {
        String cacheName = Constant.STATUS_CONFIG_NAME + "-MergedMap";
        Map<String, Object> statusConfig = getStatusConfig();
        Map<String, Object> extConfig = getMapConfig(extStatusConfig);
        if (extConfig != null) {
            ConfigMerge.mergeStatusConfig(statusConfig, extConfig);
            configCache.put(cacheName, statusConfig);
        }
    }

    public String getStringFromFile(String filename) {
        return getStringFromFile(filename, "");
    }

    public InputStream getInputStreamFromFile(String filename) {
        return getConfigStream(filename, "");
    }

    public <T> T getObjectConfig(String configName, Class<T> clazz, String path) {
        Object config = configCache.get(configName);
        if (config == null) {
            synchronized (Config.class) {
                config = configCache.get(configName);
                if (config == null) {
                    config = loadObjectConfig(configName, clazz, path);
                    configCache.put(configName, wrapNull(config));
                }
            }
        }
        return (T)unwrapNull(config);
    }

    public Object getObjectConfig(String configName, Class clazz) {
        return getObjectConfig(configName, clazz, "");
    }

    public Map<String, Object> getMapConfig(String configName, String path) {
        Object config = configCache.get(configName);
        if (config == null) {
            synchronized (Config.class) {
                config = configCache.get(configName);
                if (config == null) {
                    config = loadMapConfig(configName, path);
                    configCache.put(configName, wrapNull(config));
                }
            }
        }
        return (Map<String, Object>)unwrapNull(config);
    }

    public Map<String, Object> getMapConfig(String configName) {
        return getMapConfig(configName, "");
    }

    public Map<String, Object> getMapConfigNoCache(String configName, String path) {
        return loadMapConfig(configName, path);
    }

    public Map<String, Object> getMapConfigNoCache(String configName) {
        return getMapConfigNoCache(configName, "");
    }

    private String loadStringFromFile(String filename, String path) {
        String content = null;
        InputStream inStream = null;
        try {
            inStream = getConfigStream(filename, path);
            if (inStream != null) {
                content = convertStreamToString(inStream);
            }
        } catch (Exception ioe) {
            logger.error(ioe, "Exception");
        } finally {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException ioe) {
                    logger.error(ioe, "IOException");
                }
            }
        }
        return content;
    }

    private <T> Object loadConfigFileAsObject(String configName, String fileExtension, Class<T> clazz, String path) {
        Object config = null;
        String fileName = configName + fileExtension;
        try (InputStream inStream = getConfigStream(fileName, path)) {
            if (inStream != null) {
                Map<String, Object> configMap = yamlMapper.readValue(inStream, Map.class);
                ConfigMerge.mergeMap(configName, configMap);
                config = convertItemToObject(configMap, clazz);
            }
        } catch (Exception e) {
            logger.error(e, "Exception");
            throw new RuntimeException("Unable to load " + fileName + " as object.", e);
        }
        return config;
    }

    private <T> Object loadObjectConfig(String configName, Class<T> clazz, String path) {
        Object config;
        for (String extension : configExtensionsOrdered) {
            config = loadConfigFileAsObject(configName, extension, clazz, path);
            if (config != null) return config;
        }
        return null;
    }

    private Map<String, Object> loadConfigFileAsMap(String configName, String fileExtension, String path) {
        Map<String, Object> config = null;
        String ymlFilename = configName + fileExtension;
        try (InputStream inStream = getConfigStream(ymlFilename, path)) {
            if (inStream != null) {
                config = yamlMapper.readValue(inStream, Map.class);
                ConfigMerge.mergeMap(configName, config);
            }
        } catch (Exception e) {
            logger.error(e, "Exception");
            throw new RuntimeException("Unable to load " + ymlFilename + " as map.", e);
        }
        return config;
    }

    private Map<String, Object> loadMapConfig(String configName, String path) {
        Map<String, Object> config;
        for (String extension : configExtensionsOrdered) {
            config = loadConfigFileAsMap(configName, extension, path);
            if (config != null) return config;
        }
        return null;
    }

    public Map<String, Object> loadRawMapConfig(String configName, String path){
        Map<String, Object> config;
        if(!path.endsWith(File.separator)) path += File.separator;
        for (String extension : configExtensionsOrdered) {
            String ymlFilename = path + configName + extension;
            try (InputStream inStream = new FileInputStream(ymlFilename)) {
                if (inStream != null) {
                    config = yamlMapper.readValue(inStream, Map.class);
                    return config;
                }
            } catch (FileNotFoundException ex) {
            } catch (Exception e) {
                logger.error(e, "Exception");
                throw new RuntimeException("Unable to load " + ymlFilename + " as map.", e);
            }
        }
        return null;
    }

    private InputStream getConfigStream(String configFilename, String path) {
        InputStream inStream = null;
        String configFileDir = null;
        for (int i = 0; i < CONFIG_PATH.size(); i++) {
            String absolutePath = getAbsolutePath(path, i);
            try {
                inStream = new FileInputStream(absolutePath + "/" + configFilename);
                configFileDir = absolutePath;
            } catch (FileNotFoundException ex) {

            }
            
            if (path.startsWith("/")) break;
        }
        if (inStream != null) {
            if (logger.isInfoEnabled()) {
                logger.info("Config loaded from external folder for " + configFilename + " in " + configFileDir);
            }
            return inStream;
        }
        inStream = Ready.getClassLoader().getResourceAsStream(configFilename);
        if (inStream != null) {
            if (logger.isInfoEnabled()) {
                logger.info("config loaded from classpath for " + configFilename);
            }
            return inStream;
        }
        inStream = Ready.getClassLoader().getResourceAsStream(Constant.DEFAULT_CONFIG_FILE_DIR + "/" + configFilename);
        if (inStream != null) {
            if (logger.isInfoEnabled()) {
                logger.info("Config loaded from default folder for " + configFilename);
            }
            return inStream;
        }
        if(configFilename.endsWith(configExtensionsOrdered[configExtensionsOrdered.length-1])){
            logger.debug("Unable to load config '" + configFilename.substring(0, configFilename.indexOf(".")) + "' with extension " + Arrays.asList(configExtensionsOrdered).toString() + ". Please ignore this message if you are sure that your application is not using this config file.");
        }
        return null;
    }

    private static long getNextMidNightTime() {
        Calendar cal = new GregorianCalendar();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DAY_OF_MONTH, 1);
        return cal.getTimeInMillis();
    }

    private String getAbsolutePath(String path, int index) {
        if (path.startsWith("@")) { 
            return path.substring(1);
        } else {
            return path.equals("") ? CONFIG_PATH.get(index).trim() : CONFIG_PATH.get(index).trim() + "/" + path;
        }
    }

    public static <T> T convertItemToObject(Object item, Class<T> clazz) {
        try {
            return jsonMapper.convertValue(item, clazz);
        } catch (IllegalArgumentException e){
            if(e.getCause() instanceof InvalidFormatException){
                throw new RuntimeException(e); 
            } else
            if(e.getCause() instanceof UnrecognizedPropertyException){
                throw new RuntimeException(e); 
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    static InputStream convertStringToStream(String string) {
        return new ByteArrayInputStream(string.getBytes(UTF_8));
    }

    static class DecryptModule extends SimpleModule {
        public DecryptModule(Decryptor decryptor) {
            addDeserializer(String.class, new StdScalarDeserializer<String>(String.class) {
                @Override
                public String deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
                        JsonProcessingException {
                    if(CRYPT_PATTERN.matcher(jp.getValueAsString()).matches()){
                        return decryptor.decrypt(jp.getValueAsString());
                    }
                    return jp.getValueAsString();
                }
            });
        }
    }

    public <T extends BaseConfig> T getConfig(Class<T> configClass){
        return getConfig(configClass, null, null);
    }

    public <T extends BaseConfig> T getConfig(Class<T> configClass, String appName){
        return getConfig(configClass, appName, null);
    }

    public <T extends BaseConfig> T getConfig(Class<T> configClass, String appName, String name) {
        String key = configClass.getCanonicalName() + ":" + appName + ":" + name;
        T configObject = (T) configs.get(key);
        if (configObject == null) {
            try {
                if(ApplicationConfig.class.equals(configClass)) {
                    ApplicationConfig appConfig = StrUtil.notBlank(appName) ?
                            getApplicationConfig(appName) : getApplicationConfig();
                    configObject = (T) appConfig;
                } else {

                    configObject = Ready.beanManager().get(configClass, true, null, ScopeType.prototype);
                }
                configs.put(key, configObject);
            } catch (Exception e) {
                throw new Error(e);
            }
        }
        return configObject;
    }

    public <T extends BaseConfig> Config addConfig(T configObject, String appName, String name){
        String key = configObject.getClass().getCanonicalName() + ":" + appName + ":" + name;
        T config = (T) configs.get(key);
        if (config == null) {
            configs.put(key, configObject);
        } else {
            throw new Error("Configuration object of '" + key + "' is already exist.");
        }
        return this;
    }
}

