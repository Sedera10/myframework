package myframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour indiquer qu'une méthode nécessite une authentification
 * 
 * Utilisation :
 * <pre>
 * {@literal @}Authorized
 * {@literal @}MyMapping("/dashboard")
 * public String dashboard() {
 *     return "Page protégée";
 * }
 * </pre>
 * 
 * Si une méthode est annotée @Authorized, elle ne peut être exécutée que si
 * l'utilisateur est authentifié (la variable de session définie dans security.properties existe).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Authorized {
}
