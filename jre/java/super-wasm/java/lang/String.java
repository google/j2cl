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
import static javaemul.internal.InternalPreconditions.checkCriticalStringBounds;
import static javaemul.internal.InternalPreconditions.checkNotNull;
import static javaemul.internal.InternalPreconditions.checkPositionIndexes;
import static javaemul.internal.InternalPreconditions.checkStringBounds;
import static javaemul.internal.InternalPreconditions.checkStringElementIndex;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Comparator;
import java.util.Locale;
import java.util.StringJoiner;
import javaemul.internal.ArrayHelper;
import javaemul.internal.EmulatedCharset;
import javaemul.internal.NativeRegExp;
import javaemul.internal.StringUtil;
import javaemul.internal.annotations.Wasm;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsNonNull;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/** Intrinsic string class. */
public final class String implements Comparable<String>, CharSequence, Serializable {

  /* TODO(jat): consider whether we want to support the following methods;
   *
   * <ul>
   * <li>deprecated methods dealing with bytes (I assume not since I can't see
   * much use for them)
   * <ul>
   * <li>String(byte[] ascii, int hibyte)
   * <li>String(byte[] ascii, int hibyte, int offset, int count)
   * <li>getBytes(int srcBegin, int srcEnd, byte[] dst, int dstBegin)
   * </ul>
   * <li>methods which in JS will essentially do nothing or be the same as other
   * methods
   * <ul>
   * <li>copyValueOf(char[] data)
   * <li>copyValueOf(char[] data, int offset, int count)
   * </ul>
   * <li>methods added in Java 1.6 (the issue is how will it impact users
   * building against Java 1.5)
   * <ul>
   * <li>isEmpty()
   * </ul>
   * <li>other methods which are not straightforward in JS
   * <ul>
   * <li>format(String format, Object... args)
   * </ul>
   * </ul>
   *
   * <p>Also, in general, we need to improve our support of non-ASCII characters. The
   * problem is that correct support requires large tables, and we don't want to
   * make users who aren't going to use that pay for it. There are two ways to do
   * that:
   * <ol>
   * <li>construct the tables in such a way that if the corresponding method is
   * not called the table will be elided from the output.
   * <li>provide a deferred binding target selecting the level of compatibility
   * required. Those that only need ASCII (or perhaps a different relatively small
   * subset such as Latin1-5) will not pay for large tables, even if they do call
   * toLowercase(), for example.
   * </ol>
   *
   * Also, if we ever add multi-locale support, there are a number of other
   * methods such as toLowercase(Locale) we will want to consider supporting. This
   * is probably rare, but there will be some apps (such as a translation tool)
   * which cannot be written without this support.
   *
   * Another category of incomplete support is that we currently just use the JS
   * regex support, which is not exactly the same as Java. We should support Java
   * syntax by mapping it into equivalent JS patterns, or emulating them.
   *
   * IMPORTANT NOTE: if newer JREs add new interfaces to String, please update
   * {@link Devirtualizer} and {@link JavaResourceBase}
   */
  public static final Comparator<String> CASE_INSENSITIVE_ORDER =
      new Comparator<String>() {
        @Override
        public int compare(String a, String b) {
          return a.compareToIgnoreCase(b);
        }
      };

  public static String copyValueOf(char[] v) {
    return valueOf(v);
  }

