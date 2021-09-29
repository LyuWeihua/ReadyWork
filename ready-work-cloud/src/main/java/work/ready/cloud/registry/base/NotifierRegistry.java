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
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public abstract class NotifierRegistry extends FallbackRegistry {
    private static final Log logger = LogFactory.getLog(NotifierRegistry.class);
    private ConcurrentHashMap<URL, NotifyManager> notifyManagerMap;

    public NotifierRegistry(URL url) {
        super(url);
        notifyManagerMap = new ConcurrentHashMap<>();
        if(logger.isInfoEnabled()) logger.info("NotifierRegistry initialize: " + url.toSimpleString());
    }

    @Override
    protected void doSubscribe(URL url, final NotifyListener listener) {
        if(logger.isInfoEnabled()) logger.info("NotifierRegistry subscribe: " + url.toSimpleString());
        URL urlCopy = url.createCopy();
        NotifyManager manager = getNotifyManager(urlCopy);
        manager.addNotifyListener(listener);

        subscribeService(urlCopy, manager);

        List<URL> urls = doDiscover(urlCopy);
        if (urls != null && urls.size() > 0) {
            this.notify(urlCopy, listener, urls);
        }
    }

    @Override
    protected void doUnsubscribe(URL url, NotifyListener listener) {
        if(logger.isInfoEnabled()) logger.info("NotifierRegistry unsubscribe: " + url.toSimpleString());
        URL urlCopy = url.createCopy();
        NotifyManager manager = notifyManagerMap.get(urlCopy);

        manager.removeNotifyListener(listener);
        unsubscribeService(urlCopy, manager);

    }

    @Override
    protected List<URL> doDiscover(URL url) {
        if(logger.isInfoEnabled()) logger.info("NotifierRegistry discover: " + url.toSimpleString());
        List<URL> finalResult = discoverService(url.createCopy());
        if(logger.isInfoEnabled()) logger.info("NotifierRegistry discover size: " +
                (finalResult==null ? 0 : finalResult.size()) + ", result:" + (finalResult==null ? null: finalResult.toString()));
        return finalResult;
    }

    protected NotifyManager getNotifyManager(URL urlCopy) {
        NotifyManager manager = notifyManagerMap.get(urlCopy);
        if (manager == null) {
            manager = new NotifyManager(urlCopy);
            manager.setRegistry(this);
            NotifyManager manager1 = notifyManagerMap.putIfAbsent(urlCopy, manager);
            if (manager1 != null) manager = manager1;
        }
        return manager;
    }

    public ConcurrentHashMap<URL, NotifyManager> getNotifyManagerMap() {
        return notifyManagerMap;
    }

    protected abstract void subscribeService(URL url, ServiceNotifier listener);

    protected abstract void unsubscribeService(URL url, ServiceNotifier listener);

    protected abstract List<URL> discoverService(URL url);

}
