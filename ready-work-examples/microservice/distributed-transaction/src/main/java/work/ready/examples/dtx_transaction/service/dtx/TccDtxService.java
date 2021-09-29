package work.ready.examples.dtx_transaction.service.dtx;

import work.ready.cloud.client.annotation.Call;
import work.ready.core.database.annotation.Transactional;
import work.ready.core.database.cleverorm.IfFailure;
import work.ready.core.ioc.annotation.Service;
import work.ready.core.service.BusinessService;
import work.ready.examples.dtx_transaction.model.Demo;

import java.util.Map;

@Service
public class TccDtxService extends BusinessService {

    @Call(serviceId = "test-01", url = "/api/getById")
    public Demo getById(int id) {
        return IfFailure.get(null);
    }

    @Call(serviceId = "test-01", url = "/api/updateById")
    public int updateById(int gender, int age, int id) {
        return IfFailure.get(0);
    }

    @Transactional(type = "tcc", confirmMethod = "confirmTcc", cancelMethod = "cancelTcc")
    public boolean dtxService(Map<String, Object> param) {
        System.err.println("TCC transaction trying");

        //updateById(6, 66, 2); // we can't do any update id = 2 here, because it is in conflict with the confirmTcc and cancelTcc, we are in the same transaction but in different thread

        param.put("lockRecord", getById(2));  // lock row id = 2, assuming it locks the record here, no one else can modify
        if(param.get("withError") != null && param.get("withError").equals(true)) {
            throw new RuntimeException("Error for testing tcc transaction");
        }
        return true;
    }

    public void confirmTcc(Map<String, Object> param) {
        updateById(5, 88, 2);  // make some changes
        System.err.println("TCC transaction confirmed");
    }

    public void cancelTcc(Map<String, Object> param) {
        Demo record = (Demo)param.get("lockRecord");
        updateById(record.getGender(), record.getAge(), 2);    // rollback
        System.err.println("TCC transaction canceled");
    }
}
