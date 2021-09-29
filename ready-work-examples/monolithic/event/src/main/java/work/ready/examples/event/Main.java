package work.ready.examples.event;

import work.ready.core.event.Event;
import work.ready.core.event.GeneralEvent;
import work.ready.core.module.Application;
import work.ready.core.server.Ready;
import work.ready.examples.event.event.AppEvent;
import work.ready.examples.event.event.CustomizedEvent;
import work.ready.examples.event.service.ListenToEvent;

public class Main extends Application {

    @Override
    protected void initialize() {
        Ready.post(new GeneralEvent(AppEvent.APP_INITIALIZE, this, applicationConfig()));
        Ready.post(new CustomizedEvent<>(this, getName() + " is initializing..."));
    }

    public static void main(String[] args) {
        new ListenToEvent().listen(Event.READY_WORK_SERVER_START, false);
        Ready.For(Main.class).Work(args);
    }

}
