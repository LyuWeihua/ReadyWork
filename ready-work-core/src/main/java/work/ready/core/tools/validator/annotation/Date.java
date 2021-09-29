package work.ready.core.tools.validator.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Documented
public @interface Date {
    String format();

    String message() default "";
}
