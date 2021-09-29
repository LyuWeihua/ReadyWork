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

import com.fasterxml.jackson.dataformat.cbor.CBORConstants;
import com.fasterxml.jackson.dataformat.smile.SmileConstants;
import work.ready.cloud.jdbc.common.xcontent.cbor.CborXContent;
import work.ready.cloud.jdbc.common.xcontent.json.JsonXContent;
import work.ready.cloud.jdbc.common.xcontent.smile.SmileXContent;
import work.ready.cloud.jdbc.common.xcontent.yaml.YamlXContent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class XContentFactory {

    static final int GUESS_HEADER_LENGTH = 20;

    public static XContentBuilder jsonBuilder() throws IOException {
        return contentBuilder(XContentType.JSON);
    }

    public static XContentBuilder jsonBuilder(OutputStream os) throws IOException {
        return new XContentBuilder(JsonXContent.jsonXContent, os);
    }

    public static XContentBuilder smileBuilder() throws IOException {
        return contentBuilder(XContentType.SMILE);
    }

    public static XContentBuilder smileBuilder(OutputStream os) throws IOException {
        return new XContentBuilder(SmileXContent.smileXContent, os);
    }

    public static XContentBuilder yamlBuilder() throws IOException {
        return contentBuilder(XContentType.YAML);
    }

    public static XContentBuilder yamlBuilder(OutputStream os) throws IOException {
        return new XContentBuilder(YamlXContent.yamlXContent, os);
    }

    public static XContentBuilder cborBuilder() throws IOException {
        return contentBuilder(XContentType.CBOR);
    }

    public static XContentBuilder cborBuilder(OutputStream os) throws IOException {
        return new XContentBuilder(CborXContent.cborXContent, os);
    }

    public static XContentBuilder contentBuilder(XContentType type, OutputStream outputStream) throws IOException {
        if (type == XContentType.JSON) {
            return jsonBuilder(outputStream);
        } else if (type == XContentType.SMILE) {
            return smileBuilder(outputStream);
        } else if (type == XContentType.YAML) {
            return yamlBuilder(outputStream);
        } else if (type == XContentType.CBOR) {
            return cborBuilder(outputStream);
        }
        throw new IllegalArgumentException("No matching content type for " + type);
    }

    public static XContentBuilder contentBuilder(XContentType type) throws IOException {
        if (type == XContentType.JSON) {
            return JsonXContent.contentBuilder();
        } else if (type == XContentType.SMILE) {
            return SmileXContent.contentBuilder();
        } else if (type == XContentType.YAML) {
            return YamlXContent.contentBuilder();
        } else if (type == XContentType.CBOR) {
            return CborXContent.contentBuilder();
        }
        throw new IllegalArgumentException("No matching content type for " + type);
    }

    public static XContent xContent(XContentType type) {
        if (type == null) {
            throw new IllegalArgumentException("Cannot get xcontent for unknown type");
        }
        return type.xContent();
    }

    @Deprecated
    public static XContentType xContentType(CharSequence content) {
        int length = content.length() < GUESS_HEADER_LENGTH ? content.length() : GUESS_HEADER_LENGTH;
        if (length == 0) {
            return null;
        }
        char first = content.charAt(0);
        if (first == '{') {
            return XContentType.JSON;
        }
        
        if (length > 2
                && first == SmileConstants.HEADER_BYTE_1
                && content.charAt(1) == SmileConstants.HEADER_BYTE_2
                && content.charAt(2) == SmileConstants.HEADER_BYTE_3) {
            return XContentType.SMILE;
        }
        if (length > 2 && first == '-' && content.charAt(1) == '-' && content.charAt(2) == '-') {
            return XContentType.YAML;
        }

        for (int i = 0; i < length; i++) {
            char c = content.charAt(i);
            if (c == '{') {
                return XContentType.JSON;
            }
            if (Character.isWhitespace(c) == false) {
                break;
            }
        }
        return null;
    }

    @Deprecated
    public static XContent xContent(CharSequence content) {
        XContentType type = xContentType(content);
        if (type == null) {
            throw new XContentParseException("Failed to derive xcontent");
        }
        return xContent(type);
    }

    @Deprecated
    public static XContent xContent(byte[] data) {
        return xContent(data, 0, data.length);
    }

    @Deprecated
    public static XContent xContent(byte[] data, int offset, int length) {
        XContentType type = xContentType(data, offset, length);
        if (type == null) {
            throw new XContentParseException("Failed to derive xcontent");
        }
        return xContent(type);
    }

    @Deprecated
    public static XContentType xContentType(InputStream si) throws IOException {
        
        if (si.markSupported() == false) {
            throw new IllegalArgumentException("Cannot guess the xcontent type without mark/reset support on " + si.getClass());
        }
        si.mark(Integer.MAX_VALUE);
        try {
            
            int current;
            do {
                current = si.read();
                if (current == -1) {
                    return null;
                }
            } while (Character.isWhitespace((char) current));
            
            final byte[] firstBytes = new byte[GUESS_HEADER_LENGTH];
            firstBytes[0] = (byte) current;
            int read = 1;
            while (read < GUESS_HEADER_LENGTH) {
                final int r = si.read(firstBytes, read, GUESS_HEADER_LENGTH - read);
                if (r == -1) {
                    break;
                }
                read += r;
            }
            return xContentType(firstBytes, 0, read);
        } finally {
            si.reset();
        }

    }

    @Deprecated
    public static XContentType xContentType(byte[] bytes) {
        return xContentType(bytes, 0, bytes.length);
    }

    @Deprecated
    public static XContentType xContentType(byte[] bytes, int offset, int length) {
        int totalLength = bytes.length;
        if (totalLength == 0 || length == 0) {
            return null;
        } else if ((offset + length) > totalLength) {
            return null;
        }
        byte first = bytes[offset];
        if (first == '{') {
            return XContentType.JSON;
        }
        if (length > 2
                && first == SmileConstants.HEADER_BYTE_1
                && bytes[offset + 1] == SmileConstants.HEADER_BYTE_2
                && bytes[offset + 2] == SmileConstants.HEADER_BYTE_3) {
            return XContentType.SMILE;
        }
        if (length > 2 && first == '-' && bytes[offset + 1] == '-' && bytes[offset + 2] == '-') {
            return XContentType.YAML;
        }
        
        if (first == CBORConstants.BYTE_OBJECT_INDEFINITE && length > 1) {
            return XContentType.CBOR;
        }
        if (CBORConstants.hasMajorType(CBORConstants.MAJOR_TYPE_TAG, first) && length > 2) {
            
            if (first == (byte) 0xD9 && bytes[offset + 1] == (byte) 0xD9 && bytes[offset + 2] == (byte) 0xF7) {
                return XContentType.CBOR;
            }
        }

        if (CBORConstants.hasMajorType(CBORConstants.MAJOR_TYPE_OBJECT, first)) {
            return XContentType.CBOR;
        }

        int jsonStart = 0;
        
        if (length > 3 && first == (byte) 0xEF && bytes[offset + 1] == (byte) 0xBB && bytes[offset + 2] == (byte) 0xBF) {
            jsonStart = 3;
        }

        for (int i = jsonStart; i < length; i++) {
            byte b = bytes[offset + i];
            if (b == '{') {
                return XContentType.JSON;
            }
            if (Character.isWhitespace(b) == false) {
                break;
            }
        }
        return null;
    }
}
