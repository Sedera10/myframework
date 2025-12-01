package myframework.fw;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import myframework.annotation.MyController;
import myframework.annotation.MyMapping;
import myframework.annotation.RequestParam;
import myframework.annotation.GET;
import myframework.annotation.POST;
import myframework.fw.ModelView;
import myframework.utils.ClasseMethod;
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

    private final Map<String, ClasseMethod> staticRoutes = new HashMap<>();
    private final Map<String, ClasseMethod> dynamicRoutes = new HashMap<>();

    // premier scan au demarrage
    @Override
    public void init() throws ServletException {
        super.init();
        System.out.println(" Initialisation du FrontServlet...");
        try {
            String classesPath = getServletContext().getRealPath("/WEB-INF/classes");
            File classesDir = new File(classesPath);
            Set<Class<?>> controllers = Fonction.scanFromDirectory(
                    classesDir, "controller", MyController.class);

            for (Class<?> controller : controllers) {
                for (Method method : controller.getDeclaredMethods()) {
                    if (!method.isAnnotationPresent(MyMapping.class)) continue;

                    String pattern = method.getAnnotation(MyMapping.class).value();

                    String httpMethod = "GET"; // valeur par défaut
                    if (method.isAnnotationPresent(POST.class)) httpMethod = "POST";
                    if (method.isAnnotationPresent(GET.class))  httpMethod = "GET";

                    ClasseMethod cm = new ClasseMethod(controller, method, httpMethod);

                    // en cas de route dynamique
                    if (pattern.contains("{")) {
                        String regex = "^" + pattern.replaceAll("\\{[^/]+}", "([^/]+)") + "$";
                        dynamicRoutes.put(regex, cm);
                        System.out.println(" + Route dynamique : " + pattern + " → " + regex);
                    }
                    // en cas de route statique
                    else {
                        staticRoutes.put(pattern, cm);
                        System.out.println(" + Route statique : " + pattern);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        handleRequest(req, resp);
    }

    // Matcher les routes
    private void handleRequest(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String path = req.getRequestURI().substring(req.getContextPath().length());
        if (staticRoutes.containsKey(path)) {
            invoke(req, resp, staticRoutes.get(path), null, path);
            return;
        }

        for (String regex : dynamicRoutes.keySet()) {
            if (path.matches(regex)) {
                ClasseMethod cm = dynamicRoutes.get(regex);
                Method method = cm.getMethod();
                Map<String, String> params = new HashMap<>();
                String originalPattern = method.getAnnotation(MyMapping.class).value();
                String[] patternParts = originalPattern.split("/");
                String[] pathParts = path.split("/");
                for (int i = 0; i < patternParts.length; i++) {
                    if (patternParts[i].startsWith("{")) {
                        String key = patternParts[i].substring(1, patternParts[i].length() - 1);
                        params.put(key, pathParts[i]);
                    }
                }

                invoke(req, resp, cm, params, path);
                return;
            }
        }

        // rien trouvé
        resp.sendError(404, "Route introuvable : " + path);
    }

    // execution de la méthode
    private void invoke(HttpServletRequest req, HttpServletResponse resp,
                        ClasseMethod cm, Map<String, String> pathParams, String path)
            throws IOException, ServletException {

        String expected = cm.getHttpMethod();
        String received = req.getMethod();

        if (!expected.equalsIgnoreCase(received)) {
            resp.sendError(405,
                    "Méthode " + expected + " requise pour : " + path);
            return;
        }

        Method method = cm.getMethod();
        Class<?> controllerClass = cm.getClazz();
        try (PrintWriter out = resp.getWriter()) {
            Object controller = controllerClass.getDeclaredConstructor().newInstance();
            Object[] args = new Object[method.getParameterCount()];
            Class<?>[] types = method.getParameterTypes();
            java.lang.reflect.Parameter[] params = method.getParameters();

            for (int i = 0; i < params.length; i++) {
                if (params[i].isAnnotationPresent(RequestParam.class)) {
                    RequestParam ann = params[i].getAnnotation(RequestParam.class);
                    String name = ann.value();

                    // --- NOUVEAU : priorité au path si même nom ---
                    if (pathParams != null && pathParams.containsKey(name)) {
                        args[i] = convertValue(pathParams.get(name), types[i]);
                        continue;
                    }

                    // --- Sinon on prend dans les paramètres HTTP ---
                    String value = req.getParameter(name);

                    if (value == null && ann.required()) {
                        throw new RuntimeException("Missing required parameter: " + name);
                    }

                    args[i] = convertValue(value, types[i]);
                    continue;
                }

                if (pathParams != null) {
                    String paramName = params[i].getName(); 
                    if (pathParams.containsKey(paramName)) {
                        args[i] = convertValue(pathParams.get(paramName), types[i]);
                        continue;
                    }
                }

                if (types[i].isPrimitive()) {
                    throw new RuntimeException("Paramètre primitif non fourni : " + params[i].getName());
                }
                args[i] = null;
            }
            // appel methode
            Object result = method.invoke(controller, args);

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
                out.println("<h2>Route exécutée</h2>");
                out.println("<p>Classe : " + controllerClass.getName() + "</p>");
                out.println("<p>Méthode : " + method.getName() + "</p>");
                out.println("<p>URL : " + path + "</p>");
                out.println("</body></html>");
            }

        } catch (Exception e) {
            e.printStackTrace();
            resp.sendError(500, "Erreur interne : " + e.getMessage());
        }
    }

    // Transtypation
    private Object convertValue(String value, Class<?> type) {
        if (value == null) return null;

        if (type == int.class || type == Integer.class) return Integer.parseInt(value);
        if (type == long.class || type == Long.class) return Long.parseLong(value);
        if (type == double.class || type == Double.class) return Double.parseDouble(value);
        if (type == float.class || type == Float.class) return Float.parseFloat(value);

        return value;
    }
}
