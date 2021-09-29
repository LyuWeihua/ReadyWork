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

import java.net.http.HttpRequest;

public interface CallHandler {

    <T> T handle(Object owner, String methodName, String urlProtocol, String project, String projectVersion, String serviceId, String serviceVersion, String profile, String uri, int timeout, int retry, Class<?> returnType, Class<? extends Callback> callback, Class<? extends Fallback> fallback, HttpRequest.Builder requestBuilder);

}
