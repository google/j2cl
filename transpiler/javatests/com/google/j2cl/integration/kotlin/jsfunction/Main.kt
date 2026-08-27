/*
 * Copyright 2022 Google Inc.
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
package jsfunction

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertNotNull
import com.google.j2cl.integration.testing.Asserts.assertSame
import com.google.j2cl.integration.testing.Asserts.assertThrowsArrayStoreException
import com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException
import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsConstructor
import jsinterop.annotations.JsFunction
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsOverlay
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsProperty
import jsinterop.annotations.JsType
import kotlin.random.Random

fun main(vararg unused: String) {
  testJsFunction()
  testSpecializedJsFunction()
  testParameterizedJsFunctionMethod()
  testInvokableJsFunction()
  testCast_crossCastJavaInstance()
  testCast_fromJsFunction()
  testCast_fromJsObject()
  testCast_inJava()
  testInstanceField()
  testInstanceOf_javaInstance()
  testInstanceOf_javaInstance_jsFunction()
  testInstanceOf_javaInstance_nativeType()
  testInstanceOf_jsFunction()
  testInstanceOf_jsObject()
  testJsFunctionAccess()
  testJsFunctionBasic_java()
  testJsFunctionBasic_javaAndJs()
  testJsFunctionBasic_js()
  testJsFunctionCallbackPattern()
  testJsFunctionCallFromAMember()
  testJsFunctionIdentity_java()
  testJsFunctionIdentity_js()
  // TODO(b/63941038): enable this test
  // testJsFunctionIdentity_ctor();
  testJsFunctionJs2Java()
  testJsFunctionProperty()
  testJsFunctionReferentialIntegrity()
  testJsFunctionSuccessiveCalls()
  testJsFunctionViaFunctionMethods()
  testGetClass()
  testJsFunctionOptimization()
  testJsFunctionWithVarArgs()
  testJsFunctionLambda()
  testJsFunctionArray()
  testJsFunctionCalls_autoboxing()
  testJsFunctionWithNativeType()
  testJsFunctionWithJsType()
}

@JsFunction
internal fun interface Function {
  fun call(): Boolean

  @JsOverlay
  fun overlay(): Int {
    return f + if (call()) 1 else 2
  }

  companion object {
    @JvmField // TODO(b/570022569): remove when JsInterop support Kotlin construct.
    @JsOverlay
    val f = 1
  }
}

@JsFunction
internal fun interface FunctionWithStaticOverlay {
  fun call(): Boolean

  companion object {
    @JsOverlay
    @JvmStatic // TODO(b/570022569): remove when JsInterop support Kotlin construct.
    fun overlay(): Int {
      return 4
    }
  }
}

@JsFunction
internal fun interface FunctionWithStaticField {
  fun call(): Boolean

  // TODO(b/303321920): Remove when JsFunction with only static field produces an Overlay class.
  @JsOverlay
  fun overlay(): Int {
    return 0
  }

  companion object {
    @JvmField // TODO(b/570022569): remove when JsInterop support Kotlin construct.
    @JsOverlay
    val f = 1
  }
}

private fun testJsFunction() {
  assertTrue(Function { true }.overlay() == 2)
  assertTrue(Function { false }.overlay() == 3)
  assertTrue(FunctionWithStaticOverlay.overlay() == 4)
  assertTrue(FunctionWithStaticField.f == 1)
}

private fun testSpecializedJsFunction() {
  val stringConsumer = Consumer { s: String -> s + "!" }
  val anyConsumer: Consumer<Any> = stringConsumer as Consumer<Any>
  assertThrowsClassCastException({ anyConsumer.accept(Any()) }, String::class.java)
}

@JsFunction
internal fun interface Consumer<T> {
  fun accept(t: T)
}

@JsFunction
internal fun interface ParameterizedInterface<T> {
  fun f(t: T?): T?
}

fun <T> identity(t: T?): T? {
  return t
}

fun <T> nullFn(t: T?): T? {
  return null
}

private fun testParameterizedJsFunctionMethod() {
  open class A {
    open fun m(): String {
      return "HelloA"
    }
  }

  class B : A() {
    override fun m(): String {
      return "HelloB"
    }
  }

  var FALSE = false

  val parameterInterfaceFn: ParameterizedInterface<B>
  // Use a mutable global to make the kotlin frontend not know which method is actually passed so
  // that so the construct is actually materialized in the AST.
  if (FALSE) {
    parameterInterfaceFn = ParameterizedInterface<B>(::nullFn)
  } else {
    parameterInterfaceFn = ParameterizedInterface<B>(::identity)
  }
  A()
  assertEquals("HelloB", parameterInterfaceFn.f(B())!!.m())
}

@JsFunction
fun interface InvokableFunction {
  operator fun invoke(a: Int): Int
}

fun testInvokableJsFunction() {
  val f: InvokableFunction = InvokableFunction { a: Int -> a + 1 }
  assertEquals(2, f(1))
}

@JsType(isNative = true, namespace = "test.foo")
interface ElementLikeNativeInterface {
  @JsProperty fun getTagName(): String
}

@JsType(isNative = true, name = "RegExp", namespace = JsPackage.GLOBAL)
private class NativeRegExp constructor(regEx: String) {
  external fun exec(s: String): Array<String>

  external fun test(s: String): Boolean
}

@JsFunction
fun interface MyJsFunctionInterface {
  fun foo(a: Int): Int
}

@JsFunction
fun interface MyJsFunctionIdentityInterface {
  fun identity(): Any?
}

/** A JsFunction interface. */
@JsFunction
fun interface MyOtherJsFunctionInterface {
  fun bar(a: Int): Int
}

