package top.integer.framework.core.ioc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.integer.app.service.FirstService;
import top.integer.framework.core.exception.BeansException;
import top.integer.framework.core.ioc.annotation.*;
import top.integer.framework.core.ioc.factory.BeanDefinition;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Consumer;

public class AnnotationContext {
    private Container container;
    private Class baseClass;
    private BeanDefinition beanDefinition;
    private Logger log = LoggerFactory.getLogger(AnnotationContext.class);

    private void preRegisterBean() {
        log.info("开始注册Bean");
        List<Class<?>> classes = beanDefinition.getClassesByPackageName();
        List<BeanDefinition> configList = classes.stream()
                .filter(it -> it.isAnnotationPresent(Config.class))
                .map(BeanDefinition::new)
                .toList();
        // 方法到参数类型的映射
        // Map<Method, List<Class<?>>> methodParamClassMapping = new HashMap<>();

//        // 配置类中的方法参数的Bean名到参数类型的映射
//        Map<Method, List<String>> methodParamBeanNameMapping = new HashMap<>();
        // 类到配置类方法的映射
        Map<Class<?>, HashSet<Method>> classMethodMapping = new HashMap<>();
//        各bean类与bean类之间的映射
        Map<Class<?>, HashSet<Class<?>>> classToClassMapping = new HashMap<>();
        // Map<Class<?>, HashSet<String>> classToBeanName = new HashMap<>();
        HashSet<Method> noParamMethod = new HashSet<>();
        HashSet<Constructor> noParamConstructor = new HashSet<>();
        Map<Class<?>, HashSet<Constructor>> classConstructorMap = new HashMap<>();
        Map<Class<?>, HashSet<Field>> classFieldMap = new HashMap<>();

        configList.forEach(it -> {
            try {
                Constructor constructor = it.getClazz().getConstructor();
                noParamConstructor.add(constructor);
            } catch (NoSuchMethodException e) {
                throw new BeansException("配置类[" + it.getClazz() + "]的构造器参数必须为空");
            }
            List<Method> methodsByAnnotation = it.getMethodsByAnnotation(Bean.class);
            // 将方法和参数类型、参数类型和方法进行映射
            for (Method method : methodsByAnnotation) {
                if (method.getParameterCount() == 0) {
                    noParamMethod.add(method);
                }
                Class<?>[] parameterTypes = method.getParameterTypes();
                // methodParamClassMapping.put(method, Arrays.asList(parameterTypes));
                for (Class<?> parameterType : parameterTypes) {
                    if (classMethodMapping.get(parameterType) == null) {
                        HashSet<Method> value = new HashSet<>();
                        value.add(method);
                        classMethodMapping.put(parameterType, value);
                    } else {
                        classMethodMapping.get(parameterType).add(method);
                    }
                }
            }
        });


        List<BeanDefinition> componentList = classes.stream()
                .filter(it -> it.isAnnotationPresent(Component.class) || it.isAnnotationPresent(Service.class)
                        || it.isAnnotationPresent(Controller.class) || it.isAnnotationPresent(Repository.class))
                .map(BeanDefinition::new)
                .toList();

        componentList.forEach(it -> {
            Class clazz = it.getClazz();
            Constructor constructor = null;
            List<Constructor> constructorsByAnnotation = it.getConstructorsByAnnotation(Inject.class);
            if (constructorsByAnnotation.size() == 1) {
                constructor = constructorsByAnnotation.get(0);
            } else {
                Constructor[] declaredConstructors = clazz.getDeclaredConstructors();
                if (declaredConstructors.length != 1) {
                    throw new BeansException("[" + clazz + "]未提供能够有效识别的构造器");
                }
                constructor = declaredConstructors[0];
            }
            if (constructor.getParameters().length == 0) {
                noParamConstructor.add(constructor);
            }
            for (Class parameterType : constructor.getParameterTypes()) {
                if (classConstructorMap.get(parameterType) == null) {
                    HashSet<Constructor> value = new HashSet<>();
                    value.add(constructor);
                    classConstructorMap.put(parameterType, value);
                } else {
                    classConstructorMap.get(parameterType).add(constructor);
                }
            }


            for (Field field : it.getFieldsByAnnotation(Inject.class)) {
//                if (classToClassMapping.get(field.getType()) == null) {
//                    HashSet<Class<?>> value = new HashSet<>();
//                    value.add(it.getClazz());
//                    classToClassMapping.put(field.getType(), value);
//                } else {
//                    classToClassMapping.get(field.getType()).add(it.getClazz());
//                }
                if (classFieldMap.get(field.getType()) == null) {
                    HashSet<Field> value = new HashSet<>();
                    value.add(field);
                    classFieldMap.put(field.getType(), value);
                } else {
                    classFieldMap.get(field.getType()).add(field);
                }
            }
        });
//        System.out.println("classFieldMap001 = " + classFieldMap);
//        System.out.println("classMethodMapping = " + classMethodMapping);
        registerNoParamConstructorBean(noParamConstructor);
        registerNoParamMethodBean(noParamMethod, classConstructorMap, classFieldMap, classMethodMapping);
        registerInjectField(classFieldMap.keySet(), classFieldMap);
//        System.out.println("classFieldMap = " + classFieldMap);
//        System.out.println("classMethodMapping = " + classMethodMapping);
//        container.getContainer();
        log.info("IOC init finish.");
    }

