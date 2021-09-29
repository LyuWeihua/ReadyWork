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

import work.ready.core.server.Constant;
import work.ready.core.server.Ready;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PathUtil {

	private static String projectRootPath;
	private static String rootClassPath;
	private static String[] classPathDirs;

	@SuppressWarnings("rawtypes")
	public static String getPath(Class clazz) {
		String path = clazz.getResource("").getPath();
		return new File(path).getAbsolutePath();
	}

	public static String getPath(Object object) {
		String path = object.getClass().getResource("").getPath();
		return new File(path).getAbsolutePath();
	}

	public static String getRootClassPath() {
		if (rootClassPath == null) {
			try {

				String path = Ready.getClassLoader().getResource("").getPath();
				rootClassPath = new File(path).getAbsolutePath();
			}
			catch (Exception e) {
				String path = PathUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath();
				path = java.net.URLDecoder.decode(path, Constant.DEFAULT_CHARSET);
				if (path.endsWith(File.separator)) {
					path = path.substring(0, path.length() - 1);
				}
				rootClassPath = path;
			}
		}
		return rootClassPath;
	}

	public static void setRootClassPath(String rootClassPath) {
		PathUtil.rootClassPath = rootClassPath;
	}

	public static String getPackagePath(Object object) {
		Package p = object.getClass().getPackage();
		return p != null ? p.getName().replaceAll("\\.", "/") : "";
	}

	public static File getFileFromJar(String file) {
		throw new RuntimeException("Not finish. Do not use this method.");
	}

	public static String getProjectRootPath() {
		if (projectRootPath == null) {
			projectRootPath = detectProjectRootPath();
		}
		return projectRootPath;
	}

	public static void setProjectRootPath(String projectRootPath) {
		if (projectRootPath == null) {
			return ;
		}

		if (projectRootPath.endsWith(File.separator)) {
			projectRootPath = projectRootPath.substring(0, projectRootPath.length() - 1);
		}
		PathUtil.projectRootPath = projectRootPath;
	}

	private static String detectProjectRootPath() {
		try {

			String path = PathUtil.class.getResource("/").getPath();

			return new File(path).getParentFile().getParentFile().getCanonicalPath();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static boolean isAbsolutePath(String path) {
		return path.startsWith("@") || path.startsWith("~") ||path.startsWith("[a-zA-Z]:");
	}

	public static String[] getClassPathDirs() {
		if (classPathDirs == null) {
			classPathDirs = buildClassPathDirs();
		}
		return classPathDirs;
	}

	private static String[] buildClassPathDirs() {
		List<String> list = new ArrayList<>();
		String[] classPathArray = System.getProperty("java.class.path").split(File.pathSeparator);
		for(String classPath : classPathArray) {
			classPath = classPath.trim();

			if (classPath.startsWith("./")) {
				classPath = classPath.substring(2);
			}

			File file = new File(classPath);
			if (file.exists() && file.isDirectory()) {

				if (!classPath.endsWith(File.separator)) {
					classPath = classPath + File.separator;		
				}

				list.add(classPath);
			}
		}
		return list.toArray(new String[list.size()]);
	}
}

