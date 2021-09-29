package work.ready.examples.event.event;

import work.ready.core.event.BaseEvent;

public class CustomizedEvent<T> extends BaseEvent<T> {
    private String message;

    public CustomizedEvent(T source, String message) {
        super(source);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
