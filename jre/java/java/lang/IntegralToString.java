/*
 * Copyright 2021 Google Inc.
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

import javaemul.internal.JsUtils;

/** Converts integral types to strings. */
final class IntegralToString {

  public static String intToBinaryString(int value) {
    return intToUnsignedString(value, 2);
  }

  public static String intToHexString(int value) {
    return intToUnsignedString(value, 16);
  }

  public static String intToOctalString(int value) {
    return intToUnsignedString(value, 8);
  }

  private static String intToUnsignedString(int value, int radix) {
    return JsUtils.uintToString(value, radix);
  }

  public static String intToString(int value) {
    return "" + value;
  }

  public static String intToString(int value, int radix) {
    return JsUtils.intToString(value, radix);
  }

  public static String longToBinaryString(long value) {
    return toPowerOfTwoUnsignedString(value, 1);
  }

  public static String longToHexString(long value) {
    return toPowerOfTwoUnsignedString(value, 4);
  }

  public static String longToOctalString(long value) {
    return toPowerOfTwoUnsignedString(value, 3);
  }

  private static String toPowerOfTwoUnsignedString(long value, int shift) {
    final int radix = 1 << shift;
    if (Integer.MIN_VALUE <= value && value <= Integer.MAX_VALUE) {
      return Integer.toString((int) value, radix);
    }

    final int mask = radix - 1;
    final int bufSize = 64 / shift + 1;
    char[] buf = new char[bufSize];
    int pos = bufSize;
    do {
      buf[--pos] = Character.forDigit(((int) value) & mask);
      value >>>= shift;
    } while (value != 0);

    return String.valueOf(buf, pos, bufSize - pos);
  }

  public static String longToString(long value) {
    return "" + value;
  }

  public static String longToString(long value, int intRadix) {
    if (intRadix == 10 || intRadix < Character.MIN_RADIX || intRadix > Character.MAX_RADIX) {
      return String.valueOf(value);
    }

    int intValue = (int) value;
    if (intValue == value) {
      return Integer.toString(intValue, intRadix);
    }

    /*
     * If v is positive, negate it. This is the opposite of what one might expect. It is necessary
     * because the range of the negative values is strictly larger than that of the positive values:
     * there is no positive value corresponding to Long.MIN_VALUE.
     */
    boolean negative = value < 0;
    if (!negative) {
      value = -value;
    }

    int bufLen = intRadix < 8 ? 65 : 23; // Max chars in result (conservative)
    char[] buf = new char[bufLen];
    int cursor = bufLen;

    // Convert radix to long before hand to avoid costly conversion on each iteration.
    long radix = intRadix;
    do {
      long q = value / radix;
      buf[--cursor] = Character.forDigit((int) (radix * q - value));
      value = q;
    } while (value != 0);

    if (negative) {
      buf[--cursor] = '-';
    }

    return String.valueOf(buf, cursor, bufLen - cursor);
  }
}
