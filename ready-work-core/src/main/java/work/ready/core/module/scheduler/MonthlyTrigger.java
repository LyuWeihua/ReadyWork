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

import java.time.LocalTime;
import java.time.ZonedDateTime;

import static work.ready.core.tools.StrUtil.format;

public final class MonthlyTrigger implements Trigger {
    private final int dayOfMonth;
    private final LocalTime time;

    public MonthlyTrigger(int dayOfMonth, LocalTime time) {
        this.dayOfMonth = dayOfMonth;
        this.time = time;

        if (dayOfMonth < 1 || dayOfMonth > 28) {
            throw new Error(format("dayOfMonth is out of range, please use 1-28, dayOfMonth=%s", dayOfMonth));
        }
    }

    @Override
    public ZonedDateTime next(ZonedDateTime previous) {
        ZonedDateTime next = previous.withDayOfMonth(dayOfMonth).with(time);
        if (!next.isAfter(previous)) {
            next = next.plusMonths(1).with(time);     
        }
        return next;
    }

    @Override
    public String toString() {
        return format("monthly@%s/%s", dayOfMonth, time);
    }
}
