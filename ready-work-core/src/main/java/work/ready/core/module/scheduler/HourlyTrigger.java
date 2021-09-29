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

import java.time.ZonedDateTime;

import static work.ready.core.tools.StrUtil.format;

public final class HourlyTrigger implements Trigger {
    private final int minute;
    private final int second;

    public HourlyTrigger(int minute, int second) {
        this.minute = minute;
        this.second = second;

        if (minute < 0 || minute > 59) {
            throw new Error("minute is out of range, please use 0-59, minute=" + minute);
        }
        if (second < 0 || second > 59) {
            throw new Error("second is out of range, please use 0-59, second=" + second);
        }
    }

    @Override
    public ZonedDateTime next(ZonedDateTime previous) {
        ZonedDateTime next = previous.withMinute(minute).withSecond(second).withNano(0);
        if (!next.isAfter(previous)) {
            next = next.plusHours(1);
        }
        return next;
    }

    @Override
    public String toString() {
        return format("hourly@%s:%s", minute, second);
    }
}
