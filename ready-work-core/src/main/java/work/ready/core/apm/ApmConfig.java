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

package work.ready.core.apm;

import work.ready.core.apm.collector.http.client.HttpClientConfig;
import work.ready.core.apm.collector.http.server.WebServerConfig;
import work.ready.core.apm.collector.jdbc.JdbcConfig;
import work.ready.core.apm.collector.logger.LoggerConfig;
import work.ready.core.apm.collector.process.ProcessConfig;
import work.ready.core.apm.model.Span;
import work.ready.core.apm.reporter.ReporterConfig;
import work.ready.core.config.BaseConfig;
import work.ready.core.log.LogLevel;

import java.util.HashMap;
import java.util.Map;

public class ApmConfig extends BaseConfig {
    private boolean enabled = false;
    private LogLevel logLevel = LogLevel.ERROR;
    private boolean logConsole = false;
    private int rate = 10000;
    private String instance = "unknown";
    private String application = "unknown";
    private String env = "unknown";
    private String ip; 
    private String port = "0";
    private int heartbeatPeriod = 60;
    private int jvmPeriod = 60;

    private ReporterConfig reporter = new ReporterConfig();

    private Map<String, Object> collector = new HashMap<>();
    private transient boolean collectorInit = false;

    public boolean isEnabled() {
        return enabled;
    }

    public ApmConfig setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public ApmConfig setLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
        return this;
    }

    public ApmConfig setLogConsole(Boolean logConsole) {
        this.logConsole = logConsole;
        return this;
    }

    public ApmConfig setRate(int rate) {
        this.rate = rate;
        return this;
    }

    public ApmConfig setInstance(String instance) {
        this.instance = instance;
        return this;
    }

    public ApmConfig setApplication(String application) {
        this.application = application;
        return this;
    }

    public ApmConfig setEnv(String env) {
        this.env = env;
        return this;
    }

    public ApmConfig setIp(String ip) {
        this.ip = ip;
        return this;
    }

    public ApmConfig setPort(String port) {
        this.port = port;
        return this;
    }

    public ApmConfig setHeartbeatPeriod(int heartbeatPeriod) {
        this.heartbeatPeriod = heartbeatPeriod;
        return this;
    }

    public ApmConfig setJvmPeriod(int jvmPeriod) {
        this.jvmPeriod = jvmPeriod;
        return this;
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

    public boolean isLogConsole() {
        return logConsole;
    }

    public int getRate() {
        return rate;
    }

    public String getInstance() {
        return instance;
    }

    public String getApplication() {
        return application;
    }

    public String getIp() {
        return ip;
    }

    public String getPort() {
        return port;
    }

    public String getEnv() {
        return env;
    }

    public int getHeartbeatPeriod() {
        return heartbeatPeriod;
    }

    public int getJvmPeriod() {
        return jvmPeriod;
    }

    public ReporterConfig getReporter() {
        return reporter;
    }

    public ApmConfig setReporter(ReporterConfig reporter) {
        this.reporter = reporter;
        return this;
    }

    public Map<String, Object> getCollector() {
        
        defaultCollectorConfigInit();
        return collector;
    }

    private void defaultCollectorConfigInit() {
        if(collectorInit) return;
        collectorInit = true;
        if(collector == null) collector = new HashMap<>();

        Object map = collector.get(HttpClientConfig.name);
        if(!(map instanceof Map)){
            collector.put(HttpClientConfig.name, new HashMap<>(Map.of("config", HttpClientConfig.class.getCanonicalName(), "enabled", true)));
        }
        Map<String, Object> config = (Map<String, Object>) collector.get(HttpClientConfig.name);
        config.computeIfAbsent("config", k->HttpClientConfig.class.getCanonicalName());
        config.computeIfAbsent("enabled", k->true);

        map = collector.get(JdbcConfig.name);
        if(!(map instanceof Map)){
            collector.put(JdbcConfig.name, new HashMap<>(Map.of("config", JdbcConfig.class.getCanonicalName(), "enabled", true)));
        }
        config = (Map<String, Object>) collector.get(JdbcConfig.name);
        config.computeIfAbsent("config", k->JdbcConfig.class.getCanonicalName());
        config.computeIfAbsent("enabled", k->true);

        map = collector.get(LoggerConfig.name);
        if(!(map instanceof Map)){
            collector.put(LoggerConfig.name, new HashMap<>(Map.of("config", LoggerConfig.class.getCanonicalName(), "enabled", true)));
        }
        config = (Map<String, Object>) collector.get(LoggerConfig.name);
        config.computeIfAbsent("config", k->LoggerConfig.class.getCanonicalName());
        config.computeIfAbsent("enabled", k->true);

        map = collector.get(ProcessConfig.name);
        if(!(map instanceof Map)){
            collector.put(ProcessConfig.name, new HashMap<>(Map.of("config", ProcessConfig.class.getCanonicalName(), "enabled", true)));
        }
        config = (Map<String, Object>) collector.get(ProcessConfig.name);
        config.computeIfAbsent("config", k->ProcessConfig.class.getCanonicalName());
        config.computeIfAbsent("enabled", k->true);

        map = collector.get(WebServerConfig.name);
        if(!(map instanceof Map)){
            collector.put(WebServerConfig.name, new HashMap<>(Map.of("config", WebServerConfig.class.getCanonicalName(), "enabled", true)));
        }
        config = (Map<String, Object>) collector.get(WebServerConfig.name);
        config.computeIfAbsent("config", k-> WebServerConfig.class.getCanonicalName());
        config.computeIfAbsent("enabled", k->true);
    }

    public ApmConfig setCollector(Map<String, Object> collector) {
        collectorInit = false;
        this.collector = collector;
        return this;
    }

    public void fillEnvInfo(Span span){
        span.setIp(getIp());
        span.setPort(getPort());
        span.setInstance(getInstance());
        span.setApplication(getApplication());
        span.setEnv(getEnv());
    }
}
