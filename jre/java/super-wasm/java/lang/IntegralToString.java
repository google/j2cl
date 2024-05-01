/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package java.lang;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;

/**
 * Converts integral types to strings. This class is public but hidden so that it can also be used
 * by java.util.Formatter to speed up %d. This class is in java.lang so that it can take advantage
 * of the package-private String constructor.
 *
 * <p>The most important methods are appendInt/appendLong and intToString(int)/longToString(int).
 * The former are used in the implementation of StringBuilder, StringBuffer, and Formatter, while
 * the latter are used by Integer.toString and Long.toString.
 *
 * <p>The append methods take AbstractStringBuilder rather than Appendable because the latter
 * requires CharSequences, while we only have raw char[]s. Since much of the savings come from not
 * creating any garbage, we can't afford temporary CharSequence instances.
 *
 * <p>One day the performance advantage of the binary/hex/octal specializations will be small enough
 * that we can lose the duplication, but until then this class offers the full set.
 */
final class IntegralToString {

  private static final int BUFFER_LENGTH = 65;
  // Note that we can reuse the instance since the String construction will copy the array.
  private static final char[] BUFFER = new char[BUFFER_LENGTH];

  /** TENS[i] contains the tens digit of the number i, 0 <= i <= 99. */
  private static final char[] TENS = {
    '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
    '1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
    '2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
    '3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
    '4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
    '5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
    '6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
    '7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
    '8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
    '9', '9', '9', '9', '9', '9', '9', '9', '9', '9'
  };

  /** Ones [i] contains the tens digit of the number i, 0 <= i <= 99. */
  private static final char[] ONES = {
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
  };

  /**
   * Table for MOD / DIV 10 computation described in Section 10-21 of Hank Warren's "Hacker's
   * Delight" online addendum. http://www.hackersdelight.org/divcMore.pdf
   */
  private static final char[] MOD_10_TABLE = {0, 1, 2, 2, 3, 3, 4, 5, 5, 6, 7, 7, 8, 8, 9, 0};

  /** The digits for every supported radix. */
  private static final char[] DIGITS = {
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
    'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
    'u', 'v', 'w', 'x', 'y', 'z'
  };

  private IntegralToString() {}

  public static String intToString(int i) {
    return intToString(i, 10);
  }

  public static String intToString(int i, int radix) {
    return fromNumber(i, radix);
  }

  @JsMethod(namespace = JsPackage.GLOBAL, name = "Number.prototype.toString.call")
  private static native String fromNumber(int i, int radix);

  private static final int SMI_MAX = Integer.MAX_VALUE >> 1;
  private static final int SMI_MIN = Integer.MIN_VALUE >> 1;

  public static void appendInt(AbstractStringBuilder sb, int i) {
    // Only use fast path for SMI numbers, otherwise JS is very slow.
    if (SMI_MIN <= i && i <= SMI_MAX) {
      sb.append0(intToString(i));
      return;
    }

    boolean negative = i < 0;
    if (negative) {
      i = -i;
      if (i < 0) {
        // If it is still negative, it is the MIN_VALUE
        sb.append0("-2147483648");
        return;
      }
    }
    int bufLen = BUFFER_LENGTH;
    char[] buf = BUFFER;
    int cursor = bufLen;
    // Calculate digits two-at-a-time till remaining digits fit in 16 bits
    while (i >= (1 << 16)) {
      // Compute q = n/100 and r = n % 100 as per "Hacker's Delight" 10-8
      int q = (int) ((0x51EB851FL * i) >>> 37);
      int r = i - 100 * q;
      buf[--cursor] = ONES[r];
      buf[--cursor] = TENS[r];
      i = q;
    }
    // Calculate remaining digits one-at-a-time for performance
    while (i != 0) {
      // Compute q = n/10 and r = n % 10 as per "Hacker's Delight" 10-8
      int q = (0xCCCD * i) >>> 19;
      int r = i - 10 * q;
      buf[--cursor] = DIGITS[r];
      i = q;
    }
    if (negative) {
      buf[--cursor] = '-';
    }
    sb.append0(buf, cursor, bufLen - cursor);
  }

  public static String longToString(long v, int radix) {
    int i = (int) v;
    if (i == v) {
      return intToString(i, radix);
    }

    if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
      radix = 10;
    }
    if (radix == 10) {
      return longToString(v);
    }

    /*
     * If v is positive, negate it. This is the opposite of what one might
     * expect. It is necessary because the range of the negative values is
     * strictly larger than that of the positive values: there is no
     * positive value corresponding to Integer.MIN_VALUE.
     */
    boolean negative = false;
    if (v < 0) {
      negative = true;
    } else {
      v = -v;
    }

    int bufLen = BUFFER_LENGTH;
    char[] buf = BUFFER;
    int cursor = bufLen;

    do {
      long q = v / radix;
      buf[--cursor] = DIGITS[(int) (radix * q - v)];
      v = q;
    } while (v != 0);

    if (negative) {
      buf[--cursor] = '-';
    }

