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

package work.ready.core.handler.session.cloud;

import work.ready.cloud.ReadyCloud;
import work.ready.core.event.cloud.Event;
import work.ready.core.event.GeneralEvent;
import work.ready.core.handler.RequestHandler;
import work.ready.core.handler.session.SessionCookieConfig;
import work.ready.core.server.ApplicationConfig;
import work.ready.core.server.Ready;

public class EventHandler {

    public void listen(){
        Ready.eventManager().addListener(this, "sessionManagerListener",
                (setter -> setter.addName(Event.SESSION_MANAGER_BEFORE_INIT)));
    }

    public void sessionManagerListener(GeneralEvent event) {
        if(ReadyCloud.isReady()){
            RequestHandler handler = event.getSender();
            ApplicationConfig config = event.getObject();
            IgniteSessionRepository sessionRepository = new IgniteSessionRepository();
            sessionRepository.setDefaultMaxInactiveInterval(config.getSessionMaxInactiveInterval());
            handler.setSessionManager(new IgniteSessionManager(
                    Ready.beanManager().get(SessionCookieConfig.class),
                    sessionRepository));
        }
    }

}
