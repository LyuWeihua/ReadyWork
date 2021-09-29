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

import work.ready.core.component.time.TimeSupplier;
import work.ready.core.component.time.TimeAware;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class AnsiColorConsoleHandler extends BaseColorConsoleHandler implements TimeAware {

    private static final DateTimeFormatter dateTimePattern = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss.SSS");
    private Supplier<Long> timeSupplier;

    @Override
    public void setTimeSupplier(Supplier<Long> timeSupplier) {
        this.timeSupplier = timeSupplier;
    }

    public AnsiColorConsoleHandler(){
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
                sb.append(ts.toString().substring(5));
                while(sb.length() < 18) sb.append(" ");  
                sb.append(" ");

                sb.append("|");
                
                sb.append(LogFactory.levelToSlf4j(record.getLevel()));
                while(sb.length() < 25) sb.append(" ");  
                sb.append(" ");

                sb.append(record.getThreadID());
                while(sb.length() < 32) sb.append(" ");  
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
                    StringWriter writer = new StringWriter(); 
                    record.getThrown().printStackTrace(new PrintWriter(writer));
                    sb.append(writer);
                }

                return sb.toString();
            }
        };

        setFormatter(formatter);
    }

    @Override
    public void publish(LogRecord record) {
        if (this.isLoggable(record)) {
            System.err.print(logRecordToString(record));
        }
    }

    @Override
    public synchronized void flush() {
        System.err.flush();
    }
}
