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
package com.google.j2cl.transpiler.integration.classliteralstripped;

import static com.google.j2cl.transpiler.utils.Asserts.assertEquals;
import static com.google.j2cl.transpiler.utils.Asserts.assertFalse;
import static com.google.j2cl.transpiler.utils.Asserts.assertNotEquals;
import static com.google.j2cl.transpiler.utils.Asserts.assertSame;
import static com.google.j2cl.transpiler.utils.Asserts.assertTrue;

import jsinterop.annotations.JsEnum;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

public class Main {
  public static void main(String[] args) {
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
  }

  interface IFoo {}

  private static void testInterface() {
    assertClass(IFoo.class);
    assertSame(null, IFoo.class.getSuperclass());

    assertClass(Iterable.class);
  }

  private static void testPrimitive() {
    assertClass(int.class);
    assertSame(null, int.class.getSuperclass());
  }

  enum Bar {
    BAR,
    BAZ {};
  }

  private static void testEnum() {
    assertClass(Bar.class);
    assertSame(Enum.class, Bar.class.getSuperclass());

    assertSame(Bar.class, Bar.BAR.getClass());
  }

  private static void testEnumSubclass() {
    assertClass(Bar.BAZ.getClass());
    assertSame(Bar.class, Bar.BAZ.getClass().getSuperclass());
  }

  private static void testArray() {
    assertSame(Foo[].class, Foo[].class);
    assertSame(Object.class, Foo[].class.getSuperclass());

    assertSame(Foo.class, Foo[].class.getComponentType());
    assertSame(Foo[].class, Foo[][].class.getComponentType());

    assertEquals("[L" + Foo.class.getName() + ";", Foo[].class.getName());
    assertEquals(Foo.class.getCanonicalName() + "[]", Foo[].class.getCanonicalName());
    assertEquals(Foo.class.getSimpleName() + "[]", Foo[].class.getSimpleName());
    assertEquals("class [L" + Foo.class.getName() + ";", Foo[].class.toString());

    assertTrue("Foo[].class.isArray() returned false", Foo[].class.isArray());
    assertFalse("Foo[].class.isEnum() returned true", Foo[].class.isEnum());
    assertFalse("Foo[].class.isPrimitive() returned true", Foo[].class.isPrimitive());
    assertFalse("Foo[].class.isInterface() returned true", Foo[].class.isInterface());
  }

  @JsEnum
  private enum MyJsEnum {
    VALUE
  }

  private static void testJsEnum() {
    Object o = MyJsEnum.VALUE;
    assertSame(MyJsEnum.class, o.getClass());
    assertSame(MyJsEnum.class, MyJsEnum.VALUE.getClass());
    assertSame(null, o.getClass().getSuperclass());

    assertFalse("MyJsEnum.VALUE.class.isArray() returned true", o.getClass().isArray());
    assertFalse("MyJsEnum.VALUE.class.isEnum() returned true", o.getClass().isEnum());
    assertFalse("MyJsEnum.VALUE.class.isPrimitive() returned true", o.getClass().isPrimitive());
    assertFalse("MyJsEnum.VALUE.class.isInterface() returned true", o.getClass().isInterface());
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  interface NativeType {}

  @JsFunction
  interface NativeFunction {
    void f();
  }

  private static void testNative() {
    assertClass(NativeType.class);
    assertClass(NativeFunction.class);
  }

  private static void assertClassName(String name, String canonicalName, String simpleName) {
    assertTrue("Name should have the pattern Class$obf_1XXX", name.startsWith("Class$obf_10"));
    assertEquals(name, simpleName);
    assertEquals(name, canonicalName);
  }

  private static String[] seenNames = new String[0];

  private static void assertClass(Class clazz) {
    assertClassName(clazz.getName(), clazz.getCanonicalName(), clazz.getSimpleName());

    // Check if the names are unique.
    for (String seenName : seenNames) {
      assertNotEquals(seenName, clazz.getName());
    }
    seenNames[seenNames.length + 1] = clazz.getName();

    assertFalse("isArray() returned true", clazz.isArray());

    // When class metatadata stripped, all following queries return false.
    assertFalse("isEnum() returned true", clazz.isEnum());
    assertFalse("isPrimitive() returned true", clazz.isPrimitive());
    assertFalse("isInterface() returned true", clazz.isInterface());
  }
}
