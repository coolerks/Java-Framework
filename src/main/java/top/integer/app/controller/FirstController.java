package top.integer.app.controller;

import top.integer.app.service.FirstService;
import top.integer.framework.core.ioc.annotation.Controller;
import top.integer.framework.core.ioc.annotation.Inject;
import top.integer.framework.web.annotation.Delete;
import top.integer.framework.web.annotation.Get;
import top.integer.framework.web.annotation.Post;
import top.integer.framework.web.annotation.Request;

@Controller
@Request("/first")
public class FirstController {
    @Inject
    FirstService firstService;

    @Post("/hello")
    public String hello() {
        return "hello";
    }

    @Get("/hello")
    public String hello2() {
        return "hello2";
    }

    @Delete("/hello")
    public String hello3() {
        return "hello3";
    }

    @Get("/hello4")
    public String hello4() {
        return "hello4";
    }

    public FirstService getFirstService() {
        return firstService;
    }
}
