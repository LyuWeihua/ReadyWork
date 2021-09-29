package work.ready.cloud.jdbc.common;

import work.ready.core.tools.CollectionUtil;

import java.io.DataInputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;

public abstract class StringHelper {

    public static int bytesDifference(BytesRef priorTerm, BytesRef currentTerm) {
        int mismatch = Arrays.mismatch(priorTerm.bytes, priorTerm.offset, priorTerm.offset + priorTerm.length,
                currentTerm.bytes, currentTerm.offset, currentTerm.offset + currentTerm.length);
        if (mismatch < 0) {
            throw new IllegalArgumentException("terms out of order: priorTerm=" + priorTerm + ",currentTerm=" + currentTerm);
        }
        return mismatch;
    }

    public static int sortKeyLength(final BytesRef priorTerm, final BytesRef currentTerm) {
        return bytesDifference(priorTerm, currentTerm) + 1;
    }

    private StringHelper() {
    }

    public static boolean startsWith(byte[] ref, BytesRef prefix) {
        
        if (ref.length < prefix.length) {
            return false;
        }
        return Arrays.equals(ref, 0, prefix.length,
                prefix.bytes, prefix.offset, prefix.offset + prefix.length);
    }

    public static boolean startsWith(BytesRef ref, BytesRef prefix) {
        
        if (ref.length < prefix.length) {
            return false;
        }
        return Arrays.equals(ref.bytes, ref.offset, ref.offset + prefix.length,
                prefix.bytes, prefix.offset, prefix.offset + prefix.length);
    }

    public static boolean endsWith(BytesRef ref, BytesRef suffix) {
        int startAt = ref.length - suffix.length;
        
        if (startAt < 0) {
            return false;
        }
        return Arrays.equals(ref.bytes, ref.offset + startAt, ref.offset + startAt + suffix.length,
                suffix.bytes, suffix.offset, suffix.offset + suffix.length);
    }

    public static final int GOOD_FAST_HASH_SEED;

    static {
        String prop = System.getProperty("tests.seed");
        if (prop != null) {

            GOOD_FAST_HASH_SEED = prop.hashCode();
        } else {
            GOOD_FAST_HASH_SEED = (int) System.currentTimeMillis();
        }
    }

    @SuppressWarnings("fallthrough")
    public static int murmurhash3_x86_32(byte[] data, int offset, int len, int seed) {

        final int c1 = 0xcc9e2d51;
        final int c2 = 0x1b873593;

        int h1 = seed;
        int roundedEnd = offset + (len & 0xfffffffc);  

        for (int i=offset; i<roundedEnd; i+=4) {
            
            int k1 = (data[i] & 0xff) | ((data[i+1] & 0xff) << 8) | ((data[i+2] & 0xff) << 16) | (data[i+3] << 24);
            k1 *= c1;
            k1 = Integer.rotateLeft(k1, 15);
            k1 *= c2;

            h1 ^= k1;
            h1 = Integer.rotateLeft(h1, 13);
            h1 = h1*5+0xe6546b64;
        }

        int k1 = 0;

        switch(len & 0x03) {
            case 3:
                k1 = (data[roundedEnd + 2] & 0xff) << 16;
                
            case 2:
                k1 |= (data[roundedEnd + 1] & 0xff) << 8;
                
            case 1:
                k1 |= (data[roundedEnd] & 0xff);
                k1 *= c1;
                k1 = Integer.rotateLeft(k1, 15);
                k1 *= c2;
                h1 ^= k1;
        }

        h1 ^= len;

        h1 ^= h1 >>> 16;
        h1 *= 0x85ebca6b;
        h1 ^= h1 >>> 13;
        h1 *= 0xc2b2ae35;
        h1 ^= h1 >>> 16;

        return h1;
    }

    public static int murmurhash3_x86_32(BytesRef bytes, int seed) {
        return murmurhash3_x86_32(bytes.bytes, bytes.offset, bytes.length, seed);
    }

    private static BigInteger nextId;
    private static final BigInteger mask128;
    private static final Object idLock = new Object();

    static {
        
        byte[] maskBytes128 = new byte[16];
        Arrays.fill(maskBytes128, (byte) 0xff);
        mask128 = new BigInteger(1, maskBytes128);

        String prop = System.getProperty("tests.seed");

        long x0;
        long x1;

        if (prop != null) {

            if (prop.length() > 8) {
                prop = prop.substring(prop.length()-8);
            }
            x0 = Long.parseLong(prop, 16);
            x1 = x0;
        } else {
            
            try (DataInputStream is = new DataInputStream(Files.newInputStream(Paths.get("/dev/urandom")))) {
                x0 = is.readLong();
                x1 = is.readLong();
            } catch (Exception unavailable) {

                x0 = System.nanoTime();
                x1 = (long) StringHelper.class.hashCode() << 32;

                StringBuilder sb = new StringBuilder();
                
                try {
                    Properties p = System.getProperties();
                    for (String s: p.stringPropertyNames()) {
                        sb.append(s);
                        sb.append(p.getProperty(s));
                    }
                    x1 |= sb.toString().hashCode();
                } catch (SecurityException notallowed) {
                    
                    x1 |= StringBuffer.class.hashCode();
                }
            }
        }

        for(int i=0;i<10;i++) {
            long s1 = x0;
            long s0 = x1;
            x0 = s0;
            s1 ^= s1 << 23; 
            x1 = s1 ^ s0 ^ (s1 >>> 17) ^ (s0 >>> 26); 
        }

        byte[] maskBytes64 = new byte[8];
        Arrays.fill(maskBytes64, (byte) 0xff);
        BigInteger mask64 = new BigInteger(1, maskBytes64);

        BigInteger unsignedX0 = BigInteger.valueOf(x0).and(mask64);
        BigInteger unsignedX1 = BigInteger.valueOf(x1).and(mask64);

        nextId = unsignedX0.shiftLeft(64).or(unsignedX1);
    }

    public static final int ID_LENGTH = 16;

    public static byte[] randomId() {

        byte bits[];
        synchronized(idLock) {
            bits = nextId.toByteArray();
            nextId = nextId.add(BigInteger.ONE).and(mask128);
        }

        if (bits.length > ID_LENGTH) {
            assert bits.length == ID_LENGTH + 1;
            assert bits[0] == 0;
            return CollectionUtil.copyOfSubArray(bits, 1, bits.length);
        } else {
            byte[] result = new byte[ID_LENGTH];
            System.arraycopy(bits, 0, result, result.length - bits.length, bits.length);
            return result;
        }
    }

    public static String idToString(byte id[]) {
        if (id == null) {
            return "(null)";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(new BigInteger(1, id).toString(Character.MAX_RADIX));
            if (id.length != ID_LENGTH) {
                sb.append(" (INVALID FORMAT)");
            }
            return sb.toString();
        }
    }

    public static BytesRef intsRefToBytesRef(IntsRef ints) {
        byte[] bytes = new byte[ints.length];
        for(int i=0;i<ints.length;i++) {
            int x = ints.ints[ints.offset+i];
            if (x < 0 || x > 255) {
                throw new IllegalArgumentException("int at pos=" + i + " with value=" + x + " is out-of-bounds for byte");
            }
            bytes[i] = (byte) x;
        }

        return new BytesRef(bytes);
    }
}
