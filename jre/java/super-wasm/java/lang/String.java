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
import static javaemul.internal.InternalPreconditions.checkStringBounds;
import static javaemul.internal.InternalPreconditions.checkStringElementIndex;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import javaemul.internal.EmulatedCharset;

/**
 * An immutable sequence of characters/code units ({@code char}s). A {@code String} is represented
 * by array of UTF-16 values, such that Unicode supplementary characters (code points) are
 * stored/encoded as surrogate pairs via Unicode code units ({@code char}).
 */
public final class String implements Serializable, Comparable<String>, CharSequence {

  private static final char REPLACEMENT_CHAR = (char) 0xfffd;

  private static final class CaseInsensitiveComparator implements Comparator<String>, Serializable {

    public int compare(String o1, String o2) {
      return o1.compareToIgnoreCase(o2);
    }
  }

  public static final Comparator<String> CASE_INSENSITIVE_ORDER = new CaseInsensitiveComparator();

  private static final char[] ASCII;

  static {
    ASCII = new char[128];
    for (int i = 0; i < ASCII.length; ++i) {
      ASCII[i] = (char) i;
    }
  }

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
    checkCriticalStringBounds(offset, offset + byteCount, data.length);

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
    offset = 0;
    synchronized (stringBuffer) {
      value = stringBuffer.shareValue();
      count = stringBuffer.length();
    }
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

  public String(StringBuilder stringBuilder) {
    if (stringBuilder == null) {
      throw new NullPointerException("stringBuilder == null");
    }
    this.offset = 0;
    this.count = stringBuilder.length();
    this.value = new char[this.count];
    stringBuilder.getChars(0, this.count, this.value, 0);
  }

  public char charAt(int index) {
    checkStringElementIndex(index, count);
    return value[offset + index];
  }

  private char foldCase(char ch) {
    if (ch < 128) {
      if ('A' <= ch && ch <= 'Z') {
        return (char) (ch + ('a' - 'A'));
      }
      return ch;
    }
    return Character.toLowerCase(Character.toUpperCase(ch));
  }

  public int compareTo(String string) {
    int o1 = offset, o2 = string.offset, result;
    int end = offset + (count < string.count ? count : string.count);
    char c1, c2;
    char[] target = string.value;
    while (o1 < end) {
      if ((c1 = value[o1++]) == (c2 = target[o2++])) {
        continue;
      }
      if ((result = c1 - c2) != 0) {
        return result;
      }
    }
    return count - string.count;
  }

