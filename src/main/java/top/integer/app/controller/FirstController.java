package top.integer.app.controller;

import top.integer.app.bean.User;
import top.integer.app.service.FirstService;
import top.integer.framework.core.ioc.annotation.Controller;
import top.integer.framework.core.ioc.annotation.Inject;
import top.integer.framework.web.annotation.*;

@Controller
@Request("/first")
public class FirstController {
    @Inject
    FirstService firstService;

    @Post("/hello")
    public String hello(@CookieValue String song, @CookieValue Integer val) {
        return "hello";
    }

    @Put("/user")
    public User user(@RequestBody User user, @CookieValue String song) {
        return user;
    }

    @Get("/user/{name}/{page}/{size}")
    public String page(@PathVariable("page") Integer page1, @PathVariable Integer size, @PathVariable String name) {
        return "page";
    }

    @Get("/hello")
    public String hello2() {
        return "redirect:/demo/my.jsp";
    }

    @Delete("/hello/{second}")
    public String hello3() {
        return "hello3";
    }

    @Get("/hello/{first}")
    public String hello4() {
        return "hello4";
    }

    @Request("/my/path/{good}/require/{path}")
    public String myPath() {
        return "myPath";
    }

    @Request("/my/path/55/require/666")
    public String myPath2() {
        return "myPath2";
    }

    public FirstService getFirstService() {
        return firstService;
    }
}