/** A functional interface annotated by JsFunction that is only referenced by instanceof. */
@JsFunction
fun interface MyJsFunctionWithOnlyInstanceofReference {
  fun foo(a: Int): Int
}

/** A concrete class that implements a JsFunction interface. */
class MyJsFunctionInterfaceImpl : MyJsFunctionInterface {
  @JvmField var publicField: Int = 10

  fun callFoo(a: Int): Int {
    // to prevent optimizations from inlining function foo.
    return 5 + foo(if (Random.Default.nextDouble() > -1.0) a else -a)
  }

  override fun foo(a: Int): Int = a + 1
}

@JsType(namespace = JsPackage.GLOBAL, name = "HTMLElement", isNative = true)
internal class HTMLElementConcreteNativeJsType {}

/** A class that has a field of JsFunction type, and a method that accepts JsFunction parameter. */
class MyClassAcceptsJsFunctionAsCallBack {

  private var callBack: MyJsFunctionInterface? = null

  fun setCallBack(callBack: MyJsFunctionInterface) {
    this.callBack = callBack
  }

  fun triggerCallBack(a: Int): Int = callBack!!.foo(a)
}

// separate java call and js calls into two tests to see if it works correctly.
private fun testJsFunctionBasic_js() {
  val jsFunctionInterface =
    object : MyJsFunctionInterface {
      override fun foo(a: Int): Int = a + 2
    }
  assertEquals(12, callAsFunction(jsFunctionInterface, 10))
}

private fun testJsFunctionBasic_java() {
  val jsFunctionInterface =
    object : MyJsFunctionInterface {
      override fun foo(a: Int): Int = a + 2
    }
  assertEquals(12, jsFunctionInterface.foo(10))
}

private fun testJsFunctionBasic_javaAndJs() {
  val jsFunctionInterface =
    object : MyJsFunctionInterface {
      override fun foo(a: Int): Int = a + 2
    }
  assertEquals(12, jsFunctionInterface.foo(10))
  assertEquals(13, callAsFunction(jsFunctionInterface, 11))
}

private fun testJsFunctionViaFunctionMethods() {
  val jsFunctionInterface =
    object : MyJsFunctionInterface {
      override fun foo(a: Int): Int = a + 2
    }
  assertEquals(12, callWithFunctionApply(jsFunctionInterface, 10))
  assertEquals(12, callWithFunctionCall(jsFunctionInterface, 10))
}

private fun testJsFunctionIdentity_js() {
  val id =
    object : MyJsFunctionIdentityInterface {
      override fun identity(): Any? = this
    }
  assertEquals(id, callAsFunctionNoArgument(id))
}

private fun testJsFunctionIdentity_java() {
  val id =
    object : MyJsFunctionIdentityInterface {
      override fun identity(): Any? = this
    }
  assertTrue((id == id.identity()))
}

