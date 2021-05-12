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

import javaemul.internal.annotations.DoNotAutobox;
import jsinterop.annotations.JsType;

/**
 * Defines utility static functions that map from transpiled Long instantiation and arithmetic
 * operations to some particular Long emulation library. (In this case Closure's goog.math.Long)
 */
@JsType(namespace = "vmbootstrap")
public class LongUtils {

  public static long fromBits(int lowBits, int highBits) {
    return NativeLong.fromBits(lowBits, highBits);
  }

  public static int compare(long a, long b) {
    return toNativeLong(a).compare(b);
  }

  public static long fromInt(int value) {
    // Ensure int is coerced to 32 bits.
    return NativeLong.fromInt(value | 0);
  }

  public static long fromNumber(double value) {
    return NativeLong.fromNumber(value);
  }

  public static int toInt(long value) {
    return toNativeLong(value).toInt();
  }

  public static double toNumber(long value) {
    return toNativeLong(value).toNumber();
  }

  public static long bitAnd(long leftLong, long rightLong) {
    return toNativeLong(leftLong).and(rightLong);
  }

  public static long not(long valueLong) {
    return toNativeLong(valueLong).not();
  }

  public static long divide(long leftLong, long rightLong) {
    LongUtils.checkDivisorZero(rightLong);
    return toNativeLong(leftLong).div(rightLong);
  }

  public static boolean equals(long leftLong, long rightLong) {
    return toNativeLong(leftLong).equals(toNativeLong(rightLong));
  }

  public static boolean greater(long leftLong, long rightLong) {
    return toNativeLong(leftLong).greaterThan(rightLong);
  }

  public static boolean greaterEquals(long leftLong, long rightLong) {
    return toNativeLong(leftLong).greaterThanOrEqual(rightLong);
  }

  public static long leftShift(long valueLong, int numBits) {
    return toNativeLong(valueLong).shiftLeft(numBits);
  }

  public static boolean less(long leftLong, long rightLong) {
    return toNativeLong(leftLong).lessThan(rightLong);
  }

  public static boolean lessEquals(long leftLong, long rightLong) {
    return toNativeLong(leftLong).lessThanOrEqual(rightLong);
  }

  public static long minus(long leftLong, long rightLong) {
    return toNativeLong(leftLong).subtract(rightLong);
  }

  public static long negate(long valueLong) {
    return toNativeLong(valueLong).negate();
  }

  public static boolean notEquals(long leftLong, long rightLong) {
    return toNativeLong(leftLong).notEquals(rightLong);
  }

  public static long bitOr(long leftLong, long rightLong) {
    return toNativeLong(leftLong).or(rightLong);
  }

  public static long plus(long leftLong, long rightLong) {
    return toNativeLong(leftLong).add(rightLong);
  }

  public static long remainder(long leftLong, long rightLong) {
    LongUtils.checkDivisorZero(rightLong);
    return toNativeLong(leftLong).modulo(rightLong);
  }

  public static long rightShiftSigned(long valueLong, int numBits) {
    return toNativeLong(valueLong).shiftRight(numBits);
  }

  public static long rightShiftUnsigned(long valueLong, int numBits) {
    return toNativeLong(valueLong).shiftRightUnsigned(numBits);
  }

  public static long times(long leftLong, long rightLong) {
    return toNativeLong(leftLong).multiply(rightLong);
  }

  public static long bitXor(long leftLong, long rightLong) {
    return toNativeLong(leftLong).xor(rightLong);
  }

  public static int getHighBits(long valueLong) {
    return toNativeLong(valueLong).getHighBits();
  }

  public static int getLowBits(long valueLong) {
    return toNativeLong(valueLong).getLowBits();
  }

  public static String toString(long valueLong, int radix) {
    return toNativeLong(valueLong).toString(radix);
  }

  public static void checkDivisorZero(long divisor) {
    InternalPreconditions.checkArithmetic(!toNativeLong(divisor).isZero());
  }

  private static NativeLong toNativeLong(@DoNotAutobox Object l) {
    return JsUtils.uncheckedCast(l);
  }

  @JsType(isNative = true, name = "Long", namespace = "nativebootstrap")
  static class NativeLong {
    public static native long fromBits(int lowBits, int highBits);

    public static native long fromInt(int value);

    public static native long fromNumber(double value);

    public native long add(long rightLong);

    public native long and(long rightLong);

    public native int compare(long b);

    public native long div(long rightLong);

    public native int getHighBits();

    public native int getLowBits();

    public native boolean greaterThan(long rightLong);

    public native boolean greaterThanOrEqual(long rightLong);

    public native boolean isZero();

    public native boolean lessThan(long rightLong);

    public native boolean lessThanOrEqual(long rightLong);

    public native long modulo(long rightLong);

    public native long multiply(long rightLong);

    public native long negate();

    public native long not();

    public native boolean notEquals(long rightLong);

    public native long or(long rightLong);

    public native long shiftLeft(int int1);

    public native long shiftRight(int int1);

    public native long shiftRightUnsigned(int int1);

    public native long subtract(long rightLong);

    public native int toInt();

    public native double toNumber();

    public native long xor(long rightLong);

    public native String toString(int radix);

    // This is a native object whose hashCode method should never be called.
    @SuppressWarnings("EqualsHashCode")
    public native boolean equals(Object rightLong);
  }
}
