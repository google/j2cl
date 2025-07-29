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
import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsConstructor
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsProperty
import jsinterop.annotations.JsType
import kotlin.js.definedExternally

/** Tests JsProperty functionality. */
object JsPropertyTest {
  fun testAll() {
    testConcreteJsType()
    testJavaClassImplementingMyJsTypeInterfaceWithProperty()
    testJsPropertyAccidentalOverrideSuperCall()
    testJsPropertyBridges()
    testJsPropertyBridgesSubclass()
    testJsPropertyGetX()
    testJsPropertyIsX()
    testJsPropertyRemovedAccidentalOverrideSuperCall()
    testNativeJsType()
    testNativeJsTypeSubclass()
    testNativeJsTypeSubclassNoOverride()
    testNativeJsTypeWithConstructor()
    testNativeJsTypeWithConstructorSubclass()
    testProtectedNames()
  }

  @JvmStatic val SET_PARENT_X: Int = 500

  @JvmStatic val GET_PARENT_X: Int = 1000

  @JvmStatic val GET_X: Int = 100

  @JvmStatic val SET_X: Int = 50

  @JsType
  internal interface MyJsTypeInterfaceWithProperty {
    @JsProperty fun getX(): Int

    @JsProperty fun setX(x: Int)
  }

  internal class MyJavaTypeImplementingMyJsTypeInterfaceWithProperty :
    MyJsTypeInterfaceWithProperty {
    @JvmField internal var x: Int = 0

    override fun getX(): Int = x + GET_X

    override fun setX(x: Int) {
      this.x = x + SET_X
    }
  }

  private fun testJavaClassImplementingMyJsTypeInterfaceWithProperty() {
    val obj = MyJavaTypeImplementingMyJsTypeInterfaceWithProperty()
    assertEquals(0 + GET_X, getProperty(obj, "x"))
    assertEquals(0 + GET_X, obj.getX())
    assertEquals(0, obj.x)

    setProperty(obj, "x", 10)
    assertEquals(10 + GET_X + SET_X, getProperty(obj, "x"))
    assertEquals(10 + GET_X + SET_X, obj.getX())
    assertEquals(10 + SET_X, obj.x)

    obj.setX(12)
    assertEquals(12 + GET_X + SET_X, getProperty(obj, "x"))
    assertEquals(12 + GET_X + SET_X, obj.getX())
    assertEquals(12 + SET_X, obj.x)

    val intf: MyJsTypeInterfaceWithProperty = MyJavaTypeImplementingMyJsTypeInterfaceWithProperty()
    assertEquals(0 + GET_X, getProperty(intf, "x"))
    assertEquals(0 + GET_X, intf.getX())
    assertEquals(0, (intf as MyJavaTypeImplementingMyJsTypeInterfaceWithProperty).x)

    setProperty(intf, "x", 10)
    assertEquals(10 + GET_X + SET_X, getProperty(intf, "x"))
    assertEquals(10 + GET_X + SET_X, intf.getX())
    assertEquals(10 + SET_X, (intf as MyJavaTypeImplementingMyJsTypeInterfaceWithProperty).x)

    intf.setX(12)
    assertEquals(12 + GET_X + SET_X, getProperty(intf, "x"))
    assertEquals(12 + GET_X + SET_X, intf.getX())
    assertEquals(12 + SET_X, (intf as MyJavaTypeImplementingMyJsTypeInterfaceWithProperty).x)
  }

  @JsType
  class MyConcreteJsType {
    internal var x: Int = 0

    @JsProperty fun getY(): Int = x + GET_X

    @JsProperty
    fun setY(x: Int) {
      this.x = x + SET_X
    }
  }

  private fun testConcreteJsType() {
    val obj = MyConcreteJsType()
    assertEquals(0 + GET_X, getProperty(obj, "y"))
    assertEquals(0 + GET_X, obj.getY())
    assertEquals(0, obj.x)

    setProperty(obj, "y", 10)
    assertEquals(10 + GET_X + SET_X, getProperty(obj, "y"))
    assertEquals(10 + GET_X + SET_X, obj.getY())
    assertEquals(10 + SET_X, obj.x)

    obj.setY(12)
    assertEquals(12 + GET_X + SET_X, getProperty(obj, "y"))
    assertEquals(12 + GET_X + SET_X, obj.getY())
    assertEquals(12 + SET_X, obj.x)
  }

  @JsType(isNative = true, name = "MyNativeJsType")
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

