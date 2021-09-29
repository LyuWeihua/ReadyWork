package work.ready.examples.websocket;

import work.ready.core.handler.request.Request;
import work.ready.core.handler.websocket.Channel;
import work.ready.core.handler.websocket.ChannelListener;
import work.ready.core.ioc.annotation.WebSocketListener;

@WebSocketListener("/web/chat")
public class ChatListener implements ChannelListener<ChatMessage> {

    private String group = "private";

    @Override
    public void onConnect(Request request, Channel channel) {
        channel.context().put("name", request.getSession().getAttribute("name"));
        channel.join(group);
    }

    @Override
    public void onMessage(Channel channel, ChatMessage message) {
        if ("stop".equals(message.text)) {
            channel.close();
            return;
        }
        if(message.text.startsWith("group::")) {
            ChatMessage response = new ChatMessage();
            response.text = "mirror back: " + message.text.substring(7) + " by " + channel.context().get("name");
            channel.broadcast(group, response);
        } else {
            ChatMessage response = new ChatMessage();
            response.text = "mirror back: " + message.text + " by " + channel.context().get("name");
            channel.send(response);
        }
    }
}
