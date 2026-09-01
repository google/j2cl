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
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsProperty
import jsinterop.annotations.JsType
import kotlin.js.definedExternally

fun main(vararg args: String) {
  testJsTypeSubclassConstructor()
  testJsPropertyNativeJsTypeSubclass()
  testJsPropertyNativeJsTypeSubclassNoOverride()
  testJsPropertyBridges()
  testJsPropertyBridgesSubclass()
  testJsPropertyAccidentalOverrideSuperCall()
  testJsPropertyRemovedAccidentalOverrideSuperCall()
}

private fun testJsTypeSubclassConstructor() {
  val mc = JsPropertyMyNativeJsTypeSubclass(0, 0)
  assertTrue(mc.ctorExecuted)
}

val SET_PARENT_X: Int = 500
val GET_PARENT_X: Int = 1000
val GET_X: Int = 100
val SET_X: Int = 50

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

private fun testJsPropertyBridges() {
  val o: JsPropertyMyNativeJsTypeInterface =
    JsPropertyMyNativeNativeJsTypeTypeInterfaceSubclassNeedingBridge()

  o.setX(3)
  assertEquals(3 + 150, o.getX())
  assertEquals(3 + SET_X, (o as JsPropertyAccidentaImplementor).getXProxy())

  val accidentaImplementor = o as JsPropertyAccidentaImplementor

  accidentaImplementor.setX(3)
  assertEquals(3 + 150, accidentaImplementor.getX())
  assertEquals(3 + 150, getProperty(o, "x"))
  assertEquals(3 + SET_X, accidentaImplementor.getXProxy())

  setProperty(o, "x", 4)
  assertEquals(4 + 150, accidentaImplementor.getX())
  assertEquals(4 + 150, getProperty(o, "x"))
  assertEquals(4 + SET_X, accidentaImplementor.getXProxy())

  assertEquals(3 + 4 + SET_X, accidentaImplementor.sum(3))
}

@JsType(
  isNative = true,
  namespace = "subnativejstype.JsPropertyTest",
  name = "MyNativeJsTypeInterface",
)
internal interface JsPropertyMyNativeJsTypeInterface {
  @JsProperty fun getX(): Int

  @JsProperty fun setX(x: Int)

  fun sum(bias: Int): Int
}

internal class JsPropertyMyNativeNativeJsTypeTypeInterfaceSubclassNeedingBridge :
  JsPropertyAccidentaImplementor(), JsPropertyMyNativeJsTypeInterface

internal abstract class JsPropertyAccidentaImplementor() {
  private var x: Int = 0

  fun getX(): Int = x + GET_X

  fun setX(x: Int) {
    this.x = x + SET_X
  }

  fun getXProxy() = x

  fun sum(bias: Int): Int = bias + x
}

private fun testJsPropertyBridgesSubclass() {
  val o: JsPropertyMyNativeJsTypeInterface =
    JsPropertyMyNativeJsTypeInterfaceImplNeedingBridgeSubclass()

  o.setX(3)
  assertEquals(3 + 150, o.getX())

  val simple = o as JsPropertyOtherAccidentalImplementer

  simple.setX(3)
  assertEquals(3 + GET_X + SET_X, simple.getX())
  assertEquals(3 + GET_X + SET_X, getProperty(o, "x"))
  assertEquals(3 + SET_X, (o as JsPropertyMyNativeJsTypeInterfaceImplNeedingBridgeSubclass).y)
  assertEquals(0, (o as JsPropertyOtherAccidentalImplementer).getXProxy())

  setProperty(o, "x", 4)
  assertEquals(4 + GET_X + SET_X, simple.getX())
  assertEquals(4 + GET_X + SET_X, getProperty(o, "x"))
  assertEquals(4 + SET_X, (o as JsPropertyMyNativeJsTypeInterfaceImplNeedingBridgeSubclass).y)
  assertEquals(0, (o as JsPropertyOtherAccidentalImplementer).getXProxy())

  val subclass = o as JsPropertyMyNativeJsTypeInterfaceImplNeedingBridgeSubclass

  subclass.setParentX(5)
  assertEquals(8 + SET_PARENT_X, simple.sum(3))
  assertEquals(9 + SET_PARENT_X + GET_PARENT_X + SET_X, subclass.getXPlusY())
  assertEquals(4 + SET_X, (o as JsPropertyMyNativeJsTypeInterfaceImplNeedingBridgeSubclass).y)
  assertEquals(5 + SET_PARENT_X, (o as JsPropertyOtherAccidentalImplementer).getXProxy())
}

internal open class JsPropertyMyNativeJsTypeInterfaceImplNeedingBridgeSubclassed :
  JsPropertyOtherAccidentalImplementer(), JsPropertyMyNativeJsTypeInterface

internal abstract class JsPropertyOtherAccidentalImplementer() {
  private var x: Int = 0

  open fun getX(): Int = x + GET_PARENT_X

  open fun setX(x: Int) {
    this.x = x + SET_PARENT_X
  }

  fun getXProxy() = x

  open fun sum(bias: Int): Int = bias + x
}

internal class JsPropertyMyNativeJsTypeInterfaceImplNeedingBridgeSubclass :
  JsPropertyMyNativeJsTypeInterfaceImplNeedingBridgeSubclassed() {
  internal var y: Int = 0

  override fun getX(): Int = y + GET_X

  override fun setX(y: Int) {
    this.y = y + SET_X
  }

  fun setParentX(value: Int) {
    super.setX(value)
  }

  fun getXPlusY(): Int = super.getX() + y
}

private fun testJsPropertyAccidentalOverrideSuperCall() {
  val o = JsPropertyAccidentalOverrideProperty()
  assertEquals(50, o.getX())
  assertEquals(50, getProperty(o, "x"))
}

@JsType(
  isNative = true,
  namespace = "subnativejstype.JsPropertyTest",
  name = "AccidentalOverridePropertyJsTypeInterface",
)
internal interface JsPropertyAccidentalOverridePropertyJsTypeInterface {
  @JsProperty fun getX(): Int
}

open internal class JsPropertyAccidentalOverridePropertyBase {
  fun getX(): Int = 50
}

internal class JsPropertyAccidentalOverrideProperty :
  JsPropertyAccidentalOverridePropertyBase(), JsPropertyAccidentalOverridePropertyJsTypeInterface

private fun testJsPropertyRemovedAccidentalOverrideSuperCall() {
  val o = JsPropertyRemovedAccidentalOverrideProperty()
  // If the accidental override here were not removed the access to property x would result in
  // an infinite loop
  assertEquals(55, o.getX())
  assertEquals(55, getProperty(o, "x"))
}

@JsType
internal open class JsPropertyRemovedAccidentalOverridePropertyBase internal constructor() {
  @JsProperty fun getX(): Int = 55
}

internal class JsPropertyRemovedAccidentalOverrideProperty :
  JsPropertyRemovedAccidentalOverridePropertyBase(),
  JsPropertyAccidentalOverridePropertyJsTypeInterface

@JsMethod(namespace = "subnativejstype.JsPropertyTestHelper")
private external fun getProperty(o: Any?, name: String): Int

@JsMethod(namespace = "subnativejstype.JsPropertyTestHelper")
private external fun setProperty(o: Any?, name: String, value: Int)
