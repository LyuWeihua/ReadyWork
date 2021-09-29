package work.ready.examples.get_started.api;

import work.ready.cloud.ReadyCloud;
import work.ready.core.handler.Controller;
import work.ready.core.handler.route.RequestMapping;
import work.ready.core.service.result.Result;
import work.ready.core.service.result.Success;

@RequestMapping("/api")
public class ApiController extends Controller {

    @RequestMapping
    public Result<String> hello() {
        return Success.of(ReadyCloud.getInstance().getNodeId() + " says hello to you !");
    }

    @RequestMapping
    public Result<String> hi() {
        return Success.of(ReadyCloud.getInstance().getNodeId() + " says hi to you !");
    }
}
