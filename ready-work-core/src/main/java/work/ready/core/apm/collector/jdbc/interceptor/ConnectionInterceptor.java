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
import work.ready.core.apm.collector.jdbc.JdbcContext;
import work.ready.core.apm.common.SamplingUtil;
import work.ready.core.apm.common.SpanManager;
import work.ready.core.apm.common.TraceContext;
import work.ready.core.apm.model.Span;
import work.ready.core.apm.model.SpanType;
import work.ready.core.apm.collector.jdbc.JdbcConfig;
import work.ready.core.database.jdbc.hikari.pool.HikariProxyConnection;

import java.lang.reflect.Method;

public class ConnectionInterceptor extends AbstractMethodInterceptor {

    @Override
    protected void before(Object instance, Method method, Object[] arguments, Class<?>[] parameterTypes) throws Throwable {
        
        if(!ApmManager.getConfig(JdbcConfig.class).isEnabled() || !JdbcContext.isOn() || SamplingUtil.NO()){
            return;
        }
        Span span = JdbcContext.getJdbcSpan();
        String gid = TraceContext.getCorrelationId();
        if(span == null || !gid.equals(span.getCorrelationId())) {
            span = SpanManager.createLocalSpan(SpanType.SQL);
            JdbcContext.setJdbcSpan(span);
            if(instance instanceof HikariProxyConnection) {
                span.addTag("source",
                        ((HikariProxyConnection)instance).getPoolName());
            }
            span.addTag("sql", arguments[0]);
        }
    }

    @Override
    protected Object after(Object instance, Method method, Object[] arguments, Class<?>[] parameterTypes, Object result) throws Throwable {
        
        return result;
    }

    @Override
    protected void handleError(Object instance, Method method, Object[] arguments, Class<?>[] parameterTypes, Throwable t) throws Throwable {
        
        JdbcContext.remove();
    }

    @Override
    public ClassMatch focusOn() {

        return new MultiNameOrMatch(ApmManager.getConfig(JdbcConfig.class).getJdbcConnection().toArray(new String[]{}));

    }

    @Override
    public boolean matches(TypeDescription typeDescription, MethodDescription.InDefinedShape methodDescription, ParameterList<ParameterDescription.InDefinedShape> parameterDescriptions) {
        String name = methodDescription.getActualName();
        return methodDescription.isPublic() && (name.equals("prepareStatement") || name.equals("prepareCall"));
    }

}
