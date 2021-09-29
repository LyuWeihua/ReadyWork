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

import java.util.List;

public class NotifyManager implements ServiceNotifier {
    private static final Log logger = LogFactory.getLog(NotifyManager.class);
    private static final String REGISTRY_IS_NULL = "ERROR10024";

    private URL refUrl;
    private ConcurrentHashSet<NotifyListener> notifySet;
    private NotifierRegistry registry;

    public NotifyManager(URL refUrl) {
        if(logger.isInfoEnabled()) logger.info("NotifyManager initialize: " + refUrl.toFullStr());
        this.refUrl = refUrl;
        notifySet = new ConcurrentHashSet<NotifyListener>();
    }

    @Override
    public void notifyService(URL serviceUrl, URL registryUrl, List<URL> urls) {
        if (registry == null) {
            throw new FrameworkException(new Status(REGISTRY_IS_NULL));
        }

        for (NotifyListener notifyListener : notifySet) {
            registry.notify(serviceUrl, notifyListener, urls);
            
        }
    }

    public void addNotifyListener(NotifyListener notifyListener) {
        notifySet.add(notifyListener);
    }

    public void removeNotifyListener(NotifyListener notifyListener) {
        notifySet.remove(notifyListener);
    }

    public void setRegistry(NotifierRegistry registry) {
        this.registry = registry;
    }

}
