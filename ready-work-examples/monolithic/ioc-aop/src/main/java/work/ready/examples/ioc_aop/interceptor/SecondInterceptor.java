package work.ready.examples.ioc_aop.interceptor;

import work.ready.core.aop.Interceptor;
import work.ready.core.aop.Invocation;
import work.ready.core.aop.annotation.GlobalInterceptor;

import java.util.List;

@GlobalInterceptor(match = "name.contains(\"service.Cat\") || name.contains(\"service.Dog\")")
public class SecondInterceptor implements Interceptor {

    @Override
    public void intercept(Invocation inv) throws Throwable {
        List<String> methods = List.of("walk", "eat");
        inv.invoke();
        if(methods.contains(inv.getMethodName())){
            inv.setReturnValue(((String)inv.getReturnValue()).replace(".", " happily."));
        }
    }

}
