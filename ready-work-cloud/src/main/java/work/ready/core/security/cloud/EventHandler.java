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

package work.ready.core.security.cloud;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteMessaging;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.events.CacheEvent;
import org.apache.ignite.lang.IgniteBiPredicate;
import org.apache.ignite.lang.IgnitePredicate;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;
import work.ready.cloud.ReadyCloud;
import work.ready.cloud.cluster.Cloud;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.event.cloud.Event;
import work.ready.core.event.GeneralEvent;
import work.ready.core.security.LimiterConfig;
import work.ready.core.security.SecurityManager;
import work.ready.core.security.access.limiter.storage.cloud.IgniteStorage;
import work.ready.core.server.Ready;

import java.util.UUID;

public class EventHandler {

    private static final Log logger = LogFactory.getLog(EventHandler.class);

    private static final String limitConfigCache = "ready.work:security:LimiterConfigCache";

    public void listen(Cloud cloud){
        Ready.eventManager().addListener(this, "securityManagerCreateListener",
                (setter -> setter.addName(Event.SECURITY_MANAGER_CREATE)));
    }

    public void localLimiterConfigChangeListener(GeneralEvent event) {
        if(event.get(LimiterConfig.externalType) != null) return;  
        LimiterConfig config = event.getObject();
        sendLimiterConfigToRemote(config, false);
    }

    private void sendLimiterConfigToRemote(LimiterConfig config, boolean force) {
        IgniteCache<String, String> cache = Cloud.cache(limitConfigCache);

        String cfgMessage = force ? config.toMessage(true) : config.toMessage();
        cache.put("config", cfgMessage);

        ClusterGroup group = Cloud.cluster().forRemotes();
        if(logger.isDebugEnabled())
            logger.debug("LimitConfigCache updated and try to send to remote nodes=%s force=%s config=%s", group.nodes(), force, config.toMessage());
        if(Ready.isStarted() && Ready.isMultiAppMode())
        Ready.getApp((app)->app.stream()
                .filter(a->!config.equals(a.security().getConfig().getLimiter()))
                .map(a->LimiterConfig.fromMessage(cfgMessage, a.security().getConfig().getLimiter())));
        if(!group.nodes().isEmpty()) {
            IgniteMessaging rmtMsg = Cloud.message(group);
            
            rmtMsg.send("LimiterConfigSync", cfgMessage);
        }
    }

    public void securityManagerCreateListener(GeneralEvent event) {
        if(ReadyCloud.isReady()){
            SecurityManager manager = event.getSender();
            manager.getConfig().getLimiter().setMode(LimiterConfig.MODE_CLUSTER);
            manager.addStorageType(LimiterConfig.TYPE_IGNITE, IgniteStorage.class);

            Ready.eventManager().addListener(this, "localLimiterConfigChangeListener",
                    (setter -> setter.addName(Event.LIMITER_CONFIG_CHANGED).setFilter((evt)->
                            ((GeneralEvent)evt).get(LimiterConfig.externalType) == null
                    ).setContextReference(manager.getConfig().getLimiter())));

            IgnitePredicate<CacheEvent> eventListener = new IgnitePredicate<CacheEvent>() {
                @Override
                public boolean apply(CacheEvent evt) {
                    if(logger.isTraceEnabled()) {
                        logger.trace("Received cache event [cacheName=" + evt.cacheName() + ", evt=" + evt.name() + ", key=" + evt.key() +
                                ", oldVal=" + evt.oldValue() + ", newVal=" + evt.newValue());
                    }
                    if(limitConfigCache.equals(evt.cacheName())) {
                        String msg = (String) evt.newValue();
                        if(Integer.parseInt(msg.substring(0, msg.indexOf('`'))) > manager.getConfig().getLimiter().getVersion()) {
                            LimiterConfig.fromMessage(msg, manager.getConfig().getLimiter());
                            if(logger.isDebugEnabled()) {
                                logger.debug("Local LimiterConfig updated to: " + msg);
                            }
                        }
                    }
                    return true; 
                }
            };

            CacheConfiguration<String, String> cacheConfig = new CacheConfiguration<>();
            cacheConfig.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
            cacheConfig.setCacheMode(CacheMode.REPLICATED);
            cacheConfig.setDataRegionName(Cloud.WITHOUT_PERSISTENCE);
            cacheConfig.setName(limitConfigCache);
            IgniteCache<String, String> cache = Cloud.getOrCreateCache(cacheConfig);

            try (Transaction tx = Ignition.ignite().transactions().txStart(TransactionConcurrency.OPTIMISTIC, TransactionIsolation.SERIALIZABLE)) {
                String config = cache.get("config");
                if (config == null || Integer.parseInt(config.substring(0, config.indexOf('`'))) < manager.getConfig().getLimiter().getVersion()) {
                    cache.put("config", manager.getConfig().getLimiter().toMessage());
                    tx.commit();
                } else {
                    LimiterConfig.fromMessage(config, manager.getConfig().getLimiter());
                    if(logger.isTraceEnabled()) {
                        logger.trace("updated LimiterConfig from LimitConfigCache: %s", config);
                    }
                }
            }

            configEventListener(manager.getConfig().getLimiter()); 
            manager.setConfigSyncer((config)->sendLimiterConfigToRemote(config, true)); 
        }
    }

    private void configEventListener(LimiterConfig config) {
        
        IgniteBiPredicate<UUID, String> messageListener = new IgniteBiPredicate<>() {
            @Override
            public boolean apply(UUID nodeId, String msg) {
                if(logger.isTraceEnabled()) {
                    logger.trace("node received LimiterConfigSync message [msg=" + msg + ", from remote=" + nodeId + "]");
                }
                if(Integer.parseInt(msg.substring(0, msg.indexOf('`'))) > config.getVersion()) {
                    LimiterConfig.fromMessage(msg, config);
                    if(logger.isDebugEnabled()) {
                        logger.debug("Local LimiterConfig updated to: " + msg);
                    }
                }
                return true; 
            }
        };
        IgniteMessaging rmtMsg = Cloud.message();
        rmtMsg.localListen("LimiterConfigSync", messageListener);
    }

}
