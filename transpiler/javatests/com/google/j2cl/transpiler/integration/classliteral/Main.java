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
package com.google.j2cl.transpiler.integration.classliteral;

import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

public class Main {

  private class Foo {}

  private static void testClass() {
    Object o = new Main();
    assertSame(Main.class, o.getClass());
    assertSame(Object.class, o.getClass().getSuperclass());
    assertSame(null, Object.class.getSuperclass());

    assertEquals("com.google.j2cl.transpiler.integration.classliteral.Main", Main.class.getName());
    assertEquals(
        "com.google.j2cl.transpiler.integration.classliteral.Main$Foo", Foo.class.getName());

    assertEquals(
        "com.google.j2cl.transpiler.integration.classliteral.Main", Main.class.getCanonicalName());
    // J2CL doesn't follow JLS here:
    assertEquals(
        "com.google.j2cl.transpiler.integration.classliteral.Main$Foo",
        Foo.class.getCanonicalName());

    assertEquals("Main", Main.class.getSimpleName());
    // J2CL doesn't follow JLS here:
    assertEquals("Main$Foo", Foo.class.getSimpleName());

    assertEquals(
        "class com.google.j2cl.transpiler.integration.classliteral.Main", Main.class.toString());
    assertEquals(
        "class com.google.j2cl.transpiler.integration.classliteral.Main$Foo", Foo.class.toString());

    assert !Foo.class.isArray() : "Foo.class.isArray() returned true";
    assert !Foo.class.isEnum() : "Foo.class.isEnum() returned true";
    assert !Foo.class.isPrimitive() : "Foo.class.isPrimitive() returned true";
    assert !Foo.class.isInterface() : "Foo.class.isInterface() returned true";
  }

  private interface IFoo {}

  private static void testInterface() {
    assertSame(null, IFoo.class.getSuperclass());

    assertEquals(
        "com.google.j2cl.transpiler.integration.classliteral.Main$IFoo", IFoo.class.getName());
    // J2CL doesn't follow JLS here:
    assertEquals(
        "com.google.j2cl.transpiler.integration.classliteral.Main$IFoo",
        IFoo.class.getCanonicalName());
    assertEquals("Main$IFoo", IFoo.class.getSimpleName());
    // J2CL doesn't follow JLS here:
    assertEquals(
        "interface com.google.j2cl.transpiler.integration.classliteral.Main$IFoo",
        IFoo.class.toString());

    assert !IFoo.class.isArray() : "IFoo.class.isArray() returned true";
    assert !IFoo.class.isEnum() : "IFoo.class.isEnum() returned true";
    assert !IFoo.class.isPrimitive() : "IFoo.class.isPrimitive() returned true";
    assert IFoo.class.isInterface() : "IFoo.class.isInterface() returned false";
  }

  private static void testPrimitive() {
    assertSame(null, int.class.getSuperclass());

    assertEquals("int", int.class.getName());
    assertEquals("int", int.class.getCanonicalName());
    assertEquals("int", int.class.getSimpleName());
    assertEquals("int", int.class.toString());

    assert !int.class.isArray() : "int.class.isArray() returned true";
    assert !int.class.isEnum() : "int.class.isEnum() returned true";
    assert int.class.isPrimitive() : "int.class.isPrimitive() returned false";
    assert !int.class.isInterface() : "int.class.isInterface() returned true";
  }

  private static void testPrimitivesUnboxed() {
    Object b = true;
    Object d = 0.1;
    assertEquals(Boolean.class, b.getClass());
    assertEquals(Double.class, d.getClass());
  }

  private enum Bar {
    BAR,
    BAZ {}
  }

  private static void testEnum() {
    Object o = Bar.BAR;
    assertSame(Bar.class, o.getClass());
    assertSame(Enum.class, o.getClass().getSuperclass());

    assertEquals(
        "com.google.j2cl.transpiler.integration.classliteral.Main$Bar", o.getClass().getName());
    // J2CL doesn't follow JLS here:
    assertEquals(
        "com.google.j2cl.transpiler.integration.classliteral.Main$Bar",
        o.getClass().getCanonicalName());
    // J2CL doesn't follow JLS here:
    assertEquals("Main$Bar", o.getClass().getSimpleName());
    assertEquals(
        "class com.google.j2cl.transpiler.integration.classliteral.Main$Bar",
        o.getClass().toString());


    assert !o.getClass().isArray() : "Bar.BAR.class.isArray() returned true";
    assert o.getClass().isEnum() : "Bar.BAR.class.isEnum() returned false";
    assert !o.getClass().isPrimitive() : "Bar.BAR.class.isPrimitive() returned true";
    assert !o.getClass().isInterface() : "Bar.BAR.class.isInterface() returned true";
  }

