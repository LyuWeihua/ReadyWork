/**
 *
 * Original work copyright bee-apm
 * Modified work Copyright (c) 2020 WeiHua Lyu [ready.work]
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
import work.ready.core.apm.model.TraceContextModel;
import work.ready.core.server.Ready;

public class TraceContext {
    private static final InheritableThreadLocal<TraceContextModel> traceContext = new InheritableThreadLocal<TraceContextModel>();

    public static void clearAll() {
        traceContext.remove();
    }

    public static void set(TraceContextModel model) {
        traceContext.set(model);
    }

    public static String getTraceabilityId() {
        return getOrNew().getTraceabilityId();
    }

    public static void setTraceabilityId(String traceabilityId) {
        getOrNew().setTraceabilityId(traceabilityId);
    }

    public static String getParentId() {
        return getOrNew().getParentId();
    }

    public static void setParentId(String parentId) {
        getOrNew().setParentId(parentId);
    }

    public static String getCorrelationId() {
        TraceContextModel model = getOrNew();
        String correlationId = model.getCorrelationId();
        if (correlationId == null) {
            correlationId = String.valueOf(Ready.getId());
            model.setCorrelationId(correlationId);
        }
        return correlationId;
    }

    public static void setCorrelationId(String correlationId) {
        getOrNew().setCorrelationId(correlationId);
    }

    public static String getTarget() {
        return getOrNew().getTarget();
    }

    public static void setTarget(String target) {
        getOrNew().setTarget(target);
    }

    public static String getTag() {
        return getOrNew().getTag();
    }

    public static void setTag(String tag) {
        getOrNew().setTag(tag);
    }

    public static String getCurrentId() {
        Span span = SpanManager.getCurrentSpan();
        if (span == null) {
            return ApmConst.SRC_NO_APPLICATION;
        }
        return span.getId();
    }

    public static TraceContextModel getOrNew() {
        TraceContextModel model = traceContext.get();
        if (model == null) {
            model = new TraceContextModel();
            traceContext.set(model);
        }
        return model;
    }
}
