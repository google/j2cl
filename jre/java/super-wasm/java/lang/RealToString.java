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

import java.util.Arrays;
import javaemul.internal.LongUtils;

final class RealToString {

  private static final int FLOAT_MANTISSA_BITS = 23;
  private static final int FLOAT_MANTISSA_MASK = 0x007fffff;
  private static final int FLOAT_EXPONENT_BIAS = 127;
  private static final int FLOAT_EXPONENT_MASK = 0x7f800000;
  private static final int FLOAT_SIGN_MASK = 0x80000000;

  private static final int DOUBLE_MANTISSA_BITS = 52;
  private static final long DOUBLE_MANTISSA_MASK = 0x000fffffffffffffL;
  private static final int DOUBLE_EXPONENT_BIAS = 1023;
  private static final long DOUBLE_EXPONENT_MASK = 0x7ff0000000000000L;
  private static final long DOUBLE_SIGN_MASK = 0x8000000000000000L;

  private static final long[] LONG_POWERS_OF_TEN =
      new long[] {
        1L,
        10L,
        100L,
        1000L,
        10000L,
        100000L,
        1000000L,
        10000000L,
        100000000L,
        1000000000L,
        10000000000L,
        100000000000L,
        1000000000000L,
        10000000000000L,
        100000000000000L,
        1000000000000000L,
        10000000000000000L,
        100000000000000000L,
        1000000000000000000L,
      };

  private static final double invLogOfTenBaseTwo = Math.log(2.0) / Math.log(10.0);

  private static int firstK;
  /** An array of decimal digits, filled by longDigitGenerator or bigIntDigitGenerator. */
  private static final int[] digits = new int[64];
  /** Number of valid entries in 'digits'. */
  private static int digitCount;

  private RealToString() {}

  private static String resultOrSideEffect(AbstractStringBuilder sb, String s) {
    if (sb != null) {
      sb.append0(s);
      return null;
    }
    return s;
  }

  public static String doubleToString(double d) {
    return convertDouble(null, d);
  }

  public static void appendDouble(AbstractStringBuilder sb, double d) {
    convertDouble(sb, d);
  }

  private static String convertDouble(AbstractStringBuilder sb, double inputNumber) {
    long inputNumberBits = Double.doubleToLongBits(inputNumber);
    boolean positive = (inputNumberBits & DOUBLE_SIGN_MASK) == 0;
    int e = (int) ((inputNumberBits & DOUBLE_EXPONENT_MASK) >> DOUBLE_MANTISSA_BITS);
    long f = inputNumberBits & DOUBLE_MANTISSA_MASK;
    boolean mantissaIsZero = f == 0;
    String quickResult = null;
    if (e == 2047) {
      if (mantissaIsZero) {
        quickResult = positive ? "Infinity" : "-Infinity";
      } else {
        quickResult = "NaN";
      }
    } else if (e == 0) {
      if (mantissaIsZero) {
        quickResult = positive ? "0.0" : "-0.0";
      } else if (f == 1) {
        // special case to increase precision even though 2 * Double.MIN_VALUE is 1.0e-323
        quickResult = positive ? "4.9E-324" : "-4.9E-324";
      }
    }
    if (quickResult != null) {
      return resultOrSideEffect(sb, quickResult);
    }
    int p = DOUBLE_EXPONENT_BIAS + DOUBLE_MANTISSA_BITS; // the power offset (precision)
    int pow;
    int numBits = DOUBLE_MANTISSA_BITS;
    if (e == 0) {
      pow = 1 - p; // a denormalized number
      long ff = f;
      while ((ff & 0x0010000000000000L) == 0) {
        ff = ff << 1;
        numBits--;
      }
    } else {
      // 0 < e < 2047
      // a "normalized" number
      f = f | 0x0010000000000000L;
      pow = e - p;
    }
    firstK = digitCount = 0;
    if (-59 < pow && pow < 6 || (pow == -59 && !mantissaIsZero)) {
      longDigitGenerator(f, pow, e == 0, mantissaIsZero, numBits);
    } else {
      bigIntDigitGenerator(f, pow, e == 0, numBits);
    }
    AbstractStringBuilder dst = (sb != null) ? sb : new StringBuilder(26);
    if (inputNumber >= 1e7D
        || inputNumber <= -1e7D
        || (inputNumber > -1e-3D && inputNumber < 1e-3D)) {
      freeFormatExponential(dst, positive);
    } else {
      freeFormat(dst, positive);
    }
    return (sb != null) ? null : dst.toString();
  }

