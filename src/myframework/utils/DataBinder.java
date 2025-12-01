package myframework.utils;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.*;
import java.time.LocalDate;
import java.util.*;

public class DataBinder {

    public static <T> T bindObject(Class<T> clazz, Map<String, String[]> params) {
        try {
            T instance = clazz.getDeclaredConstructor().newInstance();

            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                String name = field.getName();
                if (List.class.isAssignableFrom(field.getType())) {
                    ParameterizedType pt = (ParameterizedType) field.getGenericType();
                    Class<?> genericType = (Class<?>) pt.getActualTypeArguments()[0];
                    List<Object> list = bindList(name, genericType, params);
                    field.set(instance, list);
                    continue;
                }

                Map<String, String[]> subParams = extractSubParams(params, name + ".");
                if (!subParams.isEmpty()) {
                    Object nested = bindObject(field.getType(), stripPrefix(subParams, name + "."));
                    field.set(instance, nested);
                    continue;
                }

                if (params.containsKey(name)) {
                    Object converted = convertSimple(field.getType(), params.get(name));
                    field.set(instance, converted);
                    continue;
                }
            }

            return instance;

        } catch (Exception e) {
            throw new RuntimeException("Erreur DataBinder : " + e.getMessage(), e);
        }
    }

    public static <T> T bindFromRequest(Class<T> type, HttpServletRequest req) {
        return bindObject(type, req.getParameterMap());
    }


    private static List<Object> bindList(String prefix, Class<?> genericType, Map<String, String[]> params) {
        List<Object> list = new ArrayList<>();
        // Cas : list de strings ou primitives 
        if (params.containsKey(prefix)) {
            for (String v : params.get(prefix)) {
                list.add(convertSimple(genericType, new String[]{v}));
            }
            return list;
        }
        // Cas : list d’objets.
        int index = 0;
        while (true) {
            String base = prefix + "[" + index + "].";
            Map<String, String[]> subParams = extractSubParams(params, base);
            if (subParams.isEmpty()) break;

            Object obj = bindObject(genericType, stripPrefix(subParams, base));
            list.add(obj);
            index++;
        }

        return list;
    }

    private static Map<String, String[]> extractSubParams(Map<String, String[]> params, String prefix) {
        Map<String, String[]> map = new HashMap<>();
        for (String key : params.keySet()) {
            if (key.startsWith(prefix)) {
                map.put(key, params.get(key));
            }
        }
        return map;
    }

    private static Map<String, String[]> stripPrefix(Map<String, String[]> params, String prefix) {
        Map<String, String[]> map = new HashMap<>();
        for (String key : params.keySet()) {
            map.put(key.substring(prefix.length()), params.get(key));
        }
        return map;
    }

    // Convertion
    private static Object convertSimple(Class<?> type, String[] values) {
        String value = values[0];
        if (value == null) return null;

        if (type == int.class || type == Integer.class) return Integer.parseInt(value);
        if (type == long.class || type == Long.class) return Long.parseLong(value);
        if (type == double.class || type == Double.class) return Double.parseDouble(value);
        if (type == float.class || type == Float.class) return Float.parseFloat(value);
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(value);
        if (type == LocalDate.class) return LocalDate.parse(value);
        if (type == java.util.Date.class) return java.sql.Date.valueOf(value);

        return value;
    }

}
