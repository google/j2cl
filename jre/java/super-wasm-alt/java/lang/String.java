/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package java.lang;

import static javaemul.internal.InternalPreconditions.checkCriticalStringBounds;
import static javaemul.internal.InternalPreconditions.checkNotNull;
import static javaemul.internal.InternalPreconditions.checkPositionIndexes;
import static javaemul.internal.InternalPreconditions.checkStringBounds;
import static javaemul.internal.InternalPreconditions.checkStringElementIndex;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.StringJoiner;
import javaemul.internal.ArrayHelper;
import javaemul.internal.EmulatedCharset;
import javaemul.internal.NativeRegExp;
import javaemul.internal.annotations.HasNoSideEffects;
import javaemul.internal.annotations.Wasm;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * An immutable sequence of characters/code units ({@code char}s). A {@code String} is represented
 * by array of UTF-16 values, such that Unicode supplementary characters (code points) are
 * stored/encoded as surrogate pairs via Unicode code units ({@code char}).
 */
public final class String implements Serializable, Comparable<String>, CharSequence {

  // TODO(b/272381112): Remove after non-stringref experiment.
  public static final boolean STRINGREF_ENABLED = false;

  private static final char REPLACEMENT_CHAR = (char) 0xfffd;

  private static final class CaseInsensitiveComparator implements Comparator<String>, Serializable {

    public int compare(String o1, String o2) {
      return o1.compareToIgnoreCase(o2);
    }
  }

  public static final Comparator<String> CASE_INSENSITIVE_ORDER = new CaseInsensitiveComparator();

  private final char[] value;
  private final int offset;
  private final int count;
  private int hashCode;

  public String() {
    value = new char[0];
    offset = 0;
    count = 0;
  }

  public String(byte[] data) {
    this(data, 0, data.length);
  }

  @Deprecated
  public String(byte[] data, int high) {
    this(data, high, 0, data.length);
  }

  public String(byte[] data, int offset, int byteCount) {
    this(data, offset, byteCount, Charset.defaultCharset());
  }

  @Deprecated
  public String(byte[] data, int high, int offset, int byteCount) {
    checkCriticalStringBounds(offset, offset + byteCount, data.length);

    this.offset = 0;
    this.value = new char[byteCount];
    this.count = byteCount;
    high <<= 8;
    for (int i = 0; i < count; i++) {
      value[i] = (char) (high + (data[offset++] & 0xff));
    }
  }

  public String(byte[] data, int offset, int byteCount, String charsetName)
      throws UnsupportedEncodingException {
    this(data, offset, byteCount, getCharset(charsetName));
  }

  public String(byte[] data, String charsetName) throws UnsupportedEncodingException {
    this(data, 0, data.length, getCharset(charsetName));
  }

  public String(byte[] data, int offset, int byteCount, Charset charset) {
    checkPositionIndexes(offset, offset + byteCount, data.length);
    this.value = ((EmulatedCharset) charset).decodeString(data, offset, byteCount);
    this.offset = 0;
    this.count = this.value.length;
  }

  public String(byte[] data, Charset charset) {
    this(data, 0, data.length, charset);
  }

  public String(char[] data) {
    this(data, 0, data.length);
  }

  public String(char[] data, int offset, int charCount) {
    checkCriticalStringBounds(offset, offset + charCount, data.length);

    this.offset = 0;
    this.value = new char[charCount];
    this.count = charCount;
    System.arraycopy(data, offset, value, 0, count);
  }

  /*
   * Internal version of the String(char[], int, int) constructor.
   * Does not range check, null check, or copy the character array.
   */
  String(int offset, int charCount, char[] chars) {
    this.value = chars;
    this.offset = offset;
    this.count = charCount;
  }

  public String(String toCopy) {
    value =
        (toCopy.value.length == toCopy.count)
            ? toCopy.value
            : Arrays.copyOfRange(toCopy.value, toCopy.offset, toCopy.offset + toCopy.length());
    offset = 0;
    count = value.length;
  }

  public String(StringBuffer stringBuffer) {
    this((AbstractStringBuilder) stringBuffer);
  }

  public String(StringBuilder stringBuilder) {
    this((AbstractStringBuilder) stringBuilder);
  }