  public static String floatToString(float f) {
    return convertFloat(null, f);
  }

  public static void appendFloat(AbstractStringBuilder sb, float f) {
    convertFloat(sb, f);
  }

  public static String convertFloat(AbstractStringBuilder sb, float inputNumber) {
    int inputNumberBits = Float.floatToIntBits(inputNumber);
    boolean positive = (inputNumberBits & FLOAT_SIGN_MASK) == 0;
    int e = (inputNumberBits & FLOAT_EXPONENT_MASK) >> FLOAT_MANTISSA_BITS;
    int f = inputNumberBits & FLOAT_MANTISSA_MASK;
    boolean mantissaIsZero = f == 0;
    String quickResult = null;
    if (e == 255) {
      if (mantissaIsZero) {
        quickResult = positive ? "Infinity" : "-Infinity";
      } else {
        quickResult = "NaN";
      }
    } else if (e == 0 && mantissaIsZero) {
      quickResult = positive ? "0.0" : "-0.0";
    }
    if (quickResult != null) {
      return resultOrSideEffect(sb, quickResult);
    }
    int p = FLOAT_EXPONENT_BIAS + FLOAT_MANTISSA_BITS; // the power offset (precision)
    int pow;
    int numBits = FLOAT_MANTISSA_BITS;
    if (e == 0) {
      pow = 1 - p; // a denormalized number
      if (f < 8) { // want more precision with smallest values
        f = f << 2;
        pow -= 2;
      }
      int ff = f;
      while ((ff & 0x00800000) == 0) {
        ff = ff << 1;
        numBits--;
      }
    } else {
      // 0 < e < 255
      // a "normalized" number
      f = f | 0x00800000;
      pow = e - p;
    }
    firstK = digitCount = 0;
    if (-59 < pow && pow < 35 || (pow == -59 && !mantissaIsZero)) {
      longDigitGenerator(f, pow, e == 0, mantissaIsZero, numBits);
    } else {
      bigIntDigitGenerator(f, pow, e == 0, numBits);
    }
    AbstractStringBuilder dst = (sb != null) ? sb : new StringBuilder(26);
    if (inputNumber >= 1e7f
        || inputNumber <= -1e7f
        || (inputNumber > -1e-3f && inputNumber < 1e-3f)) {
      freeFormatExponential(dst, positive);
    } else {
      freeFormat(dst, positive);
    }
    return (sb != null) ? null : dst.toString();
  }

  private static void freeFormatExponential(AbstractStringBuilder sb, boolean positive) {
    int digitIndex = 0;
    if (!positive) {
      sb.append0('-');
    }
    sb.append0((char) ('0' + digits[digitIndex++]));
    sb.append0('.');
    int k = firstK;
    int exponent = k;
    while (true) {
      k--;
      if (digitIndex >= digitCount) {
        break;
      }
      sb.append0((char) ('0' + digits[digitIndex++]));
    }
    if (k == exponent - 1) {
      sb.append0('0');
    }
    sb.append0('E');
    IntegralToString.appendInt(sb, exponent);
  }

  private static void freeFormat(AbstractStringBuilder sb, boolean positive) {
    int digitIndex = 0;
    if (!positive) {
      sb.append0('-');
    }
    int k = firstK;
    if (k < 0) {
      sb.append0('0');
      sb.append0('.');
      for (int i = k + 1; i < 0; ++i) {
        sb.append0('0');
      }
    }
    int U = digits[digitIndex++];
    do {
      if (U != -1) {
        sb.append0((char) ('0' + U));
      } else if (k >= -1) {
        sb.append0('0');
      }
      if (k == 0) {
        sb.append0('.');
      }
      k--;
      U = digitIndex < digitCount ? digits[digitIndex++] : -1;
    } while (U != -1 || k >= -1);
  }


