package top.integer.app.component;

import top.integer.framework.core.ioc.annotation.Component;
import top.integer.framework.core.ioc.annotation.Inject;

import javax.sql.DataSource;
import java.io.Serializable;

@Component
public class MyComponent implements Serializable {
    @Inject
    DataSource dataSource;

    @Inject
    String name;

    public String getName() {
        return name;
    }

    @Inject
    public MyComponent() {
        System.out.println("初始化");
    }

    public DataSource getDataSource() {
        return dataSource;
    }
}
