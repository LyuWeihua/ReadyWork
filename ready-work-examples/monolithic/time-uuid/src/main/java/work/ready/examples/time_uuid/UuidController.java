package work.ready.examples.time_uuid;

import work.ready.core.handler.Controller;
import work.ready.core.handler.route.RequestMapping;
import work.ready.core.server.Ready;
import work.ready.core.service.result.Result;
import work.ready.core.service.result.Success;

@RequestMapping("/uuid")
public class UuidController extends Controller {

    @RequestMapping
    public Result<Long> index() {
        return Success.of(Ready.getId());
    }

}
