/**
 *
 * Original work Copyright (c) 2016 Network New Technologies Inc.
 * Modified Copyright (c) 2020 WeiHua Lyu [ready.work]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package work.ready.core.tools;

import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Ready;

import java.io.*;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class NioUtil {
    private static final int BUFFER_SIZE = 1024 * 4;

    private static final Log logger = LogFactory.getLog(NioUtil.class);

    private static FileSystem createZipFileSystem(String zipFilename, boolean create) throws IOException {

        final Path path = Paths.get(zipFilename);
        if(Files.notExists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }
        final URI uri = URI.create("jar:file:" + path.toUri().getPath());

        final Map<String, String> env = new HashMap<>();
        if (create) {
            env.put("create", "true");
        }
        return FileSystems.newFileSystem(uri, env);
    }

    public static void unzip(String zipFilename, String destDirname)
            throws IOException{

        final Path destDir = Paths.get(destDirname);
        
        if(Files.notExists(destDir)){
            if(logger.isDebugEnabled()) logger.debug(destDir + " does not exist. Creating...");
            Files.createDirectories(destDir);
        }

        try (FileSystem zipFileSystem = createZipFileSystem(zipFilename, false)){
            final Path root = zipFileSystem.getPath("/");

            Files.walkFileTree(root, new SimpleFileVisitor<Path>(){
                @Override
                public FileVisitResult visitFile(Path file,
                                                 BasicFileAttributes attrs) throws IOException {
                    final Path destFile = Paths.get(destDir.toString(),
                            file.toString());
                    if(logger.isDebugEnabled()) logger.debug("Extracting file %s to %s", file, destFile);
                    Files.copy(file, destFile, StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir,
                                                         BasicFileAttributes attrs) throws IOException {
                    final Path dirToCreate = Paths.get(destDir.toString(),
                            dir.toString());
                    if(Files.notExists(dirToCreate)){
                        if(logger.isDebugEnabled()) logger.debug("Creating directory %s", dirToCreate);
                        Files.createDirectory(dirToCreate);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    public static void create(String zipFilename, String... filenames)
            throws IOException {

        try (FileSystem zipFileSystem = createZipFileSystem(zipFilename, true)) {
            final Path root = zipFileSystem.getPath("/");

            for (String filename : filenames) {
                final Path src = Paths.get(filename);

                if(!Files.isDirectory(src)){
                    final Path dest = zipFileSystem.getPath(root.toString(),
                            src.toString());
                    final Path parent = dest.getParent();
                    if(Files.notExists(parent)){
                        if(logger.isDebugEnabled()) logger.debug("Creating directory %s", parent);
                        Files.createDirectories(parent);
                    }
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                }
                else{
                    
                    Files.walkFileTree(src, new SimpleFileVisitor<Path>(){
                        @Override
                        public FileVisitResult visitFile(Path file,
                                                         BasicFileAttributes attrs) throws IOException {
                            final Path dest = zipFileSystem.getPath(root.toString(),
                                    file.toString());
                            Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult preVisitDirectory(Path dir,
                                                                 BasicFileAttributes attrs) throws IOException {
                            final Path dirToCreate = zipFileSystem.getPath(root.toString(),
                                    dir.toString());
                            if(Files.notExists(dirToCreate)){
                                if(logger.isDebugEnabled()) logger.debug("Creating directory %s\n", dirToCreate);
                                Files.createDirectories(dirToCreate);
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
                }
            }
        }
    }

    public static void list(String zipFilename) throws IOException{

        if(logger.isDebugEnabled()) logger.debug("Listing Archive:  %s",zipFilename);
        
        try (FileSystem zipFileSystem = createZipFileSystem(zipFilename, false)) {

            final Path root = zipFileSystem.getPath("/");

            Files.walkFileTree(root, new SimpleFileVisitor<Path>(){
                @Override
                public FileVisitResult visitFile(Path file,
                                                 BasicFileAttributes attrs) throws IOException {
                    print(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir,
                                                         BasicFileAttributes attrs) throws IOException {
                    print(dir);
                    return FileVisitResult.CONTINUE;
                }

                private void print(Path file) throws IOException{
                    final DateFormat df = new SimpleDateFormat("MM/dd/yyyy-HH:mm:ss");
                    final String modTime= df.format(new Date(
                            Files.getLastModifiedTime(file).toMillis()));
                    if(logger.isDebugEnabled()) {
                        logger.debug("%d  %s  %s",
                                Files.size(file),
                                modTime,
                                file);
                    }
                }
            });
        }
    }

    public static void deleteOldFiles(String dirPath, int olderThanMinute)  {

        File folder = new File(dirPath);
        if (folder.exists()) {
            File[] listFiles = folder.listFiles();
            long eligibleForDeletion = Ready.currentTimeMillis() - (olderThanMinute * 60 * 1000L);
            for (File listFile: listFiles) {
                if (listFile.lastModified() < eligibleForDeletion) {
                    if (!listFile.delete()) {
                        logger.error("Unable to delete file %s", listFile);
                    }
                }
            }
        }
    }

    public static ByteBuffer toByteBuffer(String s) {
        
        return ByteBuffer.wrap(s.getBytes(UTF_8));
    }

    public static ByteBuffer toByteBuffer(File file) {
        ByteBuffer buffer = ByteBuffer.allocateDirect((int) file.length());
        try {
            buffer.put(toByteArray(new FileInputStream(file)));
        } catch (IOException e) {
            logger.error("Failed to write file to byte array: " + e.getMessage());
        }
        buffer.flip();
        return buffer;
    }

    public static String getTempDir() {
        
        String tempDir = System.getProperty("user.home");
        try{
            
            File temp = File.createTempFile("A0393939", ".tmp");
            
            String absolutePath = temp.getAbsolutePath();
            tempDir = absolutePath.substring(0,absolutePath.lastIndexOf(File.separator));
        }catch(IOException e){}
        return tempDir;
    }

    public static byte[] toByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            byte[] b = new byte[BUFFER_SIZE];
            int n = 0;
            while ((n = is.read(b)) != -1) {
                output.write(b, 0, n);
            }
            return output.toByteArray();
        } finally {
            output.close();
        }
    }

    public static String toString(InputStream is) throws IOException {
        return new String(toByteArray(is), StandardCharsets.UTF_8);
    }

}
