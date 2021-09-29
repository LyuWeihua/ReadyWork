package work.ready.examples.event.service;

import work.ready.core.event.Event;
import work.ready.core.event.EventListener;
import work.ready.core.event.GeneralEvent;
import work.ready.examples.event.event.AppEvent;
import work.ready.examples.event.event.CustomizedEvent;

public class EventService {

    @EventListener(name = Event.WEB_SERVER_CREATE, async = true)
    public void predefinedEvent(GeneralEvent event){
        System.err.println(event.getSender().toString());
    }

    @EventListener(name = AppEvent.APP_INITIALIZE, async = true)
    public void userDefinedEvent(GeneralEvent event){
        System.err.println(event.getSender().toString());
        System.err.println(event.getObject().toString());
    }

    @EventListener(CustomizedEvent.class)
    public <T> void listenToCustomizedEvent(CustomizedEvent<T> customizedEvent) {
        System.err.println(customizedEvent.getSource().toString());
        System.err.println(customizedEvent.getMessage());
    }
}


