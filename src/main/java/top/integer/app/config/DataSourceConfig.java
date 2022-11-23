package top.integer.app.config;


import com.alibaba.druid.pool.DruidDataSource;
import top.integer.framework.core.ioc.annotation.Bean;
import top.integer.framework.core.ioc.annotation.Config;
import top.integer.framework.core.ioc.annotation.Inject;

import javax.sql.DataSource;
import java.util.HashSet;

@Config
public class DataSourceConfig {
    @Bean
    public DataSource getDataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://localhost:3306/tx");
        dataSource.setUsername("root");
        dataSource.setPassword("20020327");
        return dataSource;
    }

    @Bean
    public HashSet<String> getBat(DataSource dataSource) {
        System.out.println("dataSource = " + dataSource);
        HashSet<String> set = new HashSet<>();
        set.add("6666");
        set.add("7777");
        set.add("8888");
        return set;
    }


}
