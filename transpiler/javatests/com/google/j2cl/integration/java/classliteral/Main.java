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
package classliteral;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertNotSame;
import static com.google.j2cl.integration.testing.Asserts.assertSame;
import static com.google.j2cl.integration.testing.Asserts.assertThrowsNullPointerException;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;
import static com.google.j2cl.integration.testing.TestUtils.isJavaScript;
import static com.google.j2cl.integration.testing.TestUtils.isJvm;
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
    testExtendsNative();
    testGeneric();
    testClinitNotTriggered();
    testGetClassOnClass();
    testLambda();
    testLambdaIntersectionType();
    testSuperCall();
  }

  private class Foo {}

  private static void testClass() {
    Object o = new Main();
    assertSame(Main.class, o.getClass());
    assertSame(Object.class, o.getClass().getSuperclass());
    assertSame(null, Object.class.getSuperclass());

    assertEquals("classliteral.Main", Main.class.getName());
    assertEquals("classliteral.Main$Foo", Foo.class.getName());

    assertEquals("classliteral.Main", Main.class.getCanonicalName());
    if (!isJvm()) {
      // J2CL doesn't follow JLS here:
      assertEquals("classliteral.Main$Foo", Foo.class.getCanonicalName());
    } else {
      assertEquals("classliteral.Main.Foo", Foo.class.getCanonicalName());
    }

    assertEquals("Main", Main.class.getSimpleName());
    assertEquals("Foo", Foo.class.getSimpleName());

    assertEquals("class classliteral.Main", Main.class.toString());
    assertEquals("class classliteral.Main$Foo", Foo.class.toString());

    assertLiteralType("Foo.class", LiteralType.CLASS, Foo.class);
  }

  private interface IFoo {}

  private static void testInterface() {
    assertSame(null, IFoo.class.getSuperclass());

    assertEquals("classliteral.Main$IFoo", IFoo.class.getName());
    if (!isJvm()) {
      // J2CL doesn't follow JLS here:
      assertEquals("classliteral.Main$IFoo", IFoo.class.getCanonicalName());
    } else {
      assertEquals("classliteral.Main.IFoo", IFoo.class.getCanonicalName());
    }
    assertEquals("IFoo", IFoo.class.getSimpleName());
    assertEquals("interface classliteral.Main$IFoo", IFoo.class.toString());

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

    assertEquals("classliteral.Main$Bar", o.getClass().getName());
    if (!isJvm()) {
      // J2CL doesn't follow JLS here:
      assertEquals("classliteral.Main$Bar", o.getClass().getCanonicalName());
    } else {
      assertEquals("classliteral.Main.Bar", o.getClass().getCanonicalName());
    }
    assertEquals("Bar", o.getClass().getSimpleName());
    assertEquals(
        "class classliteral.Main$Bar", o.getClass().toString());

    assertLiteralType("Bar.BAR.getClass()", LiteralType.ENUM, o.getClass());
  }

  private static void testEnumSubclass() {
    Object o = Bar.BAZ;
    assertNotSame(Bar.class, o.getClass());
    assertSame(Bar.class, o.getClass().getSuperclass());

    assertEquals("classliteral.Main$Bar$1", o.getClass().getName());
    assertEquals(
        "class classliteral.Main$Bar$1", o.getClass().toString());

    assertLiteralType("Bar.BAZ.getClass()", LiteralType.CLASS, o.getClass());
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
    // In platforms other than JavaScript JsEnum behaves like a regular enum.
    assertSame(isJavaScript() ? null : Enum.class, o.getClass().getSuperclass());

    assertEquals("classliteral.Main$MyJsEnum", o.getClass().getName());
    if (!isJvm()) {
      // J2CL doesn't follow JLS here:
      assertEquals("classliteral.Main$MyJsEnum", o.getClass().getCanonicalName());
    } else {
      assertEquals("classliteral.Main.MyJsEnum", o.getClass().getCanonicalName());
    }
    assertEquals("MyJsEnum", o.getClass().getSimpleName());
    assertEquals(
        "class classliteral.Main$MyJsEnum", o.getClass().toString());

    // In platforms other than JavaScript JsEnum behaves like a regular enum.
    assertLiteralType(
        "MyJsEnum.VALUE.class",
        isJavaScript() ? LiteralType.CLASS : LiteralType.ENUM,
        o.getClass());
  }

  private static void testArray() {
    // TODO(b/184675805): enable for Wasm when class metadata is implemented for array
    if (isWasm()) {
      return;
    }
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

  @Wasm(
      "nop") // TODO(b/301385322): remove when class literals on native JS objects are implemented.
  private static void testNative() {
    // Native types don't apply in the context of the JVM.
    if (!isJavaScript()) {
      return;
    }
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

  @Wasm("nop") // Extending native types not yet supported in Wasm.
  private static void testExtendsNative() {
    assertEquals("classliteral.TypeExtendsNativeClass", TypeExtendsNativeClass.class.getName());

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
    // the class literal. This is by design.
    if (isJvm()) {
      // J2CL doesn't conform to differentiating the class instance for lambdas that implement
      // the same functional interface.
      assertNotSame(originalLambda.getClass(), lambdaWithSameInterfaceAsOriginalLambda.getClass());
    }

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
    if (isJvm()) {
      // J2CL doesn't conform to differentiating the class instance for anonymous types and
      // lambdas that implement functional interfaces.
      assertNotSame(intersectionLambda.getClass(), anotherIntersectionLambda.getClass());
      assertNotSame(
          intersectionLambda.getClass(), lambdaWithSameInterfaceAsOriginalLambda.getClass());
    }

    assertLiteralType("lambda.getClass()", LiteralType.CLASS, intersectionLambda.getClass());
  }

  private static class GenericClass<T> {}

  private interface GenericInterface<T> {}

  private static void testGeneric() {
    GenericClass<Number> g = new GenericClass<>();
    assertSame(GenericClass.class, g.getClass());
    assertEquals("GenericClass", GenericClass.class.getSimpleName());
    assertEquals("GenericInterface", GenericInterface.class.getSimpleName());

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
          Object unused = nullObject.getClass();
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

  private static void testSuperCall() {
    class A {}
    class B extends A {
      Class<?> superGetClass() {
        return super.getClass();
      }
    }
    assertEquals(B.class, new B().superGetClass());
  }
}
