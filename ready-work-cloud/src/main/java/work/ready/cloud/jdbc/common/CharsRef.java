package work.ready.cloud.jdbc.common;

import work.ready.core.tools.CollectionUtil;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

public final class CharsRef implements Comparable<CharsRef>, CharSequence, Cloneable {
    
    public static final char[] EMPTY_CHARS = new char[0];
    
    public char[] chars;
    
    public int offset;
    
    public int length;

    public CharsRef() {
        this(EMPTY_CHARS, 0, 0);
    }

    public CharsRef(int capacity) {
        chars = new char[capacity];
    }

    public CharsRef(char[] chars, int offset, int length) {
        this.chars = chars;
        this.offset = offset;
        this.length = length;
        assert isValid();
    }

    public CharsRef(String string) {
        this.chars = string.toCharArray();
        this.offset = 0;
        this.length = chars.length;
    }

    @Override
    public CharsRef clone() {
        return new CharsRef(chars, offset, length);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 0;
        final int end = offset + length;
        for (int i = offset; i < end; i++) {
            result = prime * result + chars[i];
        }
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other instanceof CharsRef) {
            return this.charsEquals((CharsRef) other);
        }
        return false;
    }

    public boolean charsEquals(CharsRef other) {
        return Arrays.equals(this.chars, this.offset, this.offset + this.length,
                other.chars, other.offset, other.offset + other.length);
    }

    @Override
    public int compareTo(CharsRef other) {
        return Arrays.compare(this.chars, this.offset, this.offset + this.length,
                other.chars, other.offset, other.offset + other.length);
    }

    @Override
    public String toString() {
        return new String(chars, offset, length);
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public char charAt(int index) {
        
        Objects.checkIndex(index, length);
        return chars[offset + index];
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        
        Objects.checkFromToIndex(start, end, length);
        return new CharsRef(chars, offset + start, end - start);
    }

    @Deprecated
    private final static Comparator<CharsRef> utf16SortedAsUTF8SortOrder = new UTF16SortedAsUTF8Comparator();

    @Deprecated
    public static Comparator<CharsRef> getUTF16SortedAsUTF8Comparator() {
        return utf16SortedAsUTF8SortOrder;
    }

    @Deprecated
    private static class UTF16SortedAsUTF8Comparator implements Comparator<CharsRef> {
        
        private UTF16SortedAsUTF8Comparator() {};

        @Override
        public int compare(CharsRef a, CharsRef b) {
            int aEnd = a.offset + a.length;
            int bEnd = b.offset + b.length;
            int i = Arrays.mismatch(a.chars, a.offset, aEnd,
                    b.chars, b.offset, bEnd);

            if (i >= 0 && i < Math.min(a.length, b.length)) {

                char aChar = a.chars[a.offset + i];
                char bChar = b.chars[b.offset + i];
                
                if (aChar >= 0xd800 && bChar >= 0xd800) {
                    if (aChar >= 0xe000) {
                        aChar -= 0x800;
                    } else {
                        aChar += 0x2000;
                    }

                    if (bChar >= 0xe000) {
                        bChar -= 0x800;
                    } else {
                        bChar += 0x2000;
                    }
                }

                return (int)aChar - (int)bChar; 
            }

            return a.length - b.length;
        }
    }

    public static CharsRef deepCopyOf(CharsRef other) {
        return new CharsRef(CollectionUtil.copyOfSubArray(other.chars, other.offset, other.offset + other.length), 0, other.length);
    }

    public boolean isValid() {
        if (chars == null) {
            throw new IllegalStateException("chars is null");
        }
        if (length < 0) {
            throw new IllegalStateException("length is negative: " + length);
        }
        if (length > chars.length) {
            throw new IllegalStateException("length is out of bounds: " + length + ",chars.length=" + chars.length);
        }
        if (offset < 0) {
            throw new IllegalStateException("offset is negative: " + offset);
        }
        if (offset > chars.length) {
            throw new IllegalStateException("offset out of bounds: " + offset + ",chars.length=" + chars.length);
        }
        if (offset + length < 0) {
            throw new IllegalStateException("offset+length is negative: offset=" + offset + ",length=" + length);
        }
        if (offset + length > chars.length) {
            throw new IllegalStateException("offset+length out of bounds: offset=" + offset + ",length=" + length + ",chars.length=" + chars.length);
        }
        return true;
    }
}
