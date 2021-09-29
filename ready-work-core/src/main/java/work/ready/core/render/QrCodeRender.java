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

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import io.undertow.util.Headers;
import io.undertow.util.MimeMappings;
import io.undertow.util.StatusCodes;
import work.ready.core.tools.StrUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class QrCodeRender extends Render {

	protected String content;
	protected int width;
	protected int height;
	protected ErrorCorrectionLevel errorCorrectionLevel;
	protected static final String DEFAULT_CONTENT_TYPE = MimeMappings.DEFAULT.getMimeType("png");;

	public QrCodeRender(String content, int width, int height) {
		init(content, width, height, null);
	}

	public QrCodeRender(String content, int width, int height, ErrorCorrectionLevel errorCorrectionLevel) {
		init(content, width, height, errorCorrectionLevel);
	}

	public QrCodeRender(String content, int width, int height, char errorCorrectionLevel) {
		init(content, width, height, errorCorrectionLevel);
	}

	protected void init(String content, int width, int height, char errorCorrectionLevel) {
		if (errorCorrectionLevel == 'H') {
			init(content, width, height, ErrorCorrectionLevel.H);
		} else if (errorCorrectionLevel == 'Q') {
			init(content, width, height, ErrorCorrectionLevel.Q);
		} else if (errorCorrectionLevel == 'M') {
			init(content, width, height, ErrorCorrectionLevel.M);
		} else if (errorCorrectionLevel == 'L') {
			init(content, width, height, ErrorCorrectionLevel.L);
		} else {
			throw new IllegalArgumentException("errorCorrectionLevel options are: 'H'、'Q'、'M'、'L'");
		}
	}

	protected void init(String content, int width, int height, ErrorCorrectionLevel errorCorrectionLevel) {
		if (StrUtil.isBlank(content)) {
			throw new IllegalArgumentException("content is empty");
		}
		if (width < 0 || height < 0) {
			throw new IllegalArgumentException("invalid width or height");
		}
		this.content = content;
		this.width = width;
		this.height = height;
		this.errorCorrectionLevel = errorCorrectionLevel;
	}

	public void render() {
		response.addHeader(Headers.PRAGMA, "no-cache");
		response.addHeader(Headers.CACHE_CONTROL, "no-cache");
		response.addHeader(Headers.EXPIRES, Integer.toString(0));
		response.setContentType(DEFAULT_CONTENT_TYPE);
		response.setStatus(StatusCodes.OK);

		Map<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>();
		hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
		hints.put(EncodeHintType.MARGIN, 0);    
		if (errorCorrectionLevel != null) {
			hints.put(EncodeHintType.ERROR_CORRECTION, errorCorrectionLevel);
		}

		try {
			response.startBlocking();

			QRCodeWriter writer = new QRCodeWriter();
			BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints);

			MatrixToImageWriter.writeToStream(bitMatrix, "png", response.getOutputStream());    
			response.responseDone();
		} catch (IOException e) {	
			String name = e.getClass().getSimpleName();
			if ("ClientAbortException".equals(name) || "EofException".equals(name)) {
			} else {
				throw new RenderException(e);
			}
		} catch (Exception e) {
			throw new RenderException(e);
		}
	}
}
