package myframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour indiquer qu'une méthode est accessible sans authentification
 * 
 * Utilisation :
 * <pre>
 * {@literal @}Anonyme
 * {@literal @}MyMapping("/public")
 * public String publicPage() {
 *     return "Accessible à tous";
 * }
 * </pre>
 * 
 * Si une méthode est annotée @Anonyme, elle peut être exécutée sans vérification
 * de session ou de rôle.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Anonyme {
}
