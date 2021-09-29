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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.*;

import work.ready.core.log.BaseColorConsoleHandler.Color;

public final class LogFactory {
    private static final Map<Level, String> TO_SLF4J_LEVEL_MAP = new HashMap<>();
    private static final Map<String, Level> FROM_SLF4J_LEVEL_MAP = new HashMap<>();
    private static final Map<String, Color> COLOR_MAP = new HashMap<>();

    private static java.util.logging.Logger rootLogger;
    private static ConsoleHandler consoleHandler;
    private static List<AsyncFileHandler> fileHandlers = new ArrayList<>();
    static {
        FROM_SLF4J_LEVEL_MAP.put(LogLevel.ALL.name(), Level.ALL);
        FROM_SLF4J_LEVEL_MAP.put(LogLevel.TRACE.name(), Level.FINEST);
        FROM_SLF4J_LEVEL_MAP.put(LogLevel.DEBUG.name(), Level.FINE);
        FROM_SLF4J_LEVEL_MAP.put(LogLevel.INFO.name(), Level.INFO);
        FROM_SLF4J_LEVEL_MAP.put(LogLevel.WARN.name(), Level.WARNING);
        FROM_SLF4J_LEVEL_MAP.put(LogLevel.ERROR.name(), Level.SEVERE);
        FROM_SLF4J_LEVEL_MAP.put(LogLevel.OFF.name(), Level.OFF);

        TO_SLF4J_LEVEL_MAP.put(Level.ALL, LogLevel.ALL.name());
        TO_SLF4J_LEVEL_MAP.put(Level.FINEST, LogLevel.TRACE.name());
        TO_SLF4J_LEVEL_MAP.put(Level.FINER, LogLevel.DEBUG.name());
        TO_SLF4J_LEVEL_MAP.put(Level.FINE, LogLevel.DEBUG.name());
        TO_SLF4J_LEVEL_MAP.put(Level.CONFIG, LogLevel.INFO.name());
        TO_SLF4J_LEVEL_MAP.put(Level.INFO, LogLevel.INFO.name());
        TO_SLF4J_LEVEL_MAP.put(Level.WARNING, LogLevel.WARN.name());
        TO_SLF4J_LEVEL_MAP.put(Level.SEVERE, LogLevel.ERROR.name());
        TO_SLF4J_LEVEL_MAP.put(Level.OFF, LogLevel.OFF.name());

        COLOR_MAP.put("BLACK", Color.BLACK);
        COLOR_MAP.put("RED", Color.RED);
        COLOR_MAP.put("GREEN", Color.GREEN);
        COLOR_MAP.put("YELLOW", Color.YELLOW);
        COLOR_MAP.put("BLUE", Color.BLUE);
        COLOR_MAP.put("MAGENTA", Color.MAGENTA);
        COLOR_MAP.put("CYAN", Color.CYAN);
        COLOR_MAP.put("WHITE", Color.WHITE);
        COLOR_MAP.put("BLACK_BRIGHT", Color.BLACK_BOLD_BRIGHT);
        COLOR_MAP.put("RED_BRIGHT", Color.RED_BOLD_BRIGHT);
        COLOR_MAP.put("GREEN_BRIGHT", Color.GREEN_BOLD_BRIGHT);
        COLOR_MAP.put("YELLOW_BRIGHT", Color.YELLOW_BOLD_BRIGHT);
        COLOR_MAP.put("BLUE_BRIGHT", Color.BLUE_BOLD_BRIGHT);
        COLOR_MAP.put("MAGENTA_BRIGHT", Color.MAGENTA_BOLD_BRIGHT);
        COLOR_MAP.put("CYAN_BRIGHT", Color.CYAN_BOLD_BRIGHT);
        COLOR_MAP.put("WHITE_BRIGHT", Color.WHITE_BOLD_BRIGHT);

        System.setProperty("sun.util.logging.disableCallerCheck", "true");
        System.setProperty("java.util.logging.manager", LogManager.class.getName());
        if(!LogManager.getLogManager().getClass().equals(work.ready.core.log.LogManager.class)) {
            System.err.println("Ready LogFactory should be called earlier than other logger.");
        } else {
            final LogManager logManager = (LogManager) LogManager.getLogManager();
            try {
                read(logManager, create());
            } catch (IOException e) {}
        }

        rootLogger = java.util.logging.Logger.getLogger("");

        rootLogger.setUseParentHandlers(false);
        Handler[] handlers = rootLogger.getHandlers();

        for (Handler handler : handlers) {
            if(handler.getClass().equals(ConsoleHandler.class)) {
                rootLogger.removeHandler(handler);
            }
        }
        consoleHandler = new AnsiColorConsoleHandler();
        rootLogger.addHandler(consoleHandler);
        consoleHandler.setLevel(Level.ALL);
    }

