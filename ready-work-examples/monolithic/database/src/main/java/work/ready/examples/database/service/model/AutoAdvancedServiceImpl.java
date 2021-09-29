package work.ready.examples.database.service.model;

import work.ready.examples.database.model.Demo;
import work.ready.core.database.ModelService;
import work.ready.core.database.Page;
import work.ready.core.database.Record;
import work.ready.core.database.annotation.Auto;
import work.ready.core.database.cleverorm.IfFailure;
import work.ready.core.ioc.annotation.Service;

import java.util.List;
import java.util.Map;

@Service
public class AutoAdvancedServiceImpl extends ModelService<Demo> implements AutoAdvancedService {

    @Override
    @Auto("select sum(age) from _TABLE_")
    public Integer getSumByAge() {
        return IfFailure.get(null);
    }

    @Override
    @Auto("select count(*) from _TABLE_ where gender in (?) ")
    public int getCountByGender(int... gender){
        return IfFailure.get(0);
    }

    @Override
    @Auto("select count(*) from _TABLE_ where gender in (?) ")
    public int getCountByGender(List<Integer> gender){
        return IfFailure.get(0);
    }

    @Override
    @Auto("select age from _TABLE_")
    public Integer[] getAllAge(){
        return IfFailure.get(null);
    }

    @Override
    @Auto("select name,age from _TABLE_")
    public Record[] getRecordArray() { return IfFailure.get(null); }

    @Override
    @Auto("select name,gender from _TABLE_")
    public Demo[] getModelArray() { return IfFailure.get(null); }

    @Override
    @Auto("select * from _TABLE_ where age > ? order by ?")
    public List<Demo> getListByAgeWithOrderBy(int age, String orderBy) { return IfFailure.get(null); }

    @Override
    @Auto("select gender, GROUP_CONCAT(name) from _TABLE_ where age > ? group by ?")
    public List<Map<String, Object>> getListByAgeWithGroupBy(int age, String groupBy) { return IfFailure.get(null); }

    @Override
    @Auto(value = "SELECT name,gender,age FROM _TABLE_ where gender in (?) ORDER BY created DESC", groupColumn = "gender", sortColumn = "age")
    public Map<String, List<Map<String, Object>>> getAllWithOrderByAndGroupBy(List<Integer> gender) { return IfFailure.get(null); }

    @Override
    @Auto("select * from _TABLE_ where ? and (height in (?) or weight in (?)) order by ?")
    public Page<Demo> getAllByPage(int page, int size, String where, List<Integer> height, List<Integer> weight, String orderBy){
        return IfFailure.get(null);
    }

    @Override
    @Auto("select (select count(*) from _TABLE_) count,a.* from (select * from _TABLE_) a left join (select * from _TABLE_) b on a.id = b.id order by created desc")
    public Page<Record> getDataWithComplexQuery(int page, int size){ return IfFailure.get(null); }

    @Override
    @Auto("insert into _TABLE_(name, gender, age) value(?,?,?)")
    public Boolean insertRecord(String name, int gender, int age){ return IfFailure.get(null); }

    @Override
    @Auto("update _TABLE_ set age = ?, height = ?, weight = ? where name in ( ? )")
    public Boolean updateRecord(int age, int height, int weight, List<String> name){ return IfFailure.get(null); }

    @Override
    @Auto("delete from _TABLE_ where name in ( ? )")
    public Boolean deleteRecord(String... name){ return IfFailure.get(null); }

    @Override
    @Auto("forAuto.getByName")
    public Demo getByName(String name) { return IfFailure.get(null); }

    @Override
    @Auto(db="test", value = "forAuto.insertRecord")
    public Boolean insertRecord1(String name, int gender, int age){ return IfFailure.get(null); }

    @Override
    @Auto("forAuto.updateRecord")
    public Boolean updateRecord1(int age, int height, int weight, List<String> name){ return IfFailure.get(null); }

    @Override
    @Auto("forAuto.deleteRecord")
    public Boolean deleteRecord1(String... name){ return IfFailure.get(null); }

    @Override
    @Auto(db = "test", value = "forAuto.getByAgeAndNameLike")
    public List<Demo> getByAgeAndNameLike(int age, String name) { return IfFailure.get(null); }

}
