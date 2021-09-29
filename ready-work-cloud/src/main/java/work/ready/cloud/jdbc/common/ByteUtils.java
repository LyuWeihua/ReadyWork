package work.ready.cloud.jdbc.common;

public enum ByteUtils {
    ;

    public static final int MAX_BYTES_VLONG = 9;

    public static long zigZagDecode(long n) {
        return ((n >>> 1) ^ -(n & 1));
    }

    public static long zigZagEncode(long n) {
        return (n >> 63) ^ (n << 1);
    }

    public static void writeLongLE(long l, byte[] arr, int offset) {
        for (int i = 0; i < 8; ++i) {
            arr[offset++] = (byte) l;
            l >>>= 8;
        }
        assert l == 0;
    }

    public static long readLongLE(byte[] arr, int offset) {
        long l = arr[offset++] & 0xFFL;
        for (int i = 1; i < 8; ++i) {
            l |= (arr[offset++] & 0xFFL) << (8 * i);
        }
        return l;
    }

    public static void writeIntLE(int l, byte[] arr, int offset) {
        for (int i = 0; i < 4; ++i) {
            arr[offset++] = (byte) l;
            l >>>= 8;
        }
        assert l == 0;
    }

    public static int readIntLE(byte[] arr, int offset) {
        int l = arr[offset++] & 0xFF;
        for (int i = 1; i < 4; ++i) {
            l |= (arr[offset++] & 0xFF) << (8 * i);
        }
        return l;
    }

    public static void writeDoubleLE(double d, byte[] arr, int offset) {
        writeLongLE(Double.doubleToRawLongBits(d), arr, offset);
    }

    public static double readDoubleLE(byte[] arr, int offset) {
        return Double.longBitsToDouble(readLongLE(arr, offset));
    }

    public static void writeFloatLE(float d, byte[] arr, int offset) {
        writeIntLE(Float.floatToRawIntBits(d), arr, offset);
    }

    public static float readFloatLE(byte[] arr, int offset) {
        return Float.intBitsToFloat(readIntLE(arr, offset));
    }

    public static void writeVLong(ByteArrayDataOutput out, long i) {
        for (int k = 0; k < 8 && (i & ~0x7FL) != 0L; ++k) {
            out.writeByte((byte)((i & 0x7FL) | 0x80L));
            i >>>= 7;
        }
        out.writeByte((byte)i);
    }

    public static long readVLong(ByteArrayDataInput in) {
        
        byte b = in.readByte();
        if (b >= 0) return b;
        long i = b & 0x7FL;
        b = in.readByte();
        i |= (b & 0x7FL) << 7;
        if (b >= 0) return i;
        b = in.readByte();
        i |= (b & 0x7FL) << 14;
        if (b >= 0) return i;
        b = in.readByte();
        i |= (b & 0x7FL) << 21;
        if (b >= 0) return i;
        b = in.readByte();
        i |= (b & 0x7FL) << 28;
        if (b >= 0) return i;
        b = in.readByte();
        i |= (b & 0x7FL) << 35;
        if (b >= 0) return i;
        b = in.readByte();
        i |= (b & 0x7FL) << 42;
        if (b >= 0) return i;
        b = in.readByte();
        i |= (b & 0x7FL) << 49;
        if (b >= 0) return i;
        b = in.readByte();
        i |= (b & 0xFFL) << 56;
        return i;
    }

}
