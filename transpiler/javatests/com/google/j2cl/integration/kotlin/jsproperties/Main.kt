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
import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsProperty

fun main(vararg args: String) {
  val m = Main()
  m.testInstanceJsProperty()
  m.testNativeInstanceJsProperty()
  m.testNativeStaticJsProperty()
  m.testStaticJsProperty()
  m.testDefaultMethodJsProperty()
  KotlinProperties().testPropertyAccessors()
}

class Main {

  fun testNativeStaticJsProperty() {
    val pi: Int = NativeFoo.getB() as Int
    assertTrue(pi == 3)
  }

  fun testNativeInstanceJsProperty() {
    assertTrue(NativeFoo().getA() != null)
  }

  fun testStaticJsProperty() {
    assertTrue(Foo.getF() == 10)
    assertTrue(Foo.getA() == 11)
    Foo.setA(10)
    assertTrue(Foo.getF() == 12)
    assertTrue(Foo.getA() == 13)
    assertTrue(Foo.getB() == 15)
    Foo.setB(20)
    assertTrue(Foo.getF() == 24)
    assertTrue(Foo.getB() == 27)

    // call by JS.
    assertTrue(getFooA() == 25)
    setFooA(30)
    assertTrue(getFooA() == 33)
    assertTrue(getFooB() == 35)
    setFooB(40)
    assertTrue(getFooB() == 47)
  }

  fun testInstanceJsProperty() {
    val bar = Bar()
    assertTrue(bar.getF() == 10)
    assertTrue(bar.getA() == 11)
    bar.setA(10)
    assertTrue(bar.getF() == 12)
    assertTrue(bar.getA() == 13)
    assertTrue(bar.getB() == 15)
    bar.setB(20)
    assertTrue(bar.getF() == 24)
    assertTrue(bar.getB() == 27)

    // call by JS.
    assertTrue(getBarA(bar) == 25)
    setBarA(bar, 30)
    assertTrue(getBarA(bar) == 33)
    assertTrue(getBarB(bar) == 35)
    setBarB(bar, 40)
    assertTrue(getBarB(bar) == 47)
  }

  interface InterfaceWithDefaultJsProperties {
    fun getterCalled(): Int

    fun setterCalled(v: Int)

    @JsProperty fun getValue(): Int = getterCalled()

    @JsProperty fun setValue(value: Int) = setterCalled(value)
  }

  inner class Implementor : InterfaceWithDefaultJsProperties {
    var v: Int = 0

    override fun getterCalled() = v

    override fun setterCalled(v: Int) {
      this.v = v
    }
  }

  fun testDefaultMethodJsProperty() {
    val i: InterfaceWithDefaultJsProperties = Implementor()
    i.setValue(3)
    assertTrue(3 == i.getValue())
  }

  companion object {
    @JvmStatic @JsMethod external fun getFooA(): Int

    @JvmStatic @JsMethod external fun getFooB(): Int

    @JvmStatic @JsMethod external fun getBarA(bar: Bar): Int

    @JvmStatic @JsMethod external fun getBarB(bar: Bar): Int

    @JvmStatic @JsMethod external fun setFooA(x: Int)

    @JvmStatic @JsMethod external fun setFooB(x: Int)

    @JvmStatic @JsMethod external fun setBarA(bar: Bar, x: Int)

    @JvmStatic @JsMethod external fun setBarB(bar: Bar, x: Int)
  }
}

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
