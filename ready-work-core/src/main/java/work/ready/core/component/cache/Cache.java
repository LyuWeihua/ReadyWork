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
package work.ready.core.component.cache;

import java.util.List;

public interface Cache {

    <T> T get(String cacheName, Object key);

    void put(String cacheName, Object key, Object value);

    void put(String cacheName, Object key, Object value, int liveSeconds);

    void remove(String cacheName, Object key);

    void removeAll(String cacheName);

    <T> T get(String cacheName, Object key, DataLoader dataLoader);

    <T> T get(String cacheName, Object key, DataLoader dataLoader, int liveSeconds);

    Integer getTtl(String cacheName, Object key);

    void setTtl(String cacheName, Object key, int seconds);

    void refresh(String cacheName, Object key);

    void refresh(String cacheName);

    List getNames();

    List getKeys(String cacheName);
}
