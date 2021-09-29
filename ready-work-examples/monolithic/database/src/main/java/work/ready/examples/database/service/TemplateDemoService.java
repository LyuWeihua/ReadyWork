package work.ready.examples.database.service;

import work.ready.core.database.Db;
import work.ready.core.database.Page;
import work.ready.core.database.Record;
import work.ready.core.database.SqlParam;
import work.ready.core.ioc.annotation.Service;
import work.ready.core.server.Ready;
import work.ready.core.tools.define.Kv;
import work.ready.core.tools.validator.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TemplateDemoService {

    private static Db db = Ready.dbManager().getDb();

    public Record getByName(String name) {
        var sql = db.getSql("demo.getByName");
        return db.findFirst(sql, name);
    }

    public Record getByName_1(String name, int age) {
        var sql = db.getSqlParam("demo.getByName_1", name, age);
        return db.findFirst(sql);
        //return db.template("demo.getByName_1", name, age).findFirst();
    }

    public Record getByName_2(String name, int age) {
        var sql = db.getSqlParam("demo.getByName_2", Kv.by("name", name).set("age", age));
        return db.findFirst(sql);
        //return db.template("demo.getByName_2", Kv.by("name", name).set("age", age)).findFirst();
    }

    public List<Record> getByNameLike(String name) {
        return db.template("demo.getByNameLike", name).find();
    }

    public Page<Record> getAllByPage(int page, int size) {
        //return db.template("demo.getAll").paginate(page, size);
        SqlParam sqlParam = db.getSqlParam("demo.getAllByPage");
        return db.paginate(1, 5, sqlParam);
    }

    public Page<Record> getAllByParam(int page, int size, Object... param) {
        Assert.equals(0, param.length % 2, "param should be in pairs");
        Map<String, Object> map = new HashMap<>();
        for(int i = 0; i < param.length; i+=2) {
            map.put(param[i].toString(), param[i+1]);
        }
        return db.template("demo.getAllByDynamicParameter", Kv.by("condition", map)).paginate(page, size);
    }

    public Page<Record> getAllByParamFromTest(int page, int size, Object... param) {
        Assert.equals(0, param.length % 2, "param should be in pairs");
        Map<String, Object> map = new HashMap<>();
        for(int i = 0; i < param.length; i+=2) {
            map.put(param[i].toString(), param[i+1]);
        }
        return db.use("test").template("test.getAllByDynamicParameter", Kv.by("condition", map)).paginate(page, size);
    }
}
