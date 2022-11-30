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
import javaemul.internal.LongUtils;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

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
    return numberToString(toDoubleFromUnsignedInt(value), radix);
  }

  public static String intToString(int value) {
    return "" + value;
  }

  public static String intToString(int value, int radix) {
    return numberToString(value, radix);
  }

  @JsMethod
  private static native double toDoubleFromUnsignedInt(int value);

  private static String numberToString(double value, int radix) {
    NativeNumber number = JsUtils.uncheckedCast(value);
    return number.toString(radix);
  }

  @JsType(isNative = true, name = "Number", namespace = JsPackage.GLOBAL)
  private interface NativeNumber {
    String toString(int radix);
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

    int highBits = LongUtils.getHighBits(value);
    if (highBits == 0) {
      return intToUnsignedString(LongUtils.getLowBits(value), radix);
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
    return LongUtils.toString(value, intRadix);
  }
}
