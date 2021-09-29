package work.ready.examples.database.service.business;

import work.ready.core.database.annotation.Transactional;
import work.ready.examples.database.model.Demo;
import work.ready.core.ioc.annotation.Autowired;
import work.ready.core.ioc.annotation.Service;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.service.BusinessService;
import work.ready.examples.database.service.model.AutoAdvancedService;

@Service
public class TxService extends BusinessService {

    private static final Log logger = LogFactory.getLog(TxService.class);

    @Autowired
    private AutoAdvancedService autoService;

    @Transactional
    public Boolean singleDatasource(boolean makeTrouble) {
        String name = "testForTx";
        int age = 66;
        int gender = 0;
        if(autoService.insertRecord(name, age, gender)){
            logger.info("==> datasource:main, insert record name=testForTx, age=66, gender=0, success !");
            if(makeTrouble) {
                throw new RuntimeException("Making trouble for transaction to break down");
            }
            Demo demo = autoService.getByName(name);
            boolean ret = demo.setAge(55).setGender(1).update();
            if(ret) {
                logger.info("==> datasource:main, update record name=testForTx, age=55, gender=1, success !");
            } else {
                logger.warn("==> datasource:main, update record name=testForTx, age=55, gender=1, failure !");
            }
            return ret;
        }
        return false;
    }

    @Transactional
    public Boolean crossDatasource(boolean makeTrouble) {
        String name = "testForTx";
        int age = 66;
        int gender = 0;
        if(autoService.insertRecord(name, age, gender)){
            logger.info("==> datasource:main, insert record name=testForTx, age=66, gender=0, success !");
            boolean ret = autoService.insertRecord1(name, 77, 1);
            if(ret) {
                logger.info("==> datasource:test, insert record name=testForTx, age=77, gender=1, success !");
            } else {
                logger.warn("==> datasource:test, insert record name=testForTx, age=77, gender=1, failure !");
            }
            if(makeTrouble) throw new RuntimeException("Making trouble for transaction to break down");
            return ret;
        }
        return false;
    }

    public boolean clear(){
        return autoService.deleteRecord("testForTx");
    }
}
