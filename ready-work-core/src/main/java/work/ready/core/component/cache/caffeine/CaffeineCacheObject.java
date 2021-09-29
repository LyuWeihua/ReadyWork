/**
 *
 * Original work Copyright (c) 2015-2020, Michael Yang 杨福海 (fuhai999@gmail.com).
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
package work.ready.core.component.cache.caffeine;

import work.ready.core.server.Ready;

import java.io.Serializable;

public class CaffeineCacheObject implements Serializable {

    private Object value;
    private Integer liveSeconds;
    private Long cachetime;

    public CaffeineCacheObject() {
    }

    public CaffeineCacheObject(Object value) {
        this.value = value;
    }

    public CaffeineCacheObject(Object value, Integer liveSeconds) {
        this.value = value;
        this.liveSeconds = liveSeconds;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Integer getTtl() {
        if (liveSeconds == null) {
            return -1;
        }

        long timeMillis = cachetime - (Ready.currentTimeMillis() - liveSeconds * 1000);

        if (timeMillis > 0) {
            return (int) (timeMillis / 1000);
        }

        return 0;
    }

    public Integer getLiveSeconds() {
        return liveSeconds;
    }

    public void setLiveSeconds(Integer liveSeconds) {
        this.liveSeconds = liveSeconds;
    }

    public Long getCachetime() {
        return cachetime;
    }

    public void setCachetime(Long cachetime) {
        this.cachetime = cachetime;
    }

    public boolean isDue() {
        if (liveSeconds == null) {
            return false;
        }

        if ((Ready.currentTimeMillis() - liveSeconds * 1000) > cachetime) {
            return true;
        }

        return false;
    }
}
