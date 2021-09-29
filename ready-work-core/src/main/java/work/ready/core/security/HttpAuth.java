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

package work.ready.core.security;

import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

public class HttpAuth {
    private static final String BASIC = "Basic";
    private boolean headerAvailable;
    private boolean basicAuth = false;
    private boolean invalidCredentials;
    private String clientId;
    private String clientSecret;
    private String credentials;
    private String authHeader;

    public HttpAuth(String authHeader) {
        process(authHeader);
    }

    private void process(String authHeader) {
        this.authHeader = authHeader;
        if(authHeader == null || authHeader.trim().length() == 0) {
            headerAvailable = false;
        } else {
            headerAvailable = true;

            String basic = authHeader.substring(0, 5);
            if("BASIC".equalsIgnoreCase(basic)) {
                basicAuth = true;
                credentials = authHeader.substring(6);
                int pos = credentials.indexOf(':');
                if (pos == -1) {
                    credentials = decodeCredentials(credentials);
                }
                pos = credentials.indexOf(':');
                if (pos != -1) {
                    clientId = credentials.substring(0, pos);
                    clientSecret = credentials.substring(pos + 1);
                    invalidCredentials = false;
                } else {
                    invalidCredentials = true;
                }
            }
        }
    }

    private String decodeCredentials(String cred) {
        return new String(Base64.getDecoder().decode(cred), UTF_8);
    }

    public boolean isHeaderAvailable() {
        return headerAvailable;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public boolean isInvalidCredentials() {
        return invalidCredentials;
    }

    public boolean isValid() {
        return isHeaderAvailable() && !isInvalidCredentials();
    }

    public boolean isBasicAuth() {
        return basicAuth;
    }

    public String getCredentials() {
        return credentials;
    }

    public String getAuthHeader() {
        return authHeader;
    }

    public static String getBasicAuthHeader(String clientId, String clientSecret) {
        return BASIC + " " + encodeCredentials(clientId, clientSecret);
    }

    public static String encodeCredentials(String clientId, String clientSecret) {
        String cred;
        if(clientSecret != null) {
            cred = clientId + ":" + clientSecret;
        } else {
            cred = clientId;
        }
        String encodedValue;
        byte[] encodedBytes = Base64.getEncoder().encode(cred.getBytes(UTF_8));
        encodedValue = new String(encodedBytes, UTF_8);
        return encodedValue;
    }
}
