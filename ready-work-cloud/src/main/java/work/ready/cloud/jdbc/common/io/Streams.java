package work.ready.cloud.jdbc.common.io;

import work.ready.cloud.jdbc.olap.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class Streams {

    private static final ThreadLocal<byte[]> buffer = ThreadLocal.withInitial(() -> new byte[8 * 1024]);

    private Streams() {

    }

    public static long copy(final InputStream in, final OutputStream out, byte[] buffer, boolean close) throws IOException {
        Exception err = null;
        try {
            long byteCount = 0;
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                byteCount += bytesRead;
            }
            out.flush();
            return byteCount;
        } catch (IOException | RuntimeException e) {
            err = e;
            throw e;
        } finally {
            if (close) {
                IOUtils.close(err, in, out);
            }
        }
    }

    public static long copy(final InputStream in, final OutputStream out, boolean close) throws IOException {
        return copy(in, out, buffer.get(), close);
    }

    public static long copy(final InputStream in, final OutputStream out, byte[] buffer) throws IOException {
        return copy(in, out, buffer, true);
    }

    public static long copy(final InputStream in, final OutputStream out) throws IOException {
        return copy(in, out, buffer.get(), true);
    }

    public static final int BUFFER_SIZE = 1024 * 8;

    public static final OutputStream NULL_OUTPUT_STREAM = new OutputStream() {
        @Override
        public void write(int b) {
            
        }
        @Override
        public void write(byte[] b, int off, int len) {
            
        }
    };

    public static void copy(byte[] in, OutputStream out) throws IOException {
        Objects.requireNonNull(in, "No input byte array specified");
        Objects.requireNonNull(out, "No OutputStream specified");
        try (OutputStream out2 = out) {
            out2.write(in);
        }
    }

    public static int copy(Reader in, Writer out) throws IOException {
        Objects.requireNonNull(in, "No Reader specified");
        Objects.requireNonNull(out, "No Writer specified");

        try (Reader in2 = in; Writer out2 = out) {
            return doCopy(in2, out2);
        }
    }

    private static int doCopy(Reader in, Writer out) throws IOException {
        int byteCount = 0;
        char[] buffer = new char[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
            byteCount += bytesRead;
        }
        out.flush();
        return byteCount;
    }

    public static void copy(String in, Writer out) throws IOException {
        Objects.requireNonNull(in, "No input String specified");
        Objects.requireNonNull(out, "No Writer specified");
        try (Writer out2 = out) {
            out2.write(in);
        }
    }

    public static String copyToString(Reader in) throws IOException {
        StringWriter out = new StringWriter();
        copy(in, out);
        return out.toString();
    }

    public static int readFully(Reader reader, char[] dest) throws IOException {
        return readFully(reader, dest, 0, dest.length);
    }

    public static int readFully(Reader reader, char[] dest, int offset, int len) throws IOException {
        int read = 0;
        while (read < len) {
            final int r = reader.read(dest, offset + read, len - read);
            if (r == -1) {
                break;
            }
            read += r;
        }
        return read;
    }

    public static int readFully(InputStream reader, byte[] dest) throws IOException {
        return readFully(reader, dest, 0, dest.length);
    }

    public static int readFully(InputStream reader, byte[] dest, int offset, int len) throws IOException {
        int read = 0;
        while (read < len) {
            final int r = reader.read(dest, offset + read, len - read);
            if (r == -1) {
                break;
            }
            read += r;
        }
        return read;
    }

    public static long consumeFully(InputStream inputStream) throws IOException {
        return copy(inputStream, NULL_OUTPUT_STREAM);
    }

    public static List<String> readAllLines(InputStream input) throws IOException {
        final List<String> lines = new ArrayList<>();
        readAllLines(input, lines::add);
        return lines;
    }

    public static void readAllLines(InputStream input, Consumer<String> consumer) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                consumer.accept(line);
            }
        }
    }

    public static InputStream noCloseStream(InputStream stream) {
        return new FilterInputStream(stream) {
            @Override
            public void close() {
                
            }
        };
    }

    public static OutputStream noCloseStream(OutputStream stream) {
        return new FilterOutputStream(stream) {

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                out.write(b, off, len);
            }

            @Override
            public void close() {
                
            }
        };
    }

    static class LimitedInputStream extends FilterInputStream {

        private static final long NO_MARK = -1L;

        private long currentLimit; 
        private long limitOnLastMark;

        LimitedInputStream(InputStream in, long limit) {
            super(in);
            if (limit < 0L) {
                throw new IllegalArgumentException("limit must be non-negative");
            }
            this.currentLimit = limit;
            this.limitOnLastMark = NO_MARK;
        }

        @Override
        public int read() throws IOException {
            final int result;
            if (currentLimit == 0 || (result = in.read()) == -1) {
                return -1;
            } else {
                currentLimit--;
                return result;
            }
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            final int result;
            if (currentLimit == 0 || (result = in.read(b, off, Math.toIntExact(Math.min(len, currentLimit)))) == -1) {
                return -1;
            } else {
                currentLimit -= result;
                return result;
            }
        }

        @Override
        public long skip(long n) throws IOException {
            final long skipped = in.skip(Math.min(n, currentLimit));
            currentLimit -= skipped;
            return skipped;
        }

        @Override
        public int available() throws IOException {
            return Math.toIntExact(Math.min(in.available(), currentLimit));
        }

        @Override
        public void close() throws IOException {
            in.close();
        }

        @Override
        public synchronized void mark(int readlimit) {
            in.mark(readlimit);
            limitOnLastMark = currentLimit;
        }

        @Override
        public synchronized void reset() throws IOException {
            in.reset();
            if (limitOnLastMark != NO_MARK) {
                currentLimit = limitOnLastMark;
            }
        }
    }
}
