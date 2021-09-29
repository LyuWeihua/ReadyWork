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
import work.ready.cloud.client.oauth.config.TokenConfig;

public class AuthorizationCodeRequest extends TokenRequest {

    private String authCode;
    private String redirectUri;

    public AuthorizationCodeRequest() {
        setGrantType(ClientConfig.AUTHORIZATION_CODE);
        TokenConfig tokenConfig = null;
        tokenConfig = ReadyCloud.getConfig().getHttpClient().getOauth().getToken();
        if(tokenConfig != null) {
            setServerUrl(tokenConfig.getServerUrl());
            setServiceId(tokenConfig.getServiceId());
            if(tokenConfig.getAuthorizationCode() != null) {
                setClientId(tokenConfig.getAuthorizationCode().getClientId());
                setClientSecret(tokenConfig.getAuthorizationCode().getClientSecret());
                setUri(tokenConfig.getAuthorizationCode().getUri());
                setScope(tokenConfig.getAuthorizationCode().getScope());
                setRedirectUri(tokenConfig.getAuthorizationCode().getRedirectUri());
            }
        }
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }
}
