/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package work.ready.core.tools.javapoet;

import java.io.IOException;

import static work.ready.core.tools.javapoet.Util.checkNotNull;

final class LineWrapper {
  private final RecordingAppendable out;
  private final String indent;
  private final int columnLimit;
  private boolean closed;

  private final StringBuilder buffer = new StringBuilder();

  private int column = 0;

  private int indentLevel = -1;

  private FlushType nextFlush;

  LineWrapper(Appendable out, String indent, int columnLimit) {
    checkNotNull(out, "out == null");
    this.out = new RecordingAppendable(out);
    this.indent = indent;
    this.columnLimit = columnLimit;
  }

  char lastChar() {
    return out.lastChar;
  }

  void append(String s) throws IOException {
    if (closed) throw new IllegalStateException("closed");

    if (nextFlush != null) {
      int nextNewline = s.indexOf('\n');

      if (nextNewline == -1 && column + s.length() <= columnLimit) {
        buffer.append(s);
        column += s.length();
        return;
      }

      boolean wrap = nextNewline == -1 || column + nextNewline > columnLimit;
      flush(wrap ? FlushType.WRAP : nextFlush);
    }

    out.append(s);
    int lastNewline = s.lastIndexOf('\n');
    column = lastNewline != -1
        ? s.length() - lastNewline - 1
        : column + s.length();
  }

  void wrappingSpace(int indentLevel) throws IOException {
    if (closed) throw new IllegalStateException("closed");

    if (this.nextFlush != null) flush(nextFlush);
    column++; 
    this.nextFlush = FlushType.SPACE;
    this.indentLevel = indentLevel;
  }

  void zeroWidthSpace(int indentLevel) throws IOException {
    if (closed) throw new IllegalStateException("closed");

    if (column == 0) return;
    if (this.nextFlush != null) flush(nextFlush);
    this.nextFlush = FlushType.EMPTY;
    this.indentLevel = indentLevel;
  }

  void close() throws IOException {
    if (nextFlush != null) flush(nextFlush);
    closed = true;
  }

  private void flush(FlushType flushType) throws IOException {
    switch (flushType) {
      case WRAP:
        out.append('\n');
        for (int i = 0; i < indentLevel; i++) {
          out.append(indent);
        }
        column = indentLevel * indent.length();
        column += buffer.length();
        break;
      case SPACE:
        out.append(' ');
        break;
      case EMPTY:
        break;
      default:
        throw new IllegalArgumentException("Unknown FlushType: " + flushType);
    }

    out.append(buffer);
    buffer.delete(0, buffer.length());
    indentLevel = -1;
    nextFlush = null;
  }

  private enum FlushType {
    WRAP, SPACE, EMPTY;
  }

  static final class RecordingAppendable implements Appendable {
    private final Appendable delegate;

    char lastChar = Character.MIN_VALUE;

    RecordingAppendable(Appendable delegate) {
      this.delegate = delegate;
    }

    @Override public Appendable append(CharSequence csq) throws IOException {
      int length = csq.length();
      if (length != 0) {
        lastChar = csq.charAt(length - 1);
      }
      return delegate.append(csq);
    }

    @Override public Appendable append(CharSequence csq, int start, int end) throws IOException {
      CharSequence sub = csq.subSequence(start, end);
      return append(sub);
    }

    @Override public Appendable append(char c) throws IOException {
      lastChar = c;
      return delegate.append(c);
    }
  }
}
