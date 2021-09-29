package work.ready.examples.database.controller;

import work.ready.examples.database.model.Demo;
import work.ready.examples.database.service.model.ModelDemoService;
import work.ready.core.database.Page;
import work.ready.core.handler.Controller;
import work.ready.core.handler.RequestMethod;
import work.ready.core.handler.route.RequestMapping;
import work.ready.core.ioc.annotation.Autowired;
import work.ready.core.service.result.Result;
import work.ready.core.service.result.Success;
import work.ready.core.tools.validator.Assert;

import java.util.List;

@RequestMapping(value = "/model")
public class ModelController extends Controller {

    @Autowired
    private ModelDemoService demoService;

    @RequestMapping
    public Result<String> index() {
        return Success.of("hello world !");
    }

    @RequestMapping
    public Result<Demo> getByName() {
        String name = Assert.notEmpty(getParam("name"), "name is required");
        return Success.of(demoService.getByName(name));
    }

    @RequestMapping
    public Result<Demo> getByNameFromTest() {
        String name = Assert.notEmpty(getParam("name"), "name is required");
        return Success.of(demoService.getByNameFromTest(name));
    }

    @RequestMapping
    public Result<List<Demo>> getAllByAge() {
        int age = Assert.notNull(getParamToInt("age"), "age is required");
        return Success.of(demoService.getAllByAge(age));
    }

    @RequestMapping
    public Result<Page<Demo>> getPageByAge() {
        int page = getParamToInt("page", 1);
        int size = getParamToInt("size",5);
        int age = Assert.notNull(getParamToInt("age"), "age is required");
        return Success.of(demoService.getPageByAge(page, size, age));
    }

    @RequestMapping
    public Result<Page<Demo>> getPageByAgeWithGroupBy() {
        int page = getParamToInt("page", 1);
        int size = getParamToInt("size",5);
        int age = Assert.notNull(getParamToInt("age"), "age is required");
        return Success.of(demoService.getPageByAgeWithGroupBy(page, size, age));
    }

    @RequestMapping
    public Result<Page<Demo>> getPageByFullSql() {
        int page = getParamToInt("page", 1);
        int size = getParamToInt("size",5);
        int age = Assert.notNull(getParamToInt("age"), "age is required");
        return Success.of(demoService.getPageByFullSql(page, size, age));
    }

    @RequestMapping(method = RequestMethod.POST)
    public Result<Boolean> addRecord() {
        String name = Assert.notNull(getParam("name"), "name is required");
        int gender = Assert.notNull(getParamToInt("gender"), "gender is required");
        int age = Assert.notNull(getParamToInt("age"), "age is required");
        return Success.of(demoService.addRecord(name, gender, age));
    }

    @RequestMapping(method = RequestMethod.PUT)
    public Result<Boolean> updateRecord() {
        String name = Assert.notEmpty(getParam("name"), "name is required");
        int age = Assert.notNull(getParamToInt("age"), "age is required");
        return Success.of(demoService.updateRecord(name, age));
    }

    @RequestMapping(method = RequestMethod.PUT)
    public Result<Boolean> updateRecordById() {
        int id = Assert.notNull(getParamToInt("id"), "id is required");
        int age = Assert.notNull(getParamToInt("age"), "age is required");
        return Success.of(demoService.updateRecord(id, age));
    }

    @RequestMapping(method = RequestMethod.DELETE)
    public Result<Boolean> deleteRecord() {
        String name = Assert.notEmpty(getParam("name"), "name is required");
        return Success.of(demoService.deleteRecord(name));
    }

    @RequestMapping(method = RequestMethod.DELETE)
    public Result<Boolean> deleteRecordById() {
        int id = Assert.notNull(getParamToInt("id"), "id is required");
        return Success.of(demoService.deleteRecord(id));
    }

}
