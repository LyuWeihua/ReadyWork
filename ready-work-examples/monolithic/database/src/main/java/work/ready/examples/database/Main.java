package work.ready.examples.database;

import work.ready.examples.database.event.DbChangeEventListener;
import work.ready.examples.database.event.JdbcEventListener;
import work.ready.examples.database.model.Demo;
import work.ready.core.database.datasource.DataSourceConfig;
import work.ready.core.module.Application;
import work.ready.core.server.ApplicationConfig;
import work.ready.core.server.Ready;

public class Main extends Application {

    @Override
    protected void globalConfig(ApplicationConfig config) {
        config.getDatabase().setDataSource("test", new DataSourceConfig()
                .setType(DataSourceConfig.TYPE_MYSQL)
                .setJdbcUrl("jdbc:mysql://127.0.0.1:3306/test?useUnicode=true&useSSL=false&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull")
                .setUsername("root").setPassword("12345678").setSqlTemplate("sql/test.sql")
        );
    }

    @Override
    protected void initialize() {
        //dbManager().getDatasourceAgent("test").setDevMode(true);
        //dbManager().getDatasourceAgent("test").addSqlTemplate("sql/test.sql");
        //dbManager().getDatasourceAgent("test").addMapping("demo", Demo.class);

        dbManager().addJdbcEventListener(new JdbcEventListener());
        dbManager().addDbChangeListener(dbManager().getTable(Demo.class).getName(), new DbChangeEventListener());
    }

    public static void main(String[] args) {
        Ready.For(Main.class).Work(args);
    }
}
