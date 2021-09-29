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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InterceptorChain<T extends Interceptor> {

    private final List<T> interceptors = new CopyOnWriteArrayList<>();

    public void addInterceptor(T interceptor) {
        interceptors.add(interceptor);
        interceptors.sort(Comparator.comparingInt(Interceptor::order));
    }

    public List<T> getInterceptors() {
        return Collections.unmodifiableList(interceptors);
    }

}
