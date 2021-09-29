package work.ready.examples.database.service.model;

import work.ready.examples.database.model.Demo;
import work.ready.core.database.ModelService;
import work.ready.core.database.Record;
import work.ready.core.database.annotation.Auto;
import work.ready.core.database.cleverorm.IfFailure;
import work.ready.core.ioc.annotation.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AutoSimpleServiceImpl extends ModelService<Demo> implements AutoSimpleService {

    @Override
    @Auto
    public Record getFirst() {
        return IfFailure.get(null);
    }

    @Override
    @Auto
    public List<Record> getTop3() {
        return IfFailure.get(null);
    }

    @Override
    @Auto(db = "test")
    public List<Demo> getTop4() {
        return IfFailure.get(null);
    }

    @Override
    @Auto
    public int getCount() {
        return IfFailure.get(0);
    }

    @Override
    @Auto
    public int getCountByHobbies() {
        return IfFailure.get(0);
    }

    @Override
    @Auto
    public List<Map<String, Object>> getCountByHeightGroupByAge() {
        return IfFailure.get(null);
    }

    @Override
    @Auto
    public int getMaxByAge() {
        return IfFailure.get(0);
    }

    @Override
    @Auto
    public List<Map<String, Object>> getMaxByHeightGroupByAge() {
        return IfFailure.get(null);
    }

    @Override
    @Auto
    public int getMinByAge() {
        return IfFailure.get(0);
    }

    @Override
    @Auto
    public List<Map<String, Object>> getMinByHeightGroupByAge() {
        return IfFailure.get(null);
    }

    @Override
    @Auto
    public int getSumByWeight() {
        return IfFailure.get(0);
    }

    @Override
    @Auto
    public List<Map<String, Object>> getSumByWeightGroupByAge() {
        return IfFailure.get(null);
    }

    @Override
    @Auto
    public float getAvgByHeight() {
        return IfFailure.get(0);
    }

    @Override
    @Auto
    public List<Map<String, Object>> getAvgByHeightGroupByAge() {
        return IfFailure.get(null);
    }

    @Override
    @Auto
    public List<Record> getAll() {
        return IfFailure.get(null);
    }

    @Override
    @Auto
    public List<Demo> getAllByGender(int gender) {
        return IfFailure.get(null);
    }

    @Override
    @Auto()
    public List<Demo> getAllByGenderOrderByAge(int gender) {
        return IfFailure.get(null);
    }

    @Override
    @Auto(groupColumn = "gender", sortColumn = "age", sortBy = Auto.Order.DESC)
    public Map<String, List<Demo>> getAllGroupByGender() {
        return IfFailure.get(null);
    }

    @Override
    @Auto
    public List<String> getNameByGender(int gender) {
        return IfFailure.get(null);
    }

    @Override
    @Auto(sortColumn = "gender", sortBy = Auto.Order.ASC, orderBy = Auto.Order.DESC)
    public List<Map<String, Object>> getAllOrderByAge() {
        return IfFailure.get(null);
    }

    @Override
    @Auto(groupColumn = "name", sortColumn = "age", sortBy = Auto.Order.DESC)
    public Set<Demo> getAllGroupById() {
        return IfFailure.get(null);
    }

    @Override
    @Auto(groupColumn = "name", sortColumn = "height", sortBy = Auto.Order.DESC)
    public Set<Map<String, Object>> getAllByStatus(int status) {
        return IfFailure.get(null);
    }

    //可选返回类型：
    //Map<String, Object>
    //Map<String, List<Map<String, Object>>>
    //Map<String, List<Record>>
    //Map<String, List<? extends Model>>

    //List<...>
    //List<Map<String, Object>>
    //List<Record>
    //List<? extends Model>

    //Set<...>
    //Set<Map<String, Object>>
    //Set<Record>
    //Set<? extends Model>

    @Override
    @Auto
    public int updateByName(String name, Map<String, Object> fields){
        return IfFailure.get(0);
    }

    @Override
    @Auto
    public int updateByName(String[] name, Map<String, Object> fields){
        return IfFailure.get(0);
    }

    @Override
    @Auto
    public int updateByName(List<String> name, Map<String, Object> fields){
        return IfFailure.get(0);
    }

    @Override
    @Auto
    public int deleteByName(String name){
        return IfFailure.get(0);
    }

    @Override
    @Auto
    public int deleteByName(String[] name){
        return IfFailure.get(0);
    }

    @Override
    @Auto
    public int deleteByName(List<String> name){
        return IfFailure.get(0);
    }
}
