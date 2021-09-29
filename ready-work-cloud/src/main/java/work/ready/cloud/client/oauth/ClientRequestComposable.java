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

import io.undertow.util.Headers;
import work.ready.core.handler.RequestMethod;
import work.ready.core.server.Constant;
import work.ready.core.tools.StrUtil;

import java.net.URI;
import java.net.http.HttpRequest;

public interface ClientRequestComposable {
    
    HttpRequest.Builder composeClientRequest(TokenRequest tokenRequest);

    default HttpRequest.Builder requestBuilder(TokenRequest tokenRequest) {
        String requestBody = composeRequestBody(tokenRequest);
        final HttpRequest.Builder builder = HttpRequest.newBuilder().method(RequestMethod.POST.name(), HttpRequest.BodyPublishers.ofString(requestBody, Constant.DEFAULT_CHARSET));
        
        if(!StrUtil.isEmpty(requestBody)) {
            long contentLength = requestBody.getBytes(Constant.DEFAULT_CHARSET).length;
            builder.header(Headers.CONTENT_LENGTH_STRING, String.valueOf(contentLength));
        }
        builder.header(Headers.CONTENT_TYPE_STRING, "application/x-www-form-urlencoded");
        return builder.uri(URI.create(tokenRequest.getServerUrl() + tokenRequest.getUri()));
    }

    String composeRequestBody(TokenRequest tokenRequest);
}
