package work.ready.cloud.jdbc.common;

import work.ready.cloud.Version;
import work.ready.cloud.jdbc.common.io.stream.StreamInput;
import work.ready.cloud.jdbc.common.io.stream.StreamOutput;
import work.ready.cloud.jdbc.common.io.stream.Writeable;
import work.ready.core.service.status.Status;
import work.ready.core.tools.define.BiTuple;
import work.ready.core.tools.define.CheckedFunction;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableMap;

public class ElasticsearchException extends RuntimeException implements Writeable {

    private static final Version UNKNOWN_VERSION_ADDED = Version.unknown();

    public static final boolean REST_EXCEPTION_SKIP_STACK_TRACE_DEFAULT = true;
    private static final boolean REST_EXCEPTION_SKIP_CAUSE_DEFAULT = false;
    private static final String CLUSTER_METADATA_KEY = "ready.cluster";
    private static final String NODE_METADATA_KEY = "ready.node";
    private static final String RESOURCE_METADATA_TYPE_KEY = "ready.resource.type";
    private static final String RESOURCE_METADATA_ID_KEY = "ready.resource.id";

    private static final String TYPE = "type";
    private static final String REASON = "reason";
    private static final String CAUSED_BY = "caused_by";
    public static final String STACK_TRACE = "stack_trace";
    private static final String HEADER = "header";
    private static final String ERROR = "error";
    private static final String ROOT_CAUSE = "root_cause";

    private static final String RUNTIME_EXCEPTION = "ERROR10010";

    private static final Map<Integer, CheckedFunction<StreamInput, ? extends ElasticsearchException, IOException>> ID_TO_SUPPLIER;
    private static final Map<Class<? extends ElasticsearchException>, ElasticsearchExceptionHandle> CLASS_TO_ELASTICSEARCH_EXCEPTION_HANDLE;
    private final Map<String, List<String>> metadata = new HashMap<>();
    private final Map<String, List<String>> headers = new HashMap<>();

    public ElasticsearchException(Throwable cause) {
        super(cause);
    }

    public ElasticsearchException(String msg, Object... args) {
        super(String.format(msg, args));
    }

    public ElasticsearchException(String msg, Throwable cause, Object... args) {
        super(String.format(msg, args), cause);
    }

    public ElasticsearchException(StreamInput in) throws IOException {
        super(in.readOptionalString(), in.readException());
        readStackTrace(this, in);
        headers.putAll(in.readMapOfLists(StreamInput::readString, StreamInput::readString));
        metadata.putAll(in.readMapOfLists(StreamInput::readString, StreamInput::readString));
    }

    public void addMetadata(String key, String... values) {
        addMetadata(key, Arrays.asList(values));
    }

    public void addMetadata(String key, List<String> values) {
        
        if (key.startsWith("ready.") == false) {
            throw new IllegalArgumentException("exception metadata must start with [es.], found [" + key + "] instead");
        }
        this.metadata.put(key, values);
    }

    public Set<String> getMetadataKeys() {
        return metadata.keySet();
    }

    public List<String> getMetadata(String key) {
        return metadata.get(key);
    }

    protected Map<String, List<String>> getMetadata() {
        return metadata;
    }

    public void addHeader(String key, List<String> value) {
        
        if (key.startsWith("ready.")) {
            throw new IllegalArgumentException("exception headers must not start with [es.], found [" + key + "] instead");
        }
        this.headers.put(key, value);
    }

    public void addHeader(String key, String... value) {
        addHeader(key, Arrays.asList(value));
    }

    public Set<String> getHeaderKeys() {
        return headers.keySet();
    }

    public List<String> getHeader(String key) {
        return headers.get(key);
    }

    protected Map<String, List<String>> getHeaders() {
        return headers;
    }

    public Status status() {
        Throwable cause = unwrapCause();
        if (cause == this) {
            return new Status(RUNTIME_EXCEPTION);
        } else {
            return ExceptionsHelper.status(cause);
        }
    }

    public Throwable unwrapCause() {
        return ExceptionsHelper.unwrapCause(this);
    }

    public String getDetailedMessage() {
        if (getCause() != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(toString()).append("; ");
            if (getCause() instanceof ElasticsearchException) {
                sb.append(((ElasticsearchException) getCause()).getDetailedMessage());
            } else {
                sb.append(getCause());
            }
            return sb.toString();
        } else {
            return super.toString();
        }
    }