  private static void longDigitGenerator(
      long f, int e, boolean isDenormalized, boolean mantissaIsZero, int p) {
    long R, S, M;
    if (e >= 0) {
      M = 1L << e;
      if (!mantissaIsZero) {
        R = f << (e + 1);
        S = 2;
      } else {
        R = f << (e + 2);
        S = 4;
      }
    } else {
      M = 1;
      if (isDenormalized || !mantissaIsZero) {
        R = f << 1;
        S = 1L << (1 - e);
      } else {
        R = f << 2;
        S = 1L << (2 - e);
      }
    }
    int k = (int) Math.ceil((e + p - 1) * invLogOfTenBaseTwo - 1e-10);
    if (k > 0) {
      S = S * LONG_POWERS_OF_TEN[k];
    } else if (k < 0) {
      long scale = LONG_POWERS_OF_TEN[-k];
      R = R * scale;
      M = M == 1 ? scale : M * scale;
    }
    if (R + M > S) { // was M_plus
      firstK = k;
    } else {
      firstK = k - 1;
      R = R * 10;
      M = M * 10;
    }
    boolean low, high;
    int U;
    while (true) {
      // Set U to floor(R/S) and R to the remainder, using *unsigned* 64-bit division
      U = 0;
      for (int i = 3; i >= 0; i--) {
        long remainder = R - (S << i);
        if (remainder >= 0) {
          R = remainder;
          U += 1 << i;
        }
      }
      low = R < M; // was M_minus
      high = R + M > S; // was M_plus
      if (low || high) {
        break;
      }
      R = R * 10;
      M = M * 10;
      digits[digitCount++] = U;
    }
    if (low && !high) {
      digits[digitCount++] = U;
    } else if (high && !low) {
      digits[digitCount++] = U + 1;
    } else if ((R << 1) < S) {
      digits[digitCount++] = U;
    } else {
      digits[digitCount++] = U + 1;
    }
  }

