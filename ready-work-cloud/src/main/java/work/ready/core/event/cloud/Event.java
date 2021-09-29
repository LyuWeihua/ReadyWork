/**
 * Copyright (c) 2020 WeiHua Lyu [ready.work]
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package work.ready.core.event.cloud;

public class Event extends work.ready.core.event.Event {

    public static final String READY_WORK_CLOUD_BEFORE_INIT = "READY_WORK_CLOUD_BEFORE_INIT";

    public static final String READY_WORK_CLOUD_AFTER_INIT = "READY_WORK_CLOUD_AFTER_INIT";

    public static final String CONFIG_LOADED_FROM_CONFIG_SERVER = "CONFIG_LOADED_FROM_CONFIG_SERVER";

    public static final String SERVICE_DISCOVER_REGISTRY_BEFORE_INIT = "SERVICE_DISCOVER_REGISTRY_BEFORE_INIT";

    public static final String SERVICE_DISCOVER_REGISTRY_AFTER_INIT = "SERVICE_DISCOVER_REGISTRY_AFTER_INIT";

    public static final String CONFIG_SERVER_MODULE_BEFORE_INIT = "CONFIG_SERVER_MODULE_BEFORE_INIT";

    public static final String CONFIG_SERVER_MODULE_SERVICE_INIT = "CONFIG_SERVER_MODULE_SERVICE_INIT";

    public static final String CONFIG_SERVER_MODULE_AFTER_INIT = "CONFIG_SERVER_MODULE_AFTER_INIT";

    public static final String CONFIG_SERVER_MODULE_REGISTERED = "CONFIG_SERVER_MODULE_REGISTERED";

    public static final String NODE_UNAVAILABLE = "NODE_UNAVAILABLE";

    public static final String MESSAGE_FROM_OLDEST = "MESSAGE_FROM_OLDEST";

    public static final String REGISTRY_AVAILABLE_SERVICE_CHANGED = "REGISTRY_AVAILABLE_SERVICE_CHANGED";

    public static final String SERVICE_HOST_UNREACHABLE = "SERVICE_HOST_UNREACHABLE";

    public static final String SERVICE_UNSTABLE = "SERVICE_UNSTABLE";

    public static final String SERVICE_STABILITY_CHANGED = "SERVICE_STABILITY_CHANGED";

}
