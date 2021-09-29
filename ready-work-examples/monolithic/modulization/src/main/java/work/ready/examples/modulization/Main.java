
package work.ready.examples.modulization;

import work.ready.core.module.AppModule;
import work.ready.core.module.Application;
import work.ready.core.server.ApplicationConfig;
import work.ready.core.server.Ready;
import work.ready.examples.modules.first.FirstModule;
import work.ready.examples.modules.second.SecondModule;

import java.util.List;

public class Main extends Application {

    public static void registerModules(List<Class<? extends AppModule>> moduleList) {
        moduleList.add(FirstModule.class);
        moduleList.add(SecondModule.class);
    }

    @Override
    protected void globalConfig(ApplicationConfig config) {
        //
    }

    @Override
    protected void appConfig(ApplicationConfig config) {
        //
    }

    public static void main(String[] args) {
        Ready.For(Main.class, Static.class).Work(args);
    }
}
