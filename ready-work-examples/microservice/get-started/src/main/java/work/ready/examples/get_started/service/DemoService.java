package work.ready.examples.get_started.service;

import work.ready.cloud.client.annotation.Call;
import work.ready.core.database.cleverorm.IfFailure;
import work.ready.core.handler.RequestMethod;
import work.ready.core.ioc.annotation.Service;
import work.ready.core.service.BusinessService;

@Service
public class DemoService extends BusinessService {

    @Call(serviceId = "test-01", method = RequestMethod.GET, url = "/api/hello")
    public String hello() {
        return IfFailure.get(null);
    }

    @Call(serviceId = "test-01", method = RequestMethod.POST, url = "/api/hi")
    public String hi() {
        return IfFailure.get(null);
    }

    @Call(serviceId = "test-01", url = "/api/{method}")
    public String dynamicMethod(String method){
        return IfFailure.get(null);
    }

    @Call(serviceId = "", type=Call.RequestType.json, url = "https://getman.cn/echo")
    public String externalApi(String q){
        return IfFailure.get(null);
    }
}
