package work.ready.examples.database.service.model;

import work.ready.examples.database.model.Demo;
import work.ready.core.database.Page;
import work.ready.core.database.Record;

import java.util.List;
import java.util.Map;

public interface AutoAdvancedService {

    Integer getSumByAge();

    int getCountByGender(int... gender);

    int getCountByGender(List<Integer> gender);

    Integer[] getAllAge();

    Record[] getRecordArray();

    Demo[] getModelArray();

    List<Demo> getListByAgeWithOrderBy(int age, String orderBy);

    List<Map<String, Object>> getListByAgeWithGroupBy(int age, String groupBy);

    Map<String, List<Map<String, Object>>> getAllWithOrderByAndGroupBy(List<Integer> gender);

    Page<Demo> getAllByPage(int page, int size, String where, List<Integer> height, List<Integer> weight, String orderBy);

    Page<Record> getDataWithComplexQuery(int page, int size);

    Boolean insertRecord(String name, int gender, int age);

    Boolean updateRecord(int age, int height, int weight, List<String> name);

    Boolean deleteRecord(String... name);

    Demo getByName(String name);

    Boolean insertRecord1(String name, int gender, int age);

    Boolean updateRecord1(int age, int height, int weight, List<String> name);

    Boolean deleteRecord1(String... name);

    List<Demo> getByAgeAndNameLike(int age, String name);
}
