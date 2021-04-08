/*
 * Copyright 2021 Google Inc.
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

import javaemul.internal.annotations.Wasm;

/** Static Primitive helper. */
public class Primitives {
  /* Widening methods for byte */

  public static int widenByteToChar(byte instance) {
    return toChar(instance);
  }

  @Wasm("i64.extend_i32_s")
  public static native long widenByteToLong(byte instance);

  @Wasm("f32.convert_i32_s")
  public static native float widenByteToFloat(byte instance);

  @Wasm("f64.convert_i32_s")
  public static native double widenByteToDouble(byte instance);

  /* Widening methods for char */

  @Wasm("i64.extend_i32_u")
  public static native long widenCharToLong(char instance);

  @Wasm("f32.convert_i32_u")
  public static native float widenCharToFloat(char instance);

  @Wasm("f64.convert_i32_u")
  public static native double widenCharToDouble(char instance);

  /* Widening methods for short */

  @Wasm("i64.extend_i32_s")
  public static native long widenShortToLong(short instance);

  @Wasm("f32.convert_i32_s")
  public static native float widenShortToFloat(short instance);

  @Wasm("f64.convert_i32_s")
  public static native double widenShortToDouble(short instance);

  /* Widening methods for integer */

  @Wasm("i64.extend_i32_s")
  public static native long widenIntToLong(int instance);

  @Wasm("f32.convert_i32_s")
  public static native float widenIntToFloat(int instance);

  @Wasm("f64.convert_i32_s")
  public static native double widenIntToDouble(int instance);

  /* Widening methods for long */

  @Wasm("f32.convert_i64_s")
  public static native float widenLongToFloat(long instance);

  @Wasm("f64.convert_i64_s")
  public static native double widenLongToDouble(long instance);

  /* Widening methods for float */

  @Wasm("f64.promote_f32")
  public static native double widenFloatToDouble(float instance);

  /* Narrowing methods for char */

  @Wasm("i32.extend8_s")
  public static native int narrowCharToByte(char instance);

  @Wasm("i32.extend16_s")
  public static native int narrowCharToShort(char instance);

  /* Narrowing methods for short */

  @Wasm("i32.extend8_s")
  public static native int narrowShortToByte(short instance);

  // This method returns int in order to avoid to return a cast. char and int are represented as i32
  // number in Wasm.
  public static int narrowShortToChar(short instance) {
    return toChar(instance);
  }

  /* Narrowing methods for int */

  @Wasm("i32.extend8_s")
  public static native int narrowIntToByte(int instance);

  // This method returns int in order to avoid to return a cast. char and int are represented as i32
  // number in Wasm.
  public static int narrowIntToChar(int instance) {
    return toChar(instance);
  }

  @Wasm("i32.extend16_s")
  public static native int narrowIntToShort(int instance);

  /* Narrowing methods for long */

  public static int narrowLongToByte(long instance) {
    return narrowIntToByte(narrowLongToInt(instance));
  }

  public static int narrowLongToChar(long instance) {
    return narrowIntToChar(narrowLongToInt(instance));
  }

  public static int narrowLongToShort(long instance) {
    return narrowIntToShort(narrowLongToInt(instance));
  }

  @Wasm("i32.wrap_i64")
  public static native int narrowLongToInt(long instance);

  /* Narrowing methods for float */

  public static int narrowFloatToByte(float instance) {
    return narrowIntToByte(narrowFloatToInt(instance));
  }

  public static int narrowFloatToChar(float instance) {
    return narrowIntToChar(narrowFloatToInt(instance));
  }

  public static int narrowFloatToShort(float instance) {
    return narrowIntToShort(narrowFloatToInt(instance));
  }

  @Wasm("i32.trunc_sat_f32_s")
  public static native int narrowFloatToInt(float instance);

  @Wasm("i64.trunc_sat_f32_s")
  public static native long narrowFloatToLong(float instance);

  /* Narrowing methods for double */

  public static int narrowDoubleToByte(double instance) {
    return narrowIntToByte(narrowDoubleToInt(instance));
  }

  public static int narrowDoubleToChar(double instance) {
    return narrowIntToChar(narrowDoubleToInt(instance));
  }

  public static int narrowDoubleToShort(double instance) {
    return narrowIntToShort(narrowDoubleToInt(instance));
  }

  @Wasm("i32.trunc_sat_f64_s")
  public static native int narrowDoubleToInt(double instance);

  @Wasm("i64.trunc_sat_f64_s")
  public static native long narrowDoubleToLong(double instance);

  @Wasm("f32.demote_f64")
  public static native float narrowDoubleToFloat(double instance);

  public static int coerceDivision(int value) {
    InternalPreconditions.checkArithmetic(Double.isFinite(value));
    return value;
  }

  public static double dmod(double x, double y) {
    if (Double.isNaN(x) || Double.isNaN(y) || Double.isInfinite(x) || y == 0.0d) {
      return Double.NaN;
    }

    if (Double.isInfinite(y) || x == 0.0d) {
      return x;
    }
    // See https://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.17.3
    // and https://en.wikipedia.org/wiki/Modulo_operation#Variants_of_the_definition
    return x - (y * narrowDoubleToInt(x / y));
  }

  public static float fmod(float x, float y) {
    if (Float.isNaN(x) || Float.isNaN(y) || Float.isInfinite(x) || y == 0.0f) {
      return Float.NaN;
    }

    if (Float.isInfinite(y) || x == 0.0f) {
      return x;
    }

    return x - (y * narrowFloatToInt(x / y));
  }

  /** Narrows a number to an unsigned 16-bit number. */
  private static int toChar(int instance) {
    return instance & 0xFFFF;
  }
}
