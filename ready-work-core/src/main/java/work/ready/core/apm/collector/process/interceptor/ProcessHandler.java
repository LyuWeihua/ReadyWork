/**
 *
 * Original work copyright bee-apm
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
package work.ready.core.apm.collector.process.interceptor;

import work.ready.core.apm.ApmManager;
import work.ready.core.apm.collector.process.ProcessConfig;
import work.ready.core.apm.common.SamplingUtil;
import work.ready.core.apm.common.SpanManager;
import work.ready.core.apm.common.TraceContext;
import work.ready.core.apm.common.TraceHandler;
import work.ready.core.apm.model.Span;
import work.ready.core.apm.model.SpanType;
import work.ready.core.apm.model.Tags;
import work.ready.core.apm.reporter.ReporterManager;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class ProcessHandler implements TraceHandler {

    private static final Log logger = LogFactory.getLog(ProcessHandler.class);

    private static final String KEY_ERROR_THROWABLE = "_ERROR_THROWABLE";
    private static final String KEY_BEE_CHILD_ID = "_BEE_CHILD_ID";
    private static final String KEY_ERROR_POINT = "_ERROR_POINT";
    private static final String KEY_PARAM = "_PARAM";

    @Override
    public Span before(String className, String methodName, Object[] arguments, Object[] extraParam) {
        if(!ApmManager.getConfig(ProcessConfig.class).isEnabled()){
            return null;
        }
        Span span = SpanManager.createEntrySpan(SpanType.PROCESS);
        String params = collectParams(arguments, span.getId(), className + "." + methodName);
        span.addTag(KEY_PARAM, params);
        return span;
    }

    @Override
    public Object after(String className, String methodName, Object[] arguments, Object result, Throwable t, Object[] extraParam) {
        Span span = SpanManager.getExitSpan();
        if (span == null) {
            return result;
        }
        Throwable childThrowable = (Throwable) span.getTag(KEY_ERROR_THROWABLE);
        String childId = (String) span.getTag(KEY_BEE_CHILD_ID);
        String childErrorPoint = (String) span.getTag(KEY_ERROR_POINT);
        String errorPoint = className + "." + methodName;
        String params = (String) span.getTag(KEY_PARAM);
        
        span.removeTag(KEY_ERROR_THROWABLE);
        span.removeTag(KEY_BEE_CHILD_ID);
        span.removeTag(KEY_ERROR_POINT);
        span.removeTag(KEY_PARAM);

        if (!ApmManager.getConfig(ProcessConfig.class).isEnabled()) {
            return null;
        }
        calculateSpend(span);
        
        if (span.getSpend() > ApmManager.getConfig(ProcessConfig.class).getSpend() && SamplingUtil.YES()) {
            sendParams(span.getId(), params);
            span.addTag("method", methodName).addTag("clazz", className);
            handleMethodSignature(span, (String) extraParam[0]);
            ApmManager.getConfig().fillEnvInfo(span);
            ReporterManager.report(span);
        }
        
        if(t != null || childThrowable != null) {
            handleError(span.getId(), errorPoint, t, childId, childErrorPoint, childThrowable);
        }
        return result;
    }

    public void handleError(String id, String errorPoint, Throwable t, String childId, String childErrorPoint, Throwable childThrowable) {
        Span parentSpan = SpanManager.getCurrentSpan(); 
        if (parentSpan == null) {      
            if (t == null && childThrowable != null) {
                sendError(childId, childErrorPoint, childThrowable);
            } else if (t != null && childThrowable == null) {
                sendError(id, errorPoint, t);
            } else {
                if (t == childThrowable || t.getCause() == childThrowable) {
                    sendError(id, errorPoint, t);
                } else {
                    sendError(id, errorPoint, t);
                    sendError(childId, childErrorPoint, childThrowable);
                }
            }
        } else {
            if (childThrowable == null && t != null) {
                
                parentSpan.addTag(KEY_ERROR_THROWABLE, t);
                parentSpan.addTag(KEY_BEE_CHILD_ID, id);
                parentSpan.addTag(KEY_ERROR_POINT, errorPoint);
                return;
            } else if (childThrowable != null && t == null) {
                sendError(childId, childErrorPoint, childThrowable);
            } else {
                parentSpan.addTag(KEY_ERROR_THROWABLE, t);
                parentSpan.addTag(KEY_BEE_CHILD_ID, id);
                parentSpan.addTag(KEY_ERROR_POINT, errorPoint);
                if (t != childThrowable && t.getCause() != childThrowable) {
                    sendError(childId, childErrorPoint, childThrowable);
                }
            }
        }
    }

    private void sendParams(String id, String params) {
        if (params == null) {
            return;
        }
        Span paramSpan = new Span(SpanType.PARAM);
        paramSpan.setId(id);
        paramSpan.addTag(Tags.PARAMS, params);
        ReporterManager.report(paramSpan);
    }

    public void sendError(String id, String errorPoint, Throwable t) {
        if (ApmManager.getConfig(ProcessConfig.class).isEnableError() && ApmManager.getConfig(ProcessConfig.class).checkErrorPoint(errorPoint)) {
            Span err = new Span(SpanType.ERROR);
            ApmManager.getConfig().fillEnvInfo(err);
            err.setId(id);
            err.setCorrelationId(TraceContext.getCorrelationId());
            err.addTag("desc", formatThrowable(t));
            ReporterManager.report(err);
        }
    }

    private void handleMethodSignature(Span span, String sign) {
        if (sign == null || sign.length() < 3) {
            return;
        }
        if (ApmManager.getConfig(ProcessConfig.class).isEnableMethodSign()) {
            span.addTag("msign", sign);
        }
    }

    private String formatThrowable(Throwable t) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        t.printStackTrace(new java.io.PrintWriter(buf, true));
        String expMessage = buf.toString();
        try {
            buf.close();
        } catch (IOException e) {
        }
        return expMessage;
    }

    private String collectParams(Object[] allArgs, String id, String point) {
        if (ApmManager.getConfig(ProcessConfig.class).isEnableParam() && allArgs != null && allArgs.length > 0 && ApmManager.getConfig(ProcessConfig.class).checkParamPoint(point)) {
            Object[] params = new Object[allArgs.length];
            for (int i = 0; i < allArgs.length; i++) {
                if (allArgs[i] != null && ApmManager.getConfig(ProcessConfig.class).isExcludeParamType(allArgs[i].getClass())) {
                    params[i] = "--";
                } else {
                    params[i] = allArgs[i];
                }
            }
            return Arrays.toString(params);
        }
        return null;
    }

}
