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
package work.ready.cloud.client.oauth;

import work.ready.cloud.ReadyCloud;
import work.ready.cloud.client.oauth.config.TokenConfig;
import work.ready.core.tools.StrUtil;

import java.util.*;

public class Jwt {

    protected Set<String> scopes = new HashSet<>();
    protected Key key;
    
    private String jwt;

    private long expire;
    private volatile boolean renewing = false;
    private volatile long expiredRetryTimeout;
    private volatile long earlyRetryTimeout;
    private static long tokenRenewBeforeExpired;
    private static long expiredRefreshRetryDelay;
    private static long earlyRefreshRetryDelay;

    public Jwt() {
        TokenConfig config = ReadyCloud.getConfig().getHttpClient().getOauth().getToken();
        if(config != null) {
            tokenRenewBeforeExpired = config.getTokenRenewBeforeExpired();
            expiredRefreshRetryDelay = config.getExpiredRefreshRetryDelay();
            earlyRefreshRetryDelay = config.getEarlyRefreshRetryDelay();
        }
    }

    public Jwt(Key key) {
        this();
        this.key = key;
    }

    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }

    public long getExpire() {
        return expire;
    }

    public void setExpire(long expire) {
        this.expire = expire;
    }

    public boolean isRenewing() {
        return renewing;
    }

    public void setRenewing(boolean renewing) {
        this.renewing = renewing;
    }

    public long getExpiredRetryTimeout() {
        return expiredRetryTimeout;
    }

    public void setExpiredRetryTimeout(long expiredRetryTimeout) {
        this.expiredRetryTimeout = expiredRetryTimeout;
    }

    public long getEarlyRetryTimeout() {
        return earlyRetryTimeout;
    }

    public void setEarlyRetryTimeout(long earlyRetryTimeout) {
        this.earlyRetryTimeout = earlyRetryTimeout;
    }

    public static long getTokenRenewBeforeExpired() {
        return tokenRenewBeforeExpired;
    }

    public static void setTokenRenewBeforeExpired(long tokenRenewBeforeExpired) {
        Jwt.tokenRenewBeforeExpired = tokenRenewBeforeExpired;
    }

    public static long getExpiredRefreshRetryDelay() {
        return expiredRefreshRetryDelay;
    }

    public static void setExpiredRefreshRetryDelay(long expiredRefreshRetryDelay) {
        Jwt.expiredRefreshRetryDelay = expiredRefreshRetryDelay;
    }

    public static long getEarlyRefreshRetryDelay() {
        return earlyRefreshRetryDelay;
    }

    public static void setEarlyRefreshRetryDelay(long earlyRefreshRetryDelay) {
        Jwt.earlyRefreshRetryDelay = earlyRefreshRetryDelay;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
    }

    public void setScopes(String scopesStr) {
        this.scopes = this.scopes == null ? new HashSet() : this.scopes;
        if(StrUtil.notBlank(scopesStr)) {
            scopes.addAll(Arrays.asList(scopesStr.split("(\\s)+")));
        }
    }

    public Key getKey() {
        return key;
    }

    public static class Key {
        
        protected Set<String> scopes;
        
        protected String serviceId;

        @Override
        public int hashCode() {
            return Objects.hash(scopes, serviceId);
        }

        @Override
        public boolean equals(Object obj) {
            return hashCode() == obj.hashCode();
        }

        public Key(Set<String> scopes) {
            this.scopes = scopes;
        }

        public Key(String serviceId) {
            this.serviceId = serviceId;
        }

        public Key() {
            this.scopes = new HashSet<>();
        }

        public Set<String> getScopes() {
            return scopes;
        }

        public String getServiceId() {
            return serviceId;
        }
    }
}
