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

package work.ready.core.module.system;

import work.ready.core.component.cache.CacheManager;
import work.ready.core.handler.Controller;
import work.ready.core.ioc.annotation.Autowired;
import work.ready.core.service.result.Failure;
import work.ready.core.service.result.Result;
import work.ready.core.service.result.Success;
import work.ready.core.service.status.Status;
import work.ready.core.tools.StrUtil;

import static work.ready.core.tools.StrUtil.format;

public class CacheController extends Controller {

    @Autowired
    private CacheManager cacheManager;

    private static final String GENERIC_EXCEPTION = "ERROR10014";

    public Result get() {
        String name = getRequest().getPathParameter("name");
        String key = getRequest().getPathParameter("key");
        Object value = cacheManager.getCache().get(name, key);
        if(value == null) return Failure.of(new Status(GENERIC_EXCEPTION,"cache key not found, name=" + name + ", key=" + key));
        return Success.of(value);
    }

    public Result delete() {
        String name = getRequest().getPathParameter("name");
        String key = getRequest().getPathParameter("key");
        cacheManager.getCache().remove(name, key);
        return Success.of(format("cache evicted, name=%s, key=%s", name, key));
    }

    public Result list() {
        String name = getRequest().getPathParameter("name");
        if(StrUtil.isBlank(name)) return Failure.of(new Status(GENERIC_EXCEPTION,"cache name cannot be empty"));
        ListCacheResponse response = new ListCacheResponse();
        if(cacheManager.getCache().getKeys(name) != null) {
            cacheManager.getCache().getKeys(name).forEach(key -> response.caches.add(view(name, key)));
        }
        return Success.of(response);
    }

    private ListCacheResponse.Cache view(String cacheName, Object key) {
        var view = new ListCacheResponse.Cache();
        view.cacheName = cacheName;
        view.key = key.toString();
        view.keyType = key.getClass().getCanonicalName();
        Object value = cacheManager.getCache().get(cacheName, key);
        view.value = value.toString();
        view.valueType = value.getClass().getCanonicalName();
        view.duration = cacheManager.getCache().getTtl(cacheName, key);
        return view;
    }
}
