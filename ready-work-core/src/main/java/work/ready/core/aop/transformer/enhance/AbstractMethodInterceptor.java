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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public abstract class AbstractMethodInterceptor extends WitnessClassSupport implements MethodInterceptor {

    protected final Log logger = LogFactory.getLog(getClass());

    @Override
    public void intercept(ExecuteContext context) throws Throwable {
        MethodProceeding proceeding = context.getMethodProceeding();

        Method method = proceeding.getMethod();
        Class<?> clazz = method.getDeclaringClass();
        Object instance = proceeding.getInstance();
        Class<?>[] argTypes = method.getParameterTypes();
        Object[] args = proceeding.getArgs();

        try {
            before(instance, method, args, argTypes);
        } catch (Throwable t) {
            logger.error(t,"class[%s] before method[%s] intercept failed.", clazz, method.getName());
        }

        try {
            
            Object rtVal = around(instance, method, args, argTypes, ()->{
                context.proceed();
                return context.getMethodProceeding().getRtVal();
            });
            if (rtVal != null) {
                proceeding.setRtVal(rtVal);
            }
        } catch (Throwable t) {
            try {
                handleError(instance, method, args, argTypes, t);
            } catch (EasyInterceptor.IgnoreException e) {

            } catch (Throwable t2) {
                if(!t2.equals(t)) {
                    logger.error(t2, "class[%s] handle error[%s] exception failed.", clazz, method.getName());
                }
                
                if (t instanceof InvocationTargetException) {
                    throw ((InvocationTargetException) t).getTargetException();
                }
                throw t;
            }
        } finally {
            try {
                Object rtVal = after(instance, method, args, argTypes, proceeding.getRtVal());
                if (rtVal != null) {
                    proceeding.setRtVal(rtVal);
                }
            } catch (Throwable t) {
                logger.error(t,"class[%s] after method[%s] intercept failed.", clazz, method.getName());
            }
        }
    }

    protected void before(Object instance, Method method, Object[] arguments, Class<?>[] parameterTypes) throws Throwable
    {}

    protected Object around(Object instance, Method method, Object[] arguments, Class<?>[] parameterTypes, ProceedingJoinPoint point) throws Throwable
    {
        return point.proceed();
    }

    protected Object after(Object instance, Method method, Object[] arguments, Class<?>[] parameterTypes, Object result) throws Throwable
    {
        return result;
    }

    protected void handleError(Object instance, Method method, Object[] arguments, Class<?>[] parameterTypes, Throwable t) throws Throwable
    {}
}
