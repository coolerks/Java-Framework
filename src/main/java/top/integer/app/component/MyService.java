package top.integer.app.component;

import top.integer.framework.core.ioc.annotation.Inject;
import top.integer.framework.core.ioc.annotation.Service;

import java.io.Serializable;

@Service
public class MyService {
    @Inject("name")
    public String aaa;

    public String getAaa() {
        return aaa;
    }

    public MyService(@Inject("good") String s) {
        System.out.println("s = " + s);
    }
}
