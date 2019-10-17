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

import static com.google.j2cl.transpiler.utils.Asserts.assertEquals;
import static com.google.j2cl.transpiler.utils.Asserts.assertNotSame;
import static com.google.j2cl.transpiler.utils.Asserts.assertSame;
import static com.google.j2cl.transpiler.utils.Asserts.assertThrowsNullPointerException;
import static com.google.j2cl.transpiler.utils.Asserts.assertTrue;

import jsinterop.annotations.JsConstructor;
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
    testExtendsNative();
    testGeneric();
    testClinitNotTriggered();
    testGetClassOnClass();
    testLambda();
    testLambdaIntersectionType();
  }

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

    assertLiteralType("Foo.class", LiteralType.CLASS, Foo.class);
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

    assertLiteralType("IFoo.class", LiteralType.INTERFACE, IFoo.class);
  }

  private static void testPrimitive() {
    assertSame(null, int.class.getSuperclass());

    assertEquals("int", int.class.getName());
    assertEquals("int", int.class.getCanonicalName());
    assertEquals("int", int.class.getSimpleName());
    assertEquals("int", int.class.toString());

    assertLiteralType("int.class", LiteralType.PRIMITIVE, int.class);
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

    assertLiteralType("Bar.BAR.getClass()", LiteralType.ENUM, o.getClass());
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

    assertLiteralType("Bar.BAZ.getClass()", LiteralType.CLASS, o.getClass());
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

    assertEquals(
        "com.google.j2cl.transpiler.integration.classliteral.Main$MyJsEnum",
        o.getClass().getName());
    // J2CL doesn't follow JLS here:
    assertEquals(
        "com.google.j2cl.transpiler.integration.classliteral.Main$MyJsEnum",
        o.getClass().getCanonicalName());
    // J2CL doesn't follow JLS here:
    assertEquals("Main$MyJsEnum", o.getClass().getSimpleName());
    assertEquals(
        "class com.google.j2cl.transpiler.integration.classliteral.Main$MyJsEnum",
        o.getClass().toString());

    assertLiteralType("MyJsEnum.VALUE.class", LiteralType.CLASS, o.getClass());
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

    assertLiteralType("Foo[].class", LiteralType.ARRAY, o.getClass());

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

  private static void testNative() {
    Class<?> clazz = NativeFunction.class;
    assertEquals("<native function>", clazz.getName());
    assertEquals(null, clazz.getSuperclass());
    assertLiteralType("NativeFunction.class", LiteralType.INTERFACE, clazz);

    clazz = NativeInterface.class;
    assertEquals("<native object>", clazz.getName());
    assertEquals(null, clazz.getSuperclass());
    assertLiteralType("NativeFunction.class", LiteralType.INTERFACE, clazz);

    clazz = NativeClass.class;
    assertEquals("<native object>", clazz.getName());
    assertEquals(Object.class, clazz.getSuperclass());
    assertLiteralType("NativeFunction.class", LiteralType.CLASS, clazz);
  }

  private static class TypeExtendsNativeClass extends NativeClass {
    @JsConstructor
    TypeExtendsNativeClass() {}
  }

  private static void testExtendsNative() {
    assertEquals(
        "com.google.j2cl.transpiler.integration.classliteral.Main$TypeExtendsNativeClass",
        TypeExtendsNativeClass.class.getName());

    // TODO(b/63081128): Uncomment when fixed.
    // assertEquals(NativeClass.class, TypeExtendsNativeClass.class.getSuperclass());

    assertLiteralType(
        "TypeExtendsNativeClass.class", LiteralType.CLASS, TypeExtendsNativeClass.class);
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

    // TODO(b/67589892): redundant after above assertTrue(are enabled, remove.
    // Lambda instances implementing different interfaces should have different class objects.
    SomeOtherFunctionalInterface lambdaWithDifferentInterface = () -> {};
    assertNotSame(originalLambda.getClass(), lambdaWithDifferentInterface.getClass());

    assertLiteralType("lambda.getClass()", LiteralType.CLASS, originalLambda.getClass());
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

    assertLiteralType("lambda.getClass()", LiteralType.CLASS, intersectionLambda.getClass());
  }

  private static class GenericClass<T> {}

  private interface GenericInterface<T> {}

  private static void testGeneric() {
    GenericClass<Number> g = new GenericClass<>();
    assertSame(GenericClass.class, g.getClass());
    assertEquals("Main$GenericClass", GenericClass.class.getSimpleName());
    assertEquals("Main$GenericInterface", GenericInterface.class.getSimpleName());

    assertLiteralType("generic.getClass()", LiteralType.CLASS, g.getClass());
  }

  private static boolean clinitCalled = false;

  private static class ClinitTest {
    static {
      clinitCalled = true;
    }
  }

  private static void testClinitNotTriggered() {
    assertTrue(clinitCalled == false);
    assertTrue(ClinitTest.class != null);
    assertTrue(clinitCalled == false);
  }

  @SuppressWarnings("GetClassOnClass")
  private static void testGetClassOnClass() {
    assertSame(Class.class, Object.class.getClass());
    assertSame(Class.class, int.class.getClass());
    assertSame(Class.class, Object[].class.getClass());

    assertThrowsNullPointerException(
        () -> {
          Object nullObject = null;
          nullObject.getClass();
        });
  }

  private enum LiteralType {
    CLASS,
    INTERFACE,
    ARRAY,
    PRIMITIVE,
    ENUM
  }

  private static void assertLiteralType(
      String messagePrefix, LiteralType expectedType, Class<?> literal) {

    assertEquals(
        messagePrefix + ".isInterface()",
        expectedType == LiteralType.INTERFACE,
        literal.isInterface());
    assertEquals(messagePrefix + ".isEnum()", expectedType == LiteralType.ENUM, literal.isEnum());
    assertEquals(
        messagePrefix + ".isPrimitive()",
        expectedType == LiteralType.PRIMITIVE,
        literal.isPrimitive());
    assertEquals(
        messagePrefix + ".isArray()", expectedType == LiteralType.ARRAY, literal.isArray());
  }
}
