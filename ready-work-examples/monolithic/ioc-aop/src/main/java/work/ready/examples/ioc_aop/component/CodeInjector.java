package work.ready.examples.ioc_aop.component;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface CodeInjector {
    boolean enable() default true;
    boolean replace() default false;
}
