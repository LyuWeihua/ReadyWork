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

import java.time.ZoneId;

import static work.ready.core.tools.StrUtil.format;

public class TriggerTask implements Task {
    final Trigger trigger;
    private final String name;
    private final Job job;
    private final ZoneId zoneId;

    TriggerTask(String name, Job job, Trigger trigger, ZoneId zoneId) {
        this.name = name;
        this.trigger = trigger;
        this.job = job;
        this.zoneId = zoneId;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Job job() {
        return job;
    }

    @Override
    public String trigger() {
        return format("%s[%s]", trigger, zoneId);
    }
}
