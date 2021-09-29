package work.ready.cloud.jdbc.common;

import com.fasterxml.jackson.core.JsonParseException;
import work.ready.cloud.jdbc.common.io.stream.StreamInput;
import work.ready.cloud.jdbc.common.io.stream.StreamOutput;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.service.status.Status;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class ExceptionsHelper {

    private static final Log logger = LogFactory.getLog(ExceptionsHelper.class);

    private static final String TOO_MANY_REQUESTS = "ERROR10999";
    private static final String ACCESS_DENIED = "ERROR10998";
    private static final String RUNTIME_EXCEPTION = "ERROR10010";
    private static final String GENERIC_EXCEPTION = "ERROR10014";

    public static RuntimeException convertToRuntime(Exception e) {
        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        }
        return new ElasticsearchException(e);
    }

    public static ElasticsearchException convertToElastic(Exception e) {
        if (e instanceof ElasticsearchException) {
            return (ElasticsearchException) e;
        }
        return new ElasticsearchException(e);
    }

    public static Status readStatusFrom(StreamInput in) throws IOException {
        return new Status(in.readString());
    }

    public static void writeStatusTo(StreamOutput out, Status status) throws IOException {
        out.writeString(status.getCode());
    }

    public static Status status(Throwable t) {
        if (t != null) {
            if (t instanceof ElasticsearchException) {
                return ((ElasticsearchException) t).status();
            } else if (t instanceof IllegalArgumentException) {
                return new Status(GENERIC_EXCEPTION);
            } else if (t instanceof JsonParseException) {
                return new Status(GENERIC_EXCEPTION);
            }
        }
        return new Status(RUNTIME_EXCEPTION);
    }

    public static Throwable unwrapCause(Throwable t) {
        int counter = 0;
        Throwable result = t;
        while (result instanceof ElasticsearchWrapperException) {
            if (result.getCause() == null) {
                return result;
            }
            if (result.getCause() == result) {
                return result;
            }
            if (counter++ > 10) {
                
                logger.warn("Exception cause unwrapping ran for 10 levels...", t);
                return result;
            }
            result = result.getCause();
        }
        return result;
    }

    public static String stackTrace(Throwable e) {
        StringWriter stackTraceStringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stackTraceStringWriter);
        e.printStackTrace(printWriter);
        return stackTraceStringWriter.toString();
    }

    public static String formatStackTrace(final StackTraceElement[] stackTrace) {
        return Arrays.stream(stackTrace).skip(1).map(e -> "\tat " + e).collect(Collectors.joining("\n"));
    }

    public static <T extends Throwable> void rethrowAndSuppress(List<T> exceptions) throws T {
        T main = null;
        for (T ex : exceptions) {
            main = useOrSuppress(main, ex);
        }
        if (main != null) {
            throw main;
        }
    }

    public static <T extends Throwable> void maybeThrowRuntimeAndSuppress(List<T> exceptions) {
        T main = null;
        for (T ex : exceptions) {
            main = useOrSuppress(main, ex);
        }
        if (main != null) {
            throw new ElasticsearchException(main);
        }
    }

    public static <T extends Throwable> T useOrSuppress(T first, T second) {
        if (first == null) {
            return second;
        } else {
            first.addSuppressed(second);
        }
        return first;
    }

    public static Throwable unwrap(Throwable t, Class<?>... clazzes) {
        if (t != null) {
            final Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            do {
                if (seen.add(t) == false) {
                    return null;
                }
                for (Class<?> clazz : clazzes) {
                    if (clazz.isInstance(t)) {
                        return t;
                    }
                }
            } while ((t = t.getCause()) != null);
        }
        return null;
    }

    public static boolean reThrowIfNotNull(Throwable e) {
        if (e != null) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Throwable> Optional<T> unwrapCausesAndSuppressed(Throwable cause, Predicate<Throwable> predicate) {
        if (predicate.test(cause)) {
            return Optional.of((T) cause);
        }

        final Queue<Throwable> queue = new LinkedList<>();
        queue.add(cause);
        final Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        while (queue.isEmpty() == false) {
            final Throwable current = queue.remove();
            if (seen.add(current) == false) {
                continue;
            }
            if (predicate.test(current)) {
                return Optional.of((T) current);
            }
            Collections.addAll(queue, current.getSuppressed());
            if (current.getCause() != null) {
                queue.add(current.getCause());
            }
        }
        return Optional.empty();
    }

    public static Optional<Error> maybeError(final Throwable cause) {
        return unwrapCausesAndSuppressed(cause, t -> t instanceof Error);
    }

    public static void maybeDieOnAnotherThread(final Throwable throwable) {
        ExceptionsHelper.maybeError(throwable).ifPresent(error -> {
            
            try {
                
                final String formatted = ExceptionsHelper.formatStackTrace(Thread.currentThread().getStackTrace());
                logger.error("fatal error\n{}", formatted);
            } finally {
                new Thread(
                        () -> {
                            throw error;
                        })
                        .start();
            }
        });
    }

}
