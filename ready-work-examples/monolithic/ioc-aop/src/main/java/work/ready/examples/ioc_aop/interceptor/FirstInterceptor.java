package work.ready.examples.ioc_aop.interceptor;

import work.ready.core.aop.Interceptor;
import work.ready.core.aop.Invocation;
import work.ready.core.service.result.Success;

public class FirstInterceptor implements Interceptor {

    @Override
    public void intercept(Invocation inv) throws Throwable {
        System.out.println("method before....."+inv.getMethodName());
        inv.invoke();
        inv.setReturnValue(Success.of("654321"));
        System.out.println("method after.....");
    }

}
