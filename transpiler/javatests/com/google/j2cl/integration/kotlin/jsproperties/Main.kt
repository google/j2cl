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
package jsproperties

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsProperty
import jsinterop.annotations.JsType
import kotlin.js.definedExternally

fun main(vararg args: String) {
  testConcreteJsType()
  testConcreteJsType_static()
  testJavaClassImplementingMyJsTypeInterfaceWithProperty()
  testJsPropertyGetX()
  testJsPropertyGetX_undefined()
  testJsPropertyIsX()
  testNativeJsType()
  testNativeJsTypeWithConstructor()
  testNonJsType()
  testNonJsType_static()
  testNativeStaticJsProperty()
  testNativeInstanceJsProperty()
  testDefaultMethodJsProperty()
  KotlinProperties().testPropertyAccessors()
}

val GET_X: Int = 100
val SET_X: Int = 50

@JsType
internal interface MyJsTypeInterfaceWithProperty {
  @JsProperty fun getX(): Int

  @JsProperty fun setX(x: Int)
}

internal class MyJavaTypeImplementingMyJsTypeInterfaceWithProperty : MyJsTypeInterfaceWithProperty {
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

@JsType(namespace = "jsproperties", name = "MyConcreteJsType")
class MyConcreteJsType {
  @JvmField var x: Int = 0

  @JsProperty fun getY(): Int = x + GET_X

  @JsProperty
  fun setY(x: Int) {
    this.x = x + SET_X
  }

  @JsProperty(name = "abc") fun getC(): Int = x + GET_X

  @JsProperty(name = "abc")
  fun setC(x: Int) {
    this.x = x + SET_X
  }