private class MyJsFunctionIdentityInConstructor : MyJsFunctionIdentityInterface {

  var storedThis: MyJsFunctionIdentityInterface? = null

  constructor() {
    storedThis = this as MyJsFunctionIdentityInterface
  }

  override fun identity(): Any? = this
}

private fun testJsFunctionIdentity_ctor() {
  val id = MyJsFunctionIdentityInConstructor()
  assertTrue((id.storedThis == id.identity()))
}

private fun testJsFunctionAccess() {
  val intf =
    object : MyJsFunctionInterface {
      val publicField: Int = 0

      override fun foo(a: Int): Int = a
    }
  assertJsTypeDoesntHaveFields(intf, "foo")
  assertJsTypeDoesntHaveFields(intf, "publicField")
}

private fun testJsFunctionCallFromAMember() {
  val impl = MyJsFunctionInterfaceImpl()
  assertEquals(16, impl.callFoo(10))
}

private fun testJsFunctionJs2Java() {
  val intf: MyJsFunctionInterface = createMyJsFunction()
  assertEquals(10, intf.foo(10))
}

private fun testJsFunctionSuccessiveCalls() {
  assertEquals(
    12,
    object : MyJsFunctionInterface {
        override fun foo(a: Int): Int = a + 2
      }
      .foo(10),
  )
  assertEquals(10, createMyJsFunction().foo(10))
}

private fun testJsFunctionCallbackPattern() {
  val c = MyClassAcceptsJsFunctionAsCallBack()
  c.setCallBack(createMyJsFunction())
  assertEquals(10, c.triggerCallBack(10))
}

private fun testJsFunctionReferentialIntegrity() {
  val intf: MyJsFunctionIdentityInterface = createReferentialFunction()
  assertEquals(intf, intf.identity())
}

private fun testCast_fromJsFunction() {
  val c1 = createFunction() as MyJsFunctionInterface
  assertNotNull(c1)
  val c2 = createFunction() as MyJsFunctionIdentityInterface
  assertNotNull(c2)
  val i = createFunction() as ElementLikeNativeInterface
  assertNotNull(i)
  assertThrowsClassCastException {
    val unused = createFunction() as MyJsFunctionInterfaceImpl
  }
}

private fun testCast_fromJsObject() {
  val obj = createObject() as ElementLikeNativeInterface
  assertNotNull(obj)
  assertThrowsClassCastException {
    val unused = createObject() as MyJsFunctionInterface
  }
  assertThrowsClassCastException {
    val unused = createObject() as MyJsFunctionInterfaceImpl
  }
  assertThrowsClassCastException {
    val unused = createObject() as MyJsFunctionIdentityInterface
  }
}

private fun testCast_inJava() {
  val o: Any = MyJsFunctionInterfaceImpl()
  val c1 = o as MyJsFunctionInterface
  assertNotNull(c1)
  val c2 = c1 as MyJsFunctionInterfaceImpl
  assertEquals(10, c2.publicField)
  val c3 = o as MyJsFunctionInterfaceImpl
  assertNotNull(c3)
  val c4 = o as MyJsFunctionIdentityInterface
  assertNotNull(c4)
  val c5 = o as ElementLikeNativeInterface
  assertNotNull(c5)
  assertThrowsClassCastException {
    val unused = o as HTMLElementConcreteNativeJsType

  }
}

private fun testCast_crossCastJavaInstance() {
  val o: Any = MyJsFunctionInterfaceImpl()
  assertEquals(11, (o as MyOtherJsFunctionInterface).bar(10))
  assertSame(o as MyJsFunctionInterface, o as MyOtherJsFunctionInterface)
}

private fun testInstanceOf_jsFunction() {
  val o: Any = createFunction()
  assertTrue(o is MyJsFunctionInterface)
  assertTrue(o is MyJsFunctionIdentityInterface)
  assertTrue(o is MyJsFunctionWithOnlyInstanceofReference)
}

private fun testInstanceOf_jsObject() {
  val o: Any = createObject()
  assertFalse(o is MyJsFunctionInterface)
  assertFalse(o is MyJsFunctionIdentityInterface)
  assertFalse(o is MyJsFunctionWithOnlyInstanceofReference)
}

