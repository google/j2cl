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
import static com.google.j2cl.integration.testing.TestUtils.isJ2Kt;
import static com.google.j2cl.integration.testing.TestUtils.isJ2KtNative;
import static com.google.j2cl.integration.testing.TestUtils.isJavaScript;
import static com.google.j2cl.integration.testing.TestUtils.isJvm;
import static com.google.j2cl.integration.testing.TestUtils.isWasm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javaemul.internal.annotations.Wasm;
import jsinterop.annotations.JsEnum;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import org.jspecify.annotations.Nullable;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
@interface J2ktIncompatible {}

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
    assertSuperclass(Object.class, o.getClass());
    assertSuperclass(null, Object.class);

    assertNameEquals("classliteral.Main", Main.class);
    assertNameEquals("classliteral.Main$Foo", Foo.class);

    assertCanonicalNameEquals("classliteral.Main", Main.class);
    assertCanonicalNameEquals("classliteral.Main$Foo", Foo.class);

    assertSimpleNameEquals("Main", Main.class);
    assertSimpleNameEquals("Foo", Foo.class);

    assertToStringEquals("class classliteral.Main", Main.class);
    assertToStringEquals("class classliteral.Main$Foo", Foo.class);

    assertLiteralType("Foo.class", LiteralType.CLASS, Foo.class);
  }

  private interface IFoo {}

  private static void testInterface() {
    assertSuperclass(null, IFoo.class);

    assertNameEquals("classliteral.Main$IFoo", IFoo.class);
    assertCanonicalNameEquals("classliteral.Main$IFoo", IFoo.class);
    assertSimpleNameEquals("IFoo", IFoo.class);
    assertToStringEquals("interface classliteral.Main$IFoo", IFoo.class);

    assertLiteralType("IFoo.class", LiteralType.INTERFACE, IFoo.class);
  }

  private static void testPrimitive() {
    assertSuperclass(null, int.class);

    assertNameEquals("int", int.class);
    assertCanonicalNameEquals("int", int.class);
    assertSimpleNameEquals("int", int.class);
    assertToStringEquals("int", int.class);

    assertLiteralType("int.class", LiteralType.PRIMITIVE, int.class);
  }

  private enum Bar {
    BAR,
    BAZ {}
  }

  private static void testEnum() {
    Object o = Bar.BAR;
    assertSame(Bar.class, o.getClass());
    assertSuperclass(Enum.class, o.getClass());

    assertNameEquals("classliteral.Main$Bar", o.getClass());
    assertCanonicalNameEquals("classliteral.Main$Bar", o.getClass());
    assertSimpleNameEquals("Bar", o.getClass());
    assertToStringEquals("class classliteral.Main$Bar", o.getClass());

    assertLiteralType("Bar.BAR.getClass()", LiteralType.ENUM, o.getClass());
  }

  private static void testEnumSubclass() {
    // Enum subclass does not behave the same in Kotlin.
    if (isJ2Kt()) {
      return;
    }
    Object o = Bar.BAZ;
    assertNotSame(Bar.class, o.getClass());
    assertSuperclass(Bar.class, o.getClass());

    assertNameEquals("classliteral.Main$Bar$1", o.getClass());
    assertToStringEquals("class classliteral.Main$Bar$1", o.getClass());

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
    assertSuperclass(isJavaScript() ? null : Enum.class, o.getClass());

    assertNameEquals("classliteral.Main$MyJsEnum", o.getClass());
    assertCanonicalNameEquals("classliteral.Main$MyJsEnum", o.getClass());
    assertSimpleNameEquals("MyJsEnum", o.getClass());
    assertToStringEquals("class classliteral.Main$MyJsEnum", o.getClass());

    // In platforms other than JavaScript JsEnum behaves like a regular enum.
    assertLiteralType(
        "MyJsEnum.VALUE.class",
        isJavaScript() ? LiteralType.CLASS : LiteralType.ENUM,
        o.getClass());
  }

  private static void testArray() {
    // TODO(b/184675805): enable for Wasm when class metadata is implemented for array
    // Array component type is erased in Kotlin and non-object array class literals like
    // `Foo[].class` are rendered as `Array::class`.
    if (isWasm() || isJ2Kt()) {
      return;
    }
    Object o = new Foo[3];
    assertSame(Foo[].class, o.getClass());
    assertSuperclass(Object.class, o.getClass());

    assertSame(Foo.class, o.getClass().getComponentType());
    assertSame(Foo[].class, Foo[][].class.getComponentType());

    assertNameEquals("[L" + Foo.class.getName() + ";", o.getClass());
    assertCanonicalNameEquals(Foo.class.getCanonicalName() + "[]", o.getClass());
    assertSimpleNameEquals(Foo.class.getSimpleName() + "[]", o.getClass());
    assertToStringEquals("class [L" + Foo.class.getName() + ";", o.getClass());

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
    assertNameEquals("<native function>", clazz);
    assertSuperclass(null, clazz);
    assertLiteralType("NativeFunction.class", LiteralType.INTERFACE, clazz);

    clazz = NativeInterface.class;
    assertNameEquals("<native object>", clazz);
    assertSuperclass(null, clazz);
    assertLiteralType("NativeFunction.class", LiteralType.INTERFACE, clazz);

    clazz = NativeClass.class;
    assertNameEquals("<native object>", clazz);
    assertSuperclass(Object.class, clazz);
    assertLiteralType("NativeFunction.class", LiteralType.CLASS, clazz);
  }

  @Wasm("nop") // Extending native types not yet supported in Wasm.
  private static void testExtendsNative() {
    assertNameEquals("classliteral.TypeExtendsNativeClass", TypeExtendsNativeClass.class);

    // TODO(b/63081128): Uncomment when fixed.
    // assertSuperClass(NativeClass.class, TypeExtendsNativeClass.class);

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
    // Kotlin does not support lambda intersection type
    if (isJ2Kt()) {
      return;
    }
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

  private static class GenericClass<T extends @Nullable Object> {}

  private interface GenericInterface<T extends @Nullable Object> {}

  private static void testGeneric() {
    GenericClass<Number> g = new GenericClass<>();
    assertSame(GenericClass.class, g.getClass());
    assertSimpleNameEquals("GenericClass", GenericClass.class);
    assertSimpleNameEquals("GenericInterface", GenericInterface.class);

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

  // isInterface() / isEnum() is not available in Kotlin
  @J2ktIncompatible
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

  // Fallback called on J2KT.
  private static void assertLiteralType(
      String messagePrefix, LiteralType expectedType, Class<?> literal, Object... unused) {
    if (!isJ2Kt()) {
      throw new AssertionError();
    }
    assertEquals(
        messagePrefix + ".isPrimitive()",
        expectedType == LiteralType.PRIMITIVE,
        literal.isPrimitive());
    assertEquals(
        messagePrefix + ".isArray()", expectedType == LiteralType.ARRAY, literal.isArray());
  }

  @J2ktIncompatible
  private static void testSuperCall() {
    class A {}
    class B extends A {
      Class<?> superGetClass() {
        return super.getClass();
      }
    }
    assertEquals(B.class, new B().superGetClass());
  }

  private static void testSuperCall(Object... unused) {}

  private static void assertNameEquals(String name, Class<?> clazz) {
    // J2KT/Native doesn't follow JLS
    assertEquals(isJ2KtNative() ? name.replace('$', '.') : name, clazz.getName());
  }

  private static void assertSimpleNameEquals(String simpleName, Class<?> clazz) {
    assertEquals(simpleName, clazz.getSimpleName());
  }

  private static void assertCanonicalNameEquals(String canonicalName, Class<?> clazz) {
    // J2CL & J2KT/Native doesn't follow JLS
    assertEquals(
        isJvm() || isJ2KtNative() ? canonicalName.replace('$', '.') : canonicalName,
        clazz.getCanonicalName());
  }

  private static void assertToStringEquals(String string, Class<?> clazz) {
    // J2KT/Native doesn't follow JLS
    assertEquals(
        isJ2KtNative() ? string.replace('$', '.').replace("interface ", "class ") : string,
        clazz.toString());
  }

  // Class.getSuperclass() is not supported in J2KT, as KClass.supertypes is available only on JVM.
  // There's no plan to support it, unless it's made available in Kotlin Common.
  @J2ktIncompatible
  private static void assertSuperclass(@Nullable Class<?> superClass, Class<?> clazz) {
    assertSame(superClass, clazz.getSuperclass());
  }

  // Overload used in J2KT.
  private static void assertSuperclass(
      @Nullable Class<?> superClass, Class<?> clazz, Object... unused) {
    if (!isJ2Kt()) {
      throw new AssertionError();
    }
  }
}
