package work.ready.examples.database.controller;

import work.ready.examples.database.service.BasicDemoService;
import work.ready.core.database.Page;
import work.ready.core.database.Record;
import work.ready.core.handler.Controller;
import work.ready.core.handler.RequestMethod;
import work.ready.core.handler.route.RequestMapping;
import work.ready.core.ioc.annotation.Autowired;
import work.ready.core.service.result.Result;
import work.ready.core.service.result.Success;
import work.ready.core.tools.validator.Assert;

import java.util.List;

@RequestMapping(value = "/basic")
public class BasicController extends Controller {

    @Autowired
    private BasicDemoService demoService;

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
    public Result<Record> getByNameFromTest() {
        String name = Assert.notEmpty(getParam("name"), "name is required");
        return Success.of(demoService.getByNameFromTest(name));
    }

    @RequestMapping
    public Result<List<Record>> getAllByAge() {
        int age = Assert.notNull(getParamToInt("age"), "age is required");
        return Success.of(demoService.getAllByAge(age));
    }

    @RequestMapping
    public Result<Page<Record>> getPageByAge() {
        int page = getParamToInt("page", 1);
        int size = getParamToInt("size",5);
        int age = Assert.notNull(getParamToInt("age"), "age is required");
        return Success.of(demoService.getPageByAge(page, size, age));
    }

    @RequestMapping
    public Result<Page<Record>> getPageByAgeWithGroupBy() {
        int page = getParamToInt("page", 1);
        int size = getParamToInt("size",5);
        int age = Assert.notNull(getParamToInt("age"), "age is required");
        return Success.of(demoService.getPageByAgeWithGroupBy(page, size, age));
    }

    @RequestMapping
    public Result<Page<Record>> getPageByFullSql() {
        int page = getParamToInt("page", 1);
        int size = getParamToInt("size",5);
        int age = Assert.notNull(getParamToInt("age"), "age is required");
        return Success.of(demoService.getPageByFullSql(page, size, age));
    }

    @RequestMapping(method = RequestMethod.POST)
    public Result<Integer> insertBySql() {
        return Success.of(demoService.insertBySql());
    }

    @RequestMapping(method = RequestMethod.POST)
    public Result<Boolean> insertByRecord() {
        return Success.of(demoService.insertByRecord());
    }

    @RequestMapping(method = RequestMethod.PUT)
    public Result<Integer> updateAgeBySql() {
        String name = Assert.notEmpty(getParam("name"), "name is required");
        int age = Assert.notNull(getParamToInt("age"), "age is required");
        return Success.of(demoService.updateAgeBySql(name, age));
    }

    @RequestMapping(method = RequestMethod.PUT)
    public Result<Integer> updateAgeByRecord() {
        String name = Assert.notEmpty(getParam("name"), "name is required");
        int age = Assert.notNull(getParamToInt("age"), "age is required");
        return Success.of(demoService.updateAgeByRecord(name, age));
    }

    @RequestMapping(method = RequestMethod.PUT)
    public Result<Boolean> updateAgeByRecordId() {
        int id = Assert.notNull(getParamToInt("id"), "id is required");
        int age = Assert.notNull(getParamToInt("age"), "age is required");
        return Success.of(demoService.updateAgeByRecordId(id, age));
    }

    @RequestMapping(method = RequestMethod.DELETE)
    public Result<Integer> deleteBySql() {
        return Success.of(demoService.deleteBySql());
    }

    @RequestMapping(method = RequestMethod.DELETE)
    public Result<Integer> deleteByRecord() {
        return Success.of(demoService.deleteByRecord());
    }

    @RequestMapping(method = RequestMethod.DELETE)
    public Result<Boolean> deleteByRecordId() {
        int id = Assert.notNull(getParamToInt("id"), "id is required");
        return Success.of(demoService.deleteByRecordId(id));
    }

}
