// package myframework.fw;

// import jakarta.servlet.ServletContextEvent;
// import jakarta.servlet.ServletContextListener;
// import jakarta.servlet.annotation.WebListener;
// import java.io.File;
// import java.util.*;
// import myframework.annotation.*;
// import myframework.utils.Fonction;

// @WebListener
// public class AppInitializer implements ServletContextListener {

//     @Override
//     public void contextInitialized(ServletContextEvent sce) {
//         System.out.println(" (old)Initialisation du framework...");

//         try {
//             // Récupération du vrai chemin des classes du projet web
//             String classesPath = sce.getServletContext().getRealPath("/WEB-INF/classes");
//             File classesDir = new File(classesPath);
//             System.out.println(" Dossier des classes : " + classesDir.getAbsolutePath());

//             if (!classesDir.exists()) {
//                 System.err.println(" Dossier WEB-INF/classes introuvable !");
//                 return;
//             }

//             //  Scanner récursivement les classes à partir de WEB-INF/classes
//             Set<Class<?>> controllers = Fonction.scanFromDirectory(
//                 classesDir,
//                 "controller",
//                 MyController.class
//             );

//             //  Affichage des résultats
//             System.out.println("=== Classes annotées avec @MyController ===");
//             for (Class<?> c : controllers) {
//                 System.out.println(" -> " + c.getName());
//             }

//             System.out.println("\n=== Méthodes annotées avec @MyMapping ===");
//             for (Class<?> controllerClass : controllers) {
//                 Map<String, String> methods = Fonction.getMappedMethods(controllerClass, MyMapping.class);
//                 if (!methods.isEmpty()) {
//                     System.out.println("▶ " + controllerClass.getSimpleName());
//                     for (Map.Entry<String, String> entry : methods.entrySet()) {
//                         System.out.println("   Méthode : " + entry.getKey() + " | URL = " + entry.getValue());
//                     }
//                 }
//             }

//         } catch (Exception e) {
//             e.printStackTrace();
//         }
//     }

//     @Override
//     public void contextDestroyed(ServletContextEvent sce) {
//         System.out.println(" Framework arrêté.");
//     }
// }