    public static Level slf4jToLevel(String slf4jLevel){
        return FROM_SLF4J_LEVEL_MAP.get(slf4jLevel);
    }

    public static String levelToSlf4j(Level level){
        return TO_SLF4J_LEVEL_MAP.get(level);
    }

    public static void addHandler(Handler handler, String defaultLevel) {
        rootLogger.addHandler(handler);
        Level level = slf4jToLevel(defaultLevel);
        handler.setLevel(level == null ? Level.ALL : level);
    }

    public static void removeHandler(Handler handler) {
        rootLogger.removeHandler(handler);
    }

    public static void setConsoleLevel(String level){
        if(FROM_SLF4J_LEVEL_MAP.keySet().contains(level)){
            consoleHandler.setLevel(FROM_SLF4J_LEVEL_MAP.get(level));
        } else {
            getLog(LogFactory.class).error("Unknown console log level: " + level);
            consoleHandler.setLevel(Level.ALL);
        }
    }

    public static void setFileLevel(String level){
        if(fileHandlers.isEmpty()) return;
        if(FROM_SLF4J_LEVEL_MAP.keySet().contains(level)){
            fileHandlers.forEach(h->{
                if(!h.isSeparated()) {
                    h.setLevel(FROM_SLF4J_LEVEL_MAP.get(level));
                }
            });
        } else {
            getLog(LogFactory.class).error("Unknown file log level: " + level);
        }
    }

    public static void applyConfig(LogConfig config) {
        String level = Optional.ofNullable(config.getDefaultLogLevel()).orElse("ALL").toUpperCase();
        if(FROM_SLF4J_LEVEL_MAP.keySet().contains(level)){
            rootLogger.setLevel(FROM_SLF4J_LEVEL_MAP.get(level));
        } else {
            getLog(LogFactory.class).warn("Unknown default log level: " + level);
            rootLogger.setLevel(Level.ALL);
        }

        String consoleLogLevel = Optional.ofNullable(config.getConsoleLogLevel()).orElse("ALL").toUpperCase();
        if(FROM_SLF4J_LEVEL_MAP.keySet().contains(consoleLogLevel)){
            consoleHandler.setLevel(FROM_SLF4J_LEVEL_MAP.get(consoleLogLevel));
        } else {
            getLog(LogFactory.class).error("Unknown console log level: " + consoleLogLevel);
            consoleHandler.setLevel(Level.ALL);
        }

        Optional.ofNullable(config.getConsoleLogErrorColor()).ifPresent((color) -> {
            if(COLOR_MAP.containsKey(color)) {
                BaseColorConsoleHandler.COLOR_SEVERE = COLOR_MAP.get(color).toString();
            }
        });
        Optional.ofNullable(config.getConsoleLogWarnColor()).ifPresent((color) -> {
            if(COLOR_MAP.containsKey(color)) {
                BaseColorConsoleHandler.COLOR_WARNING = COLOR_MAP.get(color).toString();
            }
        });
        Optional.ofNullable(config.getConsoleLogInfoColor()).ifPresent((color) -> {
            if(COLOR_MAP.containsKey(color)) {
                BaseColorConsoleHandler.COLOR_INFO = COLOR_MAP.get(color).toString();
            }
        });
        Optional.ofNullable(config.getConsoleLogDebugColor()).ifPresent((color) -> {
            if(COLOR_MAP.containsKey(color)) {
                BaseColorConsoleHandler.COLOR_FINE = COLOR_MAP.get(color).toString();
            }
        });
        Optional.ofNullable(config.getConsoleLogTraceColor()).ifPresent((color) -> {
            if(COLOR_MAP.containsKey(color)) {
                BaseColorConsoleHandler.COLOR_FINEST = COLOR_MAP.get(color).toString();
            }
        });

        setupAsyncLogger(config);
        setupFilters(config);
    }

