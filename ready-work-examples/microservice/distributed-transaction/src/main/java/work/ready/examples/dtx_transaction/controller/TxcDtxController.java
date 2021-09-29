package work.ready.examples.dtx_transaction.controller;

import work.ready.core.handler.Controller;
import work.ready.core.handler.route.RequestMapping;
import work.ready.core.ioc.annotation.Autowired;
import work.ready.core.service.result.Result;
import work.ready.core.service.result.Success;
import work.ready.examples.dtx_transaction.service.dtx.TxcDtxService;

@RequestMapping("/txc")
public class TxcDtxController extends Controller {

    @Autowired
    private TxcDtxService txcDtxService;

    @RequestMapping
    public Result<Boolean> dtxService() {
        boolean withError = getParamToBoolean("error", false);
        return Success.of(txcDtxService.dtxService(withError));
    }

}
