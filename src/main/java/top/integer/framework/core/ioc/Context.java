package top.integer.framework.core.ioc;

import java.util.List;

public interface Context {
    <T> T getBean(Class<T> clazz);
    <T> T getBean(Class<T> clazz, String beanName);
    <T> T getBean(String beanName);
    List<String> getBeanNames(Class clazz);
}
