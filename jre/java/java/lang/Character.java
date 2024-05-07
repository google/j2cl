/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package java.lang;

import static javaemul.internal.InternalPreconditions.checkArgument;

import java.io.Serializable;
import javaemul.internal.NativeRegExp;
import javaemul.internal.StringUtil;
import javaemul.internal.annotations.HasNoSideEffects;

/**
 * Wraps a native <code>char</code> as an object.
 *
 * <p>TODO(jat): many of the classification methods implemented here are not correct in that they
 * only handle ASCII characters, and many other methods are not currently implemented. I think the
 * proper approach is to introduce * a deferred binding parameter which substitutes an
 * implementation using a fully-correct Unicode character database, at the expense of additional
 * data being downloaded. That way developers that need the functionality can get it without those
 * who don't need it paying for it.
 *
 * <pre>
 * The following methods are still not implemented -- most would require Unicode
 * character db to be useful:
 *  - digit / is* / to*(int codePoint)
 *  - isDefined(char)
 *  - isIdentifierIgnorable(char)
 *  - isJavaIdentifierPart(char)
 *  - isJavaIdentifierStart(char)
 *  - isJavaLetter(char) -- deprecated, so probably not
 *  - isJavaLetterOrDigit(char) -- deprecated, so probably not
 *  - isISOControl(char)
 *  - isMirrored(char)
 *  - isUnicodeIdentifierPart(char)
 *  - isUnicodeIdentifierStart(char)
 *  - getDirectionality(*)
 *  - getNumericValue(*)
 *  - getType(*)
 *  - reverseBytes(char) -- any use for this at all in the browser?
 *  - toTitleCase(int codepoint)
 *  - all the category constants for classification
 *
 * The following do not properly handle characters outside of ASCII:
 *  - digit(char c, int radix)
 *  - isDigit(char c)
 *  - isLetter(char c)
 *  - isLetterOrDigit(char c)
 *  - isLowerCase(char c)
 *  - isUpperCase(char c)
 * </pre>
 */
public final class Character implements Comparable<Character>, Serializable {
  /**
   * Helper class to share code between implementations, by making a char
   * array look like a CharSequence.
   */
  static class CharSequenceAdapter implements CharSequence {
    private char[] charArray;
    private int start;
    private int end;

    public CharSequenceAdapter(char[] charArray) {
      this(charArray, 0, charArray.length);
    }

    public CharSequenceAdapter(char[] charArray, int start, int end) {
      this.charArray = charArray;
      this.start = start;
      this.end = end;
    }

    @Override
    public char charAt(int index) {
      return charArray[index + start];
    }

    @Override
    public int length() {
      return end - start;
    }

    @Override
    public java.lang.CharSequence subSequence(int start, int end) {
      return new CharSequenceAdapter(charArray, this.start + start,
          this.start + end);
    }
  }

  /** Use nested class to avoid clinit on outer. */
  private static class BoxedValues {
    private static final Character[] boxedValues;

    static {
      // Box values according to JLS - from \u0000 to \u007f
      Character[] values = new Character[128];
      for (char i = 0; i < 128; i++) {
        values[i] = new Character(i);
      }
      boxedValues = values;
    }

    @HasNoSideEffects
    private static Character get(char c) {
      return boxedValues[c];
    }
  }

  public static final Class<Character> TYPE = char.class;
  public static final int MIN_RADIX = 2;

  public static final int MAX_RADIX = 36;
  public static final char MIN_VALUE = '\u0000';

  public static final char MAX_VALUE = '\uFFFF';
  public static final char MIN_SURROGATE = '\uD800';
  public static final char MAX_SURROGATE = '\uDFFF';
  public static final char MIN_LOW_SURROGATE = '\uDC00';
  public static final char MAX_LOW_SURROGATE = '\uDFFF';
  public static final char MIN_HIGH_SURROGATE = '\uD800';

  public static final char MAX_HIGH_SURROGATE = '\uDBFF';
  public static final int MIN_SUPPLEMENTARY_CODE_POINT = 0x10000;
  public static final int MIN_CODE_POINT = 0x0000;

  public static final int MAX_CODE_POINT = 0x10FFFF;

