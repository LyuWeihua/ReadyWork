package work.ready.examples.mvc;

import work.ready.core.handler.resource.*;
import work.ready.core.handler.route.RouteConfig;
import work.ready.core.module.Application;
import work.ready.core.server.Ready;

public class Main extends Application {

    @Override
    protected void initialize() {
        route().add(new RouteConfig().addRoute("/", ManualController.class, "index", null)
                .addRoute("/test/", ManualController.class, "test", null));
        applicationConfig().setViewPath("/view");

        applicationConfig().getStaticResource().getPathResource().addPath(
                new PathResource().setPath("/list").setBasePath("/static/files/").setPrefix(true).setDirectoryListingEnabled(true));
        handler().addHandler(PathResourceHandler.class);

        applicationConfig().getStaticResource().setMapping("/favicon.ico", "/static/favicon.ico")
                                                .setMapping("/download/","/download/");
        handler().addHandler(StaticResourceHandler.class);

        applicationConfig().getStaticResource().getVirtualHost().addHost(
                new VirtualHost().setDomain("images.localhost").setPath("/").setBasePath("/static/images/").setDirectoryListingEnabled(true)
        );
        handler().addHandler(VirtualHostHandler.class);

        //applicationConfig().setUploadPath("/upload");
        //applicationConfig().setDirectlySaveUploadFile(true);

        serverConfig().setMaxEntitySize(20 * 1024 * 1024);
        applicationConfig().setMaxUploadFileSize(10 * 1024 * 1024);
    }

    public static void main(String[] args) {
        Ready.For(Main.class).Work(args);
    }
}
