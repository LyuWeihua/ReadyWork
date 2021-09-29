package work.ready.examples.i18n;

import work.ready.core.component.i18n.I18nServerModule;
import work.ready.core.handler.RequestMethod;
import work.ready.core.module.Application;
import work.ready.core.server.Ready;

public class Main extends Application {

    @Override
    protected void initialize() {
        applicationConfig().setViewPath("i18n_view");
        handler().addHandler(new String[]{"/**"}, new RequestMethod[]{RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE}, false, bean().get(I18nServerModule.class));
    }

    public static void main(String[] args) {
        Ready.For(Main.class).Work(args);
    }

}
