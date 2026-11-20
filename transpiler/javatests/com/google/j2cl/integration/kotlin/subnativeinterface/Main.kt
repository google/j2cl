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
package subnativeinterface

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertSame
import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsOverlay
import jsinterop.annotations.JsProperty
import jsinterop.annotations.JsType

/** Tests Kotlin types implementing native JsType interfaces. */
fun main(vararg args: String) {
  testImplementation()
  testAccidentalImplementation()
  testGenericImplementation()
  testSubInterfaceImplementation()
  testJsTypeSingleJavaConcreteInterface()
  testJsPropertyBridges()
  testJsPropertyBridgesSubclass()
  testJsPropertyAccidentalOverrideSuperCall()
  testJsPropertyRemovedAccidentalOverrideSuperCall()
  testJsPropertyRemovedAccidentalOverrideReadWrite()
}

@JsType(isNative = true, namespace = "subnativeinterface")
internal interface MyNativeInterface {
  fun getName(): String

  @JsOverlay fun describe(): String = "MyNativeInterface(" + getName() + ")"
}

@JsType(isNative = true, namespace = "subnativeinterface")
internal interface MyNativeSubInterface : MyNativeInterface

@JsType(isNative = true, namespace = "subnativeinterface")
internal interface MyNativeInterfaceWithJsMembers {
  @JsMethod(name = "renamed") fun mightBeRenamed(): String

  @JsProperty fun getProperty(): String?

  @JsProperty fun setProperty(value: String?)
}

@JsType(isNative = true, namespace = "subnativeinterface")
internal fun interface MyNativeGenericInterface<T> {
  fun echo(t: T): T
}

internal open class MyNativeInterfaceImpl : MyNativeInterface, MyNativeInterfaceWithJsMembers {
  private var property: String? = null

  override fun getName(): String = "MyNativeInterfaceImpl"

  override fun mightBeRenamed(): String = "MyNativeInterfaceImpl.mightBeRenamed"

  override fun getProperty(): String? = property

  override fun setProperty(value: String?) {
    property = value
  }
}

internal class MyNativeInterfaceImplSubclass : MyNativeInterfaceImpl() {
  override fun getName(): String = "MyNativeInterfaceImplSubclass"
}

@JsType
internal class MyNativeSubInterfaceJsTypeImpl : MyNativeSubInterface {
  override fun getName(): String = "MyNativeSubInterfaceJsTypeImpl"
}

internal enum class MyNativeInterfaceEnum : MyNativeInterface {
  INSTANCE;

  override fun getName(): String = "MyNativeInterfaceEnum"
}

internal interface MyKotlinInterface {
  fun getName(): String
}

internal class MyKotlinAndNativeInterfaceImpl : MyKotlinInterface, MyNativeInterface {
  override fun getName(): String = "MyKotlinAndNativeInterfaceImpl"
}

internal abstract class MyNativeInterfaceAbstractImpl :
  MyNativeInterface, MyNativeInterfaceWithJsMembers {
  override fun mightBeRenamed(): String = "MyNativeInterfaceAbstractImpl.mightBeRenamed"
}

internal open class MyNativeInterfaceConcreteImpl : MyNativeInterfaceAbstractImpl() {
  private var property: String? = null

  override fun getName(): String = "MyNativeInterfaceConcreteImpl"

  override fun getProperty(): String? = property

  override fun setProperty(value: String?) {
    property = value
  }
}

internal class MyNativeInterfaceFinalConcreteImpl : MyNativeInterfaceAbstractImpl() {
  private var property: String? = null

  override fun getName(): String = "MyNativeInterfaceFinalConcreteImpl"

  override fun getProperty(): String? = property

  override fun setProperty(value: String?) {
    property = value
  }
}

