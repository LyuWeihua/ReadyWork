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

import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static work.ready.core.tools.CollectionUtil.toStringArray;
import static work.ready.core.tools.NetUtil.isIPv4Address;

public class StrUtil {

	private static final Log logger = LogFactory.getLog(StrUtil.class);

	public static final int INDEX_NOT_FOUND = -1;

	public static final String EMPTY = "";

    public static final String[] EMPTY_ARRAY = new String[0];

	public static final String SPACE = " ";

	public static final String SLASH = "/";
	public static final String PATH_TOP = "..";
	public static final String PATH_CURRENT = ".";

	public static String substringBefore(final String str, final String separator) {
		if (isEmpty(str) || separator == null) {
			return str;
		}
		if (separator.isEmpty()) {
			return EMPTY;
		}
		final int pos = str.indexOf(separator);
		if (pos == INDEX_NOT_FOUND) {
			return str;
		}
		return str.substring(0, pos);
	}

	public static String substringAfter(final String str, final String separator) {
		if (isEmpty(str)) {
			return str;
		}
		if (separator == null) {
			return EMPTY;
		}
		final int pos = str.indexOf(separator);
		if (pos == INDEX_NOT_FOUND) {
			return EMPTY;
		}
		return str.substring(pos + separator.length());
	}

	public static String substringAfterLast(final String str, final String separator) {
		if (isEmpty(str)) {
			return str;
		}
		if (isEmpty(separator)) {
			return EMPTY;
		}
		final int pos = str.lastIndexOf(separator);
		if (pos == INDEX_NOT_FOUND || pos == str.length() - separator.length()) {
			return EMPTY;
		}
		return str.substring(pos + separator.length());
	}

	public static String substringBeforeLast(final String str, final String separator) {
		if (isEmpty(str) || isEmpty(separator)) {
			return str;
		}
		final int pos = str.lastIndexOf(separator);
		if (pos == INDEX_NOT_FOUND) {
			return str;
		}
		return str.substring(0, pos);
	}

	public static int countMatches(final CharSequence str, final CharSequence sub) {
		if (isEmpty(str) || isEmpty(sub)) {
			return 0;
		}
		int count = 0;
		int idx = 0;
		while ((idx = str.toString().indexOf(sub.toString(), idx)) != INDEX_NOT_FOUND) {
			count++;
			idx += sub.length();
		}
		return count;
	}

	public static int countMatches(final CharSequence str, final char ch) {
		if (isEmpty(str)) {
			return 0;
		}
		int count = 0;

		for (int i = 0; i < str.length(); i++) {
			if (ch == str.charAt(i)) {
				count++;
			}
		}
		return count;
	}

	public static String trimToEmpty(final String str) {
		return str == null ? EMPTY : str.trim();
	}

	public static String removeEnd(final String str, final String remove) {
		if (isEmpty(str) || isEmpty(remove)) {
			return str;
		}
		if (str.endsWith(remove)) {
			return str.substring(0, str.length() - remove.length());
		}
		return str;
	}

	public static String stripEnd(final String str, final String stripChars) {
		int end;
		if (str == null || (end = str.length()) == 0) {
			return str;
		}

		if (stripChars == null) {
			while (end != 0 && Character.isWhitespace(str.charAt(end - 1))) {
				end--;
			}
		} else if (stripChars.isEmpty()) {
			return str;
		} else {
			while (end != 0 && stripChars.indexOf(str.charAt(end - 1)) != INDEX_NOT_FOUND) {
				end--;
			}
		}
		return str.substring(0, end);
	}

	public static String removeStartIgnoreCase(final String str, final String remove) {
		if (isEmpty(str) || isEmpty(remove)) {
			return str;
		}
		if (startsWithIgnoreCase(str, remove)) {
			return str.substring(remove.length());
		}
		return str;
	}

	public static String removeStart(final String str, final String remove) {
		if (isEmpty(str) || isEmpty(remove)) {
			return str;
		}
		if (str.startsWith(remove)){
			return str.substring(remove.length());
		}
		return str;
	}

	private static StringBuilder newStringBuilder(final int noOfItems) {
		return new StringBuilder(noOfItems * 16);
	}

	public static String inputStreamToString(InputStream inputStream, Charset charset) throws IOException {
		if (inputStream != null && inputStream.available() != -1) {
			ByteArrayOutputStream result = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int length;
			while ((length = inputStream.read(buffer)) != -1) {
				result.write(buffer, 0, length);
			}
			if (charset != null) {
				return result.toString(charset.name());
			}
			return result.toString(StandardCharsets.UTF_8.name());
		}
		return null;
	}

	public static boolean startsWithIgnoreCase(String str, String prefix) {
		if (str == null || prefix == null) {
			return false;
		}
		if (str.startsWith(prefix)) {
			return true;
		}
		if (str.length() < prefix.length()) {
			return false;
		}

		String lcStr = str.substring(0, prefix.length()).toLowerCase();
		String lcPrefix = prefix.toLowerCase();
		return lcStr.equals(lcPrefix);
	}

	public static boolean endsWithIgnoreCase(String str, String suffix) {
		if (str == null || suffix == null) {
			return false;
		}
		if (str.endsWith(suffix)) {
			return true;
		}
		if (str.length() < suffix.length()) {
			return false;
		}

		String lcStr = str.substring(str.length() - suffix.length()).toLowerCase();
		String lcSuffix = suffix.toLowerCase();
		return lcStr.equals(lcSuffix);
	}

	public static boolean containsIgnoreCase(final String str, final String searchStr) {
		if (str == null || searchStr == null) {
			return false;
		}
		return str.toLowerCase().contains(searchStr.toLowerCase());
	}

	public static String replaceWhitespace(String inString, char newChar, boolean withTrim, boolean withReduce){
		char[] org = inString.toCharArray();
		int len = org.length;
		int st = 0;
		boolean changed = false;
		if(withTrim) {
			while ((st < len) && ((org[st] <= ' ') || Character.isWhitespace(org[st]))) {
				st++;
				changed = true;
			}
			while ((st < len) && ((org[len - 1] <= ' ') || Character.isWhitespace(org[len - 1]))) {
				len--;
				changed = true;
			}
		}
		char[] buf = new char[len - st];
		boolean encounterSpace = false;
		int reduce = 0;
		for(int i = st; i < len; i++) {
			if(org[i] <= ' ' || Character.isWhitespace(org[i])) {
				if(withReduce && encounterSpace) {
					reduce++;
					continue;
				} else {
					encounterSpace = true;
					buf[i-reduce] = newChar;
				}
				changed = true;
			} else {
				encounterSpace = false;
				buf[i-reduce] = org[i];
			}
		}
		return changed ? new String(buf, 0, buf.length - reduce) : inString;
	}

	public static String replace(String inString, char[] oldChar, char newChar) {
		char[] org = inString.toCharArray();
		char[] buf = new char[org.length];
		boolean changed = false;
		for(int i = 0; i < org.length; i++){
			boolean replace = false;
			for(char c : oldChar) {
				if(org[i] == c) {
					changed = replace = true;
					break;
				}
			}
			buf[i] = replace ? newChar : org[i];
		}
		return changed ? new String(buf) : inString;
	}

