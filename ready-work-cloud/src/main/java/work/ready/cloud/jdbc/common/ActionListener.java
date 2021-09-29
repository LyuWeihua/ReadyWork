package work.ready.cloud.jdbc.common;

import work.ready.core.tools.define.CheckedConsumer;
import work.ready.core.tools.define.CheckedFunction;
import work.ready.core.tools.define.CheckedRunnable;
import work.ready.core.tools.define.CheckedSupplier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface ActionListener<Response> {
    
    void onResponse(Response response);

    void onFailure(Exception e);

    static <Response> ActionListener<Response> wrap(CheckedConsumer<Response, ? extends Exception> onResponse,
                                                    Consumer<Exception> onFailure) {
        return new ActionListener<Response>() {
            @Override
            public void onResponse(Response response) {
                try {
                    onResponse.accept(response);
                } catch (Exception e) {
                    onFailure(e);
                }
            }

            @Override
            public void onFailure(Exception e) {
                onFailure.accept(e);
            }
        };
    }

    static <T> ActionListener<T> delegateResponse(ActionListener<T> delegate, BiConsumer<ActionListener<T>, Exception> bc) {
        return new ActionListener<T>() {

            @Override
            public void onResponse(T r) {
                delegate.onResponse(r);
            }

            @Override
            public void onFailure(Exception e) {
                bc.accept(delegate, e);
            }
        };
    }

    static <T, R> ActionListener<T> delegateFailure(ActionListener<R> delegate, BiConsumer<ActionListener<R>, T> bc) {
        return new ActionListener<T>() {

            @Override
            public void onResponse(T r) {
                bc.accept(delegate, r);
            }

            @Override
            public void onFailure(Exception e) {
                delegate.onFailure(e);
            }
        };
    }

    static <Response> ActionListener<Response> wrap(Runnable runnable) {
        return wrap(r -> runnable.run(), e -> runnable.run());
    }

    static <T, Response> ActionListener<Response> map(ActionListener<T> delegate, CheckedFunction<Response, T, Exception> fn) {
        return new ActionListener<>() {
            @Override
            public void onResponse(Response response) {
                T mapped;
                try {
                    mapped = fn.apply(response);
                } catch (Exception e) {
                    onFailure(e);
                    return;
                }
                try {
                    delegate.onResponse(mapped);
                } catch (RuntimeException e) {
                    assert false : new AssertionError("map: listener.onResponse failed", e);
                    throw e;
                }
            }

            @Override
            public void onFailure(Exception e) {
                try {
                    delegate.onFailure(e);
                } catch (RuntimeException ex) {
                    if (ex != e) {
                        ex.addSuppressed(e);
                    }
                    assert false : new AssertionError("map: listener.onFailure failed", ex);
                    throw ex;
                }
            }
        };
    }

    static <Response> BiConsumer<Response, Exception> toBiConsumer(ActionListener<Response> listener) {
        return (response, throwable) -> {
            if (throwable == null) {
                listener.onResponse(response);
            } else {
                listener.onFailure(throwable);
            }
        };
    }

    static <Response> void onResponse(Iterable<ActionListener<Response>> listeners, Response response) {
        List<Exception> exceptionList = new ArrayList<>();
        for (ActionListener<Response> listener : listeners) {
            try {
                listener.onResponse(response);
            } catch (Exception ex) {
                try {
                    listener.onFailure(ex);
                } catch (Exception ex1) {
                    exceptionList.add(ex1);
                }
            }
        }
        ExceptionsHelper.maybeThrowRuntimeAndSuppress(exceptionList);
    }

    static <Response> void onFailure(Iterable<ActionListener<Response>> listeners, Exception failure) {
        List<Exception> exceptionList = new ArrayList<>();
        for (ActionListener<Response> listener : listeners) {
            try {
                listener.onFailure(failure);
            } catch (Exception ex) {
                exceptionList.add(ex);
            }
        }
        ExceptionsHelper.maybeThrowRuntimeAndSuppress(exceptionList);
    }

    static <Response> ActionListener<Response> runAfter(ActionListener<Response> delegate, Runnable runAfter) {
        return new ActionListener<Response>() {
            @Override
            public void onResponse(Response response) {
                try {
                    delegate.onResponse(response);
                } finally {
                    runAfter.run();
                }
            }

            @Override
            public void onFailure(Exception e) {
                try {
                    delegate.onFailure(e);
                } finally {
                    runAfter.run();
                }
            }
        };
    }

    static <Response> ActionListener<Response> runBefore(ActionListener<Response> delegate, CheckedRunnable<?> runBefore) {
        return new ActionListener<>() {
            @Override
            public void onResponse(Response response) {
                try {
                    runBefore.run();
                } catch (Exception ex) {
                    delegate.onFailure(ex);
                    return;
                }
                delegate.onResponse(response);
            }

            @Override
            public void onFailure(Exception e) {
                try {
                    runBefore.run();
                } catch (Exception ex) {
                    e.addSuppressed(ex);
                }
                delegate.onFailure(e);
            }
        };
    }

    static <Response> ActionListener<Response> notifyOnce(ActionListener<Response> delegate) {
        return new NotifyOnceListener<Response>() {
            @Override
            protected void innerOnResponse(Response response) {
                delegate.onResponse(response);
            }

            @Override
            protected void innerOnFailure(Exception e) {
                delegate.onFailure(e);
            }
        };
    }

    static <Response> void completeWith(ActionListener<Response> listener, CheckedSupplier<Response, ? extends Exception> supplier) {
        Response response;
        try {
            response = supplier.get();
        } catch (Exception e) {
            try {
                listener.onFailure(e);
            } catch (RuntimeException ex) {
                assert false : ex;
                throw ex;
            }
            return;
        }
        try {
            listener.onResponse(response);
        } catch (RuntimeException ex) {
            assert false : ex;
            throw ex;
        }
    }
}
