/**
 *
 * Copyright (c) 2020 WeiHua Lyu [ready.work]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http:www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package work.ready.core.event.cloud;

import org.apache.ignite.IgniteMessaging;
import org.apache.ignite.lang.IgniteBiPredicate;
import work.ready.cloud.ReadyCloud;
import work.ready.cloud.cluster.Cloud;
import work.ready.cloud.cluster.CloudThreadFactory;
import work.ready.core.event.cloud.Event;
import work.ready.core.event.EventManager;
import work.ready.core.event.GeneralEvent;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.module.ShutdownHook;
import work.ready.core.server.Ready;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EventHandler {

    private static final Log logger = LogFactory.getLog(EventHandler.class);
    public static final String GeneralEventChannel = "GeneralEventChannel";

    public void listen(Cloud cloud){
        generalEventListener(cloud); 
        Ready.eventManager().addListener(this, "eventManagerInitListener",
                (setter -> setter.addName(Event.EVENT_MANAGER_AFTER_INIT))); 
    }

    public void eventManagerInitListener(GeneralEvent event) {
        if(ReadyCloud.isReady()){
            EventManager eventManager = event.getObject();
            ExecutorService pool = Executors.newCachedThreadPool(new CloudThreadFactory());
            
            Ready.shutdownHook.add(ShutdownHook.STAGE_6, (inMs)->pool.shutdown());
            eventManager.setEventFilter(0, evt -> {

                if(!evt.isInternal() || !evt.isGlobal()) {
                    return;
                }
                pool.submit(()->{
                    
                    IgniteMessaging rmtMsg = Cloud.message(Cloud.cluster().forRemotes());
                    
                    rmtMsg.send(GeneralEventChannel, evt.toMessage());
                });
            });
        }
    }

    private void generalEventListener(Cloud cloud) {
        
        IgniteBiPredicate<UUID, String> messageListener = new IgniteBiPredicate<>() {
            @Override
            public boolean apply(UUID nodeId, String msg) {
                if(logger.isDebugEnabled()) {
                    logger.debug("node received GeneralEvent message [msg=" + msg + ", from remote=" + nodeId + "]");
                }
                GeneralEvent event = GeneralEvent.fromMessage(msg);
                event.setInternal(false); 
                Ready.eventManager().post(event);
                return true; 
            }
        };
        IgniteMessaging rmtMsg = Cloud.message();
        rmtMsg.localListen(GeneralEventChannel, messageListener);
    }

}
