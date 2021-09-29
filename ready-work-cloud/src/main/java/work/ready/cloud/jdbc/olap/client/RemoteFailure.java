/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package work.ready.cloud.jdbc.olap.client;

import com.fasterxml.jackson.core.*;
import work.ready.core.tools.StrUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static java.util.Collections.*;

public class RemoteFailure {
    
    static final int MAX_RAW_RESPONSE = 512 * 1024;

    private static final JsonFactory JSON_FACTORY = new JsonFactory();
    static {
        
        JSON_FACTORY.configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, true);
        JSON_FACTORY.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        JSON_FACTORY.configure(JsonFactory.Feature.FAIL_ON_SYMBOL_HASH_OVERFLOW, true);
        
        JSON_FACTORY.configure(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT, false);
        JSON_FACTORY.configure(JsonParser.Feature.STRICT_DUPLICATE_DETECTION, false);
        
        JSON_FACTORY.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
    }

    public static RemoteFailure parseFromResponse(InputStream stream) throws IOException {
        
        stream = new BufferedInputStream(stream);
        stream.mark(MAX_RAW_RESPONSE);
        JsonParser parser = null;
        try {
            parser = JSON_FACTORY.createParser(stream);
            return parseResponseTopLevel(parser);
        } catch (JsonParseException e) {
            throw new IOException(parseErrorMessage(e.getOriginalMessage(), stream, parser), e);
        } catch (IOException e) {
            throw new IOException(parseErrorMessage(e.getMessage(), stream, parser), e);
        } finally {
            if (parser != null) {
                parser.close();
            }
        }
    }

    private final String type;
    private final String reason;
    private final String remoteTrace;
    private final Map<String, String> headers;
    private final Map<String, List<String>> metadata;
    private final RemoteFailure cause;

    RemoteFailure(String type,
                  String reason,
                  String remoteTrace,
                  Map<String, String> headers,
                  Map<String, List<String>> metadata,
                  RemoteFailure cause) {
        this.type = type;
        this.reason = reason;
        this.remoteTrace = remoteTrace;
        this.headers = headers;
        this.metadata = metadata;
        this.cause = cause;
    }

    public String type() {
        return type;
    }

    public String reason() {
        return reason;
    }

    public String remoteTrace() {
        return remoteTrace;
    }

    public Map<String, String> headers() {
        return headers;
    }

    public Map<String, List<String>> metadata() {
        return metadata;
    }

    public RemoteFailure cause() {
        return cause;
    }

    private static RemoteFailure parseResponseTopLevel(JsonParser parser) throws IOException {
        RemoteFailure exception = null;

        JsonToken token = parser.nextToken();
        if (token != JsonToken.START_OBJECT) {
            throw new IllegalArgumentException("Expected error to start with [START_OBJECT] but started with [" + token
                    + "][" + parser.getText() + "]");
        }
        String fieldName = null;
        while ((token = parser.nextToken()) != JsonToken.END_OBJECT) {
            if (token == JsonToken.FIELD_NAME) {
                fieldName = parser.getCurrentName();
            } else {
                switch (fieldName) {
                case "error":
                    if (token != JsonToken.START_OBJECT && token != JsonToken.VALUE_STRING) {
                        throw new IOException("Expected [error] to be an object or string but was [" + token + "]["
                                + parser.getText() + "]");
                    }
                    if (token == JsonToken.VALUE_STRING) {
                        exception = new RemoteFailure(StrUtil.EMPTY, parser.getText(), null, null, null, null);
                    } else {
                        exception = parseFailure(parser);
                    }
                    continue;
                case "status":
                    if (token != JsonToken.VALUE_NUMBER_INT) {
                        throw new IOException("Expected [status] to be a string but was [" + token + "][" + parser.getText() + "]");
                    }
                    
                    continue;
                default:
                    throw new IOException("Expected one of [error, status] but got [" + fieldName + "][" + parser.getText() + "]");
                }
            }
        }
        if (exception == null) {
            throw new IOException("Expected [error] but didn't see it.");
        }
        return exception;
    }

    private static RemoteFailure parseFailure(JsonParser parser) throws IOException {
        String type = null;
        String reason = null;
        String remoteTrace = null;
        Map<String, String> headers = emptyMap();
        RemoteFailure cause = null;
        final Map<String, List<String>> metadata = new LinkedHashMap<>();

        JsonToken token;
        String fieldName = null;
        while ((token = parser.nextToken()) != JsonToken.END_OBJECT) {
            if (token == JsonToken.FIELD_NAME) {
                fieldName = parser.getCurrentName();
            } else {
                switch (fieldName) {
                case "caused_by":
                    if (token != JsonToken.START_OBJECT) {
                        throw new IOException("Expected [caused_by] to be an object but was [" + token + "][" + parser.getText() + "]");
                    }
                    cause = parseFailure(parser);
                    break;
                case "header":
                    if (token != JsonToken.START_OBJECT) {
                        throw new IOException("Expected [header] to be an object but was [" + token + "][" + parser.getText() + "]");
                    }
                    headers = parseHeaders(parser);
                    break;
                case "reason":
                    switch (token) {
                    case VALUE_STRING:
                        reason = parser.getText();
                        break;
                    case VALUE_NULL:
                        break;
                    default:
                        throw new IOException("Expected [reason] to be a string but was [" + token + "][" + parser.getText() + "]");
                    }
                    break;
                case "root_cause":
                    if (token != JsonToken.START_ARRAY) {
                        throw new IOException("Expected [root_cause] to be an array but was [" + token + "][" + parser.getText() + "]");
                    }
                    parser.skipChildren();   
                    break;
                case "stack_trace":
                    if (token != JsonToken.VALUE_STRING) {
                        throw new IOException("Expected [stack_trace] to be a string but was [" + token + "][" + parser.getText() + "]");
                    }
                    remoteTrace = parser.getText();
                    break;
                case "type":
                    if (token != JsonToken.VALUE_STRING) {
                        throw new IOException("Expected [type] to be a string but was [" + token + "][" + parser.getText() + "]");
                    }
                    type = parser.getText();
                    break;
                default:
                    metadata.putAll(parseMetadata(parser));
                }
            }
        }
        if (type == null) {
            throw new IOException("expected [type] but didn't see it");
        }
        if (remoteTrace == null) {
            throw new IOException("expected [stack_trace] cannot but didn't see it");
        }
        return new RemoteFailure(type, reason, remoteTrace, headers, metadata, cause);
    }

    private static Map<String, String> parseHeaders(JsonParser parser) throws IOException {
        Map<String, String> headers = new HashMap<>();

        JsonToken token;
        while ((token = parser.nextToken()) != JsonToken.END_OBJECT) {
            if (token != JsonToken.FIELD_NAME) {
                throw new IOException("expected header name but was [" + token + "][" + parser.getText() + "]");
            }
            String name = parser.getText();
            token = parser.nextToken();
            if (token != JsonToken.VALUE_STRING) {
                throw new IOException("expected header value but was [" + token + "][" + parser.getText() + "]");
            }
            String value = parser.getText();
            headers.put(name, value);
        }

        return headers;
    }

    private static Map<String, List<String>> parseMetadata(final JsonParser parser) throws IOException {
        final Map<String, List<String>> metadata = new HashMap<>();
        final String currentFieldName = parser.getCurrentName();

        JsonToken token = parser.currentToken();
        if (token == JsonToken.VALUE_STRING) {
            metadata.put(currentFieldName, singletonList(parser.getText()));

        } else if (token == JsonToken.START_ARRAY) {

            final List<String> values = new ArrayList<>();
            while ((token = parser.nextToken()) != JsonToken.END_ARRAY) {
                if (token ==JsonToken.VALUE_STRING) {
                    values.add(parser.getText());
                } else {
                    parser.skipChildren();
                }
            }
            if (values.size() > 0) {
                if (metadata.containsKey(currentFieldName)) {
                    values.addAll(metadata.get(currentFieldName));
                }
                metadata.put(currentFieldName, unmodifiableList(values));
            }

        } else {

            parser.skipChildren();
        }
        return unmodifiableMap(metadata);
    }

    private static String parseErrorMessage(String message, InputStream stream, JsonParser parser) {
        String responseMessage;
        try {
            try {
                stream.reset();
            } catch (IOException e) {
                
                throw new IOException("Response too large", e);
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                StringBuilder builder = new StringBuilder();
                builder.append("Response:\n");
                char[] buf = new char[512];
                int read;
                while ((read = reader.read(buf)) != -1) {
                    builder.append(buf, 0, read);
                }
                responseMessage = builder.toString();
            }
        } catch (IOException replayException) {
            responseMessage = "Attempted to include response but failed because [" + replayException.getMessage() + "].";
        }
        String parserLocation = "";
        if (parser != null) {
            parserLocation = " at [line " + parser.getTokenLocation().getLineNr()
                    + " col " + parser.getTokenLocation().getColumnNr() + "]";
        }
        return "Can't parse error from Elasticsearch [" + message + "]" + parserLocation + ". "  + responseMessage;
    }
}
