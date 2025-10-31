package myframework.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

@Retention(RetentionPolicy.RUNTIME)  // visible à l'exécution
@Target(ElementType.TYPE)             // applicable sur les classes
public @interface MyController {
}
