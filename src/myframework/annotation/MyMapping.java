package myframework.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;


@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyMapping {
    String url() default "";  // ex: /bonjour
    String path() default ""; // ex: /etudiant/{id}
}