  companion object {
    @JvmField var staticX: Int = 0

    @JsProperty @JvmStatic fun getStaticY(): Int = staticX + GET_X

    @JsProperty
    @JvmStatic
    fun setStaticY(x: Int) {
      staticX = x + SET_X
    }

    @JsProperty(name = "abc") @JvmStatic fun getStaticC(): Int = staticX + GET_X

    @JsProperty(name = "abc")
    @JvmStatic
    fun setStaticC(x: Int) {
      staticX = x + SET_X
    }
  }
}

private fun testConcreteJsType() {
  val obj = MyConcreteJsType()
  assertEquals(0 + GET_X, getProperty(obj, "y"))
  assertEquals(0 + GET_X, obj.getY())
  assertEquals(0, getProperty(obj, "x"))
  assertEquals(0, obj.x)

  setProperty(obj, "x", 8)
  assertEquals(8 + GET_X, getProperty(obj, "y"))
  assertEquals(8 + GET_X, obj.getY())
  assertEquals(8, getProperty(obj, "x"))
  assertEquals(8, obj.x)

  obj.x = 9
  assertEquals(9 + GET_X, getProperty(obj, "y"))
  assertEquals(9 + GET_X, obj.getY())
  assertEquals(9, getProperty(obj, "x"))
  assertEquals(9, obj.x)

  setProperty(obj, "y", 10)
  assertEquals(10 + GET_X + SET_X, getProperty(obj, "y"))
  assertEquals(10 + GET_X + SET_X, obj.getY())
  assertEquals(10 + SET_X, getProperty(obj, "x"))
  assertEquals(10 + SET_X, obj.x)

  obj.setY(12)
  assertEquals(12 + GET_X + SET_X, getProperty(obj, "y"))
  assertEquals(12 + GET_X + SET_X, obj.getY())
  assertEquals(12 + SET_X, getProperty(obj, "x"))
  assertEquals(12 + SET_X, obj.x)

  setMyConcreteJsTypeAbc(obj, 20)
  assertEquals(20 + GET_X + SET_X, getMyConcreteJsTypeAbc(obj))
  assertEquals(20 + GET_X + SET_X, obj.getC())
  assertEquals(20 + SET_X, getProperty(obj, "x"))
  assertEquals(20 + SET_X, obj.x)

  obj.setC(22)
  assertEquals(22 + GET_X + SET_X, getMyConcreteJsTypeAbc(obj))
  assertEquals(22 + GET_X + SET_X, obj.getC())
  assertEquals(22 + SET_X, getProperty(obj, "x"))
  assertEquals(22 + SET_X, obj.x)
}

private fun testConcreteJsType_static() {
  assertEquals(0 + GET_X, getMyConcreteJsTypeStaticY())
  assertEquals(0 + GET_X, MyConcreteJsType.getStaticY())
  assertEquals(0, getMyConcreteJsTypeStaticX())
  assertEquals(0, MyConcreteJsType.staticX)

  setMyConcreteJsTypeStaticX(8)
  assertEquals(8 + GET_X, getMyConcreteJsTypeStaticY())
  assertEquals(8 + GET_X, MyConcreteJsType.getStaticY())
  assertEquals(8, getMyConcreteJsTypeStaticX())
  assertEquals(8, MyConcreteJsType.staticX)

  MyConcreteJsType.staticX = 9
  assertEquals(9 + GET_X, getMyConcreteJsTypeStaticY())
  assertEquals(9 + GET_X, MyConcreteJsType.getStaticY())
  assertEquals(9, getMyConcreteJsTypeStaticX())
  assertEquals(9, MyConcreteJsType.staticX)

  setMyConcreteJsTypeStaticY(11)
  assertEquals(11 + GET_X + SET_X, getMyConcreteJsTypeStaticY())
  assertEquals(11 + GET_X + SET_X, MyConcreteJsType.getStaticY())
  assertEquals(11 + SET_X, getMyConcreteJsTypeStaticX())
  assertEquals(11 + SET_X, MyConcreteJsType.staticX)

  MyConcreteJsType.setStaticY(13)
  assertEquals(13 + GET_X + SET_X, getMyConcreteJsTypeStaticY())
  assertEquals(13 + GET_X + SET_X, MyConcreteJsType.getStaticY())
  assertEquals(13 + SET_X, getMyConcreteJsTypeStaticX())
  assertEquals(13 + SET_X, MyConcreteJsType.staticX)

  setMyConcreteJsTypeStaticAbc(20)
  assertEquals(20 + GET_X + SET_X, getMyConcreteJsTypeStaticAbc())
  assertEquals(20 + GET_X + SET_X, MyConcreteJsType.getStaticC())
  assertEquals(20 + SET_X, getMyConcreteJsTypeStaticX())
  assertEquals(20 + SET_X, MyConcreteJsType.staticX)

  MyConcreteJsType.setStaticC(21)
  assertEquals(21 + GET_X + SET_X, getMyConcreteJsTypeStaticAbc())
  assertEquals(21 + GET_X + SET_X, MyConcreteJsType.getStaticC())
  assertEquals(21 + SET_X, getMyConcreteJsTypeStaticX())
  assertEquals(21 + SET_X, MyConcreteJsType.staticX)
}

@JsType(isNative = true, namespace = "jsproperties", name = "MyNativeJsType")
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

@JsType(isNative = true, namespace = "jsproperties", name = "MyNativeJsType")
open internal class MyNativeJsTypeWithConstructor(x: Int) {
  @JvmField var ctorExecuted: Boolean = definedExternally
  @JvmField var x: Int = definedExternally
}

private fun testNativeJsTypeWithConstructor() {
  val obj = MyNativeJsTypeWithConstructor(12)
  assertTrue(obj.ctorExecuted)
  assertEquals(12, obj.x)
}

@JsType(isNative = true, namespace = "jsproperties")
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

@JsType(isNative = true, namespace = "jsproperties")
internal interface JsTypeGetProperty {

  @JsProperty fun getX(): Int

  @JsProperty fun setX(x: Int)
}

private fun testJsPropertyGetX_undefined() {
  val o: JsTypeGetProperty = createJsTypeGetProperty()
  assertTrue(isUndefined(o.getX()))
}

private fun testJsPropertyGetX() {
  val o: JsTypeGetProperty = createJsTypeGetProperty()
  o.setX(10)
  assertEquals(10, o.getX())
  o.setX(0)
  assertEquals(0, o.getX())
}

class NonJsType {
  private var x: Int = 0

  @JsProperty fun getX(): Int = x

