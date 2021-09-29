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

package work.ready.cloud.jdbc.common.io.stream;

import work.ready.cloud.jdbc.common.BitUtil;
import work.ready.cloud.jdbc.common.BytesRef;
import work.ready.cloud.jdbc.common.ElasticsearchException;
import work.ready.cloud.Version;
import work.ready.cloud.jdbc.common.io.stream.Writeable.Writer;
import work.ready.cloud.jdbc.common.unit.TimeValue;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystemLoopException;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.time.Instant;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;

import static java.util.Map.entry;

public abstract class StreamOutput extends OutputStream {

    private static final int MAX_NESTED_EXCEPTION_LEVEL = 100;

    private Version version = Version.CURRENT;

    public Version getVersion() {
        return this.version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    public long position() throws IOException {
        throw new UnsupportedOperationException();
    }

    public void seek(long position) throws IOException {
        throw new UnsupportedOperationException();
    }

    public abstract void writeByte(byte b) throws IOException;

    public void writeBytes(byte[] b) throws IOException {
        writeBytes(b, 0, b.length);
    }

    public void writeBytes(byte[] b, int length) throws IOException {
        writeBytes(b, 0, length);
    }

    public abstract void writeBytes(byte[] b, int offset, int length) throws IOException;

    public void writeByteArray(byte[] b) throws IOException {
        writeVInt(b.length);
        writeBytes(b, 0, b.length);
    }

    public void writeBytesRef(BytesRef bytes) throws IOException {
        if (bytes == null) {
            writeVInt(0);
            return;
        }
        writeVInt(bytes.length);
        write(bytes.bytes, bytes.offset, bytes.length);
    }

    private static final ThreadLocal<byte[]> scratch = ThreadLocal.withInitial(() -> new byte[1024]);

    public final void writeShort(short v) throws IOException {
        final byte[] buffer = scratch.get();
        buffer[0] = (byte) (v >> 8);
        buffer[1] = (byte) v;
        writeBytes(buffer, 0, 2);
    }

    public void writeInt(int i) throws IOException {
        final byte[] buffer = scratch.get();
        buffer[0] = (byte) (i >> 24);
        buffer[1] = (byte) (i >> 16);
        buffer[2] = (byte) (i >> 8);
        buffer[3] = (byte) i;
        writeBytes(buffer, 0, 4);
    }

    public void writeVInt(int i) throws IOException {
        
        if (Integer.numberOfLeadingZeros(i) >= 25) {
            writeByte((byte) i);
            return;
        }
        byte[] buffer = scratch.get();
        int index = 0;
        do {
            buffer[index++] = ((byte) ((i & 0x7f) | 0x80));
            i >>>= 7;
        } while ((i & ~0x7F) != 0);
        buffer[index++] = ((byte) i);
        writeBytes(buffer, 0, index);
    }

    public void writeLong(long i) throws IOException {
        final byte[] buffer = scratch.get();
        buffer[0] = (byte) (i >> 56);
        buffer[1] = (byte) (i >> 48);
        buffer[2] = (byte) (i >> 40);
        buffer[3] = (byte) (i >> 32);
        buffer[4] = (byte) (i >> 24);
        buffer[5] = (byte) (i >> 16);
        buffer[6] = (byte) (i >> 8);
        buffer[7] = (byte) i;
        writeBytes(buffer, 0, 8);
    }

    public void writeVLong(long i) throws IOException {
        if (i < 0) {
            throw new IllegalStateException("Negative longs unsupported, use writeLong or writeZLong for negative numbers [" + i + "]");
        }
        writeVLongNoCheck(i);
    }

    public void writeOptionalVLong(Long l) throws IOException {
        if (l == null) {
            writeBoolean(false);
        } else {
            writeBoolean(true);
            writeVLong(l);
        }
    }

    void writeVLongNoCheck(long i) throws IOException {
        final byte[] buffer = scratch.get();
        int index = 0;
        while ((i & ~0x7F) != 0) {
            buffer[index++] = ((byte) ((i & 0x7f) | 0x80));
            i >>>= 7;
        }
        buffer[index++] = ((byte) i);
        writeBytes(buffer, 0, index);
    }

    public void writeZLong(long i) throws IOException {
        final byte[] buffer = scratch.get();
        int index = 0;
        
        long value = BitUtil.zigZagEncode(i);
        while ((value & 0xFFFFFFFFFFFFFF80L) != 0L) {
            buffer[index++] = (byte) ((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        buffer[index++] = (byte) (value & 0x7F);
        writeBytes(buffer, 0, index);
    }

    public void writeOptionalLong(Long l) throws IOException {
        if (l == null) {
            writeBoolean(false);
        } else {
            writeBoolean(true);
            writeLong(l);
        }
    }

    public void writeOptionalString(String str) throws IOException {
        if (str == null) {
            writeBoolean(false);
        } else {
            writeBoolean(true);
            writeString(str);
        }
    }

    public void writeOptionalInt(Integer integer) throws IOException {
        if (integer == null) {
            writeBoolean(false);
        } else {
            writeBoolean(true);
            writeInt(integer);
        }
    }

    public void writeOptionalVInt(Integer integer) throws IOException {
        if (integer == null) {
            writeBoolean(false);
        } else {
            writeBoolean(true);
            writeVInt(integer);
        }
    }

    public void writeOptionalFloat(Float floatValue) throws IOException {
        if (floatValue == null) {
            writeBoolean(false);
        } else {
            writeBoolean(true);
            writeFloat(floatValue);
        }
    }

    public void writeString(String str) throws IOException {
        final int charCount = str.length();
        byte[] buffer = scratch.get();
        int offset = 0;
        writeVInt(charCount);
        for (int i = 0; i < charCount; i++) {
            final int c = str.charAt(i);
            if (c <= 0x007F) {
                buffer[offset++] = ((byte) c);
            } else if (c > 0x07FF) {
                buffer[offset++] = ((byte) (0xE0 | c >> 12 & 0x0F));
                buffer[offset++] = ((byte) (0x80 | c >> 6 & 0x3F));
                buffer[offset++] = ((byte) (0x80 | c >> 0 & 0x3F));
            } else {
                buffer[offset++] = ((byte) (0xC0 | c >> 6 & 0x1F));
                buffer[offset++] = ((byte) (0x80 | c >> 0 & 0x3F));
            }

            if (offset > buffer.length - 3) {
                writeBytes(buffer, offset);
                offset = 0;
            }
        }
        writeBytes(buffer, offset);
    }

    public void writeFloat(float v) throws IOException {
        writeInt(Float.floatToIntBits(v));
    }

    public void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
    }

    public void writeOptionalDouble(Double v) throws IOException {
        if (v == null) {
            writeBoolean(false);
        } else {
            writeBoolean(true);
            writeDouble(v);
        }
    }

    private static byte ZERO = 0;
    private static byte ONE = 1;
    private static byte TWO = 2;

    public void writeBoolean(boolean b) throws IOException {
        writeByte(b ? ONE : ZERO);
    }

    public void writeOptionalBoolean(Boolean b) throws IOException {
        if (b == null) {
            writeByte(TWO);
        } else {
            writeBoolean(b);
        }
    }

    @Override
    public abstract void flush() throws IOException;

    @Override
    public abstract void close() throws IOException;

    public abstract void reset() throws IOException;

    @Override
    public void write(int b) throws IOException {
        writeByte((byte) b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        writeBytes(b, off, len);
    }

    public void writeStringArray(String[] array) throws IOException {
        writeVInt(array.length);
        for (String s : array) {
            writeString(s);
        }
    }

    public void writeStringArrayNullable(String[] array) throws IOException {
        if (array == null) {
            writeVInt(0);
        } else {
            writeVInt(array.length);
            for (String s : array) {
                writeString(s);
            }
        }
    }

    public void writeOptionalStringArray(String[] array) throws IOException {
        if (array == null) {
            writeBoolean(false);
        } else {
            writeBoolean(true);
            writeStringArray(array);
        }
    }

    public void writeMap(Map<String, Object> map) throws IOException {
        writeGenericValue(map);
    }

    public void writeMapWithConsistentOrder(Map<String, ? extends Object> map)
            throws IOException {
        if (map == null) {
            writeByte((byte) -1);
            return;
        }
        assert false == (map instanceof LinkedHashMap);
        this.writeByte((byte) 10);
        this.writeVInt(map.size());
        Iterator<? extends Map.Entry<String, ?>> iterator =
                map.entrySet().stream().sorted((a, b) -> a.getKey().compareTo(b.getKey())).iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ?> next = iterator.next();
            this.writeString(next.getKey());
            this.writeGenericValue(next.getValue());
        }
    }

    public final <K, V> void writeMapOfLists(final Map<K, List<V>> map, final Writer<K> keyWriter, final Writer<V> valueWriter)
            throws IOException {
        writeMap(map, keyWriter, (stream, list) -> {
            writeVInt(list.size());
            for (final V value : list) {
                valueWriter.write(this, value);
            }
        });
    }

    public final <K, V> void writeMap(final Map<K, V> map, final Writer<K> keyWriter, final Writer<V> valueWriter)
            throws IOException {
        writeVInt(map.size());
        for (final Map.Entry<K, V> entry : map.entrySet()) {
            keyWriter.write(this, entry.getKey());
            valueWriter.write(this, entry.getValue());
        }
    }

    public final void writeInstant(Instant instant) throws IOException {
        writeLong(instant.getEpochSecond());
        writeInt(instant.getNano());
    }

    public final void writeOptionalInstant(Instant instant) throws IOException {
        if (instant == null) {
            writeBoolean(false);
        } else {
            writeBoolean(true);
            writeInstant(instant);
        }
    }

    private static final Map<Class<?>, Writer> WRITERS = Map.ofEntries(
            entry(
                    String.class,
                    (o, v) -> {
                        o.writeByte((byte) 0);
                        o.writeString((String) v);
                    }),
            entry(
                    Integer.class,
                    (o, v) -> {
                        o.writeByte((byte) 1);
                        o.writeInt((Integer) v);
                    }),
            entry(
                    Long.class,
                    (o, v) -> {
                        o.writeByte((byte) 2);
                        o.writeLong((Long) v);
                    }),
            entry(
                    Float.class,
                    (o, v) -> {
                        o.writeByte((byte) 3);
                        o.writeFloat((float) v);
                    }),
            entry(
                    Double.class,
                    (o, v) -> {
                        o.writeByte((byte) 4);
                        o.writeDouble((double) v);
                    }),
            entry(
                    Boolean.class,
                    (o, v) -> {
                        o.writeByte((byte) 5);
                        o.writeBoolean((boolean) v);
                    }),
            entry(
                    byte[].class,
                    (o, v) -> {
                        o.writeByte((byte) 6);
                        final byte[] bytes = (byte[]) v;
                        o.writeVInt(bytes.length);
                        o.writeBytes(bytes);
                    }),
            entry(
                    List.class,
                    (o, v) -> {
                        o.writeByte((byte) 7);
                        final List list = (List) v;
                        o.writeVInt(list.size());
                        for (Object item : list) {
                            o.writeGenericValue(item);
                        }
                    }),
            entry(
                    Object[].class,
                    (o, v) -> {
                        o.writeByte((byte) 8);
                        final Object[] list = (Object[]) v;
                        o.writeVInt(list.length);
                        for (Object item : list) {
                            o.writeGenericValue(item);
                        }
                    }),
            entry(
                    Map.class,
                    (o, v) -> {
                        if (v instanceof LinkedHashMap) {
                            o.writeByte((byte) 9);
                        } else {
                            o.writeByte((byte) 10);
                        }
                        @SuppressWarnings("unchecked") final Map<String, Object> map = (Map<String, Object>) v;
                        o.writeVInt(map.size());
                        for (Map.Entry<String, Object> entry : map.entrySet()) {
                            o.writeString(entry.getKey());
                            o.writeGenericValue(entry.getValue());
                        }
                    }),
            entry(
                    Byte.class,
                    (o, v) -> {
                        o.writeByte((byte) 11);
                        o.writeByte((Byte) v);
                    }),
            entry(
                    Date.class,
                    (o, v) -> {
                        o.writeByte((byte) 12);
                        o.writeLong(((Date) v).getTime());
                    }),
            entry(
                    Short.class,
                    (o, v) -> {
                        o.writeByte((byte) 16);
                        o.writeShort((Short) v);
                    }),
            entry(
                    int[].class,
                    (o, v) -> {
                        o.writeByte((byte) 17);
                        o.writeIntArray((int[]) v);
                    }),
            entry(
                    long[].class,
                    (o, v) -> {
                        o.writeByte((byte) 18);
                        o.writeLongArray((long[]) v);
                    }),
            entry(
                    float[].class,
                    (o, v) -> {
                        o.writeByte((byte) 19);
                        o.writeFloatArray((float[]) v);
                    }),
            entry(
                    double[].class,
                    (o, v) -> {
                        o.writeByte((byte) 20);
                        o.writeDoubleArray((double[]) v);
                    }),
            entry(
                    BytesRef.class,
                    (o, v) -> {
                        o.writeByte((byte) 21);
                        o.writeBytesRef((BytesRef) v);
                    }),
            entry(
                    ZonedDateTime.class,
                    (o, v) -> {
                        o.writeByte((byte) 23);
                        final ZonedDateTime zonedDateTime = (ZonedDateTime) v;
                        o.writeString(zonedDateTime.getZone().getId());
                        o.writeLong(zonedDateTime.toInstant().toEpochMilli());
                    }),
            entry(
                    Set.class,
                    (o, v) -> {
                        if (v instanceof LinkedHashSet) {
                            o.writeByte((byte) 24);
                        } else {
                            o.writeByte((byte) 25);
                        }
                        o.writeCollection((Set<?>) v, StreamOutput::writeGenericValue);
                    }),
            entry(
                    
                    BigInteger.class,
                    (o, v) -> {
                        o.writeByte((byte) 26);
                        o.writeString(v.toString());
                    }
            ),
            entry(
                    OffsetTime.class,
                    (o, v) -> {
                        o.writeByte((byte) 27);
                        final OffsetTime offsetTime = (OffsetTime) v;
                        o.writeString(offsetTime.getOffset().getId());
                        o.writeLong(offsetTime.toLocalTime().toNanoOfDay());
                    }
            ));

    private static Class<?> getGenericType(Object value) {
        if (value instanceof List) {
            return List.class;
        } else if (value instanceof Object[]) {
            return Object[].class;
        } else if (value instanceof Map) {
            return Map.class;
        } else if (value instanceof Set) {
            return Set.class;
        } else {
            return value.getClass();
        }
    }
    
    public void writeGenericValue(Object value) throws IOException {
        if (value == null) {
            writeByte((byte) -1);
            return;
        }
        final Class<?> type = getGenericType(value);
        final Writer writer = WRITERS.get(type);
        if (writer != null) {
            writer.write(this, value);
        } else {
            throw new IllegalArgumentException("can not write type [" + type + "]");
        }
    }

    public static void checkWriteable(Object value) throws IllegalArgumentException {
        if (value == null) {
            return;
        }
        final Class<?> type = getGenericType(value);

        if (type == List.class) {
            @SuppressWarnings("unchecked") List<Object> list = (List<Object>) value;
            for (Object v : list) {
                checkWriteable(v);
            }
        } else if (type == Object[].class) {
            Object[] array = (Object[]) value;
            for (Object v : array) {
                checkWriteable(v);
            }
        } else if (type == Map.class) {
            @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) value;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                checkWriteable(entry.getKey());
                checkWriteable(entry.getValue());
            }
        } else if (type == Set.class) {
            @SuppressWarnings("unchecked") Set<Object> set = (Set<Object>) value;
            for (Object v : set) {
                checkWriteable(v);
            }
        } else if (WRITERS.containsKey(type) == false) {
            throw new IllegalArgumentException("Cannot write type [" + type.getCanonicalName() + "] to stream");
        }
    }

    public void writeIntArray(int[] values) throws IOException {
        writeVInt(values.length);
        for (int value : values) {
            writeInt(value);
        }
    }

    public void writeVIntArray(int[] values) throws IOException {
        writeVInt(values.length);
        for (int value : values) {
            writeVInt(value);
        }
    }

    public void writeLongArray(long[] values) throws IOException {
        writeVInt(values.length);
        for (long value : values) {
            writeLong(value);
        }
    }

    public void writeVLongArray(long[] values) throws IOException {
        writeVInt(values.length);
        for (long value : values) {
            writeVLong(value);
        }
    }

    public void writeFloatArray(float[] values) throws IOException {
        writeVInt(values.length);
        for (float value : values) {
            writeFloat(value);
        }
    }

    public void writeDoubleArray(double[] values) throws IOException {
        writeVInt(values.length);
        for (double value : values) {
            writeDouble(value);
        }
    }

    public <T> void writeArray(final Writer<T> writer, final T[] array) throws IOException {
        writeVInt(array.length);
        for (T value : array) {
            writer.write(this, value);
        }
    }

    public <T> void writeOptionalArray(final Writer<T> writer, final T[] array) throws IOException {
        if (array == null) {
            writeBoolean(false);
        } else {
            writeBoolean(true);
            writeArray(writer, array);
        }
    }

    public <T extends Writeable> void writeArray(T[] array) throws IOException {
        writeArray((out, value) -> value.writeTo(out), array);
    }

    public <T extends Writeable> void writeOptionalArray(T[] array) throws IOException {
        writeOptionalArray((out, value) -> value.writeTo(out), array);
    }

    public void writeOptionalWriteable(Writeable writeable) throws IOException {
        if (writeable != null) {
            writeBoolean(true);
            writeable.writeTo(this);
        } else {
            writeBoolean(false);
        }
    }

    public void writeException(Throwable throwable) throws IOException {
        writeException(throwable, throwable, 0);
    }

    private void writeException(Throwable rootException, Throwable throwable, int nestedLevel) throws IOException {
        if (throwable == null) {
            writeBoolean(false);
        } else if (nestedLevel > MAX_NESTED_EXCEPTION_LEVEL) {
            assert failOnTooManyNestedExceptions(rootException);
            writeException(new IllegalStateException("too many nested exceptions"));
        } else {
            writeBoolean(true);
            boolean writeCause = true;
            boolean writeMessage = true;
            if (throwable instanceof NullPointerException) {
                writeVInt(4);
                writeCause = false;
            } else if (throwable instanceof NumberFormatException) {
                writeVInt(5);
                writeCause = false;
            } else if (throwable instanceof IllegalArgumentException) {
                writeVInt(6);
            } else if (throwable instanceof EOFException) {
                writeVInt(8);
                writeCause = false;
            } else if (throwable instanceof SecurityException) {
                writeVInt(9);
            } else if (throwable instanceof StringIndexOutOfBoundsException) {
                writeVInt(10);
                writeCause = false;
            } else if (throwable instanceof ArrayIndexOutOfBoundsException) {
                writeVInt(11);
                writeCause = false;
            } else if (throwable instanceof FileNotFoundException) {
                writeVInt(12);
                writeCause = false;
            } else if (throwable instanceof FileSystemException) {
                writeVInt(13);
                if (throwable instanceof NoSuchFileException) {
                    writeVInt(0);
                } else if (throwable instanceof NotDirectoryException) {
                    writeVInt(1);
                } else if (throwable instanceof DirectoryNotEmptyException) {
                    writeVInt(2);
                } else if (throwable instanceof AtomicMoveNotSupportedException) {
                    writeVInt(3);
                } else if (throwable instanceof FileAlreadyExistsException) {
                    writeVInt(4);
                } else if (throwable instanceof AccessDeniedException) {
                    writeVInt(5);
                } else if (throwable instanceof FileSystemLoopException) {
                    writeVInt(6);
                } else {
                    writeVInt(7);
                }
                writeOptionalString(((FileSystemException) throwable).getFile());
                writeOptionalString(((FileSystemException) throwable).getOtherFile());
                writeOptionalString(((FileSystemException) throwable).getReason());
                writeCause = false;
            } else if (throwable instanceof IllegalStateException) {
                writeVInt(14);
            } else if (throwable instanceof InterruptedException) {
                writeVInt(16);
                writeCause = false;
            } else if (throwable instanceof IOException) {
                writeVInt(17);
            } else {
                final ElasticsearchException ex;
                if (throwable instanceof ElasticsearchException && ElasticsearchException.isRegistered(throwable.getClass(), version)) {
                    ex = (ElasticsearchException) throwable;
                } else {
                    ex = new NotSerializableExceptionWrapper(throwable);
                }
                writeVInt(0);
                writeVInt(ElasticsearchException.getId(ex.getClass()));
                ex.writeTo(this);
                return;
            }
            if (writeMessage) {
                writeOptionalString(throwable.getMessage());
            }
            if (writeCause) {
                writeException(rootException, throwable.getCause(), nestedLevel + 1);
            }
            ElasticsearchException.writeStackTraces(throwable, this, (o, t) -> o.writeException(rootException, t, nestedLevel + 1));
        }
    }

    boolean failOnTooManyNestedExceptions(Throwable throwable) {
        throw new AssertionError("too many nested exceptions", throwable);
    }

    public void writeNamedWriteable(NamedWriteable namedWriteable) throws IOException {
        writeString(namedWriteable.getWriteableName());
        namedWriteable.writeTo(this);
    }

    public void writeOptionalNamedWriteable(NamedWriteable namedWriteable) throws IOException {
        if (namedWriteable == null) {
            writeBoolean(false);
        } else {
            writeBoolean(true);
            writeNamedWriteable(namedWriteable);
        }
    }

    public void writeZoneId(ZoneId timeZone) throws IOException {
        writeString(timeZone.getId());
    }

    public void writeOptionalZoneId(ZoneId timeZone) throws IOException {
        if (timeZone == null) {
            writeBoolean(false);
        } else {
            writeBoolean(true);
            writeZoneId(timeZone);
        }
    }

    public void writeCollection(final Collection<? extends Writeable> collection) throws IOException {
        writeCollection(collection, (o, v) -> v.writeTo(o));
    }

    public void writeList(List<? extends Writeable> list) throws IOException {
        writeCollection(list);
    }

    public <T> void writeCollection(final Collection<T> collection, final Writer<T> writer) throws IOException {
        writeVInt(collection.size());
        for (final T val: collection) {
            writer.write(this, val);
        }
    }

    public void writeStringCollection(final Collection<String> collection) throws IOException {
        writeCollection(collection, StreamOutput::writeString);
    }

    public void writeOptionalStringCollection(final Collection<String> collection) throws IOException {
        if (collection != null) {
            writeBoolean(true);
            writeCollection(collection, StreamOutput::writeString);
        } else {
            writeBoolean(false);
        }
    }

    public void writeNamedWriteableList(List<? extends NamedWriteable> list) throws IOException {
        writeVInt(list.size());
        for (NamedWriteable obj: list) {
            writeNamedWriteable(obj);
        }
    }

    public <E extends Enum<E>> void writeEnum(E enumValue) throws IOException {
        writeVInt(enumValue.ordinal());
    }

    public <E extends Enum<E>> void writeEnumSet(EnumSet<E> enumSet) throws IOException {
        writeVInt(enumSet.size());
        for (E e : enumSet) {
            writeEnum(e);
        }
    }

    public void writeTimeValue(TimeValue timeValue) throws IOException {
        writeZLong(timeValue.duration());
        writeByte((byte) timeValue.timeUnit().ordinal());
    }

    public void writeOptionalTimeValue(TimeValue timeValue) throws IOException {
        if (timeValue == null) {
            writeBoolean(false);
        } else {
            writeBoolean(true);
            writeTimeValue(timeValue);
        }
    }

}