    return new String(buf, cursor, bufLen - cursor);
  }

  /** Equivalent to Long.toString(l). */
  public static String longToString(long n) {
    int i = (int) n;
    if (i == n) {
      return intToString(i);
    }
    boolean negative = (n < 0);
    if (negative) {
      n = -n;
      if (n < 0) {
        // If -n is still negative, n is Long.MIN_VALUE
        return "-9223372036854775808";
      }
    }
    int bufLen = 20; // Maximum number of chars in result
    char[] buf = BUFFER;
    int low = (int) (n % 1000000000); // Extract low-order 9 digits
    int cursor = intIntoCharArray(buf, bufLen, low);
    // Zero-pad Low order part to 9 digits
    while (cursor != (bufLen - 9)) {
      buf[--cursor] = '0';
    }
    /*
     * The remaining digits are (n - low) / 1,000,000,000.  This
     * "exact division" is done as per the online addendum to Hank Warren's
     * "Hacker's Delight" 10-20, http://www.hackersdelight.org/divcMore.pdf
     */
    n = ((n - low) >>> 9) * 0x8E47CE423A2E9C6DL;
    /*
     * If the remaining digits fit in an int, emit them using a
     * single call to intIntoCharArray. Otherwise, strip off the
     * low-order digit, put it in buf, and then call intIntoCharArray
     * on the remaining digits (which now fit in an int).
     */
    if ((n & (-1L << 32)) == 0) {
      cursor = intIntoCharArray(buf, cursor, (int) n);
    } else {
      /*
       * Set midDigit to n % 10
       */
      int lo32 = (int) n;
      int hi32 = (int) (n >>> 32);
      // midDigit = ((unsigned) low32) % 10, per "Hacker's Delight" 10-21
      int midDigit = MOD_10_TABLE[(0x19999999 * lo32 + (lo32 >>> 1) + (lo32 >>> 3)) >>> 28];
      // Adjust midDigit for hi32. (assert hi32 == 1 || hi32 == 2)
      midDigit -= hi32 << 2; // 1L << 32 == -4 MOD 10
      if (midDigit < 0) {
        midDigit += 10;
      }
      buf[--cursor] = DIGITS[midDigit];
      // Exact division as per Warren 10-20
      int rest = ((int) ((n - midDigit) >>> 1)) * 0xCCCCCCCD;
      cursor = intIntoCharArray(buf, cursor, rest);
    }
    if (negative) {
      buf[--cursor] = '-';
    }
    return new String(buf, cursor, bufLen - cursor);
  }
  /**
   * Inserts the unsigned decimal integer represented by n into the specified character array
   * starting at position cursor. Returns the index after the last character inserted (i.e., the
   * value to pass in as cursor the next time this method is called). Note that n is interpreted as
   * a large positive integer (not a negative integer) if its sign bit is set.
   */
  private static int intIntoCharArray(char[] buf, int cursor, int n) {
    // Calculate digits two-at-a-time till remaining digits fit in 16 bits
    while ((n & 0xffff0000) != 0) {
      /*
       * Compute q = n/100 and r = n % 100 as per "Hacker's Delight" 10-8.
       * This computation is slightly different from the corresponding
       * computation in intToString: the shifts before and after
       * multiply can't be combined, as that would yield the wrong result
       * if n's sign bit were set.
       */
      int q = (int) ((0x51EB851FL * (n >>> 2)) >>> 35);
      int r = n - 100 * q;
      buf[--cursor] = ONES[r];
      buf[--cursor] = TENS[r];
      n = q;
    }
    // Calculate remaining digits one-at-a-time for performance
    while (n != 0) {
      // Compute q = n / 10 and r = n % 10 as per "Hacker's Delight" 10-8
      int q = (0xCCCD * n) >>> 19;
      int r = n - 10 * q;
      buf[--cursor] = DIGITS[r];
      n = q;
    }
    return cursor;
  }

  public static String intToBinaryString(int value) {
    return intToPowerOfTwoUnsignedString(value, 1);
  }

  public static String intToHexString(int value) {
    return intToPowerOfTwoUnsignedString(value, 4);
  }

  public static String intToOctalString(int value) {
    return intToPowerOfTwoUnsignedString(value, 3);
  }

  private static String intToPowerOfTwoUnsignedString(int value, int shift) {
    final int radix = 1 << shift;
    final int mask = radix - 1;
    final int bufSize = BUFFER_LENGTH;
    char[] buf = BUFFER;
    int pos = bufSize;
    do {
      buf[--pos] = Character.forDigit(value & mask);
      value >>>= shift;
    } while (value != 0);

    return String.valueOf(buf, pos, bufSize - pos);
  }

  public static String longToBinaryString(long value) {
    return longToPowerOfTwoUnsignedString(value, 1);
  }

  public static String longToHexString(long value) {
    return longToPowerOfTwoUnsignedString(value, 4);
  }

  public static String longToOctalString(long value) {
    return longToPowerOfTwoUnsignedString(value, 3);
  }

  private static String longToPowerOfTwoUnsignedString(long value, int shift) {
    final int radix = 1 << shift;
    final int mask = radix - 1;
    final int bufSize = BUFFER_LENGTH;
    char[] buf = BUFFER;
    int pos = bufSize;
    do {
      buf[--pos] = Character.forDigit(((int) value) & mask);
      value >>>= shift;
    } while (value != 0);

    return String.valueOf(buf, pos, bufSize - pos);
  }
}
