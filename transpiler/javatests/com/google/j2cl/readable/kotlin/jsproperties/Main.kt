/*
 * Copyright 2022 Google Inc.
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

import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsProperty

var f: Int = 0

@JsProperty var g: Int = 10

@JsProperty
fun getA(): Int {
  return f + 1
}

@JsProperty
fun setA(x: Int) {
  f = x + 2
}

@JsProperty(name = "abc")
fun getB(): Int {
  return f + 3
}

@JsProperty(name = "abc")
fun setB(x: Int) {
  f = x + 4
}

/** Tests for non native instance JsProperty. */
class Bar(private var f: Int) {
  @JsProperty private var privateField: Int = -1

  @JsProperty
  fun getA(): Int {
    return f + 1
  }

  @JsProperty
  fun setA(x: Int) {
    f = x + 2
  }

  @JsProperty(name = "abc")
  fun getB(): Int {
    return f + 3
  }

  @JsProperty(name = "abc")
  fun setB(x: Int) {
    f = x + 4
  }
}

class Buzz(@JsProperty val f: Int) {
  val fieldGetter: Int
    @JsProperty get() = 10

  val backingFieldGetter: Int = 20
    @JsProperty get

  var backingFieldSetter: Int = 30
    @JsProperty set

  var fieldGetterAndSetter: Int
    @JsProperty get() = 40
    @JsProperty set(x) {}

  var fieldNonJsPropertySetter: Int
    @JsProperty get() = 50
    set(x) {}

  var fieldNonJsPropertyGetter: Int
    get() = 60
    @JsProperty set(x) {}

  @JsProperty(name = "otherName") var fieldWithCustomName: Int = 70

  var getterSetterWithCustomName: Int = 80
    @JsProperty(name = "anotherName") get
    @JsProperty(name = "mismatchedName") set
}

/** Tests for native JsProperty. */
class NativeFoo {
  @JsProperty(name = "hasOwnProperty") external fun getA(): Any

  val b: Any
    @JsProperty(name = "hasOwnProperty") external get
}

@JsProperty(name = "Math.PI", namespace = JsPackage.GLOBAL) external fun getNativeB(): Double

val nativeProperty: Double
  @JsProperty(name = "Math.E", namespace = JsPackage.GLOBAL) external get

interface InterfaceWithDefaultJsProperties {
  @JsProperty
  var m: Int
    get() = 3
    set(value) {}
}

class ImplementsInterfaceWithDefaultJsProperties : InterfaceWithDefaultJsProperties

class ClassWithJvmField {
  @JvmField var f = 1

  companion object {
    @JvmField var x = 1
  }
}

open class ValueHolder {
  @JsProperty @JvmField var value: Any? = null
}

class HasFieldAndGetterSetterFuns : ValueHolder() {
  fun getValue(): Any? = Any()

  fun setValue(v: Any?) {}
}

class Main {
  fun testNativeJsProperty() {
    NativeFoo().getA()
    NativeFoo().b
    getNativeB()
    nativeProperty
  }

  fun testStaticJsProperty() {
    getA()
    setA(10)
    getB()
    setB(10)
  }

  fun testInstanceJsProperty() {
    val bar = Bar(0)
    bar.getA()
    bar.setA(10)
    bar.getB()
    bar.setB(10)
  }

  fun testFromJava() {
    JavaFoo().bar()
    JavaFoo().buzz
  }

  fun testJvmField() {
    var z = ClassWithJvmField().f
    z = ClassWithJvmField.Companion.x
    z = ClassWithJvmField.x
  }

  fun testHasExplicitGetterSetterFuns() {
    val x = HasFieldAndGetterSetterFuns()
    x.setValue("a")
    x.getValue()

    val y = OtherHasFieldAndGetterSetterFuns()
    y.setValue("a")
    y.getValue()
  }
}

class KotlinProperties {
  @JsProperty
  val a = 10
    get() = field + 1

  @JsProperty
  val b = 20
    get

  @JsProperty val c = 30

  @JsProperty
  private val d = 40
    get() = field + 1

  @JsProperty
  private val e = 50
    get

  @JsProperty private val f = 60

  @JsProperty
  var g = 70
    get() = field + 1
    set(value) {
      field = value + 1
    }

  @JsProperty
  var h = 80
    get
    set

  @JsProperty var i = 90

  @JsProperty
  var j = 100
    set(value) {
      field = value + 1
    }

  @JsProperty
  var k = 110
    set

  @JsProperty
  private var l = 120
    get() = field + 1
    set(value) {
      field = value + 1
    }

  @JsProperty
  private var m = 130
    get
    set

  @JsProperty private var n = 140

  @JsProperty
  private var o = 150
    set(value) {
      field = value + 1
    }

  @JsProperty
  private var p = 160
    set
}
