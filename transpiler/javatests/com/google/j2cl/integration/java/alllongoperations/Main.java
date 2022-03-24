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
package alllongoperations;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

public class Main {
  public static void main(String... args) {
    testInfixOperations();
    testPrefixOperations();
    testPostfixOperations();
    testAssignmentOperators();
    testOverAndUnderflow();
    testLiteralFormats();
    testExtendedOperands();
    testInitialization();
  }

  private static void testAssignmentOperators() {
    long a = 0L;

    a += 1L;
    assertTrue(a == 1L);

    a -= 2L;
    assertTrue(a == -1L);

    a *= 30L;
    assertTrue(a == -30L);

    a /= -10L;
    assertTrue(a == 3L);

    a &= 2L;
    assertTrue(a == 2L);

    a |= 4L;
    assertTrue(a == 6L);

    a ^= 3L;
    assertTrue(a == 5L);

    a %= 2L;
    assertTrue(a == 1L);

    a <<= 3L;
    assertTrue(a == 8L);

    a >>= 1L;
    assertTrue(a == 4L);

    a >>>= 1L;
    assertTrue(a == 2L);
  }

  private static void testExtendedOperands() {
    long a = 1234L;

    assertTrue(1254L == a + 5L + 5L + 5L + 5L);
  }

  private static void testInfixOperations() {
    long a = 1234L;
    long b = 9876L;
    long c = -55L;

    assertTrue(12186984L == (a * b));
    assertTrue(8L == (b / a));
    assertTrue(4L == (b % a));
    assertTrue(11110L == (a + b));
    assertTrue(-8642L == (a - b));
    assertTrue(9872L == (a << 3L));
    assertTrue(-7L == (c >> 3L));
    assertTrue(2305843009213693945L == (c >>> 3L));
    assertTrue(a < b);
    assertTrue(b > a);
    assertTrue(b <= b);
    assertTrue(a <= b);
    assertTrue(b >= b);
    assertTrue(b >= a);
    assertTrue(1234L == a);
    assertTrue(a != b);
    assertTrue(8774L == (a ^ b));
    assertTrue(1168L == (a & b));
    assertTrue(9942L == (a | b));
  }

  private static void testLiteralFormats() {
    // Small longs get a fromInt() fast path.
    // ************************************************************
    long a = 0x0011eeffL;
    assertTrue(1175295L == a); // Hex

    long b = 01234567L;
    assertTrue(342391L == b); // Octal

    long c = 0b00011110L;
    assertTrue(30L == c); // Binary

    // Huge longs fall back on fromString() construction.
    // ************************************************************
    long e = 0x20000000000001L;
    assertTrue(9007199254740993L == e); // Hex

    long f = 0400000000000000001L;
    assertTrue(9007199254740993L == f); // Octal

    long g = 0b100000000000000000000000000000000000000000000000000001L;
    assertTrue(9007199254740993L == g); // Binary
  }

  @SuppressWarnings("ConstantOverflow")
  private static void testOverAndUnderflow() {
    long a = 999999999999999999L * 1000L;
    assertTrue(3875820019684211736L == a);

    long b = 8999999999999999999L + 8999999999999999999L;
    assertTrue(-446744073709551618L == b);

    long c = 999999999999999999L * -1000L;
    assertTrue(-3875820019684211736L == c);

    long d = -8999999999999999999L - 8999999999999999999L;
    assertTrue(446744073709551618L == d);
  }

  private static void testPostfixOperations() {
    long a = 100L;

    assertTrue(100L == a++);
    assertTrue(101L == a--);
  }

  private static void testPrefixOperations() {
    long a = 100L;

    assertTrue(101L == ++a);
    assertTrue(100L == --a);
    assertTrue(100L == +a);
    assertTrue(-100L == -a);
    assertTrue(-101L == ~a);
  }

  private static long fieldLong = 100;

  private static long getReturnInitializerLong() {
    return 100;
  }

  private static void testInitialization() {
    long localVariableLong = 100;

    assertTrue(localVariableLong == fieldLong);
    assertTrue(fieldLong == getReturnInitializerLong());
  }
}
