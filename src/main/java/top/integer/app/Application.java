package top.integer.app;

import top.integer.app.component.MyService;
import top.integer.app.service.FirstService;
import top.integer.framework.core.ioc.AnnotationContext;
import top.integer.framework.web.WebMvcResolver;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public class Application {
    public static void main(String[] args) throws SQLException {
        AnnotationContext annotationContext = new AnnotationContext(Application.class, context -> {
            WebMvcResolver webMvcResolver = new WebMvcResolver(Application.class);
        });
        FirstService bean = annotationContext.getBean(FirstService.class);
        System.out.println("bean.getDao() = " + bean.getDao());
        MyService myService = annotationContext.getBean(MyService.class);
        System.out.println("myService.getAaa() = " + myService.getAaa());
        System.out.println("context.getBean(Set.class) = " + annotationContext.getBean(Set.class));
        System.out.println("context.getBean(List.class) = " + annotationContext.getBean(List.class));
    }
}
