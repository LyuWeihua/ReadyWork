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

import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import work.ready.cloud.cluster.Cloud;
import work.ready.core.event.cloud.Event;
import work.ready.core.event.GeneralEvent;
import work.ready.core.exception.ApiException;
import work.ready.core.handler.ContentType;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;
import work.ready.core.service.status.Status;
import work.ready.core.tools.StrUtil;

import java.lang.reflect.InvocationTargetException;
import java.net.ConnectException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class DefaultCallHandler implements CallHandler {

    private static final Log logger = LogFactory.getLog(DefaultCallHandler.class);

    @Override
    public <T> T handle(Object owner, String methodName, String urlProtocol, String project, String projectVersion, String serviceId, String serviceVersion, String profile, String uri, int timeout, int retry, Class<?> returnType, Class<? extends Callback> callbackClass, Class<? extends Fallback> fallbackClass, HttpRequest.Builder requestBuilder) {
        Object returnObject;
        try {
            boolean external = StrUtil.isBlank(serviceId);
            String result = null;
            int statusCode = StatusCodes.OK;
            HttpResponse<String> response = null;

            CircuitBreaker service;
            if(external){
                service = Cloud.getService(requestBuilder);
            } else {
                service = Cloud.getService(urlProtocol, project, projectVersion, serviceId, serviceVersion, profile, uri, requestBuilder);
            }
            while(true) {
                try {
                    if(!external && statusCode >= StatusCodes.INTERNAL_SERVER_ERROR)
                        service = Cloud.getService(urlProtocol, project, projectVersion, serviceId, serviceVersion, profile, uri, requestBuilder);
                    response = service.setTimeout(timeout).call();
                    logger.debug("call response: " + response.headers().map());
                    if(callbackClass != DefaultCallback.class) { 
                        response = callbackClass.getConstructor().newInstance().callback(owner, response);
                    }
                    var contentType = response.headers().firstValue(Headers.CONTENT_TYPE_STRING).orElse(ContentType.TEXT_HTML.toString());
                    statusCode = response.statusCode();
                    result = response.body();
                    result = result.trim();
                    boolean possibleJson = contentType.contains("json");
                    boolean possibleJsonObject = (possibleJson && result.startsWith("{") && result.endsWith("}"));
                    boolean possibleHtml = contentType.contains("text"); 
                    if(statusCode == StatusCodes.OK) { 
                        if(possibleJson) {
                            if(returnType.equals(String.class)) {
                                returnObject = result;
                            } else {
                                returnObject = Ready.config().getJsonMapper().readValue(result, returnType);
                            }
                        } else {
                            if(String.class.equals(returnType)){
                                returnObject = result;
                            } else {
                                throw new ApiException(new Status("ERROR10014", "Result content cannot converter to " + returnType.getCanonicalName() + " type: " + result));
                            }
                        }
                    } else {
                        if(possibleJsonObject)
                            throw new ApiException(Ready.config().getJsonMapper().readValue(result, Status.class));
                        else
                            throw new ApiException(new Status("ERROR10014", result));
                    }
                    break;
                } catch (TimeoutException | ExecutionException | CircuitBreakerException e) {
                    if(e instanceof ExecutionException) {
                        if(e.getCause() instanceof ConnectException) {
                            if(!external) Ready.post(new GeneralEvent(Event.SERVICE_HOST_UNREACHABLE, this, service.getUrl()).put("CircuitBreaker", service));
                        } else if(e.getCause() instanceof TimeoutException || e.getCause() instanceof HttpTimeoutException) {
                            if(!external) Ready.post(new GeneralEvent(Event.SERVICE_UNSTABLE, this, service.getUrl()).put("CircuitBreaker", service).put("Exception", new TimeoutException(e.getMessage())));
                        } else {throw e;}
                    } else if(e instanceof CircuitBreakerException) {
                        throw new ApiException(new Status("ERROR13003"));

                    } else {
                        if(!external) Ready.post(new GeneralEvent(Event.SERVICE_UNSTABLE, this, service.getUrl()).put("CircuitBreaker", service).put("Exception", e));
                    }
                    if(retry <= 0) {
                        throw new ApiException(new Status("ERROR13002"));
                    }
                    statusCode = StatusCodes.SERVICE_UNAVAILABLE;
                    retry--;
                }
            }
        } catch (Exception e) {
            
            try{
                returnObject = fallbackClass.getConstructor().newInstance().fallback(owner,
                        Map.of("methodName", methodName, "protocol", urlProtocol, "project", project, "projectVersion", projectVersion, "serviceId", serviceId, "serviceVersion", serviceVersion, "profile", profile, "uri", uri, "returnType", returnType, "requestBuilder", requestBuilder), e);
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
                throw new ApiException(new Status("ERROR10014", e.getMessage()));
            }
        }
        return (T) returnObject;
    }

}