  String(AbstractStringBuilder builder) {
    offset = 0;
    value = builder.shareValue();
    count = builder.length();
  }

  public String(int[] codePoints, int offset, int count) {
    checkCriticalStringBounds(offset, offset + count, codePoints.length);

    this.offset = 0;
    this.value = new char[count * 2];
    int end = offset + count;
    int c = 0;
    for (int i = offset; i < end; i++) {
      c += Character.toChars(codePoints[i], this.value, c);
    }
    this.count = c;
  }

  public char charAt(int index) {
    checkStringElementIndex(index, count);
    return value[offset + index];
  }

  public int compareTo(String string) {
    if (this == string) {
      return 0;
    }
    int o1 = offset, o2 = string.offset;
    int end = offset + Math.min(count, string.count);
    char[] thisArray = value;
    char[] otherArray = string.value;
    while (o1 < end) {
      int result = thisArray[o1] - otherArray[o2];
      if (result != 0) {
        return result;
      }
      o1++;
      o2++;
    }
    return count - string.count;
  }

  public int compareToIgnoreCase(String other) {
    if (this == other) {
      return 0;
    }

    int o1 = offset, o2 = other.offset;
    int end = offset + Math.min(count, other.count);
    char[] thisArray = value;
    char[] otherArray = other.value;
    while (o1 < end) {
      char c1 = thisArray[o1];
      char c2 = otherArray[o2];
      if (c1 != c2) {
        if (c1 > 127 || c2 > 127) {
          // Branch into native implementation since we cannot handle case folding for non-ascii
          // chars.
          return nativeCompareToIgnoreCase(
              nativeFromCharCodeArray(thisArray, o1, offset + count),
              nativeFromCharCodeArray(otherArray, o2, other.offset + other.count));
        }
        int result = foldCaseAscii(c1) - foldCaseAscii(c2);
        if (result != 0) {
          return result;
        }
      }
      o1++;
      o2++;
    }
    return count - other.count;
  }

  @JsMethod(namespace = "j2wasm.StringUtils", name = "compareToIgnoreCase")
  private static native int nativeCompareToIgnoreCase(NativeString a, NativeString b);

  public String concat(String string) {
    if (string.count > 0 && count > 0) {
      char[] buffer = new char[count + string.count];
      System.arraycopy(value, offset, buffer, 0, count);
      System.arraycopy(string.value, string.offset, buffer, count, string.count);
      return new String(0, buffer.length, buffer);
    }
    return count == 0 ? string : this;
  }

  public static String copyValueOf(char[] data) {
    return new String(data, 0, data.length);
  }

  public static String copyValueOf(char[] data, int start, int length) {
    return new String(data, start, length);
  }

  public boolean endsWith(String suffix) {
    return regionMatches(count - suffix.count, suffix, 0, suffix.count);
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if (other instanceof String) {
      String s = (String) other;
      int length = count;
      if (s.count != length) {
        return false;
      }

      // Check the cached hashCodes (if any) for quick answer.
      int hash = hashCode;
      if (hash != 0) {
        int otherHash = s.hashCode;
        if (otherHash != 0 && otherHash != hash) {
          return false;
        }
      }

      return isRegionEqual(value, offset, s.value, s.offset, length);
    } else {
      return false;
    }
  }

  private static boolean isRegionEqual(char[] a, int aOffset, char[] b, int bOffset, int length) {
    // CHeck both ends so that we can catch early if only differs on suffix and reduce jumps.
    int aEnd = aOffset + length - 1;
    int bEnd = bOffset + length - 1;
    int iterLength = (length + 1) / 2;
    // Note that we may end up checking an index twice for half of the time but that is likely
    // better than trying to guard against that edge case in each iteration.
    for (int i = 0; i < iterLength; i++) {
      if (a[aOffset + i] != b[bOffset + i] || a[aEnd - i] != b[bEnd - i]) {
        return false;
      }
    }
    return true;
  }

  public boolean equalsIgnoreCase(String other) {
    if (other == this) {
      return true;
    }
    if (other == null) {
      return false;
    }
    int length = count;
    if (length != other.count) {
      return false;
    }

    return isRegionEqualIgnoreCase(value, offset, other.value, other.offset, length);
  }

