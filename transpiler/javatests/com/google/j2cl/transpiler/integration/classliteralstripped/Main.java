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

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

public class Main {

  static class Foo {}

  public static void testClass() {
    assertClass(Object.class);
    assertSame(null, Object.class.getSuperclass());

    assertClass(Main.class);
    assertSame(Object.class, Main.class.getSuperclass());

    assertClass(Foo.class);
    assertSame(Object.class, Foo.class.getSuperclass());
  }

  interface IFoo {}

  public static void testInterface() {
    assertClass(IFoo.class);
    assertSame(null, IFoo.class.getSuperclass());

    assertClass(Iterable.class);
  }

  public static void testPrimitive() {
    assertClass(int.class);
    assertSame(null, int.class.getSuperclass());
  }

  static enum Bar {
    BAR,
    BAZ {};
  }

  public static void testEnum() {
    assertClass(Bar.class);
    assertSame(Enum.class, Bar.class.getSuperclass());

    assertSame(Bar.class, Bar.BAR.getClass());
  }

  public static void testEnumSubclass() {
    assertClass(Bar.BAZ.getClass());
    assertSame(Bar.class, Bar.BAZ.getClass().getSuperclass());
  }

  public static void testArray() {
    assertSame(Foo[].class, Foo[].class);
    assertSame(Object.class, Foo[].class.getSuperclass());

    assertSame(Foo.class, Foo[].class.getComponentType());
    assertSame(Foo[].class, Foo[][].class.getComponentType());

    assertEquals("[L" + Foo.class.getName() + ";", Foo[].class.getName());
    assertEquals(Foo.class.getCanonicalName() + "[]", Foo[].class.getCanonicalName());
    assertEquals(Foo.class.getSimpleName() + "[]", Foo[].class.getSimpleName());
    assertEquals("class [L" + Foo.class.getName() + ";", Foo[].class.toString());

    assert Foo[].class.isArray() : "Foo[].class.isArray() returned false";
    assert !Foo[].class.isEnum() : "Foo[].class.isEnum() returned true";
    assert !Foo[].class.isPrimitive() : "Foo[].class.isPrimitive() returned true";
    assert !Foo[].class.isInterface() : "Foo[].class.isInterface() returned true";
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  interface NativeType {}

  @JsFunction
  interface NativeFunction {
    void f();
  }

  public static void testNative() {
    assertClass(NativeType.class);
    assertClass(NativeFunction.class);
  }

  public static void main(String[] args) {
    testClass();
    testInterface();
    testPrimitive();
    testEnum();
    testEnumSubclass();
    testArray();
    testNative();
  }

  private static void assertEquals(Object expected, Object actual) {
    assert expected == null ? actual == null : expected.equals(actual)
        : getFailureMessage(expected, actual, "should be equal");
  }

  private static void assertNotEquals(Object expected, Object actual) {
    assert expected == null ? actual != null : !expected.equals(actual)
        : getFailureMessage(expected, actual, "should not be equal");
  }

  private static void assertSame(Object expected, Object actual) {
    assert expected == actual : getFailureMessage(expected, actual, "should be same");
  }

  private static String getFailureMessage(Object expected, Object actual, String msg) {
    String expectedString = expected == null ? null : expected.toString();
    String actualString = actual == null ? null : actual.toString();
    return "<" + actualString + "> " + msg + " to <" + expectedString + ">";
  }

  private static void assertClassName(String name, String canonicalName, String simpleName) {
    assert name.startsWith("Class$obf_10") : "Name should have the pattern Class$obf_1XXX";
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

    assert !clazz.isArray() : "isArray() returned true";

    // When class metatadata stripped, all following queries return false.
    assert !clazz.isEnum() : "isEnum() returned true";
    assert !clazz.isPrimitive() : "isPrimitive() returned true";
    assert !clazz.isInterface() : "isInterface() returned true";
  }
}