  public static String copyValueOf(char[] v, int offset, int count) {
    return valueOf(v, offset, count);
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

  public static String valueOf(boolean value) {
    return value ? "true" : "false";
  }

  public static String valueOf(char x) {
    return new String(nativeFromCodePoint(x));
  }

  public static String valueOf(char[] x, int offset, int count) {
    return new String(x, offset, count);
  }

  public static String valueOf(char[] x) {
    return new String(x);
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

  // valueOf needs to be treated special:
  // J2cl uses it for String concat and thus it can not use string concatenation itself.
  public static @JsNonNull String valueOf(Object x) {
    return x == null ? "null" : x.toString();
  }

  /**
   * This method converts Java-escaped dollar signs "\$" into JavaScript-escaped dollar signs "$$",
   * and removes all other lone backslashes, which serve as escapes in Java but are passed through
   * literally in JavaScript.
   *
   * @skip
   */
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

  private static Charset getCharset(String charsetName) throws UnsupportedEncodingException {
    try {
      return Charset.forName(charsetName);
    } catch (UnsupportedCharsetException e) {
      throw new UnsupportedEncodingException(charsetName);
    }
  }

  static String fromCodePoint(int x) {
    return new String(nativeFromCodePoint(x));
  }

  private final NativeString value;

  String(NativeString value) {
    // Ensure value can never be null to avoid hard to debug issues. This also helps binaryen to
    // make the field non-null.
    this.value = asNonNull(value);
  }

  @Wasm("ref.as_non_null")
  private static native NativeString asNonNull(NativeString ref);

  public String() {
    this.value = "".value;
  }

  public String(byte[] bytes) {
    this.value = createImpl(bytes, EmulatedCharset.UTF_8);
  }

  public String(byte[] bytes, int ofs, int len) {
    this.value = createImpl(bytes, ofs, len, EmulatedCharset.UTF_8);
  }

  public String(byte[] bytes, int ofs, int len, String charsetName)
      throws UnsupportedEncodingException {
    this.value = createImpl(bytes, ofs, len, getCharset(charsetName));
  }

  public String(byte[] bytes, int ofs, int len, Charset charset) {
    this.value = createImpl(bytes, ofs, len, charset);
  }

  public String(byte[] bytes, String charsetName) throws UnsupportedEncodingException {
    this.value = createImpl(bytes, getCharset(charsetName));
  }

  public String(byte[] bytes, Charset charset) {
    this.value = createImpl(bytes, charset);
  }

  public String(char[] x) {
    this.value = nativeFromCharCodeArray(x, 0, x.length);
  }

  public String(char[] x, int offset, int count) {
    this.value = createImpl(x, offset, count);
  }

  public String(int[] codePoints, int offset, int count) {
    this.value = createImpl(codePoints, offset, count);
  }

  private static NativeString createImpl(byte[] bytes, Charset charset) {
    return createImpl(bytes, 0, bytes.length, charset);
  }

  private static NativeString createImpl(byte[] bytes, int ofs, int len, Charset charset) {
    checkPositionIndexes(ofs, ofs + len, bytes.length);
    return String.valueOf(((EmulatedCharset) charset).decodeString(bytes, ofs, len)).value;
  }

  private static NativeString createImpl(char[] x, int offset, int count) {
    int end = offset + count;
    checkStringBounds(offset, end, x.length);
    return nativeFromCharCodeArray(x, offset, end);
  }

  private static NativeString createImpl(int[] codePoints, int offset, int count) {
    char[] chars = new char[count * 2];
    int charIdx = 0;
    while (count-- > 0) {
      charIdx += Character.toChars(codePoints[offset++], chars, charIdx);
    }
    return String.valueOf(chars, 0, charIdx).value;
  }

  public String(String other) {
    this.value = other.value;
  }

  public String(StringBuffer sb) {
    this.value = sb.toString().value;
  }

  public String(StringBuilder sb) {
    this.value = sb.toString().value;
  }

  @Override
  public char charAt(int index) {
    checkStringElementIndex(index, length());
    return nativeCharCodeAt(asStringView(value), index);
  }

  public int codePointAt(int index) {
    return Character.codePointAt(this, index, length());
  }

  public int codePointBefore(int index) {
    return Character.codePointBefore(this, index, 0);
  }

  public int codePointCount(int beginIndex, int endIndex) {
    return Character.codePointCount(this, beginIndex, endIndex);
  }

  @Override
  public int compareTo(String other) {
    return nativeCompareTo(value, other.value);
  }

  public int compareToIgnoreCase(String other) {
    if (other == this) {
      return 0;
    }

    int end = Math.min(length(), other.length());
    for (int i = 0; i < end; i++) {
      char c1 = charAt(i);
      char c2 = other.charAt(i);
      if (c1 != c2) {
        if (c1 > 127 || c2 > 127) {
          // Branch into native implementation since we cannot handle case folding for non-ascii
          // chars.
          return nativeCompareToIgnoreCase(
              nativeSubstr(asStringView(value), i, length()),
              nativeSubstr(asStringView(other.value), i, other.length()));
        }

        int result = foldCaseAscii(c1) - foldCaseAscii(c2);
        if (result != 0) {
          return result;
        }
      }
    }
    return length() - other.length();
  }

  public String concat(String str) {
    return new String(nativeConcat(value, str.value));
  }

  public boolean contains(CharSequence s) {
    return indexOf(s.toString()) != -1;
  }

  public boolean contentEquals(CharSequence cs) {
    return equals(cs.toString());
  }

  public boolean contentEquals(StringBuffer sb) {
    return equals(sb.toString());
  }

  public boolean endsWith(String suffix) {
    // If IE8 supported negative start index, we could have just used "-suffixlength".
    int suffixlength = suffix.length();
    int length = length();
    return new String(nativeSubstr(asStringView(value), length - suffixlength, length))
        .equals(suffix);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    return other instanceof String && equals((String) other);
  }

  private boolean equals(String other) {
    if (other == null) {
      return false;
    }
    return nativeEq(value, other.value);
  }

  public boolean equalsIgnoreCase(String other) {
    if (other == this) {
      return true;
    }
    if (other == null) {
      return false;
    }
    int length = length();
    if (length != other.length()) {
      return false;
    }

    for (int i = 0; i < length; i++) {
      char c1 = charAt(i);
      char c2 = other.charAt(i);
      if (c1 == c2) {
        continue;
      }
      if (c1 > 127 && c2 > 127) {
        // Branch into native implementation since we cannot handle case folding for non-ascii
        // chars.
        return nativeEqualsIgnoreCase(
            nativeSubstr(asStringView(value), i, length),
            nativeSubstr(asStringView(other.value), i, length));
      }
      if (foldCaseAscii(c1) != foldCaseAscii(c2)) {
        return false;
      }
    }
    return true;
  }

  private static char foldCaseAscii(char value) {
    if ('A' <= value && value <= 'Z') {
      return (char) (value + ('a' - 'A'));
    }
    return value;
  }

  public byte[] getBytes() {
    // default character set for GWT is UTF-8
    return getBytes(EmulatedCharset.UTF_8);
  }

  public byte[] getBytes(String charsetName) throws UnsupportedEncodingException {
    return getBytes(getCharset(charsetName));
  }

  public byte[] getBytes(Charset charset) {
    return ((EmulatedCharset) charset).getBytes(this);
  }

  public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
    checkCriticalStringBounds(srcBegin, srcEnd, length());
    checkCriticalStringBounds(dstBegin, dstBegin + (srcEnd - srcBegin), dst.length);
    getChars0(srcBegin, srcEnd, dst, dstBegin);
  }

  private void getChars0(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
    int unused = nativeGetChars(nativeSubstr(asStringView(value), srcBegin, srcEnd), dst, dstBegin);
  }

  private int hashCode;

  @Override
  public int hashCode() {
    int h = hashCode;
    if (h == 0) {
      int length = length();
      if (length == 0) {
        return 0;
      }
      for (int i = 0; i < length; i++) {
        // Following is the common hash function '(31 * h + x)' as '(x << 5) - x' equal to '31 * x'.
        h = (h << 5) - h + charAt(i);
      }
      hashCode = h;
    }
    return h;
  }

  public int indexOf(int codePoint) {
    return value.indexOf(nativeFromCodePoint(codePoint));
  }

  public int indexOf(int codePoint, int startIndex) {
    return value.indexOf(nativeFromCodePoint(codePoint), startIndex);
  }

  public int indexOf(String str) {
    return value.indexOf(str.value, 0);
  }

  public int indexOf(String str, int startIndex) {
    return value.indexOf(str.value, startIndex);
  }

  public boolean isEmpty() {
    return length() == 0;
  }

  public int lastIndexOf(int codePoint) {
    return value.lastIndexOf(nativeFromCodePoint(codePoint));
  }

  public int lastIndexOf(int codePoint, int startIndex) {
    return value.lastIndexOf(nativeFromCodePoint(codePoint), startIndex);
  }

  public int lastIndexOf(String str) {
    return value.lastIndexOf(str.value, Integer.MAX_VALUE);
  }

  public int lastIndexOf(String str, int start) {
    return value.lastIndexOf(str.value, start);
  }

  @Override
  public int length() {
    return nativeGetLength(asStringView(value));
  }

  /**
   * Regular expressions vary from the standard implementation. The <code>regex</code> parameter is
   * interpreted by JavaScript as a JavaScript regular expression. For consistency, use only the
   * subset of regular expression syntax common to both Java and JavaScript.
   *
   * <p>TODO(jat): properly handle Java regex syntax
   */
  public boolean matches(String regex) {
    // We surround the regex with '^' and '$' because it must match the entire string.
    return new NativeRegExp("^(" + regex + ")$").test(this);
  }

  public int offsetByCodePoints(int index, int codePointOffset) {
    return Character.offsetByCodePoints(this, index, codePointOffset);
  }

  public boolean regionMatches(
      boolean ignoreCase, int toffset, String other, int ooffset, int len) {
    checkNotNull(other);
    if (toffset < 0 || ooffset < 0) {
      return false;
    }
    if (toffset + len > length() || ooffset + len > other.length()) {
      return false;
    }
    if (len <= 0) {
      return true;
    }

    String left = new String(nativeSubstr(asStringView(value), toffset, toffset + len));
    String right = new String(nativeSubstr(asStringView(other.value), ooffset, ooffset + len));
    return ignoreCase ? left.equalsIgnoreCase(right) : left.equals(right);
  }

  public boolean regionMatches(int toffset, String other, int offset, int len) {
    return regionMatches(false, toffset, other, offset, len);
  }

  public String repeat(int count) {
    checkArgument(count >= 0);
    return new String(value.repeat(count));
  }

  public String replace(char from, char to) {
    return StringUtil.replace(this, from, to, /* ignoreCase= */ false);
  }

  public String replace(CharSequence from, CharSequence to) {
    return StringUtil.replace(this, from, to, /* ignoreCase= */ false);
  }

  /**
   * Regular expressions vary from the standard implementation. The <code>regex</code> parameter is
   * interpreted by JavaScript as a JavaScript regular expression. For consistency, use only the
   * subset of regular expression syntax common to both Java and JavaScript.
   *
   * <p>TODO(jat): properly handle Java regex syntax
   */
  public String replaceAll(String regex, String replace) {
    return StringUtil.replaceAll(this, regex, replace, /* ignoreCase= */ false);
  }

  /**
   * Regular expressions vary from the standard implementation. The <code>regex</code> parameter is
   * interpreted by JavaScript as a JavaScript regular expression. For consistency, use only the
   * subset of regular expression syntax common to both Java and JavaScript.
   *
   * <p>TODO(jat): properly handle Java regex syntax
   */
  public String replaceFirst(String regex, String replace) {
    return StringUtil.replaceFirst(this, regex, replace, /* ignoreCase= */ false);
  }

  // TODO: should live on a utility instead of the String API.
  public String nativeReplace(NativeRegExp regExp, char replacement) {
    return new String(value.replace(regExp, nativeFromCodePoint(replacement)));
  }

  // TODO: should live on a utility instead of the String API.
  public String nativeReplace(NativeRegExp regExp, String replacement) {
    return new String(value.replace(regExp, replacement.value));
  }

  /**
   * Regular expressions vary from the standard implementation. The <code>regex</code> parameter is
   * interpreted by JavaScript as a JavaScript regular expression. For consistency, use only the
   * subset of regular expression syntax common to both Java and JavaScript.
   */
  public String[] split(String regex) {
    return split(regex, 0);
  }

  /**
   * Regular expressions vary from the standard implementation. The <code>regex</code> parameter is
   * interpreted by JavaScript as a JavaScript regular expression. For consistency, use only the
   * subset of regular expression syntax common to both Java and JavaScript.
   *
   * <p>TODO(jat): properly handle Java regex syntax
   */
  public String[] split(String regex, int maxMatch) {
    // The compiled regular expression created from the string
    NativeRegExp compiled = new NativeRegExp(regex, "g");
    // the Javascipt array to hold the matches prior to conversion
    String[] out = new String[0];
    // count of split strings.
    int count = 0;
    // how many matches performed so far
    int matchCount = 0;
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
      if (count == out.length) {
        out = ArrayHelper.grow(out, count + 1);
      }
      if (matchObj == null || trail.isEmpty() || (matchCount == (maxMatch - 1) && maxMatch > 0)) {
        out[count++] = trail;
        break;
      } else {
        int matchIndex = matchObj.getIndex();
        out[count++] = trail.substring(0, matchIndex);
        trail = trail.substring(matchIndex + matchObj.getAt(0).length());
        // Force the compiled pattern to reset internal state
        compiled.setLastIndex(0);
        // Only one zero length match per character to ensure termination
        if (trail.equals(lastTrail)) {
          out[matchCount] = trail.substring(0, 1);
          trail = trail.substring(1);
        }
        lastTrail = trail;
        matchCount++;
      }
    }
    // all blank delimiters at the end are supposed to disappear if maxMatch == 0;
    // however, if the input string is empty, the output should consist of a
    // single empty string
    if (maxMatch == 0 && this.length() > 0) {
      while (count > 0 && out[count - 1].isEmpty()) {
        count--;
      }
    }
    return ArrayHelper.setLength(out, count);
  }

