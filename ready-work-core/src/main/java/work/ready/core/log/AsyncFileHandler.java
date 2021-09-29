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
import work.ready.core.tools.ReadyThreadFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.logging.*;

public class AsyncFileHandler extends Handler implements TimeAware {

    private FileHandler fileHandler;
    private Supplier<Long> timeSupplier;
    
    private ThreadPoolExecutor executorPool;
    private boolean separated = false;

    @Override
    public void setTimeSupplier(Supplier<Long> timeSupplier) {
        this.timeSupplier = timeSupplier;
    }

    public AsyncFileHandler(String filePath, int limit, int count, boolean append, int corePoolSize,
                            int maximumPoolSize, int qDepth, int keepAliveTime) throws IOException {

        super();
        TimeSupplier.addListener(this);
        fileHandler = new FileHandler(filePath, limit, count, append);

        ThreadFactory threadFactory = new ReadyThreadFactory("AsyncLoggerFileHandler");
        RejectedExecutionHandlerImpl rejectionHandler = new RejectedExecutionHandlerImpl(fileHandler);

        executorPool = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(qDepth), threadFactory, rejectionHandler);

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
                    StringWriter writer = new StringWriter();   
                    record.getThrown().printStackTrace(new PrintWriter(writer));
                    sb.append(writer);
                }

                return sb.toString();
            }
        };
        fileHandler.setFormatter(formatter);
    }

    protected void setSeparated(boolean separated) {
        this.separated = separated;
    }

    protected boolean isSeparated() {
        return separated;
    }

    @Override
    public synchronized void setLevel(Level newLevel) throws SecurityException {
        if(separated){
            throw new UnsupportedOperationException("Can't change the level in separated mode!");
        } else {
            super.setLevel(newLevel);
        }
    }

    @Override
    public synchronized void publish(LogRecord record) {
        if (executorPool.isTerminating()) {
            executorPool.shutdown();
        }
        if (executorPool.isTerminated()) {
            executorPool.shutdown();
        }

        try {
            if (this.isLoggable(record)) {
                if(separated) {
                    if (record.getLevel().equals(super.getLevel())) {
                        executorPool.execute(new WorkerThread(record));
                    }
                } else {
                    executorPool.execute(new WorkerThread(record));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class RejectedExecutionHandlerImpl implements RejectedExecutionHandler {
        private FileHandler fileHandler;

        public RejectedExecutionHandlerImpl(FileHandler fileHandler) {
            this.fileHandler = fileHandler;
        }

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            fileHandler.publish(((WorkerThread) r).getCommand());
        }

    }

    class WorkerThread implements Runnable {

        private LogRecord command;

        public WorkerThread(LogRecord s) throws IOException {
            super();
            this.command = s;
        }

        @Override
        public void run() {
            processCommand();
        }

        private void processCommand() {
            fileHandler.publish(command);
        }

        @Override
        public String toString() {
            return this.command.toString();
        }

        public LogRecord getCommand() {
            return command;
        }

    }

    @Override
    public void close() {
        executorPool.shutdown();
        fileHandler.flush();
        fileHandler.close();
    }

    @Override
    public void flush() {
        fileHandler.flush();
    }

}
