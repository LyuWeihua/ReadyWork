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
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

public class SAMLBearerRequest extends TokenRequest {

    static final String CLIENT_ASSERTION_TYPE_KEY = "client_assertion_type";
    static final String CLIENT_ASSERTION_TYPE_VALUE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
    static final String CLIENT_ASSERTION_KEY = "client_assertion"; 
    static final String ASSERTION_KEY = "assertion"; 
    static final String GRANT_TYPE_KEY = "grant_type";
    static final String GRANT_TYPE_VALUE = "urn:ietf:params:oauth:grant-type:saml2-bearer";

    private String samlAssertion;
    private String jwtClientAssertion;
    private static final Log logger = LogFactory.getLog(SAMLBearerRequest.class);

    public SAMLBearerRequest(String samlAssertion, String jwtClientAssertion) {

        setGrantType(ClientConfig.SAML_BEARER);
        this.samlAssertion = samlAssertion;
        this.jwtClientAssertion = jwtClientAssertion;

        TokenConfig tokenConfig = ReadyCloud.getConfig().getHttpClient().getOauth().getToken();
        if(tokenConfig != null) {
            setServerUrl(tokenConfig.getServerUrl());
            setClientId(tokenConfig.getClientCredentials().getClientId());
            setUri(tokenConfig.getClientCredentials().getUri());
        }
    }

    public String getSamlAssertion() {
        return this.samlAssertion;
    }

    public String getJwtClientAssertion() {
        return this.jwtClientAssertion;
    }
}
