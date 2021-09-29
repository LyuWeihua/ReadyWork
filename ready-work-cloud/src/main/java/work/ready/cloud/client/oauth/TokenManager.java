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
import work.ready.cloud.client.ClientConfig;
import work.ready.cloud.client.oauth.cache.CacheStrategy;
import work.ready.cloud.client.oauth.cache.LongestExpireCacheStrategy;
import work.ready.core.service.result.Result;

import java.net.http.HttpRequest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class TokenManager {
    private static volatile TokenManager INSTANCE;
    private static int CAPACITY = 200;

    private CacheStrategy cacheStrategy;

    private TokenManager() {
        var capacity = ReadyCloud.getConfig().getHttpClient().getOauth().getToken().getCache().getCapacity();
        if(capacity > 0) CAPACITY = capacity;
        cacheStrategy = new LongestExpireCacheStrategy(CAPACITY);
    }

    public static TokenManager getInstance() {
        if(INSTANCE == null) {
            synchronized (TokenManager.class) {
                if(INSTANCE == null) {
                    INSTANCE = new TokenManager();
                }
            }
        }
        return INSTANCE;
    }

    public Result<Jwt> getJwt(Jwt.Key key) {
        Jwt cachedJwt = getJwt(cacheStrategy, key);

        Result<Jwt> result = OauthHelper.populateCCToken(cachedJwt);
        
        if (result.isSuccess()) {
            cacheStrategy.cacheJwt(key, result.getResult());
        }
        return result;
    }

    private synchronized Jwt getJwt(CacheStrategy cacheStrategy, Jwt.Key key) {
        Jwt result = cacheStrategy.getCachedJwt(key);
        if(result == null) {
            
            result = new Jwt(key);
            cacheStrategy.cacheJwt(key, result);
        }
        return result;
    }

    public Result<Jwt> getJwt(HttpRequest clientRequest) {
        Optional<String> scope = clientRequest.headers().firstValue(ClientConfig.SCOPE);
        if(scope.isPresent()) {
            Set<String> scopeSet = new HashSet<>();
            scopeSet.addAll(Arrays.asList(scope.get().split(" ")));
            return getJwt(new Jwt.Key(scopeSet));
        }
        Optional<String> serviceId = clientRequest.headers().firstValue(ClientConfig.SERVICE_ID);
        if(serviceId.isPresent()) {
            return getJwt(new Jwt.Key(serviceId.get()));
        }
        return getJwt(new Jwt.Key());
    }
}
