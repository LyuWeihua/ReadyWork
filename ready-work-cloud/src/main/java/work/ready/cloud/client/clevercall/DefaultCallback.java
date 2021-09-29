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

package work.ready.cloud.client.clevercall;

import javax.net.ssl.SSLSession;
import java.net.URI;
import java.net.http.*;
import java.util.Optional;

public class DefaultCallback implements Callback {
    private HttpResponse<String> response;

    @Override
    public Callback callback(Object owner, HttpResponse<String> response) {
        this.response = response;
        return this;
    }

    @Override
    public int statusCode() {
        return response.statusCode();
    }

    @Override
    public HttpRequest request() {
        return response.request();
    }

    @Override
    public Optional<HttpResponse<String>> previousResponse() {
        return response.previousResponse();
    }

    @Override
    public HttpHeaders headers() {
        return response.headers();
    }

    @Override
    public String body() {
        return response.body();
    }

    @Override
    public Optional<SSLSession> sslSession() {
        return response.sslSession();
    }

    @Override
    public URI uri() {
        return response.uri();
    }

    @Override
    public HttpClient.Version version() {
        return response.version();
    }
}
