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
import work.ready.cloud.client.oauth.config.SignConfig;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

public class SignKeyRequest extends KeyRequest {
    private static final Log logger = LogFactory.getLog(SignKeyRequest.class);

    public SignKeyRequest(String kid) {
        super(kid);
        SignConfig signConfig = null;
        signConfig = ReadyCloud.getConfig().getHttpClient().getOauth().getSign();
        if(signConfig != null) {
            if(signConfig.getKey() != null) {
                setServerUrl(signConfig.getKey().getServerUrl());
                setServiceId(signConfig.getKey().getServiceId());
                setUri(signConfig.getKey().getUri() + "/" + kid);
                setClientId(signConfig.getKey().getClientId());
                setClientSecret(signConfig.getKey().getClientSecret());
            } else {
                logger.error("Error: could not find key section in sign of oauth in config");
            }
        } else {
            logger.error("Error: could not find sign section of oauth in config");
        }
    }
}