	public static String replace(String inString, String oldPattern, String newPattern) {
		if (!hasLength(inString) || !hasLength(oldPattern) || newPattern == null) {
			return inString;
		}
		int index = inString.indexOf(oldPattern);
		if (index == -1) {

			return inString;
		}

		int capacity = inString.length();
		if (newPattern.length() > oldPattern.length()) {
			capacity += 16;
		}
		StringBuilder sb = new StringBuilder(capacity);

		int pos = 0;  
		int patLen = oldPattern.length();
		while (index >= 0) {
			sb.append(inString.substring(pos, index));
			sb.append(newPattern);
			pos = index + patLen;
			index = inString.indexOf(oldPattern, pos);
		}

		sb.append(inString.substring(pos));
		return sb.toString();
	}

	public static String replace(String value, Map<String, String> params) {
		if(value == null) {
			return null;
		}
		if(params == null || params.size() == 0) {
			return value;
		}
		StringBuilder b = new StringBuilder();
		Matcher matcher = tagPattern.matcher(value);
		int idx = 0;
		while(matcher.find()) {
			int start = matcher.start();
			b.append(value.substring(idx, start));
			idx = matcher.end();

			String k = matcher.group(1);
			String v = params.get(k);
			b.append(v!=null ? v : matcher.group(0));
		}
		b.append(value.substring(idx));
		return b.toString();
	}

	public static String delete(String inString, String pattern) {
		return replace(inString, pattern, "");
	}