private fun testImplementation() {
  val jsImpl = createJsImpl("JsImpl")
  assertEquals("JsImpl", callGetName(jsImpl))
  assertEquals("JsImpl", jsImpl.getName())
  assertEquals("MyNativeInterface(JsImpl)", jsImpl.describe())

  val impl = MyNativeInterfaceImpl()
  assertMyNativeInterface(impl, "MyNativeInterfaceImpl")
  assertMyNativeInterfaceWithJsMembers(impl, "MyNativeInterfaceImpl.mightBeRenamed")
  assertMyNativeInterface(MyNativeInterfaceImplSubclass(), "MyNativeInterfaceImplSubclass")
  assertMyNativeInterface(MyNativeSubInterfaceJsTypeImpl(), "MyNativeSubInterfaceJsTypeImpl")
  assertMyNativeInterface(MyNativeInterfaceEnum.INSTANCE, "MyNativeInterfaceEnum")

  val kotlinAndNativeImpl = MyKotlinAndNativeInterfaceImpl()
  assertMyNativeInterface(kotlinAndNativeImpl, "MyKotlinAndNativeInterfaceImpl")
  val kotlinInterface: MyKotlinInterface = kotlinAndNativeImpl
  assertEquals("MyKotlinAndNativeInterfaceImpl", kotlinInterface.getName())

  val concreteImpl: MyNativeInterfaceAbstractImpl = MyNativeInterfaceConcreteImpl()
  assertMyNativeInterface(concreteImpl, "MyNativeInterfaceConcreteImpl")
  assertMyNativeInterfaceWithJsMembers(concreteImpl, "MyNativeInterfaceAbstractImpl.mightBeRenamed")
  // Calls through the abstract class resolve to the methods of the native interface.
  assertEquals("MyNativeInterfaceConcreteImpl", concreteImpl.getName())
  assertEquals("MyNativeInterface(MyNativeInterfaceConcreteImpl)", concreteImpl.describe())

  val finalConcreteImpl = MyNativeInterfaceFinalConcreteImpl()
  assertMyNativeInterface(finalConcreteImpl, "MyNativeInterfaceFinalConcreteImpl")
  assertMyNativeInterfaceWithJsMembers(
    finalConcreteImpl,
    "MyNativeInterfaceAbstractImpl.mightBeRenamed",
  )

  assertMyNativeInterface(
    object : MyNativeInterface {
      override fun getName(): String = "AnonymousImpl"
    },
    "AnonymousImpl",
  )
}

internal open class NonJsBase {
  private var property: String? = null

  open fun getName(): String = "NonJsBase"

  fun mightBeRenamed(): String = "NonJsBase.mightBeRenamed"

  fun getProperty(): String? = property

  fun setProperty(value: String?) {
    property = value
  }
}

internal open class MyNativeInterfaceAccidentalImpl :
  NonJsBase(), MyNativeInterface, MyNativeInterfaceWithJsMembers

internal class MyNativeInterfaceAccidentalImplSubclass : MyNativeInterfaceAccidentalImpl() {
  override fun getName(): String = "MyNativeInterfaceAccidentalImplSubclass"
}

internal class MyNativeSubInterfaceAccidentalImpl : NonJsBase(), MyNativeSubInterface

@JsType
internal open class JsTypeBase internal constructor() {
  fun getName(): String = "JsTypeBase"
}

internal class MyNativeInterfaceAccidentalJsTypeImpl : JsTypeBase(), MyNativeInterface

private fun testAccidentalImplementation() {
  val accidentalImpl = MyNativeInterfaceAccidentalImpl()
  assertMyNativeInterface(accidentalImpl, "NonJsBase")
  assertMyNativeInterfaceWithJsMembers(accidentalImpl, "NonJsBase.mightBeRenamed")
  assertMyNativeInterface(
    MyNativeInterfaceAccidentalImplSubclass(),
    "MyNativeInterfaceAccidentalImplSubclass",
  )
  assertMyNativeInterface(MyNativeInterfaceAccidentalJsTypeImpl(), "JsTypeBase")
  assertMyNativeInterface(MyNativeSubInterfaceAccidentalImpl(), "NonJsBase")
}

internal class StringEcho : MyNativeGenericInterface<String> {
  override fun echo(t: String): String = "StringEcho: $t"
}

internal abstract class AbstractStringEcho : MyNativeGenericInterface<String>

internal class ConcreteStringEcho : AbstractStringEcho() {
  override fun echo(t: String): String = "ConcreteStringEcho: $t"
}

