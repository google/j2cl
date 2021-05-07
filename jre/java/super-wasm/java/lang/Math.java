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

import javaemul.internal.LongUtils;
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
  private static final double LOG10E = 0.4342944819032518;

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

  public static double cbrt(double x) {
    return x == 0 || !Double.isFinite(x) ? x : pow(x, 1.0 / 3.0);
  }

  @Wasm("f64.ceil")
  public static native double ceil(double x);

  @Wasm("f64.copysign")
  public static native double copySign(double magnitude, double sign);

  @Wasm("f32.copysign")
  public static native float copySign(float magnitude, float sign);

  @JsMethod(namespace = GLOBAL, name = "Math.cos")
  public static native double cos(double x);

  public static double cosh(double x) {
    return (exp(x) + exp(-x)) / 2;
  }

  public static int decrementExact(int x) {
    checkCriticalArithmetic(x != Integer.MIN_VALUE);
    return x - 1;
  }

  public static long decrementExact(long x) {
    checkCriticalArithmetic(x != Long.MIN_VALUE);
    return x - 1;
  }

  public static double exp(double x) {
    // Adapted from V8 ieee754.cc

    final double //
        one = 1.0,
        invln2 = 1.44269504088896338700e+00, /* 0x3FF71547, 0x652B82FE */
        o_threshold = 7.09782712893383973096e+02, /* 0x40862E42, 0xFEFA39EF */
        u_threshold = -7.45133219101941108420e+02, /* 0xC0874910, 0xD52D3051 */
        P1 = 1.66666666666666019037e-01, /* 0x3FC55555, 0x5555553E */
        P2 = -2.77777777770155933842e-03, /* 0xBF66C16C, 0x16BEBD93 */
        P3 = 6.61375632143793436117e-05, /* 0x3F11566A, 0xAF25DE2C */
        P4 = -1.65339022054652515390e-06, /* 0xBEBBBD41, 0xC5D26BF1 */
        P5 = 4.13813679705723846039e-08, /* 0x3E663769, 0x72BEA4D0 */
        E = 2.718281828459045, /* 0x4005BF0A, 0x8B145769 */
        huge = 1.0e+300,
        twom1000 = 9.33263618503218878990e-302, /* 2**-1000=0x01700000,0*/
        two1023 = 8.988465674311579539e307; /* 0x1p1023 */

    final double[] //
        halF = {0.5, -0.5},
        ln2HI =
            {
              6.93147180369123816490e-01, /* 0x3FE62E42, 0xFEE00000 */
              -6.93147180369123816490e-01 /* 0xBFE62E42, 0xFEE00000 */
            },
        ln2LO =
            {
              1.90821492927058770002e-10, /* 0x3DEA39EF, 0x35793C76 */
              -1.90821492927058770002e-10 /* 0xBDEA39EF, 0x35793C76 */
            };

    double y, hi = 0.0, lo = 0.0, c, t, twopk;
    int k = 0, xsb;
    int hx;

    hx = LongUtils.getHighBits(Double.doubleToRawLongBits(x));
    xsb = (hx >> 31) & 1; /* sign bit of x */
    hx &= 0x7FFFFFFF; /* high word of |x| */

    /* filter out non-finite argument */
    if (hx >= 0x40862E42) { // if |x|>=709.78...
      if (hx >= 0x7FF00000) {
        int lx;
        lx = (int) Double.doubleToRawLongBits(x);
        if (((hx & 0xFFFFF) | lx) != 0) {
          return Double.NaN;
        } else {
          return (xsb == 0) ? Double.POSITIVE_INFINITY : 0.0; /* exp(+-inf)={inf,0} */
        }
      }
      if (x > o_threshold) {
        return huge * huge; /* overflow */
      }
      if (x < u_threshold) {
        return twom1000 * twom1000; /* underflow */
      }
    }

    /* argument reduction */
    if (hx > 0x3FD62E42) { // if  |x| > 0.5 ln2
      if (hx < 0x3FF0A2B2) { // and |x| < 1.5 ln2
        if (x == 1.0) {
          return E;
        }
        hi = x - ln2HI[xsb];
        lo = ln2LO[xsb];
        k = 1 - xsb - xsb;
      } else {
        k = (int) (invln2 * x + halF[xsb]);
        t = k;
        hi = x - t * ln2HI[0]; /* t*ln2HI is exact here */
        lo = t * ln2LO[0];
      }
      x = hi - lo;
    } else if (hx < 0x3E300000) { // when |x|<2**-28
      if (huge + x > one) {
        return one + x; /* trigger inexact */
      }
    } else {
      k = 0;
    }

    /* x is now in primary range */
    t = x * x;
    if (k >= -1021) {
      // INSERT_WORDS(
      //    twopk, 0x3FF00000 + static_cast < int32_t > (static_cast < uint32_t > (k) << 20), 0);
      twopk = Double.longBitsToDouble(LongUtils.fromBits(0, 0x3FF00000 + (k << 20)));
    } else {
      // INSERT_WORDS(twopk, 0x3FF00000 + (static_cast < uint32_t > (k + 1000) << 20), 0);
      twopk = Double.longBitsToDouble(LongUtils.fromBits(0, 0x3FF00000 + ((k + 1000) << 20)));
    }
    c = x - t * (P1 + t * (P2 + t * (P3 + t * (P4 + t * P5))));
    if (k == 0) {
      return one - ((x * c) / (c - 2.0) - x);
    } else {
      y = one - ((lo - (x * c) / (2.0 - c)) - hi);
    }
    if (k >= -1021) {
      if (k == 1024) {
        return y * 2.0 * two1023;
      }
      return y * twopk;
    } else {
      return y * twopk * twom1000;
    }
  }

  public static double expm1(double d) {
    return d == 0 ? d : exp(d) - 1;
  }

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

  public static double hypot(double x, double y) {
    return Double.isInfinite(x) || Double.isInfinite(y) ?
        Double.POSITIVE_INFINITY : sqrt(x * x + y * y);
  }

  public static int incrementExact(int x) {
    checkCriticalArithmetic(x != Integer.MAX_VALUE);
    return x + 1;
  }

  public static long incrementExact(long x) {
    checkCriticalArithmetic(x != Long.MAX_VALUE);
    return x + 1;
  }

  public static double log(double x) {
    // Adapted from V8 ieee754.cc

    final double //
        ln2_hi = 6.93147180369123816490e-01, /* 3fe62e42 fee00000 */
        ln2_lo = 1.90821492927058770002e-10, /* 3dea39ef 35793c76 */
        two54 = 1.80143985094819840000e+16, /* 43500000 00000000 */
        Lg1 = 6.666666666666735130e-01, /* 3FE55555 55555593 */
        Lg2 = 3.999999999940941908e-01, /* 3FD99999 9997FA04 */
        Lg3 = 2.857142874366239149e-01, /* 3FD24924 94229359 */
        Lg4 = 2.222219843214978396e-01, /* 3FCC71C5 1D8E78AF */
        Lg5 = 1.818357216161805012e-01, /* 3FC74664 96CB03DE */
        Lg6 = 1.531383769920937332e-01, /* 3FC39A09 D078C69F */
        Lg7 = 1.479819860511658591e-01; /* 3FC2F112 DF3E5244 */

    double hfsq, f, s, z, R, w, t1, t2, dk;
    int k, hx, i, j;
    int lx;

    // EXTRACT_WORDS(hx, lx, x);
    long bits = Double.doubleToRawLongBits(x);
    hx = LongUtils.getHighBits(bits);
    lx = (int) bits;

    k = 0;
    if (hx < 0x00100000) {
      /* x < 2**-1022  */
      if (((hx & 0x7FFFFFFF) | lx) == 0) {
        return Double.NEGATIVE_INFINITY; /* log(+-0)=-inf */
      }
      if (hx < 0) {
        return Double.NaN; /* log(-#) = NaN */
      }
      k -= 54;
      x *= two54; /* subnormal number, scale up x */
      // GET_HIGH_WORD(hx, x);
      hx = LongUtils.getHighBits(Double.doubleToRawLongBits(x));
    }
    if (hx >= 0x7FF00000) {
      return x + x;
    }
    k += (hx >> 20) - 1023;
    hx &= 0x000FFFFF;
    i = (hx + 0x95F64) & 0x100000;

    // SET_HIGH_WORD(x, hx | (i ^ 0x3FF00000)); /* normalize x or x/2 */
    x = setHighWord(x, hx | (i ^ 0x3FF00000));
    k += (i >> 20);
    f = x - 1.0;
    if ((0x000FFFFF & (2 + hx)) < 3) { // -2**-20 <= f < 2**-20
      if (f == 0) {
        if (k == 0) {
          return 0;
        } else {
          dk = (double) k;
          return dk * ln2_hi + dk * ln2_lo;
        }
      }
      R = f * f * (0.5 - 0.33333333333333333 * f);
      if (k == 0) {
        return f - R;
      } else {
        dk = (double) k;
        return dk * ln2_hi - ((R - dk * ln2_lo) - f);
      }
    }
    s = f / (2.0 + f);
    dk = (double) (k);
    z = s * s;
    i = hx - 0x6147A;
    w = z * z;
    j = 0x6B851 - hx;
    t1 = w * (Lg2 + w * (Lg4 + w * Lg6));
    t2 = z * (Lg1 + w * (Lg3 + w * (Lg5 + w * Lg7)));
    i |= j;
    R = t2 + t1;
    if (i > 0) {
      hfsq = 0.5 * f * f;
      if (k == 0) {
        return f - (hfsq - s * (hfsq + R));
      } else {
        return dk * ln2_hi - ((hfsq - (s * (hfsq + R) + dk * ln2_lo)) - f);
      }
    } else {
      if (k == 0) {
        return f - s * (f - R);
      } else {
        return dk * ln2_hi - ((s * (f - R) - dk * ln2_lo) - f);
      }
    }
  }

  public static double log10(double x) {
    return log(x) * LOG10E;
  }

  public static double log1p(double x) {
    return x == 0 ? x : log(x + 1);
  }

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

  public static double pow(double x, double y) {
    // Adapted from V8 ieee754.cc

    final double[] //
        bp = {1.0, 1.5},
        dp_h = {0.0, 5.84962487220764160156e-01}, // 0x3FE2B803, 0x40000000
        dp_l = {0.0, 1.35003920212974897128e-08}; // 0x3E4CFDEB, 0x43CFD006

    final double //
        zero = 0.0,
        one = 1.0,
        two = 2.0,
        two53 = 9007199254740992.0, // 0x43400000, 0x00000000
        huge = 1.0e300,
        tiny = 1.0e-300,
        // poly coefs for (3/2)*(log(x)-2s-2/3*s**3
        L1 = 5.99999999999994648725e-01, // 0x3FE33333, 0x33333303
        L2 = 4.28571428578550184252e-01, // 0x3FDB6DB6, 0xDB6FABFF
        L3 = 3.33333329818377432918e-01, // 0x3FD55555, 0x518F264D
        L4 = 2.72728123808534006489e-01, // 0x3FD17460, 0xA91D4101
        L5 = 2.30660745775561754067e-01, // 0x3FCD864A, 0x93C9DB65
        L6 = 2.06975017800338417784e-01, // 0x3FCA7E28, 0x4A454EEF
        P1 = 1.66666666666666019037e-01, // 0x3FC55555, 0x5555553E
        P2 = -2.77777777770155933842e-03, // 0xBF66C16C, 0x16BEBD93
        P3 = 6.61375632143793436117e-05, // 0x3F11566A, 0xAF25DE2C
        P4 = -1.65339022054652515390e-06, // 0xBEBBBD41, 0xC5D26BF1
        P5 = 4.13813679705723846039e-08, // 0x3E663769, 0x72BEA4D0
        lg2 = 6.93147180559945286227e-01, // 0x3FE62E42, 0xFEFA39EF
        lg2_h = 6.93147182464599609375e-01, // 0x3FE62E43, 0x00000000
        lg2_l = -1.90465429995776804525e-09, // 0xBE205C61, 0x0CA86C39
        ovt = 8.0085662595372944372e-0017, // -(1024-log2(ovfl+.5ulp))
        cp = 9.61796693925975554329e-01, // 0x3FEEC709, 0xDC3A03FD =2/(3ln2)
        cp_h = 9.61796700954437255859e-01, // 0x3FEEC709, 0xE0000000 =(float)cp
        cp_l = -7.02846165095275826516e-09, // 0xBE3E2FE0, 0x145B01F5 =tail cp_h
        ivln2 = 1.44269504088896338700e+00, // 0x3FF71547, 0x652B82FE =1/ln2
        ivln2_h = 1.44269502162933349609e+00, // 0x3FF71547, 0x60000000 =24b 1/ln2
        ivln2_l = 1.92596299112661746887e-08; // 0x3E54AE0B, 0xF85DDF44 =1/ln2 tail

    double z, ax, z_h, z_l, p_h, p_l;
    double y1, t1, t2, r, s, t, u, v, w;
    int i, j, k, yisint, n;
    int hx, hy, ix, iy;
    int lx, ly;

    // EXTRACT_WORDS(hx, lx, x);
    long bits = Double.doubleToRawLongBits(x);
    hx = LongUtils.getHighBits(bits);
    lx = (int) bits;

    // EXTRACT_WORDS(hy, ly, y);
    bits = Double.doubleToRawLongBits(y);
    hy = LongUtils.getHighBits(bits);
    ly = (int) bits;

    ix = hx & 0x7fffffff;
    iy = hy & 0x7fffffff;

    /* y==zero: x**0 = 1 */
    if ((iy | ly) == 0) {
      return one;
    }

    /* +-NaN return x+y */
    if (ix > 0x7ff00000
        || ((ix == 0x7ff00000) && (lx != 0))
        || iy > 0x7ff00000
        || ((iy == 0x7ff00000) && (ly != 0))) {
      return x + y;
    }

    /* determine if y is an odd int when x < 0
     * yisint = 0 ... y is not an integer
     * yisint = 1 ... y is an odd int
     * yisint = 2 ... y is an even int
     */
    yisint = 0;
    if (hx < 0) {
      if (iy >= 0x43400000) {
        yisint = 2; /* even integer y */
      } else if (iy >= 0x3ff00000) {
        k = (iy >> 20) - 0x3ff; /* exponent */
        if (k > 20) {
          j = ly >> (52 - k);
          if ((j << (52 - k)) == (int) (ly)) {
            yisint = 2 - (j & 1);
          }
        } else if (ly == 0) {
          j = iy >> (20 - k);
          if ((j << (20 - k)) == iy) {
            yisint = 2 - (j & 1);
          }
        }
      }
    }

    /* special value of y */
    if (ly == 0) {
      if (iy == 0x7ff00000) {
        /* y is +-inf */
        if (((ix - 0x3ff00000) | lx) == 0) {
          @SuppressWarnings("IdentityBinaryExpression")
          double rv = y - y; /* inf**+-1 is NaN */
          return rv;
        } else if (ix >= 0x3ff00000) {
          /* (|x|>1)**+-inf = inf,0 */
          return (hy >= 0) ? y : zero;
        } else {
          /* (|x|<1)**-,+inf = inf,0 */
          return (hy < 0) ? -y : zero;
        }
      }
      if (iy == 0x3ff00000) {
        /* y is  +-1 */
        if (hy < 0) {
          return one / x;
        } else {
          return x;
        }
      }
      if (hy == 0x40000000) {
        return x * x; /* y is  2 */
      }
      if (hy == 0x3fe00000) {
        /* y is  0.5 */
        if (hx >= 0) {
          /* x >= +0 */
          return sqrt(x);
        }
      }
    }

    ax = abs(x);
    /* special value of x */
    if (lx == 0) {
      if (ix == 0x7ff00000 || ix == 0 || ix == 0x3ff00000) {
        z = ax; /*x is +-0,+-inf,+-1*/
        if (hy < 0) {
          z = one / z; /* z = (1/|x|) */
        }
        if (hx < 0) {
          if (((ix - 0x3ff00000) | yisint) == 0) {
            /* (-1)**non-int is NaN */
            z = Double.NaN;
          } else if (yisint == 1) {
            z = -z; /* (x<0)**odd = -(|x|**odd) */
          }
        }
        return z;
      }
    }

    n = (hx >> 31) + 1;

    /* (x<0)**(non-int) is NaN */
    if ((n | yisint) == 0) {
      return Double.NaN;
    }

    s = one; /* s (sign of result -ve**odd) = -1 else = 1 */
    if ((n | (yisint - 1)) == 0) {
      s = -one; /* (-ve)**(odd int) */
    }

    /* |y| is huge */
    if (iy > 0x41e00000) {
      /* if |y| > 2**31 */
      if (iy > 0x43f00000) {
        /* if |y| > 2**64, must o/uflow */
        if (ix <= 0x3fefffff) {
          return (hy < 0) ? huge * huge : tiny * tiny;
        }
        if (ix >= 0x3ff00000) {
          return (hy > 0) ? huge * huge : tiny * tiny;
        }
      }
      /* over/underflow if x is not close to one */
      if (ix < 0x3fefffff) {
        return (hy < 0) ? s * huge * huge : s * tiny * tiny;
      }
      if (ix > 0x3ff00000) {
        return (hy > 0) ? s * huge * huge : s * tiny * tiny;
      }
      /* now |1-x| is tiny <= 2**-20, suffice to compute
      log(x) by x-x^2/2+x^3/3-x^4/4 */
      t = ax - one; /* t has 20 trailing zeros */
      w = (t * t) * (0.5 - t * (0.3333333333333333333333 - t * 0.25));
      u = ivln2_h * t; /* ivln2_h has 21 sig. bits */
      v = t * ivln2_l - w * ivln2;
      t1 = u + v;
      // SET_LOW_WORD(t1, 0);
      t1 = setLowWord(t1, 0);
      t2 = v - (t1 - u);
    } else {
      double ss, s2, s_h, s_l, t_h, t_l;
      n = 0;
      /* take care subnormal number */
      if (ix < 0x00100000) {
        ax *= two53;
        n -= 53;
        // GET_HIGH_WORD(ix, ax);
        ix = LongUtils.getHighBits(Double.doubleToRawLongBits(ax));
      }
      n += ((ix) >> 20) - 0x3ff;
      j = ix & 0x000fffff;
      /* determine interval */
      ix = j | 0x3ff00000; /* normalize ix */
      if (j <= 0x3988E) {
        k = 0; /* |x|<sqrt(3/2) */
      } else if (j < 0xBB67A) {
        k = 1; /* |x|<sqrt(3)   */
      } else {
        k = 0;
        n += 1;
        ix -= 0x00100000;
      }
      // SET_HIGH_WORD(ax, ix);
      ax = setHighWord(ax, ix);

      /* compute ss = s_h+s_l = (x-1)/(x+1) or (x-1.5)/(x+1.5) */
      u = ax - bp[k]; /* bp[0]=1.0, bp[1]=1.5 */
      v = one / (ax + bp[k]);
      ss = u * v;
      s_h = ss;
      // SET_LOW_WORD(s_h, 0);
      s_h = setLowWord(s_h, 0);
      /* t_h=ax+bp[k] High */
      t_h = zero;
      // SET_HIGH_WORD(t_h, ((ix >> 1) | 0x20000000) + 0x00080000 + (k << 18));
      t_h = setHighWord(t_h, ((ix >> 1) | 0x20000000) + 0x00080000 + (k << 18));
      t_l = ax - (t_h - bp[k]);
      s_l = v * ((u - s_h * t_h) - s_h * t_l);
      /* compute log(ax) */
      s2 = ss * ss;
      r = s2 * s2 * (L1 + s2 * (L2 + s2 * (L3 + s2 * (L4 + s2 * (L5 + s2 * L6)))));
      r += s_l * (s_h + ss);
      s2 = s_h * s_h;
      t_h = 3.0 + s2 + r;
      // SET_LOW_WORD(t_h, 0);
      t_h = setLowWord(t_h, 0);
      t_l = r - ((t_h - 3.0) - s2);
      /* u+v = ss*(1+...) */
      u = s_h * t_h;
      v = s_l * t_h + t_l * ss;
      /* 2/(3log2)*(ss+...) */
      p_h = u + v;
      // SET_LOW_WORD(p_h, 0);
      p_h = setLowWord(p_h, 0);
      p_l = v - (p_h - u);
      z_h = cp_h * p_h; /* cp_h+cp_l = 2/(3*log2) */
      z_l = cp_l * p_h + p_l * cp + dp_l[k];
      /* log2(ax) = (ss+..)*2/(3*log2) = n + dp_h + z_h + z_l */
      t = (double) n;
      t1 = (((z_h + z_l) + dp_h[k]) + t);
      // SET_LOW_WORD(t1, 0);
      t1 = setLowWord(t1, 0);
      t2 = z_l - (((t1 - t) - dp_h[k]) - z_h);
    }

    /* split up y into y1+y2 and compute (y1+y2)*(t1+t2) */
    y1 = y;
    // SET_LOW_WORD(y1, 0);
    y1 = setLowWord(y1, 0);
    p_l = (y - y1) * t1 + y * t2;
    p_h = y1 * t1;
    z = p_l + p_h;
    // EXTRACT_WORDS(j, i, z);
    bits = Double.doubleToRawLongBits(z);
    j = LongUtils.getHighBits(bits);
    i = (int) bits;
    if (j >= 0x40900000) {
      /* z >= 1024 */
      if (((j - 0x40900000) | i) != 0) {
        /* if z > 1024 */
        return s * huge * huge; /* overflow */
      } else {
        if (p_l + ovt > z - p_h) {
          return s * huge * huge; /* overflow */
        }
      }
    } else if ((j & 0x7fffffff) >= 0x4090cc00) {
      /* z <= -1075 */
      if (((j - 0xc090cc00) | i) != 0) {
        /* z < -1075 */
        return s * tiny * tiny; /* underflow */
      } else {
        if (p_l <= z - p_h) {
          return s * tiny * tiny; /* underflow */
        }
      }
    }
    /*
     * compute 2**(p_h+p_l)
     */
    i = j & 0x7fffffff;
    k = (i >> 20) - 0x3ff;
    n = 0;
    if (i > 0x3fe00000) {
      /* if |z| > 0.5, set n = [z+0.5] */
      n = j + (0x00100000 >> (k + 1));
      k = ((n & 0x7fffffff) >> 20) - 0x3ff; /* new k for n */
      t = zero;
      // SET_HIGH_WORD(t, n & ~(0x000fffff >> k));
      t = setHighWord(t, n & ~(0x000fffff >> k));
      n = ((n & 0x000fffff) | 0x00100000) >> (20 - k);
      if (j < 0) {
        n = -n;
      }
      p_h -= t;
    }
    t = p_l + p_h;
    // SET_LOW_WORD(t, 0);
    t = setLowWord(t, 0);
    u = t * lg2_h;
    v = (p_l - (t - p_h)) * lg2 + t * lg2_l;
    z = u + v;
    w = v - (z - u);
    t = z * z;
    t1 = z - t * (P1 + t * (P2 + t * (P3 + t * (P4 + t * P5))));
    r = (z * t1) / ((t1 - two) - (w + z * w));
    z = one - (r - z);
    // GET_HIGH_WORD(j, z);
    j = LongUtils.getHighBits(Double.doubleToRawLongBits(z));
    j += n << 20;
    if ((j >> 20) <= 0) {
      z = scalb(z, n); /* subnormal output */
    } else {
      int tmp;
      // GET_HIGH_WORD(tmp, z);
      tmp = LongUtils.getHighBits(Double.doubleToRawLongBits(z));
      // SET_HIGH_WORD(z, tmp + (n << 20));
      z = setHighWord(z, tmp + (n << 20));
    }
    return s * z;
  }

  @JsMethod(namespace = GLOBAL, name = "Math.random")
  public static native double random();

  @Wasm("f64.nearest")
  public static native double rint(double x);

  @Wasm("f32.nearest")
  private static native float rint(float x);

  public static long round(double x) {
    if (Double.isFinite(x)) {
      double mod2 = x % 2;
      if (mod2 == -1.5 || mod2 == 0.5) {
        x = floor(x);
      } else {
        x = rint(x);
      }
    }
    return (long) x;
  }

  public static int round(float x) {
    return (int) round(x);
  }

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

  public static double signum(double d) {
    if (d == 0 || Double.isNaN(d)) {
      return d;
    } else {
      return d < 0 ? -1 : 1;
    }
  }

  public static float signum(float f) {
    return (float) signum((double) f);
  }

  @JsMethod(namespace = GLOBAL, name = "Math.sin")
  public static native double sin(double x);

  public static double sinh(double x) {
    return x == 0.0 ? x : (exp(x) - exp(-x)) / 2;
  }

  @Wasm("f64.sqrt")
  public static native double sqrt(double x);

  @JsMethod(namespace = GLOBAL, name = "Math.tan")
  public static native double tan(double x);

  public static double tanh(double x) {
    if (x == 0.0) {
      // -0.0 should return -0.0.
      return x;
    }

    double e2x = exp(2 * x);
    if (Double.isInfinite(e2x)) {
      return 1;
    }
    return (e2x - 1) / (e2x + 1);
  }

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

  private static boolean isSafeIntegerRange(double value) {
    return Integer.MIN_VALUE <= value && value <= Integer.MAX_VALUE;
  }

  private static double setLowWord(double x, int word) {
    return Double.longBitsToDouble(
        LongUtils.fromBits(word, LongUtils.getHighBits(Double.doubleToRawLongBits(x))));
  }

  private static double setHighWord(double x, int word) {
    return Double.longBitsToDouble(LongUtils.fromBits((int) Double.doubleToRawLongBits(x), word));
  }
}
