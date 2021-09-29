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
package work.ready.core.module.scheduler;

import work.ready.core.exception.NotFoundException;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.module.ThreadPools;
import work.ready.core.server.Ready;
import work.ready.core.tools.HashUtil;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static work.ready.core.tools.StrUtil.format;

public final class Scheduler {
    private static final Log logger = LogFactory.getLog(Scheduler.class);
    public final Map<String, Task> tasks = new HashMap<>();
    private final ScheduledExecutorService scheduler;
    private final ExecutorService jobExecutor;
    ZoneId zoneId = Ready.zonedDateTime().getZone();

    public Scheduler() {
        this(ThreadPools.singleThreadScheduler("scheduler-"),
            ThreadPools.cachedThreadPool(Runtime.getRuntime().availableProcessors() * 4, "scheduler-job-"));
    }

    Scheduler(ScheduledExecutorService scheduler, ExecutorService jobExecutor) {
        this.scheduler = scheduler;
        this.jobExecutor = jobExecutor;
    }

    public void start() {
        var now = Ready.zonedDateTime(zoneId);
        for (var entry : tasks.entrySet()) {
            String name = entry.getKey();
            Task task = entry.getValue();
            if (task instanceof FixedRateTask) {
                schedule((FixedRateTask) task);
                logger.info("schedule job, job=%s, trigger=%s, jobClass=%s", name, task.trigger(), task.job().getClass().getCanonicalName());
            } else if (task instanceof TriggerTask) {
                try {
                    ZonedDateTime next = next(((TriggerTask) task).trigger, now);
                    schedule((TriggerTask) task, next);
                    logger.info("schedule job, job=%s, trigger=%s, jobClass=%s, next=%s", name, task.trigger(), task.job().getClass().getCanonicalName(), next);
                } catch (Throwable e) {
                    logger.error(e,"failed to schedule job, job=%s", name);  
                }
            }
        }
        logger.info("scheduler started");
    }

    public void shutdown() throws InterruptedException {
        logger.info("shutting down scheduler");
        scheduler.shutdown();
        try {
            boolean success = scheduler.awaitTermination(5000, TimeUnit.MILLISECONDS);
            if (!success) logger.warn("failed to terminate scheduler");
        } finally {
            jobExecutor.shutdown();
        }
    }

    public void awaitTermination(long timeoutInMs) throws InterruptedException {
        boolean success = jobExecutor.awaitTermination(timeoutInMs, TimeUnit.MILLISECONDS);
        if (!success) logger.warn("failed to terminate scheduler job executor");
        else logger.info("scheduler stopped");
    }

    public void addFixedRateTask(String name, Job job, Duration rate) {
        addTask(new FixedRateTask(name, job, rate));
    }

    public void addTriggerTask(String name, Job job, Trigger trigger) {
        addTask(new TriggerTask(name, job, trigger, zoneId));
    }

    private void addTask(Task task) {
        Class<? extends Job> jobClass = task.job().getClass();
        if (jobClass.isSynthetic())
            throw new Error(format("job class must not be anonymous class or lambda, please create static class, jobClass=%s", jobClass.getCanonicalName()));

        String name = task.name();
        Task previous = tasks.putIfAbsent(name, task);
        if (previous != null)
            throw new Error(format("found duplicate job, name=%s, previous=%s", name, previous.job().getClass().getCanonicalName()));
    }

    ZonedDateTime next(Trigger trigger, ZonedDateTime previous) {
        ZonedDateTime next = trigger.next(previous);
        if (next == null || !next.isAfter(previous)) throw new Error(format("next scheduled time must be after previous, previous=%s, next=%s", previous, next));
        return next;
    }

    void schedule(TriggerTask task, ZonedDateTime time) {
        ZonedDateTime now = Ready.zonedDateTime(zoneId);
        Duration delay = Duration.between(now, time);
        scheduler.schedule(() -> executeTask(task, time), delay.toNanos(), TimeUnit.NANOSECONDS);
    }

    void schedule(FixedRateTask task) {
        Duration delay = Duration.ofMillis((long) HashUtil.nextDouble(1000, 3000)); 
        task.scheduledTime = Ready.zonedDateTime(zoneId).plus(delay);
        scheduler.scheduleAtFixedRate(() -> {
            ZonedDateTime scheduledTime = task.scheduledTime;
            ZonedDateTime next = task.scheduleNext();
            logger.info("execute scheduled job, job=%s, rate=%s, scheduled=%s, next=%s", task.name(), task.rate, scheduledTime, next);
            submitJob(task, scheduledTime, false);
        }, delay.toNanos(), task.rate.toNanos(), TimeUnit.NANOSECONDS);
    }

    void executeTask(TriggerTask task, ZonedDateTime scheduledTime) {
        try {
            ZonedDateTime next = next(task.trigger, scheduledTime);
            schedule(task, next);
            logger.info("execute scheduled job, job=%s, trigger=%s, scheduled=%s, next=%s", task.name(), task.trigger(), scheduledTime, next);
            submitJob(task, scheduledTime, false);
        } catch (Throwable e) {
            logger.error(e,"failed to execute scheduled job, job is terminated, job=%s", task.name());
        }
    }

    public void triggerNow(String name) {
        Task task = tasks.get(name);
        if (task == null) throw new NotFoundException("job not found, name=" + name);
        submitJob(task, Ready.zonedDateTime(zoneId), true);
    }

    private void submitJob(Task task, ZonedDateTime scheduledTime, boolean trace) {
        jobExecutor.submit(() -> {
            try {
                logger.info("=== job execution begin ===");
                String name = task.name();
                Job job = task.job();
                job.execute(new JobContext(name, scheduledTime));
                return null;
            } catch (Throwable e) {
                throw e;
            } finally {
                logger.info("=== job execution end ===");
            }
        });
    }
}
