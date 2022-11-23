package top.integer.framework.web;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.integer.app.Application;
import top.integer.framework.core.ioc.AnnotationContext;
import top.integer.framework.core.ioc.Context;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet(urlPatterns = "/", loadOnStartup = 1)
public class DispatcherServlet extends HttpServlet {
    private AnnotationContext context;
    private WebMvcResolver webMvcResolver;
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        AnnotationContext annotationContext = new AnnotationContext(Application.class, context -> {
            WebMvcResolver webMvcResolver = new WebMvcResolver(Application.class);
            DispatcherServlet.this.webMvcResolver = webMvcResolver;
        });
        config.getServletContext().setAttribute("ioc", annotationContext);
        this.context = annotationContext;
    }

    public DispatcherServlet() throws SQLException {
        super();

    }
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doDispatch(req, resp);
    }

    protected void doDispatch(HttpServletRequest req, HttpServletResponse resp) {

    }
}