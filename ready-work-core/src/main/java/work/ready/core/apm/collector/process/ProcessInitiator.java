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

package work.ready.core.apm.collector.process;

import work.ready.core.aop.transformer.TransformerManager;
import work.ready.core.aop.transformer.match.InterceptorPoint;
import work.ready.core.apm.ApmManager;
import work.ready.core.apm.collector.process.interceptor.ProcessInterceptor;

import java.util.Map;

public class ProcessInitiator {

    public ProcessInitiator() {
        Map<String, InterceptorPoint> collector = ApmManager.getConfig(ProcessConfig.class).getPoint();
        if(collector != null && !collector.isEmpty()) {
            for(Map.Entry<String, InterceptorPoint> entry : collector.entrySet()) {
                entry.getValue().setInterceptor(ProcessInterceptor.class.getCanonicalName());
            }
        }
        TransformerManager.getInstance().attach(collector);
    }
}
