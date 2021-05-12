/*
 * Copyright 2007 Google Inc.
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

import java.io.Serializable;
import javaemul.internal.JsUtils;
import javaemul.internal.NativeRegExp;

/**
 * Abstract base class for numeric wrapper classes.
 */
public abstract class Number implements Serializable {

  /**
   * Stores a regular expression object to verify the format of float values.
   */
  private static NativeRegExp floatRegex;

  // CHECKSTYLE_OFF: A special need to use unusual identifiers to avoid
  // introducing name collisions.

  static class __Decode {
    public final String payload;
    public final int radix;

    public __Decode(int radix, String payload) {
      this.radix = radix;
      this.payload = payload;
    }
  }

  /**
   * @skip
   *
   * This function will determine the radix that the string is expressed in
   * based on the parsing rules defined in the Javadocs for Integer.decode() and
   * invoke __parseAndValidateInt.
   */
  protected static int __decodeAndValidateInt(String s, int lowerBound,
      int upperBound) throws NumberFormatException {
    __Decode decode = __decodeNumberString(s);
    return __parseAndValidateInt(decode.payload, decode.radix, lowerBound,
        upperBound);
  }

  protected static __Decode __decodeNumberString(String s) {
    final boolean negative;
    if (s.startsWith("-")) {
      negative = true;
      s = s.substring(1);
    } else {
      negative = false;
      if (s.startsWith("+")) {
        s = s.substring(1);
      }
    }

    final int radix;
    if (s.startsWith("0x") || s.startsWith("0X")) {
      s = s.substring(2);
      radix = 16;
    } else if (s.startsWith("#")) {
      s = s.substring(1);
      radix = 16;
    } else if (s.startsWith("0")) {
      radix = 8;
    } else {
      radix = 10;
    }

    if (negative) {
      s = "-" + s;
    }
    return new __Decode(radix, s);
  }

  /**
   * @skip
   *
   * This function contains common logic for parsing a String as a floating-
   * point number and validating the range.
   */
  protected static double __parseAndValidateDouble(String s) throws NumberFormatException {
    if (!__isValidDouble(s)) {
      throw NumberFormatException.forInputString(s);
    }
    return JsUtils.parseFloat(s);
  }

  /**
   * @skip This function contains common logic for parsing a String in a given radix and validating
   *     the result.
   */
  protected static int __parseAndValidateInt(String s, int radix, int lowerBound, int upperBound)
      throws NumberFormatException {
    if (s == null || s.isEmpty()) {
      throw NumberFormatException.forInputString(s);
    }
    if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
      throw NumberFormatException.forRadix(radix);
    }

    char c = s.charAt(0);
    boolean negative = c == '-';
    int startIndex = negative || c == '+' ? 1 : 0;

    return parseInt(s, startIndex, lowerBound, radix, negative);
  }

  private static int parseInt(String string, int offset, int minValue, int radix, boolean negative)
      throws NumberFormatException {
    // Note that we are accumulating result as negative values to handle edge case around MIN_VALUE.
    int result = 0;
    int maxDivByRadix = minValue / radix;
    int length = string.length();
    while (offset < length) {
      int digit = Character.digit(string.charAt(offset++), radix);
      // Check if a valid digit or if multiplying by radixPower will overflow
      if (digit == -1 || maxDivByRadix > result) {
        throw NumberFormatException.forInputString(string);
      }
      int next = result * radix - digit;
      // Check again if we have overflowed due to the subtraction of the digit.
      if (next > result) {
        throw NumberFormatException.forInputString(string);
      }
      result = next;
    }

    if (!negative) {
      result = -result;
      // A negative value means we've overflowed.
      if (result < 0) {
        throw NumberFormatException.forInputString(string);
      }
    }

    return result;
  }

  /**
   * @skip This function contains common logic for parsing a String in a given radix and validating
   *     the result.
   */
  protected static long __parseAndValidateLong(String s, int radix) throws NumberFormatException {
    if (s == null || s.isEmpty()) {
      throw NumberFormatException.forInputString(s);
    }
    if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
      throw NumberFormatException.forRadix(radix);
    }

    char c = s.charAt(0);
    boolean negative = c == '-';
    int startIndex = negative || c == '+' ? 1 : 0;

    return parseLong(s, startIndex, radix, negative);
  }

  private static long parseLong(String string, int offset, int radix, boolean negative)
      throws NumberFormatException {
    // Note that we are accumulating result as negative values to handle edge case around MIN_VALUE.
    long result = 0;
    long maxDivByRadix = Long.MIN_VALUE / radix;
    int length = string.length();
    while (offset < length) {
      int digit = Character.digit(string.charAt(offset++), radix);
      // Check if a valid digit or if multiplying by radixPower will overflow
      if (digit == -1 || maxDivByRadix > result) {
        throw NumberFormatException.forInputString(string);
      }
      long next = result * radix - digit;
      // Check again if we have overflowed due to the subtraction of the digit.
      if (next > result) {
        throw NumberFormatException.forInputString(string);
      }
      result = next;
    }

    if (!negative) {
      result = -result;
      // A negative value means we've overflowed.
      if (result < 0) {
        throw NumberFormatException.forInputString(string);
      }
    }

    return result;
  }

  /**
   * @skip
   *
   * @param str
   * @return {@code true} if the string matches the float format, {@code false} otherwise
   */
  private static boolean __isValidDouble(String str) {
    if (floatRegex == null) {
      floatRegex =
          new NativeRegExp(
              "^\\s*[+-]?(NaN|Infinity|((\\d+\\.?\\d*)|(\\.\\d+))([eE][+-]?\\d+)?[dDfF]?)\\s*$");
    }
    return floatRegex.test(str);
  }

  // CHECKSTYLE_ON

  public byte byteValue() {
    return (byte) intValue();
  }

  public abstract double doubleValue();

  public abstract float floatValue();

  public abstract int intValue();

  public abstract long longValue();

  public short shortValue() {
    return (short) intValue();
  }
}
