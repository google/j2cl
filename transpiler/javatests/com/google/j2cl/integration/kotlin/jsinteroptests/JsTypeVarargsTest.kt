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
import com.google.j2cl.integration.testing.Asserts.assertNull
import com.google.j2cl.integration.testing.Asserts.assertSame
import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsConstructor
import jsinterop.annotations.JsFunction
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsPackage.GLOBAL
import jsinterop.annotations.JsType
import kotlin.js.definedExternally

object JsTypeVarargsTest {
  fun testAll() {
    testVarargsCall_constructors()
    testVarargsCall_fromJavaScript()
    testVarargsCall_jsFunction()
    testVarargsCall_regularMethods()
    testVarargsCall_edgeCases()
    testVarargsCall_superCalls()
    testVarargsCall_sideEffectingInstance()
    testVarargsCall_correctArrayType()
    testVarargsCall_interfaceMethods()
  }

  @JsMethod @JvmStatic private external fun varargsLengthThruArguments(vararg varargs: Any?): Int

  @JsMethod @JvmStatic private fun varargsLength(vararg varargs: Any?): Int = varargs.size

  @JsMethod @JvmStatic private fun stringVarargsLength(vararg varargs: String?): Int = varargs.size

  @JsMethod
  @JvmStatic
  private fun stringVarargsLengthV2(i: Int, vararg varargs: String?): Int = varargs.size

  @JsMethod
  @JvmStatic
  private fun getVarargsSlot(slot: Int, vararg varargs: Any?): Any? = varargs[slot]

  @Suppress("UNCHECKED_CAST")
  @JsMethod
  @JvmStatic
  private fun clrearVarargsSlot(slot: Int, vararg varargs: Any?): Array<Any?> {
    (varargs as Array<Any?>)[slot] = null
    return varargs
  }

  @JsMethod
  @JvmStatic
  private fun getVarargsArrayClass(vararg varargs: String?): Class<*> = varargs::class.java

  @JsMethod @JvmStatic private external fun callGetVarargsSlotUsingJsName(): Any?

  @JsType(isNative = true, namespace = GLOBAL, name = "Object") internal class NativeJsType

  @JsType(
    isNative = true,
    namespace = "test.foo",
    name = "JsTypeVarargsTest_MyNativeJsTypeVarargsConstructor",
  )
  internal open class NativeJsTypeWithVarargsConstructor
  internal constructor(i: Int, vararg args: Any?) {
    @JvmField var a: Any? = definedExternally
    @JvmField var b: Int = definedExternally
  }

  internal open class SubNativeWithVarargsConstructor : NativeJsTypeWithVarargsConstructor {
    internal constructor(s: String, vararg args: Any?) : this(1, args[0], args[1], null)

    @JsConstructor constructor(i: Int, vararg args: Any?) : super(i, *args)

    @JsMethod internal open fun varargsMethod(i: Int, vararg args: Any?): Any? = args[i]
  }

  internal class SubSubNativeWithVarargsConstructor @JsConstructor internal constructor() :
    SubNativeWithVarargsConstructor(0, Any()) {
    override fun varargsMethod(i: Int, vararg args: Any?): Any? = super.varargsMethod(i, *args)

    internal fun nonJsVarargsMethod(): Any? = super.varargsMethod(1, null, this)
  }

  private fun testVarargsCall_regularMethods() {
    assertEquals(3, varargsLengthThruArguments("A", "B", "C"))
    assertEquals(4, varargsLength("A", "B", "C", "D"))
    assertEquals(2, varargsLengthThruArguments(*arrayOfNulls<NativeJsType>(2)))
    assertEquals(5, varargsLength(*arrayOfNulls<NativeJsType>(5)))
    assertEquals("C", getVarargsSlot(2, "A", "B", "C", "D"))
    assertEquals("3", callGetVarargsSlotUsingJsName())
    assertNull(clrearVarargsSlot(1, "A", "B", "C")[1])
    assertEquals("A", clrearVarargsSlot(1, "A", "B", "C")[0])
    assertEquals(3, clrearVarargsSlot(1, "A", "B", "C").size)
    assertSame(Array<String>::class.java, getVarargsArrayClass("A", "B", "C"))
  }

