package work.ready.examples.dtx_transaction.service.model;

import work.ready.core.database.ModelService;
import work.ready.core.database.annotation.Auto;
import work.ready.core.database.cleverorm.IfFailure;
import work.ready.core.ioc.annotation.Service;
import work.ready.examples.dtx_transaction.model.Demo;

@Service
public class DemoServiceImpl extends ModelService<Demo> implements DemoService {

    @Override
    public Demo getById(int id) {
        return findById(id);
    }

    @Override
    @Auto("update _TABLE_ set gender = ?, age = ? where name = ?")
    public int updateByName(int gender, int age, String name){ return IfFailure.get(0); }

    @Override
    @Auto("update _TABLE_ set gender = ?, age = ? where id = ?")
    public int updateById(int gender, int age, int id){ return IfFailure.get(0); }

    @Override
    @Auto("update _TABLE_ set gender = ?, age = ? where id = ?")
    public int updateByIdForTxc(int gender, int age, int id){ return IfFailure.get(0); }
}
