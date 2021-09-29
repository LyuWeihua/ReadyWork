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
import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.registry.base.URL;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class LocalFirstLoadBalancer implements LoadBalancer {
    private static Log logger = LogFactory.getLog(LocalFirstLoadBalancer.class);
    private static ThreadLocalRandom random = ThreadLocalRandom.current();
    public static final String name = "LocalFirst";

    private AtomicInteger idx = new AtomicInteger((int)(Math.random()*10));
    static String ip = "0.0.0.0";

    public LocalFirstLoadBalancer() {
        
        ip = Cloud.getPublishIp();
        if(logger.isInfoEnabled()) logger.info("A LocalFirstLoadBalance instance is started");
    }

    @Override
    public URL select(String serviceId, List<URL> urls, Function<URL, Integer> unstableCheck,  String requestKey) {
    	
        List<URL> localUrls = searchLocalUrls(urls, ip);
        ArrayList<URL> droppedUnstableUrls = new ArrayList<>();
        int index = getNextPositive();
        URL url = null;
        if(localUrls.size() > 0) {
             if(localUrls.size() == 1) {
                 url = localUrls.get(0);
                 if(unstableCheck.apply(url) >= CircuitBreaker.MAX_UNSTABLE_LEVEL) {
                     if(logger.isWarnEnabled()) logger.warn(url + " is very unstable, temporarily unavailable.");
                     url = null;
                 }
             } else {
                
                 url = doSelect(localUrls, index, unstableCheck, droppedUnstableUrls);
             }
        }
        
        if(url == null) {
            url = doSelect(urls, index, unstableCheck, droppedUnstableUrls);
        }

        if(url == null && droppedUnstableUrls.size() > 0) {
            url = droppedUnstableUrls.get(index % droppedUnstableUrls.size());
        }
        return url;
    }

    protected URL doSelect(List<URL> urls, int index, Function<URL, Integer> unstableCheck, ArrayList<URL> droppedUnstableUrls) {
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

        return null;
    }

    private List<URL> searchLocalUrls(List<URL> urls, String ip) {
        List<URL> localUrls = new ArrayList<URL>();
        long local = ipToLong(ip);
        for (URL url : urls) {
            long tmp = ipToLong(url.getHost());
            if (local != 0 && local == tmp) {
                localUrls.add(url);
            }
        }
        return localUrls;
    }

    public static long ipToLong(final String address) {
        final String[] addressBytes = address.split("\\.");
        int length = addressBytes.length;
        if (length < 3) {
            return 0;
        }
        long ip = 0;
        try {
            for (int i = 0; i < 4; i++) {
                ip <<= 8;
                ip |= Integer.parseInt(addressBytes[i]);
            }
        } catch (Exception e) {
            logger.warn("Warn ipToLong address is wrong: address =" + address);
        }
        return ip;
    }

    private int getNextPositive() {
        return getPositive(idx.incrementAndGet());
    }
}
