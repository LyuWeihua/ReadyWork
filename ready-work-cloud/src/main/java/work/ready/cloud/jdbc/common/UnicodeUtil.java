package work.ready.cloud.jdbc.common;

public final class UnicodeUtil {

    public static final BytesRef BIG_TERM = new BytesRef(
            new byte[] {-1,-1,-1,-1,-1,-1,-1,-1,-1,-1}
    ); 

    private UnicodeUtil() {} 

    public static final int UNI_SUR_HIGH_START = 0xD800;
    public static final int UNI_SUR_HIGH_END = 0xDBFF;
    public static final int UNI_SUR_LOW_START = 0xDC00;
    public static final int UNI_SUR_LOW_END = 0xDFFF;
    public static final int UNI_REPLACEMENT_CHAR = 0xFFFD;

    private static final long UNI_MAX_BMP = 0x0000FFFF;

    private static final long HALF_SHIFT = 10;
    private static final long HALF_MASK = 0x3FFL;

    private static final int SURROGATE_OFFSET =
            Character.MIN_SUPPLEMENTARY_CODE_POINT -
                    (UNI_SUR_HIGH_START << HALF_SHIFT) - UNI_SUR_LOW_START;

    public static final int MAX_UTF8_BYTES_PER_CHAR = 3;

    public static int UTF16toUTF8(final char[] source, final int offset, final int length, byte[] out) {

        int upto = 0;
        int i = offset;
        final int end = offset + length;

        while(i < end) {

            final int code = (int) source[i++];

            if (code < 0x80)
                out[upto++] = (byte) code;
            else if (code < 0x800) {
                out[upto++] = (byte) (0xC0 | (code >> 6));
                out[upto++] = (byte)(0x80 | (code & 0x3F));
            } else if (code < 0xD800 || code > 0xDFFF) {
                out[upto++] = (byte)(0xE0 | (code >> 12));
                out[upto++] = (byte)(0x80 | ((code >> 6) & 0x3F));
                out[upto++] = (byte)(0x80 | (code & 0x3F));
            } else {

                if (code < 0xDC00 && i < end) {
                    int utf32 = (int) source[i];
                    
                    if (utf32 >= 0xDC00 && utf32 <= 0xDFFF) {
                        utf32 = (code << 10) + utf32 + SURROGATE_OFFSET;
                        i++;
                        out[upto++] = (byte)(0xF0 | (utf32 >> 18));
                        out[upto++] = (byte)(0x80 | ((utf32 >> 12) & 0x3F));
                        out[upto++] = (byte)(0x80 | ((utf32 >> 6) & 0x3F));
                        out[upto++] = (byte)(0x80 | (utf32 & 0x3F));
                        continue;
                    }
                }

                out[upto++] = (byte) 0xEF;
                out[upto++] = (byte) 0xBF;
                out[upto++] = (byte) 0xBD;
            }
        }
        
        return upto;
    }

    public static int UTF16toUTF8(final CharSequence s, final int offset, final int length, byte[] out) {
        return UTF16toUTF8(s, offset, length, out, 0);
    }

    public static int UTF16toUTF8(final CharSequence s, final int offset, final int length, byte[] out, int outOffset) {
        final int end = offset + length;

        int upto = outOffset;
        for(int i=offset;i<end;i++) {
            final int code = (int) s.charAt(i);

            if (code < 0x80)
                out[upto++] = (byte) code;
            else if (code < 0x800) {
                out[upto++] = (byte) (0xC0 | (code >> 6));
                out[upto++] = (byte)(0x80 | (code & 0x3F));
            } else if (code < 0xD800 || code > 0xDFFF) {
                out[upto++] = (byte)(0xE0 | (code >> 12));
                out[upto++] = (byte)(0x80 | ((code >> 6) & 0x3F));
                out[upto++] = (byte)(0x80 | (code & 0x3F));
            } else {

                if (code < 0xDC00 && (i < end-1)) {
                    int utf32 = (int) s.charAt(i+1);
                    
                    if (utf32 >= 0xDC00 && utf32 <= 0xDFFF) {
                        utf32 = (code << 10) + utf32 + SURROGATE_OFFSET;
                        i++;
                        out[upto++] = (byte)(0xF0 | (utf32 >> 18));
                        out[upto++] = (byte)(0x80 | ((utf32 >> 12) & 0x3F));
                        out[upto++] = (byte)(0x80 | ((utf32 >> 6) & 0x3F));
                        out[upto++] = (byte)(0x80 | (utf32 & 0x3F));
                        continue;
                    }
                }

                out[upto++] = (byte) 0xEF;
                out[upto++] = (byte) 0xBF;
                out[upto++] = (byte) 0xBD;
            }
        }
        
        return upto;
    }

