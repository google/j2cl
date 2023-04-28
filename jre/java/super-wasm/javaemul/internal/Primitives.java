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
import jsinterop.annotations.JsMethod;

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

  // This method returns int in order to avoid a cast. char and short are represented as i32 number
  // in Wasm.
  public static int narrowShortToChar(short instance) {
    return toChar(instance);
  }

  /* Narrowing methods for int */

  @Wasm("i32.extend8_s")
  public static native int narrowIntToByte(int instance);

  // This method returns int in order to avoid a cast. char and int are represented as i32 number
  // in Wasm.
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

  public static int safeDivision(int dividend, int divisor) {
    // Division may trap with overflow in Wasm division; special case the overflow scenario.
    if (dividend == Integer.MIN_VALUE && divisor == -1) {
      return Integer.MIN_VALUE;
    }
    return wasmDivision(dividend, divisor);
  }

  @Wasm("i32.div_s")
  private static native int wasmDivision(int dividend, int divisor);

  public static long safeDivision(long dividend, long divisor) {
    // Division may trap with overflow in Wasm division; special case the overflow scenario.
    if (dividend == Long.MIN_VALUE && divisor == -1L) {
      return Long.MIN_VALUE;
    }
    return wasmDivision(dividend, divisor);
  }

  @Wasm("i64.div_s")
  private static native long wasmDivision(long dividend, long divisor);

  @JsMethod(namespace = "j2wasm.DoubleUtils")
  public static native double dmod(double x, double y);

  @SuppressWarnings("IdentityBinaryExpression")
  public static float fmod(float x, float y) {
    // Derived from FreeBSD's src/e_fmodf.c which came with this notice.
    /*
     * ====================================================
     * Copyright (C) 1993 by Sun Microsystems, Inc. All rights reserved.
     *
     * Developed at SunPro, a Sun Microsystems, Inc. business.
     * Permission to use, copy, modify, and distribute this
     * software is freely granted, provided that this notice
     * is preserved.
     * ====================================================
     */

    int n, hz;
    int hx = Platform.floatToRawIntBits(x);
    int hy = Platform.floatToRawIntBits(y);
    int sx = hx & 0x80000000; /* sign of x */
    hx ^= sx; /* |x| */
    hy &= 0x7fffffff; /* |y| */

    /* purge off exception values */
    if (hy == 0 /* y=0 */
        || (hx >= 0x7f800000) /* or x not finite*/
        || (hy > 0x7f800000) /* or y is NaN */) {
      return (x * y) / (x * y);
    }
    if (hx < hy) {
      return x; /* |x|<|y| return x */
    }
    if (hx == hy) {
      return signedZero(sx); /* |x|=|y| return x*0 */
    }

    /* determine ix = ilogb(x) */
    int ix;
    if (hx < 0x00800000) {
      /* subnormal x */
      ix = -126;
      for (int i = (hx << 8); i > 0; i <<= 1) {
        ix -= 1;
      }
    } else {
      ix = (hx >> 23) - 127;
    }

    /* determine iy = ilogb(y) */
    int iy;
    if (hy < 0x00800000) {
      /* subnormal y */
      iy = -126;
      for (int i = (hy << 8); i >= 0; i <<= 1) {
        iy -= 1;
      }
    } else {
      iy = (hy >> 23) - 127;
    }

    /* set up {hx,lx}, {hy,ly} and align y to x */
    if (ix >= -126) {
      hx = 0x00800000 | (0x007fffff & hx);
    } else {
      /* subnormal x, shift x to normal */
      n = -126 - ix;
      hx = hx << n;
    }
    if (iy >= -126) {
      hy = 0x00800000 | (0x007fffff & hy);
    } else {
      /* subnormal y, shift y to normal */
      n = -126 - iy;
      hy = hy << n;
    }

    /* fix point fmod */
    n = ix - iy;
    while (n-- != 0) {
      hz = hx - hy;
      if (hz < 0) {
        hx = hx + hx;
      } else {
        if (hz == 0) {
          return signedZero(sx);
        }
        hx = hz + hz;
      }
    }
    hz = hx - hy;
    if (hz >= 0) {
      hx = hz;
    }

    /* convert back to floating value and restore the sign */
    if (hx == 0) {
      return signedZero(sx);
    }
    while (hx < 0x00800000) {
      /* normalize x */
      hx = hx + hx;
      iy -= 1;
    }
    if (iy >= -126) {
      /* normalize output */
      hx = ((hx - 0x00800000) | ((iy + 127) << 23));
      x = Float.intBitsToFloat(hx | sx);
    } else {
      /* subnormal output */
      n = -126 - iy;
      hx >>= n;
      x = Float.intBitsToFloat(hx | sx);
      x *= 1.0f; /* create necessary signal */
    }

    return x; /* exact output */
  }

  private static float signedZero(int sx) {
    return sx == 0 ? .0f : -.0f;
  }

  /** Narrows a number to an unsigned 16-bit number. */
  private static int toChar(int instance) {
    return instance & 0xFFFF;
  }
}
