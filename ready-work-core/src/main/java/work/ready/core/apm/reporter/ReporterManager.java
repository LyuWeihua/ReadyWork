/**
 *
 * Original work copyright bee-apm
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
package work.ready.core.apm.reporter;

import work.ready.core.apm.ApmManager;
import work.ready.core.apm.model.Span;
import work.ready.core.apm.reporter.reporter.console.ConsoleReporter;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.tools.ReadyThreadFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static work.ready.core.tools.ClassUtil.getDefaultClassLoader;

public class ReporterManager {
    private static final Log logger = LogFactory.getLog(ReporterManager.class);
    private static Reporter reporter;
    private static Map<String, Reporter> reporterMap;
    private static BlockingQueue<Span> queue = new LinkedBlockingQueue<>(ApmManager.getConfig().getReporter().getQueueSize());
    private static ScheduledExecutorService scheduledExecutorService;
    private static String reporterName;
    private static final int idleSleep = ApmManager.getConfig().getReporter().getIdleSleep();
    private static final int batchSize = ApmManager.getConfig().getReporter().getBatchSize();
    private static final String REPORTER_THREAD_NAME = "reporter";

    public synchronized static void init() {
        int threadNum = ApmManager.getConfig().getReporter().getThreadNum();
        scheduledExecutorService = new ScheduledThreadPoolExecutor(threadNum, new ReadyThreadFactory(REPORTER_THREAD_NAME));
        ApmManager.addShutdown((inMs)->shutdown());
        if (reporterMap == null) {
            reporterMap = new HashMap<>();
            reporterName = ApmManager.getConfig().getReporter().getDefaultReporter();
            ApmManager.getConfig().getReporter().getReporter().forEach(className->{
                try {
                    Class<?> clazz = Class.forName(className, false, getDefaultClassLoader());
                    if(Reporter.class.isAssignableFrom(clazz)) {
                        Reporter reporter = (Reporter)clazz.getDeclaredConstructor().newInstance();
                        reporterMap.put(reporter.getName(), reporter);
                    } else {
                        logger.warn("Invalid APM reporter: %s", className);
                    }
                } catch (ClassNotFoundException e) {
                    logger.warn(e, "Failed to load APM reporter: %s", className);
                } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
                    logger.warn(e, "Failed to initialize APM reporter: %s", className);
                }
            });
            if(reporterMap.size() == 1 && reporterName == null) {
                reporter = reporterMap.values().iterator().next();
                reporterName = reporter.getName();
            } else {
                if(reporterName == null) {
                    reporterName = ConsoleReporter.name;
                }
                reporter = reporterMap.get(reporterName);
            }
            if (reporter == null) {
                logger.info("The APM reporter: " + reporterName + ", which does not exist.");
                throw new RuntimeException("The APM reporter: " + reporterName + ", which does not exist.");
            }
            reporter.init();
            initTask(threadNum);
        }
    }

    public static void setReporter(Reporter reporter) {
        if(reporterMap == null) {
            logger.error("The ReporterManager hasn't been initialized.");
            return;
        }
        reporterMap.put(reporter.getName(), reporter);
        ReporterManager.reporter = reporter;
    }

    private static void shutdown() {
        if(scheduledExecutorService != null)
            scheduledExecutorService.shutdown();
    }

    private static void initQueue() {
        int queueSize = ApmManager.getConfig().getReporter().getQueueSize();
        queue = new LinkedBlockingQueue<Span>(queueSize);
    }

    private static void initTask(int threadNum) {
        for (int i = 0; i < threadNum; i++) {
            scheduledExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    while (!Thread.currentThread().isInterrupted()) {
                        doReport();
                    }
                }
            });
        }
    }

    private static void doReport() {
        try {
            if (queue.isEmpty()) {
                Thread.sleep(idleSleep);
                return;
            }
            if(reporter.isReady()) {
                List<Span> list = new ArrayList<Span>(batchSize);
                queue.drainTo(list, batchSize);
                reporter.report(list);
            }
        } catch (Exception e) {
            logger.error(e, "");
        }
    }

    public static void report(Span span) {
        if (ApmManager.getConfig().getReporter().isDebug()) {
            logger.debug(span.toString());
        }
        if (!queue.offer(span)) {
            logger.warn("report queue is full.");
        }
    }
}