  @JsProperty
  fun setX(x: Int) {
    this.x = x
  }

  @JsProperty(name = "abc") fun getC(): Int = x

  @JsProperty(name = "abc")
  fun setC(x: Int) {
    this.x = x
  }

  @JsProperty var y: Int = 0

  @JsProperty(name = "hasOwnProperty") external fun getA(): Any?

  companion object {
    @JvmField internal var staticX: Int = 0

    @JsProperty @JvmStatic fun getStaticX(): Int = staticX

    @JsProperty
    @JvmStatic
    fun setStaticX(x: Int) {
      staticX = x
    }

    @JsProperty(name = "abc") @JvmStatic fun getStaticC(): Int = staticX

    @JsProperty(name = "abc")
    @JvmStatic
    fun setStaticC(x: Int) {
      staticX = x
    }

    @JsProperty(name = "Math.PI", namespace = JsPackage.GLOBAL)
    @JvmStatic
    external fun getB(): Double
  }
}

private fun testNonJsType() {
  val obj = NonJsType()

  obj.setX(10)
  assertEquals(10, obj.getX())
  assertEquals(10, getProperty(obj, "x"))

  setProperty(obj, "x", 4)
  assertEquals(4, obj.getX())
  assertEquals(4, getProperty(obj, "x"))

  obj.y = 20
  assertEquals(20, obj.y)
  assertEquals(20, getProperty(obj, "y"))

  setProperty(obj, "y", 24)
  assertEquals(24, obj.y)
  assertEquals(24, getProperty(obj, "y"))

  obj.setC(30)
  assertEquals(30, obj.getC())
  assertEquals(30, getNonJsTypeAbc(obj))

  setNonJsTypeAbc(obj, 34)
  assertEquals(34, obj.getC())
  assertEquals(34, getNonJsTypeAbc(obj))
}

private fun testNonJsType_static() {
  NonJsType.setStaticX(10)
  assertEquals(10, NonJsType.getStaticX())
  assertEquals(10, getNonJsTypeStaticX())
  assertEquals(10, NonJsType.staticX)

  setNonJsTypeStaticX(4)
  assertEquals(4, NonJsType.getStaticX())
  assertEquals(4, getNonJsTypeStaticX())
  assertEquals(4, NonJsType.staticX)

  NonJsType.setStaticC(20)
  assertEquals(20, NonJsType.getStaticC())
  assertEquals(20, getNonJsTypeStaticAbc())
  assertEquals(20, NonJsType.staticX)

  setNonJsTypeStaticAbc(24)
  assertEquals(24, NonJsType.getStaticC())
  assertEquals(24, getNonJsTypeStaticAbc())
  assertEquals(24, NonJsType.staticX)
}

private fun testNativeStaticJsProperty() {
  val pi = NonJsType.getB().toInt()
  assertTrue(pi == 3)
}

private fun testNativeInstanceJsProperty() {
  assertTrue(NonJsType().getA() != null)
}

interface InterfaceWithDefaultJsProperties {
  fun getterCalled(): Int

  fun setterCalled(v: Int)

  @JsProperty fun getValue(): Int = getterCalled()

  @JsProperty fun setValue(value: Int) = setterCalled(value)
}

class ImplementorWithDefaultJsProperties : InterfaceWithDefaultJsProperties {
  var v: Int = 0

  override fun getterCalled() = v

