/**
 *
 * Original work Copyright (c) 2011-2019, James Zhan 詹波 (jfinal@126.com).
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

import io.undertow.server.HttpServerExchange;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.server.Constant;
import work.ready.core.server.TrustAllTrustManager;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.*;
import java.util.Map.Entry;

import static work.ready.core.tools.FileUtil.*;

public class HttpUtil {

	private static final Log logger = LogFactory.getLog(HttpUtil.class);

	private static final String GET  = "GET";
	private static final String POST = "POST";
	private static String CHARSET = "UTF-8";

    static final String HTTP_SCHEME = "http";
    static final String HTTPS_SCHEME = "https";
    static final String HTTP_PREFIX = HTTP_SCHEME + "://";
    static final String HTTPS_PREFIX = HTTPS_SCHEME + "://";

    private static int connectTimeout = 19000;	
	private static int readTimeout = 19000;		
    private static Proxy proxy;

	private static final SSLSocketFactory sslSocketFactory = initSSLSocketFactory();
	private static final TrustALLHostnameVerifier TRUST_ALL_HOSTNAME_VERIFIER = new TrustALLHostnameVerifier();

	static class TrustALLHostnameVerifier implements HostnameVerifier {
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	}

	private HttpUtil() {}

	private static SSLSocketFactory initSSLSocketFactory() {
		try {
			TrustManager[] tm = {new TrustAllTrustManager() };
			SSLContext sslContext = SSLContext.getInstance("TLSv1.2");	
			sslContext.init(null, tm, new java.security.SecureRandom());
			return sslContext.getSocketFactory();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void setCharSet(String charSet) {
		if (StrUtil.isBlank(charSet)) {
			throw new IllegalArgumentException("charSet can not be blank.");
		}
		HttpUtil.CHARSET = charSet;
	}

	public static void setConnectTimeout(int connectTimeout) {
		HttpUtil.connectTimeout = connectTimeout;
	}

	public static void setReadTimeout(int readTimeout) {
		HttpUtil.readTimeout = readTimeout;
	}

	private static HttpURLConnection getHttpConnection(String url, String method, Map<String, String> headers) throws IOException, NoSuchAlgorithmException, NoSuchProviderException, KeyManagementException {
		URL _url = new URL(url);
		HttpURLConnection conn = (HttpURLConnection)_url.openConnection();
		if (conn instanceof HttpsURLConnection) {
			((HttpsURLConnection)conn).setSSLSocketFactory(sslSocketFactory);
			((HttpsURLConnection)conn).setHostnameVerifier(TRUST_ALL_HOSTNAME_VERIFIER);
		}

		conn.setRequestMethod(method);
		conn.setDoOutput(true);
		conn.setDoInput(true);

		conn.setConnectTimeout(connectTimeout);
		conn.setReadTimeout(readTimeout);

		conn.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
		conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.146 Safari/537.36");

		if (headers != null && !headers.isEmpty()) {
			for (Entry<String, String> entry : headers.entrySet()) {
				conn.setRequestProperty(entry.getKey(), entry.getValue());
			}
		}

		return conn;
	}

	public static String get(String url, Map<String, Object> queryParams, Map<String, String> headers) {
		HttpURLConnection conn = null;
		try {
			conn = getHttpConnection(buildUrlWithQueryString(url, queryParams), GET, headers);
			conn.connect();
			return readResponseString(conn);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	public static String get(String url, Map<String, Object> queryParams) {
		return get(url, queryParams, null);
	}

	public static String get(String url) {
		return get(url, null, null);
	}

	public static String post(String url, Map<String, Object> queryParams, String data, Map<String, String> headers) {
		HttpURLConnection conn = null;
		try {
			conn = getHttpConnection(buildUrlWithQueryString(url, queryParams), POST, headers);
			conn.connect();

			if (data != null) {
				OutputStream out = conn.getOutputStream();
				out.write(data.getBytes(CHARSET));
				out.flush();
				out.close();
			}

			return readResponseString(conn);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	public static String post(String url, Map<String, Object> queryParams, String data) {
		return post(url, queryParams, data, null);
	}

	public static String post(String url, String data, Map<String, String> headers) {
		return post(url, null, data, headers);
	}

	public static String post(String url, String data) {
		return post(url, null, data, null);
	}

	private static String readResponseString(HttpURLConnection conn) {
		BufferedReader reader = null;
		try {
			StringBuilder ret;
			reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), CHARSET));
			String line = reader.readLine();
			if (line != null) {
				ret = new StringBuilder();
				ret.append(line);
			} else {
				return "";
			}

			while ((line = reader.readLine()) != null) {
				ret.append('\n').append(line);
			}
			return ret.toString();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					logger.error(e,"BufferedReader close exception");
				}
			}
		}
	}

	public static String buildUrlWithQueryString(String url, Map<String, Object> queryParams) {
		if (queryParams == null || queryParams.isEmpty()) {
			return url;
		}

		StringBuilder sb = new StringBuilder(url);
		boolean isFirst;
		if (url.indexOf('?') == -1) {
			isFirst = true;
			sb.append('?');
		}
		else {
			isFirst = false;
		}

		for (Entry<String, Object> entry : queryParams.entrySet()) {
			if (isFirst) {
				isFirst = false;
			} else {
				sb.append('&');
			}

			String key = entry.getKey();
			String value = entry.getValue().toString();
			if (StrUtil.notBlank(value)) {
				try {value = URLEncoder.encode(value, CHARSET);} catch (UnsupportedEncodingException e) {throw new RuntimeException(e);}
			}
			sb.append(key).append('=').append(value);
		}
		return sb.toString();
	}

	public static boolean download(String url, File target) {
		InputStream in = null; OutputStream out = null;
		try {
			in = httpStream(url);
			if(!target.getParentFile().exists()) {
				target.getParentFile().mkdirs();
			}
			out = new FileOutputStream(target);
			copyStream(in, out);
			return true;
		}catch(Exception e) {
			return false;
		}finally {
			close(out);
		}
	}

	public static byte[] bytes(String url) {
		InputStream in = httpStream(url);
		if(in != null) {
			ByteArrayOutputStream baos = readStream(in);
			if(baos != null) {
				return baos.toByteArray();
			}
		}
		return null;
	}

	public static InputStream httpStream(String url) {
		try {
			HttpURLConnection con = getHttpConnection(url, GET, null);
			int httpRedirect = 301;
			if(con.getResponseCode() == httpRedirect) {
				return httpStream(con.getHeaderField("Location"));
			}
			return con.getInputStream();
		}catch(Exception e) {
			logger.warn("fail to get stream of url: %s, ex: %s", url, e.getMessage());
		}
		return null;
	}

	public static String readDataFromExchange(HttpServerExchange httpServerExchange) {
		try {
			httpServerExchange.startBlocking();
			InputStream inputStream = httpServerExchange.getInputStream();
			byte[] bs = readStream(inputStream).toByteArray();
			return StrUtil.trimToEmpty(new String(bs, StandardCharsets.UTF_8));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static final char[] HEX_CHARS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

	private static final BitSet URI_UNESCAPED = new BitSet(128);

	static {
		for (int i = 'a'; i <= 'z'; i++) {
			URI_UNESCAPED.set(i);
		}
		for (int i = 'A'; i <= 'Z'; i++) {
			URI_UNESCAPED.set(i);
		}
		for (int i = '0'; i <= '9'; i++) {
			URI_UNESCAPED.set(i);
		}
		URI_UNESCAPED.set('-');
		URI_UNESCAPED.set('_');
		URI_UNESCAPED.set('.');
		URI_UNESCAPED.set('!');
		URI_UNESCAPED.set('~');
		URI_UNESCAPED.set('*');
		URI_UNESCAPED.set('\'');
		URI_UNESCAPED.set('(');
		URI_UNESCAPED.set(')');
	}

	public static String uriComponent(String value) {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		int length = bytes.length;

		for (int i = 0; i < length; i++) {
			byte b1 = bytes[i];
			if (b1 < 0 || !URI_UNESCAPED.get(b1)) {   
				StringBuilder builder = new StringBuilder(length * 2);
				if (i > 0) builder.append(value, 0, i); 
				for (int j = i; j < length; j++) {
					byte b2 = bytes[j];
					if (b2 >= 0 && URI_UNESCAPED.get(b2)) {
						builder.append((char) b2);
					} else {
						builder.append('%');
						builder.append(HEX_CHARS[(b2 >> 4) & 0xF]);
						builder.append(HEX_CHARS[b2 & 0xF]);
					}
				}
				return builder.toString();
			}
		}
		return value;
	}

	public static String decodeURIComponent(String value) {
		int length = value.length();
		int index = 0;
		for (; index < length; index++) {
			int ch = value.charAt(index);
			if (ch == '%') break;
		}
		if (index == length) return value;

		byte[] buffer = new byte[length];
		for (int i = 0; i < index; i++) buffer[i] = (byte) value.charAt(i);
		int position = index;

		for (; index < length; index++) {
			char ch = value.charAt(index);
			if (ch == '%') {
				if ((index + 2) >= length) throw new IllegalArgumentException("invalid uri encoding, value=" + value.substring(index));
				char hex1 = value.charAt(index + 1);
				char hex2 = value.charAt(index + 2);
				int u = Character.digit(hex1, 16);
				int l = Character.digit(hex2, 16);
				if (u == -1 || l == -1) throw new IllegalArgumentException("invalid uri encoding, value=" + value.substring(index));
				buffer[position] = (byte) ((u << 4) + l);
				index += 2;
			} else {
				buffer[position] = (byte) ch;   
			}
			position++;
		}
		return new String(buffer, 0, position, StandardCharsets.UTF_8);
	}

	public static String base64(String value) {
		return base64(value.getBytes(StandardCharsets.UTF_8));
	}

	public static String base64(byte[] value) {
		return Base64.getEncoder().encodeToString(value);  
	}

	public static byte[] decodeBase64(String value) {
		return Base64.getDecoder().decode(value.replace(" ","+"));
	}

	public static String base64URLSafe(byte[] value) {
		return Base64.getUrlEncoder().encodeToString(value);
	}

	public static byte[] decodeBase64URLSafe(String value) {
		return Base64.getUrlDecoder().decode(value);
	}

	public static String urlEncode(String value) {
		if (value == null || value.length() == 0) {
			return "";
		}
		try {
			return URLEncoder.encode(value, Constant.DEFAULT_ENCODING);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public static String urlDecode(String value) {
		if (value == null || value.length() == 0) {
			return "";
		}
		try {
			return URLDecoder.decode(value, Constant.DEFAULT_ENCODING);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public static Charset parseCharset(String charset) {
		Charset cs = null;
		try {
			cs = Charset.forName(charset);
		} catch (UnsupportedCharsetException e) {
		}
		return cs;
	}

    public static URI parseURI(String connectionString, URI defaultURI) {
        final URI uri = parseMaybeWithScheme(connectionString, defaultURI.getScheme() + "://");

        final String scheme = uri.getScheme() != null ? uri.getScheme() : defaultURI.getScheme();

        final String host = uri.getHost() != null ? uri.getHost() : defaultURI.getHost();
        final String path = "".equals(uri.getPath()) ? defaultURI.getPath() : uri.getPath();
        final String rawQuery = uri.getQuery() == null ? defaultURI.getRawQuery() : uri.getRawQuery();
        final String rawFragment = uri.getFragment() == null ? defaultURI.getRawFragment() : uri.getRawFragment();
        final int port = uri.getPort() < 0 ? defaultURI.getPort() : uri.getPort();
        try {

            String connStr = new URI(scheme, uri.getUserInfo(), host, port, path, null, null).toString();
            if (StrUtil.hasLength(rawQuery)) {
                connStr += "?" + rawQuery;
            }
            if (StrUtil.hasLength(rawFragment)) {
                connStr += "#" + rawFragment;
            }
            return new URI(connStr);
        } catch (URISyntaxException e) {

            throw new IllegalArgumentException("Invalid connection configuration [" + connectionString + "]: " + e.getMessage(), e);
        }
    }

    private static URI parseMaybeWithScheme(String connectionString, String defaultPrefix) {
        URI uri;
        String c = connectionString.toLowerCase(Locale.ROOT);
        boolean hasAnHttpPrefix = c.startsWith(HTTP_PREFIX) || c.startsWith(HTTPS_PREFIX);
        try {
            uri = new URI(connectionString);
        } catch (URISyntaxException e) {

            if (hasAnHttpPrefix == false) {
                return parseMaybeWithScheme(defaultPrefix + connectionString, null);
            }
            URISyntaxException s = CredentialsRedaction.redactedURISyntaxException(e);
            throw new IllegalArgumentException("Invalid connection configuration: " + s.getMessage(), s);
        }

        if (hasAnHttpPrefix == false) {
            if (uri.getHost() != null) { 
                throw new IllegalArgumentException(
                        "Invalid connection scheme [" + uri.getScheme() + "] configuration: only " + HTTP_SCHEME + " and " + HTTPS_SCHEME
                                + " protocols are supported");
            }

            if (connectionString.length() > 0) { 
                return parseMaybeWithScheme(defaultPrefix + connectionString, null);
            }
        }

        return uri;
    }

    public static class CredentialsRedaction {
        public static final Character REDACTION_CHAR = '*';
        private static final String USER_ATTR_NAME = "user";
        private static final String PASS_ATTR_NAME = "password";

        private static String redactAttributeInString(String string, String attrName, Character replacement) {
            String needle = attrName + "=";
            int attrIdx = string.toLowerCase(Locale.ROOT).indexOf(needle); 
            if (attrIdx >= 0) { 
                int attrEndIdx = attrIdx + needle.length();
                return string.substring(0, attrEndIdx) + String.valueOf(replacement).repeat(string.length() - attrEndIdx);
            }
            return string;
        }

        private static void redactValueForSimilarKey(String key, List<String> options, List<Map.Entry<String, String>> attrs,
                                                     Character replacement) {
            List<String> similar = StrUtil.findSimilar(key, options);
            for (String k : similar) {
                for (Map.Entry<String, String> e : attrs) {
                    if (e.getKey().equals(k)) {
                        e.setValue(String.valueOf(replacement).repeat(e.getValue().length()));
                    }
                }
            }
        }

        public static String redactCredentialsInRawUriQuery(String rawQuery, Character replacement) {
            List<Map.Entry<String, String>> attrs = new ArrayList<>();
            List<String> options = new ArrayList<>();

            String key, value;
            for (String param : StrUtil.split(rawQuery, "&")) {
                int eqIdx = param.indexOf('=');
                if (eqIdx <= 0) { 
                    value = eqIdx < 0 ? null : "";
                    key = redactAttributeInString(param, USER_ATTR_NAME, replacement);
                    key = redactAttributeInString(key, PASS_ATTR_NAME, replacement);
                } else {
                    key = param.substring(0, eqIdx);
                    value = param.substring(eqIdx + 1);
                    if (value.indexOf('=') >= 0) { 
                        value = redactAttributeInString(value, USER_ATTR_NAME, replacement);
                        value = redactAttributeInString(value, PASS_ATTR_NAME, replacement);
                    }
                    options.add(key);
                }
                attrs.add(new AbstractMap.SimpleEntry<>(key, value));
            }

            redactValueForSimilarKey(USER_ATTR_NAME, options, attrs, replacement);
            redactValueForSimilarKey(PASS_ATTR_NAME, options, attrs, replacement);

            StringBuilder sb = new StringBuilder(rawQuery.length());
            for (Map.Entry<String, String> a : attrs) {
                sb.append("&");
                sb.append(a.getKey());
                if (a.getValue() != null) {
                    sb.append("=");
                    sb.append(a.getValue());
                }
            }
            return sb.substring(1);
        }

        private static String editURI(URI uri, List<Map.Entry<Integer, Character>> faults, boolean hasPort) {
            StringBuilder sb = new StringBuilder();
            if (uri.getScheme() != null) {
                sb.append(uri.getScheme());
                sb.append("://");
            }
            if (uri.getRawUserInfo() != null) {
                sb.append("\0".repeat(uri.getRawUserInfo().length()));
                if (uri.getHost() != null) {
                    sb.append('@');
                }
            }
            if (uri.getHost() != null) {
                sb.append(uri.getHost());
            }
            if (hasPort || uri.getPort() > 0) {
                sb.append(':');
            }
            if (uri.getPort() > 0) {
                sb.append(uri.getPort());
            }
            if (uri.getRawPath() != null) {
                sb.append(uri.getRawPath());
            }
            if (uri.getQuery() != null) {
                sb.append('?');

                sb.append(redactCredentialsInRawUriQuery(uri.getRawQuery(), '\0'));
            }
            if (uri.getRawFragment() != null) {
                sb.append('#');
                sb.append(uri.getRawFragment());
            }

            Collections.reverse(faults);
            for (Map.Entry<Integer, Character> e : faults) {
                int idx = e.getKey();
                if (idx >= sb.length()) {
                    sb.append(e.getValue());
                } else {
                    sb.insert(idx,
                            (sb.charAt(idx) == '\0' && (idx + 1 >= sb.length() || sb.charAt(idx + 1) == '\0')) ? '\0' : e.getValue());
                }
            }

            StringBuilder ret = new StringBuilder();
            sb.chars().forEach(x -> ret.append(x == '\0' ? REDACTION_CHAR : (char) x));

            return ret.toString();
        }

        private static String redactCredentialsInURLString(String urlString) {
            List<Map.Entry<Integer, Character>> faults = new ArrayList<>();

            boolean hasPort = false;
            for (StringBuilder sb = new StringBuilder(urlString); sb.length() > 0; ) {
                try {

                    URI uri = new URI(sb.toString()).parseServerAuthority();
                    return editURI(uri, faults, hasPort);
                } catch (URISyntaxException use) {
                    int idx = use.getIndex();
                    if (idx < 0 || idx >= sb.length()) {
                        break; 
                    }
                    if (use.getReason().equals("Illegal character in port number")) {

                        hasPort = true;
                    }
                    faults.add(new AbstractMap.SimpleImmutableEntry<>(use.getIndex(), sb.charAt(idx)));
                    sb.deleteCharAt(idx);
                }
            }
            return null;
        }

        public static String redactCredentialsInConnectionString(String connectionString) {
            if (connectionString.startsWith(HTTP_PREFIX.toUpperCase(Locale.ROOT))
                    || connectionString.startsWith(HTTPS_PREFIX.toUpperCase(Locale.ROOT))

                    || connectionString.length() < "_:_@_".length()

                    || (connectionString.indexOf('@') < 0 && connectionString.indexOf('?') < 0)) {
                return connectionString;
            }

            String cs = connectionString.toLowerCase(Locale.ROOT);
            boolean prefixed = cs.startsWith(HTTP_PREFIX) || cs.startsWith(HTTPS_PREFIX);
            String redacted = redactCredentialsInURLString((prefixed ? "" : HTTP_PREFIX) + connectionString);
            if (redacted == null) {
                return "<REDACTED> ; a capitalized scheme (HTTP|HTTPS) disables the redaction";
            }
            return prefixed ? redacted : redacted.substring(HTTP_PREFIX.length());
        }

        public static URISyntaxException redactedURISyntaxException(URISyntaxException e) {
            return new URISyntaxException(redactCredentialsInConnectionString(e.getInput()), e.getReason(), e.getIndex());
        }

    }

    public static URI removeQuery(URI uri, String connectionString, URI defaultURI) {
        try {
            return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath(), null, defaultURI.getFragment());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid connection configuration [" + connectionString + "]: " + e.getMessage(), e);
        }
    }

    public static URI appendSegmentToPath(URI uri, String segment) {
        if (uri == null) {
            throw new IllegalArgumentException("URI must not be null");
        }
        if (segment == null || segment.isEmpty() || "/".equals(segment)) {
            return uri;
        }

        String path = uri.getPath();
        String concatenatedPath = "";
        String cleanSegment = segment.startsWith("/") ? segment.substring(1) : segment;

        if (path == null || path.isEmpty()) {
            path = "/";
        }

        if (path.charAt(path.length() - 1) == '/') {
            concatenatedPath = path + cleanSegment;
        } else {
            concatenatedPath = path + "/" + cleanSegment;
        }
        try {
            return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), concatenatedPath,
                    uri.getQuery(), uri.getFragment());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid segment [" + segment + "] for URI [" + uri + "]: " + e.getMessage(), e);
        }
    }

}

