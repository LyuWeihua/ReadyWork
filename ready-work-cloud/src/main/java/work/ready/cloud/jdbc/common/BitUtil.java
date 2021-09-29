package work.ready.cloud.jdbc.common;

public final class BitUtil {

    private BitUtil() {}

    public static long pop_array(long[] arr, int wordOffset, int numWords) {
        long popCount = 0;
        for (int i = wordOffset, end = wordOffset + numWords; i < end; ++i) {
            popCount += Long.bitCount(arr[i]);
        }
        return popCount;
    }

    public static long pop_intersect(long[] arr1, long[] arr2, int wordOffset, int numWords) {
        long popCount = 0;
        for (int i = wordOffset, end = wordOffset + numWords; i < end; ++i) {
            popCount += Long.bitCount(arr1[i] & arr2[i]);
        }
        return popCount;
    }

    public static long pop_union(long[] arr1, long[] arr2, int wordOffset, int numWords) {
        long popCount = 0;
        for (int i = wordOffset, end = wordOffset + numWords; i < end; ++i) {
            popCount += Long.bitCount(arr1[i] | arr2[i]);
        }
        return popCount;
    }

    public static long pop_andnot(long[] arr1, long[] arr2, int wordOffset, int numWords) {
        long popCount = 0;
        for (int i = wordOffset, end = wordOffset + numWords; i < end; ++i) {
            popCount += Long.bitCount(arr1[i] & ~arr2[i]);
        }
        return popCount;
    }

    public static long pop_xor(long[] arr1, long[] arr2, int wordOffset, int numWords) {
        long popCount = 0;
        for (int i = wordOffset, end = wordOffset + numWords; i < end; ++i) {
            popCount += Long.bitCount(arr1[i] ^ arr2[i]);
        }
        return popCount;
    }

    public static int nextHighestPowerOfTwo(int v) {
        v--;
        v |= v >> 1;
        v |= v >> 2;
        v |= v >> 4;
        v |= v >> 8;
        v |= v >> 16;
        v++;
        return v;
    }

    public static long nextHighestPowerOfTwo(long v) {
        v--;
        v |= v >> 1;
        v |= v >> 2;
        v |= v >> 4;
        v |= v >> 8;
        v |= v >> 16;
        v |= v >> 32;
        v++;
        return v;
    }

    private static final long MAGIC0 = 0x5555555555555555L;
    private static final long MAGIC1 = 0x3333333333333333L;
    private static final long MAGIC2 = 0x0F0F0F0F0F0F0F0FL;
    private static final long MAGIC3 = 0x00FF00FF00FF00FFL;
    private static final long MAGIC4 = 0x0000FFFF0000FFFFL;
    private static final long MAGIC5 = 0x00000000FFFFFFFFL;
    private static final long MAGIC6 = 0xAAAAAAAAAAAAAAAAL;

    private static final long SHIFT0 = 1;
    private static final long SHIFT1 = 2;
    private static final long SHIFT2 = 4;
    private static final long SHIFT3 = 8;
    private static final long SHIFT4 = 16;

    public static long interleave(int even, int odd) {
        long v1 = 0x00000000FFFFFFFFL & even;
        long v2 = 0x00000000FFFFFFFFL & odd;
        v1 = (v1 | (v1 << SHIFT4)) & MAGIC4;
        v1 = (v1 | (v1 << SHIFT3)) & MAGIC3;
        v1 = (v1 | (v1 << SHIFT2)) & MAGIC2;
        v1 = (v1 | (v1 << SHIFT1)) & MAGIC1;
        v1 = (v1 | (v1 << SHIFT0)) & MAGIC0;
        v2 = (v2 | (v2 << SHIFT4)) & MAGIC4;
        v2 = (v2 | (v2 << SHIFT3)) & MAGIC3;
        v2 = (v2 | (v2 << SHIFT2)) & MAGIC2;
        v2 = (v2 | (v2 << SHIFT1)) & MAGIC1;
        v2 = (v2 | (v2 << SHIFT0)) & MAGIC0;

        return (v2<<1) | v1;
    }

    public static long deinterleave(long b) {
        b &= MAGIC0;
        b = (b ^ (b >>> SHIFT0)) & MAGIC1;
        b = (b ^ (b >>> SHIFT1)) & MAGIC2;
        b = (b ^ (b >>> SHIFT2)) & MAGIC3;
        b = (b ^ (b >>> SHIFT3)) & MAGIC4;
        b = (b ^ (b >>> SHIFT4)) & MAGIC5;
        return b;
    }

    public static long flipFlop(final long b) {
        return ((b & MAGIC6) >>> 1) | ((b & MAGIC0) << 1 );
    }

    public static int zigZagEncode(int i) {
        return (i >> 31) ^ (i << 1);
    }

    public static long zigZagEncode(long l) {
        return (l >> 63) ^ (l << 1);
    }

    public static int zigZagDecode(int i) {
        return ((i >>> 1) ^ -(i & 1));
    }

    public static long zigZagDecode(long l) {
        return ((l >>> 1) ^ -(l & 1));
    }
}
