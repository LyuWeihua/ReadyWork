package work.ready.examples.database.service.model;

import work.ready.examples.database.model.Demo;
import work.ready.core.database.Record;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface AutoSimpleService {

    Record getFirst();

    List<Record> getTop3();

    List<Demo> getTop4();

    int getCount();

    int getCountByHobbies();

    List<Map<String, Object>> getCountByHeightGroupByAge();

    int getMaxByAge();

    List<Map<String, Object>> getMaxByHeightGroupByAge();

    int getMinByAge();

    List<Map<String, Object>> getMinByHeightGroupByAge();

    int getSumByWeight();

    List<Map<String, Object>> getSumByWeightGroupByAge();

    float getAvgByHeight();

    List<Map<String, Object>> getAvgByHeightGroupByAge();

    List<Record> getAll();

    List<Demo> getAllByGender(int gender);

    List<Demo> getAllByGenderOrderByAge(int gender);

    Map<String, List<Demo>> getAllGroupByGender();

    List<String> getNameByGender(int gender);

    List<Map<String, Object>> getAllOrderByAge();

    Set<Demo> getAllGroupById();

    Set<Map<String, Object>> getAllByStatus(int status);

    int updateByName(String name, Map<String, Object> fields);

    int updateByName(String[] name, Map<String, Object> fields);

    int updateByName(List<String> name, Map<String, Object> fields);

    int deleteByName(String name);

    int deleteByName(String[] name);

    int deleteByName(List<String> name);
}
