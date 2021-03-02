/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package javaemul.internal;

import jsinterop.annotations.JsType;

/**
 * Static Primitive helper. This class should only use int, long and double and avoid casts and
 * coercions.
 */
@JsType(namespace = "vmbootstrap")
public class Primitives {

  /** Narrows a number to a 8-bit signed number. */
  public static int toByte(int instance) {
    return instance << 24 >> 24;
  }

  /** Narrows a number to a 16-bit number. */
  public static int toChar(int instance) {
    return instance & 0xFFFF;
  }

  /** Narrows a number to a 16-bit signed number. */
  public static int toShort(int instance) {
    return instance << 16 >> 16;
  }

  /** Narrows a number to a 32-bit signed number. */
  public static int toInt(int instance) {
    return instance | 0;
  }

  public static int widenByteToChar(int instance) {
    return toChar(instance);
  }

  public static int narrowCharToByte(int instance) {
    return toByte(instance);
  }

  public static int narrowCharToShort(int instance) {
    return toShort(instance);
  }

  public static int narrowShortToByte(int instance) {
    return toByte(instance);
  }

  public static int narrowShortToChar(int instance) {
    return toChar(instance);
  }

  public static int narrowIntToByte(int instance) {
    return toByte(instance);
  }

  public static int narrowIntToChar(int instance) {
    return toChar(instance);
  }

  public static int narrowIntToShort(int instance) {
    return toShort(instance);
  }

  public static long widenByteToLong(int instance) {
    return LongUtils.fromInt(instance);
  }

  public static long widenCharToLong(int instance) {
    return LongUtils.fromInt(instance);
  }

  public static long widenShortToLong(int instance) {
    return LongUtils.fromInt(instance);
  }

  public static long widenIntToLong(int instance) {
    return LongUtils.fromInt(instance);
  }

  public static long narrowFloatToLong(int instance) {
    return LongUtils.fromNumber(instance);
  }

  public static long narrowDoubleToLong(int instance) {
    return LongUtils.fromNumber(instance);
  }

  /** Narrows a Long to a 8-bit signed number. */
  public static int narrowLongToByte(long instance) {
    int intValue = LongUtils.toInt(instance);
    return toByte(intValue);
  }

  /** Narrows a Long to a 16-bit number. */
  public static int narrowLongToChar(long instance) {
    int intValue = LongUtils.toInt(instance);
    return toChar(intValue);
  }

  /** Narrows a Long to a 16-bit signed number. */
  public static int narrowLongToShort(long instance) {
    int intValue = LongUtils.toInt(instance);
    return toShort(intValue);
  }

  /** Narrows a Long to a 32-bit signed number. */
  public static int narrowLongToInt(long instance) {
    return LongUtils.toInt(instance);
  }

  public static double widenLongToFloat(long instance) {
    return LongUtils.toNumber(instance);
  }

  public static double widenLongToDouble(long instance) {
    return LongUtils.toNumber(instance);
  }

  public static int narrowFloatToByte(int instance) {
    int roundInt = roundToInt(instance);
    return toByte(roundInt);
  }

  public static int narrowDoubleToByte(int instance) {
    int roundInt = roundToInt(instance);
    return toByte(roundInt);
  }

  public static int narrowFloatToChar(int instance) {
    int roundInt = roundToInt(instance);
    return toChar(roundInt);
  }

  public static int narrowDoubleToChar(int instance) {
    int roundInt = roundToInt(instance);
    return toChar(roundInt);
  }

  /**
   * Narrows a float number to a 16-bit signed number.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  public static int narrowFloatToShort(int instance) {
    int roundInt = roundToInt(instance);
    return toShort(roundInt);
  }

  /**
   * Narrows a double number to a 16-bit signed number.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  public static int narrowDoubleToShort(int instance) {
    int roundInt = roundToInt(instance);
    return toShort(roundInt);
  }

  /**
   * Narrows a float number to a 32-bit signed number.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  public static int narrowFloatToInt(int instance) {
    return roundToInt(instance);
  }

  /**
   * Narrows a double number to a 32-bit signed number.
   *
   * @param {number} instance
   * @return {number}
   * @public
   */
  public static int narrowDoubleToInt(int instance) {
    return roundToInt(instance);
  }

  /** Checks if result is Infinity or Nan to catch division by zero and coerces it to integer */
  public static int coerceDivision(int value) {
    InternalPreconditions.checkArithmetic(Double.isFinite(value));
    return toInt(value);
  }

  /**
   * Rounds to an integral value.
   */
  private static int roundToInt(int value) {
    return toInt(Math.max(Math.min(value, Integer.MAX_VALUE), Integer.MIN_VALUE));
  }
}
