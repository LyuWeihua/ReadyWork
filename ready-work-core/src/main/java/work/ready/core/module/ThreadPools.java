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
package work.ready.core.module;

import work.ready.core.tools.ReadyThreadFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class ThreadPools {

    public static ExecutorService cachedThreadPool(int poolSize, String prefix) {
        var threadPool = new ThreadPoolExecutor(poolSize, poolSize, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new ReadyThreadFactory(prefix));
        threadPool.allowCoreThreadTimeOut(true);
        return threadPool;
    }

    public static ScheduledExecutorService singleThreadScheduler(String prefix) {
        var scheduler = new ScheduledThreadPoolExecutor(1, new ReadyThreadFactory(prefix));
        scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        scheduler.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        return scheduler;
    }
}
