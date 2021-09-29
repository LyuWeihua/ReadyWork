/**
 *
 * Original work Copyright (c) 2011-2019, James Zhan 詹波 (jfinal@126.com).
 * Modified work Copyright (c) 2020 WeiHua Lyu [ready.work]
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
package work.ready.core.render;

import io.undertow.util.Headers;
import io.undertow.util.MimeMappings;
import io.undertow.util.StatusCodes;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.tools.FileUtil;
import work.ready.core.tools.StrUtil;

import java.io.*;
import java.net.URLEncoder;

import static java.nio.charset.StandardCharsets.UTF_8;

public class FileRender extends Render {
	private static final Log logger = LogFactory.getLog(FileRender.class);

	protected static final String DEFAULT_CONTENT_TYPE = MimeMappings.DEFAULT.getMimeType("bin");
	protected final RenderManager renderManager;
	protected File file;
	protected String downloadFileName = null;

	protected boolean normalRenderOnly = false;

	public void setNormalRenderOnly(boolean normalRenderOnly) {
		this.normalRenderOnly = normalRenderOnly;
	}

	public FileRender(RenderManager renderManager, File file) {
		this.renderManager = renderManager;
		if (file == null) {
			throw new IllegalArgumentException("file can not be null.");
		}
		this.file = file;
	}

	public FileRender(RenderManager renderManager, File file, String downloadFileName) {
		this(renderManager, file);

		if (StrUtil.isBlank(downloadFileName)) {
			throw new IllegalArgumentException("downloadFileName can not be blank.");
		}
		this.downloadFileName = downloadFileName;
	}

	public FileRender(RenderManager renderManager, String fileName) {
		this.renderManager = renderManager;
		if (StrUtil.isBlank(fileName)) {
			throw new IllegalArgumentException("fileName can not be blank.");
		}

		String fullFileName;
		fileName = fileName.trim();
		if (fileName.startsWith("/") || fileName.startsWith("\\")) {
			if (renderManager.getBaseDownloadPath().equals("/")) {
				fullFileName = fileName;
			} else {
				fullFileName = renderManager.getBaseDownloadPath() + fileName;
			}
		} else {
			fullFileName = renderManager.getBaseDownloadPath() + File.separator + fileName;
		}

		this.file = new File(fullFileName);
	}

	public FileRender(RenderManager renderManager, String fileName, String downloadFileName) {
		this(renderManager, fileName);

		if (StrUtil.isBlank(downloadFileName)) {
			throw new IllegalArgumentException("downloadFileName can not be blank.");
		}
		this.downloadFileName = downloadFileName;
	}

	public void render() {
		if (file == null || !file.isFile()) {
			renderManager.getRenderFactory().getErrorRender(StatusCodes.NOT_FOUND).setContext(request, response).render();
			return ;
		}

		String userAgent = request.getHeader(Headers.USER_AGENT);
		String fn = downloadFileName == null ? file.getName() : downloadFileName;
		response.addHeader(Headers.ACCEPT_RANGES, "bytes");
		response.addHeader(Headers.CONTENT_DISPOSITION, "attachment; " + encodeFileName(userAgent,fn));
		String contentType = MimeMappings.DEFAULT.getMimeType(FileUtil.getFileExtension(file.getName()));
		response.setContentType(contentType != null ? contentType : DEFAULT_CONTENT_TYPE);
		response.setStatus(StatusCodes.OK);

		if (normalRenderOnly || StrUtil.isBlank(request.getHeader(Headers.RANGE))) {
			normalRender();
		} else {
			rangeRender();
		}
		response.responseDone();
	}

	public String encodeFileName(String userAgent, String fileName) {
		String encodedFileName = URLEncoder.encode(fileName, UTF_8);

		if (userAgent == null) {
			return "filename=\"" + encodedFileName + "\"";
		}
		return "filename*=UTF-8''" + encodedFileName;
	}

	protected void normalRender() {
		response.addHeader(Headers.CONTENT_LENGTH, String.valueOf(file.length()));
		InputStream inputStream = null;
		OutputStream outputStream = null;
		try {
			inputStream = new BufferedInputStream(new FileInputStream(file));
			response.startBlocking();
			outputStream = response.getOutputStream();
			byte[] buffer = new byte[1024];
			for (int len = -1; (len = inputStream.read(buffer)) != -1;) {
				outputStream.write(buffer, 0, len);
			}

			outputStream.close();
		} catch (IOException e) {	
			String name = e.getClass().getSimpleName();
			if (name.equals("ClientAbortException") || name.equals("EofException")) {
			} else {
				throw new RenderException(e);
			}
		} catch (Exception e) {
			throw new RenderException(e);
		} finally {
			if (inputStream != null)
				try {inputStream.close();} catch (IOException e) {logger.error(e,"InputStream close exception");}
		}
	}

	protected void rangeRender() {
		Long[] range = {null, null};
		processRange(range);

		String contentLength = String.valueOf(range[1].longValue() - range[0].longValue() + 1);
		response.addHeader(Headers.CONTENT_LENGTH, contentLength);
		response.setStatus(StatusCodes.PARTIAL_CONTENT);

		StringBuilder contentRange = new StringBuilder("bytes ").append(range[0]).append("-").append(range[1]).append("/").append(file.length());
		response.addHeader(Headers.CONTENT_RANGE, contentRange.toString());

		InputStream inputStream = null;
		OutputStream outputStream = null;
		try {
			long start = range[0];
			long end = range[1];
			inputStream = new BufferedInputStream(new FileInputStream(file));
			if (inputStream.skip(start) != start)
					throw new RuntimeException("File skip error");
			response.startBlocking();
			outputStream = response.getOutputStream();
			byte[] buffer = new byte[1024];
			long position = start;
			for (int len; position <= end && (len = inputStream.read(buffer)) != -1;) {
				if (position + len <= end) {
					outputStream.write(buffer, 0, len);
					position += len;
				}
				else {
					for (int i=0; i<len && position <= end; i++) {
						outputStream.write(buffer[i]);
						position++;
					}
				}
			}

			outputStream.close();
		}
		catch (IOException e) {	
			String name = e.getClass().getSimpleName();
			if (name.equals("ClientAbortException") || name.equals("EofException")) {
			} else {
				throw new RenderException(e);
			}
		}
		catch (Exception e) {
			throw new RenderException(e);
		}
		finally {
			if (inputStream != null)
				try {inputStream.close();} catch (IOException e) {logger.error(e, "InputStream close exception");}
		}
	}

	protected void processRange(Long[] range) {
		String rangeStr = request.getExchange().getRequestHeaders().getFirst(Headers.RANGE);
		int index = rangeStr.indexOf(',');
		if (index != -1)
			rangeStr = rangeStr.substring(0, index);
		rangeStr = rangeStr.replace("bytes=", "");

		String[] arr = rangeStr.split("-", 2);
		if (arr.length < 2)
			throw new RuntimeException("Range error");

		long fileLength = file.length();
		for (int i=0; i<range.length; i++) {
			if (StrUtil.notBlank(arr[i])) {
				range[i] = Long.parseLong(arr[i].trim());
				if (range[i] >= fileLength)
					range[i] = fileLength - 1;
			}
		}

		if (range[0] != null && range[1] == null) {
			range[1] = fileLength - 1;
		}

		else if (range[0] == null && range[1] != null) {
			range[0] = fileLength - range[1];
			range[1] = fileLength - 1;
		}

		if (range[0] == null || range[1] == null || range[0].longValue() > range[1].longValue())
			throw new RuntimeException("Range error");
	}
}