private fun testInstanceOf_javaInstance() {
  val o: Any = MyJsFunctionInterfaceImpl()
  assertTrue(o is MyJsFunctionInterface)
  val nullObject: Any? = null
  assertFalse(nullObject is MyJsFunctionInterface)
}

private fun testInstanceOf_javaInstance_jsFunction() {
  val o: Any = MyJsFunctionInterfaceImpl()
  assertTrue(o is MyJsFunctionIdentityInterface)
  assertTrue(o is MyJsFunctionWithOnlyInstanceofReference)
}

private fun testInstanceOf_javaInstance_nativeType() {
  val o: Any = MyJsFunctionInterfaceImpl()
  assertFalse(o is HTMLElementConcreteNativeJsType)
}

private fun testGetClass() {
  val jsfunctionImplementation =
    object : MyJsFunctionInterface {
      override fun foo(a: Int): Int = a
    }
  assertEquals(MyJsFunctionInterface::class.java, jsfunctionImplementation.javaClass)
  assertEquals(MyJsFunctionInterface::class.java, (jsfunctionImplementation as Any).javaClass)
  assertEquals(MyJsFunctionInterface::class.java, createMyJsFunction().javaClass)
  assertEquals(MyJsFunctionInterface::class.java, (createMyJsFunction() as Any).javaClass)
}

private fun testJsFunctionOptimization() {
  val lambda = MyJsFunctionInterface { a -> a }

  // inner class optimizable to lambda
  val optimizableInner =
    object : MyJsFunctionInterface {
      override fun foo(a: Int): Int = a
    }
  assertEquals(MyJsFunctionInterface::class.java, optimizableInner.javaClass)

  // Look at the structure of the two functions to make sure they are plain functions. They should
  // look something like
  //
  //     "function <fn>( /** type */ <par>) { return <par>; }"
  //
  val functionRegExp =
    NativeRegExp(
      "function [\\w$]*\\(\\s*(?:\\/\\*.*\\*\\/)?\\s*([\\w$]+)\\)\\s*{\\s*return \\1;\\s*}"
    )
  //
  //  or "(/** type */ <par>)=>{ return <par>;}"
  //
  val arrowRegExp =
    NativeRegExp("\\(\\s*(?:\\/\\*.*\\*\\/)?\\s*([\\w$]+)\\)\\s*=>\\s*{\\s*return \\1;\\s*}")

  //
  //  or "<par>=><par>"
  //
  val es6ArrowRegExp = NativeRegExp("\\s*(?:\\/\\*.*\\*\\/)?\\s*([\\w$]+)\\s*=>\\s*\\1\\s*")

  assertTrue(
    functionRegExp.exec(optimizableInner.toString()) != null ||
      arrowRegExp.exec(optimizableInner.toString()) != null ||
      es6ArrowRegExp.exec(optimizableInner.toString()) != null
  )
  assertTrue(
    functionRegExp.exec(lambda.toString()) != null ||
      arrowRegExp.exec(lambda.toString()) != null ||
      es6ArrowRegExp.exec(lambda.toString()) != null
  )

  // inner class not optimizable to lambda
  val unoptimizableInner =
    object : MyJsFunctionInterface {
      override fun foo(a: Int): Int = id(a)

      private fun id(a: Int): Int = a
    }
  assertEquals(MyJsFunctionInterface::class.java, unoptimizableInner.javaClass)
}

private fun testInstanceField() {
  val jsfunctionImplementation =
    object : MyJsFunctionInterface {
      val hello = Any().javaClass.name

      override fun foo(a: Int): Int = hello.length + a
    }
  assertEquals(Any::class.java.name.length + 4, jsfunctionImplementation.foo(4))
}

@JsFunction
fun interface JsFunctionInterface {
  fun m(): Any?
}

@JsMethod(namespace = "jsfunction.JsFunctionTestHelper")
private external fun createFunctionThatReturnsThis(): JsFunctionInterface

