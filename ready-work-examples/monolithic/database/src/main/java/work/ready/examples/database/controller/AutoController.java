package work.ready.examples.database.controller;

import work.ready.examples.database.model.Demo;

import work.ready.examples.database.service.model.AutoAdvancedService;
import work.ready.examples.database.service.model.AutoSimpleService;
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
import java.util.Map;
import java.util.Set;

@RequestMapping(value = "/auto")
public class AutoController extends Controller {

    @Autowired
    private AutoSimpleService simpleService;

    @Autowired
    private AutoAdvancedService advancedService;

    @RequestMapping
    public Result<String> index() {
        return Success.of("hello world !");
    }

    @RequestMapping
    public Result<Record> getFirst() {
        return Success.of(simpleService.getFirst());
    }

    @RequestMapping
    public Result<List<Record>> getTop3() {
        return Success.of(simpleService.getTop3());
    }

    @RequestMapping
    public Result<List<Demo>> getTop4() {
        return Success.of(simpleService.getTop4());
    }

    @RequestMapping
    public Result<Integer> getCount() {
        return Success.of(simpleService.getCount());
    }

    @RequestMapping
    public Result<Integer> getCountByHobbies() {
        return Success.of(simpleService.getCountByHobbies());
    }

    @RequestMapping
    public Result<List<Map<String, Object>>> getCountByHeightGroupByAge() {
        return Success.of(simpleService.getCountByHeightGroupByAge());
    }

    @RequestMapping
    public Result<Integer> getMaxByAge() {
        return Success.of(simpleService.getMaxByAge());
    }

    @RequestMapping
    public Result<List<Map<String, Object>>> getMaxByHeightGroupByAge() {
        return Success.of(simpleService.getMaxByHeightGroupByAge());
    }

    @RequestMapping
    public Result<Integer> getMinByAge() {
        return Success.of(simpleService.getMinByAge());
    }

    @RequestMapping
    public Result<List<Map<String, Object>>> getMinByHeightGroupByAge() {
        return Success.of(simpleService.getMinByHeightGroupByAge());
    }

    @RequestMapping
    public Result<Integer> getSumByWeight() {
        return Success.of(simpleService.getSumByWeight());
    }

    @RequestMapping
    public Result<List<Map<String, Object>>> getSumByWeightGroupByAge() {
        return Success.of(simpleService.getSumByWeightGroupByAge());
    }

    @RequestMapping
    public Result<Float> getAvgByHeight() {
        return Success.of(simpleService.getAvgByHeight());
    }

    @RequestMapping
    public Result<List<Map<String, Object>>> getAvgByHeightGroupByAge() {
        return Success.of(simpleService.getAvgByHeightGroupByAge());
    }

    @RequestMapping
    public Result<List<Record>> getAll() {
        return Success.of(simpleService.getAll());
    }

    @RequestMapping
    public Result<List<Demo>> getAllByGender() {
        int gender = Assert.notNull(getParamToInt("gender"), "gender is required");
        Assert.greaterThan(gender, -1, "gender must be between 0 and 1");
        Assert.lessThan(gender, 2, "gender must be between 0 and 1");
        return Success.of(simpleService.getAllByGender(gender));
    }

    @RequestMapping
    public Result<List<Demo>> getAllByGenderOrderByAge() {
        int gender = Assert.notNull(getParamToInt("gender"), "gender is required");
        Assert.greaterThan(gender, -1, "gender must be between 0 and 1");
        Assert.lessThan(gender, 2, "gender must be between 0 and 1");
        return Success.of(simpleService.getAllByGenderOrderByAge(gender));
    }

    @RequestMapping
    public Result<Map<String, List<Demo>>> getAllGroupByGender() {
        return Success.of(simpleService.getAllGroupByGender());
    }

    @RequestMapping
    public Result<List<String>> getNameByGender() {
        int gender = Assert.notNull(getParamToInt("gender"), "gender is required");
        Assert.greaterThan(gender, -1, "gender must be between 0 and 1");
        Assert.lessThan(gender, 2, "gender must be between 0 and 1");
        return Success.of(simpleService.getNameByGender(gender));
    }

    @RequestMapping
    public Result<List<Map<String, Object>>> getAllOrderByAge() {
        return Success.of(simpleService.getAllOrderByAge());
    }

    @RequestMapping
    public Result<Set<Demo>> getAllGroupById() {
        return Success.of(simpleService.getAllGroupById());
    }

    @RequestMapping
    public Result<Set<Map<String, Object>>> getAllByStatus() {
        int status = Assert.notNull(getParamToInt("status"), "status is required");
        Assert.greaterThan(status, -1, "status must be between 0 and 1");
        Assert.lessThan(status, 2, "status must be between 0 and 1");
        return Success.of(simpleService.getAllByStatus(status));
    }

    @RequestMapping
    public Result<Integer> updateByName() {
        String name = Assert.notEmpty(getParam("name"), "name is required");
        return Success.of(simpleService.updateByName(name, Map.of("age",50)));
    }

    @RequestMapping
    public Result<Integer> updateByNameArray() {
        String name = Assert.notEmpty(getParam("name"), "name is required");
        return Success.of(simpleService.updateByName(new String[]{name, name, name}, Map.of("age",50)));
    }

