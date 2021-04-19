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
import static com.google.j2cl.integration.testing.Asserts.assertEqualsDelta;
import static com.google.j2cl.integration.testing.Asserts.assertFalse;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;
import static com.google.j2cl.integration.testing.Asserts.fail;

import java.util.ArrayList;
import javaemul.internal.ArrayHelper;
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
    testArrayInstanceOf();
    testWasmArrayApis();
    // TODO(b/171833737): Enable after System.getProperty support is added.
    // testArrayList();
    // TODO(b/171833737): Enable after System.getProperty and Class.getComponentType support is
    // added.
    // testSystemArrayCopy();
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

    assertEquals(Double.NaN, Math.exp(Double.NaN));
    assertEquals(Double.POSITIVE_INFINITY, Math.exp(Double.POSITIVE_INFINITY));
    assertEquals(0.0, Math.exp(Double.NEGATIVE_INFINITY));
    assertEquals(1.0, Math.exp(0));
    assertEqualsDelta(0.36787944117144, Math.exp(-1), 0.000001);
    assertEqualsDelta(2.718281, Math.exp(1), 0.000001);

    assertEquals(-0.0, Math.expm1(-0.0));
    assertEquals(0.0, Math.expm1(0.0));
    assertEquals(Double.NaN, Math.expm1(Double.NaN));
    assertEquals(Double.POSITIVE_INFINITY, Math.expm1(Double.POSITIVE_INFINITY));
    assertEquals(-1.0, Math.expm1(Double.NEGATIVE_INFINITY));
    int x = +1;
    assertEqualsDelta(-0.632, Math.expm1(-1), 0.001);
    assertEqualsDelta(1.718, Math.expm1(1), 0.001);
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

  private static void testArrayInstanceOf() {
    Object intArray = new int[0];
    assertTrue(intArray instanceof int[]);
    assertFalse(intArray instanceof long[]);
    assertFalse(intArray instanceof Object[]);
    assertFalse(intArray instanceof SomeInterface[]);

    Object multiDimIntArray = new int[0][0];
    assertFalse(multiDimIntArray instanceof int[]);
    assertFalse(multiDimIntArray instanceof long[]);
    assertTrue(multiDimIntArray instanceof Object[]);
    assertTrue(multiDimIntArray instanceof int[][]);
    // TODO(b/184675805): enable when this is fixed.
    // assertFalse(multiDimIntArray instanceof long[][]);
    // assertFalse(multiDimIntArray instanceof SomeInterface[]);

    Object objectArray = new Object[0];
    assertFalse(objectArray instanceof int[]);
    assertFalse(objectArray instanceof long[]);
    assertTrue(objectArray instanceof Object[]);
    // TODO(b/184675805): enable when this is fixed.
    // assertFalse(objectArray instanceof int[][]);
    // assertFalse(objectArray instanceof long[][]);
    // assertFalse(objectArray instanceof SomeInterface[]);

    Object multiDimObjectArray = new Object[0][0];
    assertFalse(multiDimObjectArray instanceof int[]);
    assertFalse(multiDimObjectArray instanceof long[]);
    assertTrue(multiDimObjectArray instanceof Object[]);
    assertTrue(multiDimObjectArray instanceof Object[][]);
    // TODO(b/184675805): enable when this is fixed.
    // assertFalse(multiDimObjectArray instanceof int[][]);
    // assertFalse(multiDimObjectArray instanceof long[][]);
    // assertFalse(multiDimObjectArray instanceof SomeInterface[]);

    Object referencetArray = new SomeInterface[0];
    assertFalse(referencetArray instanceof int[]);
    assertFalse(referencetArray instanceof long[]);
    assertTrue(referencetArray instanceof Object[]);
    assertTrue(referencetArray instanceof SomeInterface[]);
    // TODO(b/184675805): enable when this is fixed.
    // assertFalse(referencetArray instanceof Object[][]);
    // assertFalse(referencetArray instanceof int[][]);
    // assertFalse(referencetArray instanceof long[][]);
  }

  private static void testWasmArrayApis() {
    Object[] array = new Object[0];
    ArrayHelper.push(array, "c");
    ArrayHelper.push(array, "d");
    assertEquals(array, new Object[] {"c", "d"});

    ArrayHelper.insertTo(array, 0, "a");
    assertEquals(array, new Object[] {"a", "c", "d"});
    ArrayHelper.insertTo(array, 1, "b");
    assertEquals(array, new Object[] {"a", "b", "c", "d"});

    ArrayHelper.removeFrom(array, 3, 1);
    ArrayHelper.removeFrom(array, 0, 1);
    assertEquals(array, new Object[] {"b", "c"});

    ArrayHelper.setLength(array, 5);
    assertEquals(array, new Object[] {"b", "c", null, null, null});
    ArrayHelper.setLength(array, 1);
    assertEquals(array, new Object[] {"b"});
  }

  private static void testArrayList() {
    ArrayList<String> list = new ArrayList<>();
    list.add("a");
    list.add("b");
    list.add("d");
    list.add("e");
    list.add(2, "c");
    assertEquals(list.toArray(), new Object[] {"a", "b", "c", "d", "e"});

    list.remove(4);
    list.remove(0);
    assertEquals(list.toArray(), new Object[] {"b", "c", "d"});

    list.clear();
    assertEquals(list.toArray(), new Object[] {});
    list.add("z");
    assertEquals(list.toArray(), new Object[] {"z"});
  }

  static class Foo {}

  static class Bar extends Foo {}

  private static void testSystemArrayCopy() {
    Foo[] fooArray = new Foo[4];
    Bar[] barArray = new Bar[4];
    Object[] src = new Object[] {new Bar(), new Bar(), new Foo(), new Bar()};
    System.arraycopy(src, 0, fooArray, 0, src.length);
    for (int i = 0; i < src.length; ++i) {
      assertEquals(src[i], fooArray[i]);
    }

    String[] strArray = new String[] {"0", "1", "2", "3"};

    System.arraycopy(strArray, 0, strArray, 1, strArray.length - 1);
    String[] strArrayExpected1 = new String[] {"0", "0", "1", "2"};
    for (int i = 0; i < strArray.length; ++i) {
      assertEquals("rev str copy index " + i, strArrayExpected1[i], strArray[i]);
    }

    System.arraycopy(strArray, 1, strArray, 0, strArray.length - 1);
    String[] strArrayExpected2 = new String[] {"0", "1", "2", "2"};
    for (int i = 0; i < strArray.length; ++i) {
      assertEquals("rev str copy index " + i, strArrayExpected2[i], strArray[i]);
    }
  }
}
