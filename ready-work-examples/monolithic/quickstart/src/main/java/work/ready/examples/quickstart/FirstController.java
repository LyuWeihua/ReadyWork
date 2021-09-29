package work.ready.examples.quickstart;

import work.ready.examples.quickstart.service.DemoService;
import work.ready.core.database.Page;
import work.ready.core.database.Record;
import work.ready.core.handler.Controller;
import work.ready.core.handler.RequestMethod;
import work.ready.core.handler.route.RequestMapping;
import work.ready.core.ioc.annotation.Autowired;
import work.ready.core.server.Ready;
import work.ready.core.service.result.Result;
import work.ready.core.service.result.Success;
import work.ready.core.tools.validator.Assert;

import java.util.List;
import java.util.Map;

@RequestMapping(value = "/")
public class FirstController extends Controller {

    @Autowired
    private DemoService demoService;

    @RequestMapping
    public Result<String> index() {
        return Success.of("hello world !");
    }

    @RequestMapping
    public Result<List> db() {
        var data = Ready.dbManager().getDb().find("select * from demo");
        return Success.of(data);
    }

    @RequestMapping
    public Result<List> dbService() {
        var data = demoService.getAll();
        return Success.of(data);
    }

    @RequestMapping(value = "add", method = RequestMethod.POST)
    public Result<Boolean> addRecord() {
        String name = Assert.notEmpty(getParam("name"), "name is required");
        Integer gender = Assert.notNull(getParamToInt("gender"), "gender is required");
        Assert.greaterThan(gender, -1, "gender must be greater than or equal to 0");
        Assert.lessThan(gender, 2, "gender must be less than 2");
        int age = Assert.notNull(getParamToInt("age"), "age is required");
        Assert.greaterThan(age, 0, "age must be greater than 0");
        var data = demoService.addRecord(name, gender, age);
        return Success.of(data);
    }

    @RequestMapping(value = "update", method = RequestMethod.PUT)
    public Result<Integer> updateByName() {
        String name = Assert.notEmpty(getParam("name"), "name is required");
        Integer gender = Assert.notNull(getParamToInt("gender"), "gender is required");
        Assert.greaterThan(gender, -1, "gender must be greater than or equal to 0");
        Assert.lessThan(gender, 2, "gender must be less than 2");
        int age = Assert.notNull(getParamToInt("age"), "age is required");
        Assert.greaterThan(age, 0, "age must be greater than 0");
        var data = demoService.updateByName(name, Map.of("gender",gender, "age", age));
        return Success.of(data);
    }

    @RequestMapping(value = "delete", method = RequestMethod.DELETE)
    public Result<Boolean> deleteByName() {
        String name = Assert.notEmpty(getParam("name"), "name is required");
        var data = demoService.deleteByName(name);
        return Success.of(data);
    }

    @RequestMapping
    public Result<Record> getFirstByName() {
        String name = Assert.notEmpty(getParam("name"), "name is required");
        var data = demoService.getFirstByName(name);
        return Success.of(data);
    }

    @RequestMapping
    public Result<List> getAllByName() {
        String name = Assert.notEmpty(getParam("name"), "name is required");
        var data = demoService.getAllByName(name);
        return Success.of(data);
    }

    @RequestMapping
    public Result<Page> getByAge() {
        int page = getParamToInt("page", 1);
        int size = getParamToInt("size", 5);
        int age = Assert.notNull(getParamToInt("age"), "age is required");
        var data = demoService.getByAge(page, size, age);
        return Success.of(data);
    }


}
