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

public class DummyCache implements Cache {
    @Override
    public <T> T get(String cacheName, Object key) {
        return null;
    }

    @Override
    public void put(String cacheName, Object key, Object value) {
        
    }

    @Override
    public void put(String cacheName, Object key, Object value, int liveSeconds) {
        
    }

    @Override
    public List getKeys(String cacheName) {
        return null;
    }

    @Override
    public void remove(String cacheName, Object key) {
        
    }

    @Override
    public void removeAll(String cacheName) {
        
    }

    @Override
    public <T> T get(String cacheName, Object key, DataLoader dataLoader) {
        return (T) dataLoader.load();
    }

    @Override
    public <T> T get(String cacheName, Object key, DataLoader dataLoader, int liveSeconds) {
        return (T) dataLoader.load();
    }

    @Override
    public Integer getTtl(String cacheName, Object key) {
        return null;
    }

    @Override
    public void setTtl(String cacheName, Object key, int seconds) {
        
    }

    @Override
    public void refresh(String cacheName, Object key) {

    }

    @Override
    public void refresh(String cacheName) {

    }

    @Override
    public List getNames() {
        return null;
    }
}
