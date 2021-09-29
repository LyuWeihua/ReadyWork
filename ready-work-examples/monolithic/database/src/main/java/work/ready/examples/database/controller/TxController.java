package work.ready.examples.database.controller;

import work.ready.examples.database.service.business.TxService;
import work.ready.core.handler.Controller;
import work.ready.core.handler.route.RequestMapping;
import work.ready.core.ioc.annotation.Autowired;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.service.result.Failure;
import work.ready.core.service.result.Result;
import work.ready.core.service.result.Success;
import work.ready.core.service.status.Status;

@RequestMapping(value = "/tx")
public class TxController extends Controller {

    private static final Log logger = LogFactory.getLog(TxController.class);
    private static final String GENERIC_EXCEPTION = "ERROR10014";

    @Autowired
    private TxService txService;

    @RequestMapping
    public Result<String> singleDatasource() {
        txService.clear();
        Boolean makeTrouble = getParamToBoolean("trouble", false);
        try {
            if (txService.singleDatasource(makeTrouble)) {
                return Success.of("singleDatasource is success !");
            }
        } catch (Exception e) {
            logger.error("==> update record name=testForTx, age=55, gender=1, failed !");
            //e
        }
        return Failure.of(new Status(GENERIC_EXCEPTION, "singleDatasource is failure !"));
    }

    @RequestMapping
    public Result<String> crossDatasource() {
        txService.clear();
        Boolean makeTrouble = getParamToBoolean("trouble", false);
        try {
            if (txService.crossDatasource(makeTrouble)) {
                return Success.of("crossDatasource is success !");
            }
        } catch (Exception e) {
            logger.error("==> insert record into datasource 'main' and 'test' failed !");
            //e
        }
        return Failure.of(new Status(GENERIC_EXCEPTION, "crossDatasource is failure !"));
    }

}
