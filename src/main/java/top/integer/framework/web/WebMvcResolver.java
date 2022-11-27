package top.integer.framework.web;

import com.google.gson.Gson;
import jakarta.servlet.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.integer.framework.core.ioc.AnnotationContext;
import top.integer.framework.core.ioc.Pair;
import top.integer.framework.core.ioc.annotation.Controller;
import top.integer.framework.core.ioc.factory.BeanDefinition;
import top.integer.framework.web.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class WebMvcResolver {
    private Map<String, List<PathMapping>> normalPathMapping = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(WebMvcResolver.class);
    private BeanDefinition beanDefinition;
    public static final Gson gson = new Gson();


    public WebMvcResolver() {

    }

    public void scan() {
        List<Class<?>> classesByPackageNameAndAnnotation = beanDefinition.getClassesByPackageNameAndAnnotation(Controller.class);
        for (Class<?> aClass : classesByPackageNameAndAnnotation) {
            String basePath = scanClassRequestPath(aClass);
            List<Method> requestMethods = Arrays.stream(aClass.getMethods())
                    .filter(it -> it.isAnnotationPresent(Request.class)
                            || it.isAnnotationPresent(Get.class)
                            || it.isAnnotationPresent(Post.class)
                            || it.isAnnotationPresent(Put.class)
                            || it.isAnnotationPresent(Delete.class))
                    .collect(Collectors.toList());
            processingRequestMethod(basePath, requestMethods);
        }
    }

    public PathMapping getPath(String path, String method) {
        if ("GET".equals(method)) {
            return getPath(path, RequestType.GET);
        } else if ("POST".equals(method)) {
            return getPath(path, RequestType.POST);
        } else if ("PUT".equals(method)) {
            return getPath(path, RequestType.PUT);
        } else if ("DELETE".equals(method)) {
            return getPath(path, RequestType.DELETE);
        }
        return getPath(path, RequestType.REQUEST);
    }


    public PathMapping getPath(String path, RequestType requestType) {
        PathMapping pathMapping = null;

//        优先匹配普通路径
        List<PathMapping> pathMappings = normalPathMapping.get(path);
        if (pathMappings != null) {
            pathMapping = getNormalPathMapping(pathMappings, requestType);
        }
//        当普通路径匹配不到时，匹配带占位符的路径
        if (pathMapping == null) {
            pathMapping = getPathWithPathVariable(path, requestType);
        }
        return pathMapping;
    }

    //   匹配带占位符的路径
    private PathMapping getPathWithPathVariable(String path, RequestType requestType) {
        String[] pathSplit = path.split("/");
        List<Map.Entry<String, List<PathMapping>>> paths = normalPathMapping.entrySet().stream()
                .filter(it -> it.getKey().contains("{") && it.getKey().split("/").length == pathSplit.length)
                .toList();
        PathMapping pathMapping = null;
        for (Map.Entry<String, List<PathMapping>> it : paths) {
            String key = it.getKey();
            String[] split = key.split("/");
            boolean flag = true;
//            查找是否有对应的路径
            for (int i = 0; i < split.length; i++) {
                if (!split[i].contains("{") && !split[i].equals(pathSplit[i])) {
                    flag = false;
                    break;
                }
            }
            if (flag) {
                PathMapping requestMapping = null;
                for (PathMapping mapping : it.getValue()) {
                    if (mapping.getRequestType() == requestType) {
                        pathMapping = mapping;
                        break;
                    }
                    // 如果没有找到对应的请求类型，就使用Request进行配对
                    if (mapping.getRequestType() == RequestType.REQUEST) {
                        requestMapping = mapping;
                    }
                }
                if (pathMapping == null && requestMapping != null) {
                    pathMapping = requestMapping;
                }
                break;
            }
        }
        if (pathMapping == null) {
            log.debug("path {} not found, method = {}", path, requestType);
            return null;
        }
        pathMapping.setPlaceHolder(true);
        return pathMapping;
    }

    /**
     * 填充request response cookie session
     *
     * @param parameter
     * @param request
     * @param response
     * @return
     */
    private Object fillInNativeParameters(Parameter parameter, HttpServletRequest request, HttpServletResponse response) {
        Object o = null;
        if (parameter.getType() == HttpServletRequest.class) {
            o = request;
        } else if (parameter.getType() == HttpServletResponse.class) {
            o = response;
        } else if (parameter.getType() == Cookie.class) {
            o = request.getCookies();
        } else if (parameter.getType() == HttpSession.class) {
            o = request.getSession();
        }
        return o;
    }

    private Object fillResponseBody(HttpServletRequest request, Parameter parameter) {
        Object o = null;
        if (parameter.isAnnotationPresent(RequestBody.class)) {
            String contentType = request.getContentType();
            if (contentType != null && contentType.contains("application/json")) {
                try {
                    o = gson.fromJson(request.getReader(), parameter.getType());
                } catch (Exception e) {
                    log.error("json to object error", e);
                }
            }
        }
        return o;
    }

    private PathMapping getNormalPathMapping(List<PathMapping> pathMappings, RequestType requestType) {
        PathMapping requestMapping = pathMappings.stream()
                .filter(it -> it.getRequestType() == RequestType.REQUEST)
                .findFirst()
                .orElse(null);
        return pathMappings.stream()
                .filter(it -> it.getRequestType() == requestType)
                .findFirst()
                .orElse(requestMapping);
    }

    public void printPathHandler() {
        for (Map.Entry<String, List<PathMapping>> stringListEntry : normalPathMapping.entrySet()) {
//            log.info("path: {}", stringListEntry.getKey());
            for (PathMapping pathMapping : stringListEntry.getValue()) {
                System.out.printf("path: %s, requestType: %s, method: %s\n", pathMapping.getPath(), pathMapping.getRequestType(), pathMapping.getHandlerMethod());
            }
        }
    }

    public void invoke(AnnotationContext context, HttpServletResponse response, Method method, List<Object> args) {
        Class<?> clazz = method.getDeclaringClass();
        Object o = context.getBean(clazz);
        Class<?> returnType = method.getReturnType();
        try {
            Object result = method.invoke(o, args.toArray());
            PrintWriter writer = response.getWriter();
            System.out.println("clazz = " + clazz);
            if (returnType == Integer.class || returnType == int.class
                    || returnType == Double.class || returnType == double.class
                    || returnType == Float.class || returnType == float.class
                    || returnType == Long.class || returnType == long.class
                    || returnType == Boolean.class || returnType == boolean.class
                    || returnType == short.class || returnType == Short.class
                    || returnType == Character.class || returnType == char.class) {
                writer.write(result.toString());
            } else if (returnType == String.class) {
                String s = (String) result;
                if (s.startsWith("redirect:")) {
                    response.sendRedirect(s.substring(9));
                } else {
                    writer.write(s);
                }
            } else {
                writer.write(gson.toJson(result));
            }
            writer.flush();
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("invoke method error", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 处理请求参数
     *
     * @param request
     * @param response
     * @param method
     * @return
     */
    public List<Object> processRequestParameter(HttpServletRequest request, HttpServletResponse response, Method method, Map<String, String> pathValueMapping) {
        List<Object> fill = new ArrayList<>();
        Parameter[] parameters = method.getParameters();
        Map<String, String> cookie = Collections.emptyMap();
        if (request.getCookies() != null && request.getCookies().length != 0) {
            cookie = Arrays.stream(request.getCookies())
                    .collect(Collectors.toMap(Cookie::getName, Cookie::getValue));
        }
        if (parameters.length == 0) {
            return Collections.emptyList();
        }
//        if (request.)
        for (Parameter parameter : parameters) {
            if (parameter.isAnnotationPresent(RequestParameter.class)) {
                String name = getValue(parameter.getAnnotation(RequestParameter.class), parameter.getName());
                fill.add(fillRequestParameters(request, name, parameter));
            } else if (parameter.isAnnotationPresent(CookieValue.class)) {
                String name = getValue(parameter.getAnnotation(CookieValue.class), parameter.getName());
                fill.add(fillCookie(cookie, name, parameter));
            } else if (parameter.isAnnotationPresent(HeaderValue.class)) {
                String name = getValue(parameter.getAnnotation(HeaderValue.class), parameter.getName());
                fill.add(request.getHeader(name));
                System.out.println("name = " + name);
            } else if (parameter.isAnnotationPresent(RequestBody.class)) {
                fill.add(fillResponseBody(request, parameter));
            } else if (parameter.isAnnotationPresent(PathVariable.class)) {
                String name = getValue(parameter.getAnnotation(PathVariable.class), parameter.getName());
                fill.add(fillPathVariable(name, parameter, pathValueMapping));
            } else {
                Object o = fillInNativeParameters(parameter, request, response);
                if (o != null) {
                    fill.add(o);
                }
            }
        }
        return fill;
    }

    /**
     * 填充请求的参数
     *
     * @param request
     * @param name
     * @param parameter
     * @return
     */
    public Object fillRequestParameters(HttpServletRequest request, String name, Parameter parameter) {
        Class<?> type = parameter.getType();
        Object o = fillBasicDataType(request, name, parameter);
        if (o == null) {
            o = fillList(request, name, parameter);
        }
        return o;
    }

    /**
     * 填充基本类型
     *
     * @param request
     * @param name
     * @param parameter
     * @return
     */
    public Object fillBasicDataType(HttpServletRequest request, String name, Parameter parameter) {
        Class<?> type = parameter.getType();
        if (type == String.class) {
            return request.getParameter(name);
        } else if (type == int.class || type == Integer.class) {
            return Integer.parseInt(request.getParameter(name));
        } else if (type == long.class || type == Long.class) {
            return Long.parseLong(request.getParameter(name));
        } else if (type == double.class || type == Double.class) {
            return Double.parseDouble(request.getParameter(name));
        } else if (type == float.class || type == Float.class) {
            return Float.parseFloat(request.getParameter(name));
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(request.getParameter(name));
        } else if (type == byte.class || type == Byte.class) {
            return Byte.parseByte(request.getParameter(name));
        } else if (type == short.class || type == Short.class) {
            return Short.parseShort(request.getParameter(name));
        } else if (type == char.class || type == Character.class) {
            return request.getParameter(name).charAt(0);
        }
        return null;
    }

    public Object fillCookie(Map<String, String> cookie, String name, Parameter parameter) {
        Class<?> type = parameter.getType();
        if (type == String.class) {
            return cookie.get(name);
        } else if (type == int.class || type == Integer.class) {
            return Integer.parseInt(cookie.get(name));
        } else if (type == long.class || type == Long.class) {
            return Long.parseLong(cookie.get(name));
        } else if (type == double.class || type == Double.class) {
            return Double.parseDouble(cookie.get(name));
        } else if (type == float.class || type == Float.class) {
            return Float.parseFloat(cookie.get(name));
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(cookie.get(name));
        } else if (type == byte.class || type == Byte.class) {
            return Byte.parseByte(cookie.get(name));
        } else if (type == short.class || type == Short.class) {
            return Short.parseShort(cookie.get(name));
        } else if (type == char.class || type == Character.class) {
            return cookie.get(name).charAt(0);
        }
        return null;
    }

    private Object fillPathVariable(String name, Parameter parameter, Map<String, String> pathValueMapping) {
        Class<?> type = parameter.getType();
        if (type == String.class) {
            return pathValueMapping.get(name);
        } else if (type == int.class || type == Integer.class) {
            return Integer.parseInt(pathValueMapping.get(name));
        } else if (type == long.class || type == Long.class) {
            return Long.parseLong(pathValueMapping.get(name));
        } else if (type == double.class || type == Double.class) {
            return Double.parseDouble(pathValueMapping.get(name));
        } else if (type == float.class || type == Float.class) {
            return Float.parseFloat(pathValueMapping.get(name));
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(pathValueMapping.get(name));
        } else if (type == byte.class || type == Byte.class) {
            return Byte.parseByte(pathValueMapping.get(name));
        } else if (type == short.class || type == Short.class) {
            return Short.parseShort(pathValueMapping.get(name));
        } else if (type == char.class || type == Character.class) {
            return pathValueMapping.get(name).charAt(0);
        }
        return null;
    }


    public List fillList(HttpServletRequest request, String name, Parameter parameter) {
        Type parameterizedType = parameter.getParameterizedType();
        String typeName = parameterizedType.getTypeName();
        String genericParadigm = typeName.substring(typeName.indexOf("<") + 1, typeName.indexOf(">"));
        System.out.println("genericParadigm = " + genericParadigm);
        Class<?> aClass = null;
        try {
            aClass = Class.forName(genericParadigm);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        String[] parameterValues = request.getParameterValues(name);
        if (parameterValues == null) {
            return Collections.emptyList();
        }
        List list = new ArrayList();
        for (String parameterValue : parameterValues) {
            if (aClass == String.class) {
                list.add(parameterValue);
            } else if (aClass == int.class || aClass == Integer.class) {
                list.add(Integer.parseInt(parameterValue));
            } else if (aClass == long.class || aClass == Long.class) {
                list.add(Long.parseLong(parameterValue));
            } else if (aClass == double.class || aClass == Double.class) {
                list.add(Double.parseDouble(parameterValue));
            } else if (aClass == float.class || aClass == Float.class) {
                list.add(Float.parseFloat(parameterValue));
            } else if (aClass == boolean.class || aClass == Boolean.class) {
                list.add(Boolean.parseBoolean(parameterValue));
            } else if (aClass == byte.class || aClass == Byte.class) {
                list.add(Byte.parseByte(parameterValue));
            } else if (aClass == short.class || aClass == Short.class) {
                list.add(Short.parseShort(parameterValue));
            } else if (aClass == char.class || aClass == Character.class) {
                list.add(parameterValue.charAt(0));
            }
        }
        return list;
    }

    private String getValue(Annotation annotation, String defaultValue) {
        String value = null;
        if (annotation instanceof Request) {
            value = ((Request) annotation).value();
        } else if (annotation instanceof Get) {
            value = ((Get) annotation).value();
        } else if (annotation instanceof Post) {
            value = ((Post) annotation).value();
        } else if (annotation instanceof Put) {
            value = ((Put) annotation).value();
        } else if (annotation instanceof Delete) {
            value = ((Delete) annotation).value();
        } else if (annotation instanceof PathVariable) {
            value = ((PathVariable) annotation).value();
        } else if (annotation instanceof RequestParameter) {
            value = ((RequestParameter) annotation).value();
        } else if (annotation instanceof CookieValue) {
            value = ((CookieValue) annotation).value();
        } else if (annotation instanceof HeaderValue) {
            value = ((HeaderValue) annotation).value();
        }
        if (value == null || "".equals(value)) {
            return defaultValue;
        }
        return value;
    }


    private void processingRequestMethod(String basePath, List<Method> requestMethod) {
        requestMethod.forEach(it -> {
            PathMapping pathMapping = new PathMapping();
            String path = scanMethodRequestPath(it, pathMapping);
            pathMapping.setHandlerMethod(it);
            String fullPath = getBasePath(it.getDeclaringClass()) + path;

            pathMapping.setPath(basePath + fullPath);
            pathMapping.setPath(fullPath);
            if (normalPathMapping.containsKey(fullPath)) {
                normalPathMapping.get(fullPath).add(pathMapping);
            } else {
                normalPathMapping.put(fullPath, new ArrayList<>(List.of(pathMapping)));
            }
        });
    }

    private String getBasePath(Class clazz) {
        if (clazz.isAnnotationPresent(Request.class)) {
            Request request = (Request) clazz.getAnnotation(Request.class);
            return request.value();
        }
        if (clazz.isAnnotationPresent(Get.class)) {
            Get get = (Get) clazz.getAnnotation(Get.class);
            return get.value();
        }
        if (clazz.isAnnotationPresent(Post.class)) {
            Post post = (Post) clazz.getAnnotation(Post.class);
            return post.value();
        }
        if (clazz.isAnnotationPresent(Put.class)) {
            Put put = (Put) clazz.getAnnotation(Put.class);
            return put.value();
        }
        if (clazz.isAnnotationPresent(Delete.class)) {
            Delete delete = (Delete) clazz.getAnnotation(Delete.class);
            return delete.value();
        }
        return "";
    }

    private String scanMethodRequestPath(Method method, PathMapping pathMapping) {
        String path = null;
        if (method.isAnnotationPresent(Request.class)) {
            path = method.getAnnotation(Request.class).value();
            pathMapping.setRequestType(RequestType.REQUEST);
        } else if (method.isAnnotationPresent(Get.class)) {
            path = method.getAnnotation(Get.class).value();
            pathMapping.setRequestType(RequestType.GET);
        } else if (method.isAnnotationPresent(Post.class)) {
            path = method.getAnnotation(Post.class).value();
            pathMapping.setRequestType(RequestType.POST);
        } else if (method.isAnnotationPresent(Put.class)) {
            path = method.getAnnotation(Put.class).value();
            pathMapping.setRequestType(RequestType.PUT);
        } else if (method.isAnnotationPresent(Delete.class)) {
            path = method.getAnnotation(Delete.class).value();
            pathMapping.setRequestType(RequestType.DELETE);
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        return path;
    }

    public String scanClassRequestPath(Class clazz) {
        String path = null;
        if (clazz.getAnnotation(Request.class) != null) {
            path = ((Request) clazz.getAnnotation(Request.class)).value();
        } else if (clazz.getAnnotation(Get.class) != null) {
            path = ((Get) clazz.getAnnotation(Get.class)).value();
        } else if (clazz.getAnnotation(Post.class) != null) {
            path = ((Post) clazz.getAnnotation(Post.class)).value();
        } else if (clazz.getAnnotation(Put.class) != null) {
            path = ((Put) clazz.getAnnotation(Put.class)).value();
        } else if (clazz.getAnnotation(Delete.class) != null) {
            path = ((Delete) clazz.getAnnotation(Delete.class)).value();
        }
        if (path == null) {
            return null;
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    public Map<String, String> getPathFillingValue(String path, PathMapping pathMapping) {
        String requestPath = pathMapping.getPath();
        String[] pathArray = path.split("/");
        String[] requestPathArray = requestPath.split("/");
        Map<String, String> result = new HashMap<>();
        if (pathArray.length != requestPathArray.length) {
            return null;
        }
        for (int i = 0; i < pathArray.length; i++) {
            if (requestPathArray[i].startsWith("{") && requestPathArray[i].endsWith("}")) {
                String key = requestPathArray[i].substring(1, requestPathArray[i].length() - 1);
                result.put(key, pathArray[i]);
            } else if (!pathArray[i].equals(requestPathArray[i])) {
                return null;
            }
        }
        return result;
    }


    public WebMvcResolver(Class clazz) {
        log.info("WebMvcResolver开始初始化");
        beanDefinition = new BeanDefinition(clazz);
        log.info("扫描路径映射");
        scan();
        log.info("扫描路径映射完成");
        printPathHandler();
        log.info("WebMvcResolver初始化完成");
    }
}
