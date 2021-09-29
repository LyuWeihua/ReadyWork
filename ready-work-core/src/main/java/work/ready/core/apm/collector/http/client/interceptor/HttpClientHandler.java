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

package work.ready.core.apm.collector.http.client.interceptor;

import work.ready.core.apm.ApmManager;
import work.ready.core.apm.collector.http.client.HttpClientConfig;
import work.ready.core.apm.common.*;
import work.ready.core.apm.model.Span;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Constant;

import java.net.http.HttpRequest;
import java.util.Arrays;

public class HttpClientHandler implements TraceHandler {

    private static final Log logger = LogFactory.getLog(HttpClientHandler.class);

    @Override
    public Span before(String className, String methodName, Object[] arguments, Object[] extraParam) {
        
        if (arguments[0] instanceof HttpRequest.Builder) {
            HttpRequest.Builder requestBuilder = (HttpRequest.Builder) arguments[0];
            HttpRequest request = requestBuilder.build();
            if(ApmManager.getConfig(HttpClientConfig.class).isUrlIncluded(request.uri().toString())
                    && request.headers().firstValue(Constant.CORRELATION_ID_STRING).isEmpty()){

                var headerExclude = ApmManager.getConfig(HttpClientConfig.class).getHeaderExclude();
                if(headerExclude!= null && !headerExclude.isEmpty()) {
                    for(var entry : headerExclude.entrySet()) {
                        String head = request.headers().firstValue(entry.getKey()).orElse(null);
                        if(entry.getValue().equals(head)) {
                            return null;
                        }
                    }
                }

                if(TraceContext.getTraceabilityId() != null) {
                    requestBuilder.header(Constant.TRACEABILITY_ID_STRING, TraceContext.getTraceabilityId());
                }
                requestBuilder.header(Constant.CORRELATION_ID_STRING, TraceContext.getCorrelationId());
                requestBuilder.header(ApmConst.PARENT_ID, TraceContext.getCurrentId());
                if(TraceContext.getTag() != null) {
                    requestBuilder.header(ApmConst.TAG, TraceContext.getTag());
                }
                requestBuilder.header(ApmConst.SRC_APPLICATION, ApmManager.getConfig().getApplication());
                requestBuilder.header(ApmConst.SRC_INSTANCE, ApmManager.getConfig().getInstance());
                arguments[0] = requestBuilder;
            }
        }
        return null;
    }

    @Override
    public Object after(String className, String methodName, Object[] arguments, Object result, Throwable t, Object[] extraParam) {
        
        return result;
    }

}
