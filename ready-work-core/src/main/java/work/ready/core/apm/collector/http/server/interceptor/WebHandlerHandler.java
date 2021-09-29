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

package work.ready.core.apm.collector.http.server.interceptor;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.HttpString;
import work.ready.core.apm.ApmManager;
import work.ready.core.apm.collector.http.client.HttpClientConfig;
import work.ready.core.apm.collector.http.server.WebServerConfig;
import work.ready.core.apm.common.*;
import work.ready.core.apm.model.Span;
import work.ready.core.apm.model.SpanType;
import work.ready.core.apm.model.Tags;
import work.ready.core.apm.reporter.ReporterManager;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Constant;

import java.util.HashMap;
import java.util.Map;

import static work.ready.core.apm.collector.http.server.WebServerConfig.TRACE_SKIP;
import static work.ready.core.apm.collector.http.server.WebServerConfig.TRACE_START;

public class WebHandlerHandler implements TraceHandler {

    private static final Log logger = LogFactory.getLog(WebHandlerHandler.class);

    @Override
    public Span before(String className, String methodName, Object[] arguments, Object[] extraParam) {
        
        if(!ApmManager.getConfig(WebServerConfig.class).isEnabled()){
            return null;
        }
        HttpServerExchange exchange = (HttpServerExchange) arguments[0];
        if(exchange.getAttachment(TRACE_SKIP) != null) {
            return null;
        }
        String url = exchange.getRequestURL();
        if (ApmManager.getConfig(WebServerConfig.class).isExcludeUrlSuffix(url)) {
            exchange.putAttachment(TRACE_SKIP, true);
            return null;
        }

        var headerExclude = ApmManager.getConfig(WebServerConfig.class).getHeaderExclude();
        if(headerExclude!= null && !headerExclude.isEmpty()) {
            for(var entry : headerExclude.entrySet()) {
                String head = exchange.getRequestHeaders().getFirst(entry.getKey());
                if(entry.getValue().equals(head)) {
                    exchange.putAttachment(TRACE_SKIP, true);
                    return null;
                }
            }
        }

        HttpHandler handler = (HttpHandler)extraParam[0];
        var requestHeaders = exchange.getRequestHeaders();
        if (exchange.getAttachment(TRACE_START) == null) {
            exchange.putAttachment(TRACE_START, TraceContext.getOrNew());
            String traceabilityId = requestHeaders.getFirst(Constant.TRACEABILITY_ID);
            TraceContext.setTraceabilityId(traceabilityId);
            String correlationId = requestHeaders.getFirst(Constant.CORRELATION_ID);
            if(correlationId == null) {
                correlationId = TraceContext.getCorrelationId();
                requestHeaders.put(Constant.CORRELATION_ID, correlationId);
                if(traceabilityId != null && logger.isInfoEnabled()) {
                    logger.info("Associate traceability Id %s with correlation Id %s", traceabilityId, correlationId);
                }
            } else {
                TraceContext.setCorrelationId(correlationId);
            }
            TraceContext.setParentId(requestHeaders.getFirst(ApmConst.PARENT_ID));
            TraceContext.setTag(requestHeaders.getFirst(ApmConst.TAG));
            SpanManager.createTopologySpan(requestHeaders.getFirst(ApmConst.SRC_APPLICATION), ApmManager.getConfig().getApplication());
        } else {
            TraceContext.set(exchange.getAttachment(TRACE_START));
            
            if(handler.getClass().getCanonicalName().equals(TraceContext.getTarget())) {
                return null;
            }
        }
        return createSpan(exchange, handler, requestHeaders);
    }

    private Span createSpan(HttpServerExchange exchange, HttpHandler handler, HeaderMap requestHeaders) {
        Span span = SpanManager.createLocalSpan(SpanType.REQUEST);
        String srcApp = requestHeaders.getFirst(ApmConst.SRC_APPLICATION);
        if (srcApp == null) {
            srcApp = ApmConst.SRC_NO_APPLICATION;
        }
        span.addTag(ApmConst.SRC_APPLICATION_STRING, srcApp);
        span.addTag(ApmConst.SRC_INSTANCE_STRING, requestHeaders.getFirst(ApmConst.SRC_INSTANCE));
        span.addTag(Tags.HANDLER, handler.getClass().getCanonicalName());
        TraceContext.setTarget(handler.getClass().getCanonicalName());

        var responseHeaders = exchange.getResponseHeaders();
        span.addTag(Tags.URL, exchange.getRequestURL());
        span.addTag(Tags.REMOTE, exchange.getSourceAddress().getAddress().getHostAddress());
        span.addTag(Tags.METHOD, exchange.getRequestMethod().toString());
        if (SamplingUtil.YES()) {
            if(span.getTraceabilityId() != null) {
                responseHeaders.put(Constant.TRACEABILITY_ID, span.getTraceabilityId());
            }
            responseHeaders.put(Constant.CORRELATION_ID, span.getCorrelationId());
            responseHeaders.put(new HttpString(ApmConst.ID), span.getId());
            ApmManager.getConfig().fillEnvInfo(span);
            ReporterManager.report(span);
            collectRequestParameter(span, exchange);
            collectRequestHeader(span, exchange);

        }

        return span;
    }

    @Override
    public Object after(String className, String methodName, Object[] arguments, Object result, Throwable t, Object[] extraParam) {
        
        return result;
    }

    private void collectRequestParameter(Span span, HttpServerExchange exchange) {
        if (ApmManager.getConfig(WebServerConfig.class).isEnableHandlerRequestParam(span)) {
            var getParams = exchange.getQueryParameters();
            var pathParams = exchange.getPathParameters();
            Map<String, String> params = new HashMap<String, String>(8);
            if(pathParams != null) {
                pathParams.forEach((k,v)-> params.put(k, v.getFirst()));
            }
            if(getParams != null) {
                getParams.forEach((k,v)-> params.put(k, v.getFirst()));
            }
            if (!params.isEmpty()) {
                Span paramSpan = new Span(SpanType.REQUEST_PARAM);
                paramSpan.setId(span.getId());
                paramSpan.addTag(Tags.PARAMS, params.toString());
                ReporterManager.report(paramSpan);
            }
        }
    }

    private void collectRequestHeader(Span span, HttpServerExchange exchange) {
        if (ApmManager.getConfig(WebServerConfig.class).isEnableHandlerRequestHeader(span)) {
            Map<String, String> headers = new HashMap<>(8);
            var headerMap = exchange.getRequestHeaders();
            for(var header : headerMap) {
                headers.put(header.getHeaderName().toString(), header.getFirst());
            }
            if (!headers.isEmpty()) {
                Span headersSpan = new Span(SpanType.REQUEST_HEADERS);
                headersSpan.setId(span.getId());
                headersSpan.addTag("requestHeaders", headers.toString());
                ReporterManager.report(headersSpan);
            }
        }
    }

    private void collectResponseHeader(Span span, HttpServerExchange exchange) {
        if (ApmManager.getConfig(WebServerConfig.class).isEnableHandlerResponseHeader(span)) {
            Map<String, String> headers = new HashMap<>(8);
            var headerMap = exchange.getResponseHeaders();
            for(var header : headerMap) {
                headers.put(header.getHeaderName().toString(), header.getFirst());
            }
            if (!headers.isEmpty()) {
                Span headersSpan = new Span(SpanType.RESPONSE_HEADERS);
                headersSpan.setId(span.getId());
                headersSpan.addTag("responseHeaders", headers.toString());
                ReporterManager.report(headersSpan);
            }
        }
    }
}
