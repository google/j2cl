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
    testJsPropertyGetX()
    testJsPropertyIsX()
    testNativeJsType()
    testNativeJsTypeWithConstructor()
    testProtectedNames()
  }

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

  @JsType(isNative = true, namespace = "jsinteroptests.JsPropertyTest", name = "MyNativeJsType")
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



  @JsType(isNative = true, namespace = "jsinteroptests.JsPropertyTest", name = "MyNativeJsType")
  open internal class MyNativeJsTypeWithConstructor(x: Int) {
    @JvmField var ctorExecuted: Boolean = definedExternally
    @JvmField var x: Int = definedExternally
  }

  private fun testNativeJsTypeWithConstructor() {
    val obj = MyNativeJsTypeWithConstructor(12)
    assertTrue(obj.ctorExecuted)
    assertEquals(12, obj.x)
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

  @JsMethod(namespace = "jsinteroptests.JsPropertyTestHelper")
  @JvmStatic
  private external fun createMyNativeJsType(): MyNativeJsType

  @JsMethod(namespace = "jsinteroptests.JsPropertyTestHelper")
  @JvmStatic
  private external fun createJsTypeGetProperty(): JsTypeGetProperty

  @JsMethod(namespace = "jsinteroptests.JsPropertyTestHelper")
  @JvmStatic
  private external fun createJsTypeIsProperty(): JsTypeIsProperty

  @JsMethod(namespace = "jsinteroptests.JsPropertyTestHelper")
  @JvmStatic
  private external fun createMyJsInterfaceWithProtectedNames(): MyJsTypeInterfaceWithProtectedNames

  @JsMethod(namespace = "jsinteroptests.JsPropertyTestHelper")
  @JvmStatic
  private external fun isUndefined(value: Int): Boolean

  @JsMethod(namespace = "jsinteroptests.JsPropertyTestHelper")
  @JvmStatic
  private external fun hasField(o: Any?, fieldName: String): Boolean

  @JsMethod(namespace = "jsinteroptests.JsPropertyTestHelper")
  @JvmStatic
  private external fun getProperty(o: Any?, name: String): Int

  @JsMethod(namespace = "jsinteroptests.JsPropertyTestHelper")
  @JvmStatic
  private external fun setProperty(o: Any?, name: String, value: Int)

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
