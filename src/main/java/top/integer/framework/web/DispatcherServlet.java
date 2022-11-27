package top.integer.framework.web;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.integer.app.Application;
import top.integer.framework.core.ioc.AnnotationContext;
import top.integer.framework.core.ioc.Context;
import top.integer.framework.core.ioc.Pair;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = "/", loadOnStartup = 1)
public class DispatcherServlet extends HttpServlet {
    private AnnotationContext context;
    private WebMvcResolver webMvcResolver;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String className = config.getServletContext().getInitParameter("base-class");
        Class<?> baseClass = null;
        try {
            baseClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        Class<?> finalBaseClass = baseClass;
        AnnotationContext annotationContext = new AnnotationContext(baseClass, context -> {
            WebMvcResolver webMvcResolver = new WebMvcResolver(finalBaseClass);
            DispatcherServlet.this.webMvcResolver = webMvcResolver;
            context.registerBean("webMvc", webMvcResolver);
        });
        config.getServletContext().setAttribute("ioc", annotationContext);
        this.context = annotationContext;
        webMvcResolver = context.getBean(WebMvcResolver.class);
    }

    public DispatcherServlet() throws SQLException {
        super();

    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doDispatch(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doDispatch(req, resp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doDispatch(req, resp);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doDispatch(req, resp);
    }


    protected void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html;charset=utf-8");
        PathMapping pathMapping = webMvcResolver.getPath(req.getServletPath(), req.getMethod());
        if (pathMapping == null) {
            resp.setStatus(404);
            resp.getWriter().write("404");
            return;
        }
        Map<String, String> pathValueMapping = null;
        if (pathMapping.isPlaceHolder()) {
            pathValueMapping = webMvcResolver.getPathFillingValue(req.getServletPath(), pathMapping);
        }
        List<Object> parameterFill = webMvcResolver.processRequestParameter(req, resp, pathMapping.getHandlerMethod(), pathValueMapping);
        webMvcResolver.invoke(context, resp, pathMapping.getHandlerMethod(), parameterFill);
    }
}
