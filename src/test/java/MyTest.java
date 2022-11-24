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
        System.out.println("webMvcResolver = " + webMvcResolver);
        System.out.println("webMvcResolver.getPath(\"/hello\", RequestType.GET) = " + webMvcResolver.getPath("/hello", RequestType.GET));
        System.out.println("webMvcResolver.getPath(\"/hello/1\", RequestType.GET) = " + webMvcResolver.getPath("/hello/1", RequestType.GET));
        System.out.println("webMvcResolver.getPath(\"/my/path/dfdasf/require/565aa\", RequestType.GET) = " + webMvcResolver.getPath("/my/path/dfdasf/require/565aa", RequestType.GET));
        System.out.println("webMvcResolver.getPath(\"/my/path/55/require/666\", RequestType.POST) = " + webMvcResolver.getPath("/my/path/55/require/666", RequestType.POST));
        System.out.println("webMvcResolver.getPath(\"/my/path/55/require/666\", RequestType.REQUEST) = " + webMvcResolver.getPath("/my/path/55/require/666", RequestType.REQUEST));
        System.out.println("webMvcResolver.getPath(\"/my/path/33/require/888/faa\", RequestType.GET) = " + webMvcResolver.getPath("/my/path/33/require/888/faa", RequestType.GET));
    }
}
