package work.ready.examples.database.service.model;

import work.ready.examples.database.model.Demo;
import work.ready.core.database.ModelService;
import work.ready.core.database.Page;
import work.ready.core.ioc.annotation.Service;

import java.util.List;

@Service
public class ModelDemoServiceImpl extends ModelService<Demo> implements ModelDemoService {

    @Override
    public Demo getByName(String name) {
        String sql = "select * from demo where name = ? limit 1";
        return dao.findFirst(sql, name);
    }

    @Override
    public Demo getByNameFromTest(String name) {
        String sql = "select * from demo where name = ? limit 1";
        return use("test").findFirst(sql, name);
    }

    @Override
    public List<Demo> getAllByAge(int age) {
        var sql = "select * from demo where age > ?";
        return dao.find(sql, age);
    }

    @Override
    public Page<Demo> getPageByAge(int page, int size, int age) {
        return dao.paginate(page, size, "select *", "from demo where age > ?", age);
    }

    @Override
    public Page<Demo> getPageByAgeWithGroupBy(int page, int size, int age) {
        return dao.paginate(page, size, true, "select *", "from demo where age > ? group by age", age);
    }

    @Override
    public Page<Demo> getPageByFullSql(int page, int size, int age) {
        String from = "from demo where age > ?";
        String totalRowSql = "select count(*) " + from;
        String findSql = "select * " + from + " order by age";
        return dao.paginateByFullSql(page, size, totalRowSql, findSql, age);
    }

    @Override
    public Boolean addRecord(String name, int gender, int age) {
        return new Demo().setName(name).setGender(gender).setAge(age).save();
    }

    @Override
    public Boolean updateRecord(String name, int age) {
        return dao.findFirst("select * from demo where name = ?", name).setAge(age).update();
    }

    @Override
    public Boolean updateRecord(int id, int age) {
        return findById(id).setAge(age).update();
    }

    @Override
    public Boolean deleteRecord(String name) {
        return dao.findFirst("select * from demo where name = ?", name).delete();
    }

    @Override
    public Boolean deleteRecord(int id) {
        return findById(id).delete();
    }
}
