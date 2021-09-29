package work.ready.examples.dtx_transaction.controller;

import work.ready.core.handler.Controller;
import work.ready.core.handler.route.RequestMapping;
import work.ready.core.ioc.annotation.Autowired;
import work.ready.core.service.result.Result;
import work.ready.core.service.result.Success;
import work.ready.core.tools.validator.Assert;
import work.ready.examples.dtx_transaction.model.Demo;
import work.ready.examples.dtx_transaction.service.api.ApiService;

@RequestMapping("/api")
public class ApiController extends Controller {

    @Autowired
    private ApiService service;

    @RequestMapping("/getById")
    public Result<Demo> getById() {
        int id = Assert.notNull(getParamToInt("id"), "id is required");
        return Success.of(service.getById(id));
    }

    @RequestMapping("/updateByName")
    public Result<Integer> updateByName() {
        String name = Assert.notEmpty(getParam("name"), "name is required");
        int gender = Assert.notNull(getParamToInt("gender"), "gender is required");
        int age = Assert.notNull(getParamToInt("age"), "age is required");
        return Success.of(service.updateByName(gender, age, name));
    }

    @RequestMapping("/updateById")
    public Result<Integer> updateById() {
        int id = Assert.notNull(getParamToInt("id"), "id is required");
        int gender = Assert.notNull(getParamToInt("gender"), "gender is required");
        int age = Assert.notNull(getParamToInt("age"), "age is required");
        return Success.of(service.updateById(gender, age, id));
    }

}
