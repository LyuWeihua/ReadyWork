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

package work.ready.core.apm.collector.process;

import work.ready.core.aop.transformer.match.InterceptorPoint;
import work.ready.core.apm.model.CollectorConfig;

import java.util.*;
import java.util.regex.Pattern;

public class ProcessConfig extends CollectorConfig {
    public static final String name = "process";
    private boolean enableParam = true;
    private boolean enabled = true;
    private boolean enableMethodSign = true;
    private long spend = -1;
    private Boolean enableError = true;
    private Map<String, InterceptorPoint> point = new HashMap<>();
    private List<String> excludeParamTypePrefix = new ArrayList<>();
    private List<String> includeErrorPointPrefix = new ArrayList<>();
    private List<String> excludeErrorPointPrefix = new ArrayList<>();
    private List<String> includePointMatches = new ArrayList<>();
    private List<String> excludePointMatches = new ArrayList<>();

    private final transient List<Pattern> excludePointMatchesList = new ArrayList<Pattern>();
    private final transient List<Pattern> includePointMatchesList = new ArrayList<Pattern>();

    @Override
    public String getCollectorName() {
        return name;
    }

    @Override
    public List<Class<?>> getCollectorClasses() {
        List<Class<?>> classes = new ArrayList<>();
        classes.add(ProcessInitiator.class);
        return classes;
    }

    @Override
    public int getOrder() {
        return 1;
    }

    public ProcessConfig setEnableParam(boolean enableParam) {
        this.enableParam = enableParam;
        return this;
    }

    public ProcessConfig setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public ProcessConfig setEnableMethodSign(boolean enableMethodSign) {
        this.enableMethodSign = enableMethodSign;
        return this;
    }

    public ProcessConfig setSpend(long spend) {
        this.spend = spend;
        return this;
    }

    public ProcessConfig setEnableError(boolean enableError) {
        this.enableError = enableError;
        return this;
    }

    public Map<String, InterceptorPoint> getPoint() {
        return point;
    }

    public ProcessConfig setPoint(Map<String, InterceptorPoint> point) {
        this.point = point;
        return this;
    }

    public List<String> getExcludeParamTypePrefix() {
        return excludeParamTypePrefix;
    }

    public ProcessConfig setExcludeParamTypePrefix(List<String> excludeParamTypePrefix) {
        this.excludeParamTypePrefix = excludeParamTypePrefix;
        return this;
    }

    public List<String> getIncludeErrorPointPrefix() {
        return includeErrorPointPrefix;
    }

    public ProcessConfig setIncludeErrorPointPrefix(List<String> includeErrorPointPrefix) {
        this.includeErrorPointPrefix = includeErrorPointPrefix;
        return this;
    }

    public List<String> getExcludeErrorPointPrefix() {
        return excludeErrorPointPrefix;
    }

    public ProcessConfig setExcludeErrorPointPrefix(List<String> excludeErrorPointPrefix) {
        this.excludeErrorPointPrefix = excludeErrorPointPrefix;
        return this;
    }

    public List<String> getIncludePointMatches() {
        return includePointMatches;
    }

    public ProcessConfig setIncludePointMatches(List<String> includePointMatches) {
        this.includePointMatches = includePointMatches;
        this.includePointMatchesList.clear();
        if (includePointMatches != null && includePointMatches.size() > 0) {
            int size = includePointMatches.size();
            for (int i = 0; i < size; i++) {
                includePointMatchesList.add(Pattern.compile(includePointMatches.get(i)));
            }
        }
        return this;
    }

    public List<String> getExcludePointMatches() {
        return excludePointMatches;
    }

    public ProcessConfig setExcludePointMatches(List<String> excludePointMatches) {
        this.excludePointMatches = excludePointMatches;
        this.excludePointMatchesList.clear();
        if (excludePointMatches != null && excludePointMatches.size() > 0) {
            int size = excludePointMatches.size();
            for (int i = 0; i < size; i++) {
                excludePointMatchesList.add(Pattern.compile(excludePointMatches.get(i)));
            }
        }
        return this;
    }

    public boolean isEnableParam() {
        return enableParam;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public boolean isEnableError() {
        return enableError;
    }

    public boolean isEnableMethodSign() {
        return enableMethodSign;
    }

    public boolean isExcludeParamType(Class<?> clazz) {
        String name = clazz.getName();
        Iterator<String> it = excludeParamTypePrefix.iterator();
        while (it.hasNext()) {
            if (name.startsWith(it.next())) {
                return true;
            }
        }
        return false;
    }

    public long getSpend() {
        return spend;
    }

    public boolean checkErrorPoint(String point) {
        if (!enableError) {
            return false;
        }
        
        Iterator<String> exclude = excludeErrorPointPrefix.iterator();
        while (exclude.hasNext()) {
            if (point.startsWith(exclude.next())) {
                return false;
            }
        }
        
        Iterator<String> include = includeErrorPointPrefix.iterator();
        while (include.hasNext()) {
            if (point.startsWith(include.next())) {
                return true;
            }
        }
        return false;
    }

    public boolean checkParamPoint(String point) {
        int excludeSize = excludePointMatchesList.size();
        for (int i = 0; i < excludeSize; i++) {
            if (excludePointMatchesList.get(i).matcher(point).matches()) {
                return false;
            }
        }
        int includeSize = includePointMatchesList.size();
        if (includeSize == 0) {
            return true;
        }
        
        for (int i = 0; i < includeSize; i++) {
            if (includePointMatchesList.get(i).matcher(point).matches()) {
                return true;
            }
        }
        
        return false;
    }
}
