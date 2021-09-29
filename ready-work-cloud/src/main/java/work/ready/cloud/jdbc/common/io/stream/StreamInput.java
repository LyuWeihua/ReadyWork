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

import work.ready.cloud.Version;
import work.ready.cloud.jdbc.common.*;
import work.ready.cloud.jdbc.common.unit.TimeValue;
import work.ready.core.tools.CollectionUtil;
import work.ready.core.tools.StrUtil;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

import static work.ready.cloud.jdbc.common.ElasticsearchException.readStackTrace;

public abstract class StreamInput extends InputStream {

    private Version version = Version.CURRENT;

    public Version getVersion() {
        return this.version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    public abstract byte readByte() throws IOException;

    public abstract void readBytes(byte[] b, int offset, int len) throws IOException;

    public BytesRef readBytesRef() throws IOException {
        int length = readArraySize();
        return readBytesRef(length);
    }

    public BytesRef readBytesRef(int length) throws IOException {
        if (length == 0) {
            return new BytesRef();
        }
        byte[] bytes = new byte[length];
        readBytes(bytes, 0, length);
        return new BytesRef(bytes, 0, length);
    }

    public void readFully(byte[] b) throws IOException {
        readBytes(b, 0, b.length);
    }

    public short readShort() throws IOException {
        return (short) (((readByte() & 0xFF) << 8) | (readByte() & 0xFF));
    }

    public int readInt() throws IOException {
        return ((readByte() & 0xFF) << 24) | ((readByte() & 0xFF) << 16)
                | ((readByte() & 0xFF) << 8) | (readByte() & 0xFF);
    }

    public Integer readOptionalInt() throws IOException {
        if (readBoolean()) {
            return readInt();
        }
        return null;
    }

    public int readVInt() throws IOException {
        byte b = readByte();
        int i = b & 0x7F;
        if ((b & 0x80) == 0) {
            return i;
        }
        b = readByte();
        i |= (b & 0x7F) << 7;
        if ((b & 0x80) == 0) {
            return i;
        }
        b = readByte();
        i |= (b & 0x7F) << 14;
        if ((b & 0x80) == 0) {
            return i;
        }
        b = readByte();
        i |= (b & 0x7F) << 21;
        if ((b & 0x80) == 0) {
            return i;
        }
        b = readByte();
        if ((b & 0x80) != 0) {
            throw new IOException("Invalid vInt ((" + Integer.toHexString(b) + " & 0x7f) << 28) | " + Integer.toHexString(i));
        }
        return i | ((b & 0x7F) << 28);
    }

    public long readLong() throws IOException {
        return (((long) readInt()) << 32) | (readInt() & 0xFFFFFFFFL);
    }

    public long readVLong() throws IOException {
        byte b = readByte();
        long i = b & 0x7FL;
        if ((b & 0x80) == 0) {
            return i;
        }
        b = readByte();
        i |= (b & 0x7FL) << 7;
        if ((b & 0x80) == 0) {
            return i;
        }
        b = readByte();
        i |= (b & 0x7FL) << 14;
        if ((b & 0x80) == 0) {
            return i;
        }
        b = readByte();
        i |= (b & 0x7FL) << 21;
        if ((b & 0x80) == 0) {
            return i;
        }
        b = readByte();
        i |= (b & 0x7FL) << 28;
        if ((b & 0x80) == 0) {
            return i;
        }
        b = readByte();
        i |= (b & 0x7FL) << 35;
        if ((b & 0x80) == 0) {
            return i;
        }
        b = readByte();
        i |= (b & 0x7FL) << 42;
        if ((b & 0x80) == 0) {
            return i;
        }
        b = readByte();
        i |= (b & 0x7FL) << 49;
        if ((b & 0x80) == 0) {
            return i;
        }
        b = readByte();
        i |= ((b & 0x7FL) << 56);
        if ((b & 0x80) == 0) {
            return i;
        }
        b = readByte();
        if (b != 0 && b != 1) {
            throw new IOException("Invalid vlong (" + Integer.toHexString(b) + " << 63) | " + Long.toHexString(i));
        }
        i |= ((long) b) << 63;
        return i;
    }

    public Long readOptionalVLong() throws IOException {
        if (readBoolean()) {
            return readVLong();
        }
        return null;
    }

    public long readZLong() throws IOException {
        long accumulator = 0L;
        int i = 0;
        long currentByte;
        while (((currentByte = readByte()) & 0x80L) != 0) {
            accumulator |= (currentByte & 0x7F) << i;
            i += 7;
            if (i > 63) {
                throw new IOException("variable-length stream is too long");
            }
        }
        return BitUtil.zigZagDecode(accumulator | (currentByte << i));
    }

    public Long readOptionalLong() throws IOException {
        if (readBoolean()) {
            return readLong();
        }
        return null;
    }

    public BigInteger readBigInteger() throws IOException {
        return new BigInteger(readString());
    }

    public String readOptionalString() throws IOException {
        if (readBoolean()) {
            return readString();
        }
        return null;
    }

    public Float readOptionalFloat() throws IOException {
        if (readBoolean()) {
            return readFloat();
        }
        return null;
    }

    public Integer readOptionalVInt() throws IOException {
        if (readBoolean()) {
            return readVInt();
        }
        return null;
    }

    private static final int SMALL_STRING_LIMIT = 1024;

    private static final ThreadLocal<byte[]> stringReadBuffer = ThreadLocal.withInitial(() -> new byte[1024]);

    private static final ThreadLocal<CharsRef> smallSpare = ThreadLocal.withInitial(() -> new CharsRef(SMALL_STRING_LIMIT));

    private CharsRef largeSpare;

    public String readString() throws IOException {
        final int charCount = readArraySize();
        final CharsRef charsRef;
        if (charCount > SMALL_STRING_LIMIT) {
            if (largeSpare == null) {
                largeSpare = new CharsRef(CollectionUtil.oversize(charCount, Character.BYTES));
            } else if (largeSpare.chars.length < charCount) {
                
                largeSpare.chars = new char[CollectionUtil.oversize(charCount, Character.BYTES)];
            }
            charsRef = largeSpare;
        } else {
            charsRef = smallSpare.get();
        }
        charsRef.length = charCount;
        int charsOffset = 0;
        int offsetByteArray = 0;
        int sizeByteArray = 0;
        int missingFromPartial = 0;
        final byte[] byteBuffer = stringReadBuffer.get();
        final char[] charBuffer = charsRef.chars;
        for (; charsOffset < charCount; ) {
            final int charsLeft = charCount - charsOffset;
            int bufferFree = byteBuffer.length - sizeByteArray;
            
            final int minRemainingBytes;
            if (missingFromPartial > 0) {
                
                minRemainingBytes = missingFromPartial + charsLeft - 1;
                missingFromPartial = 0;
            } else {
                
                minRemainingBytes = charsLeft;
            }
            final int toRead;
            if (bufferFree < minRemainingBytes) {

                if (offsetByteArray > 0) {
                    sizeByteArray = sizeByteArray - offsetByteArray;
                    switch (sizeByteArray) { 
                        case 1:
                            byteBuffer[0] = byteBuffer[offsetByteArray];
                            break;
                        case 2:
                            byteBuffer[0] = byteBuffer[offsetByteArray];
                            byteBuffer[1] = byteBuffer[offsetByteArray + 1];
                            break;
                    }
                    assert sizeByteArray <= 2 : "We never copy more than 2 bytes here since a char is 3 bytes max";
                    toRead = Math.min(bufferFree + offsetByteArray, minRemainingBytes);
                    offsetByteArray = 0;
                } else {
                    toRead = bufferFree;
                }
            } else {
                toRead = minRemainingBytes;
            }
            readBytes(byteBuffer, sizeByteArray, toRead);
            sizeByteArray += toRead;

            for (; offsetByteArray < sizeByteArray - 2; offsetByteArray++) {
                final int c = byteBuffer[offsetByteArray] & 0xff;
                switch (c >> 4) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                        charBuffer[charsOffset++] = (char) c;
                        break;
                    case 12:
                    case 13:
                        charBuffer[charsOffset++] = (char) ((c & 0x1F) << 6 | byteBuffer[++offsetByteArray] & 0x3F);
                        break;
                    case 14:
                        charBuffer[charsOffset++] = (char) (
                                (c & 0x0F) << 12 | (byteBuffer[++offsetByteArray] & 0x3F) << 6 | (byteBuffer[++offsetByteArray] & 0x3F));
                        break;
                    default:
                        throwOnBrokenChar(c);
                }
            }
            
            final int bufferedBytesRemaining = sizeByteArray - offsetByteArray;
            for (int i = 0; i < bufferedBytesRemaining; i++) {
                final int c = byteBuffer[offsetByteArray] & 0xff;
                switch (c >> 4) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                        charBuffer[charsOffset++] = (char) c;
                        offsetByteArray++;
                        break;
                    case 12:
                    case 13:
                        missingFromPartial = 2 - (bufferedBytesRemaining - i);
                        if (missingFromPartial == 0) {
                            offsetByteArray++;
                            charBuffer[charsOffset++] = (char) ((c & 0x1F) << 6 | byteBuffer[offsetByteArray++] & 0x3F);
                        }
                        ++i;
                        break;
                    case 14:
                        missingFromPartial = 3 - (bufferedBytesRemaining - i);
                        ++i;
                        break;
                    default:
                        throwOnBrokenChar(c);
                }
            }
        }
        return charsRef.toString();
    }

    private static void throwOnBrokenChar(int c) throws IOException {
        throw new IOException("Invalid string; unexpected character: " + c + " hex: " + Integer.toHexString(c));
    }

    public final float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    public final double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    public final Double readOptionalDouble() throws IOException {
        if (readBoolean()) {
            return readDouble();
        }
        return null;
    }

    public final boolean readBoolean() throws IOException {
        return readBoolean(readByte());
    }

    private boolean readBoolean(final byte value) {
        if (value == 0) {
            return false;
        } else if (value == 1) {
            return true;
        } else {
            final String message = String.format(Locale.ROOT, "unexpected byte [0x%02x]", value);
            throw new IllegalStateException(message);
        }
    }

    public final Boolean readOptionalBoolean() throws IOException {
        final byte value = readByte();
        if (value == 2) {
            return null;
        } else {
            return readBoolean(value);
        }
    }

    @Override
    public abstract void close() throws IOException;

    @Override
    public abstract int available() throws IOException;

    public String[] readStringArray() throws IOException {
        int size = readArraySize();
        if (size == 0) {
            return StrUtil.EMPTY_ARRAY;
        }
        String[] ret = new String[size];
        for (int i = 0; i < size; i++) {
            ret[i] = readString();
        }
        return ret;
    }

    public String[] readOptionalStringArray() throws IOException {
        if (readBoolean()) {
            return readStringArray();
        }
        return null;
    }

    public <K, V> Map<K, V> readMap(Writeable.Reader<K> keyReader, Writeable.Reader<V> valueReader) throws IOException {
        int size = readArraySize();
        if (size == 0) {
            return Collections.emptyMap();
        }
        Map<K, V> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            K key = keyReader.read(this);
            V value = valueReader.read(this);
            map.put(key, value);
        }
        return map;
    }

    public <K, V> Map<K, List<V>> readMapOfLists(final Writeable.Reader<K> keyReader, final Writeable.Reader<V> valueReader)
            throws IOException {
        final int size = readArraySize();
        if (size == 0) {
            return Collections.emptyMap();
        }
        final Map<K, List<V>> map = new HashMap<>(size);
        for (int i = 0; i < size; ++i) {
            map.put(keyReader.read(this), readList(valueReader));
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> readMap() throws IOException {
        return (Map<String, Object>) readGenericValue();
    }

    public Object readGenericValue() throws IOException {
        byte type = readByte();
        switch (type) {
            case -1:
                return null;
            case 0:
                return readString();
            case 1:
                return readInt();
            case 2:
                return readLong();
            case 3:
                return readFloat();
            case 4:
                return readDouble();
            case 5:
                return readBoolean();
            case 6:
                return readByteArray();
            case 7:
                return readArrayList();
            case 8:
                return readArray();
            case 9:
                return readLinkedHashMap();
            case 10:
                return readHashMap();
            case 11:
                return readByte();
            case 12:
                return readDate();
            case 16:
                return readShort();
            case 17:
                return readIntArray();
            case 18:
                return readLongArray();
            case 19:
                return readFloatArray();
            case 20:
                return readDoubleArray();
            case 21:
                return readBytesRef();
            case 23:
                return readZonedDateTime();
            case 24:
                return readCollection(StreamInput::readGenericValue, LinkedHashSet::new, Collections.emptySet());
            case 25:
                return readCollection(StreamInput::readGenericValue, HashSet::new, Collections.emptySet());
            case 26:
                return readBigInteger();
            case 27:
                return readOffsetTime();
            default:
                throw new IOException("Can't read unknown type [" + type + "]");
        }
    }

    public final Instant readInstant() throws IOException {
        return Instant.ofEpochSecond(readLong(), readInt());
    }

    public final Instant readOptionalInstant() throws IOException {
        final boolean present = readBoolean();
        return present ? readInstant() : null;
    }

    private List<Object> readArrayList() throws IOException {
        int size = readArraySize();
        if (size == 0) {
            return Collections.emptyList();
        }
        List<Object> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(readGenericValue());
        }
        return list;
    }

    private ZonedDateTime readZonedDateTime() throws IOException {
        final String timeZoneId = readString();
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(readLong()), ZoneId.of(timeZoneId));
    }

    private OffsetTime readOffsetTime() throws IOException {
        final String zoneOffsetId = readString();
        return OffsetTime.of(LocalTime.ofNanoOfDay(readLong()), ZoneOffset.of(zoneOffsetId));
    }

    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    private Object[] readArray() throws IOException {
        int size8 = readArraySize();
        if (size8 == 0) {
            return EMPTY_OBJECT_ARRAY;
        }
        Object[] list8 = new Object[size8];
        for (int i = 0; i < size8; i++) {
            list8[i] = readGenericValue();
        }
        return list8;
    }

    private Map readLinkedHashMap() throws IOException {
        int size9 = readArraySize();
        if (size9 == 0) {
            return Collections.emptyMap();
        }
        Map<String, Object> map9 = new LinkedHashMap<>(size9);
        for (int i = 0; i < size9; i++) {
            map9.put(readString(), readGenericValue());
        }
        return map9;
    }

    private Map readHashMap() throws IOException {
        int size10 = readArraySize();
        if (size10 == 0) {
            return Collections.emptyMap();
        }
        Map<String, Object> map10 = new HashMap<>(size10);
        for (int i = 0; i < size10; i++) {
            map10.put(readString(), readGenericValue());
        }
        return map10;
    }

    private Date readDate() throws IOException {
        return new Date(readLong());
    }

    public ZoneId readZoneId() throws IOException {
        return ZoneId.of(readString());
    }

    public ZoneId readOptionalZoneId() throws IOException {
        if (readBoolean()) {
            return ZoneId.of(readString());
        }
        return null;
    }

    private static final int[] EMPTY_INT_ARRAY = new int[0];

    public int[] readIntArray() throws IOException {
        int length = readArraySize();
        if (length == 0) {
            return EMPTY_INT_ARRAY;
        }
        int[] values = new int[length];
        for (int i = 0; i < length; i++) {
            values[i] = readInt();
        }
        return values;
    }

    public int[] readVIntArray() throws IOException {
        int length = readArraySize();
        if (length == 0) {
            return EMPTY_INT_ARRAY;
        }
        int[] values = new int[length];
        for (int i = 0; i < length; i++) {
            values[i] = readVInt();
        }
        return values;
    }

    private static final long[] EMPTY_LONG_ARRAY = new long[0];

    public long[] readLongArray() throws IOException {
        int length = readArraySize();
        if (length == 0) {
            return EMPTY_LONG_ARRAY;
        }
        long[] values = new long[length];
        for (int i = 0; i < length; i++) {
            values[i] = readLong();
        }
        return values;
    }

    public long[] readVLongArray() throws IOException {
        int length = readArraySize();
        if (length == 0) {
            return EMPTY_LONG_ARRAY;
        }
        long[] values = new long[length];
        for (int i = 0; i < length; i++) {
            values[i] = readVLong();
        }
        return values;
    }

    private static final float[] EMPTY_FLOAT_ARRAY = new float[0];

    public float[] readFloatArray() throws IOException {
        int length = readArraySize();
        if (length == 0) {
            return EMPTY_FLOAT_ARRAY;
        }
        float[] values = new float[length];
        for (int i = 0; i < length; i++) {
            values[i] = readFloat();
        }
        return values;
    }

    private static final double[] EMPTY_DOUBLE_ARRAY = new double[0];

    public double[] readDoubleArray() throws IOException {
        int length = readArraySize();
        if (length == 0) {
            return EMPTY_DOUBLE_ARRAY;
        }
        double[] values = new double[length];
        for (int i = 0; i < length; i++) {
            values[i] = readDouble();
        }
        return values;
    }

    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    public byte[] readByteArray() throws IOException {
        final int length = readArraySize();
        if (length == 0) {
            return EMPTY_BYTE_ARRAY;
        }
        final byte[] bytes = new byte[length];
        readBytes(bytes, 0, bytes.length);
        return bytes;
    }

    public <T> T[] readArray(final Writeable.Reader<T> reader, final IntFunction<T[]> arraySupplier) throws IOException {
        final int length = readArraySize();
        final T[] values = arraySupplier.apply(length);
        for (int i = 0; i < length; i++) {
            values[i] = reader.read(this);
        }
        return values;
    }

    public <T> T[] readOptionalArray(Writeable.Reader<T> reader, IntFunction<T[]> arraySupplier) throws IOException {
        return readBoolean() ? readArray(reader, arraySupplier) : null;
    }

    public <T extends Writeable> T readOptionalWriteable(Writeable.Reader<T> reader) throws IOException {
        if (readBoolean()) {
            T t = reader.read(this);
            if (t == null) {
                throw new IOException("Writeable.Reader [" + reader
                        + "] returned null which is not allowed and probably means it screwed up the stream.");
            }
            return t;
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Exception> T readException() throws IOException {
        if (readBoolean()) {
            int key = readVInt();
            switch (key) {
                case 0:
                    final int ord = readVInt();
                    return (T) ElasticsearchException.readException(this, ord);
                case 4:
                    return (T) readStackTrace(new NullPointerException(readOptionalString()), this);
                case 5:
                    return (T) readStackTrace(new NumberFormatException(readOptionalString()), this);
                case 6:
                    return (T) readStackTrace(new IllegalArgumentException(readOptionalString(), readException()), this);
                case 8:
                    return (T) readStackTrace(new EOFException(readOptionalString()), this);
                case 9:
                    return (T) readStackTrace(new SecurityException(readOptionalString(), readException()), this);
                case 10:
                    return (T) readStackTrace(new StringIndexOutOfBoundsException(readOptionalString()), this);
                case 11:
                    return (T) readStackTrace(new ArrayIndexOutOfBoundsException(readOptionalString()), this);
                case 12:
                    return (T) readStackTrace(new FileNotFoundException(readOptionalString()), this);
                case 13:
                    final int subclass = readVInt();
                    final String file = readOptionalString();
                    final String other = readOptionalString();
                    final String reason = readOptionalString();
                    readOptionalString(); 
                    final Exception exception;
                    switch (subclass) {
                        case 0:
                            exception = new NoSuchFileException(file, other, reason);
                            break;
                        case 1:
                            exception = new NotDirectoryException(file);
                            break;
                        case 2:
                            exception = new DirectoryNotEmptyException(file);
                            break;
                        case 3:
                            exception = new AtomicMoveNotSupportedException(file, other, reason);
                            break;
                        case 4:
                            exception = new FileAlreadyExistsException(file, other, reason);
                            break;
                        case 5:
                            exception = new AccessDeniedException(file, other, reason);
                            break;
                        case 6:
                            exception = new FileSystemLoopException(file);
                            break;
                        case 7:
                            exception = new FileSystemException(file, other, reason);
                            break;
                        default:
                            throw new IllegalStateException("unknown FileSystemException with index " + subclass);
                    }
                    return (T) readStackTrace(exception, this);
                case 14:
                    return (T) readStackTrace(new IllegalStateException(readOptionalString(), readException()), this);
                case 16:
                    return (T) readStackTrace(new InterruptedException(readOptionalString()), this);
                case 17:
                    return (T) readStackTrace(new IOException(readOptionalString(), readException()), this);
                default:
                    throw new IOException("no such exception for id: " + key);
            }
        }
        return null;
    }

    public NamedWriteableRegistry namedWriteableRegistry() {
        return null;
    }

    public <C extends NamedWriteable> C readNamedWriteable(@SuppressWarnings("unused") Class<C> categoryClass) throws IOException {
        throw new UnsupportedOperationException("can't read named writeable from StreamInput");
    }

    public <C extends NamedWriteable> C readNamedWriteable(@SuppressWarnings("unused") Class<C> categoryClass,
                                                           @SuppressWarnings("unused") String name) throws IOException {
        throw new UnsupportedOperationException("can't read named writeable from StreamInput");
    }

    public <C extends NamedWriteable> C readOptionalNamedWriteable(Class<C> categoryClass) throws IOException {
        if (readBoolean()) {
            return readNamedWriteable(categoryClass);
        }
        return null;
    }

    public <T> List<T> readList(final Writeable.Reader<T> reader) throws IOException {
        return readCollection(reader, ArrayList::new, Collections.emptyList());
    }

    public List<String> readStringList() throws IOException {
        return readList(StreamInput::readString);
    }

    public List<String> readOptionalStringList() throws IOException {
        final boolean isPresent = readBoolean();
        if (isPresent) {
            return readList(StreamInput::readString);
        } else {
            return null;
        }
    }

    public <T> Set<T> readSet(Writeable.Reader<T> reader) throws IOException {
        return readCollection(reader, HashSet::new, Collections.emptySet());
    }

    private <T, C extends Collection<? super T>> C readCollection(Writeable.Reader<T> reader,
                                                                  IntFunction<C> constructor,
                                                                  C empty) throws IOException {
        int count = readArraySize();
        if (count == 0) {
            return empty;
        }
        C builder = constructor.apply(count);
        for (int i=0; i<count; i++) {
            builder.add(reader.read(this));
        }
        return builder;
    }

    public <T extends NamedWriteable> List<T> readNamedWriteableList(Class<T> categoryClass) throws IOException {
        int count = readArraySize();
        if (count == 0) {
            return Collections.emptyList();
        }
        List<T> builder = new ArrayList<>(count);
        for (int i=0; i<count; i++) {
            builder.add(readNamedWriteable(categoryClass));
        }
        return builder;
    }

    public <E extends Enum<E>> E readEnum(Class<E> enumClass) throws IOException {
        return readEnum(enumClass, enumClass.getEnumConstants());
    }

    private <E extends Enum<E>> E readEnum(Class<E> enumClass, E[] values) throws IOException {
        int ordinal = readVInt();
        if (ordinal < 0 || ordinal >= values.length) {
            throw new IOException("Unknown " + enumClass.getSimpleName() + " ordinal [" + ordinal + "]");
        }
        return values[ordinal];
    }

    public <E extends Enum<E>> EnumSet<E> readEnumSet(Class<E> enumClass) throws IOException {
        int size = readVInt();
        final EnumSet<E> res = EnumSet.noneOf(enumClass);
        if (size == 0) {
            return res;
        }
        final E[] values = enumClass.getEnumConstants();
        for (int i = 0; i < size; i++) {
            res.add(readEnum(enumClass, values));
        }
        return res;
    }

    public static StreamInput wrap(byte[] bytes) {
        return wrap(bytes, 0, bytes.length);
    }

    public static StreamInput wrap(byte[] bytes, int offset, int length) {
        return new InputStreamStreamInput(new ByteArrayInputStream(bytes, offset, length), length);
    }

    private int readArraySize() throws IOException {
        final int arraySize = readVInt();
        if (arraySize > CollectionUtil.MAX_ARRAY_LENGTH) {
            throw new IllegalStateException("array length must be <= to " + CollectionUtil.MAX_ARRAY_LENGTH  + " but was: " + arraySize);
        }
        if (arraySize < 0) {
            throw new NegativeArraySizeException("array size must be positive but was: " + arraySize);
        }

        ensureCanReadBytes(arraySize);
        return arraySize;
    }

    protected abstract void ensureCanReadBytes(int length) throws EOFException;

    private static final TimeUnit[] TIME_UNITS = TimeUnit.values();

    static {
        
        if (Arrays.equals(TIME_UNITS, new TimeUnit[]{TimeUnit.NANOSECONDS, TimeUnit.MICROSECONDS, TimeUnit.MILLISECONDS,
                TimeUnit.SECONDS, TimeUnit.MINUTES, TimeUnit.HOURS, TimeUnit.DAYS}) == false) {
            throw new AssertionError("Incompatible JDK version used that breaks assumptions on the structure of the TimeUnit enum");
        }
    }

    public TimeValue readTimeValue() throws IOException {
        long duration = readZLong();
        TimeUnit timeUnit = TIME_UNITS[readByte()];
        return new TimeValue(duration, timeUnit);
    }

    public TimeValue readOptionalTimeValue() throws IOException {
        if (readBoolean()) {
            return readTimeValue();
        } else {
            return null;
        }
    }
}
