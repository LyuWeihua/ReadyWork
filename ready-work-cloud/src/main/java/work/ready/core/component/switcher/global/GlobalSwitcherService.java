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

package work.ready.core.component.switcher.global;

import org.apache.ignite.IgniteMessaging;
import org.apache.ignite.lang.IgniteBiPredicate;
import work.ready.cloud.cluster.Cloud;
import work.ready.core.component.switcher.LocalSwitcherService;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.module.ShutdownHook;
import work.ready.core.server.Ready;
import work.ready.core.tools.ReadyThreadFactory;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GlobalSwitcherService extends LocalSwitcherService {

    private static final Log logger = LogFactory.getLog(GlobalSwitcherService.class);
    private final ExecutorService pool = Executors.newSingleThreadExecutor(new ReadyThreadFactory("GlobalSwitcher"));
    private final Cloud cloud;

    public GlobalSwitcherService(Cloud cloud) {
        this.cloud = cloud;
        Ready.shutdownHook.add(ShutdownHook.STAGE_7, (inMs)->pool.shutdown());
        if(cloud != null) switcherChangeEventListener();
    }

    @Override
    public void setGlobalValue(String switcherName, boolean value) {
        setValue(switcherName, value);
        if (cloud != null) {
            pool.submit(()->{
                
                IgniteMessaging rmtMsg = Cloud.message(Cloud.cluster().forRemotes());
                
                rmtMsg.send("SwitcherChangeEvent", switcherName + '|' + value);
            });
        }
    }

    private void switcherChangeEventListener() {
        
        IgniteBiPredicate<UUID, String> messageListener = new IgniteBiPredicate<>() {
            @Override
            public boolean apply(UUID nodeId, String msg) {
                if(logger.isDebugEnabled()) {
                    logger.debug("node received SwitcherChangeEvent message [msg=" + msg + ", from remote=" + nodeId + "]");
                }
                int pos = msg.indexOf('|');
                setValue(msg.substring(0, pos), Boolean.parseBoolean(msg.substring(pos)));
                return true; 
            }
        };
        IgniteMessaging rmtMsg = Cloud.message();
        rmtMsg.localListen("SwitcherChangeEvent", messageListener);
    }

}
