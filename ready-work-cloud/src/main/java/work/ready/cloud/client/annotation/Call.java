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

package work.ready.cloud.client.annotation;

import io.undertow.util.Headers;
import work.ready.cloud.client.clevercall.*;
import work.ready.core.handler.RequestMethod;

import java.lang.annotation.*;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Call {
    String project() default ""; 
    String serviceId(); 
    String url(); 
    Protocol protocol() default Protocol.http; 
    RequestMethod method() default RequestMethod.GET;
    RequestType type() default RequestType.json;
    String authorization() default Headers.AUTHORIZATION_STRING;

    String projectVersion() default ""; 
    String serviceVersion() default ""; 
    String group() default "default"; 
    String profile() default ""; 
    int timeout() default 0; 
    int retry() default -1; 

    Balance loadBalance() default Balance.RoundRobin; 
    Class<? extends Callback> callback() default DefaultCallback.class;
    Class<? extends CallHandler> handler() default DefaultCallHandler.class;
    Class<? extends Fallback> fallback() default DefaultFallback.class;

    public enum RequestType {
        json, form
    }

    public enum Protocol {
        http, https, ws, wss
    }
    public enum Balance {
        LocalFirst, Hash, RoundRobin
    }
}
