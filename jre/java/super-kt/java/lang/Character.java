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

import javaemul.internal.annotations.KtName;
import javaemul.internal.annotations.KtNative;

// TODO(b/223774683): Java Character should implement Serializable. Kotlin Char doesn't.
@KtNative("kotlin.Char")
public final class Character implements Comparable<Character> {
  public static /* final */ char MIN_VALUE;

  public static /* final */ char MAX_VALUE;

  public static /* final */ int MIN_RADIX;

  public static /* final */ int MAX_RADIX;

  public static /* final */ Class<Character> TYPE;

  public static /* final */ byte UNASSIGNED;

  public static /* final */ byte UPPERCASE_LETTER;

  public static /* final */ byte LOWERCASE_LETTER;

  public static /* final */ byte TITLECASE_LETTER;

  public static /* final */ byte MODIFIER_LETTER;

  public static /* final */ byte OTHER_LETTER;

  public static /* final */ byte NON_SPACING_MARK;

  public static /* final */ byte ENCLOSING_MARK;

  public static /* final */ byte COMBINING_SPACING_MARK;

  public static /* final */ byte DECIMAL_DIGIT_NUMBER;

  public static /* final */ byte LETTER_NUMBER;

  public static /* final */ byte OTHER_NUMBER;

  public static /* final */ byte SPACE_SEPARATOR;

  public static /* final */ byte LINE_SEPARATOR;

  public static /* final */ byte PARAGRAPH_SEPARATOR;

  public static /* final */ byte CONTROL;

  public static /* final */ byte FORMAT;

  public static /* final */ byte PRIVATE_USE;

  public static /* final */ byte SURROGATE;

  public static /* final */ byte DASH_PUNCTUATION;

  public static /* final */ byte START_PUNCTUATION;

  public static /* final */ byte END_PUNCTUATION;

  public static /* final */ byte CONNECTOR_PUNCTUATION;

  public static /* final */ byte OTHER_PUNCTUATION;

  public static /* final */ byte MATH_SYMBOL;

  public static /* final */ byte CURRENCY_SYMBOL;

  public static /* final */ byte MODIFIER_SYMBOL;

  public static /* final */ byte OTHER_SYMBOL;

  public static /* final */ byte INITIAL_QUOTE_PUNCTUATION;

  public static /* final */ byte FINAL_QUOTE_PUNCTUATION;

  public static /* final */ byte DIRECTIONALITY_UNDEFINED;

  public static /* final */ byte DIRECTIONALITY_LEFT_TO_RIGHT;

  public static /* final */ byte DIRECTIONALITY_RIGHT_TO_LEFT;

  public static /* final */ byte DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;

  public static /* final */ byte DIRECTIONALITY_EUROPEAN_NUMBER;

  public static /* final */ byte DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR;

  public static /* final */ byte DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR;

  public static /* final */ byte DIRECTIONALITY_ARABIC_NUMBER;

  public static /* final */ byte DIRECTIONALITY_COMMON_NUMBER_SEPARATOR;

  public static /* final */ byte DIRECTIONALITY_NONSPACING_MARK;

  public static /* final */ byte DIRECTIONALITY_BOUNDARY_NEUTRAL;

  public static /* final */ byte DIRECTIONALITY_PARAGRAPH_SEPARATOR;

  public static /* final */ byte DIRECTIONALITY_SEGMENT_SEPARATOR;

  public static /* final */ byte DIRECTIONALITY_WHITESPACE;

  public static /* final */ byte DIRECTIONALITY_OTHER_NEUTRALS;

  public static /* final */ byte DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING;

  public static /* final */ byte DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE;

  public static /* final */ byte DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING;

  public static /* final */ byte DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE;

  public static /* final */ byte DIRECTIONALITY_POP_DIRECTIONAL_FORMAT;

  public static /* final */ char MIN_HIGH_SURROGATE;

  public static /* final */ char MAX_HIGH_SURROGATE;

  public static /* final */ char MIN_LOW_SURROGATE;

  public static /* final */ char MAX_LOW_SURROGATE;

  public static /* final */ char MIN_SURROGATE;

  public static /* final */ char MAX_SURROGATE;

  public static /* final */ int MIN_SUPPLEMENTARY_CODE_POINT;

  public static /* final */ int MIN_CODE_POINT;

  public static /* final */ int MAX_CODE_POINT;

  @KtName("SIZE_BITS")
  public static /* final */ int SIZE;

  // J2KT removed: Subset, UnicodeBlock

  public Character(char value) {}

