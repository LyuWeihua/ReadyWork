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

public class OauthConfig {

    private TokenConfig token = new TokenConfig();
    private SignConfig sign = new SignConfig();
    private DerefConfig deref = new DerefConfig();

    public TokenConfig getToken() {
        return token;
    }

    public void setToken(TokenConfig token) {
        this.token = token;
    }

    public SignConfig getSign() {
        return sign;
    }

    public void setSign(SignConfig sign) {
        this.sign = sign;
    }

    public DerefConfig getDeref() {
        return deref;
    }

    public void setDeref(DerefConfig deref) {
        this.deref = deref;
    }
}

