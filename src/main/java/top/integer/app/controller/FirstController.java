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
