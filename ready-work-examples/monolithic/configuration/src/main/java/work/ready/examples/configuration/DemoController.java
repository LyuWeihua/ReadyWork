package work.ready.examples.configuration;

import work.ready.core.config.ConfigInjector;
import work.ready.core.handler.Controller;
import work.ready.core.handler.route.RequestMapping;
import work.ready.core.ioc.annotation.Autowired;
import work.ready.core.ioc.annotation.Value;
import work.ready.core.service.result.Result;
import work.ready.core.service.result.Success;

import java.util.Map;

@RequestMapping(value = "/")
public class DemoController extends Controller {

    //this is to get value from values.yaml, if it's not exist the default 127.0.0.1 will be used
    @Value("${httpIp:127.0.0.1}")
    String serverIp;

    //this is to get value from values.yaml, if it's not exist the default 8080 will be used
    @Value("${httpPort:8080}")
    String serverPort;

    //this is to get value from bootstrap/application config
    @Value(value = "${readyWork.server.dynamicPort}", source = Value.Source.APPLICATION)
    boolean dynamicPort;

    @Value(value = "${otherConfig.password}", source = Value.Source.APPLICATION)
    String password;

    @Autowired
    OtherConfig otherConfig;

    @RequestMapping
    public Result<String> index() {
        return Success.of(
                "serverIp: " + serverIp + "," +
                      "serverPort: " + serverPort + "," +
                      "dynamicPort: " + dynamicPort);
    }

    @RequestMapping
    public Result<Map> readConfig() {
        Map config = ConfigInjector.getConfigBean("${otherConfig}", Map.class, null, null);
        return Success.of(config);
    }

    @RequestMapping
    public Result<OtherConfig> autoConfig() {
        return Success.of(otherConfig);
    }

    @RequestMapping
    public Result<String> password() {
        return Success.of("password: " + password);
    }
}
