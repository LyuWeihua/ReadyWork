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

import work.ready.cloud.ReadyCloud;
import work.ready.cloud.registry.NotifyListener;
import work.ready.cloud.registry.Registry;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;
import work.ready.core.tools.StrUtil;
import work.ready.core.tools.define.ConcurrentHashSet;
import work.ready.core.server.Constant;
import work.ready.core.component.switcher.SwitcherUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

abstract class AbstractRegistry implements Registry {
    private static final Log logger = LogFactory.getLog(AbstractRegistry.class);

    private ConcurrentHashMap<URL, Map<String, List<URL>>> subscribedCategoryResponses =
            new ConcurrentHashMap<>();

    private URL registryUrl;
    private Set<URL> registeredServiceUrls = new ConcurrentHashSet<>();
    protected String registryClassName = this.getClass().getSimpleName();

    AbstractRegistry(URL url) {
        this.registryUrl = url.createCopy();

        SwitcherUtil.registerSwitcherListener(Constant.APPLICATION_READY_SWITCHER, (key, value) -> {
            if (key != null && value != null) {
                if (value) {
                    available(null);
                } else {
                    unavailable(null);
                }
            }
        });
    }

    @Override
    public void register(URL url) {
        if (url == null) {
            logger.warn("[%s] register with malformed param, url is null", registryClassName);
            return;
        }
        if(logger.isInfoEnabled()) logger.info("[%s] Url (%s) will register to Registry [%s]",
                registryClassName, url, registryUrl.getIdentity());
        doRegister(url.createCopy());
        registeredServiceUrls.add(url);
        
        if (SwitcherUtil.isOpen(Constant.APPLICATION_READY_SWITCHER)) {
            available(url);
        }
    }

    @Override
    public void unregister(URL url) {
        if (url == null) {
            logger.warn("[%s] unregister with malformed param, url is null", registryClassName);
            return;
        }
        if(logger.isInfoEnabled()) logger.info("[%s] Url (%s) will unregister from Registry [%s]",
                registryClassName, url, registryUrl.getIdentity());
        doUnregister(url.createCopy());
        registeredServiceUrls.remove(url);
    }

    @Override
    public void subscribe(URL url, NotifyListener listener) {
        if (url == null || listener == null) {
            logger.warn("[%s] subscribe with malformed param, url:%s, listener:%s",
                    registryClassName, url, listener == null ? null : listener.getName());
            return;
        }
        if(logger.isInfoEnabled()) logger.info("[%s] Listener (%s) will subscribe to url (%s) in Registry [%s]",
                registryClassName, listener.getName(), url, registryUrl.getIdentity());
        doSubscribe(url.createCopy(), listener);
    }

