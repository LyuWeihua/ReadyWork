package work.ready.examples.dtx_transaction.service.model;

import work.ready.examples.dtx_transaction.model.Demo;

public interface DemoService {

    Demo getById(int id);

    int updateByName(int gender, int age, String name);

    int updateById(int gender, int age, int id);

    int updateByIdForTxc(int gender, int age, int id);
}
