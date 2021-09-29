package work.ready.examples.get_started;

import work.ready.core.handler.Controller;
import work.ready.core.handler.route.RequestMapping;
import work.ready.core.ioc.annotation.Autowired;
import work.ready.core.service.result.Result;
import work.ready.core.service.result.Success;
import work.ready.core.tools.validator.Assert;
import work.ready.examples.get_started.service.DemoService;

@RequestMapping("/")
public class DemoController extends Controller {

    @Autowired
    private DemoService service;

    @RequestMapping
    public Result<String> hello() {
        return Success.of(service.hello());
    }

    @RequestMapping
    public Result<String> hi() {
        return Success.of(service.hi());
    }

    @RequestMapping
    public Result<String> dynamicMethod() {
        String method = Assert.notEmpty(getParam("method"), "method is null, options are 'hello' and 'hi'");
        return Success.of(service.dynamicMethod(method));
    }

    @RequestMapping
    public Result<String> externalApi() {
        String q = Assert.notEmpty(getParam("q"), "q is null, it can be any string");
        return Success.of(service.externalApi(q));
    }

}
