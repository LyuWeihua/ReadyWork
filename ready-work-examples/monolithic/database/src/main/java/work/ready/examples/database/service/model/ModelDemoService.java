package work.ready.examples.database.service.model;

import work.ready.examples.database.model.Demo;
import work.ready.core.database.Page;

import java.util.List;

public interface ModelDemoService {
    Demo getByName(String name);

    Demo getByNameFromTest(String name);

    List<Demo> getAllByAge(int age);

    Page<Demo> getPageByAge(int page, int size, int age);

    Page<Demo> getPageByAgeWithGroupBy(int page, int size, int age);

    Page<Demo> getPageByFullSql(int page, int size, int age);

    Boolean addRecord(String name, int gender, int age);

    Boolean updateRecord(String name, int age);

    Boolean updateRecord(int id, int age);

    Boolean deleteRecord(String name);

    Boolean deleteRecord(int id);
}
