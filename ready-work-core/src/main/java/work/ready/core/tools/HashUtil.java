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
package work.ready.core.tools;

import work.ready.core.component.decrypt.AESDecryptor;
import work.ready.core.component.decrypt.Decryptor;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static java.nio.charset.StandardCharsets.UTF_8;

public class HashUtil {

	public static final long FNV_OFFSET_BASIS_64 = 0xcbf29ce484222325L;
	public static final long FNV_PRIME_64 = 0x100000001b3L;

	private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();
	private static final char[] CHAR_ARRAY = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

	public static long fnv1a64(String key) {
		long hash = FNV_OFFSET_BASIS_64;
		for(int i=0, size=key.length(); i<size; i++) {
			hash ^= key.charAt(i);
			hash *= FNV_PRIME_64;
		}
		return hash;
	}

	public static String md5(String srcStr){
		return hash("MD5", srcStr);
	}

	public static String sha1(String srcStr){
		return hash("SHA-1", srcStr);
	}

	public static String sha256(String srcStr){
		return hash("SHA-256", srcStr);
	}

	public static String sha384(String srcStr){
		return hash("SHA-384", srcStr);
	}

	public static String sha512(String srcStr){
		return hash("SHA-512", srcStr);
	}

	public static String hash(String algorithm, String srcStr) {
		try {
			MessageDigest md = MessageDigest.getInstance(algorithm);
			byte[] bytes = md.digest(srcStr.getBytes("utf-8"));
			return toHexString(bytes);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static String toHexString(byte b)
	{
		return toHexString(new byte[]{b}, 0, 1);
	}

	public static String toHexString(byte[] bytes)
	{
		return toHexString(bytes, 0, bytes.length);
	}

	public static String toHexString(byte[] bytes, int offset, int length) {
		StringBuilder ret = new StringBuilder();
		for (int i = offset; i < offset + length; i++) {
			ret.append(HEX_DIGITS[(bytes[i] >> 4) & 0x0f]);
			ret.append(HEX_DIGITS[bytes[i] & 0x0f]);
		}
		return ret.toString();
	}

	public static byte[] fromHexString(String s)
	{
		if (s.length() % 2 != 0)
			throw new IllegalArgumentException(s);
		byte[] array = new byte[s.length() / 2];
		for (int i = 0; i < array.length; i++)
		{
			int b = Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
			array[i] = (byte)(0xff & b);
		}
		return array;
	}

	public static void toHex(byte b, Appendable buf)
	{
		try
		{
			int d = 0xf & ((0xF0 & b) >> 4);
			buf.append((char)((d > 9 ? ('A' - 10) : '0') + d));
			d = 0xf & b;
			buf.append((char)((d > 9 ? ('A' - 10) : '0') + d));
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public static void toHex(int value, Appendable buf) throws IOException
	{
		int d = 0xf & ((0xF0000000 & value) >> 28);
		buf.append((char)((d > 9 ? ('A' - 10) : '0') + d));
		d = 0xf & ((0x0F000000 & value) >> 24);
		buf.append((char)((d > 9 ? ('A' - 10) : '0') + d));
		d = 0xf & ((0x00F00000 & value) >> 20);
		buf.append((char)((d > 9 ? ('A' - 10) : '0') + d));
		d = 0xf & ((0x000F0000 & value) >> 16);
		buf.append((char)((d > 9 ? ('A' - 10) : '0') + d));
		d = 0xf & ((0x0000F000 & value) >> 12);
		buf.append((char)((d > 9 ? ('A' - 10) : '0') + d));
		d = 0xf & ((0x00000F00 & value) >> 8);
		buf.append((char)((d > 9 ? ('A' - 10) : '0') + d));
		d = 0xf & ((0x000000F0 & value) >> 4);
		buf.append((char)((d > 9 ? ('A' - 10) : '0') + d));
		d = 0xf & value;
		buf.append((char)((d > 9 ? ('A' - 10) : '0') + d));

		Integer.toString(0, 36);
	}

	public static void toHex(long value, Appendable buf) throws IOException
	{
		toHex((int)(value >> 32), buf);
		toHex((int)value, buf);
	}

	public static byte convertHexDigit(byte c)
	{
		byte b = (byte)((c & 0x1f) + ((c >> 6) * 0x19) - 0x10);
		if (b < 0 || b > 15)
			throw new NumberFormatException("!hex " + c);
		return b;
	}

	public static int convertHexDigit(char c)
	{
		int d = ((c & 0x1f) + ((c >> 6) * 0x19) - 0x10);
		if (d < 0 || d > 15)
			throw new NumberFormatException("!hex " + c);
		return d;
	}

	public static int convertHexDigit(int c)
	{
		int d = ((c & 0x1f) + ((c >> 6) * 0x19) - 0x10);
		if (d < 0 || d > 15)
			throw new NumberFormatException("!hex " + c);
		return d;
	}

	public static String generateSalt(int saltLength) {
		StringBuilder salt = new StringBuilder(saltLength);
		ThreadLocalRandom random = ThreadLocalRandom.current();
		for (int i=0; i<saltLength; i++) {
			salt.append(CHAR_ARRAY[random.nextInt(CHAR_ARRAY.length)]);
		}
		return salt.toString();
	}

	public static String generateSaltForSha256() {
		return generateSalt(32);
	}

	public static String generateSaltForSha512() {
		return generateSalt(64);
	}

	public static boolean slowEquals(byte[] a, byte[] b) {
		if (a == null || b == null) {
			return false;
		}

		int diff = a.length ^ b.length;
		for(int i=0; i<a.length && i<b.length; i++) {
			diff |= a[i] ^ b[i];
		}
		return diff == 0;
    }

	public static String md5Hex(String message) {
		try {
			MessageDigest md =
					MessageDigest.getInstance("MD5");
			return toHexString(md.digest(message.getBytes("CP1252")));
		} catch (NoSuchAlgorithmException e) {
		} catch (UnsupportedEncodingException e) {
		}
		return null;
	}

	public static String generateStrongPasswordHash(String password) throws NoSuchAlgorithmException, InvalidKeySpecException
	{
		int iterations = 1000;
		char[] chars = password.toCharArray();
		byte[] salt = generateSalt(16).getBytes(UTF_8);

		PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, 64 * 8);
		SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
		byte[] hash = skf.generateSecret(spec).getEncoded();
		return iterations + ":" + toHexString(salt) + ":" + toHexString(hash);
	}

	public static boolean validatePassword(char[] originalPassword, String storedPassword) throws NoSuchAlgorithmException, InvalidKeySpecException
	{
		String[] parts = storedPassword.split(":");
		int iterations = Integer.parseInt(parts[0]);
		byte[] salt = fromHexString(parts[1]);
		byte[] hash = fromHexString(parts[2]);

		PBEKeySpec spec = new PBEKeySpec(originalPassword, salt, iterations, hash.length * 8);
		SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
		byte[] testHash = skf.generateSecret(spec).getEncoded();

		int diff = hash.length ^ testHash.length;
		for(int i = 0; i < hash.length && i < testHash.length; i++)
		{
			diff |= hash[i] ^ testHash[i];
		}
		return diff == 0;
	}

	public static String getCertFingerPrint(Certificate cert)  {
		byte [] digest = null;
		try {
			byte[] encCertInfo = cert.getEncoded();
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			digest = md.digest(encCertInfo);
		} catch (Exception e) {
			throw new RuntimeException("Exception:", e);
		}
		if (digest != null) {
			return toHexString(digest).toLowerCase();
		}
		return null;
	}

	public static Map<String, Object> decryptMap(Map<String, Object> map) {
		decryptNode(map);
		return map;
	}

	private static void decryptNode(Map<String, Object> map) {
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if (value instanceof String)
				map.put(key, decryptObject(value));
			else if (value instanceof Map)
				decryptNode((Map) value);
			else if (value instanceof List) {
				decryptList((List)value);
			}
		}
	}

	private static void decryptList(List list) {
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i) instanceof String) {
				list.set(i, decryptObject((list.get(i))));
			} else if(list.get(i) instanceof Map) {
				decryptNode((Map<String, Object>)list.get(i));
			} else if(list.get(i) instanceof List) {
				decryptList((List)list.get(i));
			}
		}
	}

	private static Object decryptObject(Object object) {
		if(object instanceof String) {
			if(((String)object).startsWith(Decryptor.CRYPT_PREFIX)) {
				Decryptor decryptor = new AESDecryptor();
				object = decryptor.decrypt((String)object);
			}

		}
		return object;
	}

	private static final String ALPHA_NUMERIC = "0123456789abcdefghijklmnopqrstuvwxyz";

	public static String alphaNumeric(int length) {
		StringBuilder builder = new StringBuilder(length);
		for (int i = 0; i < length; i++)
			builder.append(ALPHA_NUMERIC.charAt(ThreadLocalRandom.current().nextInt(ALPHA_NUMERIC.length())));
		return builder.toString();
	}

	public static double nextDouble(double min, double max) {
		return (max - min) * ThreadLocalRandom.current().nextDouble() + min;
	}

	public static int nextInt(int min, int max) {
		return min + ThreadLocalRandom.current().nextInt(max - min);
	}
}

