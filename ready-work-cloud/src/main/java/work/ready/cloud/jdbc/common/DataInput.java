package work.ready.cloud.jdbc.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public abstract class DataInput implements Cloneable {

    private static final int SKIP_BUFFER_SIZE = 1024;

    private byte[] skipBuffer;

    public abstract byte readByte() throws IOException;

    public abstract void readBytes(byte[] b, int offset, int len)
            throws IOException;

    public void readBytes(byte[] b, int offset, int len, boolean useBuffer)
            throws IOException
    {
        
        readBytes(b, offset, len);
    }

    public short readShort() throws IOException {
        return (short) (((readByte() & 0xFF) <<  8) |  (readByte() & 0xFF));
    }

    public int readInt() throws IOException {
        return ((readByte() & 0xFF) << 24) | ((readByte() & 0xFF) << 16)
                | ((readByte() & 0xFF) <<  8) |  (readByte() & 0xFF);
    }

    public int readVInt() throws IOException {
    
        byte b = readByte();
        if (b >= 0) return b;
        int i = b & 0x7F;
        b = readByte();
        i |= (b & 0x7F) << 7;
        if (b >= 0) return i;
        b = readByte();
        i |= (b & 0x7F) << 14;
        if (b >= 0) return i;
        b = readByte();
        i |= (b & 0x7F) << 21;
        if (b >= 0) return i;
        b = readByte();
        
        i |= (b & 0x0F) << 28;
        if ((b & 0xF0) == 0) return i;
        throw new IOException("Invalid vInt detected (too many bits)");
    }

    public int readZInt() throws IOException {
        return BitUtil.zigZagDecode(readVInt());
    }

    public long readLong() throws IOException {
        return (((long)readInt()) << 32) | (readInt() & 0xFFFFFFFFL);
    }

    public void readLELongs(long[] dst, int offset, int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, dst.length);
        for (int i = 0; i < length; ++i) {
            dst[offset + i] = Long.reverseBytes(readLong());
        }
    }

    public long readVLong() throws IOException {
        return readVLong(false);
    }

    private long readVLong(boolean allowNegative) throws IOException {
    
        byte b = readByte();
        if (b >= 0) return b;
        long i = b & 0x7FL;
        b = readByte();
        i |= (b & 0x7FL) << 7;
        if (b >= 0) return i;
        b = readByte();
        i |= (b & 0x7FL) << 14;
        if (b >= 0) return i;
        b = readByte();
        i |= (b & 0x7FL) << 21;
        if (b >= 0) return i;
        b = readByte();
        i |= (b & 0x7FL) << 28;
        if (b >= 0) return i;
        b = readByte();
        i |= (b & 0x7FL) << 35;
        if (b >= 0) return i;
        b = readByte();
        i |= (b & 0x7FL) << 42;
        if (b >= 0) return i;
        b = readByte();
        i |= (b & 0x7FL) << 49;
        if (b >= 0) return i;
        b = readByte();
        i |= (b & 0x7FL) << 56;
        if (b >= 0) return i;
        if (allowNegative) {
            b = readByte();
            i |= (b & 0x7FL) << 63;
            if (b == 0 || b == 1) return i;
            throw new IOException("Invalid vLong detected (more than 64 bits)");
        } else {
            throw new IOException("Invalid vLong detected (negative values disallowed)");
        }
    }

    public long readZLong() throws IOException {
        return BitUtil.zigZagDecode(readVLong(true));
    }

    public String readString() throws IOException {
        int length = readVInt();
        final byte[] bytes = new byte[length];
        readBytes(bytes, 0, length);
        return new String(bytes, 0, length, StandardCharsets.UTF_8);
    }

    @Override
    public DataInput clone() {
        try {
            return (DataInput) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new Error("This cannot happen: Failing to clone DataInput");
        }
    }

    public Map<String,String> readMapOfStrings() throws IOException {
        int count = readVInt();
        if (count == 0) {
            return Collections.emptyMap();
        } else if (count == 1) {
            return Collections.singletonMap(readString(), readString());
        } else {
            Map<String,String> map = count > 10 ? new HashMap<>() : new TreeMap<>();
            for (int i = 0; i < count; i++) {
                final String key = readString();
                final String val = readString();
                map.put(key, val);
            }
            return Collections.unmodifiableMap(map);
        }
    }

    public Set<String> readSetOfStrings() throws IOException {
        int count = readVInt();
        if (count == 0) {
            return Collections.emptySet();
        } else if (count == 1) {
            return Collections.singleton(readString());
        } else {
            Set<String> set = count > 10 ? new HashSet<>() : new TreeSet<>();
            for (int i = 0; i < count; i++) {
                set.add(readString());
            }
            return Collections.unmodifiableSet(set);
        }
    }

    public void skipBytes(final long numBytes) throws IOException {
        if (numBytes < 0) {
            throw new IllegalArgumentException("numBytes must be >= 0, got " + numBytes);
        }
        if (skipBuffer == null) {
            skipBuffer = new byte[SKIP_BUFFER_SIZE];
        }
        assert skipBuffer.length == SKIP_BUFFER_SIZE;
        for (long skipped = 0; skipped < numBytes; ) {
            final int step = (int) Math.min(SKIP_BUFFER_SIZE, numBytes - skipped);
            readBytes(skipBuffer, 0, step, false);
            skipped += step;
        }
    }

}