  public int compareToIgnoreCase(String string) {
    int o1 = offset, o2 = string.offset, result;
    int end = offset + (count < string.count ? count : string.count);
    char c1, c2;
    char[] target = string.value;
    while (o1 < end) {
      if ((c1 = value[o1++]) == (c2 = target[o2++])) {
        continue;
      }
      c1 = foldCase(c1);
      c2 = foldCase(c2);
      if ((result = c1 - c2) != 0) {
        return result;
      }
    }
    return count - string.count;
  }

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
      int count = this.count;
      if (s.count != count) {
        return false;
      }
      // TODO: we want to avoid many boundchecks in the loop below
      // for long Strings until we have array equality intrinsic.
      // Bad benchmarks just push .equals without first getting a
      // hashCode hit (unlike real world use in a Hashtable). Filter
      // out these long strings here. When we get the array equality
      // intrinsic then remove this use of hashCode.
      if (hashCode() != s.hashCode()) {
        return false;
      }
      char[] value1 = value;
      int offset1 = offset;
      char[] value2 = s.value;
      int offset2 = s.offset;
      for (int end = offset1 + count; offset1 < end; ) {
        if (value1[offset1] != value2[offset2]) {
          return false;
        }
        offset1++;
        offset2++;
      }
      return true;
    } else {
      return false;
    }
  }

  public boolean equalsIgnoreCase(String string) {
    if (string == this) {
      return true;
    }
    if (string == null || count != string.count) {
      return false;
    }
    int o1 = offset, o2 = string.offset;
    int end = offset + count;
    char[] target = string.value;
    while (o1 < end) {
      char c1 = value[o1++];
      char c2 = target[o2++];
      if (c1 != c2 && foldCase(c1) != foldCase(c2)) {
        return false;
      }
    }
    return true;
  }

  public byte[] getBytes() {
    return getBytes(Charset.defaultCharset());
  }

  public byte[] getBytes(String charsetName) throws UnsupportedEncodingException {
    return getBytes(getCharset(charsetName));
  }

  public byte[] getBytes(Charset charset) {
    return ((EmulatedCharset) charset).getBytes(this);
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

  void _getChars(int start, int end, char[] buffer, int index) {
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

  public int indexOf(String string) {
    int start = 0;
    int subCount = string.count;
    int _count = count;
    if (subCount > 0) {
      if (subCount > _count) {
        return -1;
      }
      char[] target = string.value;
      int subOffset = string.offset;
      char firstChar = target[subOffset];
      int end = subOffset + subCount;
      while (true) {
        int i = indexOf(firstChar, start);
        if (i == -1 || subCount + i > _count) {
          return -1; // handles subCount > count || start >= count
        }
        int o1 = offset + i, o2 = subOffset;
        char[] _value = value;
        while (++o2 < end && _value[++o1] == target[o2]) {
          // Intentionally empty
        }
        if (o2 == end) {
          return i;
        }
        start = i + 1;
      }
    }
    return start < _count ? start : _count;
  }

  public int indexOf(String subString, int start) {
    if (start < 0) {
      start = 0;
    }
    int subCount = subString.count;
    int _count = count;
    if (subCount > 0) {
      if (subCount + start > _count) {
        return -1;
      }
      char[] target = subString.value;
      int subOffset = subString.offset;
      char firstChar = target[subOffset];
      int end = subOffset + subCount;
      while (true) {
        int i = indexOf(firstChar, start);
        if (i == -1 || subCount + i > _count) {
          return -1; // handles subCount > count || start >= count
        }
        int o1 = offset + i, o2 = subOffset;
        char[] _value = value;
        while (++o2 < end && _value[++o1] == target[o2]) {
          // Intentionally empty
        }
        if (o2 == end) {
          return i;
        }
        start = i + 1;
      }
    }
    return start < _count ? start : _count;
  }

  // public native String intern();

  public boolean isEmpty() {
    return count == 0;
  }

  public int lastIndexOf(int c) {
    if (c > 0xffff) {
      return lastIndexOfSupplementary(c, Integer.MAX_VALUE);
    }
    int _count = count;
    int _offset = offset;
    char[] _value = value;
    for (int i = _offset + _count - 1; i >= _offset; --i) {
      if (_value[i] == c) {
        return i - _offset;
      }
    }
    return -1;
  }

  public int lastIndexOf(int c, int start) {
    if (c > 0xffff) {
      return lastIndexOfSupplementary(c, start);
    }
    int _count = count;
    int _offset = offset;
    char[] _value = value;
    if (start >= 0) {
      if (start >= _count) {
        start = _count - 1;
      }
      for (int i = _offset + start; i >= _offset; --i) {
        if (_value[i] == c) {
          return i - _offset;
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
        char[] target = subString.value;
        int subOffset = subString.offset;
        char firstChar = target[subOffset];
        int end = subOffset + subCount;
        while (true) {
          int i = lastIndexOf(firstChar, start);
          if (i == -1) {
            return -1;
          }
          int o1 = offset + i, o2 = subOffset;
          while (++o2 < end && value[++o1] == target[o2]) {
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
    if (string == null) {
      throw new NullPointerException("string == null");
    }
    if (start < 0 || string.count - start < length) {
      return false;
    }
    if (thisStart < 0 || count - thisStart < length) {
      return false;
    }
    if (length <= 0) {
      return true;
    }
    int o1 = offset + thisStart, o2 = string.offset + start;
    char[] value1 = value;
    char[] value2 = string.value;
    for (int i = 0; i < length; ++i) {
      if (value1[o1 + i] != value2[o2 + i]) {
        return false;
      }
    }
    return true;
  }

  public boolean regionMatches(
      boolean ignoreCase, int thisStart, String string, int start, int length) {
    if (!ignoreCase) {
      return regionMatches(thisStart, string, start, length);
    }
    if (string == null) {
      throw new NullPointerException("string == null");
    }
    if (thisStart < 0 || length > count - thisStart) {
      return false;
    }
    if (start < 0 || length > string.count - start) {
      return false;
    }
    thisStart += offset;
    start += string.offset;
    int end = thisStart + length;
    char[] target = string.value;
    while (thisStart < end) {
      char c1 = value[thisStart++];
      char c2 = target[start++];
      if (c1 != c2 && foldCase(c1) != foldCase(c2)) {
        return false;
      }
    }
    return true;
  }

  public String replace(char oldChar, char newChar) {
    char[] buffer = value;
    int _offset = offset;
    int _count = count;
    int idx = _offset;
    int last = _offset + _count;
    boolean copied = false;
    while (idx < last) {
      if (buffer[idx] == oldChar) {
        if (!copied) {
          char[] newBuffer = new char[_count];
          System.arraycopy(buffer, _offset, newBuffer, 0, _count);
          buffer = newBuffer;
          idx -= _offset;
          last -= _offset;
          copied = true;
        }
        buffer[idx] = newChar;
      }
      idx++;
    }
    return copied ? new String(0, count, buffer) : this;
  }

  public String replace(CharSequence target, CharSequence replacement) {
    if (target == null) {
      throw new NullPointerException("target == null");
    }
    if (replacement == null) {
      throw new NullPointerException("replacement == null");
    }
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
    return CaseMapper.toLowerCase(Locale.getDefault(), this, value, offset, count);
  }

  public String toLowerCase(Locale locale) {
    return CaseMapper.toLowerCase(locale, this, value, offset, count);
  }

  @Override
  public String toString() {
    return this;
  }

  public String toUpperCase() {
    return CaseMapper.toUpperCase(Locale.getDefault(), this, value, offset, count);
  }

  public String toUpperCase(Locale locale) {
    return CaseMapper.toUpperCase(locale, this, value, offset, count);
  }

  public String trim() {
    int start = offset, last = offset + count - 1;
    int end = last;
    while ((start <= end) && (value[start] <= ' ')) {
      start++;
    }
    while ((end >= start) && (value[end] <= ' ')) {
      end--;
    }
    if (start == offset && end == last) {
      return this;
    }
    return new String(start, end - start + 1, value);
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
      s = new String(value, 1, ASCII);
    } else {
      s = new String(0, 1, new char[] {value});
    }
    s.hashCode = value;
    return s;
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
    return new String(0, 0, array);
  }

  static String fromCodePoint(int codePoint) {
    return valueOf(Character.toChars(codePoint));
  }

  public boolean contentEquals(StringBuffer strbuf) {
    synchronized (strbuf) {
      int size = strbuf.length();
      if (count != size) {
        return false;
      }
      return regionMatches(0, new String(0, size, strbuf.getValue()), 0, size);
    }
  }

  public boolean contentEquals(CharSequence cs) {
    if (cs == null) {
      throw new NullPointerException("cs == null");
    }
    int len = cs.length();
    if (len != count) {
      return false;
    }
    if (len == 0 && count == 0) {
      return true; // since both are empty strings
    }
    return regionMatches(0, cs.toString(), 0, len);
  }

  public boolean matches(String regularExpression) {
    throw new RuntimeException("No regex support yet");
  }

  public String replaceAll(String regularExpression, String replacement) {
    throw new RuntimeException("No regex support yet");
  }

  public String replaceFirst(String regularExpression, String replacement) {
    throw new RuntimeException("No regex support yet");
  }

  public String[] split(String regularExpression) {
    throw new RuntimeException("No regex support yet");
  }

  public String[] split(String regularExpression, int limit) {
    throw new RuntimeException("No regex support yet");
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
    if (cs == null) {
      throw new NullPointerException("cs == null");
    }
    return indexOf(cs.toString()) >= 0;
  }

  public int offsetByCodePoints(int index, int codePointOffset) {
    int s = index + offset;
    int r = Character.offsetByCodePoints(value, offset, count, s, codePointOffset);
    return r - offset;
  }
}
