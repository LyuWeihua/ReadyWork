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

import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.tools.HashUtil;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BackgroundTaskExecutor {
    private static final Log logger = LogFactory.getLog(BackgroundTaskExecutor.class);

    private final ScheduledExecutorService scheduler = ThreadPools.singleThreadScheduler("background-task-");
    private final List<BackgroundTask> tasks = new ArrayList();

    public void start() {
        for (BackgroundTask task : tasks) {
            Duration delay = Duration.ofMillis((long) HashUtil.nextDouble(5000, 10000)); 
            scheduler.scheduleWithFixedDelay(task, delay.toMillis(), task.rate.toMillis(), TimeUnit.MILLISECONDS);
        }
        logger.info("background task executor started");
    }

    public void scheduleWithFixedDelay(Runnable command, Duration rate) {
        tasks.add(new BackgroundTask(command, rate));
    }

    void shutdown() {
        logger.info("shutting down background task executor");
        scheduler.shutdown();
    }

    void awaitTermination(long timeoutInMs) throws InterruptedException {
        boolean success = scheduler.awaitTermination(timeoutInMs, TimeUnit.MILLISECONDS);
        if (!success) logger.warn("failed to terminate background task executor");
        else logger.info("background task executor stopped");
    }

    private static class BackgroundTask implements Runnable {
        final Duration rate;
        private static final Log logger = LogFactory.getLog(BackgroundTask.class);
        private final Runnable command;

        BackgroundTask(Runnable command, Duration rate) {
            this.command = command;
            this.rate = rate;
        }

        @Override
        public void run() {
            try {
                command.run();
            } catch (Throwable e) {
                logger.error(e,"failed to run background task, error=%s", e.getMessage());
            }
        }
    }
}
