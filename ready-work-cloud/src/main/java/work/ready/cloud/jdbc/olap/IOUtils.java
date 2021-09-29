package work.ready.cloud.jdbc.olap;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class IOUtils {

    public static final String UTF_8 = StandardCharsets.UTF_8.name();

    private IOUtils() {
        
    }

    public static void close(final Closeable... objects) throws IOException {
        close(null, Arrays.asList(objects));
    }

    public static void close(Closeable closeable) throws IOException {
        if (closeable != null) {
            closeable.close();
        }
    }

    public static void close(final Exception e, final Closeable... objects) throws IOException {
        close(e, Arrays.asList(objects));
    }

    public static void close(final Iterable<? extends Closeable> objects) throws IOException {
        close(null, objects);
    }

    public static void close(final Exception ex, final Iterable<? extends Closeable> objects) throws IOException {
        Exception firstException = ex;
        for (final Closeable object : objects) {
            try {
                close(object);
            } catch (final IOException | RuntimeException e) {
                if (firstException == null) {
                    firstException = e;
                } else {
                    firstException.addSuppressed(e);
                }
            }
        }

        if (firstException != null) {
            if (firstException instanceof IOException) {
                throw (IOException) firstException;
            } else {
                
                throw (RuntimeException) firstException;
            }
        }
    }

    public static void closeWhileHandlingException(final Closeable... objects) {
        closeWhileHandlingException(Arrays.asList(objects));
    }

    public static void closeWhileHandlingException(final Iterable<? extends Closeable> objects) {
        for (final Closeable object : objects) {
            closeWhileHandlingException(object);
        }
    }

    public static void closeWhileHandlingException(final Closeable closeable) {
        
        try {
            close(closeable);
        } catch (final IOException | RuntimeException e) {
        }
    }

    public static void deleteFilesIgnoringExceptions(final Path... files) {
        deleteFilesIgnoringExceptions(Arrays.asList(files));
    }

    public static void deleteFilesIgnoringExceptions(final Collection<? extends Path> files) {
        for (final Path name : files) {
            if (name != null) {
                
                try {
                    Files.delete(name);
                } catch (final IOException ignored) {

                }
            }
        }
    }

    public static void rm(final Path... locations) throws IOException {
        final LinkedHashMap<Path,Throwable> unremoved = rm(new LinkedHashMap<>(), locations);
        if (!unremoved.isEmpty()) {
            final StringBuilder b = new StringBuilder("could not remove the following files (in the order of attempts):\n");
            for (final Map.Entry<Path,Throwable> kv : unremoved.entrySet()) {
                b.append("   ")
                        .append(kv.getKey().toAbsolutePath())
                        .append(": ")
                        .append(kv.getValue())
                        .append("\n");
            }
            throw new IOException(b.toString());
        }
    }

    private static LinkedHashMap<Path,Throwable> rm(final LinkedHashMap<Path,Throwable> unremoved, final Path... locations) {
        if (locations != null) {
            for (final Path location : locations) {
                
                if (location != null && Files.exists(location)) {
                    try {
                        Files.walkFileTree(location, new FileVisitor<Path>() {
                            @Override
                            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult postVisitDirectory(final Path dir, final IOException impossible) throws IOException {
                                assert impossible == null;

                                try {
                                    Files.delete(dir);
                                } catch (final IOException e) {
                                    unremoved.put(dir, e);
                                }
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                                try {
                                    Files.delete(file);
                                } catch (final IOException exc) {
                                    unremoved.put(file, exc);
                                }
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
                                if (exc != null) {
                                    unremoved.put(file, exc);
                                }
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } catch (final IOException impossible) {
                        throw new AssertionError("visitor threw exception", impossible);
                    }
                }
            }
        }
        return unremoved;
    }

    public static final boolean WINDOWS = System.getProperty("os.name").startsWith("Windows");
    public static final boolean LINUX = System.getProperty("os.name").startsWith("Linux");
    public static final boolean MAC_OS_X = System.getProperty("os.name").startsWith("Mac OS X");

    public static void fsync(final Path fileToSync, final boolean isDir) throws IOException {
        if (isDir && WINDOWS) {
            
            if (Files.exists(fileToSync) == false) {
                
                throw new NoSuchFileException(fileToSync.toString());
            }
            return;
        }
        try (FileChannel file = FileChannel.open(fileToSync, isDir ? StandardOpenOption.READ : StandardOpenOption.WRITE)) {
            try {
                file.force(true);
            } catch (final IOException e) {
                if (isDir) {
                    assert (LINUX || MAC_OS_X) == false :
                            "on Linux and MacOSX fsyncing a directory should not throw IOException, "+
                                    "we just don't want to rely on that in production (undocumented); got: " + e;
                    
                    return;
                }
                
                throw e;
            }
        }
    }
}