  private static boolean isRegionEqualIgnoreCase(
      char[] a, int aOffset, char[] b, int bOffset, int length) {
    // CHeck both ends so that we can catch early if only differs on suffix and reduce jumps.
    int aEnd = aOffset + length - 1;
    int bEnd = bOffset + length - 1;
    int iterLength = (length + 1) / 2;
    // Note that we may end up checking an index twice for half of the time but that is likely
    // better than trying to guard against that edge case in each iteration.
    for (int i = 0; i < iterLength; i++) {
      {
        char c1 = a[aOffset + i];
        char c2 = b[bOffset + i];
        if (c1 != c2) {
          if (c1 > 127 && c2 > 127) {
            // Branch into native implementation since we cannot handle case folding for non-ascii
            // space.
            return nativeEqualsIgnoreCase(
                nativeFromCharCodeArray(a, aOffset + i, aEnd - i + 1),
                nativeFromCharCodeArray(b, bOffset + i, bEnd - i + 1));
          }
          if (foldCaseAscii(c1) != foldCaseAscii(c2)) {
            return false;
          }
        }
      }
      {
        char c1 = a[aEnd - i];
        char c2 = b[bEnd - i];
        if (c1 != c2) {
          if (c1 > 127 && c2 > 127) {
            // Branch into native implementation since we cannot handle case folding for non-ascii
            // space.
            return nativeEqualsIgnoreCase(
                nativeFromCharCodeArray(a, aOffset + i + 1, aEnd - i + 1),
                nativeFromCharCodeArray(b, bOffset + i + 1, bEnd - i + 1));
          }
          if (foldCaseAscii(c1) != foldCaseAscii(c2)) {
            return false;
          }
        }
      }
    }
    return true;
  }

  @JsMethod(namespace = "j2wasm.StringUtils", name = "equalsIgnoreCase")
  private static native boolean nativeEqualsIgnoreCase(NativeString a, NativeString b);

  private static char foldCaseAscii(char value) {
    if ('A' <= value && value <= 'Z') {
      return (char) (value + ('a' - 'A'));
    }
    return value;
  }

  public byte[] getBytes() {
    return getBytes(Charset.defaultCharset());
  }

  public byte[] getBytes(String charsetName) throws UnsupportedEncodingException {
    return getBytes(getCharset(charsetName));
  }

  public byte[] getBytes(Charset charset) {
    return ((EmulatedCharset) charset).getBytes(value, offset, count);
  }

  private static Charset getCharset(String charsetName) throws UnsupportedEncodingException {
    try {
      return Charset.forName(charsetName);
    } catch (UnsupportedCharsetException e) {
      throw new UnsupportedEncodingException(charsetName);
    }
  }

  public void getChars(int start, int end, char[] buffer, int index) {
    checkCriticalStringBounds(start, end, length());
    checkCriticalStringBounds(index, index + (end - start), buffer.length);
    System.arraycopy(value, start + offset, buffer, index, end - start);
  }

  void getChars0(int start, int end, char[] buffer, int index) {
    // NOTE last character not copied!
    System.arraycopy(value, start + offset, buffer, index, end - start);
  }

  @Override
  public int hashCode() {
    int hash = hashCode;
    if (hash == 0) {
      if (count == 0) {
        return 0;
      }
      final int end = count + offset;
      final char[] chars = value;
      for (int i = offset; i < end; ++i) {
        hash = 31 * hash + chars[i];
      }
      hashCode = hash;
    }
    return hash;
  }

  public int indexOf(int c) {
    // TODO: just "return indexOf(c, 0);" when the JIT can inline that deep.
    if (c > 0xffff) {
      return indexOfSupplementary(c, 0);
    }
    return fastIndexOf((char) c, 0);
  }

  public int indexOf(int c, int start) {
    if (c > 0xffff) {
      return indexOfSupplementary(c, start);
    }
    return fastIndexOf((char) c, start);
  }

  private int fastIndexOf(char c, int start) {
    char[] buffer = value;
    int first = offset + start;
    int last = offset + count;
    for (int i = first; i < last; i++) {
      if (buffer[i] == c) {
        return i - offset;
      }
    }
    return -1;
  }

