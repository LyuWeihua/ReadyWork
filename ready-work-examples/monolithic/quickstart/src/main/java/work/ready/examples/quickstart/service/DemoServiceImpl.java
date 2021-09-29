package work.ready.examples.quickstart.service;

import work.ready.core.component.cache.annotation.CacheEvict;
import work.ready.core.component.cache.annotation.Cacheable;
import work.ready.core.database.ModelService;
import work.ready.core.database.Page;
import work.ready.core.database.Record;
import work.ready.core.database.annotation.Auto;
import work.ready.core.database.cleverorm.IfFailure;
import work.ready.core.ioc.annotation.Service;
import work.ready.examples.quickstart.model.Demo;

import java.util.List;
import java.util.Map;

@Service
public class DemoServiceImpl extends ModelService<Demo> implements DemoService {

    @Override
    @Auto
    public List<Record> getAll(){
        return IfFailure.get(null);
    }

    @Override
    @CacheEvict(name = "testCache",key = "*")
    public Boolean addRecord(String name, int gender, int age) {
        Demo record = new Demo();
        record.setName(name);
        record.setGender(gender);
        record.setAge(age);
        return record.save();
    }

    @Override
    @Auto
    @CacheEvict(name = "testCache",key = "*")
    public int updateByName(String name, Map<String, Object> map) {
        return IfFailure.get(0);
    }

    @Override
    @Auto
    @CacheEvict(name = "testCache",key = "*")
    public Boolean deleteByName(String name) {
        return IfFailure.get(false);
    }

    @Override
    @Auto
    public Record getFirstByName(String name) {
        return IfFailure.get(null);
    }

    @Override
    @Auto
    public List<Record> getAllByName(String name) {
        return IfFailure.get(null);
    }

    @Override
    @Cacheable(name = "testCache", key = "page:#(page)-#(size)-#(age)")
    @Auto("select * from _TABLE_ where age > ?")
    public Page<Record> getByAge(int page, int size, int age) {
        return IfFailure.get(null);
    }

}
