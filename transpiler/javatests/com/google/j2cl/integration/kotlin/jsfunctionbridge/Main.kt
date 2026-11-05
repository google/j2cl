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
package jsfunctionbridge

import com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException
import com.google.j2cl.integration.testing.Asserts.assertTrue
import com.google.j2cl.integration.testing.Asserts.assertUnderlyingTypeEquals
import com.google.j2cl.integration.testing.TestUtils.isJavaScript
import javaemul.internal.annotations.DoNotAutobox
import jsinterop.annotations.JsEnum
import jsinterop.annotations.JsFunction

fun main(vararg args: String) {
  testBridge()
  testBridge_parameterChecks()
  testBridge_jsEnumBoxing()
  testBridge_nativeReturnType()
}

private val FOO: ApplyFunction<String> =
  object : ApplyFunction<String> {
    val field: String = "a"

    override fun apply(s: String): String = s + field + `fun`()

    fun `fun`(): String = field + "d"
  }

private fun testBridge() {
  assertTrue("eaad" == callGeneric(FOO, "e"))
  assertTrue("eaad" == callParametric(FOO, "e"))
  assertTrue("eaad" == FOO.apply("e"))
  assertTrue("hello" == Identity().apply("hello"))
}

private fun testBridge_parameterChecks() {
  assertThrowsClassCastException { callGeneric(FOO, Any()) }
}

private fun testBridge_jsEnumBoxing() {
  // Lambda, function reference and anonymous object implementing a parametric @JsFunction
  // specialized to a non-native JsEnum, so the bridge for apply(Any?) unboxes the argument and
  // boxes the result.
  checkApplyFunctionBridge(ApplyFunction<PlainJsEnum> { e -> nextPlainJsEnum(e) })
  checkApplyFunctionBridge(ApplyFunction<PlainJsEnum>(::nextPlainJsEnum))
  checkApplyFunctionBridge(
    object : ApplyFunction<PlainJsEnum> {
      override fun apply(e: PlainJsEnum): PlainJsEnum = nextPlainJsEnum(e)
    }
  )

  // Lambda and anonymous object implementing an Any?-returning @JsFunction returning a non-native
  // JsEnum, so the bridge for get() boxes the result.
  checkObjectSupplierBridge(ObjectSupplier { PlainJsEnum.ONE })
  checkObjectSupplierBridge(
    object : ObjectSupplier {
      override fun get(): PlainJsEnum = PlainJsEnum.ONE
    }
  )

  checkBridge_jsEnumTypeVariable(PlainJsEnum.ONE)
}

@Suppress("UNCHECKED_CAST")
private fun checkApplyFunctionBridge(f: ApplyFunction<PlainJsEnum>) {
  assertTrue(f.apply(PlainJsEnum.ONE) === PlainJsEnum.TWO)
  assertUnboxedPlainJsEnum(f.apply(PlainJsEnum.ONE))
  val rawF = f as ApplyFunction<Any?>
  assertTrue(rawF.apply(PlainJsEnum.ONE) === PlainJsEnum.TWO)
  assertBoxedPlainJsEnum(rawF.apply(PlainJsEnum.ONE))
  assertThrowsClassCastException { rawF.apply(OtherPlainJsEnum.UNIT) }
}

private fun checkObjectSupplierBridge(s: ObjectSupplier) {
  assertTrue(s.get() === PlainJsEnum.ONE)
  assertBoxedPlainJsEnum(s.get())
}

@Suppress("UNCHECKED_CAST")
private fun <T : PlainJsEnum> checkBridge_jsEnumTypeVariable(t: T) {
  // Lambda implementing a parametric @JsFunction specialized to a JsEnum-bounded type variable.
  val f =
    ApplyFunction<T> { x ->
      assertUnboxedPlainJsEnum(x)
      x
    }
  assertTrue(f.apply(t) === t)
  assertUnboxedPlainJsEnum(f.apply(t))
  val rawF = f as ApplyFunction<Any?>
  assertBoxedPlainJsEnum(rawF.apply(t))
  assertThrowsClassCastException { rawF.apply(OtherPlainJsEnum.UNIT) }
}

private fun testBridge_nativeReturnType() {
  // Lambda implementing a parametric @JsFunction specialized to a native JsEnum.
  val nativeJsEnumFunction =
    ApplyFunction<NativeEnum> { e -> if (e === NativeEnum.ACCEPT) NativeEnum.CANCEL else e }
  assertTrue(nativeJsEnumFunction.apply(NativeEnum.ACCEPT) === NativeEnum.CANCEL)
}

@Suppress("UNCHECKED_CAST")
private fun callGeneric(af: ApplyFunction<*>, o: Any?): Any? = (af as ApplyFunction<Any?>).apply(o)

private fun callParametric(af: ApplyFunction<String>, s: String): String = af.apply(s)

@JsFunction
fun interface ApplyFunction<T> {
  fun apply(element: T): T
}

private class Identity : ApplyFunction<Any?> {
  override fun apply(element: Any?): Any? = element
}

@JsEnum
enum class PlainJsEnum {
  ONE,
  TWO,
}

@JsEnum
enum class OtherPlainJsEnum {
  UNIT
}

@JsEnum(isNative = true, namespace = "jsfunctionbridge")
enum class NativeEnum {
  ACCEPT,
  CANCEL,
}

@JsFunction
fun interface ObjectSupplier {
  fun get(): Any?
}

private fun nextPlainJsEnum(e: PlainJsEnum): PlainJsEnum {
  assertUnboxedPlainJsEnum(e)
  return if (e === PlainJsEnum.ONE) PlainJsEnum.TWO else e
}

private fun assertUnboxedPlainJsEnum(@DoNotAutobox o: Any?) {
  if (isJavaScript()) {
    // JsEnums are only unboxed in JavaScript.
    assertUnderlyingTypeEquals(Double::class.javaObjectType, o)
  }
}

private fun assertBoxedPlainJsEnum(@DoNotAutobox o: Any?) {
  if (isJavaScript()) {
    // JsEnums are only boxed in JavaScript.
    assertUnderlyingTypeEquals(PlainJsEnum::class.java, o)
  }
}
