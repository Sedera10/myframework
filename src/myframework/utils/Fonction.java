package myframework.utils;

import java.lang.reflect.Method;
import java.io.File;
import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import myframework.annotation.MyMapping;
import java.net.URL;

@SuppressWarnings("unchecked")

public class Fonction {

    public static Map<String, String> getMappedMethods(Class<?> clazz, Class<? extends Annotation> mappingAnnotation) {
        Map<String, String> methodMappings = new HashMap<>();

        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(mappingAnnotation)) {
                Annotation annotation = method.getAnnotation(mappingAnnotation);
                try {
                    Method urlMethod = mappingAnnotation.getMethod("url");
                    Object urlValue = urlMethod.invoke(annotation);
                    methodMappings.put(method.getName(), String.valueOf(urlValue));
                } catch (Exception e) {
                    // Si l’annotation n’a pas de méthode "url", on l’ignore
                    methodMappings.put(method.getName(), "(aucune URL)");
                }
            }
        }

        return methodMappings;
    }

    // Méthode pour trouver toutes les classes annotées avec une annotation spécifique
    public static Set<Class<?>> findAnnotatedClasses(Class<? extends Annotation> annotation)
            throws IOException, ClassNotFoundException {
        Set<Class<?>> annotatedClasses = new HashSet<>();

        // 1️⃣ Récupérer le classpath complet
        String classpath = System.getProperty("java.class.path");
        String[] entries = classpath.split(File.pathSeparator);

        // 2️⃣ Parcourir chaque dossier du classpath
        for (String entry : entries) {
            File file = new File(entry);
            if (file.isDirectory()) {
                scanDirectory(file, file, annotation, annotatedClasses);
            }else if (file.getName().endsWith(".jar")) {
                scanJarFile(file, annotation, annotatedClasses);
            }
        }

        return annotatedClasses;
    }

    // En indiquant le dossier a scanner 
    public static Set<Class<?>> findAnnotatedClasses(String basePackage, Class<? extends Annotation> annotation)
        throws IOException, ClassNotFoundException {

        Set<Class<?>> annotatedClasses = new HashSet<>();
        String path = basePackage.replace('.', '/');

        System.out.println("Recherche du package : " + basePackage);
        System.out.println("Chemin converti : " + path);

        Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(path);

        if (!resources.hasMoreElements()) {

            System.err.println("⚠️ Aucun dossier trouvé pour le package : " + basePackage);
            return annotatedClasses;
        }

        // 3️⃣ Parcourir tous les emplacements trouvés
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            System.out.println("Trouvé : " + resource.getFile());
            File file = new File(resource.getFile());

            if (!file.exists()) {
                System.err.println("⚠️ Le chemin " + file + " n'existe pas.");
                continue;
            }

            if (file.isDirectory()) {
                scanDirectory(file, file, annotation, annotatedClasses);
            } else if (file.getName().endsWith(".jar")) {
                scanJarFile(file, annotation, annotatedClasses);
            }
        }

        return annotatedClasses;
    }


    // Méthode récursive pour scanner un répertoire
    private static void scanDirectory(File root, File current,
                                      Class<? extends Annotation> annotation,
                                      Set<Class<?>> annotatedClasses) throws ClassNotFoundException {
        for (File file : Objects.requireNonNull(current.listFiles())) {
            if (file.isDirectory()) {
                scanDirectory(root, file, annotation, annotatedClasses);
            } else if (file.getName().endsWith(".class")) {
                String path = file.getAbsolutePath()
                        .substring(root.getAbsolutePath().length() + 1)
                        .replace(File.separatorChar, '.')
                        .replace(".class", "");

                try {
                    Class<?> clazz = Class.forName(path);
                    if (clazz.isAnnotationPresent(annotation)) {
                        annotatedClasses.add(clazz);
                    }
                } catch (Exception e) {
                    // Certaines classes peuvent ne pas se charger
                    System.err.println("❌ Erreur chargement " + path + " → " + e);
                }
            }
        }
    }

    private static void scanJarFile(File jarFile, Class<? extends Annotation> annotation, Set<Class<?>> annotatedClasses) {
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                // On ne garde que les fichiers .class
                if (entry.getName().endsWith(".class")) {
                    String className = entry.getName()
                            .replace('/', '.')
                            .replace(".class", "");

                    try {
                        Class<?> clazz = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
                        if (clazz.isAnnotationPresent(annotation)) {
                            annotatedClasses.add(clazz);
                        }
                    } catch (Throwable t) {
                        // On ignore les classes qui ne peuvent pas être chargées
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // public static Class<?> findClassByName(String className) {
    //     try {
    //         // Si le nom contient déjà le package complet (ex: controller.Classe1Controller)
    //         return Class.forName(className);
    //     } catch (ClassNotFoundException e) {
    //         // Sinon, on cherche manuellement dans tout le classpath
    //         String classpath = System.getProperty("java.class.path");
    //         String[] entries = classpath.split(java.io.File.pathSeparator);

    //         for (String entry : entries) {
    //             java.io.File root = new java.io.File(entry);
    //             if (root.isDirectory()) {
    //                 Class<?> found = searchInDirectory(root, root, className);
    //                 if (found != null) return found;
    //             }
    //         }
    //         System.out.println("⚠️ Classe introuvable : " + className);
    //         return null;
    //     }
    // }

    // private static Class<?> searchInDirectory(java.io.File root, java.io.File current, String className) {
    //     for (java.io.File file : java.util.Objects.requireNonNull(current.listFiles())) {
    //         if (file.isDirectory()) {
    //             Class<?> found = searchInDirectory(root, file, className);
    //             if (found != null) return found;
    //         } else if (file.getName().endsWith(".class")) {
    //             String path = file.getAbsolutePath()
    //                     .substring(root.getAbsolutePath().length() + 1)
    //                     .replace(java.io.File.separatorChar, '.')
    //                     .replace(".class", "");

    //             // On ne compare que le nom simple de la classe
    //             String simpleName = path.substring(path.lastIndexOf('.') + 1);
    //             if (simpleName.equals(className)) {
    //                 try {
    //                     return Class.forName(path);
    //                 } catch (Throwable ignored) {}
    //             }
    //         }
    //     }
    //     return null;
    // }
}