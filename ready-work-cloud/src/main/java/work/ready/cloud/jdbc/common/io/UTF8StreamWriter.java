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

package work.ready.cloud.jdbc.common.io;

import java.io.CharConversionException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

public final class UTF8StreamWriter extends Writer {

    private OutputStream _outputStream;

    private final byte[] _bytes;

    private int _index;

    public UTF8StreamWriter() {
        _bytes = new byte[2048];
    }

    public UTF8StreamWriter(int capacity) {
        _bytes = new byte[capacity];
    }

    public UTF8StreamWriter setOutput(OutputStream out) {
        if (_outputStream != null)
            throw new IllegalStateException("Writer not closed or reset");
        _outputStream = out;
        return this;
    }

    public void write(char c) throws IOException {
        if ((c < 0xd800) || (c > 0xdfff)) {
            write((int) c);
        } else if (c < 0xdc00) { 
            _highSurrogate = c;
        } else { 
            int code = ((_highSurrogate - 0xd800) << 10) + (c - 0xdc00)
                    + 0x10000;
            write(code);
        }
    }

    private char _highSurrogate;

    @Override
    public void write(int code) throws IOException {
        if ((code & 0xffffff80) == 0) {
            _bytes[_index] = (byte) code;
            if (++_index >= _bytes.length) {
                flushBuffer();
            }
        } else { 
            write2(code);
        }
    }

    private void write2(int c) throws IOException {
        if ((c & 0xfffff800) == 0) { 
            _bytes[_index] = (byte) (0xc0 | (c >> 6));
            if (++_index >= _bytes.length) {
                flushBuffer();
            }
            _bytes[_index] = (byte) (0x80 | (c & 0x3f));
            if (++_index >= _bytes.length) {
                flushBuffer();
            }
        } else if ((c & 0xffff0000) == 0) { 
            _bytes[_index] = (byte) (0xe0 | (c >> 12));
            if (++_index >= _bytes.length) {
                flushBuffer();
            }
            _bytes[_index] = (byte) (0x80 | ((c >> 6) & 0x3f));
            if (++_index >= _bytes.length) {
                flushBuffer();
            }
            _bytes[_index] = (byte) (0x80 | (c & 0x3f));
            if (++_index >= _bytes.length) {
                flushBuffer();
            }
        } else if ((c & 0xff200000) == 0) { 
            _bytes[_index] = (byte) (0xf0 | (c >> 18));
            if (++_index >= _bytes.length) {
                flushBuffer();
            }
            _bytes[_index] = (byte) (0x80 | ((c >> 12) & 0x3f));
            if (++_index >= _bytes.length) {
                flushBuffer();
            }
            _bytes[_index] = (byte) (0x80 | ((c >> 6) & 0x3f));
            if (++_index >= _bytes.length) {
                flushBuffer();
            }
            _bytes[_index] = (byte) (0x80 | (c & 0x3f));
            if (++_index >= _bytes.length) {
                flushBuffer();
            }
        } else if ((c & 0xf4000000) == 0) { 
            _bytes[_index] = (byte) (0xf8 | (c >> 24));
            if (++_index >= _bytes.length) {
                flushBuffer();
            }
            _bytes[_index] = (byte) (0x80 | ((c >> 18) & 0x3f));
            if (++_index >= _bytes.length) {
                flushBuffer();
            }
            _bytes[_index] = (byte) (0x80 | ((c >> 12) & 0x3f));
            if (++_index >= _bytes.length) {
                flushBuffer();
            }
            _bytes[_index] = (byte) (0x80 | ((c >> 6) & 0x3f));
            if (++_index >= _bytes.length) {
                flushBuffer();
            }
            _bytes[_index] = (byte) (0x80 | (c & 0x3f));
            if (++_index >= _bytes.length) {
                flushBuffer();
            }
        } else if ((c & 0x80000000) == 0) { 
            _bytes[_index] = (byte) (0xfc | (c >> 30));
            if (++_index >= _bytes.length) {
                flushBuffer();
            }
            _bytes[_index] = (byte) (0x80 | ((c >> 24) & 0x3f));
            if (++_index >= _bytes.length) {
                flushBuffer();
            }
            _bytes[_index] = (byte) (0x80 | ((c >> 18) & 0x3f));
            if (++_index >= _bytes.length) {
                flushBuffer();
            }
            _bytes[_index] = (byte) (0x80 | ((c >> 12) & 0x3F));
            if (++_index >= _bytes.length) {
                flushBuffer();
            }
            _bytes[_index] = (byte) (0x80 | ((c >> 6) & 0x3F));
            if (++_index >= _bytes.length) {
                flushBuffer();
            }
            _bytes[_index] = (byte) (0x80 | (c & 0x3F));
            if (++_index >= _bytes.length) {
                flushBuffer();
            }
        } else {
            throw new CharConversionException("Illegal character U+"
                    + Integer.toHexString(c));
        }
    }

    @Override
    public void write(char cbuf[], int off, int len) throws IOException {
        final int off_plus_len = off + len;
        for (int i = off; i < off_plus_len; ) {
            char c = cbuf[i++];
            if (c < 0x80) {
                _bytes[_index] = (byte) c;
                if (++_index >= _bytes.length) {
                    flushBuffer();
                }
            } else {
                write(c);
            }
        }
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        final int off_plus_len = off + len;
        for (int i = off; i < off_plus_len; ) {
            char c = str.charAt(i++);
            if (c < 0x80) {
                _bytes[_index] = (byte) c;
                if (++_index >= _bytes.length) {
                    flushBuffer();
                }
            } else {
                write(c);
            }
        }
    }

    public void write(CharSequence csq) throws IOException {
        final int length = csq.length();
        for (int i = 0; i < length; ) {
            char c = csq.charAt(i++);
            if (c < 0x80) {
                _bytes[_index] = (byte) c;
                if (++_index >= _bytes.length) {
                    flushBuffer();
                }
            } else {
                write(c);
            }
        }
    }

    @Override
    public void flush() throws IOException {
        flushBuffer();
        _outputStream.flush();
    }

    @Override
    public void close() throws IOException {
        if (_outputStream != null) {
            flushBuffer();
            _outputStream.close();
            reset();
        }
    }

    private void flushBuffer() throws IOException {
        if (_outputStream == null)
            throw new IOException("Stream closed");
        _outputStream.write(_bytes, 0, _index);
        _index = 0;
    }

    public void reset() {
        _highSurrogate = 0;
        _index = 0;
        _outputStream = null;
    }
}
