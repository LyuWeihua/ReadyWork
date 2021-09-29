package work.ready.examples.security;

import work.ready.core.handler.RequestMethod;
import work.ready.core.module.Application;
import work.ready.core.security.cors.CorsServerModule;
import work.ready.core.server.Ready;
import work.ready.core.tools.define.Kv;

import java.util.Arrays;

public class Main extends Application {

    @Override
    protected void initialize() {
        applicationConfig().getServerModule().getModuleConfig().put("corsServerModule",
                Kv.by("allowedOrigins", Arrays.asList(new String[]{"*"})).set("allowedMethods", Arrays.asList(new String[]{"GET"})));
        handler().addHandler(new String[]{"/**"}, new RequestMethod[]{RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE}, new CorsServerModule());

        security().setEnableDownloadRateLimiter(true);
        security().setEnableSkipLocalIp(false);
        security().setLimitRateBytes(10 * 1024);

        security().setEnableConcurrentRequestLimiter(true);
        security().setMaxConcurrentPerIp(2);
    }

    public static void main(String[] args) {
        Ready.For(Main.class).Work(args);
    }

}
