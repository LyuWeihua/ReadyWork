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

package work.ready.core.template.source;

import work.ready.core.server.Ready;
import work.ready.core.template.EngineConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

public class ClassPathSource implements TemplateSource {

	protected String finalFileName;
	protected String fileName;
	protected String encoding;

	protected boolean isInJar;
	protected long lastModified;
	protected ClassLoader classLoader;
	protected URL url;

	public ClassPathSource(String fileName) {
		this(null, fileName, EngineConfig.DEFAULT_ENCODING);
	}

	public ClassPathSource(String baseTemplatePath, String fileName) {
		this(baseTemplatePath, fileName, EngineConfig.DEFAULT_ENCODING);
	}

	public ClassPathSource(String baseTemplatePath, String fileName, String encoding) {
		this.finalFileName = buildFinalFileName(baseTemplatePath, fileName);
		this.fileName = fileName;
		this.encoding= encoding;
		this.classLoader = Ready.getClassLoader();
		this.url = classLoader.getResource(finalFileName);
		if (url == null) {
			throw new IllegalArgumentException("File not found in CLASSPATH or JAR : \"" + finalFileName + "\"");
		}

		processIsInJarAndlastModified();
	}

	protected void processIsInJarAndlastModified() {
		if ("file".equalsIgnoreCase(url.getProtocol())) {
			isInJar = false;
			lastModified = new File(url.getFile()).lastModified();
		} else {
			isInJar = true;
			lastModified = -1;
		}
	}

	protected String buildFinalFileName(String baseTemplatePath, String fileName) {
		String finalFileName;
		if (baseTemplatePath != null) {
			char firstChar = fileName.charAt(0);
			if (firstChar == '/' || firstChar == '\\') {
				finalFileName = baseTemplatePath + fileName;
			} else {
				finalFileName = baseTemplatePath + "/" + fileName;
			}
		} else {
			finalFileName = fileName;
		}

		if (finalFileName.charAt(0) == '/') {
			finalFileName = finalFileName.substring(1);
		}

		return finalFileName;
	}

	public String getCacheKey() {
		return fileName;
	}

	public String getEncoding() {
		return encoding;
	}

	protected long getLastModified() {
		return new File(url.getFile()).lastModified();
	}

	public boolean isModified() {
		return isInJar ? false : lastModified != getLastModified();
	}

	public StringBuilder getContent() {

		if (!isInJar) {		
			lastModified = getLastModified();
		}

		InputStream inputStream = classLoader.getResourceAsStream(finalFileName);
		if (inputStream == null) {
			throw new RuntimeException("File not found : \"" + finalFileName + "\"");
		}

		return loadFile(inputStream, encoding);
	}

	public static StringBuilder loadFile(InputStream inputStream, String encoding) {
		StringBuilder ret = new StringBuilder();

		try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, encoding))) {
			
			String line = br.readLine();
			if (line != null) {
				ret.append(line);
			} else {
				return ret;
			}

			while ((line=br.readLine()) != null) {
				ret.append('\n').append(line);
			}
			return ret;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("In Jar File: ").append(isInJar).append("\n");
		sb.append("File name: ").append(fileName).append("\n");
		sb.append("Final file name: ").append(finalFileName).append("\n");
		sb.append("Last modified: ").append(lastModified).append("\n");
		return sb.toString();
	}
}