  private static void testEnumSubclass() {
    Object o = Bar.BAZ;
    assertNotSame(Bar.class, o.getClass());
    assertSame(Bar.class, o.getClass().getSuperclass());

    assertEquals(
        "com.google.j2cl.transpiler.integration.classliteral.Main$Bar$1", o.getClass().getName());
    assertEquals(
        "class com.google.j2cl.transpiler.integration.classliteral.Main$Bar$1",
        o.getClass().toString());

    assert !o.getClass().isArray() : "Bar.BAZ.class.isArray() returned true";
    assert !o.getClass().isEnum() : "Bar.BAZ.class.isEnum() returned true";
    assert !o.getClass().isPrimitive() : "Bar.BAZ.class.isPrimitive() returned true";
    assert !o.getClass().isInterface() : "Bar.BAZ.class.isInterface() returned true";
  }

  private static void testArray() {
    Object o = new Foo[3];
    assertSame(Foo[].class, o.getClass());
    assertSame(Object.class, o.getClass().getSuperclass());

    assertSame(Foo.class, o.getClass().getComponentType());
    assertSame(Foo[].class, Foo[][].class.getComponentType());

    assertEquals("[L" + Foo.class.getName() + ";", o.getClass().getName());
    assertEquals(Foo.class.getCanonicalName() + "[]", o.getClass().getCanonicalName());
    assertEquals(Foo.class.getSimpleName() + "[]", o.getClass().getSimpleName());
    assertEquals("class [L" + Foo.class.getName() + ";", o.getClass().toString());

    assert o.getClass().isArray() : "Foo[].class.isArray() returned false";
    assert !o.getClass().isEnum() : "Foo[].class.isEnum() returned true";
    assert !o.getClass().isPrimitive() : "Foo[].class.isPrimitive() returned true";
    assert !o.getClass().isInterface() : "Foo[].class.isInterface() returned true";

    Foo[][] f = new Foo[3][3];
    assertSame(Foo[][].class, f.getClass());
    assertSame(Foo[].class, f[0].getClass());
  }

  @JsFunction
  private interface NativeFunction {
    void f();
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  private interface NativeInterface {}

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  private static class NativeClass {}

  public static void testNative() {
    assertEquals("<native function>", NativeFunction.class.getName());
    assertEquals(null, NativeFunction.class.getSuperclass());

    // TODO(79116203): return JavaScriptInterface or fail restriction checking
    assertEquals("<native object>", NativeInterface.class.getName());
    assertEquals(null, NativeInterface.class.getSuperclass());

    assertEquals("<native object>", NativeClass.class.getName());
    // TODO(79123093): return Object.class
    assertEquals(null, NativeClass.class.getSuperclass());
  }

  private static class TypeExtendsNativeClass extends NativeClass {
    @JsConstructor
    TypeExtendsNativeClass() {}
  }

  public static void testExtendsNative() {
    assertEquals(
        "com.google.j2cl.transpiler.integration.classliteral.Main$TypeExtendsNativeClass",
        TypeExtendsNativeClass.class.getName());
    assertEquals(NativeClass.class, TypeExtendsNativeClass.class.getSuperclass());
  }

  private interface SomeFunctionalInterface {
    void m();
  }

  private interface SomeOtherFunctionalInterface {
    void m();
  }