    private static void setupFilters(LogConfig config) {
        if(config != null && config.getFilter() != null) {
            config.getFilter().forEach((name, level) -> {
                if (FROM_SLF4J_LEVEL_MAP.keySet().contains(level)) {
                    LogManager.setProperty(name.endsWith(".level") ? name : name + ".level", FROM_SLF4J_LEVEL_MAP.get(level).getName());
                } else {
                    getLog(LogFactory.class).error("Unknown log level for %s: %s", name, level);
                }
            });
        }

        if(config != null && config.getExtFilter() != null) {
            config.getExtFilter().forEach((namePrefix, level)-> {
                if (FROM_SLF4J_LEVEL_MAP.keySet().contains(level)) {
                    Level internalLevel = FROM_SLF4J_LEVEL_MAP.get(level);
                    Filter filter = new Filter() {
                        @Override
                        public boolean isLoggable(LogRecord record) {
                            if(record.getLoggerName().startsWith(namePrefix)){
                                return (record.getLevel().intValue() >= internalLevel.intValue());
                            }
                            return true;
                        }
                    };
                    consoleHandler.setFilter(filter);
                    fileHandlers.forEach(h->h.setFilter(filter));
                } else {
                    getLog(LogFactory.class).error("Unknown log level for %s: %s", namePrefix, level);
                }
            });
        }
    }

    private static Properties create() {
        Properties props = new Properties();
        props.setProperty(".level", "ALL");
        props.setProperty("java.util.logging.FileHandler.maxLocks", "100");
        
        props.setProperty("jdk.event.security.level", "WARNING");

        props.setProperty("javax.management.level", "WARNING");
        props.setProperty("com.sun.jna.level", "WARNING");
        props.setProperty("jdk.internal.httpclient.level", "WARNING");
        props.setProperty("sun.net.www.protocol.http.HttpURLConnection.level", "WARNING");
        props.setProperty("org.jboss.logging.level", "WARNING");
        props.setProperty("org.xnio.level", "WARNING");
        props.setProperty("io.undertow.request.io.level", "FINER"); 
        props.setProperty("io.undertow.server.HttpServerExchange.level", "WARNING");
        props.setProperty("io.undertow.request.error-response.level", "WARNING");

        return props;
    }