  // Ported from android-platform-libcore/luni/src/main/native/java_lang_RealToString.cpp
  private static void bigIntDigitGenerator(long f, int e, boolean isDenormalized, int p) {
    final int RM_SIZE = 21;
    final int STemp_SIZE = 22;

    int RLength, SLength, TempLength, mplus_Length, mminus_Length;
    boolean high, low;
    int i;
    int k, firstK, U;

    long[] R = new long[RM_SIZE],
        S = new long[STemp_SIZE],
        mplus = new long[RM_SIZE],
        mminus = new long[RM_SIZE],
        Temp = new long[STemp_SIZE];

    if (e >= 0) {
      R[0] = f;
      mplus[0] = mminus[0] = 1;
      simpleShiftLeftHighPrecision(mminus, RM_SIZE, e);
      if (f != (2 << (p - 1))) {
        simpleShiftLeftHighPrecision(R, RM_SIZE, e + 1);
        S[0] = 2;
        /*
         * m+ = m+ << e results in 1.0e23 to be printed as
         * 0.9999999999999999E23
         * m+ = m+ << e+1 results in 1.0e23 to be printed as
         * 1.0e23 (caused too much rounding)
         *      470fffffffffffff = 2.0769187434139308E34
         *      4710000000000000 = 2.076918743413931E34
         */
        simpleShiftLeftHighPrecision(mplus, RM_SIZE, e);
      } else {
        simpleShiftLeftHighPrecision(R, RM_SIZE, e + 2);
        S[0] = 4;
        simpleShiftLeftHighPrecision(mplus, RM_SIZE, e + 1);
      }
    } else {
      if (isDenormalized || (f != (2 << (p - 1)))) {
        R[0] = f << 1;
        S[0] = 1;
        simpleShiftLeftHighPrecision(S, STemp_SIZE, 1 - e);
        mplus[0] = mminus[0] = 1;
      } else {
        R[0] = f << 2;
        S[0] = 1;
        simpleShiftLeftHighPrecision(S, STemp_SIZE, 2 - e);
        mplus[0] = 2;
        mminus[0] = 1;
      }
    }

    k = (int) Math.ceil((e + p - 1) * invLogOfTenBaseTwo - 1e-10);

    if (k > 0) {
      timesTenToTheEHighPrecision(S, STemp_SIZE, k);
    } else {
      timesTenToTheEHighPrecision(R, RM_SIZE, -k);
      timesTenToTheEHighPrecision(mplus, RM_SIZE, -k);
      timesTenToTheEHighPrecision(mminus, RM_SIZE, -k);
    }

    RLength = mplus_Length = mminus_Length = RM_SIZE;
    SLength = TempLength = STemp_SIZE;

    // memset(Temp + RM_SIZE, 0, (STemp_SIZE - RM_SIZE) * sizeof(uint64_t));
    Arrays.fill(Temp, RM_SIZE, STemp_SIZE, 0);
    // memcpy(Temp, R, RM_SIZE * sizeof(uint64_t));
    System.arraycopy(R, 0, Temp, 0, RM_SIZE);

    while (RLength > 1 && R[RLength - 1] == 0) {
      --RLength;
    }
    while (mplus_Length > 1 && mplus[mplus_Length - 1] == 0) {
      --mplus_Length;
    }
    while (mminus_Length > 1 && mminus[mminus_Length - 1] == 0) {
      --mminus_Length;
    }
    while (SLength > 1 && S[SLength - 1] == 0) {
      --SLength;
    }
    TempLength = (RLength > mplus_Length ? RLength : mplus_Length) + 1;
    addHighPrecision(Temp, TempLength, mplus, mplus_Length);

    if (compareHighPrecision(Temp, TempLength, S, SLength) >= 0) {
      firstK = k;
    } else {
      firstK = k - 1;
      simpleAppendDecimalDigitHighPrecision(R, ++RLength, 0);
      simpleAppendDecimalDigitHighPrecision(mplus, ++mplus_Length, 0);
      simpleAppendDecimalDigitHighPrecision(mminus, ++mminus_Length, 0);
      while (RLength > 1 && R[RLength - 1] == 0) {
        --RLength;
      }
      while (mplus_Length > 1 && mplus[mplus_Length - 1] == 0) {
        --mplus_Length;
      }
      while (mminus_Length > 1 && mminus[mminus_Length - 1] == 0) {
        --mminus_Length;
      }
    }

    int digitCount = 0;
    do {
      U = 0;
      for (i = 3; i >= 0; --i) {
        TempLength = SLength + 1;
        Temp[SLength] = 0;
        // memcpy(Temp, S, SLength * sizeof(uint64_t));
        System.arraycopy(S, 0, Temp, 0, SLength);
        simpleShiftLeftHighPrecision(Temp, TempLength, i);
        if (compareHighPrecision(R, RLength, Temp, TempLength) >= 0) {
          subtractHighPrecision(R, RLength, Temp, TempLength);
          U += 1 << i;
        }
      }

      low = compareHighPrecision(R, RLength, mminus, mminus_Length) <= 0;

      // memset(Temp + RLength, 0, (STemp_SIZE - RLength) * sizeof(uint64_t));
      Arrays.fill(Temp, RLength, STemp_SIZE, 0);
      // memcpy(Temp, R, RLength * sizeof(uint64_t));
      System.arraycopy(R, 0, Temp, 0, RLength);
      TempLength = (RLength > mplus_Length ? RLength : mplus_Length) + 1;
      addHighPrecision(Temp, TempLength, mplus, mplus_Length);

      high = compareHighPrecision(Temp, TempLength, S, SLength) >= 0;

      if (low || high) {
        break;
      }

      simpleAppendDecimalDigitHighPrecision(R, ++RLength, 0);
      simpleAppendDecimalDigitHighPrecision(mplus, ++mplus_Length, 0);
      simpleAppendDecimalDigitHighPrecision(mminus, ++mminus_Length, 0);
      while (RLength > 1 && R[RLength - 1] == 0) {
        --RLength;
      }
      while (mplus_Length > 1 && mplus[mplus_Length - 1] == 0) {
        --mplus_Length;
      }
      while (mminus_Length > 1 && mminus[mminus_Length - 1] == 0) {
        --mminus_Length;
      }
      digits[digitCount++] = U;
    } while (true);

    simpleShiftLeftHighPrecision(R, ++RLength, 1);
    if (low && !high) {
      digits[digitCount++] = U;
    } else if (high && !low) {
      digits[digitCount++] = U + 1;
    } else if (compareHighPrecision(R, RLength, S, SLength) < 0) {
      digits[digitCount++] = U;
    } else {
      digits[digitCount++] = U + 1;
    }

    RealToString.digitCount = digitCount;
    RealToString.firstK = firstK;
  }

