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

import work.ready.core.apm.model.GcInfo;
import work.ready.core.apm.model.ThreadInfo;
import work.ready.core.apm.model.HeapInfo;
import work.ready.core.apm.model.MemorySpaceInfo;

import java.lang.management.*;
import java.util.ArrayList;
import java.util.List;

public class JvmUtil {
    private static List<String> youngGenCollectorNames = new ArrayList<String>();
    private static List<String> oldGenCollectorNames = new ArrayList<String>();

    static {
        youngGenCollectorNames.add("Copy");
        youngGenCollectorNames.add("ParNew");
        youngGenCollectorNames.add("PS Scavenge");
        youngGenCollectorNames.add("G1 Young Generation");
        oldGenCollectorNames.add("MarkSweepCompact");
        oldGenCollectorNames.add("PS MarkSweep");
        oldGenCollectorNames.add("ConcurrentMarkSweep");
        oldGenCollectorNames.add("G1 Old Generation");
    }

    public static GcInfo getGcInfo() {
        long oldCount = 0;
        long oldTime = 0;
        long youngCount = 0;
        long youngTime = 0;
        List<GarbageCollectorMXBean> gcs = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gc : gcs) {
            if (youngGenCollectorNames.contains(gc.getName())) {
                youngCount += gc.getCollectionCount();
                youngTime += gc.getCollectionTime();
            } else if (oldGenCollectorNames.contains(gc.getName())) {
                oldCount += gc.getCollectionCount();
                oldTime += gc.getCollectionTime();
            }
        }
        GcInfo report = new GcInfo();
        report.setYoungGcCount(youngCount);
        report.setOldGcCount(oldCount);
        report.setYoungGcTime(youngTime);
        report.setOldGcTime(oldTime);
        return report;
    }

    public static HeapInfo getHeap() {
        MemoryMXBean memoryMxBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage memoryUsage = memoryMxBean.getHeapMemoryUsage();
        HeapInfo heapInfo = new HeapInfo();
        heapInfo.setMax(memoryUsage.getMax());
        heapInfo.setUsed(memoryUsage.getUsed());
        return heapInfo;
    }

    public static MemorySpaceInfo getMemorySpaceInfo() {
        List<MemoryPoolMXBean> memoryPoolMxBeans = ManagementFactory.getMemoryPoolMXBeans();
        MemorySpaceInfo memorySpaceInfo = new MemorySpaceInfo();
        for (MemoryPoolMXBean bean : memoryPoolMxBeans) {
            String name = bean.getName();
            
            if (name.endsWith("Metaspace") || name.endsWith("Perm Gen")) {
                memorySpaceInfo.setPermGenSize(bean.getUsage().getUsed());
            } else if (name.endsWith("Survivor Space") || name.endsWith("Eden Space")) {
                memorySpaceInfo.setYoungSize(memorySpaceInfo.getYoungSize() + bean.getUsage().getUsed());
            } else if (name.endsWith("Old Gen")) {
                memorySpaceInfo.setOldSize(bean.getUsage().getUsed());
            }
        }
        return memorySpaceInfo;
    }

    public static ThreadInfo getThreadInfo() {
        ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
        ThreadInfo info = new ThreadInfo();
        
        info.setDaemonThreadCount(threadMxBean.getDaemonThreadCount());
        
        info.setThreadCount(threadMxBean.getThreadCount());
        return info;
    }

}