    public static int calcUTF16toUTF8Length(final CharSequence s, final int offset, final int len) {
        final int end = offset + len;

        int res = 0;
        for (int i = offset; i < end; i++) {
            final int code = (int) s.charAt(i);

            if (code < 0x80)
                res++;
            else if (code < 0x800) {
                res += 2;
            } else if (code < 0xD800 || code > 0xDFFF) {
                res += 3;
            } else {

                if (code < 0xDC00 && (i < end - 1)) {
                    int utf32 = (int) s.charAt(i + 1);
                    
                    if (utf32 >= 0xDC00 && utf32 <= 0xDFFF) {
                        i++;
                        res += 4;
                        continue;
                    }
                }
                res += 3;
            }
        }

        return res;
    }

    public static boolean validUTF16String(CharSequence s) {
        final int size = s.length();
        for(int i=0;i<size;i++) {
            char ch = s.charAt(i);
            if (ch >= UNI_SUR_HIGH_START && ch <= UNI_SUR_HIGH_END) {
                if (i < size-1) {
                    i++;
                    char nextCH = s.charAt(i);
                    if (nextCH >= UNI_SUR_LOW_START && nextCH <= UNI_SUR_LOW_END) {
                        
                    } else
                        
                        return false;
                } else
                    
                    return false;
            } else if (ch >= UNI_SUR_LOW_START && ch <= UNI_SUR_LOW_END)
                
                return false;
        }

        return true;
    }

    public static boolean validUTF16String(char[] s, int size) {
        for(int i=0;i<size;i++) {
            char ch = s[i];
            if (ch >= UNI_SUR_HIGH_START && ch <= UNI_SUR_HIGH_END) {
                if (i < size-1) {
                    i++;
                    char nextCH = s[i];
                    if (nextCH >= UNI_SUR_LOW_START && nextCH <= UNI_SUR_LOW_END) {
                        
                    } else
                        return false;
                } else
                    return false;
            } else if (ch >= UNI_SUR_LOW_START && ch <= UNI_SUR_LOW_END)
                
                return false;
        }

        return true;
    }

    static final int [] utf8CodeLength;
    static {
        final int v = Integer.MIN_VALUE;
        utf8CodeLength = new int [] {
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                v, v, v, v, v, v, v, v, v, v, v, v, v, v, v, v,
                v, v, v, v, v, v, v, v, v, v, v, v, v, v, v, v,
                v, v, v, v, v, v, v, v, v, v, v, v, v, v, v, v,
                v, v, v, v, v, v, v, v, v, v, v, v, v, v, v, v,
                2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
                2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
                3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
                4, 4, 4, 4, 4, 4, 4, 4 
        };
    }

    public static int codePointCount(BytesRef utf8) {
        int pos = utf8.offset;
        final int limit = pos + utf8.length;
        final byte[] bytes = utf8.bytes;

        int codePointCount = 0;
        for (; pos < limit; codePointCount++) {
            int v = bytes[pos] & 0xFF;
            if (v <    0x80) { pos += 1; continue; }
            if (v >=   0xc0) {
                if (v <  0xe0) { pos += 2; continue; }
                if (v <  0xf0) { pos += 3; continue; }
                if (v <  0xf8) { pos += 4; continue; }
                
            }

            throw new IllegalArgumentException();
        }

        if (pos > limit) throw new IllegalArgumentException();

        return codePointCount;
    }

    public static int UTF8toUTF32(final BytesRef utf8, final int[] ints) {
        
        int utf32Count = 0;
        int utf8Upto = utf8.offset;
        final byte[] bytes = utf8.bytes;
        final int utf8Limit = utf8.offset + utf8.length;
        while(utf8Upto < utf8Limit) {
            final int numBytes = utf8CodeLength[bytes[utf8Upto] & 0xFF];
            int v = 0;
            switch(numBytes) {
                case 1:
                    ints[utf32Count++] = bytes[utf8Upto++];
                    continue;
                case 2:
                    
                    v = bytes[utf8Upto++] & 31;
                    break;
                case 3:
                    
                    v = bytes[utf8Upto++] & 15;
                    break;
                case 4:
                    
                    v = bytes[utf8Upto++] & 7;
                    break;
                default :
                    throw new IllegalArgumentException("invalid utf8");
            }

            final int limit = utf8Upto + numBytes-1;
            while(utf8Upto < limit) {
                v = v << 6 | bytes[utf8Upto++]&63;
            }
            ints[utf32Count++] = v;
        }

        return utf32Count;
    }

