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

package work.ready.cloud.client.oauth.config;

import java.util.List;

public class TokenConfig {

    private CacheConfig cache = new CacheConfig();
    private int tokenRenewBeforeExpired;
    private int expiredRefreshRetryDelay;
    private int earlyRefreshRetryDelay;
    private String serverUrl;
    private String serviceId;
    private AuthorizationCodeConfig authorizationCode = new AuthorizationCodeConfig();
    private ClientCredentialsConfig clientCredentials = new ClientCredentialsConfig();
    private RefreshTokenConfig refreshToken = new RefreshTokenConfig();
    private KeyConfig key = new KeyConfig();

    public CacheConfig getCache() {
        return cache;
    }

    public void setCache(CacheConfig cache) {
        this.cache = cache;
    }

    public int getTokenRenewBeforeExpired() {
        return tokenRenewBeforeExpired;
    }

    public void setTokenRenewBeforeExpired(int tokenRenewBeforeExpired) {
        this.tokenRenewBeforeExpired = tokenRenewBeforeExpired;
    }

    public int getExpiredRefreshRetryDelay() {
        return expiredRefreshRetryDelay;
    }

    public void setExpiredRefreshRetryDelay(int expiredRefreshRetryDelay) {
        this.expiredRefreshRetryDelay = expiredRefreshRetryDelay;
    }

    public int getEarlyRefreshRetryDelay() {
        return earlyRefreshRetryDelay;
    }

    public void setEarlyRefreshRetryDelay(int earlyRefreshRetryDelay) {
        this.earlyRefreshRetryDelay = earlyRefreshRetryDelay;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public AuthorizationCodeConfig getAuthorizationCode() {
        return authorizationCode;
    }

    public void setAuthorizationCode(AuthorizationCodeConfig authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    public ClientCredentialsConfig getClientCredentials() {
        return clientCredentials;
    }

    public void setClientCredentials(ClientCredentialsConfig clientCredentials) {
        this.clientCredentials = clientCredentials;
    }

    public RefreshTokenConfig getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(RefreshTokenConfig refreshToken) {
        this.refreshToken = refreshToken;
    }

    public KeyConfig getKey() {
        return key;
    }

    public void setKey(KeyConfig key) {
        this.key = key;
    }

    public class RefreshTokenConfig {
        private String uri;
        private String clientId;
        private String clientSecret;
        private List<String> scope;

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public List<String> getScope() {
            return scope;
        }

        public void setScope(List<String> scope) {
            this.scope = scope;
        }
    }

    public class ClientCredentialsConfig {
        private String uri;
        private String clientId;
        private String clientSecret;
        private List<String> scope;

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public List<String> getScope() {
            return scope;
        }

        public void setScope(List<String> scope) {
            this.scope = scope;
        }
    }

    public class AuthorizationCodeConfig {
        private String uri;
        private String clientId;
        private String clientSecret;
        private String redirectUri;
        private List<String> scope;

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }

        public List<String> getScope() {
            return scope;
        }

        public void setScope(List<String> scope) {
            this.scope = scope;
        }
    }

    public class CacheConfig {
        private int capacity;

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }
    }
}
