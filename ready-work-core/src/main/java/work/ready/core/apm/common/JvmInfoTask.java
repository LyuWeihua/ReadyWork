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
package work.ready.core.apm.common;

import work.ready.core.apm.ApmManager;
import work.ready.core.apm.model.GcInfo;
import work.ready.core.apm.model.Span;
import work.ready.core.apm.model.SpanType;
import work.ready.core.apm.reporter.ReporterManager;
import work.ready.core.server.Ready;
import work.ready.core.tools.ReadyThreadFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class JvmInfoTask {
    private static final String THREAD_NAME = "jvm";
    private static final ScheduledExecutorService service = new ScheduledThreadPoolExecutor(1, new ReadyThreadFactory(THREAD_NAME));

    private static GcInfo preGcInfo;

    public static void start() {
        int period = ApmManager.getConfig().getJvmPeriod();
        service.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                Span span = new Span(SpanType.JVM);
                span.setId(String.valueOf(Ready.getId()));
                ApmManager.getConfig().fillEnvInfo(span);
                span.setTime(Ready.now());
                buildJvmInfo(span);
                ReporterManager.report(span);
            }
        }, 0, period, TimeUnit.SECONDS);
        ApmManager.addShutdown((inMs)->shutdown());
    }

    public static void shutdown() {
        if (service != null) {
            service.shutdown();
        }
    }

    private static void buildJvmInfo(Span span) {
        GcInfo currGcInfo = JvmUtil.getGcInfo();
        GcInfo gc = new GcInfo();
        if (preGcInfo == null) {
            gc.setOldGcCount(0);
            gc.setOldGcTime(0);
            gc.setYoungGcCount(0);
            gc.setYoungGcTime(0);
        } else {
            gc.setYoungGcCount(currGcInfo.getYoungGcCount() - preGcInfo.getYoungGcCount());
            gc.setYoungGcTime(currGcInfo.getYoungGcTime() - preGcInfo.getYoungGcTime());
            gc.setOldGcCount(currGcInfo.getOldGcCount() - preGcInfo.getOldGcCount());
            gc.setOldGcTime(currGcInfo.getOldGcTime() - preGcInfo.getOldGcTime());
        }
        preGcInfo = currGcInfo;
        span.addTag("gc", gc);
        span.addTag("heap", JvmUtil.getHeap());
        span.addTag("memory", JvmUtil.getMemorySpaceInfo());
        span.addTag("thread", JvmUtil.getThreadInfo());
    }
}
