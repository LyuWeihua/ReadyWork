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
package work.ready.cloud.registry.base;

import work.ready.cloud.registry.NotifyListener;
import work.ready.core.exception.FrameworkException;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.tools.define.ConcurrentHashSet;
import work.ready.core.service.status.Status;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class FallbackRegistry extends AbstractRegistry {
    private static final Log logger = LogFactory.getLog(FallbackRegistry.class);
    private static final String REGISTER_ERROR = "ERROR10020";
    private static final String UNREGISTER_ERROR = "ERROR10021";
    private static final String SUBSCRIBE_ERROR = "ERROR10022";
    private static final String UNSUBSCRIBE_ERROR = "ERROR10023";

    private Set<URL> failedRegistered = new ConcurrentHashSet<>();
    private Set<URL> failedUnregistered = new ConcurrentHashSet<>();
    private ConcurrentHashMap<URL, ConcurrentHashSet<NotifyListener>> failedSubscribed =
            new ConcurrentHashMap<>();
    private ConcurrentHashMap<URL, ConcurrentHashSet<NotifyListener>> failedUnsubscribed =
            new ConcurrentHashMap<>();

    private static ScheduledExecutorService retryExecutor = Executors.newScheduledThreadPool(1);

    public FallbackRegistry(URL url) {
        super(url);
        long retryPeriod = url.getIntParameter(URLParam.registryRetryPeriod.getName(), URLParam.registryRetryPeriod.getIntValue());
        retryExecutor.scheduleAtFixedRate(() -> {
            try {
                retry();
            } catch (Exception e) {
                logger.error(e, "Exception when retry in fallback registry");
            }
        }, retryPeriod, retryPeriod, TimeUnit.MILLISECONDS);
    }

    @Override
    public void register(URL url) {
        failedRegistered.remove(url);
        failedUnregistered.remove(url);
        try {
            super.register(url);
        } catch (Exception e) {
            if (isCheckingUrls(getUrl(), url)) {
                throw new FrameworkException(new Status(REGISTER_ERROR, registryClassName, url, getUrl()), e);
            }
            failedRegistered.add(url);
        }
    }

    @Override
    public void unregister(URL url) {
        failedRegistered.remove(url);
        failedUnregistered.remove(url);
        try {
            super.unregister(url);
        } catch (Exception e) {
            if (isCheckingUrls(getUrl(), url)) {
                throw new FrameworkException(new Status(UNREGISTER_ERROR, registryClassName, url, getUrl()), e);
            }
            failedUnregistered.add(url);
        }
    }

    @Override
    public void subscribe(URL url, NotifyListener listener) {
        removeForFailedSubAndUnsub(url, listener);

        try {
            super.subscribe(url, listener);
        } catch (Exception e) {
            List<URL> cachedUrls = getMatchedDiscoverService(url, getCachedUrls(url));
            if (cachedUrls != null && cachedUrls.size() > 0) {
                listener.notify(getUrl(), cachedUrls);
            } else if (isCheckingUrls(getUrl(), url)) {
                logger.error(e, "[%s] failed to subscribe %s from %s", registryClassName, url, getUrl());
                throw new FrameworkException(new Status(SUBSCRIBE_ERROR, registryClassName, url, getUrl()), e);
            }
            addToFailedMap(failedSubscribed, url, listener);
        }
    }

    @Override
    public void unsubscribe(URL url, NotifyListener listener) {
        removeForFailedSubAndUnsub(url, listener);

        try {
            super.unsubscribe(url, listener);
        } catch (Exception e) {
            if (isCheckingUrls(getUrl(), url)) {
                throw new FrameworkException(new Status(UNSUBSCRIBE_ERROR, registryClassName, url, getUrl()), e);
            }
            addToFailedMap(failedUnsubscribed, url, listener);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<URL> discover(URL url) {
        try {
            return super.discover(url);
        } catch (Exception e) {
            
            logger.error(e, String.format("Failed to discover url: %s in registry (%s)", url, getUrl()));
            return Collections.EMPTY_LIST;
        }
    }

    private boolean isCheckingUrls(URL... urls) {
        for (URL url : urls) {
            if (!Boolean.parseBoolean(url.getParameter(URLParam.check.getName(), URLParam.check.getValue()))) {
                return false;
            }
        }
        return true;
    }

    private void removeForFailedSubAndUnsub(URL url, NotifyListener listener) {
        Set<NotifyListener> listeners = failedSubscribed.get(url);
        if (listeners != null) {
            listeners.remove(listener);
        }
        listeners = failedUnsubscribed.get(url);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    private void addToFailedMap(ConcurrentHashMap<URL, ConcurrentHashSet<NotifyListener>> failedMap, URL url, NotifyListener listener) {
        Set<NotifyListener> listeners = failedMap.get(url);
        if (listeners == null) {
            failedMap.putIfAbsent(url, new ConcurrentHashSet<>());
            listeners = failedMap.get(url);
        }
        listeners.add(listener);
    }

    private void retry() {
        if (!failedRegistered.isEmpty()) {
            Set<URL> failed = new HashSet<>(failedRegistered);
            logger.info("[%s] Retry register %s", registryClassName, failed);
            try {
                for (URL url : failed) {
                    super.register(url);
                    failedRegistered.remove(url);
                }
            } catch (Exception e) {
                logger.error(e, "[%s] Failed to retry register, retry later, failedRegistered.size=%s",
                        registryClassName, failedRegistered.size());
            }

        }
        if (!failedUnregistered.isEmpty()) {
            Set<URL> failed = new HashSet<>(failedUnregistered);
            logger.info("[%s] Retry unregister %s", registryClassName, failed);
            try {
                for (URL url : failed) {
                    super.unregister(url);
                    failedUnregistered.remove(url);
                }
            } catch (Exception e) {
                logger.error(e, "[%s] Failed to retry unregister, retry later, failedUnregistered.size=%s",
                        registryClassName, failedUnregistered.size());
            }

        }
        if (!failedSubscribed.isEmpty()) {
            Map<URL, Set<NotifyListener>> failed = new HashMap<>(failedSubscribed);
            for (Map.Entry<URL, Set<NotifyListener>> entry : new HashMap<>(failed).entrySet()) {
                if (entry.getValue() == null || entry.getValue().size() == 0) {
                    failed.remove(entry.getKey());
                }
            }
            if (failed.size() > 0) {
                logger.info("[%s] Retry subscribe %s", registryClassName, failed);
                try {
                    for (Map.Entry<URL, Set<NotifyListener>> entry : failed.entrySet()) {
                        URL url = entry.getKey();
                        Set<NotifyListener> listeners = entry.getValue();
                        for (NotifyListener listener : listeners) {
                            super.subscribe(url, listener);
                            listeners.remove(listener);
                        }
                    }
                } catch (Exception e) {
                    logger.error(e, "[%s] Failed to retry subscribe, retry later, failedSubscribed.size=%s",
                            registryClassName, failedSubscribed.size());
                }
            }
        }
        if (!failedUnsubscribed.isEmpty()) {
            Map<URL, Set<NotifyListener>> failed = new HashMap<>(failedUnsubscribed);
            for (Map.Entry<URL, Set<NotifyListener>> entry : new HashMap<>(failed).entrySet()) {
                if (entry.getValue() == null || entry.getValue().size() == 0) {
                    failed.remove(entry.getKey());
                }
            }
            if (failed.size() > 0) {
                logger.info("[%s] Retry unsubscribe %s", registryClassName, failed);
                try {
                    for (Map.Entry<URL, Set<NotifyListener>> entry : failed.entrySet()) {
                        URL url = entry.getKey();
                        Set<NotifyListener> listeners = entry.getValue();
                        for (NotifyListener listener : listeners) {
                            super.unsubscribe(url, listener);
                            listeners.remove(listener);
                        }
                    }
                } catch (Exception e) {
                    logger.error(e, "[%s] Failed to retry unsubscribe, retry later, failedUnsubscribed.size=%s",
                            registryClassName, failedUnsubscribed.size());
                }
            }
        }

    }

}
