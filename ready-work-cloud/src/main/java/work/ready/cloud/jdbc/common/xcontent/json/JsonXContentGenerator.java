/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package work.ready.cloud.jdbc.common.xcontent.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.base.GeneratorBase;
import com.fasterxml.jackson.core.filter.FilteringGeneratorDelegate;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.core.json.JsonWriteContext;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;
import work.ready.cloud.jdbc.common.io.Streams;
import work.ready.cloud.jdbc.common.xcontent.DeprecationHandler;
import work.ready.cloud.jdbc.common.xcontent.NamedXContentRegistry;
import work.ready.cloud.jdbc.common.xcontent.XContent;
import work.ready.cloud.jdbc.common.xcontent.XContentFactory;
import work.ready.cloud.jdbc.common.xcontent.XContentGenerator;
import work.ready.cloud.jdbc.common.xcontent.XContentParser;
import work.ready.cloud.jdbc.common.xcontent.XContentType;
import work.ready.cloud.jdbc.common.xcontent.support.filtering.FilterPathBasedFilter;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Set;

public class JsonXContentGenerator implements XContentGenerator {

    protected final JsonGenerator generator;

    private final GeneratorBase base;

    private final FilteringGeneratorDelegate filter;

    private final OutputStream os;

    private boolean writeLineFeedAtEnd;
    private static final SerializedString LF = new SerializedString("\n");
    private static final DefaultPrettyPrinter.Indenter INDENTER = new DefaultIndenter("  ", LF.getValue());
    private boolean prettyPrint = false;

    public JsonXContentGenerator(JsonGenerator jsonGenerator, OutputStream os, Set<String> includes, Set<String> excludes) {
        Objects.requireNonNull(includes, "Including filters must not be null");
        Objects.requireNonNull(excludes, "Excluding filters must not be null");
        this.os = os;
        if (jsonGenerator instanceof GeneratorBase) {
            this.base = (GeneratorBase) jsonGenerator;
        } else {
            this.base = null;
        }

        JsonGenerator generator = jsonGenerator;

        boolean hasExcludes = excludes.isEmpty() == false;
        if (hasExcludes) {
            generator = new FilteringGeneratorDelegate(generator, new FilterPathBasedFilter(excludes, false), true, true);
        }

        boolean hasIncludes = includes.isEmpty() == false;
        if (hasIncludes) {
            generator = new FilteringGeneratorDelegate(generator, new FilterPathBasedFilter(includes, true), true, true);
        }

        if (hasExcludes || hasIncludes) {
            this.filter = (FilteringGeneratorDelegate) generator;
        } else {
            this.filter = null;
        }
        this.generator = generator;
    }

    @Override
    public XContentType contentType() {
        return XContentType.JSON;
    }

    @Override
    public final void usePrettyPrint() {
        generator.setPrettyPrinter(new DefaultPrettyPrinter().withObjectIndenter(INDENTER).withArrayIndenter(INDENTER));
        prettyPrint = true;
    }

    @Override
    public boolean isPrettyPrint() {
        return this.prettyPrint;
    }

    @Override
    public void usePrintLineFeedAtEnd() {
        writeLineFeedAtEnd = true;
    }

    private boolean isFiltered() {
        return filter != null;
    }

    private JsonGenerator getLowLevelGenerator() {
        if (isFiltered()) {
            JsonGenerator delegate = filter.getDelegate();
            if (delegate instanceof JsonGeneratorDelegate) {
                
                delegate = ((JsonGeneratorDelegate) delegate).getDelegate();
                assert delegate instanceof JsonGeneratorDelegate == false;
            }
            return delegate;
        }
        return generator;
    }

    private boolean inRoot() {
        JsonStreamContext context = generator.getOutputContext();
        return ((context != null) && (context.inRoot() && context.getCurrentName() == null));
    }

    @Override
    public void writeStartObject() throws IOException {
        if (inRoot()) {

            getLowLevelGenerator().writeStartObject();
            return;
        }
        generator.writeStartObject();
    }

    @Override
    public void writeEndObject() throws IOException {
        if (inRoot()) {

            getLowLevelGenerator().writeEndObject();
            return;
        }
        generator.writeEndObject();
    }

    @Override
    public void writeStartArray() throws IOException {
        generator.writeStartArray();
    }

    @Override
    public void writeEndArray() throws IOException {
        generator.writeEndArray();
    }

    @Override
    public void writeFieldName(String name) throws IOException {
        generator.writeFieldName(name);
    }

    @Override
    public void writeNull() throws IOException {
        generator.writeNull();
    }

    @Override
    public void writeNullField(String name) throws IOException {
        generator.writeNullField(name);
    }

    @Override
    public void writeBooleanField(String name, boolean value) throws IOException {
        generator.writeBooleanField(name, value);
    }

    @Override
    public void writeBoolean(boolean value) throws IOException {
        generator.writeBoolean(value);
    }

    @Override
    public void writeNumberField(String name, double value) throws IOException {
        generator.writeNumberField(name, value);
    }

    @Override
    public void writeNumber(double value) throws IOException {
        generator.writeNumber(value);
    }

    @Override
    public void writeNumberField(String name, float value) throws IOException {
        generator.writeNumberField(name, value);
    }

    @Override
    public void writeNumber(float value) throws IOException {
        generator.writeNumber(value);
    }

    @Override
    public void writeNumberField(String name, int value) throws IOException {
        generator.writeNumberField(name, value);
    }

    @Override
    public void writeNumberField(String name, BigInteger value) throws IOException {

        generator.writeFieldName(name);
        generator.writeNumber(value);
    }

    @Override
    public void writeNumberField(String name, BigDecimal value) throws IOException {
        generator.writeNumberField(name, value);
    }

