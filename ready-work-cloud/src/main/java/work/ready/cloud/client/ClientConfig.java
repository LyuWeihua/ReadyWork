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

import work.ready.cloud.client.oauth.config.OauthConfig;
import work.ready.core.config.BaseConfig;

import java.util.Map;

public class ClientConfig extends BaseConfig {
    public static final String SCOPE = "scope";
    public static final String CSRF = "csrf";
    public static final String SERVICE_ID = "serviceId";
    public static final String SAML_BEARER = "saml_bearer";
    public static final String REDIRECT_URI = "redirect_uri";
    public static final String AUTHORIZATION_CODE = "authorization_code";
    public static final String CLIENT_CREDENTIALS = "client_credentials";
    public static final String REFRESH_TOKEN = "refresh_token";

    public static final int DEFAULT_RETRY = 0;
    public static final int DEFAULT_ERROR_THRESHOLD = 3;
    public static final int DEFAULT_TIMEOUT = 6000; 
    public static final int DEFAULT_KEEP_ALIVE_TIMEOUT = 1200; 
    public static final int DEFAULT_RESET_TIMEOUT = 300000; 
    public static final int DEFAULT_CONNECTION_POOL_SIZE = 1000;

    private String proxy;
    private int resetTimeout = DEFAULT_RESET_TIMEOUT;
    private int timeout = DEFAULT_TIMEOUT;
    private int retry = DEFAULT_RETRY;
    private int keepAliveTimeout = DEFAULT_KEEP_ALIVE_TIMEOUT;
    private int errorThreshold = DEFAULT_ERROR_THRESHOLD;
    private int connectionPoolSize = DEFAULT_CONNECTION_POOL_SIZE;
    private String userAgent = CloudClient.DEFAULT_USER_AGENT;

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public int getResetTimeout() {
        return resetTimeout;
    }

    public void setResetTimeout(int resetTimeout) {
        this.resetTimeout = resetTimeout;
    }

    public int getRetry() {
        return retry;
    }

    public void setRetry(int retry) {
        this.retry = retry;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getKeepAliveTimeout() {
        return keepAliveTimeout;
    }

    public void setKeepAliveTimeout(int keepAliveTimeout) {
        this.keepAliveTimeout = keepAliveTimeout;
    }

    public int getErrorThreshold() {
        return errorThreshold;
    }

    public void setErrorThreshold(int errorThreshold) {
        this.errorThreshold = errorThreshold;
    }

    public int getConnectionPoolSize() {
        return connectionPoolSize;
    }

    public void setConnectionPoolSize(int connectionPoolSize) {
        this.connectionPoolSize = connectionPoolSize;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    private TlsConfig tls = new TlsConfig();

    private OauthConfig oauth = new OauthConfig();

    public TlsConfig getTls() {
        return tls;
    }

    public void setTls(TlsConfig tls) {
        this.tls = tls;
    }

    public OauthConfig getOauth() {
        return oauth;
    }

    public void setOauth(OauthConfig oauth) {
        this.oauth = oauth;
    }

    @Override
    public void validate() {
    }

    public class TlsConfig {

        private boolean verifyHostname;

        private String defaultGroupKey;

        private Map<String, String> trustedNames;

        private boolean loadTrustStore;

        private String trustStore;

        private String trustStorePass;

        private boolean loadKeyStore;

        private String keyStore;

        private String keyStorePass;

        private String keyPass;

        public boolean isVerifyHostname() {
            return verifyHostname;
        }

        public void setVerifyHostname(boolean verifyHostname) {
            this.verifyHostname = verifyHostname;
        }

        public String getDefaultGroupKey() {
            return defaultGroupKey;
        }

        public void setDefaultGroupKey(String defaultGroupKey) {
            this.defaultGroupKey = defaultGroupKey;
        }

        public Map<String, String> getTrustedNames() {
            return trustedNames;
        }

        public void setTrustedNames(Map<String, String> trustedNames) {
            this.trustedNames = trustedNames;
        }

        public boolean isLoadTrustStore() {
            return loadTrustStore;
        }

        public void setLoadTrustStore(boolean loadTrustStore) {
            this.loadTrustStore = loadTrustStore;
        }

        public String getTrustStore() {
            return trustStore;
        }

        public void setTrustStore(String trustStore) {
            this.trustStore = trustStore;
        }

        public String getTrustStorePass() {
            return trustStorePass;
        }

        public void setTrustStorePass(String trustStorePass) {
            this.trustStorePass = trustStorePass;
        }

        public boolean isLoadKeyStore() {
            return loadKeyStore;
        }

        public void setLoadKeyStore(boolean loadKeyStore) {
            this.loadKeyStore = loadKeyStore;
        }

        public String getKeyStore() {
            return keyStore;
        }

        public void setKeyStore(String keyStore) {
            this.keyStore = keyStore;
        }

        public String getKeyStorePass() {
            return keyStorePass;
        }

        public void setKeyStorePass(String keyStorePass) {
            this.keyStorePass = keyStorePass;
        }

        public String getKeyPass() {
            return keyPass;
        }

        public void setKeyPass(String keyPass) {
            this.keyPass = keyPass;
        }
    }

}
