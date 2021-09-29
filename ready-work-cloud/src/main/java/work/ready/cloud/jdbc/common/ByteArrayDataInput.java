package work.ready.cloud.jdbc.common;

public final class ByteArrayDataInput extends DataInput {

    private byte[] bytes;

    private int pos;
    private int limit;

    public ByteArrayDataInput(byte[] bytes) {
        reset(bytes);
    }

    public ByteArrayDataInput(byte[] bytes, int offset, int len) {
        reset(bytes, offset, len);
    }

    public ByteArrayDataInput() {
        reset(BytesRef.EMPTY_BYTES);
    }

    public void reset(byte[] bytes) {
        reset(bytes, 0, bytes.length);
    }

    public void rewind() {
        pos = 0;
    }

    public int getPosition() {
        return pos;
    }

    public void setPosition(int pos) {
        this.pos = pos;
    }

    public void reset(byte[] bytes, int offset, int len) {
        this.bytes = bytes;
        pos = offset;
        limit = offset + len;
    }

    public int length() {
        return limit;
    }

    public boolean eof() {
        return pos == limit;
    }

    @Override
    public void skipBytes(long count) {
        pos += count;
    }

    @Override
    public short readShort() {
        return (short) (((bytes[pos++] & 0xFF) <<  8) |  (bytes[pos++] & 0xFF));
    }

    @Override
    public int readInt() {
        return ((bytes[pos++] & 0xFF) << 24) | ((bytes[pos++] & 0xFF) << 16)
                | ((bytes[pos++] & 0xFF) <<  8) |  (bytes[pos++] & 0xFF);
    }

    @Override
    public long readLong() {
        final int i1 = ((bytes[pos++] & 0xff) << 24) | ((bytes[pos++] & 0xff) << 16) |
                ((bytes[pos++] & 0xff) << 8) | (bytes[pos++] & 0xff);
        final int i2 = ((bytes[pos++] & 0xff) << 24) | ((bytes[pos++] & 0xff) << 16) |
                ((bytes[pos++] & 0xff) << 8) | (bytes[pos++] & 0xff);
        return (((long)i1) << 32) | (i2 & 0xFFFFFFFFL);
    }

    @Override
    public int readVInt() {
        byte b = bytes[pos++];
        if (b >= 0) return b;
        int i = b & 0x7F;
        b = bytes[pos++];
        i |= (b & 0x7F) << 7;
        if (b >= 0) return i;
        b = bytes[pos++];
        i |= (b & 0x7F) << 14;
        if (b >= 0) return i;
        b = bytes[pos++];
        i |= (b & 0x7F) << 21;
        if (b >= 0) return i;
        b = bytes[pos++];
        
        i |= (b & 0x0F) << 28;
        if ((b & 0xF0) == 0) return i;
        throw new RuntimeException("Invalid vInt detected (too many bits)");
    }

    @Override
    public long readVLong() {
        byte b = bytes[pos++];
        if (b >= 0) return b;
        long i = b & 0x7FL;
        b = bytes[pos++];
        i |= (b & 0x7FL) << 7;
        if (b >= 0) return i;
        b = bytes[pos++];
        i |= (b & 0x7FL) << 14;
        if (b >= 0) return i;
        b = bytes[pos++];
        i |= (b & 0x7FL) << 21;
        if (b >= 0) return i;
        b = bytes[pos++];
        i |= (b & 0x7FL) << 28;
        if (b >= 0) return i;
        b = bytes[pos++];
        i |= (b & 0x7FL) << 35;
        if (b >= 0) return i;
        b = bytes[pos++];
        i |= (b & 0x7FL) << 42;
        if (b >= 0) return i;
        b = bytes[pos++];
        i |= (b & 0x7FL) << 49;
        if (b >= 0) return i;
        b = bytes[pos++];
        i |= (b & 0x7FL) << 56;
        if (b >= 0) return i;
        throw new RuntimeException("Invalid vLong detected (negative values disallowed)");
    }

    @Override
    public byte readByte() {
        return bytes[pos++];
    }

    @Override
    public void readBytes(byte[] b, int offset, int len) {
        System.arraycopy(bytes, pos, b, offset, len);
        pos += len;
    }
}