  @KtName("toChar")
  public native char charValue();

  public native int compareTo(Character c);

  public static native int compare(char lhs, char rhs);

  public static native Character valueOf(char c);

  public static native boolean isValidCodePoint(int codePoint);

  public static native boolean isSupplementaryCodePoint(int codePoint);

  public static native boolean isHighSurrogate(char ch);

  public static native boolean isLowSurrogate(char ch);

  public static native boolean isSurrogate(char ch);

  public static native boolean isSurrogatePair(char high, char low);

  public static native int charCount(int codePoint);

  public static native int toCodePoint(char high, char low);

  public static native int codePointAt(CharSequence seq, int index);

  public static native int codePointAt(char[] seq, int index);

  public static native int codePointAt(char[] seq, int index, int limit);

  public static native int codePointBefore(CharSequence seq, int index);

  public static native int codePointBefore(char[] seq, int index);

  public static native int codePointBefore(char[] seq, int index, int start);

  public static native int toChars(int codePoint, char[] dst, int dstIndex);

  public static native char[] toChars(int codePoint);

  public static native int codePointCount(CharSequence seq, int beginIndex, int endIndex);

  public static native int codePointCount(char[] seq, int offset, int count);

  public static native int offsetByCodePoints(CharSequence seq, int index, int codePointOffset);

  public static native int offsetByCodePoints(
      char[] seq, int start, int count, int index, int codePointOffset);

  public static native int digit(char c, int radix);

  public static native int digit(int codePoint, int radix);

  @Override
  public native boolean equals(Object object);

  public static native char forDigit(int digit, int radix);

  public static native String getName(int codePoint);

  public static native int getNumericValue(char c);

  public static native int getNumericValue(int codePoint);

  public static native int getType(char c);

  public static native int getType(int codePoint);

  public static native byte getDirectionality(char c);

  public static native byte getDirectionality(int codePoint);

  public static native boolean isMirrored(char c);

  public static native boolean isMirrored(int codePoint);

  @Override
  public native int hashCode();

  public static native char highSurrogate(int codePoint);

  public static native char lowSurrogate(int codePoint);

  public static native boolean isAlphabetic(int codePoint);

  public static native boolean isBmpCodePoint(int codePoint);

  public static native boolean isDefined(char c);

  public static native boolean isDefined(int codePoint);

  public static native boolean isDigit(char c);

  public static native boolean isDigit(int codePoint);

  public static native boolean isIdentifierIgnorable(char c);

  public static native boolean isIdeographic(int codePoint);

  public static native boolean isIdentifierIgnorable(int codePoint);

  public static native boolean isISOControl(char c);

  public static native boolean isISOControl(int c);

  public static native boolean isJavaIdentifierPart(char c);

  public static native boolean isJavaIdentifierPart(int codePoint);

  public static native boolean isJavaIdentifierStart(char c);

  public static native boolean isJavaIdentifierStart(int codePoint);

  @Deprecated
  public static native boolean isJavaLetter(char c);

  @Deprecated
  public static native boolean isJavaLetterOrDigit(char c);

  public static native boolean isLetter(char c);

  public static native boolean isLetter(int codePoint);

  public static native boolean isLetterOrDigit(char c);

  public static native boolean isLetterOrDigit(int codePoint);

  public static native boolean isLowerCase(char c);

  public static native boolean isLowerCase(int codePoint);

  @Deprecated
  public static native boolean isSpace(char c);

  public static native boolean isSpaceChar(char c);

  public static native boolean isSpaceChar(int codePoint);

  public static native boolean isTitleCase(char c);

  public static native boolean isTitleCase(int codePoint);

  public static native boolean isUnicodeIdentifierPart(char c);

  public static native boolean isUnicodeIdentifierPart(int codePoint);

  public static native boolean isUnicodeIdentifierStart(char c);

  public static native boolean isUnicodeIdentifierStart(int codePoint);

  public static native boolean isUpperCase(char c);

  public static native boolean isUpperCase(int codePoint);

  public static native boolean isWhitespace(char c);

  public static native boolean isWhitespace(int codePoint);

  public static native char reverseBytes(char c);

  public static native char toLowerCase(char c);

  public static native int toLowerCase(int codePoint);

  @Override
  public native String toString();

  public static native String toString(char value);

  public static native char toTitleCase(char c);

  public static native int toTitleCase(int codePoint);

  public static native char toUpperCase(char c);

  public static native int toUpperCase(int codePoint);

  public static native int hashCode(char c);
}
