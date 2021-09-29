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

import io.undertow.util.HttpString;
import work.ready.core.apm.ApmManager;
import work.ready.core.apm.collector.http.server.WebServerConfig;
import work.ready.core.apm.common.*;
import work.ready.core.apm.model.Span;
import work.ready.core.apm.model.SpanType;
import work.ready.core.apm.model.Tags;
import work.ready.core.apm.reporter.ReporterManager;
import work.ready.core.handler.Controller;
import work.ready.core.handler.request.HttpRequest;
import work.ready.core.handler.response.HttpResponse;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Constant;

import java.util.HashMap;
import java.util.Map;

import static work.ready.core.apm.collector.http.server.WebServerConfig.TRACE_SKIP;
import static work.ready.core.apm.collector.http.server.WebServerConfig.TRACE_START;

public class ControllerHandler implements TraceHandler {

    private static final Log logger = LogFactory.getLog(ControllerHandler.class);

    @Override
    public Span before(String className, String methodName, Object[] arguments, Object[] extraParam) {
        
        if(!ApmManager.getConfig(WebServerConfig.class).isEnabled()){
            return null;
        }

        Controller controller = (Controller) extraParam[0];
        var request = controller.getRequest();
        if(request.getExchange().getAttachment(TRACE_SKIP) != null) {
            return null;
        }

        if (request.getExchange().getAttachment(TRACE_START) == null) { 

            String url = request.getRequestURL();
            if (ApmManager.getConfig(WebServerConfig.class).isExcludeUrlSuffix(url)) {
                request.getExchange().putAttachment(TRACE_SKIP, true);
                return null;
            }

            var headerExclude = ApmManager.getConfig(WebServerConfig.class).getHeaderExclude();
            if(headerExclude!= null && !headerExclude.isEmpty()) {
                for(var entry : headerExclude.entrySet()) {
                    String head = request.getHeader(entry.getKey());
                    if(entry.getValue().equals(head)) {
                        request.getExchange().putAttachment(TRACE_SKIP, true);
                        return null;
                    }
                }
            }

            request.getExchange().putAttachment(TRACE_START, TraceContext.getOrNew());
            String traceabilityId = request.getHeader(Constant.TRACEABILITY_ID);
            TraceContext.setTraceabilityId(traceabilityId);
            String correlationId = request.getHeader(Constant.CORRELATION_ID);
            if(correlationId == null) {
                correlationId = TraceContext.getCorrelationId();
                request.setHeader(Constant.CORRELATION_ID, correlationId);
                if(traceabilityId != null && logger.isInfoEnabled()) {
                    logger.info("Associate traceability Id %s with correlation Id %s", traceabilityId, correlationId);
                }
            } else {
                TraceContext.setCorrelationId(correlationId);
            }
            TraceContext.setParentId(request.getHeader(ApmConst.PARENT_ID));
            TraceContext.setTag(request.getHeader(ApmConst.TAG));
            SpanManager.createTopologySpan(request.getHeader(ApmConst.SRC_APPLICATION), ApmManager.getConfig().getApplication());
        } else {
            TraceContext.set(request.getExchange().getAttachment(TRACE_START));
        }
        return createSpan(controller);
    }

    private Span createSpan(Controller controller) {
        Span span = SpanManager.createEntrySpan(SpanType.REQUEST);
        String srcApp = controller.getRequest().getHeader(ApmConst.SRC_APPLICATION);
        if (srcApp == null) {
            srcApp = ApmConst.SRC_NO_APPLICATION;
        }
        span.addTag(ApmConst.SRC_APPLICATION_STRING, srcApp);
        span.addTag(ApmConst.SRC_INSTANCE_STRING, controller.getHeader(ApmConst.SRC_INSTANCE));
        span.addTag(Tags.CONTROLLER, controller.getClass().getCanonicalName());
        
        return span;
    }

    @Override
    public Object after(String className, String methodName, Object[] arguments, Object result, Throwable t, Object[] extraParam) {
        
        if (!ApmManager.getConfig(WebServerConfig.class).isEnabled() || SamplingUtil.NO()) {
            return null;
        }
        Span currSpan = SpanManager.getCurrentSpan();
        Controller controller = (Controller) extraParam[0];
        var request = controller.getRequest();
        var response = controller.getResponse();
        if (currSpan != null && currSpan.getType().equals(SpanType.REQUEST)) {
            Span span = SpanManager.getExitSpan(); 
            span.addTag(Tags.URL, request.getRequestUrl());
            span.addTag(Tags.REMOTE, request.getClientIP());
            span.addTag(Tags.METHOD, request.getMethod());
            calculateSpend(span);
            if (span.getSpend() > ApmManager.getConfig(WebServerConfig.class).getSpend() && SamplingUtil.YES()) {
                if(span.getTraceabilityId() != null) {
                    response.setHeader(Constant.TRACEABILITY_ID, span.getTraceabilityId());
                }
                response.setHeader(Constant.CORRELATION_ID, span.getCorrelationId());
                response.setHeader(new HttpString(ApmConst.ID), span.getId());
                ApmManager.getConfig().fillEnvInfo(span);
                ReporterManager.report(span);
                collectRequestParameter(span, request);
                collectRequestHeader(span, request);
                collectResponseHeader(span, response);

            }
        }
        return result;
    }

    private void collectRequestParameter(Span span, HttpRequest request) {
        if (ApmManager.getConfig(WebServerConfig.class).isEnableControllerRequestParam(span)) {
            var getParams = request.getParameterMap(HttpRequest.parameterMap.formPrioritized);
            var pathParams = request.getPathParameters();
            Map<String, String> params = new HashMap<>(8);
            pathParams.forEach(params::put);
            getParams.forEach((k,v)-> params.put(k, v.toString()));
            if (!params.isEmpty()) {
                Span paramSpan = new Span(SpanType.REQUEST_PARAM);
                paramSpan.setId(span.getId());
                paramSpan.addTag(Tags.PARAMS, params.toString());
                ReporterManager.report(paramSpan);
            }
        }
    }

    private void collectRequestHeader(Span span, HttpRequest request) {
        if (ApmManager.getConfig(WebServerConfig.class).isEnableControllerRequestHeader(span)) {
            Map<String, String> headers = new HashMap<>(8);
            var headerNames = request.getHeaderNames();
            while(headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                headers.put(name, request.getHeader(name));
            }
            if (!headers.isEmpty()) {
                Span headersSpan = new Span(SpanType.REQUEST_HEADERS);
                headersSpan.setId(span.getId());
                headersSpan.addTag("requestHeaders", headers.toString());
                ReporterManager.report(headersSpan);
            }
        }
    }

    private void collectResponseHeader(Span span, HttpResponse response) {
        if (ApmManager.getConfig(WebServerConfig.class).isEnableControllerResponseHeader(span)) {
            Map<String, String> headers = new HashMap<>(8);
            var headerNames = response.getHeaderNames();
            for(String name : headerNames) {
                headers.put(name, response.getHeader(name));
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
