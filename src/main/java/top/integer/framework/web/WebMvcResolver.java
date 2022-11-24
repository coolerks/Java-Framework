package top.integer.framework.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.integer.framework.core.ioc.annotation.Controller;
import top.integer.framework.core.ioc.factory.BeanDefinition;
import top.integer.framework.web.annotation.*;

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
            throw new RuntimeException("没有找到对应的请求路径");
        }
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
            String fullPath = path;
            pathMapping.setPath(basePath + fullPath);
            if (normalPathMapping.containsKey(fullPath)) {
                normalPathMapping.get(fullPath).add(pathMapping);
            } else {
                normalPathMapping.put(fullPath, new ArrayList<>(List.of(pathMapping)));
            }
        });
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


    public WebMvcResolver(Class clazz) {
        log.info("WebMvcResolver开始工作");
        beanDefinition = new BeanDefinition(clazz);
        scan();
        printPathHandler();
    }
}
