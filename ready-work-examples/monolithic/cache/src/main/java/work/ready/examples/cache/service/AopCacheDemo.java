package work.ready.examples.cache.service;

import work.ready.core.component.cache.annotation.CacheEvict;
import work.ready.core.component.cache.annotation.Cacheable;
import work.ready.core.database.ModelService;
import work.ready.core.database.Page;
import work.ready.core.database.Record;
import work.ready.core.database.annotation.Auto;
import work.ready.core.database.cleverorm.IfFailure;
import work.ready.core.ioc.annotation.Service;
import work.ready.examples.cache.model.Demo;

import java.util.Map;

@Service
public class AopCacheDemo extends ModelService<Demo> {

    @CacheEvict(name = "aopCache",key = "*")
    @Auto("insert into _TABLE_(name, gender, age) value(?,?,?)")
    public Boolean addRecord(String name, int gender, int age) {
        return IfFailure.get(null);
    }

    @Auto
    @CacheEvict(name = "aopCache",key = "*")
    public int updateByName(String name, Map<String, Object> map) {
        return IfFailure.get(0);
    }

    @Auto
    @CacheEvict(name = "aopCache",key = "*")
    public Boolean deleteByName(String name) {
        return IfFailure.get(false);
    }

    @Cacheable(name = "aopCache", key = "page:#(page)-#(size)-#(age)")
    @Auto("select * from _TABLE_ where age > ?")
    public Page<Record> getByAge(int page, int size, int age) {
        return IfFailure.get(null);
    }

}
