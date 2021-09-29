package work.ready.examples.cache.service;

import work.ready.core.database.Db;
import work.ready.core.database.Record;
import work.ready.core.ioc.annotation.Service;
import work.ready.core.server.Ready;

import java.util.List;

@Service
public class DbCacheDemo {

    private final Db db = Ready.dbManager().getDb();

    public List<Record> findByGender(int gender) {
        return db.findByCache("DbCache", "all-gender-" + gender, "select * from demo where gender = ?", gender);
    }

    public Record findByName(String name) {
        return db.findFirstByCache("DbCache", "one-name-" + name, "select * from demo where name = ? limit 1", name);
    }

}
