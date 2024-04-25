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
package classliteralstripped;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertNotEquals;
import static com.google.j2cl.integration.testing.Asserts.assertSame;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;
import static com.google.j2cl.integration.testing.TestUtils.isWasm;

import javaemul.internal.annotations.Wasm;
import jsinterop.annotations.JsEnum;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

public class Main {
  public static void main(String... args) {
    testClass();
    testInterface();
    testPrimitive();
    testEnum();
    testEnumSubclass();
    testJsEnum();
    testArray();
    testNative();
  }

  static class Foo {}

  private static void testClass() {
    assertClass(Object.class);
    assertSame(null, Object.class.getSuperclass());

    assertClass(Main.class);
    assertSame(Object.class, Main.class.getSuperclass());

    assertClass(Foo.class);
    assertSame(Object.class, Foo.class.getSuperclass());
    assertNonArrayLiteralType("Foo.class", Foo.class);
  }

  interface IFoo {}

  private static void testInterface() {
    assertClass(IFoo.class);
    assertSame(null, IFoo.class.getSuperclass());

    assertClass(Iterable.class);
    assertNonArrayLiteralType("Iterable.class", Iterable.class);
  }

  private static void testPrimitive() {
    assertClass(int.class);
    assertSame(null, int.class.getSuperclass());
    assertNonArrayLiteralType("int.class", int.class);
  }

  enum Bar {
    BAR,
    BAZ {};
  }

  private static void testEnum() {
    assertClass(Bar.class);
    assertSame(Enum.class, Bar.class.getSuperclass());

    assertSame(Bar.class, Bar.BAR.getClass());
    assertNonArrayLiteralType("Bar.BAR.getClass()", Bar.BAR.getClass());
    assertNonArrayLiteralType("Bar.BAZ.getClass()", Bar.BAZ.getClass());
  }

  private static void testEnumSubclass() {
    assertClass(Bar.BAZ.getClass());
    assertSame(Bar.class, Bar.BAZ.getClass().getSuperclass());
    assertNonArrayLiteralType("Iterable.class", Iterable.class);
  }

  private static void testArray() {
    // TODO(b/184675805): enable for Wasm when class metadata is implemented for array
    if (isWasm()) {
      return;
    }
    assertSame(Foo[].class, Foo[].class);
    assertSame(Object.class, Foo[].class.getSuperclass());

    assertSame(Foo.class, Foo[].class.getComponentType());
    assertSame(Foo[].class, Foo[][].class.getComponentType());

    assertEquals("[L" + Foo.class.getName() + ";", Foo[].class.getName());
    assertEquals(Foo.class.getCanonicalName() + "[]", Foo[].class.getCanonicalName());
    assertEquals(Foo.class.getSimpleName() + "[]", Foo[].class.getSimpleName());
    assertEquals("class [L" + Foo.class.getName() + ";", Foo[].class.toString());

    assertArrayLiteralType("Foo[].class", Foo[].class);
  }

  @JsEnum
  private enum MyJsEnum {
    VALUE
  }

  private static void testJsEnum() {
    // TODO(b/295235576): enable for Wasm when class metadata is implemented for JsEnum.
    if (isWasm()) {
      return;
    }
    Object o = MyJsEnum.VALUE;
    assertSame(MyJsEnum.class, o.getClass());
    assertSame(MyJsEnum.class, MyJsEnum.VALUE.getClass());
    if (isWasm()) {
      assertSame(Enum.class, o.getClass().getSuperclass());
    } else {
      assertSame(null, o.getClass().getSuperclass());
    }

    assertNonArrayLiteralType("MyJsEnum.VALUE.class", o.getClass());
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  interface NativeInterface {}

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  private static class NativeClass {}

  @JsFunction
  interface NativeFunction {
    void f();
  }

  @Wasm(
      "nop") // TODO(b/301385322): remove when class literals on native JS objects are implemented.
  private static void testNative() {
    assertClass(NativeInterface.class);
    assertClass(NativeFunction.class);
    assertClass(NativeClass.class);
  }

  private static void assertClassName(String name, String canonicalName, String simpleName) {
    assertTrue("Name should have the pattern XXX_Class$obf_1XXX", name.contains("Class$obf_10"));
    assertTrue(name.endsWith(simpleName));
    assertEquals(name, canonicalName);
  }

  private static String[] seenNames = new String[100];
  private static int seenNameCount;

  private static void assertClass(Class clazz) {
    assertClassName(clazz.getName(), clazz.getCanonicalName(), clazz.getSimpleName());

    // Check if the names are unique.
    for (int i = 0; i < seenNameCount; i++) {
      assertNotEquals(seenNames[i], clazz.getName());
    }
    seenNames[seenNameCount++] = clazz.getName();

    assertNonArrayLiteralType("<class>", clazz);
  }

  private static void assertNonArrayLiteralType(String messagePrefix, Class<?> literal) {
    assertLiteralType(messagePrefix, false, literal);
  }

  private static void assertArrayLiteralType(String messagePrefix, Class<?> literal) {
    assertLiteralType(messagePrefix, true, literal);
  }

  private static void assertLiteralType(String messagePrefix, boolean isArray, Class<?> literal) {
    assertEquals(messagePrefix + ".isArray()", isArray, literal.isArray());

    // When class metatadata stripped, all following queries return false.
    assertEquals(messagePrefix + ".isInterface()", false, literal.isInterface());
    assertEquals(messagePrefix + ".isEnum()", false, literal.isEnum());
    assertEquals(messagePrefix + ".isPrimitive()", false, literal.isPrimitive());
  }
}
