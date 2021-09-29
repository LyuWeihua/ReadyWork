package work.ready.examples.dtx_transaction;

import work.ready.cloud.EnableCloud;
import work.ready.cloud.cluster.CloudConfig;
import work.ready.core.module.Application;
import work.ready.core.server.ApplicationConfig;
import work.ready.core.server.Ready;

@EnableCloud
public class Node1 extends Application {
    public static final String name = "test-01";
    public static final String version = "1.0.1.210601";
    public static final String apiVersion = "v1.0";

    public void cloudConfig(CloudConfig config){
        config.setNodeId("Node-1");
    }

    @Override
    protected void globalConfig(ApplicationConfig config) {
        config.getServer().setHttpPort(8080);
    }

    public static void main(String[] args) {
        Ready.For(Node1.class).Work(args);
    }

}

