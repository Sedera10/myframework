package myframework.utils;

import jakarta.servlet.ServletContext;
import java.io.InputStream;
import java.util.Properties;

/**
 * Classe utilitaire pour charger et accéder à la configuration de sécurité
 * Lit le fichier security.properties dans /WEB-INF/
 */
public class SecurityConfig {
    
    private static SecurityConfig instance;
    private Properties properties;
    private String authVariable;
    private String roleVariable;
    private String redirectUnauthorized;
    private String redirectForbidden;
    
    private SecurityConfig(ServletContext context) {
        properties = new Properties();
        loadConfig(context);
    }
    
    /**
     * Obtenir l'instance singleton de SecurityConfig
     */
    public static SecurityConfig getInstance(ServletContext context) {
        if (instance == null) {
            instance = new SecurityConfig(context);
        }
        return instance;
    }
    
    /**
     * Charge la configuration depuis security.properties
     */
    private void loadConfig(ServletContext context) {
        try {
            // Chercher le fichier dans /WEB-INF/security.properties
            InputStream is = context.getResourceAsStream("/WEB-INF/security.properties");
            
            if (is == null) {
                System.err.println("⚠️ Fichier security.properties introuvable dans /WEB-INF/");
                System.err.println("⚠️ Utilisation des valeurs par défaut : auth='user', role='role'");
                setDefaults();
                return;
            }
            
            properties.load(is);
            is.close();
            
            // Lire les propriétés
            authVariable = properties.getProperty("security.session.auth.variable", "user");
            roleVariable = properties.getProperty("security.session.role.variable", "role");
            redirectUnauthorized = properties.getProperty("security.redirect.unauthorized");
            redirectForbidden = properties.getProperty("security.redirect.forbidden");
            
            System.out.println("✅ Configuration de sécurité chargée :");
            System.out.println("   - Variable d'authentification : " + authVariable);
            System.out.println("   - Variable de rôle : " + roleVariable);
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement de security.properties : " + e.getMessage());
            setDefaults();
        }
    }
    
    /**
     * Définit les valeurs par défaut en cas d'erreur
     */
    private void setDefaults() {
        authVariable = "user";
        roleVariable = "role";
    }
    
    /**
     * Retourne le nom de la variable de session pour l'authentification
     * @return nom de la variable (ex: "user", "username", "currentUser")
     */
    public String getAuthVariable() {
        return authVariable;
    }
    
    /**
     * Retourne le nom de la variable de session pour le rôle
     * @return nom de la variable (ex: "role", "roles", "userRole")
     */
    public String getRoleVariable() {
        return roleVariable;
    }
    
    /**
     * Retourne l'URL de redirection en cas d'accès non autorisé (401)
     * @return URL ou null si non configuré
     */
    public String getRedirectUnauthorized() {
        return redirectUnauthorized;
    }
    
    /**
     * Retourne l'URL de redirection en cas d'accès interdit (403)
     * @return URL ou null si non configuré
     */
    public String getRedirectForbidden() {
        return redirectForbidden;
    }
}
