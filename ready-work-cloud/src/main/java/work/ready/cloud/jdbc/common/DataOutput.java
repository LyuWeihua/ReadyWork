package work.ready.cloud.jdbc.common;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public abstract class DataOutput {

    public abstract void writeByte(byte b) throws IOException;

    public void writeBytes(byte[] b, int length) throws IOException {
        writeBytes(b, 0, length);
    }

    public abstract void writeBytes(byte[] b, int offset, int length) throws IOException;

    public void writeInt(int i) throws IOException {
        writeByte((byte)(i >> 24));
        writeByte((byte)(i >> 16));
        writeByte((byte)(i >>  8));
        writeByte((byte) i);
    }

    public void writeShort(short i) throws IOException {
        writeByte((byte)(i >>  8));
        writeByte((byte) i);
    }

    public final void writeVInt(int i) throws IOException {
        while ((i & ~0x7F) != 0) {
            writeByte((byte)((i & 0x7F) | 0x80));
            i >>>= 7;
        }
        writeByte((byte)i);
    }

    public final void writeZInt(int i) throws IOException {
        writeVInt(BitUtil.zigZagEncode(i));
    }

    public void writeLong(long i) throws IOException {
        writeInt((int) (i >> 32));
        writeInt((int) i);
    }

    public final void writeVLong(long i) throws IOException {
        if (i < 0) {
            throw new IllegalArgumentException("cannot write negative vLong (got: " + i + ")");
        }
        writeSignedVLong(i);
    }

    private void writeSignedVLong(long i) throws IOException {
        while ((i & ~0x7FL) != 0L) {
            writeByte((byte)((i & 0x7FL) | 0x80L));
            i >>>= 7;
        }
        writeByte((byte)i);
    }

    public final void writeZLong(long i) throws IOException {
        writeSignedVLong(BitUtil.zigZagEncode(i));
    }

    public void writeString(String s) throws IOException {
        final BytesRef utf8Result = new BytesRef(s);
        writeVInt(utf8Result.length);
        writeBytes(utf8Result.bytes, utf8Result.offset, utf8Result.length);
    }

    private static int COPY_BUFFER_SIZE = 16384;
    private byte[] copyBuffer;

    public void copyBytes(DataInput input, long numBytes) throws IOException {
        assert numBytes >= 0: "numBytes=" + numBytes;
        long left = numBytes;
        if (copyBuffer == null)
            copyBuffer = new byte[COPY_BUFFER_SIZE];
        while(left > 0) {
            final int toCopy;
            if (left > COPY_BUFFER_SIZE)
                toCopy = COPY_BUFFER_SIZE;
            else
                toCopy = (int) left;
            input.readBytes(copyBuffer, 0, toCopy);
            writeBytes(copyBuffer, 0, toCopy);
            left -= toCopy;
        }
    }

    public void writeMapOfStrings(Map<String,String> map) throws IOException {
        writeVInt(map.size());
        for (Map.Entry<String, String> entry : map.entrySet()) {
            writeString(entry.getKey());
            writeString(entry.getValue());
        }
    }

    public void writeSetOfStrings(Set<String> set) throws IOException {
        writeVInt(set.size());
        for (String value : set) {
            writeString(value);
        }
    }
}