  public boolean startsWith(String prefix) {
    return startsWith(prefix, 0);
  }

  public boolean startsWith(String prefix, int toffset) {
    return toffset >= 0
        && new String(nativeSubstr(asStringView(value), toffset, toffset + prefix.length()))
            .equals(prefix);
  }

  @Override
  public CharSequence subSequence(int beginIndex, int endIndex) {
    return substring(beginIndex, endIndex);
  }

  public String substring(int beginIndex) {
    checkStringElementIndex(beginIndex, length() + 1);
    return new String(nativeSubstr(asStringView(value), beginIndex, length()));
  }

  public String substring(int beginIndex, int endIndex) {
    checkStringBounds(beginIndex, endIndex, length());
    return new String(nativeSubstr(asStringView(value), beginIndex, endIndex));
  }

  public char[] toCharArray() {
    int n = length();
    char[] charArr = new char[n];
    getChars0(0, n, charArr, 0);
    return charArr;
  }

  /**
   * Transforms the String to lower-case in a locale insensitive way.
   *
   * <p>Unlike JRE, we don't do locale specific transformation by default. That is backward
   * compatible for GWT and in most of the cases that is what the developer actually wants. If you
   * want to make a transformation based on native locale of the browser, you can do {@code
   * toLowerCase(Locale.getDefault())} instead.
   */
  public String toLowerCase() {
    return new String(value.toLowerCase());
  }