  private fun testVarargsCall_edgeCases() {
    assertSame(Array<String>::class.java, getVarargsArrayClass())
    assertSame(Array<String>::class.java, getVarargsArrayClass(*emptyArray<String>()))
    assertSame(Array<String>::class.java, getVarargsArrayClass(null as String?))
    // Kotlin doesn't allow passing an array in place of varags, it must be spread. However, you
    // cannot spread a nullable type.
    // assertThrowsNullPointerException { getVarargsArrayClass(null) }
    // assertThrowsNullPointerException{ getVarargsArrayClass(null as Array<String>?) }
    assertEquals(0, stringVarargsLength())
    assertEquals(0, stringVarargsLength(*emptyArray<String>()))
    assertEquals(1, stringVarargsLength(null as String?))
    // Kotlin doesn't allow passing an array in place of varags, it must be spread. However, you
    // cannot spread a nullable type.
    // assertThrowsNullPointerException { stringVarargsLength(null) }
    // assertThrowsNullPointerException{ stringVarargsLength(null as Array<String>?) }

    // Test with an additional parameter as it results in a slightly different call site.
    assertEquals(0, stringVarargsLengthV2(0))
    assertEquals(0, stringVarargsLengthV2(0, *emptyArray<String>()))
    assertEquals(1, stringVarargsLengthV2(0, null as String?))
    // Kotlin doesn't allow passing an array in place of varags, it must be spread. However, you
    // cannot spread a nullable type.
    // assertThrowsNullPointerException { stringVarargsLengthV2(0, null) }
    // assertThrowsNullPointerException{ stringVarargsLengthV2(0, null as Array<String>?) }
  }

  private fun testVarargsCall_constructors() {
    val someNativeObject = NativeJsType()
    var o = NativeJsTypeWithVarargsConstructor(1, someNativeObject, null)

    assertSame(someNativeObject, o.a)
    assertEquals(3, o.b)

    val params = arrayOf<Any?>(someNativeObject, null)
    o = NativeJsTypeWithVarargsConstructor(1, *params)

    assertSame(someNativeObject, o.a)
    assertEquals(3, o.b)

    o = SubNativeWithVarargsConstructor(1, someNativeObject, null)

    assertSame(someNativeObject, o.a)
    assertEquals(3, o.b)
  }

  @JsMethod
  @JvmStatic
  fun sumAndMultiply(multiplier: Double, vararg numbers: Double): Double {
    var result = 0.0
    for (d in numbers) {
      result += d
    }
    result *= multiplier
    return result
  }

  @JsMethod
  @JvmStatic
  fun sumAndMultiplyInt(multiplier: Int, vararg numbers: Int): Int {
    var result = 0
    for (d in numbers) {
      result += d
    }
    result *= multiplier
    return result
  }

  @JsFunction
  internal fun interface Function {
    fun f(i: Int, vararg args: Any?): Any?
  }

  internal class AFunction : Function {

    @Override override fun f(i: Int, vararg args: Any?): Any? = args[i]

    companion object {
      @JvmStatic fun create() = AFunction()
    }
  }

  private fun testVarargsCall_fromJavaScript() {
    assertEquals(60, callSumAndMultiply())
    assertEquals(30, callSumAndMultiplyInt())
    val f: Function = AFunction.create()
    assertSame(f, callAFunction(f))
  }

  @JsMethod @JvmStatic private external fun callSumAndMultiply(): Int

  @JsMethod @JvmStatic private external fun callSumAndMultiplyInt(): Int

  @JsMethod @JvmStatic private external fun callAFunction(obj: Any?): Any?

  private fun testVarargsCall_jsFunction() {
    val function: Function = AFunction()
    assertSame(function, function.f(2, null, null, function, null))
    assertSame(null, function.f(1, null, null, function, null))
  }

  private fun testVarargsCall_superCalls() {
    val o = SubSubNativeWithVarargsConstructor()
    assertSame(o, o.nonJsVarargsMethod())
    assertSame(o, o.varargsMethod(1, null, o, null))
  }

  @JvmStatic private var sideEffectCount: Int = 0

  @JvmStatic
  private fun doSideEffect(obj: SubNativeWithVarargsConstructor): SubNativeWithVarargsConstructor {
    sideEffectCount++
    return obj
  }

  private fun testVarargsCall_sideEffectingInstance() {
    val arg = Any()
    val o = SubNativeWithVarargsConstructor(0, arg)
    sideEffectCount = 0
    val params = arrayOf<Any?>(o, null)
    assertSame(o, doSideEffect(o).varargsMethod(0, *params))
    assertSame(1, sideEffectCount)
  }

  @JsFunction
  internal fun interface JsStringConsumer {
    fun consume(vararg strings: String)
  }

  private fun testVarargsCall_correctArrayType() {
    val consumer = JsStringConsumer { strings -> assertTrue(strings.isArrayOf<String>()) }
    consumer.consume("A", "B")
  }

  @JsType
  internal interface InterfaceWithJsVarargsMethods {
    fun isDoubleArray(vararg varargs: Double): Boolean {
      // Use an Object typed variable to avoid the removal of the cast at compile time.
      val varargsAlias: Any? = varargs
      return varargsAlias is DoubleArray
    }

    companion object {
      @JvmStatic
      fun isIntArray(vararg varargs: Int): Boolean {
        // Use an Object typed variable to avoid the removal of the cast at compile time.
        val varargsAlias: Any? = varargs
        return varargsAlias is IntArray
      }
    }
  }

  private fun testVarargsCall_interfaceMethods() {
    assertTrue(object : InterfaceWithJsVarargsMethods {}.isDoubleArray())
    assertTrue(InterfaceWithJsVarargsMethods.isIntArray())
  }
}