  private int indexOfSupplementary(int c, int start) {
    if (!Character.isSupplementaryCodePoint(c)) {
      return -1;
    }
    char[] chars = Character.toChars(c);
    String needle = new String(0, chars.length, chars);
    return indexOf(needle, start);
  }

  public int indexOf(String subString) {
    return indexOf(subString, 0);
  }

  public int indexOf(String string, int start) {
    int subCount = string.count;
    int length = count;
    if (subCount > 0) {
      if (subCount > length) {
        return -1;
      }
      char[] thisArray = value;
      char[] otherArray = string.value;
      int subOffset = string.offset;
      char firstChar = otherArray[subOffset];
      int end = subOffset + subCount;
      while (true) {
        int i = indexOf(firstChar, start);
        if (i == -1 || subCount + i > length) {
          return -1; // handles subCount > count || start >= count
        }
        int o1 = offset + i, o2 = subOffset;
        while (++o2 < end && thisArray[++o1] == otherArray[o2]) {
          // Intentionally empty
        }
        if (o2 == end) {
          return i;
        }
        start = i + 1;
      }
    }
    return start < length ? start : length;
  }

  // public native String intern();

  public boolean isEmpty() {
    return count == 0;
  }

  public int lastIndexOf(int c) {
    if (c > 0xffff) {
      return lastIndexOfSupplementary(c, Integer.MAX_VALUE);
    }
    int length = count;
    int thisOffset = offset;
    char[] thisArray = value;
    for (int i = thisOffset + length - 1; i >= thisOffset; --i) {
      if (thisArray[i] == c) {
        return i - thisOffset;
      }
    }
    return -1;
  }

  public int lastIndexOf(int c, int start) {
    if (c > 0xffff) {
      return lastIndexOfSupplementary(c, start);
    }
    int length = count;
    int thisOffset = offset;
    char[] thisArray = value;
    if (start >= 0) {
      if (start >= length) {
        start = length - 1;
      }
      for (int i = thisOffset + start; i >= thisOffset; --i) {
        if (thisArray[i] == c) {
          return i - thisOffset;
        }
      }
    }
    return -1;
  }

  private int lastIndexOfSupplementary(int c, int start) {
    if (!Character.isSupplementaryCodePoint(c)) {
      return -1;
    }
    char[] chars = Character.toChars(c);
    String needle = new String(0, chars.length, chars);
    return lastIndexOf(needle, start);
  }

  public int lastIndexOf(String string) {
    // Use count instead of count - 1 so lastIndexOf("") returns count
    return lastIndexOf(string, count);
  }

  public int lastIndexOf(String subString, int start) {
    int subCount = subString.count;
    if (subCount <= count && start >= 0) {
      if (subCount > 0) {
        if (start > count - subCount) {
          start = count - subCount;
        }
        // count and subCount are both >= 1
        char[] thisArray = value;
        char[] otherArray = subString.value;
        int subOffset = subString.offset;
        char firstChar = otherArray[subOffset];
        int end = subOffset + subCount;
        while (true) {
          int i = lastIndexOf(firstChar, start);
          if (i == -1) {
            return -1;
          }
          int o1 = offset + i, o2 = subOffset;
          while (++o2 < end && thisArray[++o1] == otherArray[o2]) {
            // Intentionally empty
          }
          if (o2 == end) {
            return i;
          }
          start = i - 1;
        }
      }
      return start < count ? start : count;
    }
    return -1;
  }

  public int length() {
    return count;
  }

  public boolean regionMatches(int thisStart, String string, int start, int length) {
    return regionMatches(false, thisStart, string, start, length);
  }

  public boolean regionMatches(
      boolean ignoreCase, int thisStart, String string, int start, int length) {
    checkNotNull(string);
    if (start < 0 || string.count - start < length) {
      return false;
    }
    if (thisStart < 0 || count - thisStart < length) {
      return false;
    }
    return ignoreCase
        ? isRegionEqualIgnoreCase(
            value, offset + thisStart, string.value, string.offset + start, length)
        : isRegionEqual(value, offset + thisStart, string.value, string.offset + start, length);
  }

