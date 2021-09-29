package work.ready.examples.logger;

import work.ready.core.module.Application;
import work.ready.core.server.Ready;

public class Main extends Application {

    public static void main(String[] args) {
        Ready.For(Main.class).Work(args);
    }

}