    @RequestMapping
    public Result<Integer> updateByNameList() {
        String name = Assert.notEmpty(getParam("name"), "name is required");
        return Success.of(simpleService.updateByName(List.of(name, name, name), Map.of("age",50)));
    }

    @RequestMapping
    public Result<Integer> deleteByName() {
        String name = Assert.notEmpty(getParam("name"), "name is required");
        return Success.of(simpleService.deleteByName(name));
    }

    @RequestMapping
    public Result<Integer> deleteByNameArray() {
        String name = Assert.notEmpty(getParam("name"), "name is required");
        return Success.of(simpleService.deleteByName(new String[]{name, name, name}));
    }

    @RequestMapping
    public Result<Integer> deleteByNameList() {
        String name = Assert.notEmpty(getParam("name"), "name is required");
        return Success.of(simpleService.deleteByName(List.of(name, name, name)));
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    @RequestMapping
    public Result<Integer> getSumByAge() {
        return Success.of(advancedService.getSumByAge());
    }

    @RequestMapping
    public Result<Integer> getCountByGender() {
        return Success.of(advancedService.getCountByGender(1,0));
    }

    @RequestMapping
    public Result<Integer[]> getAllAge() {
        return Success.of(advancedService.getAllAge());
    }

    @RequestMapping
    public Result<Record[]> getRecordArray() {
        return Success.of(advancedService.getRecordArray());
    }

    @RequestMapping
    public Result<Demo[]> getModelArray() {
        return Success.of(advancedService.getModelArray());
    }

    @RequestMapping
    public Result<List<Demo>> getListByAgeWithOrderBy() {
        return Success.of(advancedService.getListByAgeWithOrderBy(10, "weight DESC"));
    }

    @RequestMapping
    public Result<List<Map<String, Object>>> getListByAgeWithGroupBy() {
        return Success.of(advancedService.getListByAgeWithGroupBy(10, "gender"));
    }

    @RequestMapping
    public Result<Map<String, List<Map<String, Object>>>> getAllWithOrderByAndGroupBy() {
        return Success.of(advancedService.getAllWithOrderByAndGroupBy(List.of(1,0)));
    }

    @RequestMapping
    public Result<Page<Demo>> getAllByPage() {
        int page = getParamToInt("page", 1);
        int size = getParamToInt("size",5);
        return Success.of(advancedService.getAllByPage(page, size, "age > 10", List.of(168, 170, 175), List.of(80, 65, 68), "age DESC"));
    }

    @RequestMapping
    public Result<Page<Record>> getDataWithComplexQuery() {
        int page = getParamToInt("page", 1);
        int size = getParamToInt("size",5);
        return Success.of(advancedService.getDataWithComplexQuery(page, size));
    }

    @RequestMapping(method = RequestMethod.POST)
    public Result<Boolean> insertRecord() {
        String name = Assert.notEmpty(getParam("name"), "name is required");
        int gender = Assert.notNull(getParamToInt("gender"), "gender is required");
        int age = Assert.notNull(getParamToInt("age"), "age is required");
        return Success.of(advancedService.insertRecord(name, gender, age));
    }

    @RequestMapping(method = RequestMethod.PUT)
    public Result<Boolean> updateRecord() {
        String name = Assert.notEmpty(getParam("name"), "name is required");
        int age = Assert.notNull(getParamToInt("age"), "age is required");
        int height = Assert.notNull(getParamToInt("height"), "height is required");
        int weight = Assert.notNull(getParamToInt("weight"), "weight is required");
        return Success.of(advancedService.updateRecord(age, height, weight, List.of(name)));
    }

    @RequestMapping(method = RequestMethod.DELETE)
    public Result<Boolean> deleteRecord() {
        String name = Assert.notEmpty(getParam("name"), "name is required");
        return Success.of(advancedService.deleteRecord(name));
    }

    @RequestMapping
    public Result<Demo> getByName() {
        String name = Assert.notEmpty(getParam("name"), "name is required");
        return Success.of(advancedService.getByName(name));
    }

    @RequestMapping(method = RequestMethod.POST)
    public Result<Boolean> insertRecord1() {
        String name = Assert.notEmpty(getParam("name"), "name is required");
        int gender = Assert.notNull(getParamToInt("gender"), "gender is required");
        int age = Assert.notNull(getParamToInt("age"), "age is required");
        return Success.of(advancedService.insertRecord1(name, gender, age));
    }

    @RequestMapping(method = RequestMethod.PUT)
    public Result<Boolean> updateRecord1() {
        String name = Assert.notEmpty(getParam("name"), "name is required");
        int age = Assert.notNull(getParamToInt("age"), "age is required");
        int height = Assert.notNull(getParamToInt("height"), "height is required");
        int weight = Assert.notNull(getParamToInt("weight"), "weight is required");
        return Success.of(advancedService.updateRecord1(age, height, weight, List.of(name)));
    }

    @RequestMapping(method = RequestMethod.DELETE)
    public Result<Boolean> deleteRecord1() {
        String name = Assert.notEmpty(getParam("name"), "name is required");
        return Success.of(advancedService.deleteRecord1(name));
    }

    @RequestMapping
    public Result<List<Demo>> getByAgeAndNameLike() {
        int age = Assert.notNull(getParamToInt("age"), "age is required");
        String name = Assert.notEmpty(getParam("name"), "name is required");
        return Success.of(advancedService.getByAgeAndNameLike(age, name));
    }
}