internal class GenericEcho<T> : MyNativeGenericInterface<T> {
  override fun echo(t: T): T = t
}

internal class NumberEcho<T : Number> : MyNativeGenericInterface<T> {
  override fun echo(t: T): T = t
}

internal open class NonJsStringEcho {
  fun echo(t: String): String = "NonJsStringEcho: $t"
}

internal class AccidentalStringEcho : NonJsStringEcho(), MyNativeGenericInterface<String>

internal open class NonJsGenericEcho<T> {
  fun echo(t: T): T = t
}

internal class AccidentalGenericEcho : NonJsGenericEcho<String>(), MyNativeGenericInterface<String>

internal class NativeEcho : MyNativeGenericInterface<MyNativeInterface> {
  override fun echo(t: MyNativeInterface): MyNativeInterface =
    createJsImpl("NativeEcho: " + t.getName())
}

private fun testGenericImplementation() {
  assertEcho(StringEcho(), "hello", "StringEcho: hello")
  assertEcho(ConcreteStringEcho(), "hello", "ConcreteStringEcho: hello")
  val o = Any()
  assertEcho(GenericEcho<Any>(), o, o)
  assertEcho(NumberEcho<Int>(), 42, 42)
  assertEcho(AccidentalStringEcho(), "hello", "NonJsStringEcho: hello")
  assertEcho(AccidentalGenericEcho(), "hello", "hello")
  assertEcho(MyNativeGenericInterface<String> { "lambda: $it" }, "hello", "lambda: hello")
  assertEcho(MyNativeGenericInterface(String::uppercase), "hello", "HELLO")
  assertEquals(
    "NativeEcho: hello",
    (callEcho(NativeEcho(), createJsImpl("hello")) as MyNativeInterface).getName(),
  )
}

internal fun interface MyKotlinSubInterface : MyNativeInterface

@JsType internal interface MyJsTypeSubInterface : MyNativeInterface

internal interface MyKotlinSubInterfaceWithDefault : MyNativeInterface {
  override fun getName(): String = "MyKotlinSubInterfaceWithDefault"
}

internal class MyKotlinSubInterfaceImpl : MyKotlinSubInterface {
  override fun getName(): String = "MyKotlinSubInterfaceImpl"
}

internal class MyJsTypeSubInterfaceImpl : MyJsTypeSubInterface {
  override fun getName(): String = "MyJsTypeSubInterfaceImpl"
}

internal class MyKotlinSubInterfaceWithDefaultImpl : MyKotlinSubInterfaceWithDefault

internal class MyKotlinSubInterfaceAccidentalImpl : NonJsBase(), MyKotlinSubInterface

private fun testSubInterfaceImplementation() {
  val impl: MyKotlinSubInterface = MyKotlinSubInterfaceImpl()
  assertMyNativeInterface(impl, "MyKotlinSubInterfaceImpl")
  // Calls through the Kotlin subinterface resolve to the methods of the native interface.
  assertEquals("MyKotlinSubInterfaceImpl", impl.getName())
  assertMyNativeInterface(MyJsTypeSubInterfaceImpl(), "MyJsTypeSubInterfaceImpl")
  assertMyNativeInterface(MyKotlinSubInterfaceWithDefaultImpl(), "MyKotlinSubInterfaceWithDefault")
  assertMyNativeInterface(MyKotlinSubInterfaceAccidentalImpl(), "NonJsBase")
  val lambda = MyKotlinSubInterface { "MyKotlinSubInterface lambda" }
  assertMyNativeInterface(lambda, "MyKotlinSubInterface lambda")
}

private fun testJsTypeSingleJavaConcreteInterface() {
  // Create a couple of instances and use the objects in some way to avoid complete pruning
  // of JavaConcrete
  assertTrue(JsTypeJavaConcrete() != JsTypeJavaConcrete())
  assertSame(5, JsTypeJavaConcrete().m())
  assertSame(3, (nativeObjectImplementingM() as JsTypeInterfaceWithSingleJavaConcrete).m())
}

@JsType(
  isNative = true,
  namespace = "subnativeinterface.JsTypeTest",
  name = "InterfaceWithSingleJavaConcrete",
)
internal interface JsTypeInterfaceWithSingleJavaConcrete {
  fun m(): Int
}

