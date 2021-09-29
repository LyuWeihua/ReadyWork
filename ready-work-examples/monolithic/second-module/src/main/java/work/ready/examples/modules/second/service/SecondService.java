package work.ready.examples.modules.second.service;

import work.ready.core.ioc.annotation.Autowired;
import work.ready.core.ioc.annotation.Service;
import work.ready.examples.modules.second.SecondModule;

import java.util.HashMap;
import java.util.Map;

@Service
public class SecondService {

    @Autowired
    private SecondModule module;

    public Map<String, Object> getConfig() {
        //module.getApp();
        return module.getConfig();
    }

    public Map<String, Object> demo(){
        Map<String, Object> data = new HashMap<>();
        data.put("ddd","44444");
        data.put("eee","55555");
        data.put("fff","66666");
        return data;
    }
}