    public <T> T getBean(Class<T> clazz) {
        return container.getBean(clazz);
    }

    public <T> T getBean(Class<T> clazz, String beanName) {
        return container.getBean(clazz, beanName);
    }

    private void registerMethod(Set<Class<?>> classes, Map<Class<?>, HashSet<Method>> classMethodMapping, Map<Class<?>, HashSet<Constructor>> classConstructorMap, Map<Class<?>, HashSet<Field>> classFieldMap) {
        if(classes == null || classes.size() == 0) {
            return;
        }
        Set<Class<?>> classSet = new HashSet<>();
        classes .forEach(it -> {
            HashSet<Method> methods = classMethodMapping.get(it);
            if (methods != null) {
                HashSet<Method> completeMethod = new HashSet<>();
                for (Method method : methods) {
                    List<Object> list = new ArrayList<>();
                    for (Parameter parameter : method.getParameters()) {
                        Object bean = null;
                        Class<?> type = parameter.getType();
                        Inject inject = type.getAnnotation(Inject.class);
                        String beanName = "";
                        if (inject != null) {
                            beanName = inject.value();
                        }
                        if ("".equals(beanName)) {
                            try {
                                bean = getBean(type);
                            } catch (BeansException e) {
                                bean = getBean(type, parameter.getName());
                            }
                        } else {
                            bean = getBean(type, beanName);
                        }
                        if (bean == null) {
                            break;
                        }
                        list.add(bean);
                    }
                    if (list.size() != method.getParameterCount()) {
                        continue;
                    }
                    Class<?> clazz = method.getDeclaringClass();
                    Object configInstance = container.getBean(clazz);
                    try {
                        Object result = method.invoke(configInstance, list.toArray());
                        Bean bean = method.getAnnotation(Bean.class);
                        String beanName = bean.value();
                        if ("".equals(beanName)) {
                            beanName = method.getName();
                        }
                        container.addBean(beanName, result);
                        completeMethod.add(method);
                        classSet.add(method.getReturnType());
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }
                methods.removeAll(completeMethod);
//                System.out.println("completeMethod = " + completeMethod);
            }

        });
        registerMethod(classSet, classMethodMapping, classConstructorMap, classFieldMap);
        registerOtherBeans(classSet, classConstructorMap, classFieldMap, classMethodMapping);
        registerInjectField(classSet, classFieldMap);
    }
    private void registerNoParamMethodBean(HashSet<Method> methods, Map<Class<?>, HashSet<Constructor>> classConstructorMap, Map<Class<?>, HashSet<Field>> classFieldMap, Map<Class<?>, HashSet<Method>> classMethodMapping) {

        HashSet<Class<?>> classSet = new HashSet<>();
        for (Method method : methods) {
            Class<?> clazz = method.getDeclaringClass();
            classSet.add(method.getReturnType());
            try {
                Object o = method.invoke(container.getBean(clazz));
                Bean bean = method.getAnnotation(Bean.class);
                if ("".equals(bean.value())) {
                    container.addBean(method.getName(), o);
                } else {
                    container.addBean(bean.value(), o);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
        registerOtherBeans(classSet, classConstructorMap, classFieldMap, classMethodMapping);
        registerInjectField(classSet, classFieldMap);
        registerMethod(classSet, classMethodMapping, classConstructorMap, classFieldMap);
    }

    private void registerInjectField(Set<Class<?>> classes, Map<Class<?>, HashSet<Field>> classFieldMap) {
        classes.forEach(it -> {
            HashSet<Field> fields = classFieldMap.get(it);
            HashSet<Field> completedFields = new HashSet<>();
            if (fields != null) {
                for (Field field : fields) {
                    Object o = container.getBean(field.getDeclaringClass());
                    if (o == null) {
                        continue;
                    }
                    Inject inject = field.getAnnotation(Inject.class);
                    String beanName = inject.value();
                    Object bean = null;
                    if ("".equals(beanName)) {
                        try {
                            bean = container.getBean(field.getType());
                        } catch (BeansException e) {
                            beanName = field.getName();
                            bean = container.getBean(field.getType(), beanName);
                        }
                    } else {
                        bean = container.getBean(field.getType(), beanName);
                    }
                    if (bean == null) {
                        continue;
                    }
                    try {
                        field.setAccessible(true);
                        field.set(o, bean);
                        completedFields.add(field);
                        if (o instanceof FirstService) {
                            FirstService o1 = (FirstService) o;
                        }
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
                fields.removeAll(completedFields);
            }
        });
    }

    private void registerOtherBeans(Set<Class<?>> classes, Map<Class<?>, HashSet<Constructor>> classConstructorMap, Map<Class<?>, HashSet<Field>> classFieldMap, Map<Class<?>, HashSet<Method>> classMethodMapping) {
        if (classes.size() == 0) {
            return;
        }
        HashSet<Class<?>> classSet = new HashSet<>();
        for (Class aClass : classes) {
            BeanDefinition beanDefinition = new BeanDefinition(aClass);
            // 通过父类注入
            for (Class superClass : beanDefinition.getSuperClasses()) {
                injectByClass(superClass, classConstructorMap, classSet);
            }
            // 通过接口进行注入
            for (Class anInterface : beanDefinition.getInterfaces()) {
                // System.out.println("anInterface = " + anInterface);
                injectByClass(anInterface, classConstructorMap, classSet);
            }

        }
        registerOtherBeans(classSet, classConstructorMap, classFieldMap, classMethodMapping);
        registerInjectField(classSet, classFieldMap);
        registerMethod(classSet, classMethodMapping, classConstructorMap, classFieldMap);
    }

    // 尝试通过接口、父类进行注入
    private void injectByClass(Class<?> clazz, Map<Class<?>, HashSet<Constructor>> classConstructorMap, HashSet<Class<?>> classSet) {
        HashSet<Constructor> constructors = classConstructorMap.get(clazz);
        HashSet<Constructor> completeRegister = new HashSet<>();
        if (constructors == null) {
            return;
        }
        // 通过构造器查找能够被实力化的
        constructors.forEach(it -> {
            Parameter[] parameters = it.getParameters();
            List<Object> list = new ArrayList<>();

            for (Parameter parameter : parameters) {
                Inject inject = parameter.getAnnotation(Inject.class);
                String beanName = "";
                if (inject != null) {
                    beanName = inject.value();
                }
                Object bean = null;
                if ("".equals(beanName)) {
                    try {
                        bean = getBean(parameter.getType());
                    } catch (BeansException e) {
                        bean = getBean(parameter.getType(), parameter.getName());
                    }
                } else {
                    bean = getBean(parameter.getType(), beanName);
                }

                if (bean == null) {
                    break;
                }
                list.add(bean);
            }


            if (list.size() == parameters.length) {
                String beanName = getBeanName(it.getDeclaringClass());
                try {
                    container.addBean(beanName, it.newInstance(list.toArray()));
                    classSet.add(it.getDeclaringClass());
                    completeRegister.add(it);
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        classConstructorMap.forEach((k, v) -> v.removeAll(completeRegister));
    }

    private String getBeanName(Class<?> beanClass) {
        if (beanClass == null) {
            return null;
        }
        Component component = beanClass.getAnnotation(Component.class);
        if (component != null && !"".equals(component.value())) {
            return component.value();
        }
        Service service = beanClass.getAnnotation(Service.class);
        if (service != null && !"".equals(service.value())) {
            return service.value();
        }
        Controller controller = beanClass.getAnnotation(Controller.class);
        if (controller != null && !"".equals(controller.value())) {
            return controller.value();
        }
        Repository repository = beanClass.getAnnotation(Repository.class);
        if (repository != null && !"".equals(repository.value())) {
            return repository.value();
        }
        // 类名首字母小写
        String simpleName = beanClass.getSimpleName();
        return simpleName.substring(0, 1).toLowerCase() + simpleName.substring(1);
    }

    private void registerNoParamConstructorBean(HashSet<Constructor> constructors) {
        for (Constructor constructor : constructors) {
            try {
                String beanName = getBeanName(constructor.getDeclaringClass());
                StringBuilder sb = new StringBuilder(beanName);
                sb.setCharAt(0, Character.isUpperCase(sb.charAt(0)) ? Character.toLowerCase(sb.charAt(0)) : sb.charAt(0));
                container.addBean(sb.toString(), constructor.newInstance());
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public List<String> getBeanNames(Class<?> clazz) {
        return container.getBeanNamesByType(clazz);
    }

    public AnnotationContext(Class baseClass) {
        if (baseClass == null) {
            throw new NullPointerException();
        }
        this.baseClass = baseClass;
        this.container = new Container();
        beanDefinition = new BeanDefinition(baseClass);
        container.addBean("context", this);
        preRegisterBean();
    }

    public AnnotationContext(Class baseClass, Runnable callback) {
        this(baseClass);
        callback.run();
    }

    public void registerBean(String name, Object o) {
        container.addBean(name, o);
    }

    public AnnotationContext(Class baseClass, Consumer<AnnotationContext> context) {
        this(baseClass);
        context.accept(this);
    }
}
