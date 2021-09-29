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

package work.ready.cloud.registry;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.lang.IgniteFuture;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;
import work.ready.cloud.ReadyCloud;
import work.ready.cloud.client.clevercall.CircuitBreaker;
import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.registry.base.NotifierRegistry;
import work.ready.cloud.registry.base.ServiceNotifier;
import work.ready.cloud.registry.base.URL;
import work.ready.cloud.registry.base.URLParam;
import work.ready.core.event.cloud.Event;
import work.ready.core.event.GeneralEvent;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.module.Application;
import work.ready.core.server.Ready;
import work.ready.core.tools.define.TriTuple;

import javax.cache.Cache;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class ReadyRegistry extends NotifierRegistry {
    private static final Log logger = LogFactory.getLog(ReadyRegistry.class);
    private Cloud cloud;
    private RegistryConfig config;
    public static final String availableCacheName = "ready.work:registry:available";
    public static final String unavailableCacheName = "ready.work:registry:unavailable";
    public static final String stabilityCacheName = "ready.work:registry:stability";
    private IgniteCache<String, Set<URL>> AVAILABLE_CACHE;
    private IgniteCache<String, Set<URL>> UNAVAILABLE_CACHE;
    private IgniteCache<String, Map<URL, Integer>> STABILITY_CACHE;
    private HeartbeatManager heartbeatManager;
    
    private Map<String, List<URL>> applicationUrls = new HashMap<>();

    private ConcurrentHashMap<String, List<URL>> serviceCache = new ConcurrentHashMap<String, List<URL>>();
    private ConcurrentHashMap<URL, Integer> stabilityCache = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ConcurrentHashMap<URL, ServiceNotifier>> serviceListeners = new ConcurrentHashMap<String, ConcurrentHashMap<URL, ServiceNotifier>>();
    private ThreadPoolExecutor notifyExecutor;

    private final ReentrantLock lock = new ReentrantLock();

    public ReadyRegistry(Cloud cloud, URL url) {
        super(url);
        this.cloud = cloud;
        config = ReadyCloud.getConfig().getRegistry();
        CacheConfiguration<String, Set<URL>> availableConfig = new CacheConfiguration<>();
        availableConfig.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        availableConfig.setCacheMode(CacheMode.REPLICATED);
        availableConfig.setDataRegionName(Cloud.WITHOUT_PERSISTENCE);
        availableConfig.setBackups(1);
        availableConfig.setName(availableCacheName);
        AVAILABLE_CACHE = cloud.getOrCreateCache(availableConfig);
        CacheConfiguration<String, Set<URL>> unavailableConfig = new CacheConfiguration<>();
        unavailableConfig.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        unavailableConfig.setCacheMode(CacheMode.REPLICATED);
        unavailableConfig.setDataRegionName(Cloud.WITHOUT_PERSISTENCE);
        unavailableConfig.setBackups(1);
        unavailableConfig.setName(unavailableCacheName);
        UNAVAILABLE_CACHE = cloud.getOrCreateCache(unavailableConfig);
        CacheConfiguration<String, Map<URL, Integer>> unstableConfig = new CacheConfiguration<>();
        unstableConfig.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        unstableConfig.setCacheMode(CacheMode.REPLICATED);
        unstableConfig.setDataRegionName(Cloud.WITHOUT_PERSISTENCE);
        unstableConfig.setBackups(1);
        unstableConfig.setName(stabilityCacheName);
        STABILITY_CACHE = cloud.getOrCreateCache(unstableConfig);

        if(!cloud.configuration().isClientMode()) {
            
            TriTuple<IgniteFuture<Boolean>, IgniteFuture<Boolean>, IgniteFuture<Boolean>> states = new TriTuple<>();
            AVAILABLE_CACHE.rebalance().listen(states::set1);
            UNAVAILABLE_CACHE.rebalance().listen(states::set2);
            STABILITY_CACHE.rebalance().listen(states::set3);
            try {
                while (states.get1() == null || states.get2() == null || states.get3() == null ||
                        !states.get1().isDone() || !states.get2().isDone() || !states.get3().isDone()) {
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("InterruptedException while waiting for cache rebalancing.");
            }
        }

        new EventHandler().listen(cloud, this);

        ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<Runnable>(200);
        notifyExecutor = new ThreadPoolExecutor(10, 30, 30 * 1000, TimeUnit.MILLISECONDS, workQueue);
    }

    protected void availableCacheChanged(String serviceId) {
        ConcurrentHashMap<String, List<URL>> serviceUrls = new ConcurrentHashMap<>();
        serviceUrls.put(serviceId, new LinkedList<>(getAvailableService(serviceId)));
        updateServiceCache(serviceId, serviceUrls, true);
        stabilityCacheChanged(null);
    }

    protected void stabilityCacheChanged(String uri) {
        if(uri == null) {
            Iterator<javax.cache.Cache.Entry<String, Map<URL, Integer>>> it = STABILITY_CACHE.iterator();
            ConcurrentHashMap<URL, Integer> newCache = new ConcurrentHashMap<>();
            while (it.hasNext()) {
                it.next().getValue().forEach(newCache::put);
            }
            stabilityCache = newCache;
        } else {
            Map<URL, Integer> cache = STABILITY_CACHE.get(uri);
            if (cache != null) cache.forEach((key, val) -> {
                if(stabilityCache.get(key) != null && stabilityCache.get(key) > val) {
                    if(val <= CircuitBreaker.MIN_UNSTABLE_LEVEL) {
                        CircuitBreaker.tryHalfOpen(key);    
                    }
                }
                stabilityCache.put(key, val);
            });
        }
    }

    @Override
    public void startHeartbeat() {
        if(heartbeatManager == null) {
            heartbeatManager = new HeartbeatManager(this, new HealthClient(), config.getToken());
            heartbeatManager.start();
            heartbeatManager.setHeartbeatOpen(true);
        }
    }

    @Override
    public void stopHeartbeat() {
        if(heartbeatManager != null) {
            heartbeatManager.setHeartbeatOpen(false);
        }
    }

    @Override
    protected void subscribeService(URL url, ServiceNotifier listener) {
        addServiceListener(url, listener);
    }

    @Override
    protected void unsubscribeService(URL url, ServiceNotifier listener) {
        ConcurrentHashMap<URL, ServiceNotifier> listeners = serviceListeners.get(url.getPath());
        if (listeners != null) {
            listeners.remove(url);
        }
    }

    private synchronized void addServiceListener(URL url, ServiceNotifier serviceNotifier) {
        String service = url.getPath();
        ConcurrentHashMap<URL, ServiceNotifier> map = serviceListeners.get(service);
        if (map == null) {
            serviceListeners.putIfAbsent(service, new ConcurrentHashMap<>());
            map = serviceListeners.get(service);
        }
        map.put(url, serviceNotifier);
    }

    @Override
    protected List<URL> discoverService(URL url) {
        String serviceId = url.getPath();
        List<URL> urls = serviceCache.get(serviceId);
        if (urls == null || urls.isEmpty()) {
            synchronized (serviceId.intern()) {
                urls = serviceCache.get(serviceId);
                if (urls == null || urls .isEmpty()) {
                    ConcurrentHashMap<String, List<URL>> serviceUrls = new ConcurrentHashMap<>();
                    serviceUrls.put(serviceId, new LinkedList<>(getAvailableService(url.getPath())));
                    updateServiceCache(serviceId, serviceUrls, false);
                    urls = serviceCache.get(serviceId);
                }
            }
        }
        return getMatchedDiscoverService(url, urls);
    }

    @Override
    public void register(String nodeType, String serviceId, String serviceVersion, String protocol, int port, Map<String, String> param) {
        try {
            String ipAddress = cloud.getPublishIp();
            Map<String, String> parameters = param == null ? new HashMap<>() : new HashMap<>(param);
            parameters.put(URLParam.nodeType.getName(), nodeType);
            parameters.putIfAbsent(URLParam.group.getName(), URLParam.group.getValue());
            parameters.putIfAbsent(URLParam.project.getName(), Ready.getBootstrapConfig().getProject());
            parameters.putIfAbsent(URLParam.projectVersion.getName(), Ready.getBootstrapConfig().getVersion());
            parameters.put(URLParam.serviceVersion.getName(), serviceVersion);
            parameters.put(URLParam.nodeId.getName(), Cloud.cluster().localNode().id().toString());
            parameters.put(URLParam.nodeConsistentId.getName(), Cloud.cluster().localNode().consistentId().toString());
            parameters.put(URLParam.environment.getName(), Ready.getBootstrapConfig().getActiveProfile());
            URL applicationUrl = new URL(protocol, ipAddress, port, serviceId, parameters);
            if (logger.isInfoEnabled()) logger.info("register service: " + applicationUrl.toFullStr());
            register(applicationUrl);
        } catch (Exception e) {
            if (logger.isErrorEnabled())
                logger.error(e,"Failed to register service.");
        }
    }

    @Override
    public void register(String nodeType, Application application, String serviceId, String protocol, int port, Map<String, String> param) {
        try {
            String ipAddress = cloud.getPublishIp();

            Map<String, String> parameters = param == null ? new HashMap<>() : new HashMap<>(param);
            parameters.put(URLParam.nodeType.getName(), nodeType);
            parameters.putIfAbsent(URLParam.group.getName(), URLParam.group.getValue());
            parameters.putIfAbsent(URLParam.project.getName(), Ready.getBootstrapConfig().getProject());
            parameters.putIfAbsent(URLParam.projectVersion.getName(), Ready.getBootstrapConfig().getVersion());
            parameters.putIfAbsent(URLParam.serviceVersion.getName(), application.getVersion());
            parameters.put(URLParam.nodeId.getName(), Cloud.cluster().localNode().id().toString());
            parameters.put(URLParam.nodeConsistentId.getName(), Cloud.cluster().localNode().consistentId().toString());
            parameters.put(URLParam.environment.getName(), Ready.getBootstrapConfig().getActiveProfile());
            URL applicationUrl = new URL(protocol, ipAddress, port, serviceId, parameters);
            if (logger.isInfoEnabled()) logger.info("register service: " + applicationUrl.toFullStr());
            register(applicationUrl);

            var urlList = applicationUrls.computeIfAbsent(application.getName(), k -> new ArrayList<>());
            urlList.add(applicationUrl);
        } catch (Exception e) {
            if (logger.isErrorEnabled())
                logger.error(e,"Failed to register service.");
        }
    }

    @Override
    protected void doRegister(URL url) {
        try {
            lock.lock(); 
            try (Transaction tx = Cloud.transactions().txStart(TransactionConcurrency.OPTIMISTIC, TransactionIsolation.SERIALIZABLE)) {
                String serviceId = url.getPath();
                Set<URL> unavailableServiceSet = removeUnavailableService(url);
                AVAILABLE_CACHE.put(serviceId, removeAvailableService(url));
                unavailableServiceSet.add(url);
                UNAVAILABLE_CACHE.put(serviceId, unavailableServiceSet);
                setStabilityLevel(url, CircuitBreaker.MIN_UNSTABLE_LEVEL);
                tx.commit();
                Ready.post(new GeneralEvent(Event.REGISTRY_AVAILABLE_SERVICE_CHANGED, this, serviceId));
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void unregister(Application application) {
        if(applicationUrls.containsKey(application.getName()))
        for(URL applicationUrl: applicationUrls.get(application.getName())) {
            unregister(applicationUrl);
            if (logger.isInfoEnabled())
                logger.info("unregister applicationUrl " + applicationUrl);
        }
    }

    @Override
    protected void doUnregister(URL url) {
        try {
            lock.lock(); 
            try (Transaction tx = Cloud.transactions().txStart(TransactionConcurrency.OPTIMISTIC, TransactionIsolation.SERIALIZABLE)) {
                String serviceId = url.getPath();
                UNAVAILABLE_CACHE.put(serviceId, removeUnavailableService(url));
                AVAILABLE_CACHE.put(serviceId, removeAvailableService(url));
                STABILITY_CACHE.remove(url.getUri());
                tx.commit();
                Ready.post(new GeneralEvent(Event.REGISTRY_AVAILABLE_SERVICE_CHANGED, this, serviceId));
            }
        } finally {
            lock.unlock();
        }
    }

    private Set<URL> removeUnavailableService(URL url) {
        Set<URL> unavailableServiceSet = UNAVAILABLE_CACHE.get(url.getPath());
        if(unavailableServiceSet == null){ unavailableServiceSet = new LinkedHashSet<>(); }
        unavailableServiceSet.remove(url);
        return unavailableServiceSet;
    }

    private Set<URL> addUnavailableService(URL url) {
        Set<URL> unavailableServiceSet = UNAVAILABLE_CACHE.get(url.getPath());
        if(unavailableServiceSet == null){ unavailableServiceSet = new LinkedHashSet<>(); }
        unavailableServiceSet.add(url);
        return unavailableServiceSet;
    }

    @Override
    public void setStabilityLevel(URL url, int unstableLevel) {
        if(unstableLevel > CircuitBreaker.MAX_UNSTABLE_LEVEL)
            unstableLevel = CircuitBreaker.MAX_UNSTABLE_LEVEL;
        if(unstableLevel < CircuitBreaker.MIN_UNSTABLE_LEVEL)
            unstableLevel = CircuitBreaker.MIN_UNSTABLE_LEVEL;
        STABILITY_CACHE.put(url.getUri(), new HashMap<>(Map.of(url, unstableLevel)));
        stabilityCache.put(url, unstableLevel);
    }

    @Override
    public int getStabilityLevel(URL url) {
        if(stabilityCache.containsKey(url)) {
            return stabilityCache.get(url);
        }
        return CircuitBreaker.MIN_UNSTABLE_LEVEL;   
    }

    @Override
    public Map<URL, Integer> getStabilityLevel() {
        return Collections.unmodifiableMap(stabilityCache);
    }

    @Override
    public Set<URL> getStabilityUrls() {
        return Collections.unmodifiableSet(stabilityCache.keySet());
    }

    private Set<URL> getAvailableService(String serviceId) {
        Set<URL> availableServiceSet = AVAILABLE_CACHE.get(serviceId);
        if(availableServiceSet == null){ availableServiceSet = new LinkedHashSet<>(); }
        return availableServiceSet;
    }

    private Set<URL> removeAvailableService(URL url) {
        Set<URL> availableServiceSet = AVAILABLE_CACHE.get(url.getPath());
        if(availableServiceSet == null){ availableServiceSet = new LinkedHashSet<>(); }
        availableServiceSet.remove(url);
        return availableServiceSet;
    }

    private Set<URL> addAvailableService(URL url) {
        Set<URL> availableServiceSet = AVAILABLE_CACHE.get(url.getPath());
        if(availableServiceSet == null){ availableServiceSet = new LinkedHashSet<>(); }
        availableServiceSet.add(url);
        return availableServiceSet;
    }

    @Override
    protected void doAvailable(URL url) {
        try {
            lock.lock(); 
            try (Transaction tx = Cloud.transactions().txStart(TransactionConcurrency.OPTIMISTIC, TransactionIsolation.SERIALIZABLE)) {
                if (url == null) {
                    for (URL u : getRegisteredServiceUrlsOnThisNode()) {
                        UNAVAILABLE_CACHE.put(u.getPath(), removeUnavailableService(u));
                        Set<URL> availableServiceSet = removeAvailableService(u);
                        availableServiceSet.add(u);
                        AVAILABLE_CACHE.put(u.getPath(), availableServiceSet);
                        setStabilityLevel(u, CircuitBreaker.MIN_UNSTABLE_LEVEL);
                    }
                } else {
                    UNAVAILABLE_CACHE.put(url.getPath(), removeUnavailableService(url));
                    Set<URL> availableServiceSet = removeAvailableService(url);
                    availableServiceSet.add(url);
                    AVAILABLE_CACHE.put(url.getPath(), availableServiceSet);
                    setStabilityLevel(url, CircuitBreaker.MIN_UNSTABLE_LEVEL);
                }
                tx.commit();
                
                if (url == null) {
                    for (URL u : getRegisteredServiceUrlsOnThisNode()) {
                        Ready.post(new GeneralEvent(Event.REGISTRY_AVAILABLE_SERVICE_CHANGED, this, u.getPath()));
                    }
                } else {
                    Ready.post(new GeneralEvent(Event.REGISTRY_AVAILABLE_SERVICE_CHANGED, this, url.getPath()));
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected void doUnavailable(URL url) {
        try {
            lock.lock();
            try (Transaction tx = Cloud.transactions().txStart(TransactionConcurrency.OPTIMISTIC, TransactionIsolation.SERIALIZABLE)) {
                if (url == null) {
                    for (URL u : getRegisteredServiceUrlsOnThisNode()) {
                        AVAILABLE_CACHE.put(u.getPath(), removeAvailableService(u));
                        Set<URL> unavailableServiceSet = removeUnavailableService(u);
                        unavailableServiceSet.add(u);
                        UNAVAILABLE_CACHE.put(u.getPath(), unavailableServiceSet);
                        STABILITY_CACHE.remove(u.getUri());
                        stabilityCache.remove(u);
                    }
                } else {
                    AVAILABLE_CACHE.put(url.getPath(), removeAvailableService(url));
                    Set<URL> unavailableServiceSet = removeUnavailableService(url);
                    unavailableServiceSet.add(url);
                    UNAVAILABLE_CACHE.put(url.getPath(), unavailableServiceSet);
                    STABILITY_CACHE.remove(url.getUri());
                    stabilityCache.remove(url);
                }
                tx.commit();
                
                if (url == null) {
                    for (URL u : getRegisteredServiceUrlsOnThisNode()) {
                        Ready.post(new GeneralEvent(Event.REGISTRY_AVAILABLE_SERVICE_CHANGED, this, u.getPath()));
                    }
                } else {
                    Ready.post(new GeneralEvent(Event.REGISTRY_AVAILABLE_SERVICE_CHANGED, this, url.getPath()));
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected void doUnavailableByNodeId(String nodeId) {
        try {
            lock.lock(); 
            try (Transaction tx = Cloud.transactions().txStart(TransactionConcurrency.OPTIMISTIC, TransactionIsolation.SERIALIZABLE)) {
                Iterator<javax.cache.Cache.Entry<String, Set<URL>>> it = AVAILABLE_CACHE.iterator();
                while (it.hasNext()) {
                    Cache.Entry<String, Set<URL>> entry = it.next();
                    String serviceId = entry.getKey();
                    Set<URL> availableServiceSet = entry.getValue();
                    if (availableServiceSet == null) {
                        availableServiceSet = new LinkedHashSet<>();
                    }
                    Set<URL> unavailableServiceSet = UNAVAILABLE_CACHE.get(serviceId);
                    if (unavailableServiceSet == null) {
                        unavailableServiceSet = new LinkedHashSet<>();
                    }
                    Iterator<URL> availableIt = availableServiceSet.iterator();
                    while (availableIt.hasNext()) {
                        URL url = availableIt.next();
                        String id = url.getParameter(URLParam.nodeConsistentId.getName());
                        if (nodeId.equals(id)) {
                            availableIt.remove();
                            unavailableServiceSet.add(url);
                            STABILITY_CACHE.remove(url.getUri());
                            stabilityCache.remove(url);
                        }
                    }
                    AVAILABLE_CACHE.put(serviceId, availableServiceSet);
                    UNAVAILABLE_CACHE.put(serviceId, unavailableServiceSet);
                }
                tx.commit();
                
                it = AVAILABLE_CACHE.iterator();
                while (it.hasNext()) {
                    Ready.post(new GeneralEvent(Event.REGISTRY_AVAILABLE_SERVICE_CHANGED, this, it.next().getKey()));
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private void updateServiceCache(String serviceId, ConcurrentHashMap<String, List<URL>> serviceUrls, boolean needNotify) {
        if (serviceUrls != null && !serviceUrls.isEmpty()) {
            List<URL> cachedUrls = serviceCache.get(serviceId);
            List<URL> newUrls = serviceUrls.get(serviceId);
            try {
                logger.trace("update service cache serviceUrls = %s", Ready.config().getJsonMapper().writeValueAsString(serviceUrls));
            } catch(Exception e) {
            }
            boolean changed = true;
            if (isSame(newUrls, cachedUrls)) {
                changed = false;
            } else {
                serviceCache.put(serviceId, newUrls);
            }
            if (changed && needNotify) {
                notifyExecutor.execute(new NotifyService(serviceId, newUrls));
            }
        }
    }

    private class NotifyService implements Runnable {
        private String service;
        private List<URL> urls;

        public NotifyService(String service, List<URL> urls) {
            this.service = service;
            this.urls = urls;
        }

        @Override
        public void run() {
            ConcurrentHashMap<URL, ServiceNotifier> listeners = serviceListeners.get(service);
            if (listeners != null) {
                synchronized (listeners) {
                    logger.debug("notify listeners that " + service + " changed to: " + urls);
                    for (Map.Entry<URL, ServiceNotifier> entry : listeners.entrySet()) {
                        ServiceNotifier serviceNotifier = entry.getValue();
                        serviceNotifier.notifyService(entry.getKey(), getUrl(), getMatchedDiscoverService(entry.getKey(), urls));
                    }
                }
            } else {
                logger.debug("no listeners to notify, " + service + " changed to: " + urls + "");
            }
        }
    }

    private boolean isSame(List<URL> urls1, List<URL> urls2) {
        if(urls1 == null && urls2 == null) {
            return true;
        }
        if (urls1 == null || urls2 == null) {
            return false;
        }
        if (urls1.size() != urls2.size()) {
            return false;
        }
        return urls1.containsAll(urls2);
    }
}
