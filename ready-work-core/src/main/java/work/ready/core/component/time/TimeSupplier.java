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

package work.ready.core.component.time;

import work.ready.core.event.Event;
import work.ready.core.event.GeneralEvent;
import work.ready.core.server.Ready;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class TimeSupplier {

    private static final List<TimeAware> listeners = new ArrayList<>();
    private static Supplier<Long> timeSupplier;

    public static void addListener(TimeAware listener) {
        listeners.add(listener);
        if(timeSupplier != null) {
            listener.setTimeSupplier(timeSupplier);
        }
    }

    public void start() {
        Ready.eventManager().addListener(this, "timeInitListener",
                (setter -> setter.addName(Event.READY_WORK_TIME_INIT)));
    }

    public void timeInitListener(GeneralEvent event){
        timeSupplier = event.getObject();
        listeners.forEach(listener->listener.setTimeSupplier(timeSupplier));
    }
}
