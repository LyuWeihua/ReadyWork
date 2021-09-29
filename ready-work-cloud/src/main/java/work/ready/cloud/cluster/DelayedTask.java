/**
 *
 * Copyright (c) 2020 WeiHua Lyu [ready.work]
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

package work.ready.cloud.cluster;

import work.ready.core.server.Ready;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class DelayedTask implements Delayed, Serializable {

    private String name;
    private Object object;
    private Long time;

    public DelayedTask(String name, Long delayTime) {
        this.name = name;
        this.time = Ready.currentTimeMillis() + delayTime;
    }

    public DelayedTask(String name, Object object, Long delayTime) {
        this.name = name;
        this.object = object;
        this.time = Ready.currentTimeMillis() + delayTime;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public Long getTime() {
        return time;
    }
    public void setTime(Long delayTime) {
        this.time = Ready.currentTimeMillis() + delayTime;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        DelayedTask task = (DelayedTask) obj;

        return Objects.equals(name, task.name);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result;
        return result;
    }

    @Override
    public String toString() {
        return "DelayedTask [name=" + name + ", time=" + Instant.ofEpochSecond(time/1000).toString() + "]";
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(time - Ready.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        return (int) (this.getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS));
    }
}
