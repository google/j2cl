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
package subnativejstype

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsConstructor
import jsinterop.annotations.JsProperty
import jsinterop.annotations.JsType
import kotlin.js.definedExternally

fun main(vararg args: String) {
  testJsTypeSubclassConstructor()
  testJsPropertyNativeJsTypeSubclass()
  testJsPropertyNativeJsTypeSubclassNoOverride()
}

private fun testJsTypeSubclassConstructor() {
  val mc = JsPropertyMyNativeJsTypeSubclass(0, 0)
  assertTrue(mc.ctorExecuted)
}

val GET_X: Int = 100

private fun testJsPropertyNativeJsTypeSubclass() {
  val mc = JsPropertyMyNativeJsTypeSubclass(10, 20)
  assertTrue(mc.x == 30)
  assertTrue(mc.y == 200)
  assertTrue(mc.f == 10)
  assertEquals(131, mc.sum(1))

  mc.x = -mc.x
  assertEquals(70, mc.sum(0))

  assertEquals(52, mc.getZ())
}

@JsType(isNative = true, namespace = "subnativejstype.JsPropertyTest", name = "MyNativeJsType")
open internal class JsPropertyMyNativeJsType {
  constructor(x: Int, y: Int)

  constructor()

  @JvmField var ctorExecuted: Boolean = definedExternally

  @JvmField var x: Int = definedExternally
  @JvmField var y: Int = definedExternally

  @JsProperty external fun getZ(): Int

  @JsProperty external fun setZ(z: Int)

  open external fun sum(bias: Int): Int

  companion object {
    @JvmField var staticX: Int = definedExternally

    @JvmStatic external fun answerToLife(): Int
  }
}

internal class JsPropertyMyNativeJsTypeSubclass : JsPropertyMyNativeJsType {
  var f: Int = 20

  @JsConstructor
  internal constructor(x: Int, y: Int) : super(x + y, x * y) {
    f += x - y
    setZ(52)
  }

  override fun sum(bias: Int): Int = super.sum(bias) + GET_X
}

private fun testJsPropertyNativeJsTypeSubclassNoOverride() {
  val myNativeJsType = JsPropertyMyNativeJsTypeSubclassNoOverride()
  myNativeJsType.x = 12
  assertEquals(42, myNativeJsType.sum(30))
}

internal class JsPropertyMyNativeJsTypeSubclassNoOverride @JsConstructor constructor() :
  JsPropertyMyNativeJsType()
