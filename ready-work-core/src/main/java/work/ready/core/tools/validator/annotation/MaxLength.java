package work.ready.core.tools.validator.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Documented
public @interface MaxLength {
    int value();

    String message() default "";
}
