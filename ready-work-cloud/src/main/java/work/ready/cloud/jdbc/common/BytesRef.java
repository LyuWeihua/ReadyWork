package work.ready.cloud.jdbc.common;

import work.ready.core.tools.CollectionUtil;

import java.util.Arrays;

public final class BytesRef implements Comparable<BytesRef>, Cloneable {
    
    public static final byte[] EMPTY_BYTES = new byte[0];

    public byte[] bytes;

    public int offset;

    public int length;

    public BytesRef() {
        this(EMPTY_BYTES);
    }

    public BytesRef(byte[] bytes, int offset, int length) {
        this.bytes = bytes;
        this.offset = offset;
        this.length = length;
        assert isValid();
    }

    public BytesRef(byte[] bytes) {
        this(bytes, 0, bytes.length);
    }

    public BytesRef(int capacity) {
        this.bytes = new byte[capacity];
    }

    public BytesRef(CharSequence text) {
        this(new byte[UnicodeUtil.maxUTF8Length(text.length())]);
        length = UnicodeUtil.UTF16toUTF8(text, 0, text.length(), bytes);
    }

    public boolean bytesEquals(BytesRef other) {
        return Arrays.equals(this.bytes, this.offset, this.offset + this.length,
                other.bytes, other.offset, other.offset + other.length);
    }

    @Override
    public BytesRef clone() {
        return new BytesRef(bytes, offset, length);
    }

    @Override
    public int hashCode() {
        return StringHelper.murmurhash3_x86_32(this, StringHelper.GOOD_FAST_HASH_SEED);
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other instanceof BytesRef) {
            return this.bytesEquals((BytesRef) other);
        }
        return false;
    }

    public String utf8ToString() {
        final char[] ref = new char[length];
        final int len = UnicodeUtil.UTF8toUTF16(bytes, offset, length, ref);
        return new String(ref, 0, len);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        final int end = offset + length;
        for(int i=offset;i<end;i++) {
            if (i > offset) {
                sb.append(' ');
            }
            sb.append(Integer.toHexString(bytes[i]&0xff));
        }
        sb.append(']');
        return sb.toString();
    }

    @Override
    public int compareTo(BytesRef other) {
        return Arrays.compareUnsigned(this.bytes, this.offset, this.offset + this.length,
                other.bytes, other.offset, other.offset + other.length);
    }

    public static BytesRef deepCopyOf(BytesRef other) {
        return new BytesRef(CollectionUtil.copyOfSubArray(other.bytes, other.offset, other.offset + other.length), 0, other.length);
    }

    public boolean isValid() {
        if (bytes == null) {
            throw new IllegalStateException("bytes is null");
        }
        if (length < 0) {
            throw new IllegalStateException("length is negative: " + length);
        }
        if (length > bytes.length) {
            throw new IllegalStateException("length is out of bounds: " + length + ",bytes.length=" + bytes.length);
        }
        if (offset < 0) {
            throw new IllegalStateException("offset is negative: " + offset);
        }
        if (offset > bytes.length) {
            throw new IllegalStateException("offset out of bounds: " + offset + ",bytes.length=" + bytes.length);
        }
        if (offset + length < 0) {
            throw new IllegalStateException("offset+length is negative: offset=" + offset + ",length=" + length);
        }
        if (offset + length > bytes.length) {
            throw new IllegalStateException("offset+length out of bounds: offset=" + offset + ",length=" + length + ",bytes.length=" + bytes.length);
        }
        return true;
    }
}
