/**
 *
 * Original work copyright (c) 2016 Network New Technologies Inc.
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
package work.ready.cloud.registry;

import work.ready.cloud.registry.base.URL;
import work.ready.core.module.Application;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface RegistryService {

    void register(URL url);

    void unregister(URL url);

    void available(URL url);

    void unavailable(URL url);

    void unavailableNode(String nodeId);

    void register(String nodeType, String serviceId, String serviceVersion, String protocol, int port, Map<String, String> param);

    void register(String nodeType, Application application, String serviceId, String protocol, int port, Map<String, String> param);

    void unregister(Application application);

    void startHeartbeat();

    void stopHeartbeat();

    void setStabilityLevel(URL url, int unstableLevel);

    int getStabilityLevel(URL url);

    Map<URL, Integer> getStabilityLevel();

    Set<URL> getStabilityUrls();

    URL getUrl();

    Collection<URL> getRegisteredServiceUrlsOnThisNode();
}
