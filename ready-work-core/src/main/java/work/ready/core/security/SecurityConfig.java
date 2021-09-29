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

package work.ready.core.security;

import work.ready.core.config.BaseConfig;

import java.util.Map;

public class SecurityConfig extends BaseConfig {

    private LimiterConfig limiter = new LimiterConfig();
    
    private String serverKeystoreName;
    private String serverKeystorePass;
    private String serverKeyPass;
    private boolean enableTwoWayTls = false;
    private String serverTruststoreName;
    private String serverTruststorePass;

    private String clientKeystorePass;
    private String clientKeyPass;
    private String clientTruststorePass;

    private String emailPassword;

    private String issuer;
    private String audience;
    private String version;
    private int expiredInMinutes;
    private Key key;
    private String providerId;
    private String jwtPrivateKeyPassword;
    private Map<String, Object> certificate;
    private int clockSkewInSeconds = 60;
    private boolean enableJwtCache = true;
    private boolean bootstrapFromKeyService = false;
    private String keyResolver = "X509Certificate"; 

    public LimiterConfig getLimiter() {
        return limiter;
    }

    public SecurityConfig setLimiter(LimiterConfig limiter) {
        this.limiter = limiter;
        return this;
    }

    public String getServerKeystoreName() {
        return serverKeystoreName;
    }

    public SecurityConfig setServerKeystoreName(String serverKeystoreName) {
        this.serverKeystoreName = serverKeystoreName;
        return this;
    }

    public String getServerTruststoreName() {
        return serverTruststoreName;
    }

    public SecurityConfig setServerTruststoreName(String serverTruststoreName) {
        this.serverTruststoreName = serverTruststoreName;
        return this;
    }

    public String getServerKeystorePass() {
        return serverKeystorePass;
    }

    public SecurityConfig setServerKeystorePass(String serverKeystorePass) {
        this.serverKeystorePass = serverKeystorePass;
        return this;
    }

    public String getServerKeyPass() {
        return serverKeyPass;
    }

    public SecurityConfig setServerKeyPass(String serverKeyPass) {
        this.serverKeyPass = serverKeyPass;
        return this;
    }

    public String getServerTruststorePass() {
        return serverTruststorePass;
    }

    public SecurityConfig setServerTruststorePass(String serverTruststorePass) {
        this.serverTruststorePass = serverTruststorePass;
        return this;
    }

    public boolean isEnableTwoWayTls() {
        return enableTwoWayTls;
    }

    public SecurityConfig setEnableTwoWayTls(boolean enableTwoWayTls) {
        this.enableTwoWayTls = enableTwoWayTls;
        return this;
    }

    public String getClientKeystorePass() {
        return clientKeystorePass;
    }

    public SecurityConfig setClientKeystorePass(String clientKeystorePass) {
        this.clientKeystorePass = clientKeystorePass;
        return this;
    }

    public String getClientKeyPass() {
        return clientKeyPass;
    }

    public SecurityConfig setClientKeyPass(String clientKeyPass) {
        this.clientKeyPass = clientKeyPass;
        return this;
    }

    public String getClientTruststorePass() {
        return clientTruststorePass;
    }

    public SecurityConfig setClientTruststorePass(String clientTruststorePass) {
        this.clientTruststorePass = clientTruststorePass;
        return this;
    }

    public String getEmailPassword() {
        return emailPassword;
    }

    public SecurityConfig setEmailPassword(String emailPassword) {
        this.emailPassword = emailPassword;
        return this;
    }

    public String getJwtPrivateKeyPassword() {
        return jwtPrivateKeyPassword;
    }

    public SecurityConfig setJwtPrivateKeyPassword(String jwtPrivateKeyPassword) {
        this.jwtPrivateKeyPassword = jwtPrivateKeyPassword;
        return this;
    }

    public String getIssuer() {
        return issuer;
    }

    public SecurityConfig setIssuer(String issuer) {
        this.issuer = issuer;
        return this;
    }

    public String getAudience() {
        return audience;
    }

    public SecurityConfig setAudience(String audience) {
        this.audience = audience;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public SecurityConfig setVersion(String version) {
        this.version = version;
        return this;
    }

    public int getExpiredInMinutes() {
        return expiredInMinutes;
    }

    public SecurityConfig setExpiredInMinutes(int expiredInMinutes) {
        this.expiredInMinutes = expiredInMinutes;
        return this;
    }

    public Key getKey() {
        return key;
    }

    public SecurityConfig setKey(Key key) {
        this.key = key;
        return this;
    }

    public String getProviderId() { return providerId; }

    public SecurityConfig setProviderId(String providerId) {
        this.providerId = providerId;
        return this;
    }

    public Map<String, Object> getCertificate() {
        return certificate;
    }

    public SecurityConfig setCertificate(Map<String, Object> certificate) {
        this.certificate = certificate;
        return this;
    }

    public int getClockSkewInSeconds() {
        return clockSkewInSeconds;
    }

    public void setClockSkewInSeconds(int clockSkewInSeconds) {
        this.clockSkewInSeconds = clockSkewInSeconds;
    }

    public boolean isEnableJwtCache() {
        return enableJwtCache;
    }

    public void setEnableJwtCache(boolean enableJwtCache) {
        this.enableJwtCache = enableJwtCache;
    }

    public boolean isBootstrapFromKeyService() {
        return bootstrapFromKeyService;
    }

    public void setBootstrapFromKeyService(boolean bootstrapFromKeyService) {
        this.bootstrapFromKeyService = bootstrapFromKeyService;
    }

    public String getKeyResolver() {
        return keyResolver;
    }

    public void setKeyResolver(String keyResolver) {
        this.keyResolver = keyResolver;
    }

    @Override
    public void validate() {

    }

    public class Key {
        String kid;
        String filename;
        String password;
        String keyName;

        public Key() {
        }

        public String getKid() {
            return kid;
        }

        public void setKid(String kid) {
            this.kid = kid;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getKeyName() {
            return keyName;
        }

        public void setKeyName(String keyName) {
            this.keyName = keyName;
        }

        public boolean validate() {
            return (kid != null && filename != null && keyName != null);
        }
    }
}
