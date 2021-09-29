/**
 *
 * Original work Copyright 2017-2019 CodingApi
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
package work.ready.cloud.transaction.tracing;

import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;
import work.ready.core.tools.CollectionUtil;
import work.ready.core.tools.StrUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TracingContext {
    private static final Log logger = LogFactory.getLog(TracingContext.class);
    private static final ThreadLocal<TracingContext> tracingContextThreadLocal = new ThreadLocal<>();

    private TracingContext() {

    }

    public static TracingContext tracing() {
        if (tracingContextThreadLocal.get() == null) {
            tracingContextThreadLocal.set(new TracingContext());
        }
        return tracingContextThreadLocal.get();
    }

    private Map<String, String> fields;

    public void beginTransactionGroup() {
        if (hasGroup()) {
            return;
        }
        init(Map.of(TracingHelper.GROUP_ID, String.valueOf(Ready.getId()), TracingHelper.APP_MAP, ""));
    }

    public static void init(Map<String, String> initFields) {
        if (initFields == null) {
            logger.warn("init tracingContext fail, null fields.");
            return;
        }
        TracingContext tracingContext = tracing();
        if (tracingContext.fields == null) {
            tracingContext.fields = new HashMap<>();
        }
        tracingContext.fields.putAll(initFields);
    }

    public boolean hasGroup() {
        return fields != null && fields.containsKey(TracingHelper.GROUP_ID) &&
                StrUtil.notBlank(fields.get(TracingHelper.GROUP_ID));
    }

    public String groupId() {
        if (hasGroup()) {
            return fields.get(TracingHelper.GROUP_ID);
        }
        raiseNonGroupException();
        return "";
    }

    public Map<String, String> fields() {
        return this.fields;
    }

    public void addApp(String serviceId, String address) {
        if (hasGroup()) {
            String[] appMap = StrUtil.split(this.fields.get(TracingHelper.APP_MAP), ',');
            if(appMap.length % 2 == 1) {
                appMap = new String[]{serviceId, address};
            } else {
                for(int i = 0; i < appMap.length; i++) {
                    if(i % 2 == 0 && appMap[i].equals(serviceId)) {
                        return;
                    }
                }
                CollectionUtil.appendArray(appMap, serviceId, address);
            }
            this.fields.put(TracingHelper.APP_MAP, StrUtil.join(appMap, ","));
            return;
        }
        raiseNonGroupException();
    }

    public String[] appMap() {
        return StrUtil.split(this.fields.get(TracingHelper.APP_MAP), ',');
    }

    public String appMapString() {
        if (hasGroup()) {
            String appMap = Optional.ofNullable(this.fields.get(TracingHelper.APP_MAP)).orElse("");
            logger.debug("App map: %s", appMap);
            return appMap;
        }
        raiseNonGroupException();
        return "";
    }

    public void destroy() {
        if (tracingContextThreadLocal.get() != null) {
            tracingContextThreadLocal.remove();
        }
    }

    private void raiseNonGroupException() {
        throw new IllegalStateException("no group id.");
    }
}