  public static final int SIZE = 16;
  public static final int BYTES = SIZE / Byte.SIZE;

  public static int charCount(int codePoint) {
    return codePoint >= MIN_SUPPLEMENTARY_CODE_POINT ? 2 : 1;
  }

  public static int codePointAt(char[] a, int index) {
    return codePointAt(new CharSequenceAdapter(a), index, a.length);
  }

  public static int codePointAt(char[] a, int index, int limit) {
    return codePointAt(new CharSequenceAdapter(a), index, limit);
  }

  public static int codePointAt(CharSequence seq, int index) {
    return codePointAt(seq, index, seq.length());
  }

  public static int codePointBefore(char[] a, int index) {
    return codePointBefore(new CharSequenceAdapter(a), index, 0);
  }

  public static int codePointBefore(char[] a, int index, int start) {
    return codePointBefore(new CharSequenceAdapter(a), index, start);
  }

  public static int codePointBefore(CharSequence cs, int index) {
    return codePointBefore(cs, index, 0);
  }

  public static int codePointCount(char[] a, int offset, int count) {
    return codePointCount(new CharSequenceAdapter(a), offset, offset + count);
  }

  public static int codePointCount(CharSequence seq, int beginIndex,
      int endIndex) {
    int count = 0;
    for (int idx = beginIndex; idx < endIndex; ) {
      char ch = seq.charAt(idx++);
      if (isHighSurrogate(ch) && idx < endIndex
          && (isLowSurrogate(seq.charAt(idx)))) {
        // skip the second char of surrogate pairs
        ++idx;
      }
      ++count;
    }
    return count;
  }

  public static int compare(char x, char y) {
    // JLS specifies that the chars are promoted to int before subtraction.
    return x - y;
  }

  /*
   * TODO: correct Unicode handling.
   */
  public static int digit(char c, int radix) {
    if (radix < MIN_RADIX || radix > MAX_RADIX) {
      return -1;
    }

    if (c >= '0' && c < '0' + Math.min(radix, 10)) {
      return c - '0';
    }

    // The offset by 10 is to re-base the alpha values
    if (c >= 'a' && c < (radix + 'a' - 10)) {
      return c - 'a' + 10;
    }

    if (c >= 'A' && c < (radix + 'A' - 10)) {
      return c - 'A' + 10;
    }

    return -1;
  }

  public static char forDigit(int digit, int radix) {
    if (radix < MIN_RADIX || radix > MAX_RADIX) {
      return 0;
    }

    if (digit < 0 || digit >= radix) {
      return 0;
    }

    return forDigit(digit);
  }

  public static int hashCode(char c) {
    return c;
  }

  public static boolean isBmpCodePoint(int codePoint) {
    return codePoint >= MIN_VALUE && codePoint <= MAX_VALUE;
  }

  private static NativeRegExp digitRegex;

  /*
   * TODO: correct Unicode handling.
   */
  public static boolean isDigit(char c) {
    if (digitRegex == null) {
      digitRegex = new NativeRegExp("\\d");
    }
    return digitRegex.test(String.valueOf(c));
  }

  public static boolean isHighSurrogate(char ch) {
    return ch >= MIN_HIGH_SURROGATE && ch <= MAX_HIGH_SURROGATE;
  }

  private static NativeRegExp leterRegex;

  /*
   * TODO: correct Unicode handling.
   */
  public static boolean isLetter(char c) {
    if (leterRegex == null) {
      leterRegex = new NativeRegExp("[A-Z]", "i");
    }
    return leterRegex.test(String.valueOf(c));
  }

  private static NativeRegExp isLeterOrDigitRegex;

  /*
   * TODO: correct Unicode handling.
   */
  public static boolean isLetterOrDigit(char c) {
    if (isLeterOrDigitRegex == null) {
      isLeterOrDigitRegex = new NativeRegExp("[A-Z\\d]", "i");
    }
    return isLeterOrDigitRegex.test(String.valueOf(c));
  }

  /*
   * TODO: correct Unicode handling.
   */
  public static boolean isLowerCase(char c) {
    return toLowerCase(c) == c && isLetter(c);
  }

