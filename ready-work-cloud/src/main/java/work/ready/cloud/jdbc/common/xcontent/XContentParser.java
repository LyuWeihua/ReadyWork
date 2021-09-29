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

package work.ready.cloud.jdbc.common.xcontent;

import work.ready.core.tools.define.CheckedFunction;

import java.io.Closeable;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public interface XContentParser extends Closeable {

    enum Token {
        START_OBJECT {
            @Override
            public boolean isValue() {
                return false;
            }
        },

        END_OBJECT {
            @Override
            public boolean isValue() {
                return false;
            }
        },

        START_ARRAY {
            @Override
            public boolean isValue() {
                return false;
            }
        },

        END_ARRAY {
            @Override
            public boolean isValue() {
                return false;
            }
        },

        FIELD_NAME {
            @Override
            public boolean isValue() {
                return false;
            }
        },

        VALUE_STRING {
            @Override
            public boolean isValue() {
                return true;
            }
        },

        VALUE_NUMBER {
            @Override
            public boolean isValue() {
                return true;
            }
        },

        VALUE_BOOLEAN {
            @Override
            public boolean isValue() {
                return true;
            }
        },

        VALUE_EMBEDDED_OBJECT {
            @Override
            public boolean isValue() {
                return true;
            }
        },

        VALUE_NULL {
            @Override
            public boolean isValue() {
                return false;
            }
        };

        public abstract boolean isValue();
    }

    enum NumberType {
        INT, BIG_INTEGER, LONG, FLOAT, DOUBLE, BIG_DECIMAL
    }

    XContentType contentType();

    Token nextToken() throws IOException;

    void skipChildren() throws IOException;

    Token currentToken();

    String currentName() throws IOException;

    Map<String, Object> map() throws IOException;

    Map<String, Object> mapOrdered() throws IOException;

    Map<String, String> mapStrings() throws IOException;

    <T> Map<String, T> map(
            Supplier<Map<String, T>> mapFactory, CheckedFunction<XContentParser, T, IOException> mapValueParser) throws IOException;

    List<Object> list() throws IOException;

    List<Object> listOrderedMap() throws IOException;

    String text() throws IOException;

    String textOrNull() throws IOException;

    CharBuffer charBufferOrNull() throws IOException;

    CharBuffer charBuffer() throws IOException;

    Object objectText() throws IOException;

    Object objectBytes() throws IOException;

    boolean hasTextCharacters();

    char[] textCharacters() throws IOException;

    int textLength() throws IOException;

    int textOffset() throws IOException;

    Number numberValue() throws IOException;

    NumberType numberType() throws IOException;

    short shortValue(boolean coerce) throws IOException;

    int intValue(boolean coerce) throws IOException;

    long longValue(boolean coerce) throws IOException;

    float floatValue(boolean coerce) throws IOException;

    double doubleValue(boolean coerce) throws IOException;

    short shortValue() throws IOException;

    int intValue() throws IOException;

    long longValue() throws IOException;

    float floatValue() throws IOException;

    double doubleValue() throws IOException;

    boolean isBooleanValue() throws IOException;

    boolean booleanValue() throws IOException;

    byte[] binaryValue() throws IOException;

    XContentLocation getTokenLocation();

    <T> T namedObject(Class<T> categoryClass, String name, Object context) throws IOException;

    NamedXContentRegistry getXContentRegistry();

    boolean isClosed();

    DeprecationHandler getDeprecationHandler();
}
