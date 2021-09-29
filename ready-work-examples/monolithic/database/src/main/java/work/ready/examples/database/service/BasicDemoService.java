package work.ready.examples.database.service;

import work.ready.core.database.Db;
import work.ready.core.database.Page;
import work.ready.core.database.Record;
import work.ready.core.server.Ready;

import java.util.List;
import java.util.Map;

public class BasicDemoService {

    private static Db db = Ready.dbManager().getDb();

    public Record getByName(String name) {
        var sql = "select * from demo where name = ? limit 1";
        return db.findFirst(sql, name);
    }

    public Record getByNameFromTest(String name) {
        var sql = "select * from demo where name = ? limit 1";
        return db.use("test").findFirst(sql, name);
    }

    public List<Record> getAllByAge(int age) {
        var sql = "select * from demo where age > ?";
        return db.find(sql, age);
    }

    public Page<Record> getPageByAge(int page, int size, int age) {
        return db.paginate(page, size, "select *", "from demo where age > ?", age);
    }

    public Page<Record> getPageByAgeWithGroupBy(int page, int size, int age) {
        return db.paginate(page, size, true, "select *", "from demo where age > ? group by age", age);
    }

    public Page<Record> getPageByFullSql(int page, int size, int age) {
        String from = "from demo where age > ?";
        String totalRowSql = "select count(*) " + from;
        String findSql = "select * " + from + " order by age";
        return db.paginateByFullSql(page, size, totalRowSql, findSql, age);
    }

    public int insertBySql(){
        return db.update("insert into demo (name, gender, age, height, weight) values(?,?,?,?,?)", "test", 1, 20, 170, 65);
    }

    public boolean insertByRecord(){
        return db.save("demo",
                db.record(Map.of("name","test", "gender", 1, "age", 20, "height", 170, "weight", 65)));
    }

    public int updateAgeBySql(String name, int age){
        return db.update("update demo set age = ? where name = ?", age, name);
    }

    public int updateAgeByRecord(String name, int age){
        var list = db.find("select * from demo where name = ?", name);
        int count = 0;
        for(Record record : list) {
            if(db.update("demo", record.set("age", age))){
                count ++;
            }
        }
        return count;
    }

    public boolean updateAgeByRecordId(int id, int age){
        return db.update("demo", db.record().set("id", id).set("age", age));
    }

    public int deleteBySql(){
        return db.delete("delete from demo where name = ?", "test");
    }

    public int deleteByRecord() {
        var list = db.find("select * from demo where name = ?", "test");
        int count = 0;
        for(Record record : list) {
            if(db.delete("demo", record)){
                count ++;
            }
        }
        return count;
    }

    public boolean deleteByRecordId(int id){
        return db.delete("demo", db.record().set("id", id));
    }
}