    private static void read(LogManager manager, Properties props) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(512);
        props.store(out, "No comment");
        manager.readConfiguration(new ByteArrayInputStream(out.toByteArray()));
    }

    private static int getSystemPropertyValue(String value, int defaultValue) {
        try {
            if(value!=null){
                defaultValue = Integer.valueOf(value);
            }

        } catch (Exception e) {
        }
        return defaultValue;
    }

    private static boolean setupAsyncLogger(LogConfig config) {
        try {
            String logPath = work.ready.core.server.Ready.getProperty("ready.asyncLogger.log.path", "logs" + File.separator);
            String maxThread = work.ready.core.server.Ready.getProperty("ready.asyncLogger.max.thread");
            int maxThreadCount = getSystemPropertyValue(maxThread, 10); 
            String queueSize = work.ready.core.server.Ready.getProperty("ready.asyncLogger.queue.size");
            int queueSizeLength = getSystemPropertyValue(queueSize, 10);
            String keepAlive = work.ready.core.server.Ready.getProperty("ready.asyncLogger.thread.keepalive.time");
            int keepAliveTime = getSystemPropertyValue(keepAlive, 60);
            String logFileName = "Ready.%g.log";
            Level fileLogLevel = Level.ALL;

            boolean isEnabled = true;
            boolean isSeparated = false;
            int fileLogLimit = 10000000;
            int fileLogCount = 10;
            int corePoolSize = 2;
            int fileLogMaxLocks = 100;

            if(config != null) {
                isEnabled = Optional.ofNullable(config.isFileLogEnabled()).orElse(true);
                isSeparated = Optional.ofNullable(config.isSeparatedFileLog()).orElse(false);
                logPath = Optional.ofNullable(config.getFileLogPath()).orElse(logPath);
                logFileName = Optional.ofNullable(config.getFileLogNamePattern()).orElse(logFileName);
                String level = config.getFileLogLevel().toUpperCase();
                if(FROM_SLF4J_LEVEL_MAP.containsKey(level)) {
                    fileLogLevel = FROM_SLF4J_LEVEL_MAP.get(level);
                } else {
                    getLog(LogFactory.class).warn("Unknown file log level: " + level);
                }
                fileLogLimit = Optional.ofNullable(config.getFileLogLimit()).orElse(fileLogLimit);
                fileLogCount = Optional.ofNullable(config.getFileLogCount()).orElse(fileLogCount);
                maxThreadCount = Optional.ofNullable(config.getMaxThread()).orElse(maxThreadCount);
                corePoolSize = Optional.ofNullable(config.getCorePoolSize()).orElse(corePoolSize);
                queueSizeLength = Optional.ofNullable(config.getQueueSize()).orElse(queueSizeLength);
                keepAliveTime = Optional.ofNullable(config.getThreadKeepalive()).orElse(keepAliveTime);
                fileLogMaxLocks = Optional.ofNullable(config.getFileLogMaxLocks()).orElse(fileLogMaxLocks);
            }

            if(!isEnabled) return false;

            if (logPath.startsWith("@")) {
                logPath = logPath.substring(1);
            } else {
                logPath = work.ready.core.server.Ready.root() + File.separator + logPath;
            }
            Path path = Paths.get(logPath).toAbsolutePath();
            if (!Files.exists(path)){
                Files.createDirectories(path);
            }

            for (Handler handler : rootLogger.getHandlers()) {
                if(handler.getClass().equals(AsyncFileHandler.class)) {
                    rootLogger.removeHandler(handler);
                }
            }

            if(isSeparated) {
                for (Map.Entry<String, Level> entry : FROM_SLF4J_LEVEL_MAP.entrySet()) {
                    if(entry.getValue().equals(Level.ALL) || entry.getValue().equals(Level.OFF)) continue;
                    if(entry.getValue().intValue() >= fileLogLevel.intValue()) {
                        Path newPath =  path.resolve(entry.getKey());
                        if (!Files.exists(newPath)){
                            Files.createDirectories(newPath);
                        }
                        var fileHandler = new AsyncFileHandler(newPath + File.separator + logFileName, fileLogLimit, fileLogCount, true, corePoolSize, maxThreadCount, queueSizeLength, keepAliveTime);
                        fileHandler.setLevel(entry.getValue());
                        fileHandler.setSeparated(true);
                        fileHandlers.add(fileHandler);
                        rootLogger.addHandler(fileHandler);
                    }
                }
            } else {
                AsyncFileHandler fileHandler = new AsyncFileHandler(path + File.separator + logFileName, fileLogLimit, fileLogCount, true, corePoolSize, maxThreadCount, queueSizeLength, keepAliveTime);
                fileHandler.setLevel(fileLogLevel);

                fileHandlers.add(fileHandler);
                rootLogger.addHandler(fileHandler);
            }
        } catch (Exception e) {
            
            return false;
        }
        return true;
    }

    private static final Map<String, Log> loggerMap = new ConcurrentHashMap<String, Log>();

    public static Log getLog(Class<?> cls) {
        return getLog(cls.getName());
    }

    public static Log getLog(String name) {
        Log logger = loggerMap.get(name);
        if (logger != null) {
            return logger;
        } else {
            Log newInstance = new Log(name);
            Log oldInstance = loggerMap.putIfAbsent(name, newInstance);
            return oldInstance == null ? newInstance : oldInstance;
        }
    }

    public static boolean isEmpty() {
        return loggerMap.isEmpty();
    }
}
