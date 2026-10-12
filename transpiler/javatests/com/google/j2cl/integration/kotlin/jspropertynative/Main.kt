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
package jspropertynative

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsProperty
import jsinterop.annotations.JsType
import kotlin.js.definedExternally

fun main(vararg args: String) {
  testJsPropertyGetY()
  testJsPropertyGetY_undefined()
  testJsPropertyIsX()
  testNativeJsType()
  testNativeJsTypeWithConstructor()
  testNativeStaticJsProperty()
  testNativeInstanceJsProperty()
}

@JsType(isNative = true, namespace = "jspropertynative", name = "MyNativeJsType")
open internal class MyNativeJsType {
  constructor(n: Int)

  constructor()

  @JvmField var ctorExecuted: Boolean = definedExternally

  @JvmField var x: Int = definedExternally

  @JsProperty external fun getY(): Int

  @JsProperty external fun setY(x: Int)

  open external fun sum(bias: Int): Int

  companion object {
    @JvmField var staticX: Int = definedExternally
  }
}

private fun testNativeJsType() {
  MyNativeJsType.staticX = 34
  assertEquals(34, MyNativeJsType.staticX)

  val obj: MyNativeJsType = createMyNativeJsType()
  assertTrue(obj.ctorExecuted)
  assertEquals(obj.x, 0)
  obj.x = 72
  assertEquals(72, obj.x)
  assertEquals(74, obj.sum(2))

  assertEquals(0, obj.getY())
  obj.setY(91)
  assertEquals(91, obj.getY())
}

@JsType(isNative = true, namespace = "jspropertynative", name = "MyNativeJsType")
open internal class MyNativeJsTypeWithConstructor(x: Int) {
  @JvmField var ctorExecuted: Boolean = definedExternally
  @JvmField var x: Int = definedExternally
}

private fun testNativeJsTypeWithConstructor() {
  val obj = MyNativeJsTypeWithConstructor(12)
  assertTrue(obj.ctorExecuted)
  assertEquals(12, obj.x)
}

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
internal interface HasIsX {

  @JsProperty fun isX(): Boolean

  @JsProperty fun setX(x: Boolean)
}

private fun testJsPropertyIsX() {
  val o: HasIsX = createHasIsX()
  o.setX(true)
  assertTrue(o.isX())
  o.setX(false)
  assertFalse(o.isX())
}

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
internal interface HasGetY {

  @JsProperty fun getY(): Int

  @JsProperty fun setY(x: Int)
}

private fun testJsPropertyGetY_undefined() {
  val o: HasGetY = createHasGetY()
  assertTrue(isUndefined(o.getY()))
}

private fun testJsPropertyGetY() {
  val o: HasGetY = createHasGetY()
  o.setY(10)
  assertEquals(10, o.getY())
  o.setY(0)
  assertEquals(0, o.getY())
}

class NonNativeTypeWithNativeProperties {
  @JsProperty(name = "hasOwnProperty") external fun getA(): Any?

  companion object {
    @JsProperty(name = "Math.PI", namespace = JsPackage.GLOBAL)
    @JvmStatic
    external fun getB(): Double
  }
}

private fun testNativeStaticJsProperty() {
  val pi = NonNativeTypeWithNativeProperties.getB().toInt()
  assertTrue(pi == 3)
}

private fun testNativeInstanceJsProperty() {
  assertTrue(NonNativeTypeWithNativeProperties().getA() != null)
}

@JsMethod(namespace = "jspropertynative.helper")
private external fun createMyNativeJsType(): MyNativeJsType

@JsMethod(namespace = "jspropertynative.helper", name = "createJsObject")
private external fun createHasGetY(): HasGetY

@JsMethod(namespace = "jspropertynative.helper", name = "createJsObject")
private external fun createHasIsX(): HasIsX

@JsMethod(namespace = "jspropertynative.helper")
private external fun isUndefined(value: Int): Boolean
