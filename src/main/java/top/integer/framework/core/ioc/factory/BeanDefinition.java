package top.integer.framework.core.ioc.factory;

import cn.hutool.core.util.ClassUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BeanDefinition {
    private final Class clazz;

    public boolean isAbstract() {
        return Modifier.isAbstract(clazz.getModifiers());
    }

    public List<Class> getSuperClasses() {
        List<Class> list = new ArrayList<>(2);
        Class c1 = clazz;
        while (c1 != null) {
            list.add(c1);
            c1 = c1.getSuperclass();
        }
        return list;
    }

    public List<Class> getInterfaces() {
        return Arrays.asList(clazz.getInterfaces());
    }

    public List<Field> getFields() {
        return Arrays.asList(clazz.getDeclaredFields());
    }

    public List<Field> getFieldsByAnnotation(Class<? extends Annotation> clazz) {
        return getFields().stream().filter(it -> it.isAnnotationPresent(clazz)).collect(Collectors.toList());
    }

    public List<Method> getMethods() {
        return Arrays.asList(clazz.getDeclaredMethods());
    }

    public List<Method> getMethodsByAnnotation(Class<? extends Annotation> clazz) {
        return getMethods().stream().filter(it -> it.isAnnotationPresent(clazz)).collect(Collectors.toList());
    }

    public List<Constructor> getConstructors() {
        return Arrays.asList(clazz.getDeclaredConstructors());
    }

    public List<Constructor> getConstructorsByAnnotation(Class<? extends Annotation> clazz) {
        return getConstructors().stream().filter(it -> it.isAnnotationPresent(clazz)).collect(Collectors.toList());
    }

    public List<Constructor> getNoArgsConstructors() {
        return getConstructors().stream().filter(it -> it.getParameterCount() == 0).collect(Collectors.toList());
    }

    public List<Class<?>> getClassesByPackageName() {
        return ClassUtil.scanPackage(clazz.getPackageName()).stream().toList();
    }

    public List<Class<?>> getClassesByPackageNameAndAnnotation(Class<? extends Annotation> clazz) {
        return getClassesByPackageName().stream().filter(it -> it.isAnnotationPresent(clazz)).toList();
    }



    public boolean hasAnnotation(Class<? extends Annotation> clazz) {
        return this.clazz.isAnnotationPresent(clazz);
    }

    public Class getClazz() {
        return clazz;
    }

    public BeanDefinition(Class clazz) {
        if (clazz == null) {
            throw new NullPointerException();
        }
        this.clazz = clazz;
    }
}