  private static boolean addHighPrecision(long[] arg1, int length1, long[] arg2, int length2) {
    long temp1, temp2, temp3; /* temporary variables to help the SH-4, and gcc */
    long carry;
    int index;

    if (length1 == 0 || length2 == 0) {
      return false;
    } else if (length1 < length2) {
      length2 = length1;
    }

    carry = 0;
    index = 0;
    do {
      temp1 = arg1[index];
      temp2 = arg2[index];
      temp3 = temp1 + temp2;
      arg1[index] = temp3 + carry;
      if (lessThanUnsigned(arg2[index], arg1[index])) {
        carry = 0;
      } else if (arg2[index] != arg1[index]) {
        carry = 1;
      }
    } while (++index < length2);
    if (carry == 0) {
      return false;
    } else if (index == length1) {
      return true;
    }

    while (++arg1[index] == 0 && ++index < length1) {}

    return index == length1;
  }

  private static void subtractHighPrecision(long[] arg1, int length1, long[] arg2, int length2) {
    /* assumes arg1 > arg2 */
    int index;
    for (index = 0; index < length1; ++index) {
      arg1[index] = ~arg1[index];
    }
    simpleAddHighPrecision(arg1, length1, 1);

    while (length2 > 0 && arg2[length2 - 1] == 0) {
      --length2;
    }

    addHighPrecision(arg1, length1, arg2, length2);

    for (index = 0; index < length1; ++index) {
      arg1[index] = ~arg1[index];
    }
    simpleAddHighPrecision(arg1, length1, 1);
  }

  private static int compareHighPrecision(long[] arg1, int length1, long[] arg2, int length2) {
    while (--length1 >= 0 && arg1[length1] == 0) {}
    while (--length2 >= 0 && arg2[length2] == 0) {}

    if (length1 > length2) {
      return 1;
    } else if (length1 < length2) {
      return -1;
    } else if (length1 > -1) {
      do {
        if (lessThanUnsigned(arg2[length1], arg1[length1])) {
          return 1;
        } else if (arg1[length1] != arg2[length1]) {
          return -1;
        }
      } while (--length1 >= 0);
    }

    return 0;
  }

  private static boolean simpleAddHighPrecision(long[] arg1, int length, long arg2) {
    /* assumes length > 0 */
    int index = 1;

    arg1[0] += arg2;
    if (lessThanOrEqualUnsigned(arg2, arg1[0])) {
      return false;
    } else if (length == 1) {
      return true;
    }

    while (++arg1[index] == 0 && ++index < length) {}

    return index == length;
  }

  private static final long TEN_E1 = 0xAL;
  private static final long TEN_E2 = 0x64L;
  private static final long TEN_E3 = 0x3E8L;
  private static final long TEN_E4 = 0x2710L;
  private static final long TEN_E5 = 0x186A0L;
  private static final long TEN_E6 = 0xF4240L;
  private static final long TEN_E7 = 0x989680L;
  private static final long TEN_E8 = 0x5F5E100L;
  private static final long TEN_E9 = 0x3B9ACA00L;
  private static final long TEN_E19 = 0x8AC7230489E80000L;

