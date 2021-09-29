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
package work.ready.core.apm.collector.logger;

import work.ready.core.apm.model.CollectorConfig;
import work.ready.core.log.LogLevel;
import work.ready.core.server.Ready;
import work.ready.core.tools.StrUtil;

import java.util.*;

public class LoggerConfig extends CollectorConfig {
    public static final String name = "logger";
    private boolean enabled = true;
    private boolean errorRate = false;
    private String defaultLevel = LogLevel.DEBUG.name().toLowerCase();
    private List<String> points;

    public static int LEVEL_TRACE = 0;
    public static int LEVEL_DEBUG = LEVEL_TRACE + 1;
    public static int LEVEL_INFO = LEVEL_DEBUG + 1;
    public static int LEVEL_WARN = LEVEL_INFO + 1;
    public static int LEVEL_ERROR = LEVEL_WARN + 1;

    private static final Map<String, Integer> levelMap = new HashMap<>();
    private transient List<LoggerPoint> pointList;

    static {
        levelMap.put(LogLevel.TRACE.name().toLowerCase(), LEVEL_TRACE);
        levelMap.put(LogLevel.DEBUG.name().toLowerCase(), LEVEL_DEBUG);
        levelMap.put(LogLevel.INFO.name().toLowerCase(), LEVEL_INFO);
        levelMap.put(LogLevel.WARN.name().toLowerCase(), LEVEL_WARN);
        levelMap.put(LogLevel.ERROR.name().toLowerCase(), LEVEL_ERROR);
    }

    @Override
    public String getCollectorName() {
        return name;
    }

    @Override
    public List<Class<?>> getCollectorClasses() {
        List<Class<?>> collectors = new ArrayList<>();
        collectors.add(LoggerHandler.class);
        return collectors;
    }

    @Override
    public int getOrder() {
        return -99;
    }

    public int level(String level) {
        return levelMap.get(level);
    }

    private void initLoggerPoints() {
        if (pointList == null) {
            pointList = new ArrayList<LoggerPoint>();
        } else {
            pointList.clear();
        }
        if (points == null) {
            points = new ArrayList<>();
            
            points.add("work.ready.");
            Ready.getAppClass().forEach(app->points.add(app.getPackageName() + '.'));
        }
        for (String item : points) {
            if (StrUtil.notBlank(item)) {
                
                if (!item.contains("|")) {
                    item = item + "|" + defaultLevel;
                }
                String[] array = StrUtil.split(item, '|');
                Integer nLevel = levelMap.get(array[1]);
                
                if (nLevel == null) {
                    nLevel = LEVEL_DEBUG;
                }
                LoggerPoint point = new LoggerPoint(array[0], nLevel);
                pointList.add(point);
            }
        }
        pointList.sort((a, b) -> b.point.compareTo(a.point));
    }

    public boolean checkLevel(String point, String level) {
        int nLevel = levelMap.get(level);
        if(pointList == null) initLoggerPoints();
        int size = pointList.size();
        for (int i = 0; i < size; i++) {
            LoggerPoint lp = pointList.get(i);
            if (point.startsWith(lp.point)) {
                if (nLevel >= lp.level) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public boolean isErrorRate() {
        return errorRate;
    }

    public LoggerConfig setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public LoggerConfig setErrorRate(boolean errorRate) {
        this.errorRate = errorRate;
        return this;
    }

    public String getDefaultLevel() {
        return defaultLevel;
    }

    public LoggerConfig setDefaultLevel(String defaultLevel) {
        this.defaultLevel = defaultLevel;
        return this;
    }

    public List<String> getPoints() {
        return points;
    }

    public LoggerConfig setPoints(List<String> points) {
        this.points = points;
        this.pointList = null;
        return this;
    }

    private static class LoggerPoint {
        public String point;
        public int level;

        public LoggerPoint(String point, int level) {
            this.point = point;
            this.level = level;
        }
    }
}
