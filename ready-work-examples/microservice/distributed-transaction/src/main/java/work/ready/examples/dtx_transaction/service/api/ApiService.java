package work.ready.examples.dtx_transaction.service.api;

import work.ready.core.database.annotation.Transactional;
import work.ready.core.ioc.annotation.Autowired;
import work.ready.core.ioc.annotation.Service;
import work.ready.core.service.BusinessService;
import work.ready.examples.dtx_transaction.model.Demo;
import work.ready.examples.dtx_transaction.service.model.DemoService;

@Service
public class ApiService extends BusinessService {

    @Autowired
    private DemoService demoService;

    public Demo getById(int id) {
        return demoService.getById(id);
    }

    @Transactional
    public int updateByName(int gender, int age, String name) {
        return demoService.updateByName(gender, age, name);
    }

    @Transactional
    public int updateById(int gender, int age, int id) {
        return demoService.updateById(gender, age, id);
    }

}