private fun testJsFunctionProperty() {
  class JsFuncionProperty {
    @JsProperty val func: JsFunctionInterface = createFunctionThatReturnsThis()

    @JsProperty fun getF(): JsFunctionInterface = createFunctionThatReturnsThis()
  }

  val array = arrayOf(createFunctionThatReturnsThis())
  val instance = JsFuncionProperty()
  var funcInVar: JsFunctionInterface? = null

  // Field
  assertTrue(instance != instance.func.m())
  // Assert that "this" is bound to the same object regardless of whether the calls is made
  // directly or from variable.
  funcInVar = instance.func
  assertSame(funcInVar.m(), instance.func.m())

  // Getter
  assertTrue(instance != instance.getF().m())
  // Assert that "this" is bound to the same object regardless of whether the calls is made
  // directly or from variable.
  funcInVar = instance.getF()
  assertSame(funcInVar.m(), instance.getF().m())

  // Array Access
  assertTrue(array != array[0].m())
  // Assert that "this" is bound to the same object regardless of whether the calls is made
  // directly or from variable.
  funcInVar = array[0]
  assertSame(funcInVar.m(), array[0].m())

  // Parenthesized
  assertTrue(instance != (instance.func).m())
  // Assert that "this" is bound to the same object regardless of whether the calls is made
  // directly or from variable.
  funcInVar = instance.func
  assertSame(funcInVar.m(), (instance.func).m())

  // Conditional expression
  // Currently there is no way to write it in Java without parenthesis but the parenthesis might
  // be dropped in the future.
  assertTrue(instance != (if (instance != null) instance.func else instance.func).m())
  // Assert that "this" is bound to the same object regardless of whether the calls is made
  // directly or from variable.
  funcInVar = if (instance != null) instance.func else instance.func
  assertSame(funcInVar.m(), (if (instance != null) instance.func else instance.func).m())
}

@JsFunction
fun interface JsFunctionWithVarargs {
  fun f(n: Int, vararg numbers: Int): Int
}

internal class JsFunctionWithVarargsOptimizable : JsFunctionWithVarargs {
  override fun f(n: Int, vararg numbers: Int): Int = numbers[n]
}

internal class JsFunctionWithVarargsNonOptimizable : JsFunctionWithVarargs {
  override fun f(n: Int, vararg numbers: Int): Int {
    accum = numbers[n]
    return accum
  }

  var accum: Int = 0
}

internal open class JsFunctionWithVarargsTestSuper {
  open fun m(): Int = 5
}

internal class JsFunctionWithVarargsTestSub : JsFunctionWithVarargsTestSuper() {
  var instanceField: Int = 5

  override fun m(): Int = 3

  fun test() {
    // Access through super
    assertEquals(8, JsFunctionWithVarargs { n, numbers -> numbers[n] + super.m() }.f(1, 1, 3))
    // Access through this (instanceField)
    assertEquals(8, JsFunctionWithVarargs { n, numbers -> numbers[n] + instanceField }.f(1, 1, 3))
  }
}

private fun testJsFunctionWithVarArgs() {
  assertEquals(3, (JsFunctionWithVarargsOptimizable() as JsFunctionWithVarargs).f(1, 1, 3))
  assertEquals(3, (JsFunctionWithVarargsNonOptimizable() as JsFunctionWithVarargs).f(1, 1, 3))
  assertEquals(3, (JsFunctionWithVarargs { n, numbers -> numbers[n] }).f(1, 1, 3))
  // Kotlin doesn't allow expressing varargs in a lambda so this isn't representable.
  // assertEquals(3, ((JsFunctionWithVarargs) (int n, int... numbers) -> numbers[n]).f(1, 1, 3));
  assertEquals(3, JsFunctionWithVarargs { n: Int, numbers: IntArray -> numbers[n] }.f(1, 1, 3))

  JsFunctionWithVarargsTestSub().test()
}

private fun testJsFunctionLambda() {
  val jsFunctionInterface = MyJsFunctionInterface { a -> a + 2 }
  assertEquals(12, callAsFunction(jsFunctionInterface, 10))
  assertEquals(12, jsFunctionInterface.foo(10))
}

private fun testJsFunctionArray() {
  val functionArray: Array<MyJsFunctionInterface?> = arrayOfNulls(1)
  functionArray[0] = MyJsFunctionInterface { a -> a + 2 }

  assertThrowsArrayStoreException {
    val temp = functionArray as Array<Any?>
    // Storing anything other than a function throws.
    temp[0] = 1
  }

  val function2dArray: Array<Array<MyJsFunctionInterface?>> = arrayOf(functionArray)

  assertThrowsArrayStoreException {
    val temp = function2dArray as Array<Array<*>>
    // Trying to store an integer array as a JsFunction array throws.
    temp[0] = arrayOfNulls<Int>(1)
  }

  assertThrowsClassCastException {
    // Casting an integer array to a JsFunction array throws.
    val o: Any = arrayOfNulls<Int>(1)
    val temp = o as Array<JsFunctionInterface>
  }
}

