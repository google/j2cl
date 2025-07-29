/*
 * Copyright 2023 Google Inc.
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
package classliteral

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertNotNull
import com.google.j2cl.integration.testing.Asserts.assertNotSame
import com.google.j2cl.integration.testing.Asserts.assertSame
import com.google.j2cl.integration.testing.Asserts.assertThrowsNullPointerException
import com.google.j2cl.integration.testing.Asserts.assertTrue
import com.google.j2cl.integration.testing.TestUtils.isJavaScript
import com.google.j2cl.integration.testing.TestUtils.isJvm
import jsinterop.annotations.JsEnum
import jsinterop.annotations.JsFunction
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsType

fun main(vararg unused: String) {
  val main = Main()
  main.testClass()
  main.testInterface()
  main.testPrimitive()
  main.testEnum()
  main.testEnumSubclass()
  main.testJsEnum()
  main.testArray()
  main.testNative()
  main.testExtendsNative()
  main.testGeneric()
  main.testClinitNotTriggered()
  main.testGetClassOnClass()
  main.testLambda()
}

class Main {

  private inner class Foo

  fun testClass() {
    val o: Any = Main()
    assertSame(Main::class.java, o.javaClass)
    assertSame(Any::class.java, o.javaClass.superclass)
    assertSame(null, Any::class.java.superclass)

    assertEquals("classliteral.Main", Main::class.java.name)
    assertEquals("classliteral.Main\$Foo", Foo::class.java.name)

    assertEquals("classliteral.Main", Main::class.java.canonicalName)
    if (!isJvm()) {
      // J2CL doesn't follow JLS here:
      assertEquals("classliteral.Main\$Foo", Foo::class.java.canonicalName)
    } else {
      assertEquals("classliteral.Main.Foo", Foo::class.java.canonicalName)
    }

    assertEquals("Main", Main::class.java.simpleName)
    assertEquals("Foo", Foo::class.java.simpleName)

    assertEquals("class classliteral.Main", Main::class.java.toString())
    assertEquals("class classliteral.Main\$Foo", Foo::class.java.toString())

    assertLiteralType("Foo.class", LiteralType.CLASS, Foo::class.java)
  }

  private interface IFoo

  fun testInterface() {
    assertSame(null, IFoo::class.java.superclass)

    assertEquals("classliteral.Main\$IFoo", IFoo::class.java.name)
    if (!isJvm()) {
      // J2CL doesn't follow JLS here:
      assertEquals("classliteral.Main\$IFoo", IFoo::class.java.canonicalName)
    } else {
      assertEquals("classliteral.Main.IFoo", IFoo::class.java.canonicalName)
    }
    assertEquals("IFoo", IFoo::class.java.simpleName)
    assertEquals("interface classliteral.Main\$IFoo", IFoo::class.java.toString())

    assertLiteralType("IFoo.class", LiteralType.INTERFACE, IFoo::class.java)
  }

  fun testPrimitive() {
    assertSame(null, Int::class.java.superclass)

    assertEquals("int", Int::class.java.name)
    assertEquals("int", Int::class.java.canonicalName)
    assertEquals("int", Int::class.java.simpleName)
    assertEquals("int", Int::class.java.toString())

    assertEquals("int", 1.javaClass.name)
    assertEquals("int", 1.javaClass.canonicalName)
    assertEquals("int", 1.javaClass.simpleName)
    assertEquals("int", 1.javaClass.toString())

    assertLiteralType("int.class", LiteralType.PRIMITIVE, Int::class.java)
  }

  private enum class Bar {
    BAR,
    BAZ {
      val unused = 0
    },
  }

  fun testEnum() {
    val o: Any = Bar.BAR
    assertSame(Bar::class.java, o.javaClass)
    assertSame(Enum::class.java, o.javaClass.superclass)

    assertEquals("classliteral.Main\$Bar", o.javaClass.name)
    if (!isJvm()) {
      // J2CL doesn't follow JLS here:
      assertEquals("classliteral.Main\$Bar", o.javaClass.canonicalName)
    } else {
      assertEquals("classliteral.Main.Bar", o.javaClass.canonicalName)
    }
    assertEquals("Bar", o.javaClass.simpleName)
    assertEquals("class classliteral.Main\$Bar", o.javaClass.toString())

    assertLiteralType("Bar.BAR.getClass()", LiteralType.ENUM, o.javaClass)
  }

  fun testEnumSubclass() {
    val o: Any = Bar.BAZ
    assertNotSame(Bar::class.java, o.javaClass)
    assertSame(Bar::class.java, o.javaClass.superclass)

    assertEquals("classliteral.Main\$Bar\$BAZ", o.javaClass.name)
    assertEquals("class classliteral.Main\$Bar\$BAZ", o.javaClass.toString())

    assertLiteralType("Bar.BAZ.getClass()", LiteralType.CLASS, o.javaClass)
  }

  @JsEnum
  private enum class MyJsEnum {
    VALUE
  }

  fun testJsEnum() {
    val o: Any = MyJsEnum.VALUE
    assertSame(MyJsEnum::class.java, o.javaClass)
    assertSame(MyJsEnum::class.java, MyJsEnum.VALUE.javaClass)
    // In platforms other than JavaScript JsEnum behaves like a regular enum.
    assertSame(if (isJavaScript()) null else Enum::class.java, o.javaClass.superclass)

    assertEquals("classliteral.Main\$MyJsEnum", o.javaClass.name)
    if (!isJvm()) {
      // J2CL doesn't follow JLS here:
      assertEquals("classliteral.Main\$MyJsEnum", o.javaClass.canonicalName)
    } else {
      assertEquals("classliteral.Main.MyJsEnum", o.javaClass.canonicalName)
    }
    assertEquals("MyJsEnum", o.javaClass.simpleName)
    assertEquals("class classliteral.Main\$MyJsEnum", o.javaClass.toString())

    // In platforms other than JavaScript JsEnum behaves like a regular enum.
    assertLiteralType(
      "MyJsEnum.VALUE.class",
      if (isJavaScript()) LiteralType.CLASS else LiteralType.ENUM,
      o.javaClass,
    )
  }

  fun testArray() {
    val o: Any = arrayOfNulls<Foo>(3)
    assertSame(Array<Foo>::class.java, o.javaClass)
    assertSame(Any::class.java, o.javaClass.superclass)

    assertSame(Foo::class.java, o.javaClass.componentType)
    assertSame(Array<Foo>::class.java, Array<Array<Foo>>::class.java.componentType)

    assertEquals("[L" + Foo::class.java.name + ";", o.javaClass.name)
    assertEquals(Foo::class.java.canonicalName + "[]", o.javaClass.canonicalName)
    assertEquals(Foo::class.java.simpleName + "[]", o.javaClass.simpleName)
    assertEquals("class [L" + Foo::class.java.name + ";", o.javaClass.toString())

    assertLiteralType("Foo[].class", LiteralType.ARRAY, o.javaClass)

    val f = Array(3) { arrayOfNulls<Foo>(3) }
    assertSame(Array<Array<Foo>>::class.java, f.javaClass)
    assertSame(Array<Foo>::class.java, f[0].javaClass)
  }

  @JsFunction
  private fun interface NativeFunction {
    fun f()
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  private interface NativeInterface

  fun testNative() {
    // Native types don't apply in the context of the JVM.
    if (!isJavaScript()) {
      return
    }
    var clazz: Class<*> = NativeFunction::class.java
    assertEquals("<native function>", clazz.name)
    assertEquals(null, clazz.superclass)
    assertLiteralType("NativeFunction.class", LiteralType.INTERFACE, clazz)

    clazz = NativeInterface::class.java
    assertEquals("<native object>", clazz.name)
    assertEquals(null, clazz.superclass)
    assertLiteralType("NativeFunction.class", LiteralType.INTERFACE, clazz)

    clazz = NativeClass::class.java
    assertEquals("<native object>", clazz.name)
    assertEquals(Any::class.java, clazz.superclass)
    assertLiteralType("NativeFunction.class", LiteralType.CLASS, clazz)
  }

  fun testExtendsNative() {
    assertEquals("classliteral.TypeExtendsNativeClass", TypeExtendsNativeClass::class.java.name)

    // TODO(b/63081128): Uncomment when fixed.
    // assertEquals(NativeClass.class, TypeExtendsNativeClass.class.getSuperclass());

    assertLiteralType(
      "TypeExtendsNativeClass.class",
      LiteralType.CLASS,
      TypeExtendsNativeClass::class.java,
    )
  }

  private fun interface SomeFunctionalInterface {
    fun m()
  }

  private fun interface SomeOtherFunctionalInterface {
    fun m()
  }

  fun testLambda() {
    val originalLambda: SomeFunctionalInterface = SomeFunctionalInterface {}
    val lambdaWithSameInterfaceAsOriginalLambda: SomeFunctionalInterface =
      SomeFunctionalInterface {}

    // TODO(b/67589892): lambda instances are all instances of the same adaptor class, sharing
    // the class literal. This is by design.
    if (isJvm()) {
      // J2CL doesn't conform to differentiating the class instance for lambdas that implement
      // the same functional interface.
      assertNotSame(originalLambda.javaClass, lambdaWithSameInterfaceAsOriginalLambda.javaClass)
    }
    assertSame(originalLambda.javaClass, originalLambda.javaClass)

    // TODO(b/67589892): redundant after above assertTrue(are enabled, remove.
    // Lambda instances implementing different interfaces should have different class objects.
    val lambdaWithDifferentInterface = SomeOtherFunctionalInterface {}
    assertNotSame(originalLambda.javaClass, lambdaWithDifferentInterface.javaClass)

    assertLiteralType("lambda.getClass()", LiteralType.CLASS, originalLambda.javaClass)
  }

  private fun interface SomeFunctionalInterfaceWithParameter {
    fun m(a: Int)
  }

  // TODO: Implement once Kotlin has implemented intersection types:
  //   https://youtrack.jetbrains.com/issue/KT-13108
  // private interface MarkerInterface

  // fun testLambdaIntersectionType() {
  //   val intersectionLambda =
  //     SomeFunctionalInterfaceWithParameter { a: Int -> } as SomeFunctionalInterfaceWithParameter
  //   val anotherIntersectionLambda =
  //     SomeFunctionalInterfaceWithParameter { a: Int -> } as SomeFunctionalInterfaceWithParameter
  //   val lambdaWithSameInterfaceAsOriginalLambda: SomeFunctionalInterfaceWithParameter =
  //     SomeFunctionalInterfaceWithParameter { a: Int ->
  //     }

  //   assertSame(intersectionLambda.javaClass, intersectionLambda.javaClass)

  // if (isJvm()) {
  //   // J2CL doesn't conform to differentiating the class instance for anonymous types and
  //   // lambdas that implement functional interfaces.
  //   assertNotSame(intersectionLambda.javaClass, anotherIntersectionLambda.javaClass)
  //   assertNotSame(
  //     intersectionLambda.javaClass, lambdaWithSameInterfaceAsOriginalLambda.javaClass)
  // }

  //   assertLiteralType("lambda.getClass()", LiteralType.CLASS, intersectionLambda.javaClass)
  // }

  private class GenericClass<T>

  private interface GenericInterface<T>

  fun testGeneric() {
    val g = GenericClass<Number>()
    assertSame(GenericClass::class.java, g.javaClass)
    assertEquals("GenericClass", GenericClass::class.java.simpleName)
    assertEquals("GenericInterface", GenericInterface::class.java.simpleName)

    assertLiteralType("generic.getClass()", LiteralType.CLASS, g.javaClass)
  }

  internal var clinitCalled = false

  private inner class ClinitTest {
    init {
      clinitCalled = true
    }
  }

  fun testClinitNotTriggered() {
    assertFalse(clinitCalled)
    assertNotNull(ClinitTest::class.java)
    assertFalse(clinitCalled)
    assertNotNull(ClinitTest()::class.java)
    assertTrue(clinitCalled)
  }

  private val nullObject: Any? = null

  fun testGetClassOnClass() {
    assertSame(Class::class.java, Any::class.java.javaClass)
    assertSame(Class::class.java, Int::class.java.javaClass)
    assertSame(Class::class.java, Array<Any>::class.java.javaClass)

    assertThrowsNullPointerException {
      val unused: Any = nullObject!!.javaClass
    }
  }

  private enum class LiteralType {
    CLASS,
    INTERFACE,
    ARRAY,
    PRIMITIVE,
    ENUM,
  }

  private fun assertLiteralType(
    messagePrefix: String,
    expectedType: LiteralType,
    literal: Class<*>?,
  ) {
    assertEquals(
      "$messagePrefix.isInterface()",
      expectedType == LiteralType.INTERFACE,
      literal!!.isInterface,
    )
    assertEquals("$messagePrefix.isEnum()", expectedType == LiteralType.ENUM, literal.isEnum)
    assertEquals(
      "$messagePrefix.isPrimitive()",
      expectedType == LiteralType.PRIMITIVE,
      literal.isPrimitive,
    )
    assertEquals("$messagePrefix.isArray()", expectedType == LiteralType.ARRAY, literal.isArray)
  }

  fun testPrimtives() {
    assertSame(Int::class.java, 1::class.java)

    val nullableInt: Int? = 1
    assertSame(Int::class.javaObjectType, nullableInt!!::class.java)
    assertSame(Int::class.javaObjectType, requireNotNull(nullableInt)::class.java)

    class Holder<T>(val v: T) {
      val prop: T
        get() = v
    }

    assertSame(Int::class.javaObjectType, Holder(1).v::class.java)
    assertSame(Int::class.javaObjectType, Holder(1).prop::class.java)
    assertSame(Int::class.javaObjectType, Holder<Int?>(1).v!!::class.java)
    assertSame(Int::class.javaObjectType, Holder<Int?>(1).prop!!::class.java)

    // Will always be true but not trivially obvious to the compiler.
    val alwaysTrue = java.util.Random().nextInt() >= 0

    assertSame(Int::class.java, (if (alwaysTrue) 1 else 0)::class.java)
    assertSame(Int::class.java, (if (alwaysTrue) requireNotNull(nullableInt) else 0)::class.java)
    assertSame(
      Int::class.java,
      (if (alwaysTrue) requireNotNull(nullableInt) else requireNotNull(nullableInt))::class.java,
    )
  }
}
