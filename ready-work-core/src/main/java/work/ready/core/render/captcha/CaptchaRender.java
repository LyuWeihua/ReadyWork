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
package work.ready.core.render.captcha;

import io.undertow.server.handlers.Cookie;
import io.undertow.util.Headers;
import io.undertow.util.MimeMappings;
import io.undertow.util.StatusCodes;
import work.ready.core.handler.Controller;
import work.ready.core.handler.cookie.CookieItem;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.render.Render;
import work.ready.core.render.RenderException;
import work.ready.core.server.Ready;
import work.ready.core.tools.StrUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.QuadCurve2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Random;

public class CaptchaRender extends Render {
	private static final Log logger = LogFactory.getLog(CaptchaRender.class);

	protected static String captchaName = "captcha_cache";
	protected static final Random random = new Random(System.nanoTime());
	protected static final String DEFAULT_CONTENT_TYPE = MimeMappings.DEFAULT.getMimeType("jpg");

	protected static final int WIDTH = 108, HEIGHT = 40;

	protected static final char[] charArray = "3456789ABCDEFGHJKMNPQRSTUVWXY".toCharArray();

	protected static final Font[] RANDOM_FONT = new Font[] {
		new Font(Font.DIALOG, Font.BOLD, 33),
		new Font(Font.DIALOG_INPUT, Font.BOLD, 34),
		new Font(Font.SERIF, Font.BOLD, 33),
		new Font(Font.SANS_SERIF, Font.BOLD, 34),
		new Font(Font.MONOSPACED, Font.BOLD, 34)
	};

	public static void setCaptchaName(String captchaName) {
		if (StrUtil.isBlank(captchaName)) {
			throw new IllegalArgumentException("captchaName can not be blank.");
		}
		CaptchaRender.captchaName = captchaName;
	}

	public void render() {
		Captcha captcha = createCaptcha();

		var cache = Ready.cacheManager().getCache();
		cache.put(captchaName, captcha.getKey(), captcha, Captcha.liveSeconds);

		CookieItem cookie = new CookieItem(captchaName, captcha.getKey());
		cookie.setMaxAge(-1);
		cookie.setPath("/");

		request.getExchange().getResponseCookies().put(captchaName, cookie);
		request.getExchange().getResponseHeaders().add(Headers.PRAGMA, "no-cache");
		request.getExchange().getResponseHeaders().add(Headers.CACHE_CONTROL, "no-cache");
		request.getExchange().getResponseHeaders().add(Headers.EXPIRES, 0);
		request.getExchange().getResponseHeaders().add(Headers.CONTENT_TYPE, DEFAULT_CONTENT_TYPE);
		request.getExchange().setStatusCode(StatusCodes.OK);

		OutputStream sos = null;
		try {
			response.startBlocking();

			BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
			drawGraphic(captcha.getValue(), image);

			sos = request.getExchange().getOutputStream();

			ImageIO.write(image, "jpeg", sos);
			response.responseDone();
		} catch (IOException e) {
			if (Ready.getBootstrapConfig().isDevMode()) {
				throw new RenderException(e);
			}
		} catch (Exception e) {
			throw new RenderException(e);
		} finally {
			if (sos != null) {
				try {sos.close();} catch (IOException e) {logger.warn(e.getMessage());}
			}
		}
	}

	protected Captcha createCaptcha() {
		String captchaKey = getCaptchaKeyFromCookie();
		if (StrUtil.isBlank(captchaKey)) {
			captchaKey = StrUtil.getRandomUUID();
		}
		return new Captcha(captchaKey, getRandomString(), Captcha.liveSeconds);
	}

	protected String getCaptchaKeyFromCookie() {
		Map<String, Cookie> cookieMap = request.getExchange().getRequestCookies();
		if (cookieMap != null) {
			for (Cookie cookie : cookieMap.values()) {
				if (cookie.getName().equals(captchaName)) {
					return cookie.getValue();
				}
			}
		}
		return null;
	}

	protected String getRandomString() {
		char[] randomChars = new char[4];
		for (int i=0; i<randomChars.length; i++) {
			randomChars[i] = charArray[random.nextInt(charArray.length)];
		}
		return String.valueOf(randomChars);
	}

	protected void drawGraphic(String randomString, BufferedImage image){

		Graphics2D g = image.createGraphics();

		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		g.setColor(getRandColor(210, 250));
		g.fillRect(0, 0, WIDTH, HEIGHT);

		Color color = null;
		for(int i = 0; i < 20; i++){
			color = getRandColor(120, 200);
			g.setColor(color);
			String rand = String.valueOf(charArray[random.nextInt(charArray.length)]);
			g.drawString(rand, random.nextInt(WIDTH), random.nextInt(HEIGHT));
			color = null;
		}

		g.setFont(RANDOM_FONT[random.nextInt(RANDOM_FONT.length)]);

		for (int i = 0; i < randomString.length(); i++){

			int degree = random.nextInt(28);
			if (i % 2 == 0) {
				degree = degree * (-1);
			}

			int x = 22 * i, y = 21;

			g.rotate(Math.toRadians(degree), x, y);

			color = getRandColor(20, 130);
			g.setColor(color);

			g.drawString(String.valueOf(randomString.charAt(i)), x + 8, y + 10);

			g.rotate(-Math.toRadians(degree), x, y);
		}

		g.setColor(color);

		BasicStroke bs = new BasicStroke(3);
		g.setStroke(bs);

		QuadCurve2D.Double curve = new QuadCurve2D.Double(0d, random.nextInt(HEIGHT - 8) + 4, WIDTH / 2, HEIGHT / 2, WIDTH, random.nextInt(HEIGHT - 8) + 4);
		g.draw(curve);

		g.dispose();
	}

	protected Color getRandColor(int fc, int bc) {
		Random random = new Random();
		if (fc > 255)
			fc = 255;
		if (bc > 255)
			bc = 255;
		int r = fc + random.nextInt(bc - fc);
		int g = fc + random.nextInt(bc - fc);
		int b = fc + random.nextInt(bc - fc);
		return new Color(r, g, b);
	}

	public static boolean validate(Controller controller, String userInputString) {
		String captchaKey = controller.getCookie(captchaName);
		if (validate(captchaKey, userInputString)) {
			controller.removeCookie(captchaName);
			return true;
		}
		return false;
	}

	public static boolean validate(String captchaKey, String userInputString) {
		if(captchaKey == null) return false;
		Captcha captcha;
		var cache = Ready.cacheManager().getCache();
		captcha = cache.get(captchaName, captchaKey);
		cache.remove(captchaName, captchaKey);
		if (captcha != null && captcha.notExpired() && captcha.getValue().equalsIgnoreCase(userInputString)) {
			return true;
		}

		return false;
	}
}