  public String replace(char oldChar, char newChar) {
    char[] buffer = value;
    int thisOffset = offset;
    int length = count;
    int idx = thisOffset;
    int last = thisOffset + length;
    boolean copied = false;
    while (idx < last) {
      if (buffer[idx] == oldChar) {
        if (!copied) {
          char[] newBuffer = new char[length];
          System.arraycopy(buffer, thisOffset, newBuffer, 0, length);
          buffer = newBuffer;
          idx -= thisOffset;
          last -= thisOffset;
          copied = true;
        }
        buffer[idx] = newChar;
      }
      idx++;
    }
    return copied ? new String(0, count, buffer) : this;
  }

  public String replace(CharSequence target, CharSequence replacement) {
    String targetString = target.toString();
    int matchStart = indexOf(targetString, 0);
    if (matchStart == -1) {
      // If there's nothing to replace, return the original string untouched.
      return this;
    }
    String replacementString = replacement.toString();
    // The empty target matches at the start and end and between each character.
    int targetLength = targetString.length();
    if (targetLength == 0) {
      // The result contains the original 'count' characters, a copy of the
      // replacement string before every one of those characters, and a final
      // copy of the replacement string at the end.
      int resultLength = count + (count + 1) * replacementString.length();
      StringBuilder result = new StringBuilder(resultLength);
      result.append(replacementString);
      int end = offset + count;
      for (int i = offset; i != end; ++i) {
        result.append(value[i]);
        result.append(replacementString);
      }
      return result.toString();
    }
    StringBuilder result = new StringBuilder(count);
    int searchStart = 0;
    do {
      // Copy characters before the match...
      result.append(value, offset + searchStart, matchStart - searchStart);
      // Insert the replacement...
      result.append(replacementString);
      // And skip over the match...
      searchStart = matchStart + targetLength;
    } while ((matchStart = indexOf(targetString, searchStart)) != -1);
    // Copy any trailing chars...
    result.append(value, offset + searchStart, count - searchStart);
    return result.toString();
  }

  public boolean startsWith(String prefix) {
    return startsWith(prefix, 0);
  }

  public boolean startsWith(String prefix, int start) {
    return regionMatches(start, prefix, 0, prefix.count);
  }

  public String substring(int start) {
    checkStringElementIndex(start, count + 1);

    if (start == 0) {
      return this;
    }
    return new String(offset + start, count - start, value);
  }

  public String substring(int start, int end) {
    checkStringBounds(start, end, count);

    if (start == 0 && end == count) {
      return this;
    }

    return new String(offset + start, end - start, value);
  }

  public char[] toCharArray() {
    char[] buffer = new char[count];
    System.arraycopy(value, offset, buffer, 0, count);
    return buffer;
  }

  public String toLowerCase() {
    return CaseMapper.toLowerCase(this, value, offset, count);
  }

  public String toLowerCase(Locale locale) {
    return locale == Locale.getDefault()
        ? fromJsString(toJsString().toLocaleLowerCase())
        : toLowerCase();
  }

  @Override
  public String toString() {
    return this;
  }

  public String toUpperCase() {
    return CaseMapper.toUpperCase(this, value, offset, count);
  }

  public String toUpperCase(Locale locale) {
    return locale == Locale.getDefault()
        ? fromJsString(toJsString().toLocaleUpperCase())
        : toUpperCase();
  }

  public String trim() {
    int start = offset, last = offset + count - 1;
    int end = last;
    char[] thisArray = value;
    while ((start <= end) && (thisArray[start] <= ' ')) {
      start++;
    }
    while ((end >= start) && (thisArray[end] <= ' ')) {
      end--;
    }
    if (start == offset && end == last) {
      return this;
    }
    return new String(start, end - start + 1, value);
  }

  public static String join(CharSequence delimiter, CharSequence... elements) {
    StringJoiner joiner = new StringJoiner(delimiter);
    for (CharSequence e : elements) {
      joiner.add(e);
    }
    return joiner.toString();
  }

  public static String join(CharSequence delimiter, Iterable<? extends CharSequence> elements) {
    StringJoiner joiner = new StringJoiner(delimiter);
    for (CharSequence e : elements) {
      joiner.add(e);
    }
    return joiner.toString();
  }

  public static String valueOf(char[] data) {
    return new String(data, 0, data.length);
  }

