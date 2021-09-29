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

import work.ready.core.aop.transformer.TransformerManager;
import work.ready.core.aop.transformer.utils.MethodUtil;
import net.bytebuddy.implementation.bind.annotation.*;

import java.lang.reflect.Method;

public class MethodInterceptorDispatcher {

    @BindingPriority(9)
    @RuntimeType
    public static Object intercept(@This Object proxy, @SuperMethod Method originAccessor,
            @Origin Method originMethod, @AllArguments Object[] args) throws Throwable {

        String key = MethodUtil.signature(originMethod.getDeclaringClass(), originMethod);
        String mn = originMethod.getName();

        InterceptorChain<MethodInterceptor> mic = TransformerManager.METHOD.interceptorChainOf(key);
        if (mic == null) {
            
        }

        ExecuteContext executeContext = new ExecuteContext(mic,
                new MethodProceeding(proxy, originAccessor, originMethod, args));
        executeContext.proceed();
        return executeContext.getMethodProceeding().getRtVal();
    }

    @RuntimeType
    public static Object intercept(@SuperMethod Method originAccessor, @Origin Method originMethod,
            @AllArguments Object[] args) throws Throwable {
        
        return intercept(null, originAccessor, originMethod, args);
    }
}