    private static final int LEAD_SURROGATE_SHIFT_ = 10;
    
    private static final int TRAIL_SURROGATE_MASK_ = 0x3FF;
    
    private static final int TRAIL_SURROGATE_MIN_VALUE = 0xDC00;
    
    private static final int LEAD_SURROGATE_MIN_VALUE = 0xD800;
    
    private static final int SUPPLEMENTARY_MIN_VALUE = 0x10000;
    
    private static final int LEAD_SURROGATE_OFFSET_ = LEAD_SURROGATE_MIN_VALUE
            - (SUPPLEMENTARY_MIN_VALUE >> LEAD_SURROGATE_SHIFT_);

    public static String newString(int[] codePoints, int offset, int count) {
        if (count < 0) {
            throw new IllegalArgumentException();
        }
        char[] chars = new char[count];
        int w = 0;
        for (int r = offset, e = offset + count; r < e; ++r) {
            int cp = codePoints[r];
            if (cp < 0 || cp > 0x10ffff) {
                throw new IllegalArgumentException();
            }
            while (true) {
                try {
                    if (cp < 0x010000) {
                        chars[w] = (char) cp;
                        w++;
                    } else {
                        chars[w] = (char) (LEAD_SURROGATE_OFFSET_ + (cp >> LEAD_SURROGATE_SHIFT_));
                        chars[w + 1] = (char) (TRAIL_SURROGATE_MIN_VALUE + (cp & TRAIL_SURROGATE_MASK_));
                        w += 2;
                    }
                    break;
                } catch (IndexOutOfBoundsException ex) {
                    int newlen = (int) (Math.ceil((double) codePoints.length * (w + 2)
                            / (r - offset + 1)));
                    char[] temp = new char[newlen];
                    System.arraycopy(chars, 0, temp, 0, w);
                    chars = temp;
                }
            }
        }
        return new String(chars, 0, w);
    }

    public static String toHexString(String s) {
        StringBuilder sb = new StringBuilder();
        for(int i=0;i<s.length();i++) {
            char ch = s.charAt(i);
            if (i > 0) {
                sb.append(' ');
            }
            if (ch < 128) {
                sb.append(ch);
            } else {
                if (ch >= UNI_SUR_HIGH_START && ch <= UNI_SUR_HIGH_END) {
                    sb.append("H:");
                } else if (ch >= UNI_SUR_LOW_START && ch <= UNI_SUR_LOW_END) {
                    sb.append("L:");
                } else if (ch > UNI_SUR_LOW_END) {
                    if (ch == 0xffff) {
                        sb.append("F:");
                    } else {
                        sb.append("E:");
                    }
                }

                sb.append("0x").append(Integer.toHexString(ch));
            }
        }
        return sb.toString();
    }

    public static int UTF8toUTF16(byte[] utf8, int offset, int length, char[] out) {
        int out_offset = 0;
        final int limit = offset + length;
        while (offset < limit) {
            int b = utf8[offset++]&0xff;
            if (b < 0xc0) {
                assert b < 0x80;
                out[out_offset++] = (char)b;
            } else if (b < 0xe0) {
                out[out_offset++] = (char)(((b&0x1f)<<6) + (utf8[offset++]&0x3f));
            } else if (b < 0xf0) {
                out[out_offset++] = (char)(((b&0xf)<<12) + ((utf8[offset]&0x3f)<<6) + (utf8[offset+1]&0x3f));
                offset += 2;
            } else {
                assert b < 0xf8: "b = 0x" + Integer.toHexString(b);
                int ch = ((b&0x7)<<18) + ((utf8[offset]&0x3f)<<12) + ((utf8[offset+1]&0x3f)<<6) + (utf8[offset+2]&0x3f);
                offset += 3;
                if (ch < UNI_MAX_BMP) {
                    out[out_offset++] = (char)ch;
                } else {
                    int chHalf = ch - 0x0010000;
                    out[out_offset++] = (char) ((chHalf >> 10) + 0xD800);
                    out[out_offset++] = (char) ((chHalf & HALF_MASK) + 0xDC00);
                }
            }
        }
        return out_offset;
    }

    public static int maxUTF8Length(int utf16Length) {
        return Math.multiplyExact(utf16Length, MAX_UTF8_BYTES_PER_CHAR);
    }

    public static int UTF8toUTF16(BytesRef bytesRef, char[] chars) {
        return UTF8toUTF16(bytesRef.bytes, bytesRef.offset, bytesRef.length, chars);
    }

}
