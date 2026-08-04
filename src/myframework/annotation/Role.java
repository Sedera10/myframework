package myframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour restreindre l'accès à une méthode selon le rôle de l'utilisateur
 * 
 * Utilisation :
 * <pre>
 * {@literal @}Role("admin")
 * {@literal @}MyMapping("/admin/users")
 * public String manageUsers() {
 *     return "Gestion des utilisateurs";
 * }
 * 
 * // Plusieurs rôles autorisés
 * {@literal @}Role({"admin", "moderator"})
 * {@literal @}MyMapping("/content/edit")
 * public String editContent() {
 *     return "Édition de contenu";
 * }
 * </pre>
 * 
 * L'utilisateur doit être authentifié ET avoir au moins un des rôles spécifiés.
 * Le nom de la variable de session contenant le(s) rôle(s) est défini dans security.properties.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Role {
    /**
     * Liste des rôles autorisés à accéder à cette méthode
     * @return tableau de noms de rôles
     */
    String[] value();
}