  /**
   * If provided {@code locale} is {@link Locale#getDefault()}, uses javascript's {@code
   * toLocaleLowerCase} to do a locale specific transformation. Otherwise, it will fallback to
   * {@code toLowerCase} which performs the right thing for the limited set of Locale's predefined
   * in GWT Locale emulation.
   */
  public String toLowerCase(Locale locale) {
    return locale == Locale.getDefault()
        ? new String(value.toLocaleLowerCase())
        : new String(value.toLowerCase());
  }

  // See the notes in lowerCase pair.
  public String toUpperCase() {
    return new String(value.toUpperCase());
  }

  // See the notes in lowerCase pair.
  public String toUpperCase(Locale locale) {
    return locale == Locale.getDefault()
        ? new String(value.toLocaleUpperCase())
        : new String(value.toUpperCase());
  }

  @Override
  public String toString() {
    return this;
  }

  public String trim() {
    int length = length();
    int start = 0;
    while (start < length && charAt(start) <= ' ') {
      start++;
    }
    int end = length;
    while (end > start && charAt(end - 1) <= ' ') {
      end--;
    }
    return start > 0 || end < length ? substring(start, end) : this;
  }

  // TODO(b/335375385): Replace with the concat instance method.
  static String concat(String str1, String str2) {
    return new String(nativeConcat(str1.value, str2.value));
  }

