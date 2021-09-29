package work.ready.examples.modules.first.service;

import work.ready.core.ioc.annotation.Autowired;
import work.ready.core.ioc.annotation.Service;
import work.ready.examples.modules.first.FirstModule;

import java.util.HashMap;
import java.util.Map;

@Service
public class FirstService {

    @Autowired
    private FirstModule module;

    public Map<String, Object> getConfig() {
        //module.getApp();
        return module.getConfig();
    }

    public Map<String, Object> demo(){
        Map<String, Object> data = new HashMap<>();
        data.put("aaa","11111");
        data.put("bbb","22222");
        data.put("ccc","33333");
        return data;
    }
}
