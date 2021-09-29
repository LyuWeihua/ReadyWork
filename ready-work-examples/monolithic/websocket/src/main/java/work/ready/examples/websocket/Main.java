package work.ready.examples.websocket;

import work.ready.core.module.Application;
import work.ready.core.server.Ready;

public class Main extends Application {

    @Override
    protected void initialize() {
        applicationConfig().setViewPath("/chat");
        //ChatListener add by @WebSocketListener already, so we don't need to add it manually
        //webSocket().addListener("/web/chat", ChatMessage.class, ChatMessage.class, ChatListener.class);
    }

    public static void main(String[] args) {
        Ready.For(Main.class).Work(args);
    }
}