      @JvmStatic external fun answerToLife(): Int
    }
  }

  private fun testNativeJsType() {
    MyNativeJsType.staticX = 34
    assertEquals(34, MyNativeJsType.staticX)
    assertEquals(42, MyNativeJsType.answerToLife())

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

  internal class MyNativeJsTypeSubclass @JsConstructor internal constructor() : MyNativeJsType(42) {

    init {
      setY(52)
    }

    override fun sum(bias: Int): Int = super.sum(bias) + GET_X
  }

  private fun testNativeJsTypeSubclass() {
    val mc = MyNativeJsTypeSubclass()
    assertTrue(mc.ctorExecuted)
    assertEquals(143, mc.sum(1))

    mc.x = -mc.x
    assertEquals(58, mc.sum(0))

    assertEquals(52, mc.getY())
  }

  internal class MyNativeJsTypeSubclassNoOverride @JsConstructor constructor() : MyNativeJsType()

  private fun testNativeJsTypeSubclassNoOverride() {
    val myNativeJsType = MyNativeJsTypeSubclassNoOverride()
    myNativeJsType.x = 12
    assertEquals(42, myNativeJsType.sum(30))
  }

  @JsType(isNative = true, name = "MyNativeJsType")
  open internal class MyNativeJsTypeWithConstructor(x: Int) {
    @JvmField var ctorExecuted: Boolean = definedExternally
    @JvmField var x: Int = definedExternally
  }

  private fun testNativeJsTypeWithConstructor() {
    val obj = MyNativeJsTypeWithConstructor(12)
    assertTrue(obj.ctorExecuted)
    assertEquals(12, obj.x)
  }

  internal class MyNativeJsTypeWithConstructorSubclass @JsConstructor constructor(x: Int) :
    MyNativeJsTypeWithConstructor(x)

  private fun testNativeJsTypeWithConstructorSubclass() {
    val obj = MyNativeJsTypeWithConstructorSubclass(12)
    assertTrue(obj.ctorExecuted)
    assertEquals(12, obj.x)
  }

  @JsType(isNative = true)
  internal interface MyNativeJsTypeInterface {
    @JsProperty fun getX(): Int

    @JsProperty fun setX(x: Int)

    fun sum(bias: Int): Int
  }

  internal class MyNativeNativeJsTypeTypeInterfaceSubclassNeedingBridge :
    AccidentaImplementor(), MyNativeJsTypeInterface

  internal abstract class AccidentaImplementor() {
    private var x: Int = 0

    fun getX(): Int = x + GET_X

    fun setX(x: Int) {
      this.x = x + SET_X
    }

    fun getXProxy() = x

    fun sum(bias: Int): Int = bias + x
  }

  private fun testJsPropertyBridges() {
    val o: MyNativeJsTypeInterface = MyNativeNativeJsTypeTypeInterfaceSubclassNeedingBridge()

    o.setX(3)
    assertEquals(3 + 150, o.getX())
    assertEquals(3 + SET_X, (o as AccidentaImplementor).getXProxy())

    val accidentaImplementor = o as AccidentaImplementor

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

  internal open class MyNativeJsTypeInterfaceImplNeedingBridgeSubclassed :
    OtherAccidentalImplementer(), MyNativeJsTypeInterface

  internal abstract class OtherAccidentalImplementer() {
    private var x: Int = 0

    open fun getX(): Int = x + GET_PARENT_X

    open fun setX(x: Int) {
      this.x = x + SET_PARENT_X
    }

    fun getXProxy() = x

    open fun sum(bias: Int): Int = bias + x
  }

  internal class MyNativeJsTypeInterfaceImplNeedingBridgeSubclass :
    MyNativeJsTypeInterfaceImplNeedingBridgeSubclassed() {
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

  private fun testJsPropertyBridgesSubclass() {
    val o: MyNativeJsTypeInterface = MyNativeJsTypeInterfaceImplNeedingBridgeSubclass()

    o.setX(3)
    assertEquals(3 + 150, o.getX())

    val simple = o as OtherAccidentalImplementer

    simple.setX(3)
    assertEquals(3 + GET_X + SET_X, simple.getX())
    assertEquals(3 + GET_X + SET_X, getProperty(o, "x"))
    assertEquals(3 + SET_X, (o as MyNativeJsTypeInterfaceImplNeedingBridgeSubclass).y)
    assertEquals(0, (o as OtherAccidentalImplementer).getXProxy())

    setProperty(o, "x", 4)
    assertEquals(4 + GET_X + SET_X, simple.getX())
    assertEquals(4 + GET_X + SET_X, getProperty(o, "x"))
    assertEquals(4 + SET_X, (o as MyNativeJsTypeInterfaceImplNeedingBridgeSubclass).y)
    assertEquals(0, (o as OtherAccidentalImplementer).getXProxy())

    val subclass = o as MyNativeJsTypeInterfaceImplNeedingBridgeSubclass

    subclass.setParentX(5)
    assertEquals(8 + SET_PARENT_X, simple.sum(3))
    assertEquals(9 + SET_PARENT_X + GET_PARENT_X + SET_X, subclass.getXPlusY())
    assertEquals(4 + SET_X, (o as MyNativeJsTypeInterfaceImplNeedingBridgeSubclass).y)
    assertEquals(5 + SET_PARENT_X, (o as OtherAccidentalImplementer).getXProxy())
  }

  @JsType(isNative = true)
  internal interface MyJsTypeInterfaceWithProtectedNames {
    fun `var`(): String

    @JsProperty fun getNullField(): String // Defined in object scope but shouldn't obfuscate

    @JsProperty fun getImport(): String

    @JsProperty fun setImport(str: String)
  }

  private fun testProtectedNames() {
    val obj: MyJsTypeInterfaceWithProtectedNames = createMyJsInterfaceWithProtectedNames()
    assertEquals("var", obj.`var`())
    assertEquals("nullField", obj.getNullField())
    assertEquals("import", obj.getImport())
    obj.setImport("import2")
    assertEquals("import2", obj.getImport())
  }

  @JsType(isNative = true)
  internal interface JsTypeIsProperty {

    @JsProperty fun isX(): Boolean

    @JsProperty fun setX(x: Boolean)
  }

  private fun testJsPropertyIsX() {
    val o: JsTypeIsProperty = createJsTypeIsProperty()

    assertFalse(o.isX())
    o.setX(true)
    assertTrue(o.isX())
    o.setX(false)
    assertFalse(o.isX())
  }

  @JsType(isNative = true)
  internal interface AccidentalOverridePropertyJsTypeInterface {
    @JsProperty fun getX(): Int
  }

  open internal class AccidentalOverridePropertyBase {
    fun getX(): Int = 50
  }

  internal class AccidentalOverrideProperty :
    AccidentalOverridePropertyBase(), AccidentalOverridePropertyJsTypeInterface

  private fun testJsPropertyAccidentalOverrideSuperCall() {
    val o = AccidentalOverrideProperty()
    assertEquals(50, o.getX())
    assertEquals(50, getProperty(o, "x"))
  }

  @JsType
  internal open class RemovedAccidentalOverridePropertyBase internal constructor() {
    @JsProperty fun getX(): Int = 55
  }

  internal class RemovedAccidentalOverrideProperty :
    RemovedAccidentalOverridePropertyBase(), AccidentalOverridePropertyJsTypeInterface

  private fun testJsPropertyRemovedAccidentalOverrideSuperCall() {
    val o = RemovedAccidentalOverrideProperty()
    // If the accidental override here were not removed the access to property x would result in
    // an infinite loop
    assertEquals(55, o.getX())
    assertEquals(55, getProperty(o, "x"))
  }

  @JsType(isNative = true)
  internal interface JsTypeGetProperty {

    @JsProperty fun getX(): Int

    @JsProperty fun setX(x: Int)
  }

  private fun testJsPropertyGetX() {
    val o: JsTypeGetProperty = createJsTypeGetProperty()

    assertTrue(isUndefined(o.getX()))
    o.setX(10)
    assertEquals(10, o.getX())
    o.setX(0)
    assertEquals(0, o.getX())
  }

  @JsMethod @JvmStatic private external fun createMyNativeJsType(): MyNativeJsType

  @JsMethod @JvmStatic private external fun createJsTypeGetProperty(): JsTypeGetProperty

  @JsMethod @JvmStatic private external fun createJsTypeIsProperty(): JsTypeIsProperty

  @JsMethod
  @JvmStatic
  private external fun createMyJsInterfaceWithProtectedNames(): MyJsTypeInterfaceWithProtectedNames

  @JsMethod @JvmStatic private external fun isUndefined(value: Int): Boolean

  @JsMethod @JvmStatic private external fun hasField(o: Any?, fieldName: String): Boolean

  @JsMethod @JvmStatic private external fun getProperty(o: Any?, name: String): Int

  @JsMethod @JvmStatic private external fun setProperty(o: Any?, name: String, value: Int)

  fun assertJsTypeHasFields(obj: Any, vararg fields: String) {
    for (field in fields) {
      assertTrue("Field '" + field + "' should be exported", hasField(obj, field))
    }
  }

  fun assertJsTypeDoesntHaveFields(obj: Any, vararg fields: String) {
    for (field in fields) {
      assertFalse("Field '" + field + "' should not be exported", hasField(obj, field))
    }
  }
}
