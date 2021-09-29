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

package work.ready.core.apm.reporter;

import work.ready.core.apm.reporter.reporter.console.ConsoleReporter;
import work.ready.core.apm.reporter.reporter.database.DatabaseReporter;
import work.ready.core.apm.reporter.reporter.elasticsearch.ElasticsearchReporter;
import work.ready.core.apm.reporter.reporter.http.HttpReporter;
import work.ready.core.config.BaseConfig;

import java.util.ArrayList;
import java.util.List;

public class ReporterConfig extends BaseConfig {

    private int idleSleep = 100;
    private int batchSize = 100;
    private int threadNum = 1;
    private String defaultReporter = ConsoleReporter.name;
    private List<String> reporter;
    private int queueSize = 1000;
    private boolean debug = false;

    private String datasource;  

    private transient boolean reporterInit = false;

    public int getIdleSleep() {
        return idleSleep;
    }

    public ReporterConfig setIdleSleep(int idleSleep) {
        this.idleSleep = idleSleep;
        return this;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public ReporterConfig setBatchSize(int batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    public int getThreadNum() {
        return threadNum;
    }

    public ReporterConfig setThreadNum(int threadNum) {
        this.threadNum = threadNum;
        return this;
    }

    public String getDefaultReporter() {
        return defaultReporter;
    }

    public ReporterConfig setDefaultReporter(String defaultReporter) {
        this.defaultReporter = defaultReporter;
        return this;
    }

    public List<String> getReporter() {
        if(!reporterInit) {
            reporterInit = true;
            if(reporter == null) {
                reporter = new ArrayList<>();
            }
            reporter.add(ConsoleReporter.class.getCanonicalName());
            reporter.add(DatabaseReporter.class.getCanonicalName());
            reporter.add(ElasticsearchReporter.class.getCanonicalName());
            reporter.add(HttpReporter.class.getCanonicalName());
        }
        return reporter;
    }

    public ReporterConfig setReporter(List<String> reporter) {
        reporterInit = false;
        this.reporter = reporter;
        return this;
    }

    public ReporterConfig setReporter(String reporter) {
        getReporter().add(reporter);
        return this;
    }

    public String getDatasource() {
        return datasource;
    }

    public ReporterConfig setDatasource(String datasource) {
        this.datasource = datasource;
        return this;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public ReporterConfig setQueueSize(int queueSize) {
        this.queueSize = queueSize;
        return this;
    }

    public boolean isDebug() {
        return debug;
    }

    public ReporterConfig setDebug(boolean debug) {
        this.debug = debug;
        return this;
    }
}
