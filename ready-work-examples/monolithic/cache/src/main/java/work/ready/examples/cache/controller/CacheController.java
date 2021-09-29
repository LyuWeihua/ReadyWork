package work.ready.examples.cache.controller;

import work.ready.core.database.Page;
import work.ready.core.database.Record;
import work.ready.core.handler.Controller;
import work.ready.core.handler.RequestMethod;
import work.ready.core.handler.route.RequestMapping;
import work.ready.core.ioc.annotation.Autowired;
import work.ready.core.service.result.Result;
import work.ready.core.service.result.Success;
import work.ready.core.tools.validator.Assert;
import work.ready.examples.cache.service.AopCacheDemo;
import work.ready.examples.cache.service.CacheService;
import work.ready.examples.cache.service.DbCacheDemo;

import java.util.List;
import java.util.Map;

@RequestMapping("/cache")
public class CacheController extends Controller {

    @Autowired
    private AopCacheDemo aopCacheDemo;
    @Autowired
    private DbCacheDemo dbCacheDemo;
    @Autowired
    private CacheService cacheService;

    @RequestMapping
    public Result<String> index() {
        return Success.of("Hello world !");
    }

    @RequestMapping(value = "add", method = RequestMethod.POST)
    public Result<Boolean> addRecord() {
        String name = Assert.notEmpty(getParam("name"), "name is required");
        Integer gender = Assert.notNull(getParamToInt("gender"), "gender is required");
        Assert.greaterThan(gender, -1, "gender must be greater than or equal to 0");
        Assert.lessThan(gender, 2, "gender must be less than 2");
        int age = Assert.notNull(getParamToInt("age"), "age is required");
        Assert.greaterThan(age, 0, "age must be greater than 0");
        var data = aopCacheDemo.addRecord(name, gender, age);
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
        var data = aopCacheDemo.updateByName(name, Map.of("gender",gender, "age", age));
        return Success.of(data);
    }

    @RequestMapping(value = "delete", method = RequestMethod.DELETE)
    public Result<Boolean> deleteByName() {
        String name = Assert.notEmpty(getParam("name"), "name is required");
        var data = aopCacheDemo.deleteByName(name);
        return Success.of(data);
    }

    @RequestMapping
    public Result<Page> getByAge() {
        int page = getParamToInt("page", 1);
        int size = getParamToInt("size", 5);
        int age = Assert.notNull(getParamToInt("age"), "age is required");
        var data = aopCacheDemo.getByAge(page, size, age);
        return Success.of(data);
    }

    @RequestMapping
    public Result<List<Record>> getByGender() {
        int gender = getParamToInt("gender", 0);
        List<Record> data = dbCacheDemo.findByGender(gender);
        return Success.of(data);
    }

    @RequestMapping
    public Result<Record> getByName() {
        String name = Assert.notEmpty(getParam("name"), "name is required");
        Record data = dbCacheDemo.findByName(name);
        return Success.of(data);
    }

    @RequestMapping
    public Result<String> putCache() {
        String key = Assert.notEmpty(getParam("key"), "key is required");
        String value = Assert.notEmpty(getParam("value"), "value is required");
        Integer ttl = getParamToInt("ttl");
        if(ttl == null) {
            cacheService.putCache(key, value);
        } else {
            cacheService.putCache(key, value, ttl);
        }
        return Success.of(cacheService.getCache(key));
    }

    @RequestMapping
    public Result<String> getCache() {
        String key = Assert.notEmpty(getParam("key"), "key is required");
        return Success.of(cacheService.getCache(key));
    }

    @RequestMapping
    public Result<String> removeCache() {
        String key = Assert.notEmpty(getParam("key"), "key is required");
        cacheService.removeCache(key);
        return Success.of(cacheService.getCache(key));
    }

    @RequestMapping
    public Result<List> getCacheKeys() {
        return Success.of(cacheService.getKeys());
    }
}
