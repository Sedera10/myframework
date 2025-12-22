package myframework.fw;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import myframework.annotation.MyController;
import myframework.annotation.MyMapping;
import myframework.annotation.RequestParam;
import myframework.annotation.GET;
import myframework.annotation.POST;
import myframework.annotation.Json;
import myframework.fw.ModelView;
import myframework.fw.FileUpload;
import myframework.utils.ClasseMethod;
import myframework.utils.Fonction;
import myframework.utils.DataBinder;
import myframework.utils.JsonResponse;


import jakarta.servlet.RequestDispatcher;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Collection;

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
        Object result = null;
        Exception capturedError = null;
        PrintWriter out = resp.getWriter();
        try{
            Object controller = controllerClass.getDeclaredConstructor().newInstance();
            Object[] args = new Object[method.getParameterCount()];
            Class<?>[] types = method.getParameterTypes();
            java.lang.reflect.Parameter[] params = method.getParameters();
            // Traitement des paramètres
            for (int i = 0; i < params.length; i++) {
                // Injection de HttpServletRequest si demandé
                if (types[i] == HttpServletRequest.class) {
                    args[i] = req;
                    continue;
                }
                
                // Injection de HttpServletResponse si demandé
                if (types[i] == HttpServletResponse.class) {
                    args[i] = resp;
                    continue;
                }
                
                if (Map.class.isAssignableFrom(types[i])) {
                    Object mapValue = processMapParameter(req, types[i], params[i]);
                    args[i] = mapValue;
                    continue;
                }

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
                if (!types[i].isPrimitive() && !types[i].isEnum() && !types[i].getName().startsWith("java.") && !Map.class.isAssignableFrom(types[i])) {
                    args[i] = DataBinder.bindFromRequest(types[i], req);
                    continue;
                }

                args[i] = null;
            }
            // appel methode
            try {
                result = method.invoke(controller, args);
            } catch (Exception e) {
                capturedError = e;
            }
            // Gestion JSON
            if (method.isAnnotationPresent(Json.class)) {
                JsonResponse json = handleJsonResponse(result, capturedError);
                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                com.google.gson.Gson gson = new com.google.gson.Gson();
                out.print(gson.toJson(json));
                return;
            }
            if (capturedError != null) throw capturedError;
            // Comportement normal
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
        if (type == java.util.Date.class) return java.sql.Date.valueOf(value);
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(value);

        return value;
    }

    // Sprint 8 + Sprint Upload
    private Object processMapParameter(HttpServletRequest req,
                                   Class<?> type,
                                   java.lang.reflect.Parameter param) {

        String genericType = param.getParameterizedType().getTypeName();
        if (!genericType.equals("java.util.Map<java.lang.String, java.lang.Object>")) {
            if (type.isPrimitive()) {
                throw new RuntimeException("Impossible d'injecter un Map non supporté dans un type primitif.");
            }
            return null;
        }
        
        Map<String, Object> formMap = new HashMap<>();
        
        // Vérifier si c'est une requête multipart (upload de fichiers)
        String contentType = req.getContentType();
        if (contentType != null && contentType.toLowerCase().startsWith("multipart/form-data")) {
            try {
                Collection<Part> parts = req.getParts();
                for (Part part : parts) {
                    String fieldName = part.getName();
                    
                    // Si c'est un fichier
                    if (part.getSubmittedFileName() != null && !part.getSubmittedFileName().isEmpty()) {
                        InputStream inputStream = part.getInputStream();
                        byte[] fileBytes = inputStream.readAllBytes();
                        inputStream.close();
                        
                        FileUpload fileUpload = new FileUpload(
                            fieldName,
                            part.getSubmittedFileName(),
                            part.getContentType(),
                            fileBytes
                        );
                        
                        // Injection automatique du ServletContext
                        fileUpload.setServletContext(req.getServletContext());
                        
                        formMap.put(fieldName, fileUpload);
                    } 
                    // Si c'est un champ texte normal
                    else {
                        InputStream inputStream = part.getInputStream();
                        String value = new String(inputStream.readAllBytes());
                        inputStream.close();
                        formMap.put(fieldName, value);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Erreur lors du traitement multipart : " + e.getMessage(), e);
            }
        } 
        // Sinon, traitement normal (formulaire classique)
        else {
            Map<String, String[]> parameterMap = req.getParameterMap();
            for (String key : parameterMap.keySet()) {
                String[] values = parameterMap.get(key);
                if (values.length == 1) {
                    formMap.put(key, values[0]); 
                } else {
                    formMap.put(key, List.of(values)); // checkbox ou valeur multiple
                }
            }
        }

        return formMap;
    }

    // Sprint 8 bis 
    private Object bindFromRequest(Class<?> clazz, HttpServletRequest req) throws Exception {
        Object obj = clazz.getDeclaredConstructor().newInstance();
        Field[] fields = clazz.getDeclaredFields();
        for (Field f : fields) {
            String name = f.getName();
            String raw = req.getParameter(name);
            if (raw == null) continue;

            f.setAccessible(true);

            Object value = convertValue(raw, f.getType());
            f.set(obj, value);
        }
        return obj;
    }

    // Sprint 9
    private JsonResponse handleJsonResponse(Object data, Exception error) {
        JsonResponse response = new JsonResponse();

        if (data != null && data instanceof ModelView mv) {
            data = mv.getData();
        }

        if (error == null) {
            response.setStatus("success");
            response.setCode(200);
            response.setData(data);
            // si data est une liste -> count
            if (data instanceof java.util.List<?> list) {
                response.setCount(list.size());
            }
        } else {
            response.setStatus("error");
            response.setCode(500);
            response.setData(error.getMessage());
            response.setCount(null);
        }
        return response;
    }


}