  // Needed to be able to pass a native wasm i32 array to a non native method.
  @Wasm("$char.array")
  private interface CharArrayRef {}

  static String fromNativeCharArray(CharArrayRef x, int length) {
    return new String(nativeFromCharCodeArray(x, 0, length));
  }

  static String fromJsString(NativeString o) {
    return o == null ? null : new String(o);
  }

  static NativeString toJsString(String string) {
    return string == null ? null : string.value;
  }

  NativeString toJsString() {
    return this.value;
  }

  /** Native JS compatible representation of a string. */
  @Wasm("string")
  @JsType(isNative = true, name = "string", namespace = JsPackage.GLOBAL)
  interface NativeString {

    int indexOf(NativeString str);

    int indexOf(NativeString str, int startIndex);

    int lastIndexOf(NativeString str);

    int lastIndexOf(NativeString str, int start);

    NativeString repeat(int count);

    NativeString replace(NativeRegExp regex, NativeString replace);

    NativeString toLocaleLowerCase();

    NativeString toLocaleUpperCase();

    NativeString toLowerCase();

    NativeString toUpperCase();
  }

  @Wasm("stringview_wtf16")
  private interface NativeStringView {}

  @Wasm("string.as_wtf16")
  private static native NativeStringView asStringView(NativeString stringView);

