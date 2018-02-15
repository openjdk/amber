/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.lang;

import java.util.Arrays;
import java.util.Formatter;
import java.util.Locale;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import jdk.internal.HotSpotIntrinsicCandidate;

import static java.lang.String.LATIN1;
import static java.lang.String.UTF16;
import static java.lang.String.checkOffset;

final class StringLatin1 {

    public static char charAt(byte[] value, int index) {
        if (index < 0 || index >= value.length) {
            throw new StringIndexOutOfBoundsException(index);
        }
        return (char)(value[index] & 0xff);
    }

    public static boolean canEncode(int cp) {
        return cp >>> 8 == 0;
    }

    public static int length(byte[] value) {
        return value.length;
    }

    public static int codePointAt(byte[] value, int index, int end) {
        return value[index] & 0xff;
    }

    public static int codePointBefore(byte[] value, int index) {
        return value[index - 1] & 0xff;
    }

    public static int codePointCount(byte[] value, int beginIndex, int endIndex) {
        return endIndex - beginIndex;
    }

    public static char[] toChars(byte[] value) {
        char[] dst = new char[value.length];
        inflate(value, 0, dst, 0, value.length);
        return dst;
    }

    public static byte[] inflate(byte[] value, int off, int len) {
        byte[] ret = StringUTF16.newBytesFor(len);
        inflate(value, off, ret, 0, len);
        return ret;
    }

    public static void getChars(byte[] value, int srcBegin, int srcEnd, char dst[], int dstBegin) {
        inflate(value, srcBegin, dst, dstBegin, srcEnd - srcBegin);
    }

    public static void getBytes(byte[] value, int srcBegin, int srcEnd, byte dst[], int dstBegin) {
        System.arraycopy(value, srcBegin, dst, dstBegin, srcEnd - srcBegin);
    }

