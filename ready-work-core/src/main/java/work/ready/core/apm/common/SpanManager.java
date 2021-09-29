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

package work.ready.core.apm.common;

import work.ready.core.apm.model.Span;
import work.ready.core.apm.model.SpanType;
import work.ready.core.apm.reporter.ReporterManager;
import work.ready.core.server.Ready;

import java.util.Stack;

public class SpanManager {
    private static final ThreadLocal<Stack<Span>> threadLocalSpan = new ThreadLocal<Stack<Span>>();

    private static Span createSpan(String spanType) {
        Stack<Span> stack = threadLocalSpan.get();
        if (stack == null) {
            stack = new Stack<>();
            threadLocalSpan.set(stack);
        }
        String parentId, correlationId,traceabilityId;
        if (stack.isEmpty()) {
            parentId = TraceContext.getParentId();
            if (parentId == null) {
                parentId = ApmConst.SRC_NO_APPLICATION;
                TraceContext.setParentId(parentId);
            }
            traceabilityId = TraceContext.getTraceabilityId();
            correlationId = TraceContext.getCorrelationId();
            if (correlationId == null) {
                correlationId = String.valueOf(Ready.getId());
                TraceContext.setCorrelationId(correlationId);
            }
        } else {
            Span parentSpan = stack.peek();
            parentId = parentSpan.getId();
            traceabilityId = parentSpan.getTraceabilityId();
            correlationId = parentSpan.getCorrelationId();
            TraceContext.setParentId(parentId);
        }
        Span span = new Span(spanType);
        span.setId(String.valueOf(Ready.getId())).setParentId(parentId).setTraceabilityId(traceabilityId).setCorrelationId(correlationId);
        return span;
    }

    public static Span createEntrySpan(String spanType) {
        Span span = createSpan(spanType);
        Stack<Span> stack = threadLocalSpan.get();
        stack.push(span);
        return span;
    }

    public static Span getExitSpan() {
        Stack<Span> stack = threadLocalSpan.get();
        if (stack == null || stack.isEmpty()) {
            TraceContext.clearAll();
            return null;
        }
        return stack.pop();
    }

    public static Span getCurrentSpan() {
        Stack<Span> stack = threadLocalSpan.get();
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return stack.peek();
    }

    public static Span createLocalSpan(String spanType) {
        return createSpan(spanType);
    }

    public static void createTopologySpan(String fromApp, String toApp) {
        if (fromApp == null || fromApp.isEmpty()) {
            fromApp = ApmConst.SRC_NO_APPLICATION;
        }
        Span span = new Span(SpanType.TOPOLOGY);
        span.setTraceabilityId(TraceContext.getTraceabilityId());
        span.setCorrelationId(TraceContext.getCorrelationId());
        span.addTag("from", fromApp);
        span.setParentId(TraceContext.getParentId());
        span.setApplication(toApp);
        span.setId(String.valueOf(Ready.getId()));

        ReporterManager.report(span);
    }

}