  private static void testLambda() {
    SomeFunctionalInterface originalLambda = () -> {};
    SomeFunctionalInterface lambdaWithSameInterfaceAsOriginalLambda = () -> {};

    // TODO(b/67589892): lambda instances are all instances of the same adaptor class, sharing
    // the class literal. Uncomment the test when this is fixed.

    // Different lambda instances should have different class objects, even if they implement the
    // same functional interface.
    // assertNotSame(
    //     originalLambda.getClass(),
    //     lambdaWithSameInterfaceAsOriginalLambda.getClass());

    assertSame(originalLambda.getClass(), originalLambda.getClass());

    // TODO(b/67589892): redundant after above assert are enabled, remove.
    // Lambda instances implementing different interfaces should have different class objects.
    SomeOtherFunctionalInterface lambdaWithDifferentInterface = () -> {};
    assertNotSame(originalLambda.getClass(), lambdaWithDifferentInterface.getClass());

    assert !originalLambda.getClass().isArray() : "lambda.getClass().isArray() returned true";
    assert !originalLambda.getClass().isEnum() : "lambda.getClass().isEnum() returned true";
    assert !originalLambda.getClass().isPrimitive()
        : "lambda.getClass().isPrimitive() returned true";
    assert !originalLambda.getClass().isInterface()
        : "lambda.getClass().isInterface() returned true";
  }

  private interface SomeFunctionalInterfaceWithParameter {
    void m(int a);
  }

  private interface MarkerInterface {}

  private static void testLambdaIntersectionType() {
    SomeFunctionalInterfaceWithParameter intersectionLambda =
        (SomeFunctionalInterfaceWithParameter & MarkerInterface) (a) -> {};
    SomeFunctionalInterfaceWithParameter anotherIntersectionLambda =
        (SomeFunctionalInterfaceWithParameter & MarkerInterface) (a) -> {};
    SomeFunctionalInterfaceWithParameter lambdaWithSameInterfaceAsOriginalLambda = (a) -> {};

    assertSame(intersectionLambda.getClass(), intersectionLambda.getClass());
    assertNotSame(intersectionLambda.getClass(), anotherIntersectionLambda.getClass());
    assertNotSame(
        intersectionLambda.getClass(), lambdaWithSameInterfaceAsOriginalLambda.getClass());

    assert !intersectionLambda.getClass().isArray() : "lambda.getClass().isArray() returned true";
    assert !intersectionLambda.getClass().isEnum() : "lambda.getClass().isEnum() returned true";
    assert !intersectionLambda.getClass().isPrimitive()
        : "lambda.getClass().isPrimitive() returned true";
    assert !intersectionLambda.getClass().isInterface()
        : "lambda.getClass().isInterface() returned true";
  }

  private static class GenericClass<T> {}

  private interface GenericInterface<T> {}

  private static void testGeneric() {
    GenericClass<Number> g = new GenericClass<>();
    assertSame(GenericClass.class, g.getClass());
    assertEquals("Main$GenericClass", GenericClass.class.getSimpleName());
    assertEquals("Main$GenericInterface", GenericInterface.class.getSimpleName());
  }

  private static boolean clinitCalled = false;

  private static class ClinitTest {
    static {
      clinitCalled = true;
    }
  }

  private static void testClinit() {
    assert clinitCalled == false;
    assert ClinitTest.class != null;
    assert clinitCalled == false;
  }

  @SuppressWarnings("GetClassOnClass")
  private static void testMisc() {
    assertSame(Class.class, Object.class.getClass());
    assertSame(Class.class, int.class.getClass());
    assertSame(Class.class, Object[].class.getClass());

    try {
      Object nullObject = null;
      nullObject.getClass();
      assert false;
    } catch (NullPointerException expected) {
      // expected
    }
  }

  public static void main(String[] args) {
    testClass();
    testInterface();
    testPrimitive();
    testPrimitivesUnboxed();
    testEnum();
    testEnumSubclass();
    testArray();
    testNative();
    // TODO(63081128): Enable the test.
    // testExtendsNative();
    testGeneric();
    testClinit();
    testMisc();
    testLambda();
    testLambdaIntersectionType();
  }

  private static void assertEquals(Object expected, Object actual) {
    assert expected == null ? actual == null : expected.equals(actual)
        : getFailureMessage(expected, actual, "should be equal");
  }

  private static void assertSame(Object expected, Object actual) {
    assert expected == actual : getFailureMessage(expected, actual, "should be same");
  }

  private static void assertNotSame(Object expected, Object actual) {
    assert expected != actual : getFailureMessage(expected, actual, "should not be same");
  }

  private static String getFailureMessage(Object expected, Object actual, String msg) {
    String expectedString = expected == null ? null : expected.toString();
    String actualString = actual == null ? null : actual.toString();
    return "<" + actualString + "> " + msg + " to <" + expectedString + ">";
  }
}
