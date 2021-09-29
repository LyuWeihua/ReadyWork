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

package work.ready.cloud.registry.support;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.ScanQuery;
import work.ready.cloud.ReadyCloud;
import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.registry.ReadyRegistry;
import work.ready.cloud.registry.base.URL;
import work.ready.core.handler.Controller;
import work.ready.core.service.result.Result;
import work.ready.core.service.result.Success;

import java.util.*;

public class SupportController extends Controller {

    public Result list(){
        Map<String, Map<String, Set<URL>>> registryMap = new HashMap<>();
        Map<String, Set<URL>> available = new HashMap<>();
        registryMap.put("available", available);

        IgniteCache<String, Set<URL>> availableCache = Cloud.cache(ReadyRegistry.availableCacheName);
        availableCache.query(new ScanQuery<>(null)).forEach(entry -> {
            available.put((String)entry.getKey(), (Set<URL>) entry.getValue());
                });

        Map<String, Set<URL>> unavailable = new HashMap<>();
        registryMap.put("unavailable", unavailable);

        IgniteCache<String, Set<URL>> unavailableCache = Cloud.cache(ReadyRegistry.unavailableCacheName);
        unavailableCache.query(new ScanQuery<>(null)).forEach(entry -> {
            unavailable.put((String)entry.getKey(), (Set<URL>) entry.getValue());
        });

        return Success.of(registryMap);
    }
}
