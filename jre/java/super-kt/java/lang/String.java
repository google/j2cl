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

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.Locale;
import javaemul.internal.annotations.KtNative;
import javaemul.internal.annotations.KtProperty;

// TODO(b/223774683): Java String should implement Serializable. Kotlin String doesn't.
@KtNative("kotlin.String")
public final class String implements Comparable<String>, CharSequence {

  public static /* final */ Comparator<String> CASE_INSENSITIVE_ORDER;

  public String() {}

  public String(byte[] data) {}

  @Deprecated
  public String(byte[] data, int high) {}

  public String(byte[] data, int offset, int byteCount) {}

  @Deprecated
  public String(byte[] data, int high, int offset, int byteCount) {}

  public String(byte[] data, int offset, int byteCount, String charsetName)
      throws UnsupportedEncodingException {}

  public String(byte[] data, String charsetName) throws UnsupportedEncodingException {}

  public String(byte[] data, int offset, int byteCount, Charset charset) {}

  public String(byte[] data, Charset charset) {}

  public String(char[] data) {}

  public String(char[] data, int offset, int charCount) {}

  public String(String toCopy) {}

  public String(StringBuffer stringBuffer) {}

  public String(int[] codePoints, int offset, int count) {}

  public String(StringBuilder stringBuilder) {}

  @Override
  public native char charAt(int index);

  public native int compareTo(String string);

  public native int compareToIgnoreCase(String string);

  public native String concat(String string);

  public static native String copyValueOf(char[] data);

  public static native String copyValueOf(char[] data, int start, int length);

  public native boolean endsWith(String suffix);

  @Override
  public native boolean equals(Object object);

  public native boolean equalsIgnoreCase(String string);

  @Deprecated
  public native void getBytes(int start, int end, byte[] data, int index);

  public native byte[] getBytes();

  public native byte[] getBytes(String charsetName) throws UnsupportedEncodingException;

  public native byte[] getBytes(Charset charset);

  public native void getChars(int start, int end, char[] buffer, int index);

  @Override
  public native int hashCode();

  public native int indexOf(int c);

  public native int indexOf(int c, int start);

  public native int indexOf(String string);

  public native int indexOf(String subString, int start);

  // TODO(b/222269323): Revisit.
  public native String intern();

  public native boolean isEmpty();

  public native int lastIndexOf(int c);

  public native int lastIndexOf(int c, int start);

  public native int lastIndexOf(String string);

  public native int lastIndexOf(String subString, int start);

  @KtProperty
  public native int length();

  public native boolean regionMatches(int thisStart, String string, int start, int length);

  public native boolean regionMatches(
      boolean ignoreCase, int thisStart, String string, int start, int length);

  public native String replace(char oldChar, char newChar);

  // TODO(b/222269323): Different regexp/literal interpretation of `target`
  // public native String replace(CharSequence target, CharSequence replacement);

  public native boolean startsWith(String prefix);

  public native boolean startsWith(String prefix, int start);

  public native String substring(int start);

  public native String substring(int start, int end);

  public native char[] toCharArray();

  public native String toLowerCase();

  public native String toLowerCase(Locale locale);

  @Override
  public native String toString();

  public native String toUpperCase();

  public native String toUpperCase(Locale locale);

  public native String trim();

  public static native String valueOf(char[] data);

  public static native String valueOf(char[] data, int start, int length);

  public static native String valueOf(char value);

  public static native String valueOf(double value);

  public static native String valueOf(float value);

  public static native String valueOf(int value);

  public static native String valueOf(long value);

  public static native String valueOf(Object value);

  public static native String valueOf(boolean value);

  public native boolean contentEquals(StringBuffer strbuf);

  public native boolean contentEquals(CharSequence cs);

  public native boolean matches(String regularExpression);

  public native String replaceAll(String regularExpression, String replacement);

  public native String replaceFirst(String regularExpression, String replacement);

  public native String[] split(String regularExpression);

  public native String[] split(String regularExpression, int limit);

  @Override
  public native CharSequence subSequence(int start, int end);

  public native int codePointAt(int index);

  public native int codePointBefore(int index);

  public native int codePointCount(int start, int end);

  public native boolean contains(CharSequence cs);

  public native int offsetByCodePoints(int index, int codePointOffset);

  public static native String format(String format, Object... args);

  public static native String format(Locale locale, String format, Object... args);
}
