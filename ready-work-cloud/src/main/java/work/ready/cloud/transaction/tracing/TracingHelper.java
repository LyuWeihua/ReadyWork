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
import work.ready.core.tools.HttpUtil;
import work.ready.core.tools.StrUtil;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

public class TracingHelper {
    private static final Log logger = LogFactory.getLog(TracingHelper.class);

    public static final String GROUP_ID = "groupId";

    public static final String APP_MAP = "appMap";

    public static final String HEADER_KEY_GROUP_ID = "DTX-Group-ID";

    public static final String HEADER_KEY_APP_MAP = "DTX-App-Map";

    private TracingHelper() {
    }

    public static void transmit(TracingSetter tracingSetter) {
        if (TracingContext.tracing().hasGroup()) {
            logger.debug("tracing transmit group: %s", TracingContext.tracing().groupId());
            tracingSetter.set(TracingHelper.HEADER_KEY_GROUP_ID, TracingContext.tracing().groupId());
            tracingSetter.set(TracingHelper.HEADER_KEY_APP_MAP,
                    HttpUtil.base64URLSafe(TracingContext.tracing().appMapString().getBytes(StandardCharsets.UTF_8)));
        }
    }

    public static void apply(TracingGetter tracingGetter) {
        String groupId = Optional.ofNullable(tracingGetter.get(TracingHelper.HEADER_KEY_GROUP_ID)).orElse("");
        String appList = Optional.ofNullable(tracingGetter.get(TracingHelper.HEADER_KEY_APP_MAP)).orElse("");
        TracingContext.init(Map.of(TracingHelper.GROUP_ID, groupId, TracingHelper.APP_MAP,
                StrUtil.isEmpty(appList) ? appList : new String(HttpUtil.decodeBase64URLSafe(appList), StandardCharsets.UTF_8)));
        if (TracingContext.tracing().hasGroup()) {
            logger.debug("tracing apply group: %s, app map: %s", groupId, appList);
        }
    }

    @FunctionalInterface
    public interface TracingSetter {
        
        void set(String key, String value);
    }

    @FunctionalInterface
    public interface TracingGetter {
        
        String get(String key);
    }
}
