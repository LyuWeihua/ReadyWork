package work.ready.examples.dtx_transaction.service.dtx;

import work.ready.cloud.client.annotation.Call;
import work.ready.core.database.annotation.Transactional;
import work.ready.core.database.cleverorm.IfFailure;
import work.ready.core.ioc.annotation.Service;
import work.ready.core.service.BusinessService;

@Service
public class LcnDtxService extends BusinessService {

    @Call(serviceId = "test-01", url = "/api/updateByName")
    public int updateByName(int gender, int age, String name) {
        return IfFailure.get(0);
    }

    @Call(serviceId = "test-01", url = "/api/updateById")
    public int updateById(int gender, int age, int id) {
        return IfFailure.get(0);
    }

    @Transactional
    public boolean dtxService1(boolean withError) {
        if(updateById(1, 20, 2) > 0 && updateById(0, 21, 3) > 0 &&
                updateById(1, 22, 4) > 0) {
            if(withError) {
                throw new RuntimeException("Error for testing lcn transaction");
            }
            return true;
        }
        return false;
    }

    @Transactional
    public boolean dtxService2(boolean withError) {
        if(updateById(2, 21, 2) > 0 && // it returns true
                updateById(2, 22, 211) > 0) {  // it returns false, there is no id = 211
            if(withError) {
                throw new RuntimeException("Error for testing lcn transaction");
            }
            return true;
        }
        return false;
    }

    @Transactional
    public boolean dtxServiceDeadLock1() {  // conflicting lock with the same table
        // update by name, name column in demo table without index, transaction will lock the whole table
        // while the first updateByName locked the table, the second updateByName cannot get the table lock before the first updateByName is committed.
        if(updateByName(3, 23, "name2") > 0 &&
                updateByName(3, 23, "name4") > 0) {
            return true;
        }
        return false;
    }

    @Transactional
    public boolean dtxServiceDeadLock2() {  // conflicting lock with the same row
        // update by id, id column in demo table is the key, transaction will only lock one row
        // while the first updateById locked the row id = 2, the second updateById cannot get the row lock before the first updateById is committed.
        if(updateById(4, 24, 2) > 0 &&
                updateById(4, 24, 2) > 0) {
            return true;
        }
        return false;
    }


    @Transactional
    public boolean dtxServiceDeadLock3() {  // conflicting lock with the same table
        // update by name, name column in demo table without index, transaction will lock the whole table
        // while the updateById locked the row id = 2, the updateByName cannot get table lock before the first updateById is committed.
        if(updateById(5, 25, 2) > 0 &&  // locked one row already
                updateByName(5, 25, "name4") > 0) { // it requires table lock
            return true;
        }
        return false;
    }
}
