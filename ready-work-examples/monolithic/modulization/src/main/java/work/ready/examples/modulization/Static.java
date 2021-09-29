package work.ready.examples.modulization;

import work.ready.core.handler.resource.VirtualHost;
import work.ready.core.handler.resource.VirtualHostHandler;
import work.ready.core.module.Application;
import work.ready.core.server.ApplicationConfig;

public class Static extends Application {

    @Override
    protected void appConfig(ApplicationConfig config) {
        //
    }

    @Override
    protected void initialize() {
        applicationConfig().getServer().setHttpPort(8081);
        applicationConfig().getStaticResource().getVirtualHost().addHost(
                new VirtualHost().setDomain("localhost")
                        .setPath("/")
                        .setBasePath("/static/images/")
                        .setDirectoryListingEnabled(true)
        );
        handler().addHandler(VirtualHostHandler.class);
    }
}
