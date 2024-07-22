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

import static javaemul.internal.InternalPreconditions.checkCriticalArithmetic;
import static jsinterop.annotations.JsPackage.GLOBAL;

import javaemul.internal.annotations.Wasm;
import jsinterop.annotations.JsMethod;

/**
 * Math utility methods and constants.
 */
public final class Math {

  public static final double E = 2.7182818284590452354;
  public static final double PI = 3.14159265358979323846;

  private static final double PI_OVER_180 = PI / 180.0;
  private static final double PI_UNDER_180 = 180.0 / PI;

  @Wasm("f64.abs")
  public static native double abs(double x);

  @Wasm("f32.abs")
  public static native float abs(float x);

  public static int abs(int x) {
    // Reference: Hacker's Delight - 2–4 Absolute Value Function
    int y = x >> 31;
    return (x + y) ^ y;
  }

  public static long abs(long x) {
    long y = x >> 63;
    return (x + y) ^ y;
  }

  @JsMethod(namespace = GLOBAL, name = "Math.acos")
  public static native double acos(double x);

  @JsMethod(namespace = GLOBAL, name = "Math.asin")
  public static native double asin(double x);

  public static int addExact(int x, int y) {
    int r = x + y;
    // "Hacker's Delight" 2-12 Overflow if both arguments have the opposite sign of the result
    checkCriticalArithmetic(((x ^ r) & (y ^ r)) >= 0);
    return r;
  }

  public static long addExact(long x, long y) {
    long r = x + y;
    // "Hacker's Delight" 2-12 Overflow if both arguments have the opposite sign of the result
    checkCriticalArithmetic(((x ^ r) & (y ^ r)) >= 0);
    return r;
  }

  @JsMethod(namespace = GLOBAL, name = "Math.atan")
  public static native double atan(double x);

  @JsMethod(namespace = GLOBAL, name = "Math.atan2")
  public static native double atan2(double y, double x);

  @JsMethod(namespace = GLOBAL, name = "Math.cbrt")
  public static native double cbrt(double x);

  @Wasm("f64.ceil")
  public static native double ceil(double x);

  @Wasm("f64.copysign")
  public static native double copySign(double magnitude, double sign);

  @Wasm("f32.copysign")
  public static native float copySign(float magnitude, float sign);

  @JsMethod(namespace = GLOBAL, name = "Math.cos")
  public static native double cos(double x);

  @JsMethod(namespace = GLOBAL, name = "Math.cosh")
  public static native double cosh(double x);

  public static int decrementExact(int x) {
    checkCriticalArithmetic(x != Integer.MIN_VALUE);
    return x - 1;
  }

  public static long decrementExact(long x) {
    checkCriticalArithmetic(x != Long.MIN_VALUE);
    return x - 1;
  }

  @JsMethod(namespace = GLOBAL, name = "Math.exp")
  public static native double exp(double x);

  @JsMethod(namespace = GLOBAL, name = "Math.expm1")
  public static native double expm1(double d);

  @Wasm("f64.floor")
  public static native double floor(double x);

  public static int floorDiv(int dividend, int divisor) {
    checkCriticalArithmetic(divisor != 0);
    // round down division if the signs are different and modulo not zero
    return ((dividend ^ divisor) >= 0 ? dividend / divisor : ((dividend + 1) / divisor) - 1);
  }

  public static long floorDiv(long dividend, long divisor) {
    checkCriticalArithmetic(divisor != 0);
    // round down division if the signs are different and modulo not zero
    return ((dividend ^ divisor) >= 0 ? dividend / divisor : ((dividend + 1) / divisor) - 1);
  }

  public static int floorMod(int dividend, int divisor) {
    checkCriticalArithmetic(divisor != 0);
    return ((dividend % divisor) + divisor) % divisor;
  }

  public static long floorMod(long dividend, long divisor) {
    checkCriticalArithmetic(divisor != 0);
    return ((dividend % divisor) + divisor) % divisor;
  }

  @JsMethod(namespace = GLOBAL, name = "Math.hypot")
  public static native double hypot(double x, double y);

  public static int incrementExact(int x) {
    checkCriticalArithmetic(x != Integer.MAX_VALUE);
    return x + 1;
  }

  public static long incrementExact(long x) {
    checkCriticalArithmetic(x != Long.MAX_VALUE);
    return x + 1;
  }

  @JsMethod(namespace = GLOBAL, name = "Math.log")
  public static native double log(double x);

  @JsMethod(namespace = GLOBAL, name = "Math.log10")
  public static native double log10(double x);

  @JsMethod(namespace = GLOBAL, name = "Math.log1p")
  public static native double log1p(double x);

  @Wasm("f64.max")
  public static native double max(double x, double y);

  @Wasm("f32.max")
  public static native float max(float x, float y);

  public static int max(int x, int y) {
    return x > y ? x : y;
  }

  public static long max(long x, long y) {
    return x > y ? x : y;
  }

  @Wasm("f64.min")
  public static native double min(double x, double y);

  @Wasm("f32.min")
  public static native float min(float x, float y);

  public static int min(int x, int y) {
    return x < y ? x : y;
  }

  public static long min(long x, long y) {
    return x < y ? x : y;
  }

  public static int multiplyExact(int x, int y) {
    long lr = (long) x * (long) y;
    int r = (int) lr;
    checkCriticalArithmetic(r == lr);
    return r;
  }

  public static long multiplyExact(long x, long y) {
    if (y == -1) {
      return negateExact(x);
    }
    if (y == 0) {
      return 0;
    }
    long r = x * y;
    checkCriticalArithmetic(r / y == x);
    return r;
  }

