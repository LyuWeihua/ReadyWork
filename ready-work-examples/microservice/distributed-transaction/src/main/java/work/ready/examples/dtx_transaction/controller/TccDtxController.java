package work.ready.examples.dtx_transaction.controller;

import work.ready.core.handler.Controller;
import work.ready.core.handler.route.RequestMapping;
import work.ready.core.ioc.annotation.Autowired;
import work.ready.core.service.result.Result;
import work.ready.core.service.result.Success;
import work.ready.core.tools.define.Kv;
import work.ready.examples.dtx_transaction.service.dtx.TccDtxService;

@RequestMapping("/tcc")
public class TccDtxController extends Controller {

    @Autowired
    private TccDtxService tccDtxService;

    @RequestMapping
    public Result<Boolean> dtxService() {
        boolean withError = getParamToBoolean("error", false);
        return Success.of(tccDtxService.dtxService(Kv.by("withError", withError)));
    }

}