  public static boolean isLowSurrogate(char ch) {
    return ch >= MIN_LOW_SURROGATE && ch <= MAX_LOW_SURROGATE;
  }

  public static boolean isSurrogate(char ch) {
    return ch >= MIN_SURROGATE && ch <= MAX_SURROGATE;
  }

  /**
   * Deprecated - see isWhitespace(char).
   */
  @Deprecated
  public static boolean isSpace(char c) {
    switch (c) {
      case ' ':
        return true;
      case '\n':
        return true;
      case '\t':
        return true;
      case '\f':
        return true;
      case '\r':
        return true;
      default:
        return false;
    }
  }

  public static boolean isWhitespace(char ch) {
    return StringUtil.isWhitespace(String.valueOf(ch));
  }

  public static boolean isWhitespace(int codePoint) {
    return isValidCodePoint(codePoint) && StringUtil.isWhitespace(String.fromCodePoint(codePoint));
  }

  public static boolean isSpaceChar(char ch) {
    return StringUtil.isSpace(String.valueOf(ch));
  }

  public static boolean isSpaceChar(int codePoint) {
    return isValidCodePoint(codePoint) && StringUtil.isSpace(String.fromCodePoint(codePoint));
  }

  public static boolean isSupplementaryCodePoint(int codePoint) {
    return codePoint >= MIN_SUPPLEMENTARY_CODE_POINT && codePoint <= MAX_CODE_POINT;
  }

  public static boolean isSurrogatePair(char highSurrogate, char lowSurrogate) {
    return isHighSurrogate(highSurrogate) && isLowSurrogate(lowSurrogate);
  }

  public static boolean isTitleCase(char c) {
    // https://www.compart.com/en/unicode/category/Lt
    return c != toUpperCase(c) && c != toLowerCase(c);
  }

  public static char toTitleCase(char c) {
    // In the vast majority of cases titlecase == uppercase, but there are some exceptions. A list
    // of codepoints where this isn't the case can be generated with:
    //   curl https://unicode.org/Public/UNIDATA/UnicodeData.txt 2>/dev/null \
    //     | awk -F';' '{ if ($13 != $15) print "cp: ",$1,"uc: ",$13,"tc: ",$15 }'
    if ((c >= '\u01C4' && c <= '\u01CC') || (c >= '\u01F1' && c <= '\u01F3')) {
      // In these ranges characters are grouped in sets of three consisting of (UC,Tc,lc).
      // Coincidentally the titlecase char is always on a value divisible by three, so we can abuse
      // integer division here.
      return (char) (((c + 1) / 3) * 3);
    } else if ((c >= '\u10D0' && c <= '\u10FA') || (c >= '\u10FD' && c <= '\u10FF')) {
      // In these ranges titlecase is equal to the original character.
      return c;
    }
    return toUpperCase(c);
  }

  /*
   * TODO: correct Unicode handling.
   */
  public static boolean isUpperCase(char c) {
    return toUpperCase(c) == c && isLetter(c);
  }

  public static boolean isValidCodePoint(int codePoint) {
    return codePoint >= MIN_CODE_POINT && codePoint <= MAX_CODE_POINT;
  }

  public static int offsetByCodePoints(char[] a, int start, int count, int index,
      int codePointOffset) {
    return offsetByCodePoints(new CharSequenceAdapter(a, start, count), index,
        codePointOffset);
  }

  public static int offsetByCodePoints(CharSequence seq, int index,
      int codePointOffset) {
    if (codePointOffset < 0) {
      // move backwards
      while (codePointOffset < 0) {
        --index;
        if (Character.isLowSurrogate(seq.charAt(index))
            && Character.isHighSurrogate(seq.charAt(index - 1))) {
          --index;
        }
        ++codePointOffset;
      }
    } else {
      // move forwards
      while (codePointOffset > 0) {
        if (Character.isHighSurrogate(seq.charAt(index))
            && Character.isLowSurrogate(seq.charAt(index + 1))) {
          ++index;
        }
        ++index;
        --codePointOffset;
      }
    }
    return index;
  }

  public static char[] toChars(int codePoint) {
    checkArgument(isValidCodePoint(codePoint));

    if (codePoint >= MIN_SUPPLEMENTARY_CODE_POINT) {
      return new char[] {
        highSurrogate(codePoint), lowSurrogate(codePoint),
      };
    } else {
      return new char[] {
          (char) codePoint,
      };
    }
  }