  public static String valueOf(char[] data, int start, int length) {
    return new String(data, start, length);
  }

  public static String valueOf(char value) {
    String s;
    if (value < 128) {
      s = new String(value, 1, getSmallCharValuesArray());
    } else {
      s = new String(0, 1, new char[] {value});
    }
    s.hashCode = value;
    return s;
  }

  // Do not initialize inline to avoid clinit.
  private static char[] smallCharValues;

  @HasNoSideEffects
  private static char[] getSmallCharValuesArray() {
    if (smallCharValues == null) {
      smallCharValues = new char[128];
      for (int i = 0; i < smallCharValues.length; ++i) {
        smallCharValues[i] = (char) i;
      }
    }
    return smallCharValues;
  }

  public static String valueOf(double value) {
    return RealToString.doubleToString(value);
  }

  public static String valueOf(float value) {
    return RealToString.floatToString(value);
  }

  public static String valueOf(int value) {
    return IntegralToString.intToString(value);
  }

  public static String valueOf(long value) {
    return IntegralToString.longToString(value);
  }

  public static String valueOf(Object value) {
    return value != null ? value.toString() : "null";
  }

  public static String valueOf(boolean value) {
    return value ? "true" : "false";
  }

  // Internal API to create String instance without an array copy.
  static String fromInternalArray(char[] array) {
    return new String(0, array.length, array);
  }

  static String fromCodePoint(int codePoint) {
    return fromInternalArray(Character.toChars(codePoint));
  }

  public boolean contentEquals(StringBuffer strbuf) {
    int len = strbuf.length();
    if (len != count) {
      return false;
    }
    return isRegionEqual(value, offset, strbuf.getValue(), 0, len);
  }

  public boolean contentEquals(CharSequence cs) {
    if (cs == this) {
      return true;
    }
    int len = cs.length();
    if (len != count) {
      return false;
    }
    if (len == 0) {
      return true; // since both are empty strings
    }
    String str = cs.toString();
    return isRegionEqual(value, offset, str.value, str.offset, len);
  }

  public boolean matches(String regex) {
    // We surround the regex with '^' and '$' because it must match the entire string.
    return new NativeRegExp("^(" + regex + ")$").test(this);
  }

  public String replaceAll(String regex, String replace) {
    replace = translateReplaceString(replace);
    return nativeReplaceAll(regex, replace);
  }

  // TODO: should live on a utility instead of the String API.
  public String nativeReplace(NativeRegExp regExp, char replacement) {
    return fromJsString(toJsString().replace(regExp, nativeFromCharCode(replacement)));
  }

  // TODO: should live on a utility instead of the String API.
  public String nativeReplace(NativeRegExp regExp, String replacement) {
    return fromJsString(toJsString().replace(regExp, replacement.toJsString()));
  }

  private String nativeReplaceAll(String regex, String replace) {
    return fromJsString(toJsString().replace(new NativeRegExp(regex, "g"), replace.toJsString()));
  }

  public String replaceFirst(String regex, String replace) {
    replace = translateReplaceString(replace);
    NativeRegExp jsRegEx = new NativeRegExp(regex);
    return fromJsString(toJsString().replace(jsRegEx, replace.toJsString()));
  }

  private static String translateReplaceString(String replaceStr) {
    int pos = 0;
    while (0 <= (pos = replaceStr.indexOf("\\", pos))) {
      if (replaceStr.charAt(pos + 1) == '$') {
        replaceStr = replaceStr.substring(0, pos) + "$" + replaceStr.substring(++pos);
      } else {
        replaceStr = replaceStr.substring(0, pos) + replaceStr.substring(++pos);
      }
    }
    return replaceStr;
  }

  public String[] split(String regex) {
    return split(regex, 0);
  }