    public Throwable getRootCause() {
        Throwable rootCause = this;
        Throwable cause = getCause();
        while (cause != null && cause != rootCause) {
            rootCause = cause;
            cause = cause.getCause();
        }
        return rootCause;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeOptionalString(this.getMessage());
        out.writeException(this.getCause());
        writeStackTraces(this, out, StreamOutput::writeException);
        out.writeMapOfLists(headers, StreamOutput::writeString, StreamOutput::writeString);
        out.writeMapOfLists(metadata, StreamOutput::writeString, StreamOutput::writeString);
    }

    public static ElasticsearchException readException(StreamInput input, int id) throws IOException {
        CheckedFunction<StreamInput, ? extends ElasticsearchException, IOException> elasticsearchException = ID_TO_SUPPLIER.get(id);
        if (elasticsearchException == null) {
            throw new IllegalStateException("unknown exception for id: " + id);
        }
        return elasticsearchException.apply(input);
    }

    public static boolean isRegistered(Class<? extends Throwable> exception, Version version) {
        ElasticsearchExceptionHandle elasticsearchExceptionHandle = CLASS_TO_ELASTICSEARCH_EXCEPTION_HANDLE.get(exception);
        if (elasticsearchExceptionHandle != null) {
            return version.onOrAfter(elasticsearchExceptionHandle.versionAdded);
        }
        return false;
    }

    static Set<Class<? extends ElasticsearchException>> getRegisteredKeys() { 
        return CLASS_TO_ELASTICSEARCH_EXCEPTION_HANDLE.keySet();
    }

    public static int getId(Class<? extends ElasticsearchException> exception) {
        return CLASS_TO_ELASTICSEARCH_EXCEPTION_HANDLE.get(exception).id;
    }

    public ElasticsearchException[] guessRootCauses() {
        final Throwable cause = getCause();
        if (cause != null && cause instanceof ElasticsearchException) {
            return ((ElasticsearchException) cause).guessRootCauses();
        }
        return new ElasticsearchException[]{this};
    }

    public static ElasticsearchException[] guessRootCauses(Throwable t) {
        Throwable ex = ExceptionsHelper.unwrapCause(t);
        if (ex instanceof ElasticsearchException) {
            
            return ((ElasticsearchException) ex).guessRootCauses();
        }
        return new ElasticsearchException[]{new ElasticsearchException(ex.getMessage(), ex) {
            @Override
            protected String getExceptionName() {
                return getExceptionName(getCause());
            }
        }};
    }

    protected String getExceptionName() {
        return getExceptionName(this);
    }

    public static String getExceptionName(Throwable ex) {
        String simpleName = ex.getClass().getSimpleName();
        if (simpleName.startsWith("Elasticsearch")) {
            simpleName = simpleName.substring("Elasticsearch".length());
        }
        
        return toUnderscoreCase(simpleName);
    }

    static String buildMessage(String type, String reason, String stack) {
        StringBuilder message = new StringBuilder("Elasticsearch exception [");
        message.append(TYPE).append('=').append(type).append(", ");
        message.append(REASON).append('=').append(reason);
        if (stack != null) {
            message.append(", ").append(STACK_TRACE).append('=').append(stack);
        }
        message.append(']');
        return message.toString();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (metadata.containsKey(CLUSTER_METADATA_KEY)) {
            builder.append(getCluster());
            if (metadata.containsKey(NODE_METADATA_KEY)) {
                builder.append('[').append(getNodeId()).append(']');
            }
            builder.append(' ');
        }
        return builder.append(super.toString().trim()).toString();
    }

    public String getCluster() {
        List<String> cluster = getMetadata(CLUSTER_METADATA_KEY);
        return cluster != null ? cluster.get(0) : null;
    }

    public String getNodeId() {
        List<String> nodeId = getMetadata(NODE_METADATA_KEY);
        return nodeId != null ? nodeId.get(0) : null;
    }

    public void setCluster(String cluster) {
        if (cluster != null) {
            addMetadata(CLUSTER_METADATA_KEY, cluster);
        }
    }

    public void setNodeId(String nodeId) {
        if (nodeId != null) {
            addMetadata(NODE_METADATA_KEY, nodeId);
        }
    }

