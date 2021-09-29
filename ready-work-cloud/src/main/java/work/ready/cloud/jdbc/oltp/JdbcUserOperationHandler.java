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

package work.ready.cloud.jdbc.oltp;

import work.ready.core.aop.transformer.enhance.EasyInterceptor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class JdbcUserOperationHandler implements EasyInterceptor {

    private boolean handle = false;

    public void setHandle(boolean handle) {
        this.handle = handle;
    }

    @Override
    public void before(Object instance, Method method, Object[] arguments, Class<?>[] parameterTypes) throws Throwable {
    }

    @Override
    public Object after(Object instance, Method method, Object[] arguments, Class<?>[] parameterTypes, Object result) throws Throwable {
        return result;
    }

    @Override
    public void handleError(Object instance, Method method, Object[] arguments, Class<?>[] parameterTypes, Throwable t) throws Throwable {
        if (handle && t instanceof InvocationTargetException) {
            if(((InvocationTargetException) t).getTargetException().getMessage().startsWith("User management operations initiated on behalf of")) {
                throw new IgnoreException();
            }
        }
    }
}
