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
package work.ready.cloud.config.source;

import work.ready.cloud.ReadyCloud;
import work.ready.cloud.config.Application;
import work.ready.cloud.config.ApplicationConfigs;
import work.ready.core.exception.ApiException;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.security.HttpAuth;
import work.ready.core.server.Constant;
import work.ready.core.server.Ready;
import work.ready.core.service.status.Status;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LocalFileSystemSource implements ConfigFileSource {
    private static final Log logger = LogFactory.getLog(LocalFileSystemSource.class);

    private String token;

    @Override
    public String login(String authorization) throws ApiException {
        HttpAuth httpAuth = new HttpAuth(authorization);
        String clientId = "";
        String secret = "";
        if(httpAuth.isValid()){
            clientId = httpAuth.getClientId();
            secret = httpAuth.getClientSecret();
        }

        String rightClientId = ReadyCloud.getConfig().getConfigServer().getClientId();
        String rightSecret = ReadyCloud.getConfig().getConfigServer().getClientSecret();
        if(rightClientId == null && rightSecret == null){
            token = "";
        } else {
            if(clientId.equals(rightClientId) && secret.equals(rightSecret)){
                token = UUID.randomUUID().toString();
            }
        }
        return token;
    }

    private void checkToken(String token){
        String INVALID_AUTH_TOKEN = "ERROR10000";
        if(!this.token.equals(token)) throw new ApiException(new Status(INVALID_AUTH_TOKEN));
    }

    @Override
    public ApplicationConfigs getApplicationConfigs(String authToken, Application application) throws ApiException {
        checkToken(authToken);
        ApplicationConfigs applicationConfigs = new ApplicationConfigs();
        applicationConfigs.setConfigProperties(new HashMap<String, Object>());
        applicationConfigs.setApplication(application);
        String configPath = null;
        Map<String, Object> configsMap = null;

        if(application.getApplicationName() != null && application.getApplicationVersion() != null) {
            
            configPath = buildConfigPath(application, CONFIG, application.getApplicationName(), application.getApplicationVersion());
            configsMap = Ready.config().loadRawMapConfig(Constant.VALUES_CONFIG_NAME, configPath);
            if (configsMap != null) {
                ((Map<String, Object>) applicationConfigs.getConfigProperties()).putAll(configsMap);
            }
        } else {
            
            configPath = buildConfigPath(application, CONFIG, GLOBAL, application.getProjectVersion());
            configsMap = Ready.config().loadRawMapConfig(Constant.VALUES_CONFIG_NAME, configPath);
            if (configsMap != null) {
                ((Map<String, Object>) applicationConfigs.getConfigProperties()).putAll(configsMap);
            }
        }
        return applicationConfigs;
    }

    @Override
    public ApplicationConfigs getApplicationCertificates(String authToken, Application application) throws ApiException {
        checkToken(authToken);
        ApplicationConfigs applicationConfigs = new ApplicationConfigs();
        applicationConfigs.setApplication(application);
        applicationConfigs.setConfigProperties(new HashMap<String, Object>());
        String configPath = null;
        Map<String, Object> certsMap = null;
        if(application.getApplicationName() != null && application.getApplicationVersion() != null) {
            
            configPath = buildConfigPath(application, CERT, application.getApplicationName(), application.getApplicationVersion());
            certsMap = getFiles(configPath);
            if (certsMap != null) {
                ((Map<String, Object>) applicationConfigs.getConfigProperties()).putAll(certsMap);
            }
        } else {
            
            configPath = buildConfigPath(application, CERT, GLOBAL, application.getProjectVersion());
            certsMap = getFiles(configPath);
            if (certsMap != null) {
                ((Map<String, Object>) applicationConfigs.getConfigProperties()).putAll(certsMap);
            }
        }
        return applicationConfigs;
    }

    @Override
    public ApplicationConfigs getApplicationFiles(String authToken, Application application) throws ApiException {
        checkToken(authToken);
        ApplicationConfigs applicationConfigs = new ApplicationConfigs();
        applicationConfigs.setApplication(application);
        applicationConfigs.setConfigProperties(new HashMap<String, Object>());
        String configPath = null;
        Map<String, Object> filesMap = null;

        if(application.getApplicationName() != null && application.getApplicationVersion() != null) {
            
            configPath = buildConfigPath(application, FILE, application.getApplicationName(), application.getApplicationVersion());
            filesMap = getFiles(configPath);
            if (filesMap != null) {
                ((Map<String, Object>) applicationConfigs.getConfigProperties()).putAll(filesMap);
            }
        } else {
            
            configPath = buildConfigPath(application, FILE, GLOBAL, application.getProjectVersion());
            filesMap = getFiles(configPath);
            if (filesMap != null) {
                ((Map<String, Object>) applicationConfigs.getConfigProperties()).putAll(filesMap);
            }
        }
        return applicationConfigs;
    }

    private String buildConfigPath(Application application, String configType, String name, String version) {
        String configsDir = Ready.getProperty(Constant.READY_CONFIG_LOCAL_REPOSITORY_PROPERTY);
        if(configsDir == null){
            String value = ReadyCloud.getConfig().getConfigServer().getConfigRepository();
            if(value == null) value = File.separator + Constant.DEFAULT_CONFIG_REPOSITORY_DIR + File.separator;
            if (value.startsWith("@")) {
                value = value.substring(1);
            } else {
                value = Ready.root() + value;
            }
            configsDir = value;
        }

        StringBuffer configPath = new StringBuffer(configsDir);
        if(!configsDir.endsWith(File.separator)) configPath.append(File.separator);
        configPath.append(configType)
                .append(File.separator).append(application.getProjectName())
                .append(File.separator).append(name)
                .append(File.separator).append(version)
                .append(File.separator).append(application.getProfile());
        return configPath.toString();
    }

    private Map<String, Object> getFiles(String filesPath) {
        Map<String, Object> configsMap = new HashMap<>();
        try {
            Files.list(Paths.get(filesPath)).filter(Files::isRegularFile).forEach(file -> {
                String fileName = file.getFileName().toString();
                byte[] content = new byte[0];
                try {
                    if (Files.size(file) > 1024*1024*2) { 
                        logger.error("The size of file: " + fileName + " is exceeded 2M, the maximum size of a config file is 2M.");
                        return;
                    } else {
                        content = Files.readAllBytes(file);
                    }
                } catch (IOException e) {
                    logger.error("Exception while reading file: " + fileName);
                }
                if (content != null) {
                    String encodedContent = Base64.getMimeEncoder().encodeToString(content);
                    configsMap.put(fileName, encodedContent);
                }
            });
        } catch (IOException e) {
            logger.error("Exception while reading files from configs directory: " + filesPath);
        }
        return configsMap;
    }
}
