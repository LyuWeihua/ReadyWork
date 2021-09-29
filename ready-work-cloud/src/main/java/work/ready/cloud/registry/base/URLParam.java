/**
 *
 * Original work copyright (c) 2016 Network New Technologies Inc.
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
package work.ready.cloud.registry.base;

import work.ready.core.server.Constant;
import work.ready.core.server.Ready;

public enum URLParam {
    
    environment(Constant.TAG_ENVIRONMENT, "null"), 
    project("project", Constant.DEFAULT_PROJECT),
    
    projectVersion("projectVersion", Constant.DEFAULT_VERSION),
    serviceVersion("serviceVersion", Constant.DEFAULT_VERSION),
    
    requestTimeout("requestTimeout", 200),
    
    requestIdFromClient("requestIdFromClient", 0),
    
    connectTimeout("connectTimeout", 1000),
    
    minWorkerThread("minWorkerThread", 20),
    
    maxWorkerThread("maxWorkerThread", 200),
    
    minClientConnection("minClientConnection", 2),
    
    maxClientConnection("maxClientConnection", 10),
    
    maxContentLength("maxContentLength", 10 * 1024 * 1024),
    
    maxServerConnection("maxServerConnection", 100000),
    
    poolLifo("poolLifo", true),

    lazyInit("lazyInit", false),
    
    shareChannel("shareChannel", false),

    serialize("serialization", "hessian2"),
    
    codec("codec", "ready"),
    
    endpointFactory("endpointFactory", "ready"),
    
    heartbeatFactory("heartbeatFactory", "ready"),
    
    switcherService("switcherService", "localSwitcherService"),

    group("group", "default"),
    clientGroup("clientGroup", "default"),
    accessLog("accessLog", false),

    actives("actives", 0),

    refreshTimestamp("refreshTimestamp", 0),
    nodeType("nodeType", Constant.NODE_TYPE_APPLICATION),
    nodeId("nodeId", ""),
    nodeConsistentId("nodeConsistentId", ""),

    healthCheck("healthCheck", false),
    healthCheckPath("healthCheckPath", ""),
    crucialCheckPath("crucialCheckPath", ""),

    export("export", ""),
    embed("embed", ""),

    registryRetryPeriod("registryRetryPeriod", 30 * Constant.SECOND_MILLS),

    cloud("cloud", Constant.DEFAULT_VALUE),
    loadbalance("loadbalance", "activeWeight"),
    haStrategy("haStrategy", "failover"),
    protocol("protocol", Constant.PROTOCOL_DEFAULT),
    path("path", ""),
    host("host", ""),
    port("port", 0),
    iothreads("iothreads", Runtime.getRuntime().availableProcessors() + 1),
    workerQueueSize("workerQueueSize", 0),
    acceptConnections("acceptConnections", 0),
    filter("filter", ""),

    application("application", Constant.FRAMEWORK_NAME),
    module("module", Constant.FRAMEWORK_NAME),

    retries("retries", 0),
    async("async", false),
    mock("mock", "false"),
    mean("mean", "2"),
    p90("p90", "4"),
    p99("p99", "10"),
    p999("p999", "70"),
    errorRate("errorRate", "0.01"),
    check("crucialCheck", "true"),
    directUrl("directUrl", ""),
    registrySessionTimeout("registrySessionTimeout", 1 * Constant.MINUTE_MILLS),

    register("register", true),
    subscribe("subscribe", true),
    throwException("throwException", "true"),

    localServiceAddress("localServiceAddress", "");

    private final String name;
    private final String value;
    private final long longValue;
    private final int intValue;
    private final boolean boolValue;

    URLParam(String name, String value) {
        this.name = name;
        this.value = value;
        this.longValue = 0L;
        this.intValue = 0;
        this.boolValue = false;

    }

    URLParam(String name, long longValue) {
        this.name = name;
        this.value = String.valueOf(longValue);
        this.longValue = longValue;
        this.intValue = 0;
        this.boolValue = false;
    }

    URLParam(String name, int intValue) {
        this.name = name;
        this.value = String.valueOf(intValue);
        this.intValue = intValue;
        this.longValue = 0L;
        this.boolValue = false;

    }

    URLParam(String name, boolean boolValue) {
        this.name = name;
        this.value = String.valueOf(boolValue);
        this.boolValue = boolValue;
        this.longValue = 0L;
        this.intValue = 0;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public int getIntValue() {
        return intValue;
    }

    public long getLongValue() {
        return longValue;
    }

    public boolean getBooleanValue() {
        return boolValue;
    }

}
