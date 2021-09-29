/**
 *
 * Original work copyright bee-apm
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
package work.ready.core.apm.model;

public class GcInfo {
    private long oldGcCount = 0;
    private long oldGcTime = 0;
    private long youngGcCount = 0;
    private long youngGcTime = 0;

    public long getOldGcCount() {
        return oldGcCount;
    }

    public void setOldGcCount(long oldGcCount) {
        this.oldGcCount = oldGcCount;
    }

    public long getOldGcTime() {
        return oldGcTime;
    }

    public void setOldGcTime(long oldGcTime) {
        this.oldGcTime = oldGcTime;
    }

    public long getYoungGcCount() {
        return youngGcCount;
    }

    public void setYoungGcCount(long youngGcCount) {
        this.youngGcCount = youngGcCount;
    }

    public long getYoungGcTime() {
        return youngGcTime;
    }

    public void setYoungGcTime(long youngGcTime) {
        this.youngGcTime = youngGcTime;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("{")
                .append("oldGcCount=").append(oldGcCount)
                .append(", oldGcTime=").append(oldGcTime)
                .append(", youngGcCount=").append(youngGcCount)
                .append(", youngGcTime=").append(youngGcTime)
                .append('}').toString();
    }
}
