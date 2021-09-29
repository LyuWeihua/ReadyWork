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

package work.ready.core.apm.collector.http.server;

import io.undertow.util.AttachmentKey;
import work.ready.core.apm.collector.http.server.interceptor.ControllerInterceptor;
import work.ready.core.apm.collector.http.server.interceptor.WebHandlerInterceptor;
import work.ready.core.apm.model.CollectorConfig;
import work.ready.core.apm.model.Span;
import work.ready.core.apm.model.Tags;
import work.ready.core.apm.model.TraceContextModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebServerConfig extends CollectorConfig {

    public static final String name = "webserver";
    public static final AttachmentKey<TraceContextModel> TRACE_START = AttachmentKey.create(TraceContextModel.class);
    public static final AttachmentKey<Boolean> TRACE_SKIP = AttachmentKey.create(Boolean.class);

    private boolean enabled = true;
    private boolean enableHandlerRequestParam = false;
    private boolean enableHandlerRequestBody = false;
    private boolean enableHandlerRequestHeader = false;
    private boolean enableHandlerResponseHeader = false;
    private boolean enableHandlerResponseBody = false;

    private boolean enableControllerRequestParam = true;
    private boolean enableControllerRequestBody = false;
    private boolean enableControllerRequestHeader = true;
    private boolean enableControllerResponseHeader = true;
    private boolean enableControllerResponseBody = false;

    private Map<String, Boolean> requestParam;
    private Map<String, Boolean> requestBody;
    private Map<String, Boolean> requestHeader;
    private Map<String, Boolean> responseHeader;
    private Map<String, Boolean> responseBody;
    private long spend = -1;
    private Map<String, String> headerExclude;
    private List<String> urlSuffixExclude = new ArrayList<>();
    private List<String> defaultUrlSuffixExclude = List.of(".css",".js",".ico",".jpg",".gif");
    private List<String> handlerExclude = new ArrayList<>();
    private List<String> defaultHandlerExclude = List.of("io.undertow.server.handlers.resource.ResourceHandler",
                                                         "io.undertow.server.handlers.resource.ResourceHandler$1",
                                                         "io.undertow.server.handlers.PathHandler",
                                                         "io.undertow.server.handlers.NameVirtualHostHandler",
                                                         "io.undertow.server.handlers.encoding.EncodingHandler",
                                                         "io.undertow.server.handlers.ResponseCodeHandler",
                                                         "io.undertow.server.protocol.http2.Http2UpgradeHandler",
                                                         "io.undertow.server.protocol.http2.Http2UpgradeHandler$4$1",
                                                         "work.ready.core.handler.response.SetHeaderHandler");
    private List<String> controllerExclude;

    @Override
    public String getCollectorName() {
        return name;
    }

    @Override
    public List<Class<?>> getCollectorClasses() {
        List<Class<?>> interceptor = new ArrayList<>();
        interceptor.add(WebHandlerInterceptor.class);
        interceptor.add(ControllerInterceptor.class);
        return interceptor;
    }

    @Override
    public int getOrder() {
        return 5;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getSpend() {
        return spend;
    }

    public void setSpend(long spend) {
        this.spend = spend;
    }
    public void setEnableHandlerRequestParam(boolean enableHandlerRequestParam) {
        this.enableHandlerRequestParam = enableHandlerRequestParam;
    }

    public void setEnableHandlerRequestBody(boolean enableHandlerRequestBody) {
        this.enableHandlerRequestBody = enableHandlerRequestBody;
    }

    public void setEnableHandlerRequestHeader(boolean enableHandlerRequestHeader) {
        this.enableHandlerRequestHeader = enableHandlerRequestHeader;
    }

    public void setEnableHandlerResponseHeader(boolean enableHandlerResponseHeader) {
        this.enableHandlerResponseHeader = enableHandlerResponseHeader;
    }

    public void setEnableHandlerResponseBody(boolean enableHandlerResponseBody) {
        this.enableHandlerResponseBody = enableHandlerResponseBody;
    }

    public void setEnableControllerRequestParam(boolean enableControllerRequestParam) {
        this.enableControllerRequestParam = enableControllerRequestParam;
    }

    public void setEnableControllerRequestBody(boolean enableControllerRequestBody) {
        this.enableControllerRequestBody = enableControllerRequestBody;
    }

    public void setEnableControllerRequestHeader(boolean enableControllerRequestHeader) {
        this.enableControllerRequestHeader = enableControllerRequestHeader;
    }

    public void setEnableControllerResponseHeader(boolean enableControllerResponseHeader) {
        this.enableControllerResponseHeader = enableControllerResponseHeader;
    }

    public void setEnableControllerResponseBody(boolean enableControllerResponseBody) {
        this.enableControllerResponseBody = enableControllerResponseBody;
    }

    public List<String> getDefaultUrlSuffixExclude() {
        return defaultUrlSuffixExclude;
    }

    public void setDefaultUrlSuffixExclude(List<String> defaultUrlSuffixExclude) {
        this.defaultUrlSuffixExclude = defaultUrlSuffixExclude;
    }

    public List<String> getDefaultHandlerExclude() {
        return defaultHandlerExclude;
    }

    public void setDefaultHandlerExclude(List<String> defaultHandlerExclude) {
        this.defaultHandlerExclude = defaultHandlerExclude;
    }

    public Map<String, Boolean> getRequestParam() {
        return requestParam;
    }

    public void setRequestParam(Map<String, Boolean> requestParam) {
        this.requestParam = requestParam;
    }

    public Map<String, Boolean> getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(Map<String, Boolean> requestBody) {
        this.requestBody = requestBody;
    }

    public Map<String, Boolean> getRequestHeader() {
        return requestHeader;
    }

    public void setRequestHeader(Map<String, Boolean> requestHeader) {
        this.requestHeader = requestHeader;
    }

    public Map<String, Boolean> getResponseHeader() {
        return responseHeader;
    }

    public void setResponseHeader(Map<String, Boolean> responseHeader) {
        this.responseHeader = responseHeader;
    }

    public Map<String, Boolean> getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(Map<String, Boolean> responseBody) {
        this.responseBody = responseBody;
    }

    public boolean isEnableHandlerRequestParam(Span span) {
        if(requestParam != null && !requestParam.isEmpty()) {
            String className = (String)span.getTag(Tags.HANDLER);
            if(className != null && requestParam.get(className) != null) {
                return requestParam.get(className);
            }
        }
        return enableHandlerRequestParam;
    }

    public boolean isEnableHandlerRequestBody(Span span) {
        if(requestBody != null && !requestBody.isEmpty()) {
            String className = (String)span.getTag(Tags.HANDLER);
            if(className != null && requestBody.containsKey(className)) {
                return requestBody.get(className);
            }
        }
        return enableHandlerRequestBody;
    }

    public boolean isEnableHandlerRequestHeader(Span span) {
        if(requestHeader != null && !requestHeader.isEmpty()) {
            String className = (String)span.getTag(Tags.HANDLER);
            if(className != null && requestHeader.containsKey(className)) {
                return requestHeader.get(className);
            }
        }
        return enableHandlerRequestHeader;
    }

    public boolean isEnableHandlerResponseHeader(Span span) {
        if(responseHeader != null && !responseHeader.isEmpty()) {
            String className = (String)span.getTag(Tags.HANDLER);
            if(className != null && responseHeader.containsKey(className)) {
                return responseHeader.get(className);
            }
        }
        return enableHandlerResponseHeader;
    }

    public boolean isEnableHandlerResponseBody(Span span) {
        if(responseBody != null && !responseBody.isEmpty()) {
            String className = (String)span.getTag(Tags.HANDLER);
            if(className != null && responseBody.containsKey(className)) {
                return responseBody.get(className);
            }
        }
        return enableHandlerResponseBody;
    }

    public boolean isEnableControllerRequestParam(Span span) {
        if(requestParam != null && !requestParam.isEmpty()) {
            String className = (String)span.getTag(Tags.CONTROLLER);
            if(className != null && requestParam.get(className) != null) {
                return requestParam.get(className);
            }
        }
        return enableControllerRequestParam;
    }

    public boolean isEnableControllerRequestBody(Span span) {
        if(requestBody != null && !requestBody.isEmpty()) {
            String className = (String)span.getTag(Tags.CONTROLLER);
            if(className != null && requestBody.containsKey(className)) {
                return requestBody.get(className);
            }
        }
        return enableControllerRequestBody;
    }

    public boolean isEnableControllerRequestHeader(Span span) {
        if(requestHeader != null && !requestHeader.isEmpty()) {
            String className = (String)span.getTag(Tags.CONTROLLER);
            if(className != null && requestHeader.containsKey(className)) {
                return requestHeader.get(className);
            }
        }
        return enableControllerRequestHeader;
    }

    public boolean isEnableControllerResponseHeader(Span span) {
        if(responseHeader != null && !responseHeader.isEmpty()) {
            String className = (String)span.getTag(Tags.CONTROLLER);
            if(className != null && responseHeader.containsKey(className)) {
                return responseHeader.get(className);
            }
        }
        return enableControllerResponseHeader;
    }

    public boolean isEnableControllerResponseBody(Span span) {
        if(responseBody != null && !responseBody.isEmpty()) {
            String className = (String)span.getTag(Tags.CONTROLLER);
            if(className != null && responseBody.containsKey(className)) {
                return responseBody.get(className);
            }
        }
        return enableControllerResponseBody;
    }

    public Map<String, String> getHeaderExclude() {
        return headerExclude;
    }

    public void setHeaderExclude(Map<String, String> headerExclude) {
        this.headerExclude = headerExclude;
    }

    public void setHeaderExclude(String header, String value) {
        if(this.headerExclude == null) {
            this.headerExclude = new HashMap<>();
        }
        this.headerExclude.put(header, value);
    }

    public List<String> getUrlSuffixExclude() {
        return urlSuffixExclude;
    }

    public void setUrlSuffixExclude(List<String> urlSuffixExclude) {
        this.urlSuffixExclude = urlSuffixExclude;
    }

    public List<String> getHandlerExclude() {
        if(defaultHandlerExclude != null) {
            synchronized (this) {
                if(defaultHandlerExclude != null) {
                    handlerExclude.addAll(defaultHandlerExclude);
                    defaultHandlerExclude = null;
                }
            }
        }
        return handlerExclude;
    }

    public void setHandlerExclude(List<String> handlerExclude) {
        this.handlerExclude = handlerExclude;
    }

    public List<String> getControllerExclude() {
        return controllerExclude;
    }

    public void setControllerExclude(List<String> controllerExclude) {
        this.controllerExclude = controllerExclude;
    }

    public boolean isExcludeUrlSuffix(String url) {
        if(defaultUrlSuffixExclude != null) {
            synchronized (this) {
                if(defaultUrlSuffixExclude != null) {
                    urlSuffixExclude.addAll(defaultUrlSuffixExclude);
                    defaultUrlSuffixExclude = null;
                }
            }
        }
        int size = urlSuffixExclude.size();
        for (int i = 0; i < size; i++) {
            if (url.endsWith(urlSuffixExclude.get(i))) {
                return true;
            }
        }
        return false;
    }
}
