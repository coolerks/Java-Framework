package top.integer.framework.web;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.integer.framework.core.ioc.Pair;
import top.integer.framework.core.ioc.annotation.Controller;
import top.integer.framework.core.ioc.factory.BeanDefinition;
import top.integer.framework.web.annotation.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class WebMvcResolver {
    private Map<String, List<PathMapping>> normalPathMapping = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(WebMvcResolver.class);
    private BeanDefinition beanDefinition;


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
        if (pathMapping == null) {
            throw new RuntimeException("没有找到对应的请求路径");
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
            throw new RuntimeException("没有找到对应的请求路径");
        }
        pathMapping.setPlaceHolder(true);
        return pathMapping;
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
            throw new RuntimeException("Controller must have RequestMapping annotation");
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    public List<Pair> getPathFillingValue(String path, PathMapping pathMapping) {
        String requestPath = pathMapping.getPath();
        String[] pathArray = path.split("/");
        String[] requestPathArray = requestPath.split("/");
        if (pathArray.length != requestPathArray.length) {
            return null;
        }
        List<Pair> fillingValue = new ArrayList<>();
        for (int i = 0; i < pathArray.length; i++) {
            if (requestPathArray[i].startsWith("{") && requestPathArray[i].endsWith("}")) {
                fillingValue.add(new Pair(requestPathArray[i].substring(1, requestPathArray[i].length() - 1), pathArray[i]));
            } else if (!pathArray[i].equals(requestPathArray[i])) {
                return null;
            }
        }
        return fillingValue;
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