    public static <T extends Throwable> T readStackTrace(T throwable, StreamInput in) throws IOException {
        throwable.setStackTrace(in.readArray(i -> {
            final String declaringClasss = i.readString();
            final String fileName = i.readOptionalString();
            final String methodName = i.readString();
            final int lineNumber = i.readVInt();
            return new StackTraceElement(declaringClasss, methodName, fileName, lineNumber);
        }, StackTraceElement[]::new));

        int numSuppressed = in.readVInt();
        for (int i = 0; i < numSuppressed; i++) {
            throwable.addSuppressed(in.readException());
        }
        return throwable;
    }

    public static <T extends Throwable> T writeStackTraces(T throwable, StreamOutput out,
                                                           Writer<Throwable> exceptionWriter) throws IOException {
        out.writeArray((o, v) -> {
            o.writeString(v.getClassName());
            o.writeOptionalString(v.getFileName());
            o.writeString(v.getMethodName());
            o.writeVInt(v.getLineNumber());
        }, throwable.getStackTrace());
        out.writeArray(exceptionWriter, throwable.getSuppressed());
        return throwable;
    }

    private enum ElasticsearchExceptionHandle {
        TEST_EXCEPTION(
                ElasticsearchException.class,
                ElasticsearchException::new,
                1,
                Version.CURRENT);

        final Class<? extends ElasticsearchException> exceptionClass;
        final CheckedFunction<StreamInput, ? extends ElasticsearchException, IOException> constructor;
        final int id;
        final Version versionAdded;

        <E extends ElasticsearchException> ElasticsearchExceptionHandle(Class<E> exceptionClass,
                                                                        CheckedFunction<StreamInput, E, IOException> constructor, int id,
                                                                        Version versionAdded) {
            
            this.exceptionClass = exceptionClass;
            this.constructor = constructor;
            this.versionAdded = versionAdded;
            this.id = id;
        }
    }

    static int[] ids() {
        return Arrays.stream(ElasticsearchExceptionHandle.values()).mapToInt(h -> h.id).toArray();
    }

    static BiTuple<Integer, Class<? extends ElasticsearchException>>[] classes() {
        @SuppressWarnings("unchecked")
        final BiTuple<Integer, Class<? extends ElasticsearchException>>[] ts =
                Arrays.stream(ElasticsearchExceptionHandle.values())
                        .map(h -> new BiTuple<>(h.id, h.exceptionClass)).toArray(BiTuple[]::new);
        return ts;
    }

    static {
        ID_TO_SUPPLIER = unmodifiableMap(Arrays
                .stream(ElasticsearchExceptionHandle.values()).collect(Collectors.toMap(e -> e.id, e -> e.constructor)));
        CLASS_TO_ELASTICSEARCH_EXCEPTION_HANDLE = unmodifiableMap(Arrays
                .stream(ElasticsearchExceptionHandle.values()).collect(Collectors.toMap(e -> e.exceptionClass, e -> e)));
    }

    public void setResources(String type, String... id) {
        assert type != null;
        addMetadata(RESOURCE_METADATA_ID_KEY, id);
        addMetadata(RESOURCE_METADATA_TYPE_KEY, type);
    }

    public List<String> getResourceId() {
        return getMetadata(RESOURCE_METADATA_ID_KEY);
    }

    public String getResourceType() {
        List<String> header = getMetadata(RESOURCE_METADATA_TYPE_KEY);
        if (header != null && header.isEmpty() == false) {
            assert header.size() == 1;
            return header.get(0);
        }
        return null;
    }

    private static String toUnderscoreCase(String value) {
        StringBuilder sb = new StringBuilder();
        boolean changed = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isUpperCase(c)) {
                if (!changed) {
                    
                    for (int j = 0; j < i; j++) {
                        sb.append(value.charAt(j));
                    }
                    changed = true;
                    if (i == 0) {
                        sb.append(Character.toLowerCase(c));
                    } else {
                        sb.append('_');
                        sb.append(Character.toLowerCase(c));
                    }
                } else {
                    sb.append('_');
                    sb.append(Character.toLowerCase(c));
                }
            } else {
                if (changed) {
                    sb.append(c);
                }
            }
        }
        if (!changed) {
            return value;
        }
        return sb.toString();
    }

}