  private static int timesTenToTheEHighPrecision(long[] result, int length, int e) {
    /* assumes result can hold value */
    long overflow;
    int exp10 = e;

    if (e == 0) {
      return length;
    }

    /* Replace the current implementaion which performs a
     * "multiplication" by 10 e number of times with an actual
     * multiplication. 10e19 is the largest exponent to the power of ten
     * that will fit in a 64-bit integer, and 10e9 is the largest exponent to
     * the power of ten that will fit in a 64-bit integer. Not sure where the
     * break-even point is between an actual multiplication and a
     * simpleAappendDecimalDigit() so just pick 10e3 as that point for
     * now.
     */
    while (exp10 >= 19) {
      overflow = simpleMultiplyHighPrecision64(result, length, TEN_E19);
      if (overflow != 0) {
        result[length++] = overflow;
      }
      exp10 -= 19;
    }
    while (exp10 >= 9) {
      overflow = simpleMultiplyHighPrecision(result, length, TEN_E9);
      if (overflow != 0) {
        result[length++] = overflow;
      }
      exp10 -= 9;
    }
    if (exp10 == 0) {
      return length;
    } else if (exp10 == 1) {
      overflow = simpleAppendDecimalDigitHighPrecision(result, length, 0);
      if (overflow != 0) {
        result[length++] = overflow;
      }
    } else if (exp10 == 2) {
      overflow = simpleAppendDecimalDigitHighPrecision(result, length, 0);
      if (overflow != 0) {
        result[length++] = overflow;
      }
      overflow = simpleAppendDecimalDigitHighPrecision(result, length, 0);
      if (overflow != 0) {
        result[length++] = overflow;
      }
    } else if (exp10 == 3) {
      overflow = simpleMultiplyHighPrecision(result, length, TEN_E3);
      if (overflow != 0) {
        result[length++] = overflow;
      }
    } else if (exp10 == 4) {
      overflow = simpleMultiplyHighPrecision(result, length, TEN_E4);
      if (overflow != 0) {
        result[length++] = overflow;
      }
    } else if (exp10 == 5) {
      overflow = simpleMultiplyHighPrecision(result, length, TEN_E5);
      if (overflow != 0) {
        result[length++] = overflow;
      }
    } else if (exp10 == 6) {
      overflow = simpleMultiplyHighPrecision(result, length, TEN_E6);
      if (overflow != 0) {
        result[length++] = overflow;
      }
    } else if (exp10 == 7) {
      overflow = simpleMultiplyHighPrecision(result, length, TEN_E7);
      if (overflow != 0) {
        result[length++] = overflow;
      }
    } else if (exp10 == 8) {
      overflow = simpleMultiplyHighPrecision(result, length, TEN_E8);
      if (overflow != 0) {
        result[length++] = overflow;
      }
    }
    return length;
  }

  private static int simpleAppendDecimalDigitHighPrecision(long[] arg1, int length, long digit) {
    /* assumes digit is less than 32 bits */
    long arg;
    int index = 0;

    digit <<= 32;
    do {
      long x = arg1[index];
      arg = LOW_IN_U64(x);
      digit = HIGH_IN_U64(digit) + arg * 10;
      // LOW_U32_FROM_PTR(arg1 + index) = LOW_U32_FROM_VAR(digit);
      x = LongUtils.fromBits((int) digit, LongUtils.getHighBits(x));

      arg = HIGH_IN_U64(x);
      digit = HIGH_IN_U64(digit) + arg * 10;
      // HIGH_U32_FROM_PTR(arg1 + index) = LOW_U32_FROM_VAR(digit);
      arg1[index] = LongUtils.fromBits((int) x, (int) digit);
    } while (++index < length);

    //  return HIGH_U32_FROM_VAR (digit);
    return LongUtils.getHighBits(digit);
  }

