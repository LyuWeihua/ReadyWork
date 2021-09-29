/**
 *
 * Original work Copyright core-ng
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

package work.ready.core.module.system;

import work.ready.core.handler.Controller;

import javax.management.*;
import java.lang.management.*;

public class DiagnosticController extends Controller {

    public void vm() {
        renderText(invoke("vmInfo"));
    }

    public void thread() {
        renderText(invoke("threadPrint"));
    }

    public void heap() {
        renderText(invoke("gcClassHistogram"));
    }

    public void memory(){
        renderJson(memoryUsage());
    }

    public void threadUsage() {
        ThreadUsage usage = new ThreadUsage();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        usage.threadCount = threadMXBean.getThreadCount();
        usage.peakThreadCount = threadMXBean.getPeakThreadCount();
        renderJson(usage);
    }

    public void threadDump() {
        renderText(threadDumpText());
    }

    private String invoke(String operation) {
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            var name = new ObjectName("com.sun.management", "type", "DiagnosticCommand");
            return (String) server.invoke(name, operation, new Object[]{null}, new String[]{String[].class.getName()});
        } catch (MalformedObjectNameException | InstanceNotFoundException | MBeanException | ReflectionException e) {
            throw new Error(e);
        }
    }

    private work.ready.core.module.system.MemoryUsage memoryUsage() {
        work.ready.core.module.system.MemoryUsage usage = new work.ready.core.module.system.MemoryUsage();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        java.lang.management.MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        usage.heapInit = heapMemoryUsage.getInit();
        usage.heapUsed = heapMemoryUsage.getUsed();
        usage.heapCommitted = heapMemoryUsage.getCommitted();
        usage.heapMax = heapMemoryUsage.getMax();

        java.lang.management.MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();
        usage.nonHeapInit = nonHeapMemoryUsage.getInit();
        usage.nonHeapUsed = nonHeapMemoryUsage.getUsed();
        usage.nonHeapCommitted = nonHeapMemoryUsage.getCommitted();
        usage.nonHeapMax = nonHeapMemoryUsage.getMax();

        return usage;
    }

    String threadDumpText() {
        StringBuilder builder = new StringBuilder();
        ThreadInfo[] threads = ManagementFactory.getThreadMXBean().dumpAllThreads(true, true);
        for (ThreadInfo thread : threads) {
            appendThreadInfo(builder, thread);
        }
        return builder.toString();
    }

    private void appendThreadInfo(StringBuilder builder, ThreadInfo threadInfo) {
        builder.append('\"').append(threadInfo.getThreadName())
                .append("\" Id=").append(threadInfo.getThreadId())
                .append(' ').append(threadInfo.getThreadState());
        if (threadInfo.getLockName() != null) {
            builder.append(" on ").append(threadInfo.getLockName());
        }
        if (threadInfo.getLockOwnerName() != null) {
            builder.append(" owned by \"").append(threadInfo.getLockOwnerName()).append("\" Id=").append(threadInfo.getLockOwnerId());
        }
        if (threadInfo.isSuspended()) {
            builder.append(" (suspended)");
        }
        if (threadInfo.isInNative()) {
            builder.append(" (in native)");
        }
        builder.append('\n');
        appendStackTrace(builder, threadInfo);

        LockInfo[] locks = threadInfo.getLockedSynchronizers();
        if (locks.length > 0) {
            builder.append("\n\tNumber of locked synchronizers = ").append(locks.length);
            builder.append('\n');
            for (LockInfo lock : locks) {
                builder.append("\t- ").append(lock);
                builder.append('\n');
            }
        }
        builder.append('\n');
    }

    private void appendStackTrace(StringBuilder builder, ThreadInfo threadInfo) {
        StackTraceElement[] stackTrace = threadInfo.getStackTrace();
        int length = stackTrace.length;
        for (int i = 0; i < length; i++) {
            StackTraceElement stack = stackTrace[i];
            builder.append("\tat ").append(stack);
            builder.append('\n');
            if (i == 0 && threadInfo.getLockInfo() != null) {
                Thread.State threadState = threadInfo.getThreadState();
                switch (threadState) {
                    case BLOCKED:
                        builder.append("\t-  blocked on ").append(threadInfo.getLockInfo());
                        builder.append('\n');
                        break;
                    case WAITING:
                        builder.append("\t-  waiting on ").append(threadInfo.getLockInfo());
                        builder.append('\n');
                        break;
                    case TIMED_WAITING:
                        builder.append("\t-  timed-waiting on ").append(threadInfo.getLockInfo());
                        builder.append('\n');
                        break;
                    default:
                        break;
                }
            }

            for (MonitorInfo monitorInfo : threadInfo.getLockedMonitors()) {
                if (monitorInfo.getLockedStackDepth() == i) {
                    builder.append("\t-  locked ").append(monitorInfo);
                    builder.append('\n');
                }
            }
        }
    }
}
