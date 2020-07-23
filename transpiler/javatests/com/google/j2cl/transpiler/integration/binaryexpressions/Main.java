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
package com.google.j2cl.transpiler.integration.binaryexpressions;

import static com.google.j2cl.transpiler.utils.Asserts.assertTrue;

/** Test basic binary operations. This test does not aim to test primitive overflow and Coercion. */
@SuppressWarnings({"NarrowingCompoundAssignment", "ReferenceEquality", "ConstantOverflow"})
public class Main {
  public static void main(String[] args) {
    testArithmetic();
    testOverflow();
    testBooleanOperations();
    testStringConcatentation();
    testExtendedOperands();
    testFloatConsistency();
    testParenthesizedLvalues();
  }

  private static void testArithmetic() {
    int a = 6;
    int b = 3;
    int c = -1;
    assertTrue(a * b == 18);
    assertTrue(a / b == 2);
    assertTrue(a % b == 0);
    assertTrue(a + b == 9);
    assertTrue(a - b == 3);
    assertTrue(a << 1 == 12);
    assertTrue(c >> 16 == -1);
    assertTrue(c >>> 16 == 65535);
    assertTrue(a > b);
    assertTrue(a >= b);
    assertTrue(b < a);
    assertTrue(b <= a);
    assertTrue(a != b);
    assertTrue((a ^ b) == 5);
    assertTrue((a & b) == 2);
    assertTrue((a | b) == 7);
    assertTrue((a > b) && (a == 6));
    assertTrue((a < b) || (a == 6));
    assertTrue((-1 >>> 0) == -1);

    assertTrue(a + b + a - b == 12);
    assertTrue((a + b) * (a - b) == 27);

    assertTrue(((5 / 2) - 0.0) == 2.0);

    int i = 1;
    i += 1L;
    assertTrue(i == 2);
    i += Double.MAX_VALUE;
    assertTrue(i == Integer.MAX_VALUE);

    int d = 10;
    assertTrue(d == 10);
    d ^= d;
    assertTrue(d == 0);
    d += 15;
    assertTrue(d == 15);
    d -= 5;
    assertTrue(d == 10);
    d *= 2;
    assertTrue(d == 20);
    d /= 4;
    assertTrue(d == 5);
    d &= 3;
    assertTrue(d == 1);
    d |= 2;
    assertTrue(d == 3);
    d %= 2;
    assertTrue(d == 1);
    d <<= 3;
    assertTrue(d == 8);
    d >>= 3;
    assertTrue(d == 1);
    d = -1;
    d >>>= 16;
    assertTrue(d == 65535);
    d = -1;
    d >>>= 0;
    assertTrue(d == -1);

    // Make sure that promotion rules from shift operations are correct.
    assertTrue(Integer.MAX_VALUE << 1L == -2);
  }

  private static void testOverflow() {
    // No overflow, as the mutiplication is always performed in integer precision
    assertTrue(Byte.MAX_VALUE * Byte.MAX_VALUE == 0x3f01);
    assertTrue(Short.MAX_VALUE * Short.MAX_VALUE == 0x3fff0001);

    // Overflows.
    assertTrue(Long.MAX_VALUE * Long.MAX_VALUE == 1L);
    assertTrue(Character.MAX_VALUE * Character.MAX_VALUE == -131071 /*0xfffe0001*/);
    assertTrue(Integer.MAX_VALUE * Integer.MAX_VALUE == 1);

    assertTrue(Integer.MAX_VALUE + Integer.MAX_VALUE == -2);
    assertTrue(Integer.MAX_VALUE + Integer.MAX_VALUE == -2.0d);
  }

  public static void testBooleanOperations() {
    boolean bool = true;
    bool &= false;
    assertTrue(("" + bool).equals("false"));
    // Compound assignment with enclosing instance.
    class Outer {

      boolean b;

      class Inner {

        {
          b |= true;
        }
      }
    }

    final Outer finalOuter = new Outer();
    finalOuter.b |= true;
    assertTrue(finalOuter.b);

    Outer outer = new Outer();
    Outer copy = outer;
    outer.b |= (outer = null) == null;
    assertTrue(copy.b);

    outer = new Outer();
    outer.new Inner();
    assertTrue(outer.b);
  }

  private static void testStringConcatentation() {
    String s = null;
    assertTrue(s + s == "nullnull");

    String[] stringArray = new String[1];
    assertTrue(stringArray[0] + stringArray[0] == "nullnull");
  }

  private static void testExtendedOperands() {
    // Binary expression from JDT InfixExpression with extended operands.
    double n = 1;
    long l = 2L;
    assertTrue(20 + l + n == n + l + 20);
  }

  private static final float FLOAT_CONSTANT = 1.1f;
  private static final double DOUBLE_CONSTANT = FLOAT_CONSTANT;
  private static final double SUM = FLOAT_CONSTANT + FLOAT_CONSTANT;

  private static void testFloatConsistency() {
    float floatSum = FLOAT_CONSTANT + FLOAT_CONSTANT;
    assertTrue(floatSum == FLOAT_CONSTANT + FLOAT_CONSTANT);
    assertTrue(floatSum == SUM);
    assertTrue(DOUBLE_CONSTANT == FLOAT_CONSTANT);
  }

  private static void testParenthesizedLvalues() {
    long l = 1;
    ((l))++;
    assertTrue(l == 2);

    ((((l)))) += 2;
    assertTrue(l == 4);
  }
}
