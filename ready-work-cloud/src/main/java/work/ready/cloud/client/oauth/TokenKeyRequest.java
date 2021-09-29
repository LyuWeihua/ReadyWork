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
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

public class TokenKeyRequest extends KeyRequest {
    private static final Log logger = LogFactory.getLog(TokenKeyRequest.class);

    public TokenKeyRequest(String kid) {
        super(kid);

        if(ReadyCloud.getConfig().getHttpClient() != null) {
            if(ReadyCloud.getConfig().getHttpClient().getOauth() != null) {
                TokenConfig tokenConfig = ReadyCloud.getConfig().getHttpClient().getOauth().getToken();
                if(tokenConfig != null) {
                    if(tokenConfig.getKey() != null) {
                        setServerUrl(tokenConfig.getKey().getServerUrl());
                        setServiceId(tokenConfig.getKey().getServiceId());
                        setUri(tokenConfig.getKey().getUri() + "/" + kid);
                        setClientId(tokenConfig.getKey().getClientId());
                        setClientSecret(tokenConfig.getKey().getClientSecret());
                    } else {
                        logger.error("Error: could not find key section in token of oauth in bootstrap config");
                    }
                } else {
                    logger.error("Error: could not find token section of oauth in bootstrap config");
                }
            } else {
                logger.error("Error: could not find oauth section of http2client in bootstrap config");
            }
        } else {
            logger.error("Error: could not find http2client section in bootstrap config for Token Key");
        }
    }
}

