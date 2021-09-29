package work.ready.examples.configuration;

import work.ready.core.database.datasource.DataSourceConfig;
import work.ready.core.module.Application;
import work.ready.core.server.ApplicationConfig;
import work.ready.core.server.Ready;

public class Main extends Application {

    @Override
    protected void globalConfig(ApplicationConfig config) {
        config.getDatabase().setDataSource("test", new DataSourceConfig()
                .setJdbcUrl("jdbc:mysql://127.0.0.1:3306/test?useUnicode=true&useSSL=false&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull")
                .setUsername("root").setPassword("12345678")
        );
    }

    @Override
    protected void appConfig(ApplicationConfig config) {
        //
    }

    public static void main(String[] args) {
        Ready.For(Main.class).Work(args);
    }

}
