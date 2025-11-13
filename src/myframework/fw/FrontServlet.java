package myframework.fw;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import myframework.utils.Fonction;
import myframework.annotation.MyController;
import myframework.annotation.MyMapping;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.ServletException;
import myframework.fw.ModelView;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FrontServlet extends HttpServlet {

    private final Map<String, Method> routeMappings = new HashMap<>();
    private final Map<String, Class<?>> controllerMappings = new HashMap<>();

    @Override
    public void init() throws ServletException {
        super.init();
        System.out.println(" Initialisation du framework via FrontServlet...");

        try {
            // Trouver le dossier réel des classes webapp
            String classesPath = getServletContext().getRealPath("/WEB-INF/classes");
            File classesDir = new File(classesPath);
            System.out.println("Dossier des classes = " + classesDir.getAbsolutePath());

            if (!classesDir.exists()) {
                System.err.println("Dossier WEB-INF/classes introuvable !");
                return;
            }

            // Scanner toutes les classes à partir de ce dossier
            Set<Class<?>> controllers = Fonction.scanFromDirectory(
                classesDir,
                "controller",
                MyController.class
            );

            System.out.println("=== Classes annotées avec @MyController ===");
            for (Class<?> c : controllers) {
                System.out.println(" -> " + c.getName());
            }

            // Construire les routes à partir des @MyMapping
            System.out.println("\n=== Méthodes annotées avec @MyMapping ===");
            for (Class<?> controllerClass : controllers) {
                Map<String, String> mappedMethods = Fonction.getMappedMethods(controllerClass, MyMapping.class);

                if (!mappedMethods.isEmpty()) {
                    System.out.println("\n▶ " + controllerClass.getSimpleName());
                    for (Map.Entry<String, String> entry : mappedMethods.entrySet()) {
                        String methodName = entry.getKey();
                        String url = entry.getValue();

                        try {
                            Method method = controllerClass.getDeclaredMethod(methodName);
                            routeMappings.put(url, method);
                            controllerMappings.put(url, controllerClass);
                            System.out.println( methodName + " | URL = " + url);
                        } catch (NoSuchMethodException e) {
                            System.err.println("Méthode introuvable : " + methodName + " dans " + controllerClass.getName());
                        }
                    }
                }
            }

            System.out.println("\n✅ Scan terminé. " + routeMappings.size() + " routes détectées.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        handleRequest(req, resp);
    }

    private void handleRequest(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String path = req.getRequestURI().substring(req.getContextPath().length());

        if (routeMappings.containsKey(path)) {
            Method method = routeMappings.get(path);
            Class<?> controllerClass = controllerMappings.get(path);

            resp.setContentType("text/html;charset=UTF-8");
            try (PrintWriter out = resp.getWriter()) {
                Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
                Object result = method.invoke(controllerInstance);

                if (result instanceof String) {
                    out.println("<html><body>");
                    out.println("<h2>✅ Méthode exécutée</h2>");
                    out.println("<p>" + result + "</p>");
                    out.println("</body></html>");
                } 
                else if (result instanceof ModelView mv) {
                    System.out.println("🧭 Redirection vers la page : " + mv.getView());
                    // Injecter les attributs dans la requête
                    RequestDispatcher dispatcher = req.getRequestDispatcher("/" + mv.getView());
                    dispatcher.forward(req, resp);
                } else {
                    // Sinon, on affiche les infos de debug
                    out.println("<html><body>");
                    out.println("<h2>✅ Route trouvée et exécutée (dans log)</h2>");
                    out.println("<p><b>Classe :</b> " + controllerClass.getName() + "</p>");
                    out.println("<p><b>Méthode :</b> " + method.getName() + "</p>");
                    out.println("<p><b>URL :</b> " + path + "</p>");
                    out.println("<p>(aucun contenu retourné)</p>");
                    out.println("</body></html>");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        // Si route inconnue → 404
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        resp.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            out.println("<h3>FrontServlet</h3>");
            out.println("<p>URL demandée : " + path + "</p>");
            out.println("<p style='color:red'>⚠️ Route non trouvée.</p>");
        }
    }
}
