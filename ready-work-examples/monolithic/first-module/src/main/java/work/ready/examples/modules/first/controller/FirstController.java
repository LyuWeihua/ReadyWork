package work.ready.examples.modules.first.controller;

import work.ready.core.handler.Controller;
import work.ready.core.handler.route.RequestMapping;
import work.ready.core.ioc.annotation.Autowired;
import work.ready.core.service.result.Result;
import work.ready.core.service.result.Success;
import work.ready.examples.modules.first.service.FirstService;

import java.util.Map;

@RequestMapping("/module/first")
public class FirstController extends Controller {

    @Autowired
    private FirstService service;

    @RequestMapping
    public Result<Map<String, Object>> demo(){
        Map<String, Object> data = service.demo();
        data.putAll(service.getConfig());
        return Success.of(data);
    }

}
