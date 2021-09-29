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
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

import java.lang.reflect.Constructor;

public class ConstructorWeaver {

    private static final Log logger = LogFactory.getLog(ConstructorWeaver.class);

    @BindingPriority(9)
    @RuntimeType
    public static void intercept(@This Object proxy, @Origin Constructor constructor, @AllArguments Object[] args) {
        String key = MethodUtil.signature(constructor.getDeclaringClass(), constructor);
        String cn = constructor.getName();

        InterceptorChain<ConstructorInterceptor> cic = TransformerManager.CONSTRUCTOR.interceptorChainOf(key);
        if (cic == null) {
            
            logger.error("%s be intercepted but has no InterceptorChain.", cn);
            return;
        }
        
        for (ConstructorInterceptor ci : cic.getInterceptors()) {
            try {
                ci.onConstruct(proxy, constructor, args);
            } catch (Throwable t) {
                logger.error(t,"Invoke %s's onConstruct error.", ci.getClass().getName());
            }
        }
    }

}
