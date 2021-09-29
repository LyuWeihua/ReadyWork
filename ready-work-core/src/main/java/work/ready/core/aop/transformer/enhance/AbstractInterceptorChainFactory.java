/**
 *
 * Original work copyright dyagent
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
package work.ready.core.aop.transformer.enhance;

import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

import java.util.List;

public abstract class AbstractInterceptorChainFactory<ITRCPT extends Interceptor> {
    protected final Log logger = LogFactory.getLog(getClass());

    public void addInterceptors(List<Interceptor> itrcpts) {
        if (itrcpts == null || itrcpts.isEmpty()) {
            return;
        }
        for (Interceptor itrcpt : itrcpts) {
            if (itrcpt instanceof WitnessClassSupport) {
                if (((WitnessClassSupport) itrcpt).witness()) {
                    addInterceptors(itrcpt);
                    continue;
                }
                logger.warn("Interceptor (%s)'s witness class not exist.", itrcpt);
            } else {
                addInterceptors(itrcpt);
            }
        }
        logger.info("Current %s interceptors %s.", getClass().equals(ConstructorInterceptorChainFactory.class) ? "constructor" : "method", getInterceptors());
    }

    protected abstract void addInterceptors(Interceptor itrcpt);

    protected abstract List<ITRCPT> getInterceptors();

    protected abstract InterceptorChain<ITRCPT> interceptorChainOf(String key);

}
