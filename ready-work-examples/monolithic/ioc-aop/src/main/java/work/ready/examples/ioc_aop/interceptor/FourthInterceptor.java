package work.ready.examples.ioc_aop.interceptor;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatchers;
import work.ready.examples.ioc_aop.service.Animal;
import work.ready.core.aop.transformer.enhance.AbstractMethodInterceptor;
import work.ready.core.aop.transformer.match.ClassMatch;
import work.ready.core.aop.transformer.match.SubTypeMatch;

import java.lang.reflect.Method;

public class FourthInterceptor extends AbstractMethodInterceptor {

    @Override
    protected void before(Object instance, Method method, Object[] arguments, Class<?>[] parameterTypes) throws Throwable {
        System.err.println("FourthInterceptor before! " + instance.getClass().getCanonicalName());
    }

    @Override
    protected Object after(Object instance, Method method, Object[] arguments, Class<?>[] parameterTypes, Object result) throws Throwable {
        System.err.println("ThirdInterceptor end");
        result = ((String)result).replace(" is "," is not ");
        return result;
    }

    @Override
    protected void handleError(Object instance, Method method, Object[] arguments, Class<?>[] parameterTypes, Throwable t) throws Throwable {
        System.err.println("FourthInterceptor error");
    }

    @Override
    public ClassMatch focusOn() {
        return new SubTypeMatch(Animal.class)
                .and(ElementMatchers.not(ElementMatchers.isInterface()))
                .and(ElementMatchers.not(ElementMatchers.<TypeDescription>isAbstract()))
                .and(ElementMatchers.not(ElementMatchers.nameContains("$")));
    }

    @Override
    public boolean matches(TypeDescription typeDescription, MethodDescription.InDefinedShape methodDescription, ParameterList<ParameterDescription.InDefinedShape> parameterDescriptions) {
        String name = methodDescription.getActualName();
        return methodDescription.isPublic() && (name.equals("sleep"));
    }
}