    @Override
    public void writeNumber(int value) throws IOException {
        generator.writeNumber(value);
    }

    @Override
    public void writeNumberField(String name, long value) throws IOException {
        generator.writeNumberField(name, value);
    }

    @Override
    public void writeNumber(long value) throws IOException {
        generator.writeNumber(value);
    }

    @Override
    public void writeNumber(short value) throws IOException {
        generator.writeNumber(value);
    }

    @Override
    public void writeNumber(BigInteger value) throws IOException {
        generator.writeNumber(value);
    }

    @Override
    public void writeNumber(BigDecimal value) throws IOException {
        generator.writeNumber(value);
    }

    @Override
    public void writeStringField(String name, String value) throws IOException {
        generator.writeStringField(name, value);
    }

    @Override
    public void writeString(String value) throws IOException {
        generator.writeString(value);
    }

    @Override
    public void writeString(char[] value, int offset, int len) throws IOException {
        generator.writeString(value, offset, len);
    }

    @Override
    public void writeUTF8String(byte[] value, int offset, int length) throws IOException {
        generator.writeUTF8String(value, offset, length);
    }

    @Override
    public void writeBinaryField(String name, byte[] value) throws IOException {
        generator.writeBinaryField(name, value);
    }

    @Override
    public void writeBinary(byte[] value) throws IOException {
        generator.writeBinary(value);
    }

    @Override
    public void writeBinary(byte[] value, int offset, int len) throws IOException {
        generator.writeBinary(value, offset, len);
    }

    private void writeStartRaw(String name) throws IOException {
        writeFieldName(name);
        generator.writeRaw(':');
    }

    public void writeEndRaw() {
        assert base != null : "JsonGenerator should be of instance GeneratorBase but was: " + generator.getClass();
        if (base != null) {
            JsonStreamContext context = base.getOutputContext();
            assert (context instanceof JsonWriteContext) : "Expected an instance of JsonWriteContext but was: " + context.getClass();
            ((JsonWriteContext) context).writeValue();
        }
    }

    @Override
    public void writeRawField(String name, InputStream content) throws IOException {
        if (content.markSupported() == false) {
            
            content = new BufferedInputStream(content);
        }
        XContentType contentType = XContentFactory.xContentType(content);
        if (contentType == null) {
            throw new IllegalArgumentException("Can't write raw bytes whose xcontent-type can't be guessed");
        }
        writeRawField(name, content, contentType);
    }

    @Override
    public void writeRawField(String name, InputStream content, XContentType contentType) throws IOException {
        if (mayWriteRawData(contentType) == false) {
            
            try (XContentParser parser = XContentFactory.xContent(contentType)

                    .createParser(NamedXContentRegistry.EMPTY, DeprecationHandler.THROW_UNSUPPORTED_OPERATION, content)) {
                parser.nextToken();
                writeFieldName(name);
                copyCurrentStructure(parser);
            }
        } else {
            writeStartRaw(name);
            flush();
            Streams.copy(content, os);
            writeEndRaw();
        }
    }

    @Override
    public void writeRawValue(InputStream stream, XContentType xContentType) throws IOException {
        if (mayWriteRawData(xContentType) == false) {
            copyRawValue(stream, xContentType.xContent());
        } else {
            if (generator.getOutputContext().getCurrentName() != null) {
                
                generator.writeRaw(':');
            }
            flush();
            Streams.copy(stream, os, false);
            writeEndRaw();
        }
    }

    private boolean mayWriteRawData(XContentType contentType) {

        return supportsRawWrites()
                && isFiltered() == false
                && contentType == contentType()
                && prettyPrint == false;
    }

    protected boolean supportsRawWrites() {
        return true;
    }

    protected void copyRawValue(InputStream stream, XContent xContent) throws IOException {
        
        try (XContentParser parser = xContent

                 .createParser(NamedXContentRegistry.EMPTY, DeprecationHandler.THROW_UNSUPPORTED_OPERATION, stream)) {
            copyCurrentStructure(parser);
        }
    }

    @Override
    public void copyCurrentStructure(XContentParser parser) throws IOException {
        
        if (parser.currentToken() == null) {
            parser.nextToken();
        }
        if (parser instanceof JsonXContentParser) {
            generator.copyCurrentStructure(((JsonXContentParser) parser).parser);
        } else {
            copyCurrentStructure(this, parser);
        }
    }

    private static void copyCurrentStructure(XContentGenerator destination, XContentParser parser) throws IOException {
        XContentParser.Token token = parser.currentToken();

        if (token == XContentParser.Token.FIELD_NAME) {
            destination.writeFieldName(parser.currentName());
            token = parser.nextToken();
            
        }

        switch (token) {
            case START_ARRAY:
                destination.writeStartArray();
                while (parser.nextToken() != XContentParser.Token.END_ARRAY) {
                    copyCurrentStructure(destination, parser);
                }
                destination.writeEndArray();
                break;
            case START_OBJECT:
                destination.writeStartObject();
                while (parser.nextToken() != XContentParser.Token.END_OBJECT) {
                    copyCurrentStructure(destination, parser);
                }
                destination.writeEndObject();
                break;
            default: 
                destination.copyCurrentEvent(parser);
        }
    }

    @Override
    public void flush() throws IOException {
        generator.flush();
    }

    @Override
    public void close() throws IOException {
        if (generator.isClosed()) {
            return;
        }
        JsonStreamContext context = generator.getOutputContext();
        if ((context != null) && (context.inRoot() ==  false)) {
            throw new IOException("Unclosed object or array found");
        }
        if (writeLineFeedAtEnd) {
            flush();
            
            getLowLevelGenerator().writeRaw(LF);
        }
        generator.close();
    }

    @Override
    public boolean isClosed() {
        return generator.isClosed();
    }
}
