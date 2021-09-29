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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ConstructorInterceptorChainFactory extends AbstractInterceptorChainFactory<ConstructorInterceptor> {

    private final ConcurrentMap<String, InterceptorChain<ConstructorInterceptor>> interceptorChains = new ConcurrentHashMap<>();
    private final List<ConstructorInterceptor> interceptors = new ArrayList<>(8);

    @Override
    protected void addInterceptors(Interceptor itrcpt) {
        if (itrcpt instanceof ConstructorInterceptor) {
            this.interceptors.add((ConstructorInterceptor) itrcpt);
        }
    }

    @Override
    protected List<ConstructorInterceptor> getInterceptors() {
        return Collections.unmodifiableList(this.interceptors);
    }

    @Override
    protected InterceptorChain<ConstructorInterceptor> interceptorChainOf(String key) {
        InterceptorChain<ConstructorInterceptor> itrcptChain = this.interceptorChains.get(key);
        if (itrcptChain == null) {
            itrcptChain = new InterceptorChain<>();
            this.interceptorChains.put(key, itrcptChain);
        }
        return itrcptChain;
    }
}