  public static int toChars(int codePoint, char[] dst, int dstIndex) {
    checkArgument(isValidCodePoint(codePoint));

    if (codePoint >= MIN_SUPPLEMENTARY_CODE_POINT) {
      dst[dstIndex++] = highSurrogate(codePoint);
      dst[dstIndex] = lowSurrogate(codePoint);
      return 2;
    } else {
      dst[dstIndex] = (char) codePoint;
      return 1;
    }
  }

  public static int toCodePoint(char highSurrogate, char lowSurrogate) {
    /*
     * High and low surrogate chars have the bottom 10 bits to store the value
     * above MIN_SUPPLEMENTARY_CODE_POINT, so grab those bits and add the
     * offset.
     */
    return MIN_SUPPLEMENTARY_CODE_POINT + ((highSurrogate & 1023) << 10) + (lowSurrogate & 1023);
  }

  public static int toLowerCase(int c) {
    return CaseMapper.codePointToLowerCase(c);
  }

  public static int toUpperCase(int c) {
    return CaseMapper.codePointToUpperCase(c);
  }

  public static char toLowerCase(char c) {
    return CaseMapper.charToLowerCase(c);
  }

  public static char toUpperCase(char c) {
    return CaseMapper.charToUpperCase(c);
  }

  public static String toString(char x) {
    return String.valueOf(x);
  }

  public static String toString(int x) {
    return String.fromCodePoint(x);
  }

  public static Character valueOf(char c) {
    if (c < 128) {
      return BoxedValues.get(c);
    }
    return new Character(c);
  }

  static int codePointAt(CharSequence cs, int index, int limit) {
    char hiSurrogate = cs.charAt(index++);
    char loSurrogate;
    if (Character.isHighSurrogate(hiSurrogate) && index < limit
        && Character.isLowSurrogate(loSurrogate = cs.charAt(index))) {
      return Character.toCodePoint(hiSurrogate, loSurrogate);
    }
    return hiSurrogate;
  }

  static int codePointBefore(CharSequence cs, int index, int start) {
    char loSurrogate = cs.charAt(--index);
    char highSurrogate;
    if (isLowSurrogate(loSurrogate) && index > start
        && isHighSurrogate(highSurrogate = cs.charAt(index - 1))) {
      return toCodePoint(highSurrogate, loSurrogate);
    }
    return loSurrogate;
  }

  /**
   * Shared implementation with {@link Long#toString}.
   *
   * @skip
   */
  static char forDigit(int digit) {
    final int overBaseTen = digit - 10;
    return (char) (overBaseTen < 0 ? '0' + digit : 'a' + overBaseTen);
  }

  /**
   * Computes the high surrogate character of the UTF16 representation of a non-BMP code point. See
   * {@link lowSurrogate}.
   *
   * @param codePoint requested codePoint, required to be >= MIN_SUPPLEMENTARY_CODE_POINT
   * @return high surrogate character
   */
  public static char highSurrogate(int codePoint) {
    return (char) (MIN_HIGH_SURROGATE
        + (((codePoint - MIN_SUPPLEMENTARY_CODE_POINT) >> 10) & 1023));
  }

  /**
   * Computes the low surrogate character of the UTF16 representation of a non-BMP code point. See
   * {@link highSurrogate}.
   *
   * @param codePoint requested codePoint, required to be >= MIN_SUPPLEMENTARY_CODE_POINT
   * @return low surrogate character
   */
  public static char lowSurrogate(int codePoint) {
    return (char) (MIN_LOW_SURROGATE + ((codePoint - MIN_SUPPLEMENTARY_CODE_POINT) & 1023));
  }

  private final char value;

  public Character(char value) {
    this.value = value;
  }

  public char charValue() {
    return value;
  }

  @Override
  public int compareTo(Character c) {
    return compare(value, c.value);
  }

  @Override
  public boolean equals(Object o) {
    return (o instanceof Character) && (((Character) o).value == value);
  }

  @Override
  public int hashCode() {
    return hashCode(value);
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }
}