internal class JsTypeJavaConcrete : JsTypeInterfaceWithSingleJavaConcrete {
  override fun m(): Int = 5
}

@JsMethod(namespace = "subnativeinterface.helper")
private external fun nativeObjectImplementingM(): Any?

val SET_PARENT_X: Int = 500
val GET_PARENT_X: Int = 1000
val GET_X: Int = 100
val SET_X: Int = 50

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
  namespace = "subnativeinterface.JsPropertyTest",
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
  namespace = "subnativeinterface.JsPropertyTest",
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

private fun testJsPropertyRemovedAccidentalOverrideReadWrite() {
  val o: JsPropertyMyNativeJsTypeInterface = JsPropertyRemovedAccidentalOverrideReadWriteProperty()
  // If the accidental overrides here were not removed the accesses to property x would result in
  // an infinite loop
  o.setX(3)
  assertEquals(3 + 150, o.getX())
  assertEquals(3 + 150, getProperty(o, "x"))

  setProperty(o, "x", 4)
  assertEquals(4 + 150, o.getX())
  assertEquals(4 + 150, getProperty(o, "x"))

  assertEquals(3 + 4 + SET_X, o.sum(3))
}

@JsType
internal open class JsPropertyRemovedAccidentalOverrideReadWritePropertyBase
internal constructor() {
  private var x: Int = 0

  @JsProperty fun getX(): Int = x + GET_X

  @JsProperty
  fun setX(x: Int) {
    this.x = x + SET_X
  }

  fun sum(bias: Int): Int = bias + x
}

internal class JsPropertyRemovedAccidentalOverrideReadWriteProperty :
  JsPropertyRemovedAccidentalOverrideReadWritePropertyBase(), JsPropertyMyNativeJsTypeInterface

@JsMethod(namespace = "subnativeinterface.helper")
private external fun getProperty(o: Any?, name: String): Int

@JsMethod(namespace = "subnativeinterface.helper")
private external fun setProperty(o: Any?, name: String, value: Int)

/** Checks calls on a Kotlin object from JavaScript and from Kotlin through the native interface. */
private fun assertMyNativeInterface(o: MyNativeInterface, name: String) {
  assertEquals(name, callGetName(o))
  // Calls through native interfaces in Kotlin go through JavaScript.
  assertEquals(name, o.getName())
  assertEquals("MyNativeInterface($name)", o.describe())
  assertSame(o, passThrough(o))
}

private fun assertMyNativeInterfaceWithJsMembers(
  o: MyNativeInterfaceWithJsMembers,
  renamedResult: String,
) {
  assertEquals(renamedResult, callRenamed(o))
  assertEquals(renamedResult, o.mightBeRenamed())
  writeProperty(o, "set from JS")
  assertEquals("set from JS", o.getProperty())
  o.setProperty("set from Kotlin")
  assertEquals("set from Kotlin", readProperty(o))
}

private fun <T> assertEcho(o: MyNativeGenericInterface<T>, value: T, expected: T) {
  assertEquals(expected, callEcho(o, value))
  assertEquals(expected, o.echo(value))
}

@JsMethod(namespace = "subnativeinterface.helper")
private external fun createJsImpl(name: String): MyNativeInterface

@JsMethod(namespace = "subnativeinterface.helper")
private external fun callGetName(o: MyNativeInterface): String

@JsMethod(namespace = "subnativeinterface.helper")
private external fun callRenamed(o: MyNativeInterfaceWithJsMembers): String

@JsMethod(namespace = "subnativeinterface.helper")
private external fun readProperty(o: MyNativeInterfaceWithJsMembers): String?

@JsMethod(namespace = "subnativeinterface.helper")
private external fun writeProperty(o: MyNativeInterfaceWithJsMembers, value: String?)

@JsMethod(namespace = "subnativeinterface.helper")
private external fun callEcho(o: MyNativeGenericInterface<*>, value: Any?): Any?

@JsMethod(namespace = "subnativeinterface.helper") private external fun passThrough(o: Any?): Any?