  private static long simpleMultiplyHighPrecision64(long[] arg1, int length, long arg2) {
    long intermediate, carry1, carry2, prod1, prod2, sum;
    int index;
    int buf32;

    index = 0;
    intermediate = 0;

    carry1 = carry2 = 0;

    do {
      long x = arg1[index];
      if ((x != 0) || (intermediate != 0)) {
        // prod1 = static_cast<uint64_t>(LOW_U32_FROM_VAR (arg2)) *
        //    static_cast<uint64_t>(LOW_U32_FROM_PTR (pArg1));
        prod1 = LOW_IN_U64(arg2) * LOW_IN_U64(x);
        sum = intermediate + prod1;
        if (lessThanUnsigned(sum, prod1) || lessThanUnsigned(sum, intermediate)) {
          carry1 = 1;
        } else {
          carry1 = 0;
        }
        // prod1 = static_cast<uint64_t>(LOW_U32_FROM_VAR (arg2)) *
        //    static_cast<uint64_t>(HIGH_U32_FROM_PTR (pArg1));
        prod1 = LOW_IN_U64(arg2) * HIGH_IN_U64(x);
        //  prod2 = static_cast<uint64_t>(HIGH_U32_FROM_VAR (arg2)) *
        //     static_cast<uint64_t>(LOW_U32_FROM_PTR (pArg1));
        prod2 = HIGH_IN_U64(arg2) * LOW_IN_U64(x);
        //  intermediate = carry2 + HIGH_IN_U64 (sum) + prod1 + prod2;
        intermediate = carry2 + HIGH_IN_U64(sum) + prod1 + prod2;
        if (lessThanUnsigned(intermediate, prod1) || lessThanUnsigned(intermediate, prod2)) {
          carry2 = 1;
        } else {
          carry2 = 0;
        }
        // LOW_U32_FROM_PTR(pArg1) = LOW_U32_FROM_VAR(sum);
        // buf32 = HIGH_U32_FROM_PTR (pArg1);
        // HIGH_U32_FROM_PTR(pArg1) = LOW_U32_FROM_VAR(intermediate);
        buf32 = LongUtils.getHighBits(x);
        arg1[index] = LongUtils.fromBits((int) sum, (int) intermediate);

        // intermediate = carry1 + HIGH_IN_U64 (intermediate)
        //    + static_cast<uint64_t>(HIGH_U32_FROM_VAR (arg2)) * static_cast<uint64_t>(buf32);
        intermediate =
            carry1 + HIGH_IN_U64(intermediate) + HIGH_IN_U64(arg2) * LongUtils.fromBits(buf32, 0);
      }
    } while (++index < length);

    return intermediate;
  }

  private static int simpleMultiplyHighPrecision(long[] arg1, int length, long arg2) {
    /* assumes arg2 only holds 32 bits of information */
    long product;
    int index;

    index = 0;
    product = 0;

    do {
      long x = arg1[index];
      // product = HIGH_IN_U64 (product) + arg2 * LOW_U32_FROM_PTR (arg1 + index);
      product = HIGH_IN_U64(product) + arg2 * LOW_IN_U64(x);
      // LOW_U32_FROM_PTR(arg1 + index) = LOW_U32_FROM_VAR(product);
      x = LongUtils.fromBits((int) product, LongUtils.getHighBits(x));
      // product = HIGH_IN_U64(product) + arg2 * HIGH_U32_FROM_PTR(arg1 + index);
      product = HIGH_IN_U64(product) + arg2 * HIGH_IN_U64(x);
      // HIGH_U32_FROM_PTR(arg1 + index) = LOW_U32_FROM_VAR(product);
      arg1[index] = LongUtils.fromBits((int) x, (int) product);

    } while (++index < length);

    return LongUtils.getHighBits(product);
  }

  private static void simpleShiftLeftHighPrecision(long[] arg1, int length, int arg2) {
    /* assumes length > 0 */
    int index, offset;
    if (arg2 >= 64) {
      offset = arg2 >>> 6;
      index = length;

      while (--index - offset >= 0) {
        arg1[index] = arg1[index - offset];
      }
      do {
        arg1[index] = 0;
      } while (--index >= 0);

      arg2 &= 0x3F;
    }

    if (arg2 == 0) {
      return;
    }
    while (--length > 0) {
      arg1[length] = arg1[length] << arg2 | arg1[length - 1] >>> (64 - arg2);
    }
    arg1[0] <<= arg2;
  }

  private static long LOW_IN_U64(long l) {
    return l & 0x00000000FFFFFFFFL;
  }

  private static long HIGH_IN_U64(long l) {
    return l >>> 32;
  }

  private static boolean lessThanUnsigned(long x, long y) {
    return x + Long.MIN_VALUE < y + Long.MIN_VALUE;
  }

  private static boolean lessThanOrEqualUnsigned(long x, long y) {
    return x + Long.MIN_VALUE <= y + Long.MIN_VALUE;
  }
}
