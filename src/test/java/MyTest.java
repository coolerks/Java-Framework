import org.junit.jupiter.api.Test;
import top.integer.app.Application;
import top.integer.framework.core.ioc.AnnotationContext;
import top.integer.framework.web.RequestType;
import top.integer.framework.web.WebMvcResolver;

import java.util.concurrent.atomic.AtomicReference;

public class MyTest {
    @Test
    public void test() {
        AtomicReference<WebMvcResolver> resolver = new AtomicReference<>();
        AnnotationContext annotationContext = new AnnotationContext(Application.class, context -> {
            resolver.set(new WebMvcResolver(Application.class));
        });
        WebMvcResolver webMvcResolver = resolver.get();
        System.out.println("webMvcResolver.getPath(\"/second/hello\", RequestType.GET) = " + webMvcResolver.getPath("/second/hello", RequestType.GET));
    }
}
