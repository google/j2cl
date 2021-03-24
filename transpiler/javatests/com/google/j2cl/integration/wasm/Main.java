/*
 * Copyright 2021 Google Inc.
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
package com.google.j2cl.integration.wasm;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertFalse;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;
import static com.google.j2cl.integration.testing.Asserts.fail;

import javaemul.internal.annotations.Wasm;
import jsinterop.annotations.JsMethod;

/**
 * Incrementally tests wasm features as they are being added.
 *
 * <p>This test will be removed when all WASM features are implemented and all integration tests are
 * enabled for WASM.
 */
public class Main {

  public static void main(String... args) {
    testDynamicClassMethodDispatch();
    testSwitch();
    testWasmAnnotation();
    testMathLogAndFriends();
    testClassLiterals();
    testTry();
  }

  static class A {
    public int m() {
      return 1;
    }
  }

  static class B extends A {
    @Override
    public int m() {
      return 2;
    }
  }

  private static void testDynamicClassMethodDispatch() {
    A a = new A();
    B b = new B();
    assertEquals(1, a.m());
    assertEquals(2, b.m());
    a = b;
    assertEquals(2, b.m());
  }

  enum MyEnum {
    A,
    B;
  }

  private static int next = 0;

  private static int returnsNext() {
    return next++;
  }

  private static void testSwitch() {
    // returnsNext = 0;
    switch (returnsNext()) {
      case 1:
        fail();
      case 0:
        break;
      default:
        fail();
    }

    // returnsNext() = 1
    switch (returnsNext()) {
      case 2:
        fail();
      default:
        break;
      case 0:
        fail();
    }

    MyEnum a = MyEnum.A;
    switch (a) {
      case B:
        fail();
      case A:
        break;
      default:
        fail();
    }
  }

  private static void testWasmAnnotation() {
    assertTrue(42 == multiply(6, 7));
  }

  @JsMethod // Exist to keep to test running under closure output
  @Wasm("i32.mul")
  private static native int multiply(int x, int y);

  private static void testMathLogAndFriends() {
    assertEquals(Double.NaN, Math.log(Double.NaN));
    assertEquals(Double.NaN, Math.log(Double.NEGATIVE_INFINITY));
    assertEquals(Double.NaN, Math.log(-1));
    assertEquals(Double.POSITIVE_INFINITY, Math.log(Double.POSITIVE_INFINITY));
    assertEquals(Double.NEGATIVE_INFINITY, Math.log(0.0));
    assertEquals(Double.NEGATIVE_INFINITY, Math.log(-0.0));
    assertEqualsDelta(1.0, Math.log(Math.E), 1e-15);

    assertEquals(Double.NaN, Math.log10(-2541.057456872342));
    assertEquals(Double.NaN, Math.log10(-0.1));
    assertEquals(Double.POSITIVE_INFINITY, Math.log10(Double.POSITIVE_INFINITY));
    assertEquals(Double.NEGATIVE_INFINITY, Math.log10(0.0));
    assertEquals(Double.NEGATIVE_INFINITY, Math.log10(-0.0));
    assertEqualsDelta(3.0, Math.log10(1000.0), 1e-15);
    assertEqualsDelta(3.73895612695404, Math.log10(5482.2158), 1e-15);
    assertEquals(308.25471555991675, Math.log10(Double.MAX_VALUE));
    assertEqualsDelta(-323.30621534311575, Math.log10(Double.MIN_VALUE), 1e-10);

    assertEquals(Double.NaN, Math.log1p(Double.NaN));
    assertEquals(Double.NaN, Math.log1p(-2));
    assertEquals(Double.NaN, Math.log1p(Double.NEGATIVE_INFINITY));
    assertEquals(Double.POSITIVE_INFINITY, Math.log1p(Double.POSITIVE_INFINITY));
    assertEquals(Double.NEGATIVE_INFINITY, Math.log1p(-1));
    assertEqualsDelta(Double.MIN_VALUE, Math.log1p(Double.MIN_VALUE), 1e-25);
    assertEquals(709.782712893384, Math.log1p(Double.MAX_VALUE));
    assertEquals(0.0, Math.log1p(0.0));
    assertEquals(-0.0, Math.log1p(-0.0));

    assertEqualsDelta(-0.693147180, Math.log1p(-0.5), 1e-7);
    assertEqualsDelta(1.313261687, Math.log1p(Math.E), 1e-7);
    assertEqualsDelta(-0.2941782295312541, Math.log1p(-0.254856327), 1e-7);
    assertEquals(7.368050685564151, Math.log1p(1583.542));
    assertEqualsDelta(0.4633708685409921, Math.log1p(0.5894227), 1e-15);
  }

  private static void assertEqualsDelta(double expected, double actual, double delta) {
    if (Double.compare(expected, actual) == 0) {
      return;
    }
    if ((Math.abs(expected - actual) <= delta)) {
      return;
    }
    fail();
  }

  private static class SomeClass {}

  private interface SomeInterface {}

  private enum SomeEnum {
    A
  }

  private static void testClassLiterals() {
    assertEquals("com.google.j2cl.integration.wasm.Main$SomeClass", SomeClass.class.getName());
    assertEquals(Object.class, SomeClass.class.getSuperclass());
    assertFalse(SomeClass.class.isEnum());
    assertFalse(SomeClass.class.isInterface());
    assertFalse(SomeClass.class.isArray());
    assertFalse(SomeClass.class.isPrimitive());
    assertEquals(
        "com.google.j2cl.integration.wasm.Main$SomeInterface", SomeInterface.class.getName());
    assertEquals(null, SomeInterface.class.getSuperclass());
    assertFalse(SomeInterface.class.isEnum());
    assertTrue(SomeInterface.class.isInterface());
    assertFalse(SomeInterface.class.isArray());
    assertFalse(SomeInterface.class.isPrimitive());
    assertEquals("com.google.j2cl.integration.wasm.Main$SomeEnum", SomeEnum.class.getName());
    assertEquals(Enum.class, SomeEnum.class.getSuperclass());
    assertTrue(SomeEnum.class.isEnum());
    assertFalse(SomeEnum.class.isInterface());
    assertFalse(SomeEnum.class.isArray());
    assertFalse(SomeEnum.class.isPrimitive());
    assertEquals("int", int.class.getName());
    assertEquals(null, int.class.getSuperclass());
    assertFalse(int.class.isEnum());
    assertFalse(int.class.isInterface());
    assertFalse(int.class.isArray());
    assertTrue(int.class.isPrimitive());
    assertEquals(int.class, int[].class.getComponentType());
    assertFalse(int[].class.isEnum());
    assertFalse(int[].class.isInterface());
    assertTrue(int[].class.isArray());
    assertFalse(int[].class.isPrimitive());
  }

  private static void testTry() {
    int i = 1;
    try {
      i += 10;
    } finally {
      i += 3;
    }
    assertEquals(14, i);
  }
}
