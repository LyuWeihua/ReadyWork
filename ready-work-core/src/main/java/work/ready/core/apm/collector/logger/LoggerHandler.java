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
package work.ready.core.apm.collector.logger;

import work.ready.core.apm.ApmManager;
import work.ready.core.apm.common.SamplingUtil;
import work.ready.core.apm.common.SpanManager;
import work.ready.core.apm.common.TraceContext;
import work.ready.core.apm.model.Span;
import work.ready.core.apm.model.SpanType;
import work.ready.core.apm.reporter.ReporterManager;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.component.time.TimeSupplier;
import work.ready.core.component.time.TimeAware;
import work.ready.core.log.LogLevel;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.function.Supplier;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class LoggerHandler extends Handler implements TimeAware {

    private Supplier<Long> timeSupplier;

    public LoggerHandler() {
        TimeSupplier.addListener(this);
        Formatter formatter = new Formatter() {
            @Override
            public String format(LogRecord record) {
                StringBuffer sb = new StringBuffer();
                java.sql.Timestamp ts;
                if(timeSupplier != null) {
                    
                    ts = new java.sql.Timestamp(timeSupplier.get()); 
                } else {
                    ts = new java.sql.Timestamp(record.getMillis()); 
                }
                sb.append(ts.toString());
                while(sb.length() < 23) sb.append(" ");  
                sb.append(" ");

                sb.append("|");
                
                sb.append(LogFactory.levelToSlf4j(record.getLevel()));
                while(sb.length() < 30) sb.append(" ");  
                sb.append(" ");

                sb.append(record.getThreadID());
                while(sb.length() < 36) sb.append(" ");  
                sb.append("| ");

                if(Log.LoggerName.equals(record.getLoggerName())) {
                    sb.append(record.getSourceClassName());
                    sb.append(" ");
                } else {
                    sb.append(record.getLoggerName());
                    sb.append(" ");
                }

                sb.append(record.getSourceMethodName());

                sb.append(" - ");

                sb.append(formatMessage(record));
                sb.append("\n");
                if(record.getThrown() != null) {
                    sb.append(parseThrowable(record.getThrown()));
                }

                return sb.toString();
            }
        };

        String packagePrefix = ApmManager.class.getPackageName() + '.';
        setFormatter(formatter);
        setFilter(record -> !record.getSourceClassName().startsWith(packagePrefix));
        LogFactory.addHandler(this, LogLevel.ALL.name());
        ApmManager.addShutdown((inMs)->LogFactory.removeHandler(this));
    }

    public static String parseThrowable(Throwable t) {
        StringBuilder builder = new StringBuilder();
        StringWriter writer = new StringWriter(); 
        t.printStackTrace(new PrintWriter(writer));
        builder.append(writer);
        try {

            Field messageField = Throwable.class.getDeclaredField("detailMessage");
            messageField.setAccessible(true);
            String detailMessage = t.getMessage();
            messageField.set(t, "[" + TraceContext.getCorrelationId() + "]" + detailMessage);
        } catch (Throwable tt) {
            System.err.println("exception while processing detailMessage: ");
            tt.printStackTrace();
        }
        return builder.toString();
    }

    @Override
    public void setTimeSupplier(Supplier<Long> timeSupplier) {
        this.timeSupplier = timeSupplier;
    }

    @Override
    public void publish(LogRecord record) {
        if (this.isLoggable(record)) {
            String message = getFormatter().format(record);
            String level = LogFactory.levelToSlf4j(record.getLevel()).toLowerCase();
            String pointMethod = record.getSourceMethodName();
            String point = record.getLoggerName();

            boolean isCollect = true;   
            if (ApmManager.getConfig(LoggerConfig.class).level(level) >= LoggerConfig.LEVEL_ERROR) {
                
                if (ApmManager.getConfig(LoggerConfig.class).isErrorRate()) {
                    isCollect = SamplingUtil.YES();
                }
            }
            if (!isCollect || !ApmManager.getConfig(LoggerConfig.class).isEnabled() || !ApmManager.getConfig(LoggerConfig.class).checkLevel(point, level)) {
                return;
            }
            Span span = SpanManager.createLocalSpan(SpanType.LOGGER);
            span.addTag("point", point + "." + pointMethod);
            span.addTag("log", message);
            span.addTag("level", level);
            ApmManager.getConfig().fillEnvInfo(span);
            ReporterManager.report(span);
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }
}
