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

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import static work.ready.core.tools.StrUtil.format;

public final class WeeklyTrigger implements Trigger {
    private final DayOfWeek dayOfWeek;
    private final LocalTime time;

    public WeeklyTrigger(DayOfWeek dayOfWeek, LocalTime time) {
        this.dayOfWeek = dayOfWeek;
        this.time = time;
    }

    @Override
    public ZonedDateTime next(ZonedDateTime previous) {
        ZonedDateTime next = previous.plusDays(dayOfWeek.getValue() - previous.getDayOfWeek().getValue()).with(time);
        if (!next.isAfter(previous)) {
            next = next.plusWeeks(1).with(time);     
        }
        return next;
    }

    @Override
    public String toString() {
        return format("weekly@%s/%s", dayOfWeek, time);
    }
}
