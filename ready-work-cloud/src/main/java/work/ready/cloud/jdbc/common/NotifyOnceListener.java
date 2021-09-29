package work.ready.cloud.jdbc.common;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class NotifyOnceListener<Response> implements ActionListener<Response> {

    private final AtomicBoolean hasBeenCalled = new AtomicBoolean(false);

    protected abstract void innerOnResponse(Response response);

    protected abstract void innerOnFailure(Exception e);

    @Override
    public final void onResponse(Response response) {
        if (hasBeenCalled.compareAndSet(false, true)) {
            innerOnResponse(response);
        }
    }

    @Override
    public final void onFailure(Exception e) {
        if (hasBeenCalled.compareAndSet(false, true)) {
            innerOnFailure(e);
        }
    }
}
