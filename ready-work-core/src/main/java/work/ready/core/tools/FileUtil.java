/**
 * Copyright (c) 2011-2019, James Zhan 詹波 (jfinal@126.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package work.ready.core.tools;

import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Constant;

import java.io.*;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.zip.*;

public class FileUtil {

	private static final Log logger = LogFactory.getLog(FileUtil.class);
	public static String	unixLineSeparator		= "\n";
	public static String	dosLineSeparator		= "\r\n";
	public static String	defaultlineSeparator	= System.getProperty("line.separator");
	public static String	lineSeparator		= defaultlineSeparator;
	public static String	defaultCharsetName	= Charset.defaultCharset().name();

	public static void delete(File file) {
		if (file != null && file.exists()) {
			if (file.isFile()) {
				file.delete();
			}
			else if (file.isDirectory()) {
				File files[] = file.listFiles();
				if (files != null) {
					for (int i=0; i<files.length; i++) {
						delete(files[i]);
					}
				}
				file.delete();
			}
		}
	}

	public static String getFileExtension(String fileFullName) {
		if (StrUtil.isBlank(fileFullName)) {
			throw new RuntimeException("fileFullName is empty");
		}
		return  getFileExtension(new File(fileFullName));
	}

	public static String getFileExtension(File file) {
		if (null == file) {
			throw new NullPointerException();
		}
		String fileName = file.getName();
		int dotIdx = fileName.lastIndexOf('.');
		return (dotIdx == -1) ? "" : fileName.substring(dotIdx + 1);
	}

	public static String withExtension(String filePath, String extension) {
		if (filePath.toLowerCase().endsWith(extension)) {
			return filePath;
		}
		return removeExtension(filePath) + extension;
	}

	public static String removeExtension(String filePath) {
		int fileNameStart = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
		int extensionPos = filePath.lastIndexOf('.');

		if (extensionPos > fileNameStart) {
			return filePath.substring(0, extensionPos);
		}
		return filePath;
	}

	public static void clear(File file) {
		if(file == null || !file.exists()) {
			return;
		}
		if(file.isFile()) {
			tryDeleteFile(file, 3);
		}else {
			for(File subfile : file.listFiles()) {
				if(subfile.isDirectory()) {
					clear(subfile);
				}else {
					tryDeleteFile(subfile, 3);
				}
			}
			tryDeleteFile(file, 3);
		}
	}

	public static void close(Closeable closeable) {
		if(closeable == null) {
			return;
		}
		try {
			closeable.close();
		} catch (IOException e) {
			logger.warn("fail to close %s, ex: %s", closeable, e.getMessage());
		}
	}

	public static void copyStream(InputStream in, OutputStream out) throws IOException {
		int bufSize = 4 * 1024, bytesRead = -1;
		byte[] buf = new byte[bufSize];
		while((bytesRead = in.read(buf, 0, bufSize)) != -1) {
			out.write(buf, 0, bytesRead);
		}
	}

	public static boolean copyFile(File in, File out) {
		boolean state = false;
		int i = 1024;
		long mbs = in.length() / i / i;
		int three = 3;
		if(mbs <= three) {
			state = copySmallFile(in, out);
		}else if(mbs <= i) {
			state = copyChannelFile(in, out);
			if(!state) {
				state = copyBigFile(in, out);
			}
		}else {
			state = copyBigFile(in, out);
		}
		return state;
	}

	public static boolean copySmallFile(File in, File out) {
		if(!in.exists() || !in.isFile()) {
			return false;
		}
		if(out.exists()) {
			out.delete();
		}
		FileInputStream fis = null;
		FileOutputStream fos = null;
		try {
			fis = new FileInputStream(in);
			fos= new FileOutputStream(out);
			copyStream(fis, fos);
			return true;
		}catch(IOException e) {
			logger.warn("fail to copy small file: %s to: %s, ex: %s", in, out, e.getMessage());
			return false;
		}finally {
			close(fis);
			close(fos);
		}
	}

	public static boolean copyChannelFile(File srcFile, File dstFile) {
		try(
				FileInputStream fin = new FileInputStream(srcFile);
				FileOutputStream fout = new FileOutputStream(dstFile);
				FileChannel srcChannel = fin.getChannel();
				FileChannel dstChannel = fout.getChannel();
		){
			dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
			return true;
		} catch (Exception e) {
			logger.warn("fail to copy channel file: %s to: %s, ex: %s", srcFile, dstFile, e.getMessage());
			return false;
		}
	}

	public static boolean copyBigFile(File in, File out) {
		if(!in.exists() || !in.isFile()) {
			return false;
		}
		if(out.exists()) {
			out.delete();
		}
		FileInputStream fis = null;
		FileOutputStream fos = null;
		try {
			fis = new FileInputStream(in);
			fos= new FileOutputStream(out);
			int bufSize = 4 * 1024 * 1024, bytesRead = -1;
			byte[] buf = new byte[bufSize];
			while((bytesRead = fis.read(buf, 0, bufSize)) != -1) {
				fos.write(buf, 0, bytesRead);
			}
			return true;
		}catch(IOException e) {
			logger.warn("fail to copy big file: %s to: %s, ex: %s", in, out, e.getMessage());
			return false;
		}finally {
			close(fis);
			close(fos);
		}
	}

	public static void copyDir(File in, File out) {
		if(!in.exists() || !in.isDirectory()) {
			return;
		}
		if(!out.exists()) {
			out.mkdirs();
		}
		for(File file : in.listFiles()) {
			File target = new File(out, file.getName());
			if(file.isFile()) {
				copyFile(file, target);
			}else {
				copyDir(file, target);
			}
		}
	}

	public static Path createIfNotExists(Path file, FileAttribute<?>... attributes) throws IOException {
		Objects.requireNonNull(file, "'file' must not be null");
		Objects.requireNonNull(attributes, "'attributes' must not be null");
		try {
			return Files.createFile(file, attributes);
		}
		catch (FileAlreadyExistsException ex) {
			return file;
		}
	}

	public static boolean delete(Path path) throws IOException {
		if (path == null) {
			return false;
		}
		if (!Files.exists(path)) {
			return false;
		}
		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.deleteIfExists(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException ex) throws IOException {
				if (ex != null) {
					throw ex;
				}
				Files.deleteIfExists(dir);
				return FileVisitResult.CONTINUE;
			}
		});

		return true;
	}

	public static void copy(Path src, Path dest, BiPredicate<? super Path, ? super BasicFileAttributes> matcher) throws IOException {
		Objects.requireNonNull(src, "'src' must not be null");
		Objects.requireNonNull(dest, "'dest' must not be null");
		Files.walkFileTree(src, new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attrs) throws IOException {
				if (matcher == null || matcher.test(directory, attrs)) {
					Files.createDirectories(dest.resolve(src.relativize(directory)));
					return FileVisitResult.CONTINUE;
				}
				return FileVisitResult.SKIP_SUBTREE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (matcher == null || matcher.test(file, attrs)) {
					Files.copy(file, dest.resolve(src.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
				}
				return FileVisitResult.CONTINUE;
			}
		});

	}

	public static boolean tryDeleteFile(File file, int tryCount) {
		if(!file.exists()) {
			return false;
		}
		boolean deleteSucceed = false;
		while(!deleteSucceed && tryCount-- > 0) {
			deleteSucceed = file.delete();
			if(deleteSucceed) {
				return true;
			}else {
				nap(100);
			}
		}

		if(deleteSucceed==false) {
			logger.info("fail to delete file: %s, try count: %s", file, tryCount);
		}
		return deleteSucceed;
	}

	public static boolean tryRenameTo(File oldFile, File newFile, int tryCount) {
		if(!oldFile.exists() || oldFile.isDirectory() || newFile.isDirectory()) {
			return false;
		}
		if(newFile.exists()) {
			boolean deleteSucceed = tryDeleteFile(newFile, tryCount);
			if(deleteSucceed == false) {
				return false;
			}
		}
		boolean renameSucceed = false;
		while(!renameSucceed && tryCount-- > 0) {
			renameSucceed = oldFile.renameTo(newFile);
			if(renameSucceed) {
				return true;
			}else {
				nap(100);
			}
		}
		if(renameSucceed==false) {
			logger.info("fail to rename file: %s to: %s, try count: %s", oldFile, newFile, tryCount);
		}
		return renameSucceed;
	}

	public static boolean tryCopyFile(File source, File target, int tryCount) {
		if(!source.exists() || source.isDirectory() || target.isDirectory()) {
			return false;
		}
		boolean copySucceed = false;
		while(!copySucceed && tryCount-- > 0) {
			if(target.exists() && target.delete() == false) {
				nap(100);
				continue;
			}else {
				copySucceed = copyFile(source, target);
				if(copySucceed) {
					return true;
				}else {
					nap(100);
				}
			}
		}

		if(copySucceed==false) {
			logger.info("fail to copy file: %s to: %s, try count: %s", source, target, tryCount);
		}
		return copySucceed;
	}

	public static void nap(long milliseconds) {
		try {
			Thread.sleep(milliseconds);
		}catch(Exception e) {
			logger.warn("fail to nap millis: %s, ex: %s", milliseconds, e.getMessage());
		}
	}

	public static List<String> readLines(File textfile, String charsetName) {
		List<String> lines = new LinkedList<String>();
		String content = readString(textfile, charsetName);
		if(content != null) {
			StringTokenizer tokens = new StringTokenizer(content, "\r\n");
			while(tokens.hasMoreTokens()) {
				String token = tokens.nextToken();
				if(token != null) {
					lines.add(token);
				}
			}
		}
		return lines;
	}

	public static List<String> readLines(InputStream inputStream, String charsetName) {
		List<String> lines = new LinkedList<String>();
		String content = readString(inputStream, charsetName);
		if(content != null) {
			StringTokenizer tokens = new StringTokenizer(content, "\r\n");
			while(tokens.hasMoreTokens()) {
				String token = tokens.nextToken();
				if(token != null) {
					lines.add(token);
				}
			}
		}
		return lines;
	}

	public static Object[] readObject(File file) {
		List<Object> objs = new ArrayList<>();
		try(ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))){
			Object obj = null;
			while((obj=in.readObject())!=null) {
				objs.add(obj);
			}
		}catch(EOFException e) {

		}catch(Exception e) {
			logger.warn("fail to read object file: %s", e.getMessage());
		}
		return objs.toArray();
	}

	public static ByteArrayOutputStream readStream(File file) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			copyStream(fis, baos);
		}
		catch (IOException e) {
			logger.warn("fail to read stream from file: %s, ex: %s", file, e.getMessage());
		}finally {
			close(fis);
		}
		return baos;
	}

	public static ByteArrayOutputStream readStream(InputStream in) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			copyStream(in, baos);
		}catch(Exception e) {
			logger.warn("fail to read stream from inputstream: %s, ex: %s", in, e.getMessage());
		}finally {
			close(in);
		}
		return baos;
	}

	public static String readString(File textfile, String charsetName) {
		String content = null;
		ByteArrayOutputStream baos = readStream(textfile);
		try {
			content = new String(baos.toByteArray(), charsetName);
		} catch (IOException e) {
			logger.warn("fail to read string from file: %s, charset: %s, ex: %s", textfile, charsetName, e.getMessage());
		}
		return content;
	}

	public static String readString(InputStream inputStream, String charsetName) {
		String content = null;
		ByteArrayOutputStream baos = readStream(inputStream);
		try {
			content = new String(baos.toByteArray(), charsetName);
		} catch (IOException e) {
			logger.warn("fail to read string from inputstream: %s, charset: %s, ex: %s", inputStream, charsetName, e.getMessage());
		}
		return content;
	}

	public static String digest(File file, String algorithm) {
		byte[] digest = null;
		if(file.exists() && file.isFile()) {
			FileInputStream fis = null;
			try {
				MessageDigest md = MessageDigest.getInstance(algorithm);
				fis = new FileInputStream(file);
				int bufSize = 4 * 1024, bytesRead = -1;
				byte[] buf = new byte[bufSize];
				while((bytesRead = fis.read(buf, 0, bufSize)) != -1) {
					md.update(buf, 0, bytesRead);
				}
				digest = md.digest();
			} catch (Exception e) {
				logger.warn("fail to digest file: %s, algo: %s, ex: %s", file, algorithm, e.getMessage());
			}finally {
				close(fis);
			}
		}
		return StrUtil.toHexString(digest);
	}

	public static void writeObject(File file, Object ... objs) {
		try(ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))){
			for(Object obj : objs ) {
				out.writeObject(obj);
			}
		}catch(Exception e) {
			logger.warn("fail to write object to file, ex: %s", e.getMessage());
		}
	}

	public static OutputStream writeStream(File file) {
		try {
			File parentFile = file.getParentFile();
			if(!parentFile.exists()) {
				parentFile.mkdirs();
			}
			return new FileOutputStream(file);
		}catch (Exception e) {
			logger.info("fail to open write stream to file: %s, ex: %s", file, e.getMessage());
		}
		return null;
	}

	public static void writeString(File file, String content, String charsetName) {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file, file.exists());
			copyStream(new ByteArrayInputStream(content.getBytes(charsetName)), fos);
		} catch (IOException e) {
			logger.warn("fail to write string to file: %s, charset: %s, ex: %s", file, charsetName, e.getMessage());
		}finally {
			close(fos);
		}
	}

	public static void writeStream(File file, InputStream in) {
		OutputStream fos = null;
		try {
			fos = writeStream(file);
			if(fos!=null) {
				copyStream(in, fos);
			}
		} catch (IOException e) {
			logger.warn("fail to write stream to file: %s, ex: %s", file, e.getMessage());
		}finally {
			close(fos);
		}
	}

	public static void writeBytes(File file, byte[] bytes) {
		OutputStream fos = null;
		try {
			fos = writeStream(file);
			if(fos!=null) {
				fos.write(bytes);
			}
		} catch (IOException e) {
			logger.warn("fail to write bytes to file: %s, ex: %s", file, e.getMessage());
		}finally {
			close(fos);
		}
	}

	public static void unZip(File zip, File parent) {
		if(!zip.exists() || !zip.isFile()) {
			return;
		}
		ZipInputStream zis = null;
		ZipEntry ze = null;
		try {
			zis = new ZipInputStream(new ByteArrayInputStream(readStream(zip).toByteArray()));
			while((ze = zis.getNextEntry()) != null) {
				File target = new File(parent, ze.getName());
				if(ze.isDirectory()) {
					target.mkdirs();
				}else {
					FileOutputStream fos = null;
					try {
						fos = new FileOutputStream(target);
						copyStream(zis, fos);
					}catch(IOException e) {
						logger.warn("fail to unzip file: %s, entry: %s, ex: %s", zip, ze.getName(), e.getMessage());
					}finally {
						close(fos);
					}
				}
			}
		} catch (IOException e) {
			logger.warn("fail to unzip file: %s, ex: %s", zip, e.getMessage());
		}finally {
			close(zis);
		}
	}

	public static void zip(File file, File zip) {
		if(!file.exists()) {
			return;
		}
		ZipOutputStream zos = null;
		File current = null;
		try {
			zos = new ZipOutputStream(new FileOutputStream(zip));
			zos.setLevel(Deflater.BEST_COMPRESSION);
			Queue<File> files = new LinkedList<File>();
			files.add(file);
			int pos = file.getCanonicalFile().getParent().length() + 1;
			while((current = files.poll()) != null) {
				if(current.isFile()) {
					zos.putNextEntry(new ZipEntry(current.getCanonicalPath().substring(pos)));
					InputStream is = new ByteArrayInputStream(readStream(current).toByteArray());
					copyStream(is, zos);
					zos.closeEntry();
				}else {
					zos.putNextEntry(new ZipEntry(current.getCanonicalPath().substring(pos) + "/"));
					zos.closeEntry();
					for(File f : current.listFiles()) {
						files.add(f);
					}
				}
			}
		}catch(IOException e) {
			logger.warn("fail to zip file: %s to: %s, ex: %s", file, zip, e.getMessage());
		}finally {
			close(zos);
		}
	}

	public static String getFileName(File file) {
		String name = file.getName();
		int lastIndexOfDot = name.lastIndexOf('.');
		return lastIndexOfDot != -1 ? name.substring(0, lastIndexOfDot) : name;
	}

	public static String getFileName(String name) {
		return getFileName(new File(name));
	}

	public static String getFileExt(File file) {
		return getFileExt(file.getName());
	}

	public static String getFileExt(String name) {
		int lastIndexOfDot = name.lastIndexOf('.');
		return lastIndexOfDot != -1 ? name.substring(lastIndexOfDot + 1) : "";
	}

	public static String removePrefix(String src, String prefix) {
		if (src != null && src.startsWith(prefix)) {
			return src.substring(prefix.length());
		}
		return src;
	}

	public static String removeRootPath(String src) {
		return removePrefix(src, PathUtil.getProjectRootPath());
	}

	public static String readString(File file) {
		ByteArrayOutputStream baos = null;
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			baos = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			for (int len = 0; (len = fis.read(buffer)) > 0; ) {
				baos.write(buffer, 0, len);
			}
			return new String(baos.toByteArray(), Constant.DEFAULT_CHARSET);
		} catch (Exception e) {
		} finally {
			close(fis, baos);
		}
		return null;
	}

	public static void writeString(File file, String string) {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file, false);
			fos.write(string.getBytes(Constant.DEFAULT_CHARSET));
		} catch (Exception e) {
		} finally {
			close(null, fos);
		}
	}

	private static void close(InputStream is, OutputStream os) {
		if (is != null)
			try {
				is.close();
			} catch (IOException e) {
			}
		if (os != null)
			try {
				os.close();
			} catch (IOException e) {
			}
	}

	public static void unzip(String zipFilePath) throws IOException {
		String targetPath = zipFilePath.substring(0, zipFilePath.lastIndexOf("."));
		unzip(zipFilePath, targetPath, true);
	}

	public static void unzip(String zipFilePath, String targetPath) throws IOException {
		unzip(zipFilePath, targetPath, true);
	}

	public static void unzip(String zipFilePath, String targetPath, boolean safeUnzip) throws IOException {
		ZipFile zipFile = new ZipFile(zipFilePath);
		try {
			Enumeration<?> entryEnum = zipFile.entries();
			if (null != entryEnum) {
				while (entryEnum.hasMoreElements()) {
					OutputStream os = null;
					InputStream is = null;
					try {
						ZipEntry zipEntry = (ZipEntry) entryEnum.nextElement();
						if (!zipEntry.isDirectory()) {
							if (safeUnzip && isNotSafeFile(zipEntry.getName())) {
								continue;
							}
							File targetFile = new File(targetPath + File.separator + zipEntry.getName());
							if (!targetFile.getParentFile().exists()) {
								targetFile.getParentFile().mkdirs();
							}
							os = new BufferedOutputStream(new FileOutputStream(targetFile));
							is = zipFile.getInputStream(zipEntry);
							byte[] buffer = new byte[4096];
							int readLen = 0;
							while ((readLen = is.read(buffer, 0, 4096)) > 0) {
								os.write(buffer, 0, readLen);
							}
						}
					} finally {
						close(is, os);
					}
				}
			}
		} finally {
			zipFile.close();
		}
	}

	private static boolean isNotSafeFile(String name) {
		name = name.toLowerCase();
		return name.contains("..") || name.endsWith(".jsp") || name.endsWith(".jspx");
	}

	public static List<String> getFileNames(File dir, ExtFileFilter filter) {
		List<String> fileNames = new ArrayList<>();
		if(!dir.exists() || !dir.isDirectory()) {
			return fileNames;
		}
		File[] files = dir.listFiles(filter);
		for(File file : files) {
			fileNames.add(file.getName());
		}
		return fileNames;
	}

	public static Map<String, String> getStringMap(File file, String charsetName){
		Map<String, String> stringMap = new HashMap<String, String>(8);
		TextReader textReader = new TextReader();
		textReader.open(file, charsetName);
		String key = null, value = null;
		while((key = textReader.read()) != null && (value = textReader.read()) != null) {
			stringMap.put(key, value);
		}
		textReader.close();
		return stringMap;
	}

	public static InputStream getStream(String resource) {
		if(StrUtil.isBlank(resource)) {
			return null;
		}else {
			try {
				if(StrUtil.isUrl(resource)) {
					return new URL(resource).openStream();
				}else {
					File file = new File(resource);
					if(file.exists() && file.isFile()) {
						return new FileInputStream(file);
					}else {
						logger.info("file not exist or is not file, resource: %s, exists: %s, isFile: %s", resource, file.exists(), file.isFile());
					}
				}
			}catch(Exception e) {
				logger.info("fail to get resource: %s, ex: %s", resource, e.getMessage());
			}
		}
		return null;
	}

	public static class TextReader {
		private Scanner	in;
		public TextReader() {}
		public TextReader(File file) { open(file); }
		public TextReader(File file, String charsetName) { open(file, charsetName); }
		public TextReader(InputStream is) { open(is); }
		public TextReader(InputStream is, String charsetName) { open(is, charsetName); }
		public TextReader(Reader reader) { open(reader); }
		public TextReader(String path) { open(path); }
		public TextReader(String path, String charsetName) { open(path, charsetName); }

		public void open(File file) { open(file, defaultCharsetName); }
		public void open(File file, String charsetName) {
			try {
				close();
				in = new Scanner(file, charsetName);
			} catch (FileNotFoundException e) {
				logger.warn("fail to open file: %s, charset: %s, ex: %s", file, charsetName, e.getMessage());
			}
		}
		public void open(InputStream is) { open(is, defaultCharsetName); }
		public void open(InputStream is, String charsetName) {
			close();
			in = new Scanner(new BufferedInputStream(is), charsetName);
		}
		public void open(Reader reader) {
			close();
			in = new Scanner(new BufferedReader(reader));
		}
		public void open(String path) { open(path, defaultCharsetName); }
		public void open(String path, String charsetName) { open(new File(path), charsetName); }

		public void close() { FileUtil.close(in); }

		public String read() {
			if (in != null) {
				try {
					return in.nextLine();
				} catch (Exception e) { }
			}
			return null;
		}

		public String readAsString(File file) {
			open(file);
			return readAsString();
		}
		public String readAsString(String path) {
			open(path);
			return readAsString();
		}
		public String readAsString(InputStream is) {
			open(is);
			return readAsString();
		}
		public String readAsString() {
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = read()) != null) {
				sb.append(line + "\n");
			}
			close();
			return sb.toString();
		}
	}

	public static class TextWriter {
		private PrintWriter	out;
		private File file;
		private String charsetName = defaultCharsetName;
		private String lineSeparator = defaultlineSeparator;
		private boolean append = false, autoFlush = false;

		public TextWriter() {}
		public TextWriter(File file) { open(file, charsetName, append, autoFlush); }
		public TextWriter(File file, boolean append) { open(file, charsetName, append, autoFlush); }
		public TextWriter(File file, String charsetName) { open(file, charsetName, append, autoFlush); }
		public TextWriter(File file, String charsetName, boolean append) { open(file, charsetName, append, autoFlush); }
		public TextWriter(File file, String charsetName, boolean append, boolean autoFlush) { open(file, charsetName, append, autoFlush); }
		public TextWriter(OutputStream os) { open(os); }
		public TextWriter(OutputStream os, String charsetName) { open(os, charsetName); }
		public TextWriter(OutputStream os, String charsetName, boolean autoFlush) { open(os, charsetName, autoFlush); }
		public TextWriter(String path) { open(path); }
		public TextWriter(String path, boolean append) { this(new File(path), append); }
		public TextWriter(String path, String charsetName) { this(new File(path), charsetName); }
		public TextWriter(String path, String charsetName, boolean append) { this(new File(path), charsetName, append); }
		public TextWriter(String path, String charsetName, boolean append, boolean autoFlush) { this(new File(path), charsetName, append, autoFlush); }
		public TextWriter(Writer writer) { close(); out = new PrintWriter(new BufferedWriter(writer)); };
		public void open(File file) { open(file, defaultCharsetName, false); }
		public void open(File file, boolean append) { open(file, defaultCharsetName, append); }
		public void open(File file, String charsetName) { open(file, charsetName, false); }
		public void open(File file, String charsetName, boolean append) { open(file, charsetName, append, false); }
		public void open(OutputStream os) { open(os, defaultCharsetName); }
		public void open(OutputStream os, String charsetName) { open(os, charsetName, false); }
		public void open(String path) { open(path, defaultCharsetName, false); }
		public void open(String path, boolean append) { open(path, defaultCharsetName, append); }
		public void open(String path, String charsetName) { open(path, charsetName, false); }
		public void open(String path, String charsetName, boolean append) { open(new File(path), charsetName, append); }
		public void open(File file, String charsetName, boolean append, boolean autoFlush) {
			close();
			try {
				out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, append), charsetName)), autoFlush);
			} catch (IOException e) {
				logger.warn("fail to open write file: %s, charset: %s, append: %s, ex: %s", file, charsetName, append, e.getMessage());
			}
		}
		public void open(OutputStream os, String charsetName, boolean autoFlush) {
			close();
			try {
				out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os, charsetName)), autoFlush);
			} catch (IOException e) {
				logger.warn("fail to open write stream: %s, charset: %s, flush: %s, ex: %s", file, charsetName, autoFlush, e.getMessage());
			}
		}

		public void setFile(File file) { this.file = file; }
		public void setFile(String path) { file = new File(path); }
		public void open() { close(); if(file != null) { open(file, charsetName, append, autoFlush); } }

		public void setCharsetName(String charsetName) { this.charsetName = charsetName; }
		public void setAppend(boolean append) { this.append = append; }
		public void setAutoFlush(boolean autoFlush) { this.autoFlush = autoFlush; }
		public TextWriter setLineSeparator(String lineSeparator) { this.lineSeparator = lineSeparator; return this; }

		public void close() { if (out != null) { out.close(); out = null; file = null; } }
		public void flush() { if (out != null) { out.flush(); } }

		public void write(String str) { if (out != null) { out.print(str); } }
		public void writeln() { write(lineSeparator); }
		public void writeln(String str) { write(str + lineSeparator); }

		public void writeFile(File file, String content) { open(file); write(content); close(); }
		public void writeFile(String path, String content) { writeFile(new File(path), content); }

		public void appendFile(File file, String appendContent) { open(file, true); write(appendContent); close(); }
		public void appendFile(String path, String appendContent) { appendFile(new File(path), appendContent); }

		public void appendlnFile(File file, String appendLine) { appendFile(file, appendLine + lineSeparator); }
		public void appendlnFile(String path, String appendLine) { appendFile(new File(path), appendLine + lineSeparator); }
	}

	public static class ExtFileFilter extends javax.swing.filechooser.FileFilter implements FileFilter {
		private boolean acceptDirectory = true;

		public ExtFileFilter() {}
		public ExtFileFilter(String extension) { addExtension(extension); }
		public ExtFileFilter(String extension, String description) { addExtension(extension); setDescription(description); }

		@Override
		public boolean accept(File f) {
			if (f.isDirectory()) {
				return acceptDirectory;
			}
			for (String extension : extensions) {
				if (f.getName().toLowerCase().endsWith(extension)) {
					return true;
				}
			}
			return extensions.size() == 0;
		}

		public boolean accept(File dir, String name) {
			File f = new File(dir, name);
			if (f.isDirectory()) {
				return acceptDirectory;
			}
			for (String extension : extensions) {
				if (name.toLowerCase().endsWith(extension)) {
					return true;
				}
			}
			return extensions.size() == 0;
		}

		public ExtFileFilter addExtension(String extension) {
			if(StrUtil.isBankCardNumber(extension)) {
				return this;
			}
			String[] exts = extension.split("[,;]");
			for (String ext : exts) {
				if ((ext == null) || (ext.trim().length() == 0)) {
					continue;
				}
				ext = ext.trim().toLowerCase();
				if (ext.startsWith("*")) {
					ext = ext.substring(1);
				}
				if (!ext.startsWith(".")) {
					ext = "." + ext;
				}
				if (!extensions.contains(ext)) {
					extensions.add(ext);
				}
			}
			return this;
		}

		public ExtFileFilter removeExtension(String extension) {
			String[] exts = extension.split("[,;]");
			for (String ext : exts) {
				if ((ext == null) || (ext.trim().length() == 0)) {
					continue;
				}
				ext = ext.trim().toLowerCase();
				if (ext.startsWith("*")) {
					ext = ext.substring(1);
				}
				if (!ext.startsWith(".")) {
					ext = "." + ext;
				}
				if (extensions.contains(ext)) {
					extensions.remove(ext);
				}
			}
			return this;
		}

		public ExtFileFilter setExtension(String extension) {
			extensions.clear();
			return addExtension(extension);
		}

		public List<String> getExtention(){
			return extensions;
		}

		@Override
		public String getDescription() {
			return description;
		}

		public ExtFileFilter setDescription(String description) {
			this.description = description;
			return this;
		}

		public ExtFileFilter acceptDirectory(boolean acceptDirectory) {
			this.acceptDirectory = acceptDirectory;
			return this;
		}

		private String description = null;
		private List<String> extensions = new LinkedList<String>();

		public static final ExtFileFilter IMAGES = new ExtFileFilter("jpg,jpeg,png,gif").acceptDirectory(false);
		public static final ExtFileFilter AUDIOS = new ExtFileFilter("mp3,wav,ogg").acceptDirectory(false);
		public static final ExtFileFilter VIDEOS = new ExtFileFilter("mp4,ogv,webm").acceptDirectory(false);
		public static final ExtFileFilter FOLDERS = new ExtFileFilter("none").acceptDirectory(true);
		public static final ExtFileFilter FILES = new ExtFileFilter("").acceptDirectory(false);
		public FilenameFilter filenameFilter() {
			return new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return ExtFileFilter.this.accept(dir, name);
				}
			};
		}
	}

	public static Comparator<File> fileComparator = new Comparator<File>() {
		@Override
		public int compare(File o1, File o2) {
			if(o1==null) {
				return o2==null ? 0 : -1;
			} else if(o2==null) {
				return 1;
			}
			return fileNameComparator.compare(o1.getName(), o2.getName());
		}
	};
	public static Comparator<String> fileNameComparator = new Comparator<String>() {
		@Override
		public int compare(String o1, String o2) {
			if(o1==null) {
				return o2==null ? 0 : -1;
			} else if(o2==null) {
				return 1;
			}
			int len1=o1.length(), len2 = o2.length(), len = Math.min(len1, len2);
			for(int i=0; i<len; i++) {
				char c1 = o1.charAt(i), c2 = o2.charAt(i);
				if(c1 == c2) {
					continue;
				}
				if(StrUtil.isDigit(c1) && StrUtil.isDigit(c2)) {
					StringBuilder num1 = new StringBuilder().append(c1), num2 = new StringBuilder().append(c2);
					int j = i+1; char c;
					while(j<len1 && StrUtil.isDigit(c=o1.charAt(j++))) {
						num1.append(c);
					}
					j = i+1;
					while(j<len2 && StrUtil.isDigit(c=o2.charAt(j++))) {
						num2.append(c);
					}
					int n1 = Integer.parseInt(num1.toString()), n2 = Integer.parseInt(num2.toString());
					if(n1 != n2) {
						return n1 > n2 ? 1 : -1;
					}
					i += Math.min(num1.length(), num2.length()) - 1;
				}else if(StrUtil.isChinese(c1) || StrUtil.isChinese(c2)) {
					String p1 = PinyinUtil.getPinyin(c1), p2 = PinyinUtil.getPinyin(c2);
					if(p1!=null && !p1.equals(p2)) {
						return p1.compareTo(p2);
					}
				} else {
					return c1 > c2 ? 1 : -1;
				}
			}
			return 0;
		}
	};
}

