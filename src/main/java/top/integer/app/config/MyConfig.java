package top.integer.app.config;

import top.integer.framework.core.ioc.annotation.Bean;
import top.integer.framework.core.ioc.annotation.Config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Config
public class MyConfig {
    @Bean("name")
    public String getHello() {
        return "你好，世界";
    }

    @Bean("good")
    public String getSecond() {
        return "hello, world";
    }

    @Bean
    public List<String> getList(HashSet<String> set) {
        System.out.println("-----------***********-----------");
        List<String> strings = new ArrayList<>(set.stream().toList());
        return strings;
    }
}