  override fun setterCalled(v: Int) {
    this.v = v
  }
}

fun testDefaultMethodJsProperty() {
  val i: InterfaceWithDefaultJsProperties = ImplementorWithDefaultJsProperties()
  i.setValue(3)
  assertTrue(3 == i.getValue())
}

@JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
private external fun createMyNativeJsType(): MyNativeJsType

@JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
private external fun createJsTypeGetProperty(): JsTypeGetProperty

@JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
private external fun createJsTypeIsProperty(): JsTypeIsProperty

@JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
private external fun isUndefined(value: Int): Boolean

@JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
private external fun getMyConcreteJsTypeStaticY(): Int

@JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
private external fun setMyConcreteJsTypeStaticY(value: Int)

@JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
private external fun getMyConcreteJsTypeStaticX(): Int

@JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
private external fun setMyConcreteJsTypeStaticX(value: Int)

@JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
private external fun getMyConcreteJsTypeStaticAbc(): Int

@JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
private external fun setMyConcreteJsTypeStaticAbc(value: Int)

@JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
internal external fun getProperty(o: Any?, name: String): Int

@JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
internal external fun setProperty(o: Any?, name: String, value: Int)

@JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
private external fun getNonJsTypeStaticX(): Int

@JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
private external fun setNonJsTypeStaticX(value: Int)

@JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
private external fun getMyConcreteJsTypeAbc(o: Any?): Int

@JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
private external fun setMyConcreteJsTypeAbc(o: Any?, value: Int)

@JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
private external fun getNonJsTypeAbc(o: Any?): Int

@JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
private external fun setNonJsTypeAbc(o: Any?, value: Int)

@JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
private external fun getNonJsTypeStaticAbc(): Int

@JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
private external fun setNonJsTypeStaticAbc(value: Int)

class KotlinProperties {
  @JsProperty
  val customGetterVal = 10
    get() = field + 1

  @JsProperty
  val declaredGetterVal = 20
    get

  @JsProperty val defaultAccessorsVal = 30

  @JsProperty
  private val privateCustomGetterVal = 40
    get() = field + 1

  @JsProperty
  private val privateDeclaredGetterVal = 50
    get

  @JsProperty private val privateDefaultAccessorsVal = 60

  @JsProperty
  var customAccessorsVar = 70
    get() = field + 1
    set(value) {
      field = value + 1
    }

  @JsProperty
  var declaredAccessorsVar = 80
    get
    set

  @JsProperty var defaultAccessorsVar = 90

  @JsProperty
  var customSetterVar = 100
    set(value) {
      field = value + 1
    }

  @JsProperty
  var declaredSetterVar = 110
    set

  @JsProperty
  private var privateCustomAccessorsVar = 120
    get() = field + 1
    set(value) {
      field = value + 1
    }

  @JsProperty
  private var privateDeclaredAcessorsVar = 130
    get
    set

  @JsProperty private var privateDefaultAccessorsVar = 140

  @JsProperty
  private var privateCustomSetterVar = 150
    set(value) {
      field = value + 1
    }

  @JsProperty
  private var privateDeclaredSetterVar = 160
    set

  fun testPropertyAccessors() {
    assertEquals(11, customGetterVal)
    assertEquals(20, declaredGetterVal)
    assertEquals(30, defaultAccessorsVal)

    assertEquals(41, privateCustomGetterVal)
    assertEquals(50, privateDeclaredGetterVal)
    assertEquals(60, privateDefaultAccessorsVal)

    assertEquals(71, customAccessorsVar)
    customAccessorsVar = 0
    assertEquals(2, customAccessorsVar)

    assertEquals(80, declaredAccessorsVar)
    declaredAccessorsVar = 0
    assertEquals(0, declaredAccessorsVar)

    assertEquals(90, defaultAccessorsVar)
    defaultAccessorsVar = 0
    assertEquals(0, defaultAccessorsVar)

    assertEquals(100, customSetterVar)
    customSetterVar = 0
    assertEquals(1, customSetterVar)

    assertEquals(110, declaredSetterVar)
    declaredSetterVar = 0
    assertEquals(0, declaredSetterVar)

    assertEquals(121, privateCustomAccessorsVar)
    privateCustomAccessorsVar = 0
    assertEquals(2, privateCustomAccessorsVar)

    assertEquals(130, privateDeclaredAcessorsVar)
    privateDeclaredAcessorsVar = 0
    assertEquals(0, privateDeclaredAcessorsVar)

    assertEquals(140, privateDefaultAccessorsVar)
    privateDefaultAccessorsVar = 0
    assertEquals(0, privateDefaultAccessorsVar)

    assertEquals(150, privateCustomSetterVar)
    privateCustomSetterVar = 0
    assertEquals(1, privateCustomSetterVar)

    assertEquals(160, privateDeclaredSetterVar)
    privateDeclaredSetterVar = 0
    assertEquals(0, privateDeclaredSetterVar)
  }
}
