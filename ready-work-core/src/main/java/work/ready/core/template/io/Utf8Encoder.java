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

package work.ready.core.template.io;

public class Utf8Encoder extends Encoder {

	public static final Utf8Encoder me = new Utf8Encoder();

	public float maxBytesPerChar() {
		return 3.0F;
	}

	public int encode(char[] chars, int offset, int len, byte[] bytes) {
        int sl = offset + len;
        int dp = 0;
        int dlASCII = dp + Math.min(len, bytes.length);

        while (dp < dlASCII && chars[offset] < '\u0080') {
            bytes[dp++] = (byte) chars[offset++];
        }

        while (offset < sl) {
            char c = chars[offset++];
            if (c < 0x80) {

                bytes[dp++] = (byte) c;
            } else if (c < 0x800) {

                bytes[dp++] = (byte) (0xc0 | (c >> 6));
                bytes[dp++] = (byte) (0x80 | (c & 0x3f));
            } else if (c >= '\uD800' && c < ('\uDFFF' + 1)) { 
                final int uc;
                int ip = offset - 1;
                if (Character.isHighSurrogate(c)) {
                    if (sl - ip < 2) {
                        uc = -1;
                    } else {
                        char d = chars[ip + 1];
                        if (Character.isLowSurrogate(d)) {
                            uc = Character.toCodePoint(c, d);
                        } else {

                            bytes[dp++] = (byte) '?';
                            continue;
                        }
                    }
                } else {
                    if (Character.isLowSurrogate(c)) {

                        bytes[dp++] = (byte) '?';
                        continue;
                    } else {
                        uc = c;
                    }
                }

                if (uc < 0) {
                    bytes[dp++] = (byte) '?';
                } else {
                    bytes[dp++] = (byte) (0xf0 | ((uc >> 18)));
                    bytes[dp++] = (byte) (0x80 | ((uc >> 12) & 0x3f));
                    bytes[dp++] = (byte) (0x80 | ((uc >> 6) & 0x3f));
                    bytes[dp++] = (byte) (0x80 | (uc & 0x3f));
                    offset++; 
                }
            } else {

                bytes[dp++] = (byte) (0xe0 | ((c >> 12)));
                bytes[dp++] = (byte) (0x80 | ((c >> 6) & 0x3f));
                bytes[dp++] = (byte) (0x80 | (c & 0x3f));
            }
        }
        return dp;
    }
}

