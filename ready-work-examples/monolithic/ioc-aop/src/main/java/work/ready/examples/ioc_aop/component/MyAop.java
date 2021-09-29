package work.ready.examples.ioc_aop.component;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface MyAop {
    boolean enable() default true;
}
