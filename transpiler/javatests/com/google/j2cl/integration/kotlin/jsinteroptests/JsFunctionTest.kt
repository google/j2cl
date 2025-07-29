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
package jsinteroptests

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertNotNull
import com.google.j2cl.integration.testing.Asserts.assertSame
import com.google.j2cl.integration.testing.Asserts.assertThrowsArrayStoreException
import com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException
import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsFunction
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsProperty
import jsinterop.annotations.JsType
import kotlin.random.Random

object JsFunctionTest {
  fun testAll() {
    testCast_crossCastJavaInstance()
    testCast_fromJsFunction()
    testCast_fromJsObject()
    testCast_inJava()
    testInstanceField()
    testInstanceOf_javaInstance()
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
  }

  @JsType(isNative = true, name = "RegExp", namespace = JsPackage.GLOBAL)
  private class NativeRegExp constructor(regEx: String) {
    external fun exec(s: String): Array<String>
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

  /**
   * A class that has a field of JsFunction type, and a method that accepts JsFunction parameter.
   */
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
    assertTrue(o is MyJsFunctionIdentityInterface)
    assertTrue(o is MyJsFunctionWithOnlyInstanceofReference)
    assertFalse(o is HTMLElementConcreteNativeJsType)
    val nullObject: Any? = null
    assertFalse(nullObject is MyJsFunctionInterface)
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

  @JsFunction
  fun interface JsFunctionInterfaceWithT<T> {
    fun m(): T
  }

  @JsMethod @JvmStatic private external fun createFunctionThatReturnsThis(): JsFunctionInterface

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

  private fun assertJsTypeDoesntHaveFields(obj: Any?, vararg fields: String) {
    for (field in fields) {
      assertFalse("Field '" + field + "' should not be exported", hasField(obj, field))
    }
  }

  @JsMethod @JvmStatic private external fun callAsFunctionNoArgument(fn: Any?): Any?

  @JsMethod @JvmStatic private external fun callAsFunction(fn: Any?, arg: Int): Int

  @JsMethod @JvmStatic private external fun callWithFunctionApply(fn: Any?, arg: Int): Int

  @JsMethod @JvmStatic private external fun callWithFunctionCall(fn: Any?, arg: Int): Int

  @JsMethod @JvmStatic private external fun setField(o: Any?, fieldName: String, value: Int)

  @JsMethod @JvmStatic private external fun getField(o: Any?, fieldName: String): Int

  @JsMethod @JvmStatic private external fun callIntFunction(o: Any?, functionName: String): Int

  @JsMethod @JvmStatic private external fun createMyJsFunction(): MyJsFunctionInterface

  @JsMethod
  @JvmStatic
  private external fun createReferentialFunction(): MyJsFunctionIdentityInterface

  @JsMethod @JvmStatic private external fun createFunction(): Any

  @JsMethod @JvmStatic private external fun createObject(): Any

  @JsMethod @JvmStatic private external fun hasField(o: Any?, fieldName: String): Boolean
}