	public static String deleteAny(String inString, String charsToDelete) {
		if (!hasLength(inString) || !hasLength(charsToDelete)) {
			return inString;
		}

		StringBuilder sb = new StringBuilder(inString.length());
		for (int i = 0; i < inString.length(); i++) {
			char c = inString.charAt(i);
			if (charsToDelete.indexOf(c) == -1) {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	public static Locale parseLocale(String localeValue) {
		String[] tokens = tokenizeLocaleSource(localeValue);
		if (tokens.length == 1) {
			validateLocalePart(localeValue);
			Locale resolved = Locale.forLanguageTag(localeValue);
			return (resolved.getLanguage().length() > 0 ? resolved : null);
		}
		return parseLocaleTokens(localeValue, tokens);
	}

	public static Locale parseLocaleString(String localeString) {
		return parseLocaleTokens(localeString, tokenizeLocaleSource(localeString));
	}

	private static String[] tokenizeLocaleSource(String localeSource) {
		return split(localeSource, "_ ", false, false, false);
	}

	private static Locale parseLocaleTokens(String localeString, String[] tokens) {
		String language = (tokens.length > 0 ? tokens[0] : "");
		String country = (tokens.length > 1 ? tokens[1] : "");
		validateLocalePart(language);
		validateLocalePart(country);

		String variant = "";
		if (tokens.length > 2) {

			int endIndexOfCountryCode = localeString.indexOf(country, language.length()) + country.length();

			variant = trimLeading(localeString.substring(endIndexOfCountryCode));
			if (variant.startsWith("_")) {
				variant = trimLeadingChar(variant, '_');
			}
		}

		if (variant.isEmpty() && country.startsWith("#")) {
			variant = country;
			country = "";
		}

		return (language.length() > 0 ? new Locale(language, country, variant) : null);
	}

	private static void validateLocalePart(String localePart) {
		for (int i = 0; i < localePart.length(); i++) {
			char ch = localePart.charAt(i);
			if (ch != ' ' && ch != '_' && ch != '-' && ch != '#' && !Character.isLetterOrDigit(ch)) {
				throw new IllegalArgumentException(
						"Locale part \"" + localePart + "\" contains invalid characters");
			}
		}
	}

	@Deprecated
	public static String toLanguageTag(Locale locale) {
		return locale.getLanguage() + (notBlank(locale.getCountry()) ? "-" + locale.getCountry() : "");
	}

	public static TimeZone parseTimeZoneString(String timeZoneString) {
		TimeZone timeZone = TimeZone.getTimeZone(timeZoneString);
		if ("GMT".equals(timeZone.getID()) && !timeZoneString.startsWith("GMT")) {

			throw new IllegalArgumentException("Invalid time zone specification '" + timeZoneString + "'");
		}
		return timeZone;
	}

	public static String[] split(String str, String delimiters) {
		return split(str, delimiters, false, true, true);
	}

	public static String[] split(
			String text, String delimiters, boolean escapeQuote, boolean trimTokens, boolean ignoreEmptyTokens) {

		if (text == null) {
			return new String[0];
		}

		StringTokenizer st = escapeQuote ? new QuotedStringTokenizer(text, delimiters, false, true) : new StringTokenizer(text, delimiters);
		List<String> tokens = new ArrayList<>();
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (trimTokens) {
				token = token.trim();
			}
			if (!ignoreEmptyTokens || token.length() > 0) {
				tokens.add(token);
			}
		}
		return toStringArray(tokens);
	}

	public static String[] split(String text, char delimiter) {
		List<String> tokens = new ArrayList<>();
		int start = 0;
		while (true) {
			int next = text.indexOf(delimiter, start);
			if (next == -1) break;
			tokens.add(text.substring(start, next));
			start = next + 1;
		}
		if (start == 0) return new String[]{text};
		else tokens.add(text.substring(start));
		return tokens.toArray(new String[0]);
	}

	public static Set<String> splitToSet(String src, String regex) {
		if (src == null) {
			return null;
		}

		String[] strings = src.split(regex);
		Set<String> set = new LinkedHashSet<>();
		for (String s : strings) {
			if (isBlank(s)) {
				continue;
			}
			set.add(s.trim());
		}
		return set;
	}

	public static String join(String[] stringArray) {
		StringBuilder sb = new StringBuilder();
		for (String s : stringArray) {
			sb.append(s);
		}
		return sb.toString();
	}

	public static String join(Object[] array, String delimiter) {
		if (CollectionUtil.isEmpty(array)) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < array.length; i++) {
			if (i > 0) {
				sb.append(delimiter);
			}
			sb.append(array[i]);
		}
		return sb.toString();
	}

	public static String join(Collection<?> collection, String separator) {
		return join(collection, separator, null, null);
	}

	public static String join(
			Collection<?> collection, String delimiter, String prefix, String suffix) {

		if (CollectionUtil.isEmpty(collection)) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		Iterator<?> it = collection.iterator();
		while (it.hasNext()) {
			if(prefix != null) sb.append(prefix);
			sb.append(it.next());
			if(suffix != null) sb.append(suffix);
			if (it.hasNext()) {
				sb.append(delimiter);
			}
		}
		return sb.toString();
	}

	public static String[] join(String str, String delimiter) {
		return join(str, delimiter, null);
	}

	public static String[] join(
			String str, String delimiter, String charsToDelete) {

		if (str == null) {
			return new String[0];
		}
		if (delimiter == null) {
			return new String[] {str};
		}

		List<String> result = new ArrayList<>();
		if (delimiter.isEmpty()) {
			for (int i = 0; i < str.length(); i++) {
				result.add(deleteAny(str.substring(i, i + 1), charsToDelete));
			}
		}
		else {
			int pos = 0;
			int delPos;
			while ((delPos = str.indexOf(delimiter, pos)) != -1) {
				result.add(deleteAny(str.substring(pos, delPos), charsToDelete));
				pos = delPos + delimiter.length();
			}
			if (str.length() > 0 && pos <= str.length()) {

				result.add(deleteAny(str.substring(pos), charsToDelete));
			}
		}
		return toStringArray(result);
	}

	public static String toUpperCase(String text) {
		if (text == null) return null;
		int length = text.length();
		for (int i = 0; i < length; i++) {
			if (isLowerCase(text.charAt(i))) {
				char[] chars = text.toCharArray();
				for (int j = i; j < length; j++) {
					char ch = chars[j];
					if (isLowerCase(ch)) {
						chars[j] = (char) (ch & 0x5F);
					}
				}
				return String.valueOf(chars);
			}
		}
		return text;
	}

	public static char toUpperCase(char ch) {
		if (isLowerCase(ch)) return (char) (ch & 0x5F);
		return ch;
	}

	public static String toLowerCase(String text) {
		if (text == null) return null;
		int length = text.length();
		for (int i = 0; i < length; i++) {
			if (isUpperCase(text.charAt(i))) {
				char[] chars = text.toCharArray();
				for (int j = i; j < length; j++) {
					char ch = chars[j];
					if (isUpperCase(ch)) {
						chars[j] = (char) (ch ^ 0x20);
					}
				}
				return String.valueOf(chars);
			}
		}
		return text;
	}

	public static char toLowerCase(char ch) {
		if (isUpperCase(ch)) return (char) (ch ^ 0x20);
		return ch;
	}

	public static String asUTFString(byte[] content) {
		return asUTFString(content, 0, content.length);
	}

	public static String asUTFString(byte[] content, int offset, int length) {
		return (content == null || length == 0 ? EMPTY : new String(content, offset, length, StandardCharsets.UTF_8));
	}

	public static byte[] toUTF(String string) {
		return string.getBytes(StandardCharsets.UTF_8);
	}

	public static String asHexString(byte[] content, int offset, int length) {
		StringBuilder buf = new StringBuilder();
		for (int i = offset; i < length; i++) {
			String hex = Integer.toHexString(0xFF & content[i]);
			if (hex.length() == 1) {
				buf.append('0');
			}
			buf.append(hex);
		}
		return buf.toString();
	}

	private static int levenshteinDistance(CharSequence one, CharSequence another, int threshold) {
		int n = one.length();
		int m = another.length();

		if (n == 0) {
			return m <= threshold ? m : -1;
		}
		else if (m == 0) {
			return n <= threshold ? n : -1;
		}

		if (n > m) {

			final CharSequence tmp = one;
			one = another;
			another = tmp;
			n = m;
			m = another.length();
		}

		int p[] = new int[n + 1]; 
		int d[] = new int[n + 1]; 
		int _d[];

		final int boundary = Math.min(n, threshold) + 1;
		for (int i = 0; i < boundary; i++) {
			p[i] = i;
		}

		Arrays.fill(p, boundary, p.length, Integer.MAX_VALUE);
		Arrays.fill(d, Integer.MAX_VALUE);

		for (int j = 1; j <= m; j++) {
			final char t_j = another.charAt(j - 1);
			d[0] = j;

			final int min = Math.max(1, j - threshold);
			final int max = (j > Integer.MAX_VALUE - threshold) ? n : Math.min(n, j + threshold);

			if (min > max) {
				return -1;
			}

			if (min > 1) {
				d[min - 1] = Integer.MAX_VALUE;
			}

			for (int i = min; i <= max; i++) {
				if (one.charAt(i - 1) == t_j) {

					d[i] = p[i - 1];
				}
				else {

					d[i] = 1 + Math.min(Math.min(d[i - 1], p[i]), p[i - 1]);
				}
			}

			_d = p;
			p = d;
			d = _d;
		}

		if (p[n] <= threshold) {
			return p[n];
		}
		return -1;
	}

	public static List<String> findSimilar(CharSequence match, Collection<String> potential) {
		List<String> list = new ArrayList<String>(3);

		int maxDistance = 5;

		for (String string : potential) {
			int dist = levenshteinDistance(match, string, maxDistance);
			if (dist >= 0) {
				if (dist < maxDistance) {
					maxDistance = dist;
					list.clear();
					list.add(string);
				}
				else if (dist == maxDistance) {
					list.add(string);
				}
			}
		}

		return list;
	}

	public static String pathNormalize(String path) {
		if (path == null) {
			return null;
		}
		String pathToUse = path.replace("\\", SLASH);

		int prefixIndex = pathToUse.indexOf(":");
		String prefix = "";
		if (prefixIndex != -1) {
			prefix = pathToUse.substring(0, prefixIndex + 1);
			if (prefix.contains(SLASH)) {
				prefix = "";
			}
			else {
				pathToUse = pathToUse.substring(prefixIndex + 1);
			}
		}
		if (pathToUse.startsWith(SLASH)) {
			prefix = prefix + SLASH;
			pathToUse = pathToUse.substring(1);
		}

		String[] pathList = split(pathToUse, SLASH);
		List<String> pathTokens = new LinkedList<String>();
		int tops = 0;

		for (int i = pathList.length - 1; i >= 0; i--) {
			String element = pathList[i];
			if (PATH_CURRENT.equals(element)) {

			}
			else if (PATH_TOP.equals(element)) {

				tops++;
			}
			else {
				if (tops > 0) {

					tops--;
				}
				else {
					pathTokens.add(0, element);
				}
			}
		}

		for (int i = 0; i < tops; i++) {
			pathTokens.add(0, PATH_TOP);
		}

		return prefix + join(pathTokens, SLASH);
	}

	public static boolean isLowerCase(char ch) {
		return ch >= 'a' && ch <= 'z';
	}

	public static boolean isUpperCase(char ch) {
		return ch >= 'A' && ch <= 'Z';
	}

	public static boolean isUpperCase(String text) {
		if (text == null) return false;
		int length = text.length();
		for (int i = 0; i < length; i++) {
			if (!isUpperCase(text.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	public static boolean isDigit(char ch) {
		return ch >= '0' && ch <= '9';
	}

	public static boolean isLetter(char ch) {
		return isLowerCase(ch) || isUpperCase(ch);
	}

	public static String firstCharToLowerCase(String str) {
		char firstChar = str.charAt(0);
		if (firstChar >= 'A' && firstChar <= 'Z') {
			char[] arr = str.toCharArray();
			arr[0] += ('a' - 'A');
			return new String(arr);
		}
		return str;
	}

	public static String firstCharToUpperCase(String str) {
		char firstChar = str.charAt(0);
		if (firstChar >= 'a' && firstChar <= 'z') {
			char[] arr = str.toCharArray();
			arr[0] -= ('a' - 'A');
			return new String(arr);
		}
		return str;
	}

	public static String toCamelCase(String stringWithUnderline) {
		if (stringWithUnderline.indexOf('_') == -1) {
			return isUpperCase(stringWithUnderline) ? toLowerCase(stringWithUnderline) : stringWithUnderline;
		}

		stringWithUnderline = stringWithUnderline.toLowerCase();
		char[] fromArray = stringWithUnderline.toCharArray();
		char[] toArray = new char[fromArray.length];
		int j = 0;
		for (int i=0; i<fromArray.length; i++) {
			if (fromArray[i] == '_') {

				i++;
				if (i < fromArray.length) {
					toArray[j++] = Character.toUpperCase(fromArray[i]);
				}
			}
			else {
				toArray[j++] = fromArray[i];
			}
		}
		return new String(toArray, 0, j);
	}

	public static boolean slowEquals(String a, String b) {
		byte[] aBytes = (a != null ? a.getBytes() : null);
		byte[] bBytes = (b != null ? b.getBytes() : null);
		return HashUtil.slowEquals(aBytes, bBytes);
	}

	public static boolean equals(String a, String b) {
		return a == null ? b == null : a.equals(b);
	}

    public static final long ONE_KB = 1024;

    public static final long ONE_MB = ONE_KB * ONE_KB;

    public static final long ONE_GB = ONE_KB * ONE_MB;

    public static String humanReadableSize(long bytes) {
        return humanReadableSize(bytes,
                new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.ROOT)));
    }

    public static String humanReadableSize(long bytes, DecimalFormat df) {
        if (bytes / ONE_GB > 0) {
            return df.format((float) bytes / ONE_GB) + " GB";
        } else if (bytes / ONE_MB > 0) {
            return df.format((float) bytes / ONE_MB) + " MB";
        } else if (bytes / ONE_KB > 0) {
            return df.format((float) bytes / ONE_KB) + " KB";
        } else {
            return bytes + " bytes";
        }
    }

	public static String getRandomUUID() {
		return java.util.UUID.randomUUID().toString().replace("-", "");
	}

	public static String getUrlSafeUUID() {
		UUID id = UUID.randomUUID();
		ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
		bb.putLong(id.getMostSignificantBits());
		bb.putLong(id.getLeastSignificantBits());
		return Base64.getUrlEncoder().encodeToString(bb.array());
	}

	public static boolean isValidVersion(String version){
		return version.matches("[1-9]\\d*\\.\\d+\\.\\d+\\.\\d{6}");
	}

	private static Pattern statusCodePattern = Pattern.compile("(FATAL|ERROR|WARN|INFO|SUCCESS|DEBUG|TRACE)\\d{5}");
	public static boolean isValidStatusCode(String code) {
		return statusCodePattern.matcher(code).matches();
	}

	public static String getJarVersion() {
		String path = StrUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		logger.debug("path = " + path);
		String ver = null;
		if(path.endsWith(".jar")) {
			int endIndex = path.indexOf(".jar");
			int startIndex = path.lastIndexOf("/");
			String jarName = path.substring(startIndex + 1, endIndex);
			ver = jarName.substring(jarName.lastIndexOf("-") + 1);
		}
		return ver;
	}

	private static final String LINE_SPLIT = "[\r\n]+";

	@SuppressWarnings("unchecked")
	public static List<String> linesToList(String lines) {
		if(isBlank(lines)) {
			return Collections.EMPTY_LIST;
		}
		List<String> list = new ArrayList<String>();
		for(String line:lines.split(LINE_SPLIT)) {
			if(isBlank(line)==false) {
				list.add(line.trim());
			}
		}
		return list;
	}

	public static String listToLines(List<String> list) {
		if(list == null || list.size() == 0) {
			return "";
		}
		StringBuilder lines = new StringBuilder();
		for(String line:list) {
			lines.append(line);
			lines.append('\n');
		}
		lines.deleteCharAt(lines.length()-1);
		return lines.toString();
	}

	public static boolean splitContains(String content, String contain) {
		if(isBlank(content)) {
			return false;
		}
		String[] splits = content.split("[,;]");
		for(String s : splits) {
			if(s.equals(contain)) {
				return true;
			}
		}
		return false;
	}

	public static String digest(String content, String algorithm) {
		byte[] digest = null;
		MessageDigest md;
		try {
			md = MessageDigest.getInstance(algorithm);
			digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
		} catch (Exception e) {
			logger.warn("fail to digest " + algorithm + " of content: " + content, e);
		}
		return toHexString(digest);
	}

	public static boolean hasLength(String string) {
		return string != null && string.length() > 0;
	}

	public static boolean hasAnyBlank(String ... strs) {
		if(strs==null || strs.length==0) {
			return false;
		}
		for(String str : strs) {
			if(isBlank(str)) {
				return true;
			}
		}
		return false;
	}

	public static boolean hasNoneBlank(String ... strs) {
		if(strs==null || strs.length==0) {
			return true;
		}
		for(String str : strs) {
			if(isBlank(str)) {
				return false;
			}
		}
		return true;
	}

	public static boolean isEmpty(final CharSequence cs) {
		return cs == null || cs.length() == 0;
	}

	public static boolean isBlank(String str) {
		if(!hasLength(str)) {
			return true;
		}
		for (int i = 0; i < str.length(); i++) {
			if (!Character.isWhitespace(str.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	public static boolean notBlank(String str) {
		return !isBlank(str);
	}

	public static boolean notBlank(String... strings) {
		if (strings == null || strings.length == 0) {
			return false;
		}
		for (String str : strings) {
			if (isBlank(str)) {
				return false;
			}
		}
		return true;
	}

	public static boolean notNull(Object... params) {
		if (params == null) {
			return false;
		}
		for (Object obj : params) {
			if (obj == null) {
				return false;
			}
		}
		return true;
	}

	public static String requireNonBlank(String string) {
		if (isBlank(string))
			throw new NullPointerException();
		return string;
	}

	public static String requireNonBlank(String string, String message) {
		if (isBlank(string))
			throw new NullPointerException(message);
		return string;
	}

	public static String defaultIfBlank(String str, String defaultValue) {
		return isBlank(str) ? defaultValue : str;
	}

	public static String trim(String str) {
		if(!hasLength(str)) {
			return str;
		}
		int from = 0, to = str.length();
		while(from<to && Character.isWhitespace(str.charAt(from))) {
			from++;
		}
		while(to>from && Character.isWhitespace(str.charAt(to-1))) {
			to--;
		}
		return str.substring(from, to);
	}

	public static String trimAll(String str) {
		if (!hasLength(str)) {
			return str;
		}

		int len = str.length();
		StringBuilder sb = new StringBuilder(str.length());
		for (int i = 0; i < len; i++) {
			char c = str.charAt(i);
			if (!Character.isWhitespace(c)) {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	public static String trimLeading(String str) {
		if (!hasLength(str)) {
			return str;
		}

		StringBuilder sb = new StringBuilder(str);
		while (sb.length() > 0 && Character.isWhitespace(sb.charAt(0))) {
			sb.deleteCharAt(0);
		}
		return sb.toString();
	}

	public static String trimTrailing(String str) {
		if (!hasLength(str)) {
			return str;
		}

		StringBuilder sb = new StringBuilder(str);
		while (sb.length() > 0 && Character.isWhitespace(sb.charAt(sb.length() - 1))) {
			sb.deleteCharAt(sb.length() - 1);
		}
		return sb.toString();
	}

	public static String trimLeadingChar(String str, char leadingCharacter) {
		if (!hasLength(str)) {
			return str;
		}

		StringBuilder sb = new StringBuilder(str);
		while (sb.length() > 0 && sb.charAt(0) == leadingCharacter) {
			sb.deleteCharAt(0);
		}
		return sb.toString();
	}

	public static String trimTrailingChar(String str, char trailingCharacter) {
		if (!hasLength(str)) {
			return str;
		}

		StringBuilder sb = new StringBuilder(str);
		while (sb.length() > 0 && sb.charAt(sb.length() - 1) == trailingCharacter) {
			sb.deleteCharAt(sb.length() - 1);
		}
		return sb.toString();
	}

	public static String firstNotBlank(String ... strings) {
		if(strings==null || strings.length==0) {
			return null;
		}
		for(String string : strings) {
			if(!isBlank(string)) {
				return string;
			}
		}
		return null;
	}

	public static boolean isClassName(String name){
		return name.matches("[a-zA-Z]+[0-9a-zA-Z_]*(\\.[a-zA-Z]+[0-9a-zA-Z_]*)*\\.[a-zA-Z]+[0-9a-zA-Z_]*");
	}

	public static boolean isHostName(String name, boolean checkLocal){
		boolean result = false;
		if(notBlank(name)) {
			if (checkLocal && name.matches("[a-zA-Z0-9][-a-zA-Z0-9]{0,62}")){
				result = true;
			} else {
				result = name.matches("^(?=^.{3,255}$)[a-zA-Z0-9][-a-zA-Z0-9]{0,62}(\\.[a-zA-Z0-9][-a-zA-Z0-9]{0,62})+$");
			}
		}
		return result;
	}

	public static String rootDomain(String domain) {
		if(!hasLength(domain)) {
			return domain;
		}
		int dot = domain.lastIndexOf('.');
		if(dot <= 0 || isIp(domain)) {
			return domain;
		}
		dot = domain.lastIndexOf('.', dot-1);
		return dot==-1 ? domain : domain.substring(dot+1);
	}

	public static String rootUrl(String url) {
		int dot = url.indexOf("://"), slash = url.indexOf('/', dot+3);
		return slash < 0 ? url : url.substring(0, slash+1);
	}

	public static boolean globMatch(String pattern, String str) {
		if (pattern == null || str == null) {
			return false;
		}
		int firstIndex = pattern.indexOf('*');
		if (firstIndex == -1) {
			return pattern.equals(str);
		}
		if (firstIndex == 0) {
			if (pattern.length() == 1) {
				return true;
			}
			int nextIndex = pattern.indexOf('*', firstIndex + 1);
			if (nextIndex == -1) {
				return str.endsWith(pattern.substring(1));
			} else if (nextIndex == 1) {

				return globMatch(pattern.substring(1), str);
			}
			String part = pattern.substring(1, nextIndex);
			int partIndex = str.indexOf(part);
			while (partIndex != -1) {
				if (globMatch(pattern.substring(nextIndex), str.substring(partIndex + part.length()))) {
					return true;
				}
				partIndex = str.indexOf(part, partIndex + 1);
			}
			return false;
		}
		return (str.length() >= firstIndex &&
				pattern.substring(0, firstIndex).equals(str.substring(0, firstIndex)) &&
				globMatch(pattern.substring(firstIndex), str.substring(firstIndex)));
	}

	public static boolean equals(Object left, Object right) {
		return (left == right) || (left == null && right == null) || (left != null && right != null && left.equals(right));
	}

	public static boolean emptyEquals(Object left, Object right) {
		return ("".equals(left) && right == null) || (left == null && "".equals(right)) || equals(left, right);
	}

	public static boolean primitiveEquals(Object left, Object right) {
		return emptyEquals(left, right) || String.valueOf(left).equals(String.valueOf(right));
	}

	public static String nullOrString(String str) {
		return hasLength(str) && !"null".equalsIgnoreCase(str) ? str : null;
	}

	public static String format(String pattern, Object... params) {
		return java.lang.String.format(pattern, params);
	}

	public static String toString(Object obj) {
		if(obj != null) {
			if(obj instanceof Date) {
				return DateUtil.format((Date)obj);
			} else if(obj instanceof Object[]) {
				Object[] arr = (Object[])obj;
				int iMax = arr.length-1;
				if(iMax == -1) {
					return "[]";
				}
				StringBuilder sb = new StringBuilder("[");
				for(int i=0; ; i++) {
					sb.append(toString(arr[i]));
					if(i == iMax) {
						return sb.append(']').toString();
					}
					sb.append(',');
				}
			} else {
				return obj.toString();
			}
		}else {
			return "null";
		}
	}

	public static String randomString(int length) {
		if (length < 1) {
			throw new IllegalArgumentException("random string length must be greater than 0.");
		}

		char [] randBuffer = new char[length];
		for (int i = 0; i < randBuffer.length; i++) {
			randBuffer[i] = numbersAndLetters[randomGenerator.nextInt(numbersAndLetters.length)];
		}
		return new String(randBuffer);
	}

	public static String toDBC(String input) {
		char c[] = input.toCharArray();
		for (int i = 0; i < c.length; i++) {
			if (c[i] == '\u3000') {
				c[i] = ' ';
			} else if ((c[i] > '\uFF00') && (c[i] < '\uFF5F')) {
				c[i] = (char) (c[i] - 65248);
			}
		}
		return new String(c);
	}

	public static String toHexString(byte[] bytes) {
		StringBuilder hex = new StringBuilder();
		if (bytes != null) {
			for (byte bt : bytes) {
				hex.append(hexChar[(bt & 0xf0) >>> 4]);
				hex.append(hexChar[bt & 0x0f]);
			}
		}
		return hex.toString();
	}

	public static byte[] fromHexString(String hexString) {
		if(hexString == null) {
			return null;
		}
		byte[] bytes = new byte[hexString.length() / 2];
		for(int i = 0; i < hexString.length(); i++) {
			char c = hexString.charAt(i);
			byte b = -1;
			for(int j = 0; j < hexChar.length; j++) {
				if(c == hexChar[j]) {
					b = (byte)j;
					break;
				}
			}
			if(b == -1) {
				return null;
			}
			if(i % 2 == 0) {
				bytes[i / 2] = (byte)(b << 4);
			}else {
				bytes[i / 2] += b;
			}
		}
		return bytes;
	}

	public static String toSBC(String input) {
		char c[] = input.toCharArray();
		for (int i = 0; i < c.length; i++) {
			if (c[i] == ' ') {
				c[i] = '\u3000';
			} else if ((c[i] < 127) && (c[i] > 32)) {
				c[i] = (char) (c[i] + 65248);
			}
		}
		return new String(c);
	}

	public static boolean isHasChinese(String source){
		char[] chars=source.toCharArray();
		for(int i=0;i<chars.length;i++){
			if(isChinese(chars[i])) {
				return true;
			}
		}
		return false;
	}

	public static boolean isChinese(String source) {
		char[] chars=source.toCharArray();
		for(int i=0;i<chars.length;i++){
			if(isChinese(chars[i])==false) {
				return false;
			}
		}
		return true;
	}

	public static boolean isChinese(char c) {

		Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
		return ub.equals(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS)
				|| ub.equals(Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS)
				|| ub.equals(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A)
				|| ub.equals(Character.UnicodeBlock.GENERAL_PUNCTUATION)
				|| ub.equals( Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION)
				|| ub.equals(Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS);
	}

	public static boolean isIdentifier(String string) {
		if(!hasLength(string)) {
			return false;
		}
		char[] chars=string.toCharArray();
		for(int i=0;i<chars.length;i++) {
			if(i==0 && chars[i]!='_' && !CharUtil.isAsciiAlpha(chars[i]))
			{

				return false;
			}
			if(i>0 && !CharUtil.isAsciiPrintable(chars[i]))
			{

				return false;
			}
		}
		return true;
	}

	public static boolean isBusinessNo(String bizNo) {
		int businessNoLength = 15;
		if(isNumbers(bizNo) && bizNo.length()==businessNoLength) {
			return true;
		}
		if(isSccNumber(bizNo)) {
			return true;
		}
		return false;
	}

	public static boolean isSccNumber(String sccNo) {
		int sccNumberLength = 18;
		if(sccNo==null || sccNo.length()!=sccNumberLength) {
			return false;
		}
		if(!sccPattern.matcher(sccNo).matches()) {
			return false;
		}
		final char[] cs = sccNo.toUpperCase().toCharArray();
		int t = 0, p, c;
		int end = 17;
		for(int i=0;i<end;i++) {
			c = cs[i];
			p = sccwf.indexOf(c);
			if(p<0) {
				return false;
			}
			t += sccwi[i]*p;
		}
		p = 31 - t % 31;
		c = p<10 ? Character.forDigit(p, 10) : sccwf.charAt(p);
		return c == cs[end];
	}

	public static boolean isBarcode(String barcode) {
		if(isBlank(barcode) || !isNumbers(barcode)) {
			return false;
		}
		int length = barcode.length();
		if(length <= 1) {
			return false;
		}
		int calc = calcBarcode(barcode.substring(0, length-1));
		return calc>=0 && calc<=9 && calc==(barcode.charAt(length-1)-'0');
	}

	public static int calcBarcode(String barcode) {
		if(isBlank(barcode) || !isNumbers(barcode)) {
			return -1;
		}
		int sums = 0, length = barcode.length();
		for(int i=1;i<=length;i++) {
			char c = barcode.charAt(length-i);
			if(i%2==0) {
				sums += 3*(c-'0');
			}else {
				sums += c-'0';
			}
		}
		sums %= 10;
		return sums==0 ? 0 : 10-sums;
	}

	public static boolean isHexString(String string) {
		char[] chars=string.toCharArray();
		for(int i=0;i<chars.length;i++){
			if(!isHexChar(chars[i])) {
				return false;
			}
		}
		return true;
	}

	public static boolean isHexChar(char c) {
		for(int i = 0; i< hexChar.length; i++) {
			if(c == hexChar[i]) {
				return true;
			}
		}
		return false;
	}

	public static String escapeHTMLTags(String in) {
		if (in == null) {
			return null;
		}
		char ch;
		int i = 0;
		int last = 0;
		char[] input = in.toCharArray();
		int len = input.length;
		StringBuffer out = new StringBuffer((int)(len * 1.3));
		for (; i < len; i++) {
			ch = input[i];
			if (ch > '>') {

			}
			else if (ch == '<') {
				if (i > last) {
					out.append(input, last, i - last);
				}
				last = i + 1;
				out.append(LT_ENCODE);
			}
			else if (ch == '>') {
				if (i > last) {
					out.append(input, last, i - last);
				}
				last = i + 1;
				out.append(GT_ENCODE);
			}
			else if (ch == '"') {
				if (i > last) {
					out.append(input, last, i - last);
				}
				last = i + 1;
				out.append(QUOTE_ENCODE);
			}
			else if (ch == '&') {
				if (i > last) {
					out.append(input, last, i - last);
				}
				last = i + 1;
				out.append(AMP_ENCODE);
			}
		}
		if (last == 0) {
			return in;
		}
		if (i > last) {
			out.append(input, last, i - last);
		}
		return out.toString();
	}

	public static String unescapeFromXML(String string) {
		string = string.replaceAll("&lt;", "<");
		string = string.replaceAll("&gt;", ">");
		string = string.replaceAll("&quot;", "\"");
		return string.replaceAll("&amp;", "&");
	}
	public static Pattern getPattern(String pattern) {
		Pattern p = patterns.get(pattern);
		if(p==null) {
			p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
			patterns.put(pattern, p);
		}
		return p;
	}
	public static String getPatternString(String input, String pattern) {
		Pattern p = getPattern(pattern);
		return getPatternString(input, p);
	}
	public static String getPatternString(String input, Pattern pattern) {
		Matcher matcher = pattern.matcher(input);
		if(matcher.find()) {
			return matcher.group(1);
		}else {
			return null;
		}
	}
	public static boolean matches(String input, String pattern) {
		Pattern p = getPattern(pattern);
		return p.matcher(input).matches();
	}
	public static String getXmlTag(String input, String tag) {
		return getPatternString(input, "<"+tag+">[^<]*</"+tag+">");
	}

	public static boolean simpleMatch(String pattern, String str) {
		if (pattern == null || str == null) {
			return false;
		}
		int firstIndex = pattern.indexOf('*');
		if (firstIndex == -1) {
			return pattern.equals(str);
		}
		if (firstIndex == 0) {
			if (pattern.length() == 1) {
				return true;
			}
			int nextIndex = pattern.indexOf('*', firstIndex + 1);
			if (nextIndex == -1) {
				return str.endsWith(pattern.substring(1));
			}
			String part = pattern.substring(1, nextIndex);
			if (part.isEmpty()) {
				return simpleMatch(pattern.substring(nextIndex), str);
			}
			int partIndex = str.indexOf(part);
			while (partIndex != -1) {
				if (simpleMatch(pattern.substring(nextIndex), str.substring(partIndex + part.length()))) {
					return true;
				}
				partIndex = str.indexOf(part, partIndex + 1);
			}
			return false;
		}
		return (str.length() >= firstIndex &&
				pattern.substring(0, firstIndex).equals(str.substring(0, firstIndex)) &&
				simpleMatch(pattern.substring(firstIndex), str.substring(firstIndex)));
	}

	public static boolean simpleMatch(String[] patterns, String str) {
		if (patterns != null) {
			for (String pattern : patterns) {
				if (simpleMatch(pattern, str)) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean isUrl(String string) {
		if(!hasLength(string)) {
			return false;
		}
		return urlPattern.matcher(string).matches();
	}

	public static boolean isEmail(String string) {
		if(!hasLength(string)) {
			return false;
		}
		return emailPattern.matcher(string).matches();
	}

	public static boolean isMobile(String string) {
		if(!hasLength(string)) {
			return false;
		}
		return mobilePattern.matcher(string).matches();
	}

	public static int getMobileType(String string) {
		int mobileLength = 11;
		int mobilePrefixLength = 3;
		if(!hasLength(string) || string.length()<mobilePrefixLength || string.length()>mobileLength) {
			return -1;
		}
		if(string.length() < mobileLength) {
			StringBuilder sb = new StringBuilder(string);
			for(int i=string.length();i<mobileLength;i++) {
				sb.append('1');
			}
			string = sb.toString();
		}
		if(!isMobile(string)) {
			return -1;
		}
		if(mobile1.matcher(string).matches()) {
			return 1;
		}
		if(mobile2.matcher(string).matches()) {
			return 2;
		}
		if(mobile3.matcher(string).matches()) {
			return 3;
		}
		return 0;
	}

	public static boolean isTel(String tel) {
		if(!hasLength(tel)) {
			return false;
		}
		return telPattern.matcher(tel).matches();
	}

	public static boolean isIp(String ip) {
		if(!hasLength(ip)) {
			return false;
		}
		return isIPv4Address(ip);
	}

	public static boolean isPlateNumber(String plateNumber) {
		if(!hasLength(plateNumber)) {
			return false;
		}
		return plateNumberPattern.matcher(plateNumber).matches();
	}

	public static boolean isIdNumber(String idNumber) {
		int idLength1 = 15, idLength2 = 18, idLength = idNumber==null?0:idNumber.length();
		boolean idType1 = idLength==idLength1, idType2 = idLength==idLength2;
		if(idType1==false && idType2==false) {
			return false;
		}

		final char[] cs = idNumber.toUpperCase().toCharArray();

		int power = 0;
		for(int i=0; i<cs.length; i++){
			if(i==cs.length-1 && cs[i] == 'X')
			{
				break;
			}
			if(cs[i]<'0' || cs[i]>'9') {
				return false;
			}
			if(i < cs.length -1){
				power += (cs[i] - '0') * wi[i];
			}
		}

		String area = idNumber.substring(0,2);
		if(!Arrays.asList(areas).contains(area)) {
			return false;
		}

		String year = idType1 ? "19" + idNumber.substring(6,8) : idNumber.substring(6, 10);

		final int iyear = Integer.parseInt(year);
		int low = 1900;
		if(iyear < low || iyear > Calendar.getInstance().get(Calendar.YEAR))
		{

			return false;
		}

		int monthHigh = 12;
		String month = idType1 ? idNumber.substring(8, 10) : idNumber.substring(10,12);
		final int imonth = Integer.parseInt(month);
		if(imonth <1 || imonth > monthHigh){
			return false;
		}

		String day = idType1 ? idNumber.substring(10, 12) : idNumber.substring(12, 14);
		final int iday = Integer.parseInt(day);
		int dayHigh = 31;
		if(iday < 1 || iday > dayHigh) {
			return false;
		}

		if(idType1) {
			return true;
		}

		return cs[cs.length -1] == vi[power % 11];
	}

	public static boolean isOrganizationCode(String organizationCode) {
		int organLength1 = 9, organLength2 = 18, organLength = organizationCode==null?0:organizationCode.length();
		boolean organType1 = organLength==organLength1, organType2 = organLength==organLength2;
		if(organType1==false && organType2==false) {
			return false;
		}
		if(organType2) {
			return isSccNumber(organizationCode);
		}

		int c9 = 0, check=organizationCode.charAt(organLength-1)-'0';
		for(int i=0;i<organLength;i++) {
			char c = organizationCode.charAt(i);
			if(i<8) {
				if(!CharUtil.isAsciiNumeric(c) && !CharUtil.isAsciiAlphaUpper(c))
				{

					return false;
				}
				c9 += (c-'0')*orgWi[i];
			}else {
				if(c!='-' && !CharUtil.isAsciiNumeric(c)) {
					return false;
				}
			}
		}

		int eleven = 11, ten = 10;
		c9 = eleven-c9%eleven;
		if(c9==eleven) {
			c9=0;
		} else {
			if(c9==ten) {
				c9='X'-'0';
			}
		}
		return c9==check;
	}

	public static boolean isTaxRegistrationNo(String taxRegistrationNo) {
		int taxLength1 = 15, taxLength2 = 18, taxLength = taxRegistrationNo==null?0:taxRegistrationNo.length();
		boolean taxType1 = taxLength==taxLength1, taxType2 = taxLength==taxLength2;
		if(taxType1==false && taxType2==false) {
			return false;
		}
		if(taxType2) {
			return isSccNumber(taxRegistrationNo);
		}
		int pos = 6;
		if(taxType1==false || !isNumbers(taxRegistrationNo.substring(0, pos))) {
			return false;
		}
		if(!isOrganizationCode(taxRegistrationNo.substring(pos))) {

			return false;
		}
		return true;
	}

	public static boolean isNumbers(String numbers) {
		if(!hasLength(numbers)) {
			return false;
		}
		return getPattern("\\d+").matcher(numbers).matches();
	}

	public static boolean isDigit(char c, boolean checkChineseDigit) {
		if(checkChineseDigit) {
			char left = '\uFF00';
			char right = '\uFF5F';
			if (c > left && c < right) {
				c = (char) (c - 65248);
			}
		}
		return isDigit(c);
	}

	public static boolean isAlphaNumbers(String alphaNumbers) {
		if(!hasLength(alphaNumbers)) {
			return false;
		}
		return getPattern("[0-9a-zA-Z]+").matcher(alphaNumbers).matches();
	}

	public static boolean isDecimal(String decimal) {
		if(!hasLength(decimal)) {
			return false;
		}
		return getPattern("-?\\d+(\\.\\d+)?").matcher(decimal).matches();
	}

	public static boolean isMoney(String numbers) {
		if(!hasLength(numbers)) {
			return false;
		}
		return getPattern("-?\\d+(\\.\\d{1,2})?").matcher(numbers).matches();
	}

	public static boolean isBankCardNumber(String bankCard) {
		boolean isBadNumber = !isNumbers(bankCard) || (bankCard.length()!=16 && bankCard.length()!=19);
		if(isBadNumber) {

			return false;
		}
		char[] chars=bankCard.toCharArray();
		int len = chars.length, checkSum = 0, check = chars[len-1]-'0', noCheck = 2;

		for(int i=len-noCheck;i>=0;i--) {
			int n = chars[i]-'0';
			int j = len-i-1;

			if(j%2==1) {
				n *= 2;
				checkSum += n/10;
				checkSum += n%10;

			}else {
				checkSum += n;
			}
		}

		checkSum += check;
		return checkSum % 10 ==0;
	}

	public static String mask(String string, char c, int len, boolean reverse) {
		if(!hasLength(string)) {
			return string;
		}
		StringBuilder sb = new StringBuilder();
		int length = string.length();
		for(int i=0;i<length;i++) {
			if(len>0 && i<len) {
				sb.append(reverse ? string.charAt(i) : c);
			} else if(len<0 && i-length>=len) {
				sb.append(reverse ? string.charAt(i) : c);
			} else {
				sb.append(reverse ? c : string.charAt(i));
			}
		}
		return sb.toString();
	}

	public static boolean containsOneOf(String string, String... ones) {
		if(!hasLength(string)) {
			return false;
		}
		if(ones!=null && ones.length>0) {
			for(String one : ones) {
				if(string.contains(one)) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean containsOneOfIgnoreCase(String string, String... ones) {
		if(!hasLength(string)) {
			return false;
		}
		string=string.toLowerCase();
		if(ones!=null && ones.length>0) {
			for(String one : ones) {
				if(string.contains(one.toLowerCase())) {
					return true;
				}
			}
		}
		return false;
	}

	public static Map<String, String> params(String... param){
		Map<String, String> params = new HashMap<>(2);
		if(param!=null && param.length>1) {
			int step = 2;
			for(int idx = 0, l=param.length-1; idx < l; idx+=step) {
				params.put(param[idx], param[idx+1]);
			}
		}
		return params;
	}

	public static String expandVariables(String template, Map<String, String> variables) {
		Pattern pattern = Pattern.compile("\\$\\{(.+?)\\}");
		Matcher matcher = pattern.matcher(template);

		StringBuffer buffer = new StringBuffer();
		while (matcher.find()) {
			if (variables.containsKey(matcher.group(1))) {
				String replacement = variables.get(matcher.group(1));

				matcher.appendReplacement(buffer, replacement != null ? Matcher.quoteReplacement(replacement) : "null");
			}
		}
		matcher.appendTail(buffer);
		return buffer.toString();
	}

	public static String sqlParam(String sqlParam) {
		return sqlParam.replaceAll("([';]+|(--)+)", "");
	}

	public static Color getColor(String color) {
		if(isBlank(color)) {
			return null;
		}
		color = color.toLowerCase();
		String colorRegex = "(0x)?([0-9a-f]+)";
		if(color.matches(colorRegex)) {
			return new Color(Integer.parseInt(color.startsWith("0x")?color.substring(2):color, 16));
		}
		Matcher matcher = colorPattern.matcher(color);
		if(!matcher.matches()) {
			return null;
		}
		return new Color(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
	}

	public static String limited(String text, Charset charset, int bytes) {
		if(text==null || text.getBytes(charset).length < bytes) {
			return text;
		}
		char[] chars = text.toCharArray();
		CharBuffer cb = CharBuffer.allocate(1);
		ByteBuffer buf = ByteBuffer.allocate(bytes);
		int cnt = 0;
		for (int i = 0; i < chars.length; i++) {
			char c = chars[i];
			cb.put(c);
			cb.flip();
			ByteBuffer bb = charset.encode(cb);
			cnt += bb.array().length;
			if(cnt > bytes) {
				break;
			}
			buf.put(bb);
			cb.clear();
		}
		return new String(buf.array(), charset);
	}

	public static String toMysqlUtf8(String str) {
		if(!hasLength(str)) {
			return str;
		}
		StringBuilder sb = new StringBuilder();
		byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
		for(int i=0; i<bytes.length; i++) {
			byte b = bytes[i];
			if(CharUtil.isAscii((char)b)){
				sb.append(new String(bytes, i, 1, StandardCharsets.UTF_8));
			}else if((b & 0xE0) == 0xC0) {
				sb.append(new String(bytes, i++, 2, StandardCharsets.UTF_8));
			}else if((b & 0xF0) == 0xE0) {
				sb.append(new String(bytes, i, 3, StandardCharsets.UTF_8));
				i += 2;
			}else if((b & 0xF8) == 0xF0) {
				String str1 = new String(bytes, i, 4, StandardCharsets.UTF_8);
				try{
					sb.append(URLEncoder.encode(str1, StandardCharsets.UTF_8));
				}catch(Exception e) {
					logger.warn("fail to encode str: %s, ex: %s", str1, e.getMessage());
				}
				i += 3;
			}
		}
		return sb.toString();
	}

	public static String fromMysqlUtf8(String str) {
		if(!hasLength(str)) {
			return str;
		}
		try{
			return URLDecoder.decode(str, StandardCharsets.UTF_8);
		}catch(Exception ignore) {
			return str;
		}
	}

	private static Random randomGenerator = new Random(System.currentTimeMillis());
	private static char[] hexChar = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
	private static char[] numbersAndLetters = ("0123456789abcdefghijklmnopqrstuvwxyz" + "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ").toCharArray();
	private static char[] QUOTE_ENCODE = "&quot;".toCharArray();
	private static char[] AMP_ENCODE = "&amp;".toCharArray();
	private static char[] LT_ENCODE = "&lt;".toCharArray();
	private static char[] GT_ENCODE = "&gt;".toCharArray();
	private static String urlPatternString = "^([hH][tT][tT][pP]([sS]?)|[fF][tT][pP]|[fF][iI][lL][eE]):\\/\\/(\\S+\\.)+\\S{2,}$";
	private static String emailPatternString = "\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*";
	private static Pattern tagPattern = Pattern.compile("\\{(\\w+)\\}");
	private static Pattern urlPattern = Pattern.compile(urlPatternString, Pattern.CASE_INSENSITIVE);
	private static Pattern emailPattern = Pattern.compile(emailPatternString, Pattern.CASE_INSENSITIVE);
	private static Pattern mobilePattern = Pattern.compile("^(13[0-9]|14[5-9]|15[0-3,5-9]|16[2,5,6,7]|17[0-8]|18[0-9]|19[0-3,5-9])\\d{8}$");
	private static Pattern mobile1 = Pattern.compile("^1(3[4-9]|4[7]|5[0-27-9]|7[8]|8[2-478])\\d{8}$|^1705\\d{7}$"),
			mobile2 = Pattern.compile("^1(3[012]|4[5]|5[56]|7[6]|8[56])\\d{8}$|^1709\\d{7}$"),
			mobile3 = Pattern.compile("^1(33|53|77|8[019])\\d{8}$|^1700\\d{7}$");
	private static Pattern telPattern = Pattern.compile("(\\d{3,4}-?)?\\d{7,8}");
	private static Pattern plateNumberPattern = Pattern.compile("([\\u4E00-\\u9FA5][a-zA-Z]-?[a-zA-Z0-9]{5})|(WJ\\d{2}-?(\\d{5}|[\\u4E00-\\u9FA5]\\d{4}))");
	private static Pattern colorPattern = Pattern.compile("(\\d+),(\\d+),(\\d+)");
	private static Map<String, Pattern> patterns = new HashMap<>();
	private static String[] areas={"11","12","13","14","15","21","22","23","31","32","33","34","35","36","37","41","42","43","44","45","46","50","51","52","53","54","61","62","63","64","65","71","81","82","91"};
	private static int[] vi = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};
	private static int[] wi = { 7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};

	private static int[] orgWi = {3, 7, 9, 10, 5, 8, 4, 2};
	private static String sccPatternString = "^([0-9ABCDEFGHJKLMNPQRTUWXY]{2})(\\d{6})([0-9ABCDEFGHJKLMNPQRTUWXY]{9})([0-9ABCDEFGHJKLMNPQRTUWXY])$";
	private static Pattern sccPattern = Pattern.compile(sccPatternString);
	private static String sccwf = "0123456789ABCDEFGHJKLMNPQRTUWXY";

	private static int[] sccwi = {1, 3, 9, 27, 19, 26, 16, 17, 20, 29, 25, 13, 8, 24, 10, 30, 28};
}
