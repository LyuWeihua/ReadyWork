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

import work.ready.cloud.client.CloudClient;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

import java.net.http.HttpRequest;
import java.util.HashMap;
import java.util.Map;

import static work.ready.cloud.client.oauth.ClientRequestComposerProvider.ClientRequestComposers.CLIENT_CREDENTIAL_REQUEST_COMPOSER;
import static work.ready.cloud.client.oauth.ClientRequestComposerProvider.ClientRequestComposers.SAML_BEARER_REQUEST_COMPOSER;

public class ClientRequestComposerProvider {
    public enum ClientRequestComposers { CLIENT_CREDENTIAL_REQUEST_COMPOSER, SAML_BEARER_REQUEST_COMPOSER }
    private static final ClientRequestComposerProvider INSTANCE = new ClientRequestComposerProvider();
    private Map<ClientRequestComposers, ClientRequestComposable> composersMap = new HashMap<>();
    private static final Log logger = LogFactory.getLog(ClientRequestComposerProvider.class);
    private ClientRequestComposerProvider() {
    }

    public static ClientRequestComposerProvider getInstance() {
        return INSTANCE;
    }

    public ClientRequestComposable getComposer(ClientRequestComposers composerName) {
        ClientRequestComposable composer = composersMap.get(composerName);
        if(composer == null) {
            initDefaultComposer(composerName);
        }
        return composersMap.get(composerName);
    }

    private void initDefaultComposer(ClientRequestComposers composerName) {
        switch (composerName) {
            case CLIENT_CREDENTIAL_REQUEST_COMPOSER:
                composersMap.put(CLIENT_CREDENTIAL_REQUEST_COMPOSER, new DefaultClientCredentialRequestComposer());
                break;
            case SAML_BEARER_REQUEST_COMPOSER:
                composersMap.put(SAML_BEARER_REQUEST_COMPOSER, new DefaultSAMLBearerRequestComposer());
                break;
            default:
                break;
        }
    }

    public void registerComposer(ClientRequestComposers composerName, ClientRequestComposable composer) {
        composersMap.put(composerName, composer);
    }

    private static class DefaultSAMLBearerRequestComposer implements ClientRequestComposable {

        @Override
        public HttpRequest.Builder composeClientRequest(TokenRequest tokenRequest) {
            return requestBuilder(tokenRequest);
        }

        @Override
        public String composeRequestBody(TokenRequest tokenRequest) {
            SAMLBearerRequest SamlTokenRequest = (SAMLBearerRequest)tokenRequest;
            Map<String, Object> postBody = new HashMap<>();
            postBody.put(SAMLBearerRequest.GRANT_TYPE_KEY , SAMLBearerRequest.GRANT_TYPE_VALUE );
            postBody.put(SAMLBearerRequest.ASSERTION_KEY, SamlTokenRequest.getSamlAssertion());
            postBody.put(SAMLBearerRequest.CLIENT_ASSERTION_TYPE_KEY, SAMLBearerRequest.CLIENT_ASSERTION_TYPE_VALUE);
            postBody.put(SAMLBearerRequest.CLIENT_ASSERTION_KEY, SamlTokenRequest.getJwtClientAssertion());
            return CloudClient.getFormDataString(postBody);
        }
    }

    private static class DefaultClientCredentialRequestComposer implements ClientRequestComposable {

        @Override
        public HttpRequest.Builder composeClientRequest(TokenRequest tokenRequest) {
            return requestBuilder(tokenRequest);
        }

        @Override
        public String composeRequestBody(TokenRequest tokenRequest) {
            return OauthHelper.getEncodedString(tokenRequest);
        }
    }
}
