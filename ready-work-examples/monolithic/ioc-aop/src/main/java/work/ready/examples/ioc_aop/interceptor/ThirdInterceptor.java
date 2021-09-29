package work.ready.examples.ioc_aop.interceptor;

import work.ready.core.aop.transformer.enhance.EasyInterceptor;

import java.lang.reflect.Method;

public class ThirdInterceptor implements EasyInterceptor {

    @Override
    public void before(Object instance, Method method, Object[] arguments, Class<?>[] parameterTypes) throws Throwable {
        System.err.println("ThirdInterceptor before! " + instance.getClass().getCanonicalName());
    }

    @Override
    public Object after(Object instance, Method method, Object[] arguments, Class<?>[] parameterTypes, Object result) throws Throwable {
        System.err.println("ThirdInterceptor end");
        System.err.println("ThirdInterceptor replaced '" + result + "' to 'The pig is awake. '");
        result = "The pig is awake. ";
        return result;
    }

    @Override
    public void handleError(Object instance, Method method, Object[] arguments, Class<?>[] parameterTypes, Throwable t) throws Throwable {
        System.err.println("ThirdInterceptor error");
    }
}
