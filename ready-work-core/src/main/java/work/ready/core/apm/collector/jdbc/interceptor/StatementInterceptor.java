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

package work.ready.core.apm.collector.jdbc.interceptor;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import work.ready.core.aop.transformer.enhance.AbstractMethodInterceptor;
import work.ready.core.aop.transformer.match.ClassMatch;
import work.ready.core.aop.transformer.match.MultiNameOrMatch;
import work.ready.core.apm.ApmManager;
import work.ready.core.apm.collector.jdbc.JdbcConfig;

import java.lang.reflect.Method;

public class StatementInterceptor extends AbstractMethodInterceptor {

    private final StatementHandler handler = new StatementHandler();

    @Override
    protected void before(Object instance, Method method, Object[] arguments, Class<?>[] parameterTypes) throws Throwable {
        handler.before(instance.getClass().getCanonicalName(), method.getName(), arguments, null);
    }

    @Override
    protected Object after(Object instance, Method method, Object[] arguments, Class<?>[] parameterTypes, Object result) throws Throwable {
        return handler.after(instance.getClass().getCanonicalName(), method.getName(), arguments, result, null, null);
    }

    @Override
    protected void handleError(Object instance, Method method, Object[] arguments, Class<?>[] parameterTypes, Throwable t) throws Throwable {
        handler.after(instance.getClass().getCanonicalName(), method.getName(), arguments, null, t, null);
    }

    @Override
    public ClassMatch focusOn() {

        return new MultiNameOrMatch(ApmManager.getConfig(JdbcConfig.class).getPreparedStatement().toArray(new String[]{}));

    }

    @Override
    public boolean matches(TypeDescription typeDescription, MethodDescription.InDefinedShape methodDescription, ParameterList<ParameterDescription.InDefinedShape> parameterDescriptions) {
        String name = methodDescription.getActualName();
        boolean forExecute = methodDescription.isPublic() && name.startsWith("execute");
        if(forExecute) return true;
        return name.startsWith("set")
                && methodDescription.isPublic()
                && parameterDescriptions.size() > 0
                && parameterDescriptions.get(0).getType().getTypeName().equals("int");
    }
}
