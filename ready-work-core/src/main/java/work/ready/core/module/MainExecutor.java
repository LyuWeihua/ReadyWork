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

import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;

public final class MainExecutor implements Executor {
    private static final Log logger = LogFactory.getLog(MainExecutor.class);
    private final ExecutorService executor;
    private final String name;
    private volatile ScheduledExecutorService scheduler;

    public MainExecutor(int poolSize, String name) {
        this.name = "executor" + (name == null ? "" : "-" + name);
        this.executor = ThreadPools.cachedThreadPool(poolSize, this.name + "-");
    }

    public void shutdown() {
        logger.info("shutting down %s", name);
        synchronized (this) {
            if (scheduler != null) {
                List<Runnable> delayedTasks = scheduler.shutdownNow(); 
                logger.info("cancelled delayed tasks, name=%s, cancelled=%s", name, delayedTasks.size());
            }
            executor.shutdown();
        }
    }

    public void awaitTermination(long timeoutInMs) throws InterruptedException {
        boolean success = executor.awaitTermination(timeoutInMs, TimeUnit.MILLISECONDS);
        if (!success) logger.warn("failed to terminate %s", name);
        else logger.info("%s stopped", name);
    }

    @Override
    public <T> Future<T> submit(String action, Callable<T> task) {
        Callable<T> execution = execution(action, task);
        return submitTask(action, execution);
    }

    @Override
    public void submit(String action, Task task, Duration delay) {
        synchronized (this) {
            if (executor.isShutdown()) {
                logger.warn("TASK_REJECTED", "reject task due to server is shutting down, action=%s", action);    
                return;
            }
            if (scheduler == null) {
                scheduler = ThreadPools.singleThreadScheduler(name + "-scheduler-");
            }
        }
        
        Callable<Void> execution = execution(action, task);
        Runnable delayedTask = () -> {
            try {
                submitTask(action, execution);
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);    
            }
        };
        try {
            scheduler.schedule(delayedTask, delay.toMillis(), TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {    
            logger.warn(e,"TASK_REJECTED", "reject task due to server is shutting down, action=%s", action);
        }
    }

    private <T> Future<T> submitTask(String action, Callable<T> execution) {
        try {
            return executor.submit(execution);
        } catch (RejectedExecutionException e) {    
            logger.warn(e,"TASK_REJECTED", "reject task due to server is shutting down, action=%s", action);
            return new CancelledFuture<>();
        }
    }

    private <T> Callable<T> execution(String action, Callable<T> task) {
        return new ExecutorTask<>(action, task);
    }
}
