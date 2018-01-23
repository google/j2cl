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

/** Test basic binary operations. This test does not aim to test primitive overflow and Coercion. */
@SuppressWarnings({"NarrowingCompoundAssignment", "ReferenceEquality", "ConstantOverflow"})
public class Main {
  public static void main(String[] args) {
    testArithmetic();
    testOverflow();
    testBooleanOperations();
    testStringConcatentation();
    testExtendedOperands();
  }

  public static void testArithmetic() {
    int a = 6;
    int b = 3;
    int c = -1;
    assert a * b == 18;
    assert a / b == 2;
    assert a % b == 0;
    assert a + b == 9;
    assert a - b == 3;
    assert a << 1 == 12;
    assert c >> 16 == -1;
    assert c >>> 16 == 65535;
    assert a > b;
    assert a >= b;
    assert b < a;
    assert b <= a;
    assert a != b;
    assert (a ^ b) == 5;
    assert (a & b) == 2;
    assert (a | b) == 7;
    assert (a > b) && (a == 6);
    assert (a < b) || (a == 6);
    assert (-1 >>> 0) == -1;

    assert a + b + a - b == 12;
    assert (a + b) * (a - b) == 27;

    assert ((5 / 2) - 0.0) == 2.0;

    int i = 1;
    i += 1L;
    assert i == 2;
    i += Double.MAX_VALUE;
    assert i == Integer.MAX_VALUE;

    int d = 10;
    assert d == 10;
    d ^= d;
    assert d == 0;
    d += 15;
    assert d == 15;
    d -= 5;
    assert d == 10;
    d *= 2;
    assert d == 20;
    d /= 4;
    assert d == 5;
    d &= 3;
    assert d == 1;
    d |= 2;
    assert d == 3;
    d %= 2;
    assert d == 1;
    d <<= 3;
    assert d == 8;
    d >>= 3;
    assert d == 1;
    d = -1;
    d >>>= 16;
    assert d == 65535;
    d = -1;
    d >>>= 0;
    assert d == -1;

    // Make sure that promotion rules from shift operations are correct.
    assert Integer.MAX_VALUE << 1L == -2;
  }

  public static void testOverflow() {
    // No overflow, as the mutiplication is always performed in integer precision
    assert Byte.MAX_VALUE * Byte.MAX_VALUE == 0x3f01;
    assert Short.MAX_VALUE * Short.MAX_VALUE == 0x3fff0001;

    // Overflow in long precision which is emulated
    assert Long.MAX_VALUE * Long.MAX_VALUE == 1L;

    // Overflow followed by an explicit int coercion results in the correct value because this
    // 32-bit integer overflow does not exceed the 52-bit JS integer precision.
    assert (Character.MAX_VALUE * Character.MAX_VALUE | 0) == -131071;

    // TODO(b/72332921): overflow in integer precision has different semantics in J2CL, the code
    // below should have been:
    // assert Character.MAX_VALUE * Character.MAX_VALUE == -131071 /*0xfffe0001*/;
    assert Character.MAX_VALUE * Character.MAX_VALUE == 4294836225d;

    // TODO(b/72332921): overflow in integer precision has different semantics in J2CL, the code
    // below should have been:
    // assert Integer.MAX_VALUE * Integer.MAX_VALUE == 1;
    assert Integer.MAX_VALUE * Integer.MAX_VALUE == 4611686014132420600d;
    // TODO(b/72332921): overflow in integer precision has different semantics in J2CL, the code
    // below should have been:
    // assert (Integer.MAX_VALUE * Integer.MAX_VALUE | 0) == 1;
    assert (Integer.MAX_VALUE * Integer.MAX_VALUE | 0) == 0;
  }

  public static void testBooleanOperations() {
    boolean bool = true;
    bool &= false;
    assert ("" + bool).equals("false");
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
    assert finalOuter.b;

    Outer outer = new Outer();
    Outer copy = outer;
    outer.b |= (outer = null) == null;
    assert copy.b;

    outer = new Outer();
    outer.new Inner();
    assert outer.b;
  }

  public static void testStringConcatentation() {
    String s = null;
    assert s + s == "nullnull";

    String[] stringArray = new String[1];
    assert stringArray[0] + stringArray[0] == "nullnull";
  }

  public static void testExtendedOperands() {
    // Binary expression from JDT InfixExpression with extended operands.
    double n = 1;
    long l = 2L;
    assert 20 + l + n == n + l + 20;
  }
}
