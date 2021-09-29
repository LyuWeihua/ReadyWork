package work.ready.examples.quickstart.service;

import work.ready.core.database.Page;
import work.ready.core.database.Record;
import java.util.List;
import java.util.Map;

public interface DemoService {
    List<Record> getAll();

    Boolean addRecord(String name, int gender, int age);

    int updateByName(String name, Map<String, Object> map);

    Boolean deleteByName(String name);

    Record getFirstByName(String name);

    List<Record> getAllByName(String name);

    Page<Record> getByAge(int page, int size, int age);
}