    @Override
    public void unsubscribe(URL url, NotifyListener listener) {
        if (url == null || listener == null) {
            logger.warn("[%s] unsubscribe with malformed param, url:%s, listener:%s", registryClassName, url, listener == null ? null : listener.getName());
            return;
        }
        if(logger.isInfoEnabled()) logger.info("[%s] Listener (%s) will unsubscribe from url (%s) in Registry [%s]",
                registryClassName, listener.getName(), url, registryUrl.getIdentity());
        doUnsubscribe(url.createCopy(), listener);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<URL> discover(URL url) {
        if (url == null) {
            logger.warn("[%s] discover with malformed param, refUrl is null", registryClassName);
            return Collections.EMPTY_LIST;
        }
        url = url.createCopy();
        List<URL> results = new ArrayList<>();

        Map<String, List<URL>> categoryUrls = subscribedCategoryResponses.get(url);
        if (categoryUrls != null && categoryUrls.size() > 0) {
            for (List<URL> urls : categoryUrls.values()) {
                for (URL tempUrl : urls) {
                    results.add(tempUrl.createCopy());
                }
            }
        } else {
            List<URL> urlsDiscovered = doDiscover(url);
            if (urlsDiscovered != null) {
                for (URL u : urlsDiscovered) {
                    results.add(u.createCopy());
                }
            }
        }
        return results;
    }

    @Override
    public URL getUrl() {
        return registryUrl;
    }

    @Override
    public Collection<URL> getRegisteredServiceUrlsOnThisNode() {
        return registeredServiceUrls;
    }

    @Override
    public void available(URL url) {
        if(logger.isInfoEnabled()) logger.info("[%s] Url (%s) will set to available to Registry [%s]",
                registryClassName, url, registryUrl.getIdentity());
        if (url != null) {
            doAvailable(url.createCopy());
        } else {
            doAvailable(null);
        }
    }

    @Override
    public void unavailable(URL url) {
        if(logger.isInfoEnabled()) logger.info("[%s] Url (%s) will set to unavailable to Registry [%s]",
                registryClassName, url, registryUrl.getIdentity());
        if (url != null) {
            doUnavailable(url.createCopy());
        } else {
            doUnavailable(null);
        }
    }

    @Override
    public void unavailableNode(String nodeId) {
        if(logger.isInfoEnabled()) logger.info("All services on node [%s] will set to unavailable to Registry [%s]",
                nodeId, registryUrl.getIdentity());
        if (nodeId != null) {
            doUnavailableByNodeId(nodeId);
        }
    }

    List<URL> getCachedUrls(URL url) {
        Map<String, List<URL>> rsUrls = subscribedCategoryResponses.get(url);
        if (rsUrls == null || rsUrls.size() == 0) {
            return null;
        }

        List<URL> urls = new ArrayList<>();
        for (List<URL> us : rsUrls.values()) {
            for (URL tempUrl : us) {
                urls.add(tempUrl.createCopy());
            }
        }
        return urls;
    }

    protected List<URL> getMatchedDiscoverService(URL url, List<URL> urls){
        if(url == null || urls == null || urls.size() ==0) return null;
        String serviceId = url.getPath();
        String nodeType = url.getParameter(URLParam.nodeType.getName());
        String profile = url.getParameter(URLParam.environment.getName(), Ready.getBootstrapConfig().getActiveProfile());
        String project = url.getParameter(URLParam.project.getName());
        String projectVersion = url.getParameter(URLParam.projectVersion.getName());
        String serviceVersion = url.getParameter(URLParam.serviceVersion.getName());
        String group = url.getParameter(URLParam.group.getName(), URLParam.group.getValue());
        String protocol = url.getProtocol();
        if(logger.isTraceEnabled()) {
            logger.trace("nodeType = " + nodeType + ", protocol = " + protocol + ", project = " + project + "[" + projectVersion + "], serviceId = " + serviceId + "[" + serviceVersion + "], profile = " + profile + ", group = " + group);
        }
        List<URL> matchedServices = new LinkedList<>();
        for(URL service : urls){

            if((Constant.PROTOCOL_DEFAULT.equals(protocol) || protocol.equals(service.getProtocol()))
                    && (StrUtil.isBlank(project) || project.equals(service.getParameter(URLParam.project.getName())))
                    && (StrUtil.isBlank(projectVersion) || projectVersion.equals(service.getParameter(URLParam.projectVersion.getName())))
                    && (StrUtil.isBlank(serviceVersion) || serviceVersion.equals(service.getParameter(URLParam.serviceVersion.getName())))
                    && (StrUtil.isBlank(nodeType) || nodeType.equals(service.getParameter(URLParam.nodeType.getName())))
                    && group.equals(service.getParameter(URLParam.group.getName(), URLParam.group.getValue()))){
                matchedServices.add(service);
            }
        }
        return matchedServices;
    }

    protected void notify(URL refUrl, NotifyListener listener, List<URL> urls) {
        if (listener == null || urls == null) {
            return;
        }
        Map<String, List<URL>> serviceIdUrls = new HashMap<>();
        for (URL surl : urls) {
            String serviceId = surl.getPath();
            List<URL> serviceUrlList = serviceIdUrls.get(serviceId);
            if (serviceUrlList == null) {
                serviceIdUrls.put(serviceId, new ArrayList<>());
                serviceUrlList = serviceIdUrls.get(serviceId);
            }
            serviceUrlList.add(surl);
        }

        Map<String, List<URL>> curls = subscribedCategoryResponses.get(refUrl);
        if (curls == null) {
            subscribedCategoryResponses.putIfAbsent(refUrl, new ConcurrentHashMap<>());
            curls = subscribedCategoryResponses.get(refUrl);
        }

        for (String serviceId : serviceIdUrls.keySet()) {
            curls.put(serviceId, serviceIdUrls.get(serviceId));
        }

        for (List<URL> us : serviceIdUrls.values()) {
            logger.debug("notify " + listener.getName() + " that service " + refUrl.getPath() + " changed");
            listener.notify(getUrl(), us);
        }
    }

    protected abstract void doRegister(URL url);

    protected abstract void doUnregister(URL url);

    protected abstract void doSubscribe(URL url, NotifyListener listener);

    protected abstract void doUnsubscribe(URL url, NotifyListener listener);

    protected abstract List<URL> doDiscover(URL url);

    protected abstract void doAvailable(URL url);

    protected abstract void doUnavailable(URL url);

    protected abstract void doUnavailableByNodeId(String nodeId);

}