  public String[] split(String regex, int maxMatch) {
    // The compiled regular expression created from the string
    NativeRegExp compiled = new NativeRegExp(regex, "g");
    // the Javascipt array to hold the matches prior to conversion
    String[] out = new String[0];
    // how many matches performed so far
    int count = 0;
    // The current string that is being matched; trimmed as each piece matches
    String trail = this;
    // used to detect repeated zero length matches
    // Must be null to start with because the first match of "" makes no
    // progress by intention
    String lastTrail = null;
    // We do the split manually to avoid Javascript incompatibility
    while (true) {
      // None of the information in the match returned are useful as we have no
      // subgroup handling
      NativeRegExp.Match matchObj = compiled.exec(trail);
      if (matchObj == null || trail.isEmpty() || (count == (maxMatch - 1) && maxMatch > 0)) {
        ArrayHelper.push(out, trail);
        break;
      } else {
        int matchIndex = matchObj.getIndex();
        ArrayHelper.push(out, trail.substring(0, matchIndex));
        trail = trail.substring(matchIndex + matchObj.getAt(0).length(), trail.length());
        // Force the compiled pattern to reset internal state
        compiled.setLastIndex(0);
        // Only one zero length match per character to ensure termination
        if (lastTrail == trail) {
          out[count] = trail.substring(0, 1);
          trail = trail.substring(1);
        }
        lastTrail = trail;
        count++;
      }
    }
    // all blank delimiters at the end are supposed to disappear if maxMatch == 0;
    // however, if the input string is empty, the output should consist of a
    // single empty string
    if (maxMatch == 0 && this.length() > 0) {
      int lastNonEmpty = out.length;
      while (lastNonEmpty > 0 && out[lastNonEmpty - 1].isEmpty()) {
        --lastNonEmpty;
      }
      if (lastNonEmpty < out.length) {
        ArrayHelper.setLength(out, lastNonEmpty);
      }
    }
    return out;
  }

  public CharSequence subSequence(int start, int end) {
    return substring(start, end);
  }

  public int codePointAt(int index) {
    return Character.codePointAt(value, offset + index, offset + count);
  }

  public int codePointBefore(int index) {
    return Character.codePointBefore(value, offset + index, offset);
  }

  public int codePointCount(int start, int end) {
    return Character.codePointCount(value, offset + start, end - start);
  }

  public boolean contains(CharSequence cs) {
    return indexOf(cs.toString()) >= 0;
  }

  public int offsetByCodePoints(int index, int codePointOffset) {
    int s = index + offset;
    int r = Character.offsetByCodePoints(value, offset, count, s, codePointOffset);
    return r - offset;
  }

  // Helper methods to pass and receive strings to and from JavaScript.

  // TODO(b/268386628): Hide this helper once external references are cleaned up.
  /** Returns a JavaScript string that can be used to pass to JavaScript imported methods. */
  public NativeString toJsString() {
    return nativeFromCharCodeArray(value, offset, offset + count);
  }

  // TODO(b/268386628): Hide this helper once external references are cleaned up.
  public static NativeString toJsString(String string) {
    return string == null ? null : string.toJsString();
  }

  /** Returns a String using the char values provided as a JavaScript array. */
  public static String fromJsString(NativeString jsString) {
    if (jsString == null) {
      return null;
    }
    int count = nativeGetLength(asStringView(jsString));
    char[] array = new char[count];
    int unused = nativeGetChars(jsString, array, 0);
    return new String(0, count, array);
  }

  @Wasm("string.new_wtf16_array")
  private static native NativeString nativeFromCharCodeArray(char[] x, int start, int end);

  @Wasm("stringview_wtf16.length")
  private static native int nativeGetLength(NativeStringView stringView);

  @Wasm("string.encode_wtf16_array")
  private static native int nativeGetChars(NativeString s, char[] x, int start);

  @JsMethod(namespace = JsPackage.GLOBAL, name = "String.fromCharCode")
  private static native NativeString nativeFromCharCode(char x);

  /** Native JS compatible representation of a string. */
  // TODO(b/268386628): Hide NativeString once external references are cleaned up.
  @Wasm("string")
  @JsType(isNative = true, name = "string", namespace = JsPackage.GLOBAL)
  public interface NativeString {
    NativeString replace(NativeRegExp regex, NativeString replace);

    NativeString toLowerCase();

    NativeString toUpperCase();

    NativeString toLocaleLowerCase();

    NativeString toLocaleUpperCase();
  }

  @Wasm("stringview_wtf16")
  private interface NativeStringView {}

  @Wasm("string.as_wtf16")
  private static native NativeStringView asStringView(NativeString stringView);
}
