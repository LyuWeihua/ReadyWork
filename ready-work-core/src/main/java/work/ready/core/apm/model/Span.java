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

import work.ready.core.server.Ready;

import java.util.Date;

public class Span{
    private Tags tags;
    private String type;
    private Date time;
    private String instance;
    private String application;
    private String env;
    private String parentId;
    private String correlationId;
    private String traceabilityId;
    private String id;
    private Long spend;
    private String port;
    private String ip;

    public Span(String spanType){
        setType(spanType);
        setTime(Ready.now());
    }

    public Object getTag(String key){
        if(tags == null){
            return null;
        }
        return tags.get(key);
    }

    public Span addTag(String key,Object val) {
        if(tags == null){
            tags = new Tags();
        }
        this.tags.put(key,val);
        return this;
    }

    public Span removeTag(String key){
        if(tags != null){
            this.tags.remove(key);
        }
        return this;
    }

    public String getType() {
        return type;
    }

    public Span setType(String type) {
        this.type = type;
        return this;
    }

    public Date getTime() {
        return time;
    }

    public Span setTime(Date time) {
        this.time = time;
        return this;
    }

    public String getInstance() {
        return instance;
    }

    public Span setInstance(String instance) {
        this.instance = instance;
        return this;
    }

    public String getApplication() {
        return application;
    }

    public Span setApplication(String application) {
        this.application = application;
        return this;
    }

    public String getParentId() {
        return parentId;
    }

    public Span setParentId(String parentId) {
        this.parentId = parentId;
        return this;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Span setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
        return this;
    }

    public String getTraceabilityId() {
        return traceabilityId;
    }

    public Span setTraceabilityId(String traceabilityId) {
        this.traceabilityId = traceabilityId;
        return this;
    }

    public String getId() {
        return id;
    }

    public Span setId(String id) {
        this.id = id;
        return this;
    }

    public Long getSpend() {
        return spend;
    }

    public void setSpend(Long spend) {
        this.spend = spend;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public Tags getTags() {
        return tags;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("{")
                .append("tags=").append(tags)
                .append(", type='").append(type).append('\'')
                .append(", time=").append(time)
                .append(", instance='").append(instance).append('\'')
                .append(", application='").append(application).append('\'')
                .append(", env='").append(env).append('\'')
                .append(", parentId='").append(parentId).append('\'')
                .append(", correlationId='").append(correlationId).append('\'')
                .append(", traceabilityId='").append(traceabilityId).append('\'')
                .append(", id='").append(id).append('\'')
                .append(", spend=").append(spend)
                .append(", port='").append(port).append('\'')
                .append(", ip='").append(ip).append('\'')
                .append('}').toString();
    }
}
