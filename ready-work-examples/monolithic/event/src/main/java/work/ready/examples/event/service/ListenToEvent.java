package work.ready.examples.event.service;

import work.ready.core.event.GeneralEvent;
import work.ready.core.server.Ready;

public class ListenToEvent {

    public void listen(String event, boolean async){
        Ready.eventManager().addListener(this, "eventHandler",
                (setter -> setter.addName(event).setAsync(async)));
    }

    public void eventHandler(GeneralEvent event) {
        System.err.println(event.getSender().toString());
    }

}