private fun testJsFunctionCalls_autoboxing() {
  val fn: ParameterizedInterface<Int> = { arg -> arg!! + 1 }
  val result: Int = fn.f(100)!!
  assertEquals(101, result)
}

@JsFunction
private fun interface JsFunctionWithNativeType {
  fun f(regExp: NativeRegExp): NativeRegExp
}

@JsMethod(namespace = "jsfunction.JsFunctionTestHelper")
private external fun callAsFunctionWithNativeType(
  fn: JsFunctionWithNativeType,
  arg: NativeRegExp,
): NativeRegExp

@JsMethod(namespace = "jsfunction.JsFunctionTestHelper", name = "createFunction")
private external fun createJsFunctionWithNativeType(): JsFunctionWithNativeType

private fun testJsFunctionWithNativeType() {
  val regExp = NativeRegExp("a")
  assertTrue(
    (JsFunctionWithNativeType { a ->
        // Make a simple call to ensure the argument isn't just blindly being passed
        // and that proper conversions are taking place.
        a.test("a")
        a
      })
      .f(regExp) === regExp
  )

  val fn = JsFunctionWithNativeType { a -> a }
  assertTrue(callAsFunctionWithNativeType(fn, regExp) === regExp)

  val fnFromJs = createJsFunctionWithNativeType()
  assertTrue(fnFromJs.f(regExp) === regExp)
}

@JsType class SomeJsType @JsConstructor constructor()

@JsFunction
fun interface JsFunctionWithJsType {
  fun f(jsType: SomeJsType): SomeJsType
}

@JsMethod(namespace = "jsfunction.JsFunctionTestHelper")
private external fun callAsFunctionWithJsType(fn: JsFunctionWithJsType, arg: SomeJsType): SomeJsType

@JsMethod(namespace = "jsfunction.JsFunctionTestHelper")
private external fun createJsFunctionWithJsType(): JsFunctionWithJsType

private fun testJsFunctionWithJsType() {
  val jsType = SomeJsType()
  assertTrue((JsFunctionWithJsType { a -> a }).f(jsType) === jsType)

  val fn = JsFunctionWithJsType { a -> a }
  assertTrue(callAsFunctionWithJsType(fn, jsType) === jsType)

  val fnFromJs = createJsFunctionWithJsType()
  assertTrue(fnFromJs.f(jsType) === jsType)
}

private fun assertJsTypeDoesntHaveFields(obj: Any?, vararg fields: String) {
  for (field in fields) {
    assertFalse("Field '" + field + "' should not be exported", hasField(obj, field))
  }
}

@JsMethod(namespace = "jsfunction.JsFunctionTestHelper")
private external fun callAsFunctionNoArgument(fn: Any?): Any?

@JsMethod(namespace = "jsfunction.JsFunctionTestHelper")
private external fun callAsFunction(fn: Any?, arg: Int): Int

@JsMethod(namespace = "jsfunction.JsFunctionTestHelper")
private external fun callWithFunctionApply(fn: Any?, arg: Int): Int

@JsMethod(namespace = "jsfunction.JsFunctionTestHelper")
private external fun callWithFunctionCall(fn: Any?, arg: Int): Int

@JsMethod(namespace = "jsfunction.JsFunctionTestHelper")
private external fun createMyJsFunction(): MyJsFunctionInterface

@JsMethod(namespace = "jsfunction.JsFunctionTestHelper")
private external fun createReferentialFunction(): MyJsFunctionIdentityInterface

@JsMethod(namespace = "jsfunction.JsFunctionTestHelper") private external fun createFunction(): Any

@JsMethod(namespace = "jsfunction.JsFunctionTestHelper") private external fun createObject(): Any

@JsMethod(namespace = "jsfunction.JsFunctionTestHelper")
private external fun hasField(o: Any?, fieldName: String): Boolean
