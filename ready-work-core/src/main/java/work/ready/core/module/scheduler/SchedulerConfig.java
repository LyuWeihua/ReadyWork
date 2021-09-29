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

import work.ready.core.module.ApplicationContext;
import work.ready.core.module.ShutdownHook;
import work.ready.core.module.scheduler.*;

import java.time.*;

public final class SchedulerConfig {
    private Scheduler scheduler;
    private boolean triggerAdded;

    public SchedulerConfig(ApplicationContext context) {
        scheduler = new Scheduler();
        context.startupHook.add(scheduler::start);
        context.shutdownHook.add(ShutdownHook.STAGE_0, timeout -> scheduler.shutdown());
        context.shutdownHook.add(ShutdownHook.STAGE_1, scheduler::awaitTermination);
        context.coreContext.getBeanManager().addSingletonObject(scheduler);
    }

    public void timeZone(ZoneId zoneId) {
        if (triggerAdded) throw new Error("schedule timeZone must be configured before adding trigger");
        if (zoneId == null) throw new Error("zoneId must not be null");
        scheduler.zoneId = zoneId;
    }

    public void fixedRate(String name, Job job, Duration rate) {
        scheduler.addFixedRateTask(name, job, rate);
        triggerAdded = true;
    }

    public void hourlyAt(String name, Job job, int minute, int second) {
        trigger(name, job, new HourlyTrigger(minute, second));
    }

    public void dailyAt(String name, Job job, LocalTime time) {
        trigger(name, job, new DailyTrigger(time));
    }

    public void weeklyAt(String name, Job job, DayOfWeek dayOfWeek, LocalTime time) {
        trigger(name, job, new WeeklyTrigger(dayOfWeek, time));
    }

    public void monthlyAt(String name, Job job, int dayOfMonth, LocalTime time) {
        trigger(name, job, new MonthlyTrigger(dayOfMonth, time));
    }

    public void trigger(String name, Job job, Trigger trigger) {
        scheduler.addTriggerTask(name, job, trigger);
        triggerAdded = true;
    }
}
