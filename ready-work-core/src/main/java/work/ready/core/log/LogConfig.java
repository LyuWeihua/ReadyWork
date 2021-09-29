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

package work.ready.core.log;

import work.ready.core.config.BaseConfig;

import java.util.HashMap;
import java.util.Map;

public class LogConfig extends BaseConfig {

    private String defaultLogLevel = LogLevel.ALL.name();
    
    private String igniteLogLevel = LogLevel.INFO.name();
    private Map<String, String> filter;
    private Map<String, String> extFilter;

    private Boolean fileLogEnabled = true;
    private Boolean separatedFileLog = false;
    private String fileLogLevel = LogLevel.ALL.name();
    private String fileLogPath = "logs/";
    
    private String fileLogNamePattern = "Ready.%g.log";
    private Integer fileLogLimit = 10000000;
    private Integer fileLogCount = 10;
    private Integer fileLogMaxLocks = 100;
    private Integer corePoolSize = 2;
    private Integer maxThread = 10; 
    private Integer queueSize = 10;
    private Integer threadKeepalive = 60; 

    private String consoleLogLevel = LogLevel.ALL.name();

    private String consoleLogTraceColor = "WHITE";
    private String consoleLogDebugColor = "CYAN";
    private String consoleLogInfoColor = "WHITE";
    private String consoleLogWarnColor = "YELLOW";
    private String consoleLogErrorColor = "RED";

    public String getDefaultLogLevel() {
        return defaultLogLevel;
    }

    public LogConfig setDefaultLogLevel(String defaultLogLevel) {
        this.defaultLogLevel = defaultLogLevel;
        return this;
    }

    public String getIgniteLogLevel() {
        return igniteLogLevel;
    }

    public LogConfig setIgniteLogLevel(String igniteLogLevel) {
        this.igniteLogLevel = igniteLogLevel;
        return this;
    }

    public Map<String, String> getFilter() {
        return filter;
    }

    public LogConfig setFilter(Map<String, String> filter) {
        this.filter = filter;
        return this;
    }

    public LogConfig addFilter(String name, String level) {
        if(this.filter == null) this.filter = new HashMap<>();
        this.filter.put(name, level);
        return this;
    }

    public Map<String, String> getExtFilter() {
        return extFilter;
    }

    public LogConfig setExtFilter(Map<String, String> extFilter) {
        this.extFilter = extFilter;
        return this;
    }

    public LogConfig addExtFilter(String namePrefix, String level) {
        if(this.extFilter == null) this.extFilter = new HashMap<>();
        this.extFilter.put(namePrefix, level);
        return this;
    }

    public Boolean isFileLogEnabled() {
        return fileLogEnabled;
    }

    public LogConfig setFileLogEnabled(boolean fileLogEnabled) {
        this.fileLogEnabled = fileLogEnabled;
        return this;
    }

    public Boolean isSeparatedFileLog() {
        return separatedFileLog;
    }

    public void setSeparatedFileLog(boolean separated) {
        this.separatedFileLog = separated;
    }

    public String getFileLogLevel() {
        return fileLogLevel;
    }

    public String getFileLogPath() {
        return fileLogPath;
    }

    public LogConfig setFileLogPath(String fileLogPath) {
        this.fileLogPath = fileLogPath;
        return this;
    }

    public LogConfig setFileLogLevel(String fileLogLevel) {
        this.fileLogLevel = fileLogLevel;
        return this;
    }

    public String getFileLogNamePattern() {
        return fileLogNamePattern;
    }

    public LogConfig setFileLogNamePattern(String fileLogNamePattern) {
        this.fileLogNamePattern = fileLogNamePattern;
        return this;
    }

    public Integer getFileLogLimit() {
        return fileLogLimit;
    }

    public LogConfig setFileLogLimit(Integer fileLogLimit) {
        this.fileLogLimit = fileLogLimit;
        return this;
    }

    public Integer getFileLogCount() {
        return fileLogCount;
    }

    public LogConfig setFileLogCount(Integer fileLogCount) {
        this.fileLogCount = fileLogCount;
        return this;
    }

    public Integer getFileLogMaxLocks() {
        return fileLogMaxLocks;
    }

    public LogConfig setFileLogMaxLocks(Integer fileLogMaxLocks) {
        this.fileLogMaxLocks = fileLogMaxLocks;
        return this;
    }

    public Integer getCorePoolSize() {
        return corePoolSize;
    }

    public LogConfig setCorePoolSize(Integer corePoolSize) {
        this.corePoolSize = corePoolSize;
        return this;
    }

    public Integer getMaxThread() {
        return maxThread;
    }

    public LogConfig setMaxThread(Integer maxThread) {
        this.maxThread = maxThread;
        return this;
    }

    public Integer getQueueSize() {
        return queueSize;
    }

    public LogConfig setQueueSize(Integer queueSize) {
        this.queueSize = queueSize;
        return this;
    }

    public Integer getThreadKeepalive() {
        return threadKeepalive;
    }

    public LogConfig setThreadKeepalive(Integer threadKeepalive) {
        this.threadKeepalive = threadKeepalive;
        return this;
    }

    public String getConsoleLogLevel() {
        return consoleLogLevel;
    }

    public LogConfig setConsoleLogLevel(String consoleLogLevel) {
        this.consoleLogLevel = consoleLogLevel;
        return this;
    }

    public String getConsoleLogDebugColor() {
        return consoleLogDebugColor;
    }

    public LogConfig setConsoleLogDebugColor(String consoleLogDebugColor) {
        this.consoleLogDebugColor = consoleLogDebugColor;
        return this;
    }

    public String getConsoleLogTraceColor() {
        return consoleLogTraceColor;
    }

    public LogConfig setConsoleLogTraceColor(String consoleLogTraceColor) {
        this.consoleLogTraceColor = consoleLogTraceColor;
        return this;
    }

    public String getConsoleLogInfoColor() {
        return consoleLogInfoColor;
    }

    public LogConfig setConsoleLogInfoColor(String consoleLogInfoColor) {
        this.consoleLogInfoColor = consoleLogInfoColor;
        return this;
    }

    public String getConsoleLogWarnColor() {
        return consoleLogWarnColor;
    }

    public LogConfig setConsoleLogWarnColor(String consoleLogWarnColor) {
        this.consoleLogWarnColor = consoleLogWarnColor;
        return this;
    }

    public String getConsoleLogErrorColor() {
        return consoleLogErrorColor;
    }

    public LogConfig setConsoleLogErrorColor(String consoleLogErrorColor) {
        this.consoleLogErrorColor = consoleLogErrorColor;
        return this;
    }
}
