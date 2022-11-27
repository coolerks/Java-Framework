package top.integer.app.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import top.integer.app.service.FirstService;
import top.integer.framework.core.ioc.annotation.Controller;
import top.integer.framework.core.ioc.annotation.Inject;
import top.integer.framework.web.annotation.*;

import java.util.List;

@Controller
@Request("/second")
public class SecondController {
    @Inject
    FirstService firstService;

    @Post("/hello")
    public String hello() {
        return "hello";
    }

    @Get("/hello")
    public String hello2(@CookieValue("JSESSIONID") String val, @RequestParameter("name") List<Integer> list, @HeaderValue String cookie) {
        return "hello2";
    }

    @Delete("/hello")
    public String hello3() {
        return "hello3";
    }

    @Get("/hello4")
    public String hello4(HttpServletRequest request, HttpServletResponse response, HttpSession session, Cookie cookie) {
        return "hello4";
    }

    public FirstService getFirstService() {
        return firstService;
    }
}
