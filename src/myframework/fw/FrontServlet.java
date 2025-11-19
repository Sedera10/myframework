package myframework.fw;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import myframework.annotation.MyController;
import myframework.annotation.MyMapping;
import myframework.annotation.RequestParam;
import myframework.fw.ModelView;
import myframework.utils.Fonction;

import jakarta.servlet.RequestDispatcher;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FrontServlet extends HttpServlet {

    // Routes statiques : /bonjour
    private final Map<String, Method> staticRoutes = new HashMap<>();
    private final Map<String, Class<?>> staticControllers = new HashMap<>();

    // Routes dynamiques : REGEX --> méthode
    private final Map<String, Method> dynamicRoutes = new HashMap<>();
    private final Map<String, Class<?>> dynamicControllers = new HashMap<>();

    @Override
    public void init() throws ServletException {
        super.init();
        System.out.println(" Initialisation du FrontServlet...");

        try {
            String classesPath = getServletContext().getRealPath("/WEB-INF/classes");
            File classesDir = new File(classesPath);

            Set<Class<?>> controllers = Fonction.scanFromDirectory(
                    classesDir, "controller", MyController.class);

            System.out.println("=== CONTROLLERS ===");
            for (Class<?> c : controllers) System.out.println(" -> " + c.getName());

            System.out.println("\n=== ROUTES ===");

            for (Class<?> controller : controllers) {

                for (Method method : controller.getDeclaredMethods()) {
                    if (!method.isAnnotationPresent(MyMapping.class)) continue;

                    MyMapping mapping = method.getAnnotation(MyMapping.class);

                    // ----- ROUTE STATIQUE -----
                    if (!mapping.url().isEmpty()) {
                        staticRoutes.put(mapping.url(), method);
                        staticControllers.put(mapping.url(), controller);
                        System.out.println(" + Route statique : " + mapping.url());
                    }

                    // ----- ROUTE DYNAMIQUE -----
                    if (!mapping.path().isEmpty()) {
                        String pattern = mapping.path(); // ex: /etudiant/{id}
                        String regex = "^" + pattern.replaceAll("\\{[^/]+}", "([^/]+)") + "$";

                        dynamicRoutes.put(regex, method);
                        dynamicControllers.put(regex, controller);

                        System.out.println(" + Route dynamique : " + pattern + " | REGEX=" + regex);
                    }
                }
            }

            System.out.println("\n✔ Initialisation terminée");

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

        // 1) Route statique ?
        if (staticRoutes.containsKey(path)) {
            invoke(req, resp, staticControllers.get(path), staticRoutes.get(path), null, path);
            return;
        }

        // 2) Route dynamique ?
        for (String regex : dynamicRoutes.keySet()) {
            if (path.matches(regex)) {

                Class<?> ctrlClass = dynamicControllers.get(regex);
                Method method = dynamicRoutes.get(regex);

                // Extraire les valeurs dynamiques
                String[] routeParts = regex.replace("^", "").replace("$", "").split("/");
                String[] pathParts = path.split("/");

                Map<String, String> params = new HashMap<>();

                String originalPattern = method.getAnnotation(MyMapping.class).path();
                String[] patternParts = originalPattern.split("/");

                for (int i = 0; i < patternParts.length; i++) {
                    if (patternParts[i].startsWith("{")) {
                        String key = patternParts[i].substring(1, patternParts[i].length() - 1);
                        params.put(key, pathParts[i]);
                    }
                }

                invoke(req, resp, ctrlClass, method, params, path);
                return;
            }
        }

        // 3) Route introuvable
        resp.sendError(404, "Route introuvable : " + path);
    }


    private void invoke(HttpServletRequest req, HttpServletResponse resp,
                        Class<?> controllerClass, Method method,
                        Map<String, String> pathParams, String path)
            throws IOException, ServletException {

        resp.setContentType("text/html;charset=UTF-8");

        try (PrintWriter out = resp.getWriter()) {

            Object controller = controllerClass.getDeclaredConstructor().newInstance();

            // --- Préparation des arguments ---
            Object[] args = new Object[method.getParameterCount()];
            Class<?>[] types = method.getParameterTypes();
            java.lang.reflect.Parameter[] params = method.getParameters();

            for (int i = 0; i < params.length; i++) {

                // 1) Vérifier si c'est un RequestParam
                if (params[i].isAnnotationPresent(RequestParam.class)) {

                    var ann = params[i].getAnnotation(RequestParam.class);
                    String name = ann.value();
                    String value = req.getParameter(name);

                    // obligatoire et manquant = erreur
                    if (value == null && ann.required()) {
                        throw new RuntimeException("Missing required parameter: " + name);
                    }

                    args[i] = convertValue(value, types[i]);
                    continue;
                }

                // 2) Sinon, essayer de remplir depuis les pathParams
                if (pathParams != null) {
                    String paramName = params[i].getName(); // nécessite compilation avec -parameters
                    if (pathParams.containsKey(paramName)) {
                        args[i] = convertValue(pathParams.get(paramName), types[i]);
                        continue;
                    }
                }

                // 3) Sinon laisser null si objet, sinon erreur pour type primitif
                if (types[i].isPrimitive()) {
                    throw new RuntimeException(
                            "Paramètre primitif non fourni : " + params[i].getName());
                }
                args[i] = null;
            }

            // --- Appel de la méthode ---
            Object result = method.invoke(controller, args);

            // --- Gestion des retours ---
            if (result instanceof String str) {
                out.println("<html><body>");
                out.println("<h2>Résultat</h2>");
                out.println("<p>" + str + "</p>");
                out.println("</body></html>");
            }
            else if (result instanceof ModelView mv) {
                for (Map.Entry<String, Object> e : mv.getData().entrySet()) {
                    req.setAttribute(e.getKey(), e.getValue());
                }
                RequestDispatcher rd = req.getRequestDispatcher("/" + mv.getView());
                rd.forward(req, resp);
            }
            else {
                out.println("<html><body>");
                out.println("<h2>✅ Route exécutée</h2>");
                out.println("<p><b>Classe :</b> " + controllerClass.getName() + "</p>");
                out.println("<p><b>Méthode :</b> " + method.getName() + "</p>");
                out.println("<p><b>URL :</b> " + path + "</p>");
                out.println("<p>(aucun contenu retourné)</p>");
                out.println("</body></html>");
            }

        } catch (Exception e) {
            e.printStackTrace();
            resp.sendError(500, "Erreur interne : " + e.getMessage());
        }
    }

    private Object convertValue(String value, Class<?> type) {
        if (value == null) return null;

        if (type == int.class || type == Integer.class) return Integer.parseInt(value);
        if (type == long.class || type == Long.class) return Long.parseLong(value);
        if (type == double.class || type == Double.class) return Double.parseDouble(value);
        if (type == float.class || type == Float.class) return Float.parseFloat(value);

        return value; // String par défaut
    }

}
