package top.integer.framework.core.ioc;

import com.alibaba.druid.pool.DruidDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.integer.framework.core.exception.BeansException;
import top.integer.framework.core.ioc.factory.BeanDefinition;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Container {
    private Map<String, Object> beanNameContainer;
    private Map<Class, List<Pair>> classBeanContainer;

    public static final Logger logger = LoggerFactory.getLogger(Container.class);

    public void addBean(String beanName, Object bean) {
        if (beanNameContainer.containsKey(beanName)) {
            Object o = beanNameContainer.get(beanName);
            throw new BeansException("Bean名称重复，容器中已包含类型为[" + o.getClass() + "]" + "，值为[" + o + "]的bean");
        }
        beanNameContainer.put(beanName, bean);
        addBeanToClassBeanContainer(beanName, bean);
    }

    private void addBeanToClassBeanContainer(String beanName, Object bean) {
        BeanDefinition beanDefinition = new BeanDefinition(bean.getClass());
        Pair pair = new Pair(beanName, bean);
        // System.out.println(beanDefinition.getSuperClasses());
        addParent(pair, beanDefinition.getSuperClasses());
        addAllInterface(pair, beanDefinition.getInterfaces());
    }

    public List<String> getBeanNamesByType(Class clazz) {
        List<String> list = new ArrayList<>();
        List<Pair> pairs = classBeanContainer.get(clazz);
        if (pairs != null) {
            pairs.forEach(it -> list.add(it.key));
        }
        return list;
    }

    private void addParent(Pair pair, List<Class> items) {
//        System.out.println("items = " + items);
        for (Class item : items) {
            if(classBeanContainer.containsKey(item)) {
                classBeanContainer.get(item).add(pair);
                continue;
            }
            ArrayList<Pair> list = new ArrayList<>(2);
            list.add(pair);
            classBeanContainer.put(item, list);
        }
    }

    private void addAllInterface(Pair pair, List<Class> inter) {
        HashSet<Class> set = new HashSet<>();
        for (Class anInterface : inter) {
            getAllInterface(set, anInterface);
        }
        addParent(pair, set.stream().toList());
    }

    private void getAllInterface(HashSet<Class> set, Class<?> clazz) {
        set.add(clazz);
        for (Class<?> anInterface : clazz.getInterfaces()) {
            getAllInterface(set, anInterface);
        }
    }
    protected void getContainer() {
        System.out.println("-------------------------------------");
        System.out.println("beanNameContainer = " + beanNameContainer);
        System.out.println("-----------------classBeanContainer------------------");
//        System.out.println("classBeanContainer = " + classBeanContainer);
        for (Map.Entry<Class, List<Pair>> classListEntry : classBeanContainer.entrySet()) {
            StringJoiner joiner = new StringJoiner(", ");
            for (Pair pair : classListEntry.getValue()) {
                joiner.add("{" + pair.key + ": " + pair.value + "}");
            }
            System.out.println("[" + classListEntry.getKey() + "] " + "[" + joiner.toString() + "]");
        }
        System.out.println("-------------------------------------");

    }
    public <T> T getBean(Class<T> clazz) {
        List<Pair> list = classBeanContainer.getOrDefault(clazz, null);
        if (list == null || list.size() == 0) {
            return null;
        }

        //System.out.println("clazz = " + clazz);
        //list.forEach(it -> {
       //     System.out.println(it.key + " " + it.value);
       // });
        if (list.size() > 1) {
            throw new BeansException("含有多个[" + clazz + "]类型的Bean，请使用getBean(Class<T> clazz, String beanName)方法获取");
        }
        return (T) list.get(0).value;
    }

    public <T> T getBean(Class<T> clazz, String beanName) {
        List<Pair> list = classBeanContainer.get(clazz);
        if (list == null || list.size() == 0) {
            return null;
        }
        Optional<Pair> first = list.stream().filter(it -> it.key.equals(beanName)).findFirst();
        if (first.isEmpty()) {
            return null;
        }
        return (T) first.get().value;
    }

    public Container() {
        beanNameContainer = new ConcurrentHashMap<>(16);
        classBeanContainer = new ConcurrentHashMap<>(16);
        if (logger.isDebugEnabled()) {
            logger.debug("Container init");
        }
    }
}
