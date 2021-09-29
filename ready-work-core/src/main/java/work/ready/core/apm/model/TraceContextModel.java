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
package work.ready.core.apm.model;

public class TraceContextModel {
    private String traceabilityId;
    private String correlationId;
    private String parentId;
    private String target;
    private String tag;

    public String getTraceabilityId() {
        return traceabilityId;
    }

    public void setTraceabilityId(String traceabilityId) {
        this.traceabilityId = traceabilityId;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public TraceContextModel copy() {
        TraceContextModel model = new TraceContextModel();
        model.setTag(this.tag);
        model.setTarget(this.target);
        model.setTraceabilityId(this.traceabilityId);
        model.setCorrelationId(this.correlationId);
        model.setParentId(this.parentId);
        return model;
    }
}