  public static int negateExact(int x) {
    checkCriticalArithmetic(x != Integer.MIN_VALUE);
    return -x;
  }

  public static long negateExact(long x) {
    checkCriticalArithmetic(x != Long.MIN_VALUE);
    return -x;
  }

  @JsMethod(namespace = GLOBAL, name = "Math.pow")
  public static native double pow(double x, double y);

  @JsMethod(namespace = GLOBAL, name = "Math.random")
  public static native double random();

  @Wasm("f64.nearest")
  public static native double rint(double x);

  @Wasm("f32.nearest")
  private static native float rint(float x);

  public static long round(double x) {
    return (long) nativeRound(x);
  }

  public static int round(float x) {
    return (int) nativeRound(x);
  }

  @JsMethod(namespace = GLOBAL, name = "Math.round")
  public static native double nativeRound(double x);

  public static int subtractExact(int x, int y) {
    int r = x - y;
    // "Hacker's Delight" Overflow if the arguments have different signs and
    // the sign of the result is different than the sign of x
    checkCriticalArithmetic(((x ^ y) & (x ^ r)) >= 0);
    return r;
  }

  public static long subtractExact(long x, long y) {
    long r = x - y;
    // "Hacker's Delight" Overflow if the arguments have different signs and
    // the sign of the result is different than the sign of x
    checkCriticalArithmetic(((x ^ y) & (x ^ r)) >= 0);
    return r;
  }

  public static double scalb(double d, int scaleFactor) {
    if (scaleFactor >= 31 || scaleFactor <= -31) {
      return d * pow(2, scaleFactor);
    } else if (scaleFactor > 0) {
      return d * (1 << scaleFactor);
    } else if (scaleFactor == 0) {
      return d;
    } else {
      return d / (1 << -scaleFactor);
    }
  }

  public static float scalb(float f, int scaleFactor) {
    return (float) scalb((double) f, scaleFactor);
  }

  @JsMethod(namespace = GLOBAL, name = "Math.sign")
  public static native double signum(double d);

  @JsMethod(namespace = GLOBAL, name = "Math.sign")
  public static native float signum(float f);

  @JsMethod(namespace = GLOBAL, name = "Math.sin")
  public static native double sin(double x);

  @JsMethod(namespace = GLOBAL, name = "Math.sinh")
  public static native double sinh(double x);

  @Wasm("f64.sqrt")
  public static native double sqrt(double x);

  @JsMethod(namespace = GLOBAL, name = "Math.tan")
  public static native double tan(double x);

  @JsMethod(namespace = GLOBAL, name = "Math.tanh")
  public static native double tanh(double x);

  public static double toDegrees(double x) {
    return x * PI_UNDER_180;
  }

  public static int toIntExact(long x) {
    int ix = (int) x;
    checkCriticalArithmetic(ix == x);
    return ix;
  }

  public static double toRadians(double x) {
    return x * PI_OVER_180;
  }

  public static double nextUp(double start) {
    return nextAfter(start, Double.POSITIVE_INFINITY);
  }

  public static double nextDown(double start) {
    return nextAfter(start, Double.NEGATIVE_INFINITY);
  }

  public static double nextAfter(double start, double direction) {
    // Simple case described by Javadoc:
    if (start == direction) {
      return direction;
    }

    // NaN special case, if either is NaN, return NaN.
    if (Double.isNaN(start) || Double.isNaN(direction)) {
      return Double.NaN;
    }

    // The javadoc 'special cases' for infinities and min_value are handled already by manipulating
    // the bits of the start value below. However, that approach used below doesn't work around
    // zeros - we have two zero values to deal with (positive and negative) with very different bit
    // representations (zero and Long.MIN_VALUE respectively).
    if (start == 0) {
      return direction > start ? Double.MIN_VALUE : -Double.MIN_VALUE;
    }

    // Convert to int bits and increment or decrement - the fact that two positive ieee754 float
    // values can be compared as ints (or two negative values, with the comparison inverted) means
    // that this trick works as naturally as A + 1 > A. NaNs and zeros were already handled above.
    long bits = Double.doubleToLongBits(start);
    boolean increment = (direction > start) == (bits >= 0);
    return Double.longBitsToDouble(bits + (increment ? 1 : -1));
  }

  public static float nextUp(float start) {
    return nextAfter(start, Double.POSITIVE_INFINITY);
  }

  public static float nextDown(float start) {
    return nextAfter(start, Double.NEGATIVE_INFINITY);
  }

  public static float nextAfter(float start, double direction) {
    // Simple case described by Javadoc:
    if (start == direction) {
      return (float) direction;
    }

    // NaN special case, if either is NaN, return NaN.
    if (Float.isNaN(start) || Double.isNaN(direction)) {
      return Float.NaN;
    }
    // The javadoc 'special cases' for INFINITYs, MIN_VALUE, and MAX_VALUE are handled already by
    // manipulating the bits of the start value below. However, that approach used below doesn't
    // work around zeros - we have two zero values to deal with (positive and negative) with very
    // different bit representations (zero and Integer.MIN_VALUE respectively).
    if (start == 0) {
      return direction > start ? Float.MIN_VALUE : -Float.MIN_VALUE;
    }

    // Convert to int bits and increment or decrement - the fact that two positive ieee754 float
    // values can be compared as ints (or two negative values, with the comparison inverted) means
    // that this trick works as naturally as A + 1 > A. NaNs and zeros were already handled above.
    int bits = Float.floatToIntBits(start);
    boolean increment = (direction > start) == (bits >= 0);
    return Float.intBitsToFloat(bits + (increment ? 1 : -1));
  }
}
