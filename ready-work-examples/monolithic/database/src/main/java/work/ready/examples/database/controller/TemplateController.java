package work.ready.examples.database.controller;

import work.ready.examples.database.service.TemplateDemoService;
import work.ready.core.database.Page;
import work.ready.core.database.Record;
import work.ready.core.handler.Controller;
import work.ready.core.handler.route.RequestMapping;
import work.ready.core.ioc.annotation.Autowired;
import work.ready.core.service.result.Result;
import work.ready.core.service.result.Success;
import work.ready.core.tools.validator.Assert;

import java.util.List;

@RequestMapping(value = "/template")
public class TemplateController extends Controller {

    @Autowired
    private TemplateDemoService demoService;

    @RequestMapping
    public Result<String> index() {
        return Success.of("hello world !");
    }

    @RequestMapping
    public Result<Record> getByName() {
        String name = Assert.notEmpty(getParam("name"), "name is required");
        return Success.of(demoService.getByName(name));
    }

    @RequestMapping
    public Result<Record> getByName_1() {
        String name = Assert.notEmpty(getParam("name"), "name is required");
        int age = Assert.notNull(getParamToInt("age"), "age is required");
        return Success.of(demoService.getByName_1(name, age));
    }

    @RequestMapping
    public Result<Record> getByName_2() {
        String name = Assert.notEmpty(getParam("name"), "name is required");
        int age = Assert.notNull(getParamToInt("age"), "age is required");
        return Success.of(demoService.getByName_2(name, age));
    }

    @RequestMapping
    public Result<List<Record>> getByNameLike() {
        String name = Assert.notEmpty(getParam("name"), "name is required");
        return Success.of(demoService.getByNameLike(name));
    }

    @RequestMapping
    public Result<Page<Record>> getAllByPage() {
        int page = getParamToInt("page", 1);
        int size = getParamToInt("size",5);
        return Success.of(demoService.getAllByPage(page,size));
    }

    @RequestMapping
    public Result<Page<Record>> getAllByParam() {
        int page = getParamToInt("page", 1);
        int size = getParamToInt("size",5);
        return Success.of(demoService.getAllByParam(page, size, "age >", 15, "height >", 0));
    }

    @RequestMapping
    public Result<Page<Record>> getAllByParamFromTest() {
        int page = getParamToInt("page", 1);
        int size = getParamToInt("size",5);
        return Success.of(demoService.getAllByParamFromTest(page, size, "age >", 15, "height >", 0));
    }
}
