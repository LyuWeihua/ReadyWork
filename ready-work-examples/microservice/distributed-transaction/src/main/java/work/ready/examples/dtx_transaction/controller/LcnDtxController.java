package work.ready.examples.dtx_transaction.controller;

import work.ready.core.handler.Controller;
import work.ready.core.handler.route.RequestMapping;
import work.ready.core.ioc.annotation.Autowired;
import work.ready.core.service.result.Result;
import work.ready.core.service.result.Success;
import work.ready.examples.dtx_transaction.service.dtx.LcnDtxService;

@RequestMapping("/lcn")
public class LcnDtxController extends Controller {

    @Autowired
    private LcnDtxService lcnDtxService;

    @RequestMapping
    public Result<Integer> updateByName() {
        return Success.of(lcnDtxService.updateByName(0, 18, "name2"));
    }

    @RequestMapping
    public Result<Integer> updateById() {
        return Success.of(lcnDtxService.updateById(0, 20, 4));
    }

    @RequestMapping
    public Result<Boolean> dtxService1() {
        boolean withError = getParamToBoolean("error", false);
        return Success.of(lcnDtxService.dtxService1(withError));
    }

    @RequestMapping
    public Result<Boolean> dtxService2() {
        boolean withError = getParamToBoolean("error", false);
        return Success.of(lcnDtxService.dtxService2(withError));
    }
}
