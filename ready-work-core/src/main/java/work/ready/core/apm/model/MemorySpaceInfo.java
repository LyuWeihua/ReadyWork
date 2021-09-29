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

public class MemorySpaceInfo {
    
    private long youngSize = 0;

    private long oldSize = 0;

    private long permGenSize = 0;

    public long getYoungSize() {
        return youngSize;
    }

    public void setYoungSize(long youngSize) {
        this.youngSize = youngSize;
    }

    public long getOldSize() {
        return oldSize;
    }

    public void setOldSize(long oldSize) {
        this.oldSize = oldSize;
    }

    public long getPermGenSize() {
        return permGenSize;
    }

    public void setPermGenSize(long permGenSize) {
        this.permGenSize = permGenSize;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("{")
                .append("youngSize=").append(youngSize)
                .append(", oldSize=").append(oldSize)
                .append(", permGenSize=").append(permGenSize)
                .append('}').toString();
    }
}