    @HotSpotIntrinsicCandidate
    public static boolean equals(byte[] value, byte[] other) {
        if (value.length == other.length) {
            for (int i = 0; i < value.length; i++) {
                if (value[i] != other[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @HotSpotIntrinsicCandidate
    public static int compareTo(byte[] value, byte[] other) {
        int len1 = value.length;
        int len2 = other.length;
        int lim = Math.min(len1, len2);
        for (int k = 0; k < lim; k++) {
            if (value[k] != other[k]) {
                return getChar(value, k) - getChar(other, k);
            }
        }
        return len1 - len2;
    }

    @HotSpotIntrinsicCandidate
    public static int compareToUTF16(byte[] value, byte[] other) {
        int len1 = length(value);
        int len2 = StringUTF16.length(other);
        int lim = Math.min(len1, len2);
        for (int k = 0; k < lim; k++) {
            char c1 = getChar(value, k);
            char c2 = StringUTF16.getChar(other, k);
            if (c1 != c2) {
                return c1 - c2;
            }
        }
        return len1 - len2;
    }

    public static int compareToCI(byte[] value, byte[] other) {
        int len1 = value.length;
        int len2 = other.length;
        int lim = Math.min(len1, len2);
        for (int k = 0; k < lim; k++) {
            if (value[k] != other[k]) {
                char c1 = (char) CharacterDataLatin1.instance.toUpperCase(getChar(value, k));
                char c2 = (char) CharacterDataLatin1.instance.toUpperCase(getChar(other, k));
                if (c1 != c2) {
                    c1 = Character.toLowerCase(c1);
                    c2 = Character.toLowerCase(c2);
                    if (c1 != c2) {
                        return c1 - c2;
                    }
                }
            }
        }
        return len1 - len2;
    }

    public static int compareToCI_UTF16(byte[] value, byte[] other) {
        int len1 = length(value);
        int len2 = StringUTF16.length(other);
        int lim = Math.min(len1, len2);
        for (int k = 0; k < lim; k++) {
            char c1 = getChar(value, k);
            char c2 = StringUTF16.getChar(other, k);
            if (c1 != c2) {
                c1 = Character.toUpperCase(c1);
                c2 = Character.toUpperCase(c2);
                if (c1 != c2) {
                    c1 = Character.toLowerCase(c1);
                    c2 = Character.toLowerCase(c2);
                    if (c1 != c2) {
                        return c1 - c2;
                    }
                }
            }
        }
        return len1 - len2;
    }

    public static int hashCode(byte[] value) {
        int h = 0;
        for (byte v : value) {
            h = 31 * h + (v & 0xff);
        }
        return h;
    }

    public static int indexOf(byte[] value, int ch, int fromIndex) {
        if (!canEncode(ch)) {
            return -1;
        }
        int max = value.length;
        if (fromIndex < 0) {
            fromIndex = 0;
        } else if (fromIndex >= max) {
            // Note: fromIndex might be near -1>>>1.
            return -1;
        }
        byte c = (byte)ch;
        for (int i = fromIndex; i < max; i++) {
            if (value[i] == c) {
               return i;
            }
        }
        return -1;
    }

    @HotSpotIntrinsicCandidate
    public static int indexOf(byte[] value, byte[] str) {
        if (str.length == 0) {
            return 0;
        }
        if (value.length == 0) {
            return -1;
        }
        return indexOf(value, value.length, str, str.length, 0);
    }

    @HotSpotIntrinsicCandidate
    public static int indexOf(byte[] value, int valueCount, byte[] str, int strCount, int fromIndex) {
        byte first = str[0];
        int max = (valueCount - strCount);
        for (int i = fromIndex; i <= max; i++) {
            // Look for first character.
            if (value[i] != first) {
                while (++i <= max && value[i] != first);
            }
            // Found first character, now look at the rest of value
            if (i <= max) {
                int j = i + 1;
                int end = j + strCount - 1;
                for (int k = 1; j < end && value[j] == str[k]; j++, k++);
                if (j == end) {
                    // Found whole string.
                    return i;
                }
            }
        }
        return -1;
    }

    public static int lastIndexOf(byte[] src, int srcCount,
                                  byte[] tgt, int tgtCount, int fromIndex) {
        int min = tgtCount - 1;
        int i = min + fromIndex;
        int strLastIndex = tgtCount - 1;
        char strLastChar = (char)(tgt[strLastIndex] & 0xff);

  startSearchForLastChar:
        while (true) {
            while (i >= min && (src[i] & 0xff) != strLastChar) {
                i--;
            }
            if (i < min) {
                return -1;
            }
            int j = i - 1;
            int start = j - strLastIndex;
            int k = strLastIndex - 1;
            while (j > start) {
                if ((src[j--] & 0xff) != (tgt[k--] & 0xff)) {
                    i--;
                    continue startSearchForLastChar;
                }
            }
            return start + 1;
        }
    }

    public static int lastIndexOf(final byte[] value, int ch, int fromIndex) {
        if (!canEncode(ch)) {
            return -1;
        }
        int off  = Math.min(fromIndex, value.length - 1);
        for (; off >= 0; off--) {
            if (value[off] == (byte)ch) {
                return off;
            }
        }
        return -1;
    }

    public static String replace(byte[] value, char oldChar, char newChar) {
        if (canEncode(oldChar)) {
            int len = value.length;
            int i = -1;
            while (++i < len) {
                if (value[i] == (byte)oldChar) {
                    break;
                }
            }
            if (i < len) {
                if (canEncode(newChar)) {
                    byte buf[] = new byte[len];
                    for (int j = 0; j < i; j++) {    // TBD arraycopy?
                        buf[j] = value[j];
                    }
                    while (i < len) {
                        byte c = value[i];
                        buf[i] = (c == (byte)oldChar) ? (byte)newChar : c;
                        i++;
                    }
                    return new String(buf, LATIN1);
                } else {
                    byte[] buf = StringUTF16.newBytesFor(len);
                    // inflate from latin1 to UTF16
                    inflate(value, 0, buf, 0, i);
                    while (i < len) {
                        char c = (char)(value[i] & 0xff);
                        StringUTF16.putChar(buf, i, (c == oldChar) ? newChar : c);
                        i++;
                    }
                    return new String(buf, UTF16);
                }
            }
        }
        return null; // for string to return this;
    }

    // case insensitive
    public static boolean regionMatchesCI(byte[] value, int toffset,
                                          byte[] other, int ooffset, int len) {
        int last = toffset + len;
        while (toffset < last) {
            char c1 = (char)(value[toffset++] & 0xff);
            char c2 = (char)(other[ooffset++] & 0xff);
            if (c1 == c2) {
                continue;
            }
            char u1 = Character.toUpperCase(c1);
            char u2 = Character.toUpperCase(c2);
            if (u1 == u2) {
                continue;
            }
            if (Character.toLowerCase(u1) == Character.toLowerCase(u2)) {
                continue;
            }
            return false;
        }
        return true;
    }

    public static boolean regionMatchesCI_UTF16(byte[] value, int toffset,
                                                byte[] other, int ooffset, int len) {
        int last = toffset + len;
        while (toffset < last) {
            char c1 = (char)(value[toffset++] & 0xff);
            char c2 = StringUTF16.getChar(other, ooffset++);
            if (c1 == c2) {
                continue;
            }
            char u1 = Character.toUpperCase(c1);
            char u2 = Character.toUpperCase(c2);
            if (u1 == u2) {
                continue;
            }
            if (Character.toLowerCase(u1) == Character.toLowerCase(u2)) {
                continue;
            }
            return false;
        }
        return true;
    }

    public static String toLowerCase(String str, byte[] value, Locale locale) {
        if (locale == null) {
            throw new NullPointerException();
        }
        int first;
        final int len = value.length;
        // Now check if there are any characters that need to be changed, or are surrogate
        for (first = 0 ; first < len; first++) {
            int cp = value[first] & 0xff;
            if (cp != Character.toLowerCase(cp)) {  // no need to check Character.ERROR
                break;
            }
        }
        if (first == len)
            return str;
        String lang = locale.getLanguage();
        if (lang == "tr" || lang == "az" || lang == "lt") {
            return toLowerCaseEx(str, value, first, locale, true);
        }
        byte[] result = new byte[len];
        System.arraycopy(value, 0, result, 0, first);  // Just copy the first few
                                                       // lowerCase characters.
        for (int i = first; i < len; i++) {
            int cp = value[i] & 0xff;
            cp = Character.toLowerCase(cp);
            if (!canEncode(cp)) {                      // not a latin1 character
                return toLowerCaseEx(str, value, first, locale, false);
            }
            result[i] = (byte)cp;
        }
        return new String(result, LATIN1);
    }

    private static String toLowerCaseEx(String str, byte[] value,
                                        int first, Locale locale, boolean localeDependent)
    {
        byte[] result = StringUTF16.newBytesFor(value.length);
        int resultOffset = 0;
        for (int i = 0; i < first; i++) {
            StringUTF16.putChar(result, resultOffset++, value[i] & 0xff);
        }
        for (int i = first; i < value.length; i++) {
            int srcChar = value[i] & 0xff;
            int lowerChar;
            char[] lowerCharArray;
            if (localeDependent) {
                lowerChar = ConditionalSpecialCasing.toLowerCaseEx(str, i, locale);
            } else {
                lowerChar = Character.toLowerCase(srcChar);
            }
            if (Character.isBmpCodePoint(lowerChar)) {    // Character.ERROR is not a bmp
                StringUTF16.putChar(result, resultOffset++, lowerChar);
            } else {
                if (lowerChar == Character.ERROR) {
                    lowerCharArray = ConditionalSpecialCasing.toLowerCaseCharArray(str, i, locale);
                } else {
                    lowerCharArray = Character.toChars(lowerChar);
                }
                /* Grow result if needed */
                int mapLen = lowerCharArray.length;
                if (mapLen > 1) {
                    byte[] result2 = StringUTF16.newBytesFor((result.length >> 1) + mapLen - 1);
                    System.arraycopy(result, 0, result2, 0, resultOffset << 1);
                    result = result2;
                }
                for (int x = 0; x < mapLen; ++x) {
                    StringUTF16.putChar(result, resultOffset++, lowerCharArray[x]);
                }
            }
        }
        return StringUTF16.newString(result, 0, resultOffset);
    }

    public static String toUpperCase(String str, byte[] value, Locale locale) {
        if (locale == null) {
            throw new NullPointerException();
        }
        int first;
        final int len = value.length;

        // Now check if there are any characters that need to be changed, or are surrogate
        for (first = 0 ; first < len; first++ ) {
            int cp = value[first] & 0xff;
            if (cp != Character.toUpperCaseEx(cp)) {   // no need to check Character.ERROR
                break;
            }
        }
        if (first == len) {
            return str;
        }
        String lang = locale.getLanguage();
        if (lang == "tr" || lang == "az" || lang == "lt") {
            return toUpperCaseEx(str, value, first, locale, true);
        }
        byte[] result = new byte[len];
        System.arraycopy(value, 0, result, 0, first);  // Just copy the first few
                                                       // upperCase characters.
        for (int i = first; i < len; i++) {
            int cp = value[i] & 0xff;
            cp = Character.toUpperCaseEx(cp);
            if (!canEncode(cp)) {                      // not a latin1 character
                return toUpperCaseEx(str, value, first, locale, false);
            }
            result[i] = (byte)cp;
        }
        return new String(result, LATIN1);
    }

    private static String toUpperCaseEx(String str, byte[] value,
                                        int first, Locale locale, boolean localeDependent)
    {
        byte[] result = StringUTF16.newBytesFor(value.length);
        int resultOffset = 0;
        for (int i = 0; i < first; i++) {
            StringUTF16.putChar(result, resultOffset++, value[i] & 0xff);
        }
        for (int i = first; i < value.length; i++) {
            int srcChar = value[i] & 0xff;
            int upperChar;
            char[] upperCharArray;
            if (localeDependent) {
                upperChar = ConditionalSpecialCasing.toUpperCaseEx(str, i, locale);
            } else {
                upperChar = Character.toUpperCaseEx(srcChar);
            }
            if (Character.isBmpCodePoint(upperChar)) {
                StringUTF16.putChar(result, resultOffset++, upperChar);
            } else {
                if (upperChar == Character.ERROR) {
                    if (localeDependent) {
                        upperCharArray =
                            ConditionalSpecialCasing.toUpperCaseCharArray(str, i, locale);
                    } else {
                        upperCharArray = Character.toUpperCaseCharArray(srcChar);
                    }
                } else {
                    upperCharArray = Character.toChars(upperChar);
                }
                /* Grow result if needed */
                int mapLen = upperCharArray.length;
                if (mapLen > 1) {
                    byte[] result2 = StringUTF16.newBytesFor((result.length >> 1) + mapLen - 1);
                    System.arraycopy(result, 0, result2, 0, resultOffset << 1);
                    result = result2;
                }
                for (int x = 0; x < mapLen; ++x) {
                    StringUTF16.putChar(result, resultOffset++, upperCharArray[x]);
                }
            }
        }
        return StringUTF16.newString(result, 0, resultOffset);
    }

    public static int skipLeadingSpaces(byte[] value) {
        int left = 0;
        while ((left < value.length) && ((value[left] & 0xff) <= ' ')) {
            left++;
        }
        return left;
    }

    public static int skipTrailingSpaces(byte[] value) {
        int right = value.length;
        while ((0 < right) && ((value[right - 1] & 0xff) <= ' ')) {
            right--;
        }
        return right;
    }

    public static String trim(byte[] value) {
        int left = skipLeadingSpaces(value);
        if (left == value.length) {
            return "";
        }
        int right = skipTrailingSpaces(value);
        return ((left > 0) || (right < value.length)) ?
                newString(value, left, right - left) : null;
    }

    public static String trimLeft(byte[] value) {
        int left = skipLeadingSpaces(value);
        if (left == value.length) {
            return "";
        }
        return (left != 0) ?
                newString(value, left, value.length - left) : null;
    }

    public static String trimRight(byte[] value) {
        int right = skipTrailingSpaces(value);
        if (right == 0) {
            return "";
        }
        return (right != value.length) ?
                newString(value, 0, right) : null;
    }

    public static void putChar(byte[] val, int index, int c) {
        //assert (canEncode(c));
        val[index] = (byte)(c);
    }

    public static char getChar(byte[] val, int index) {
        return (char)(val[index] & 0xff);
    }

    public static byte[] toBytes(int[] val, int off, int len) {
        byte[] ret = new byte[len];
        for (int i = 0; i < len; i++) {
            int cp = val[off++];
            if (!canEncode(cp)) {
                return null;
            }
            ret[i] = (byte)cp;
        }
        return ret;
    }

    public static byte[] toBytes(char c) {
        return new byte[] { (byte)c };
    }

    public static String newString(byte[] val, int index, int len) {
        return new String(Arrays.copyOfRange(val, index, index + len),
                          LATIN1);
    }

    public static void fillNull(byte[] val, int index, int end) {
        Arrays.fill(val, index, end, (byte)0);
    }

    // inflatedCopy byte[] -> char[]
    @HotSpotIntrinsicCandidate
    public static void inflate(byte[] src, int srcOff, char[] dst, int dstOff, int len) {
        for (int i = 0; i < len; i++) {
            dst[dstOff++] = (char)(src[srcOff++] & 0xff);
        }
    }

    // inflatedCopy byte[] -> byte[]
    @HotSpotIntrinsicCandidate
    public static void inflate(byte[] src, int srcOff, byte[] dst, int dstOff, int len) {
        StringUTF16.inflate(src, srcOff, dst, dstOff, len);
    }

    static String unescape(byte[] value, boolean backslash, boolean unicode)
            throws MalformedEscapeException {
        int length = value.length;
        byte[] chars = new byte[length];

        int from = 0;
        int to = 0;

        while (from < length) {
            char ch = (char)value[from++];
            if (ch == '\\' && from < length) {
                ch = (char)value[from++];
                if (unicode && ch == 'u') {
                    while (from < length && (char)value[from] == 'u') {
                        from++;
                    }
                    if (length <= from + 3) {
                        throw new MalformedEscapeException();
                    }
                    int code = (Character.digit(value[from + 0], 16) << 12) |
                               (Character.digit(value[from + 1], 16) <<  8) |
                               (Character.digit(value[from + 2], 16) <<  4) |
                                Character.digit(value[from + 3], 16);
                    if (code < 0) {
                        throw new MalformedEscapeException();
                    }
                    if (canEncode(code)) {
                        ch = (char)code;
                        from += 4;
                    } else {
                        return StringUTF16.unescape(toChars(value), backslash, unicode);
                    }
                } else if (backslash) {
                    switch (ch) {
                    case 'b':
                        ch = '\b';
                        break;
                    case 'f':
                        ch = '\f';
                        break;
                    case 'n':
                        ch = '\n';
                        break;
                    case 'r':
                        ch = '\r';
                        break;
                    case 't':
                        ch = '\t';
                        break;
                    case 'u':
                        chars[to++] = (byte)'\\';
                        break;
                    case '0': case '1': case '2': case '3':
                    case '4': case '5': case '6': case '7':
                        int code = ch - '0';
                        for (int i = 0; i < 2 && from < length; i++) {
                            int digit = Character.digit(value[from], 8);
                            if (digit < 0) {
                                break;
                            }
                            from++;
                            code = code << 3 | digit;
                        }
                        if (0377 < code) {
                            throw new MalformedEscapeException();
                        }
                        ch = (char)code;
                        break;
                    default:
                        throw new MalformedEscapeException();
                    }
                } else {
                    chars[to++] = (byte)'\\';
                }
            }

            chars[to++] = (byte)ch;
        }

        return new String(Arrays.copyOfRange(chars, 0, to), LATIN1);
    }

    static String escape(byte[] value, boolean backslash, boolean unicode) {
        if (!backslash) {
            return null;
        }
        int length = value.length;
        StringBuilder sb = new StringBuilder(value.length);
        Formatter fmt = null;
        for (int i = 0; i < length; i++) {
            char ch = (char)value[i];
            switch (ch) {
            case '\b':
                sb.append('\\');
                sb.append('b');
                break;
            case '\f':
                sb.append('\\');
                sb.append('f');
                break;
            case '\n':
                sb.append('\\');
                sb.append('n');
                break;
            case '\r':
                sb.append('\\');
                sb.append('r');
                break;
            case '\t':
                sb.append('\\');
                sb.append('t');
                break;
            case '\\':
                sb.append('\\');
                sb.append('\\');
                break;
            case '\"':
                sb.append('\\');
                sb.append('\"');
                break;
            case '\'':
                sb.append('\\');
                sb.append('\'');
                break;
            default:
                if (' ' <= ch && ch <= '~') {
                    sb.append(ch);
                } else {
                    if (fmt == null) {
                        fmt = new Formatter(sb);
                    }
                    sb.append('\\');
                    sb.append('u');
                    fmt.format("%04x", (int)ch);
                }
                break;
            }
        }

        return sb.length() == length ? null : sb.toString();
    }

    static class CharsSpliterator implements Spliterator.OfInt {
        private final byte[] array;
        private int index;        // current index, modified on advance/split
        private final int fence;  // one past last index
        private final int cs;

        CharsSpliterator(byte[] array, int acs) {
            this(array, 0, array.length, acs);
        }

        CharsSpliterator(byte[] array, int origin, int fence, int acs) {
            this.array = array;
            this.index = origin;
            this.fence = fence;
            this.cs = acs | Spliterator.ORDERED | Spliterator.SIZED
                      | Spliterator.SUBSIZED;
        }

        @Override
        public OfInt trySplit() {
            int lo = index, mid = (lo + fence) >>> 1;
            return (lo >= mid)
                   ? null
                   : new CharsSpliterator(array, lo, index = mid, cs);
        }

        @Override
        public void forEachRemaining(IntConsumer action) {
            byte[] a; int i, hi; // hoist accesses and checks from loop
            if (action == null)
                throw new NullPointerException();
            if ((a = array).length >= (hi = fence) &&
                (i = index) >= 0 && i < (index = hi)) {
                do { action.accept(a[i] & 0xff); } while (++i < hi);
            }
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            if (action == null)
                throw new NullPointerException();
            if (index >= 0 && index < fence) {
                action.accept(array[index++] & 0xff);
                return true;
            }
            return false;
        }

        @Override
        public long estimateSize() { return (long)(fence - index); }

        @Override
        public int characteristics() {
            return cs;
        }
    }
}