  @Wasm("string.from_code_point")
  private static native NativeString nativeFromCodePoint(int x);

  @Wasm("string.new_wtf16_array")
  private static native NativeString nativeFromCharCodeArray(char[] x, int start, int end);

  @Wasm("string.new_wtf16_array")
  private static native NativeString nativeFromCharCodeArray(CharArrayRef x, int start, int end);

  @Wasm("string.encode_wtf16_array")
  private static native int nativeGetChars(NativeString s, char[] x, int start);

  @Wasm("stringview_wtf16.length")
  private static native int nativeGetLength(NativeStringView stringView);

  @Wasm("stringview_wtf16.get_codeunit")
  private static native char nativeCharCodeAt(NativeStringView stringView, int index);

  @Wasm("stringview_wtf16.slice")
  private static native NativeString nativeSubstr(NativeStringView s, int beginIndex, int end);

  @Wasm("string.compare")
  private static native int nativeCompareTo(NativeString a, NativeString b);

  @JsMethod(namespace = "j2wasm.StringUtils", name = "compareToIgnoreCase")
  private static native int nativeCompareToIgnoreCase(NativeString a, NativeString b);

  @JsMethod(namespace = "j2wasm.StringUtils", name = "equalsIgnoreCase")
  private static native boolean nativeEqualsIgnoreCase(NativeString a, NativeString b);

  @Wasm("string.eq")
  private static native boolean nativeEq(NativeString a, NativeString b);

  @Wasm("string.concat")
  private static native NativeString nativeConcat(NativeString a, NativeString b);
}
