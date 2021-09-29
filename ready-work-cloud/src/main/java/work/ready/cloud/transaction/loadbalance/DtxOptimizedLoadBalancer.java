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

package work.ready.cloud.transaction.loadbalance;

import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.loadbalance.RoundRobinLoadBalancer;
import work.ready.cloud.registry.base.URL;
import work.ready.cloud.transaction.DistributedTransactionManager;
import work.ready.cloud.transaction.tracing.TracingContext;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;

import java.util.List;
import java.util.function.Function;

public class DtxOptimizedLoadBalancer extends RoundRobinLoadBalancer {
    private static Log logger = LogFactory.getLog(DtxOptimizedLoadBalancer.class);
    public static final String name = "DtxOptimized";

    public DtxOptimizedLoadBalancer() {
        if(logger.isInfoEnabled()) {
            logger.info("A DtxOptimizedLoadBalancer instance is started");
        }
    }

    @Override
    public URL select(String serviceId, List<URL> urls, Function<URL, Integer> unstableCheck, String requestKey) {
        if (!TracingContext.tracing().hasGroup() || DistributedTransactionManager.SERVICE_ID.equals(serviceId)) {
            return super.select(serviceId, urls, unstableCheck, requestKey);
        }
        Ready.getApp(appList->appList.forEach(
                app->TracingContext.tracing().addApp(app.getName(),
                        Cloud.getPublishIp() + ':' + (app.webServer().getHttpPort() != null ? app.webServer().getHttpPort() : app.webServer().getHttpsPort()))));

        String[] map = TracingContext.tracing().appMap();
        for(int i = 0; i < map.length; i++) {
            if(i % 2 == 0 && map[i].equals(serviceId)) {
                for(URL url : urls) {
                    if(map[i + 1].equals(url.getHost() + ':' + url.getPort())) {
                        logger.debug("DTX optimized loadBalancer choose node: " + url);
                        return url;
                    }
                }
            }
        }

        URL url = super.select(serviceId, urls, unstableCheck, requestKey);
        if(url != null) {
            TracingContext.tracing().addApp(serviceId, url.getHost() + ':' + url.getPort());
        }
        return url;
    }

}
