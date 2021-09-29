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
package work.ready.cloud.loadbalance;

import work.ready.cloud.client.clevercall.CircuitBreaker;
import work.ready.cloud.registry.base.URL;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class RoundRobinLoadBalancer implements LoadBalancer {
    private static Log logger = LogFactory.getLog(RoundRobinLoadBalancer.class);
    private static ThreadLocalRandom random = ThreadLocalRandom.current();
    public static final String name = "RoundRobin";

    public RoundRobinLoadBalancer() {
        if(logger.isInfoEnabled()) logger.info("A RoundRobinLoadBalance instance is started");
    }

    private AtomicInteger idx = new AtomicInteger((int)(Math.random()*10));

    @Override
    public URL select(String serviceId, List<URL> urls, Function<URL, Integer> unstableCheck, String requestKey) {
        URL url = null;
        if (urls.size() > 1) {
            url = doSelect(urls, unstableCheck);
        } else if (urls.size() == 1) {
            url = urls.get(0);
            if(unstableCheck.apply(url) >= CircuitBreaker.MAX_UNSTABLE_LEVEL) {
                if(logger.isWarnEnabled()) logger.warn(url + " is very unstable, but there is no choice.");
                
            }
        }
        return url;
    }

    protected URL doSelect(List<URL> urls, Function<URL, Integer> unstableCheck) {
        int index = getNextPositive();
        ArrayList<URL> droppedUnstableUrls = new ArrayList<>();
        for (int i = 0; i < urls.size(); i++) {
            URL url = urls.get((i + index) % urls.size());
            int unstableLevel = unstableCheck.apply(url);
            if(unstableLevel > 0) {
                if(unstableLevel >= CircuitBreaker.MAX_UNSTABLE_LEVEL) {
                    if(logger.isWarnEnabled()) logger.warn(url + " is very unstable, temporarily unavailable.");
                    continue; 
                }
                if(random.nextInt(CircuitBreaker.MIN_UNSTABLE_LEVEL + 1, CircuitBreaker.MAX_UNSTABLE_LEVEL + 1) <= unstableLevel) { 
                    droppedUnstableUrls.add(url);
                    continue;
                }
            }
            if (url != null) {
                return url;
            }
        }

        if(droppedUnstableUrls.size() > 0) {
            return droppedUnstableUrls.get(index % droppedUnstableUrls.size());
        }
        return null;
    }

    private int getNextPositive() {
        return getPositive(idx.incrementAndGet());
    }

}
