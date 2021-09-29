package work.ready.examples.dtx_transaction.service.dtx;

import work.ready.cloud.client.annotation.Call;
import work.ready.core.database.annotation.Transactional;
import work.ready.core.database.cleverorm.IfFailure;
import work.ready.core.ioc.annotation.Autowired;
import work.ready.core.ioc.annotation.Service;
import work.ready.core.service.BusinessService;
import work.ready.examples.dtx_transaction.service.model.DemoService;


@Service
public class TxcDtxService extends BusinessService {

    @Autowired
    private DemoService demoService;

    @Call(serviceId = "test-01", url = "/api/updateByName")
    public int updateByName(int gender, int age, String name) {
        return IfFailure.get(0);
    }

    @Call(serviceId = "test-01", url = "/api/updateById")
    public int updateById(int gender, int age, int id) {
        return IfFailure.get(0);
    }

    @Transactional(type = "txc")
    public boolean dtxService(boolean withError) {
        demoService.updateByIdForTxc(6, 26, 3);
        if(updateById(6, 26, 2) > 0 &&
                updateById(6, 26, 4) > 0) {
            if(withError) {
                throw new RuntimeException("Error for testing txc transaction");
            }
            return true;
        }
        return false;
    }
}
