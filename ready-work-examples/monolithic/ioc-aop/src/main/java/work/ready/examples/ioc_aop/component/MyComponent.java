package work.ready.examples.ioc_aop.component;

import work.ready.core.aop.Interceptor;
import work.ready.core.aop.Invocation;

public class MyComponent implements Interceptor {

    @Override
    public void intercept(Invocation inv) throws Throwable {
        MyAop myAop = inv.getMethod().getAnnotation(MyAop.class);
        if(myAop == null) {
            myAop = inv.getMethod().getDeclaringClass().getAnnotation(MyAop.class);
        }
        String methodName = inv.getMethod().getName();
        if(myAop.enable()) {
            System.err.println("MyComponent is working before " + methodName + " method.");
        }
        inv.invoke();
        if(myAop.enable()) {
            System.err.println("MyComponent is working after " + methodName + " method.");
        }
    }

}
