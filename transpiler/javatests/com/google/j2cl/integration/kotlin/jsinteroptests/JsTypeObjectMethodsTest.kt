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
import com.google.j2cl.integration.testing.Asserts.assertNotNull
import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsConstructor
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsType
import kotlin.js.definedExternally

/** Tests JsType object method devirtualization functionality. */
object JsTypeObjectMethodsTest {
  fun testAll() {
    testEquals()
    testHashCode()
    testJavaLangObjectMethodsOrNativeSubtypes()
    testObjectMethodDispatch()
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  internal class NativeObjectClass {}

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  private interface NativeObjectInterface {}

  @JsType(isNative = true) private interface NativeInterface {}

  @JsMethod
  @JvmStatic
  private external fun createWithEqualsAndHashCode(a: Int, b: Int): NativeObjectInterface

  @JsMethod
  @JvmStatic
  private external fun createWithoutEqualsAndHashCode(a: Int, b: Int): NativeObjectInterface

  @JsType(isNative = true)
  open internal class NativeClassWithHashCode {
    internal constructor()

    internal constructor(n: Int)

    override external fun hashCode(): Int

    @JvmField var myValue: Int = definedExternally
  }

  internal class SubclassNativeClassWithHashCode @JsConstructor constructor(private var n: Int) :
    NativeClassWithHashCode() {
    @JsMethod override fun hashCode(): Int = n
  }

  internal class ImplementsNativeInterface(private var n: Int) : NativeInterface {
    @JsMethod override fun hashCode(): Int = n
  }

  private fun testHashCode() {
    assertEquals(3, createWithEqualsAndHashCode(1, 3).hashCode())
    val o1: NativeObjectInterface = createWithoutEqualsAndHashCode(1, 3)
    val o2: NativeObjectInterface = createWithoutEqualsAndHashCode(1, 3)
    assertTrue(o1.hashCode() != o2.hashCode())
    assertTrue((o1 as Any).hashCode() != (o2 as Any).hashCode())
    assertEquals(8, SubclassNativeClassWithHashCode(8).hashCode())
    assertEquals(8, (SubclassNativeClassWithHashCode(8) as Any).hashCode())
    assertEquals(9, (ImplementsNativeInterface(9) as Any).hashCode())
    assertEquals(10, callHashCode(SubclassNativeClassWithHashCode(10)))
  }

  private fun testEquals() {
    assertEquals(createWithEqualsAndHashCode(1, 3), createWithEqualsAndHashCode(1, 4))
    val o1: NativeObjectInterface = createWithoutEqualsAndHashCode(1, 3)
    val o2: NativeObjectInterface = createWithoutEqualsAndHashCode(1, 3)
    assertTrue(createWithEqualsAndHashCode(1, 3) == createWithoutEqualsAndHashCode(1, 4))
    assertTrue((createWithEqualsAndHashCode(1, 3) as Any) == createWithoutEqualsAndHashCode(1, 4))
    assertFalse(createWithoutEqualsAndHashCode(1, 4) == createWithEqualsAndHashCode(1, 3))
    assertFalse((createWithoutEqualsAndHashCode(1, 4) as Any) == createWithEqualsAndHashCode(1, 3))
    assertFalse(o1 == o2)
    assertFalse((o1 as Any) == o2)
  }

  @JsMethod @JvmStatic private external fun callHashCode(obj: Any?): Int

  private class SubtypeOfNativeClass @JsConstructor constructor(n: Int) :
    NativeClassWithHashCode() {
    init {
      myValue = n
    }

    override fun toString(): String = "(Sub)myValue: " + myValue
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
  interface GenericObjectInterfaceWithObjectMethods {
    override fun equals(other: Any?): Boolean

    override fun hashCode(): Int

    override fun toString(): String
  }

  private fun testJavaLangObjectMethodsOrNativeSubtypes() {
    assertTrue(NativeClassWithHashCode(3).equals(NativeClassWithHashCode(3)))
    assertFalse(NativeClassWithHashCode(3).equals(NativeClassWithHashCode(4)))

    assertTrue(NativeClassWithHashCode(6).equals(SubtypeOfNativeClass(6)))
    assertTrue(NativeClassWithHashCode(6).toString().contains("myValue: 6"))
    assertEquals("(Sub)myValue: 6", SubtypeOfNativeClass(6).toString())

    // Tests that hashcode is actually working through the object trampoline and
    // assumes that consecutive hashchodes are different.
    // TODO(b/31272546): uncomment when bug is fixed
    // assertFalse(createMyNativeError(3).hashCode() == MyNativeError().hashCode());

    val o = NativeObjectClass() as GenericObjectInterfaceWithObjectMethods
    assertNotNull(o.toString())
    // TODO(b/31272546): uncomment when bug is fixed
    // assertNotNull(((Double) (double) o.hashCode()));
    // Do not change to assertEquals because we are testing the dispatch to equals().
    // assertTrue(o.equals(o));
  }

  @JsType(isNative = true, name = "NativeJsTypeImplementsObjectMethods")
  internal class NativeClassWithDeclarations internal constructor(number: Double) {

    override external fun equals(other: Any?): Boolean

    override external fun hashCode(): Int

    override external fun toString(): String
  }

  @JsType(isNative = true, name = "NativeJsTypeImplementsObjectMethods")
  internal class NativeClassWithoutDeclarations internal constructor(number: Double)

  @JsType(isNative = true, name = "NativeJsTypeImplementsObjectMethods")
  internal interface NativeInterfaceWithDeclarations {
    override fun equals(other: Any?): Boolean

    override fun hashCode(): Int

    override fun toString(): String
  }

  @JsType(isNative = true, name = "NativeJsTypeImplementsObjectMethods")
  internal interface NativeInterfaceWithoutDeclarations {}

  private fun testObjectMethodDispatch() {
    val nativeClassWithDeclarations = NativeClassWithDeclarations(5.0)
    val nativeClassWithoutDeclarations = NativeClassWithoutDeclarations(5.0)

    // Do not change to assertEquals because we are testing the dispatch to equals().
    assertTrue(nativeClassWithDeclarations.equals(nativeClassWithoutDeclarations))
    assertTrue(nativeClassWithoutDeclarations.equals(nativeClassWithDeclarations))

    assertEquals(nativeClassWithDeclarations.toString(), nativeClassWithoutDeclarations.toString())
    assertTrue(nativeClassWithDeclarations.toString().contains("Native Object with value"))
    assertEquals(nativeClassWithDeclarations.hashCode(), nativeClassWithoutDeclarations.hashCode())

    val nativeInterfaceWithDeclarations =
      nativeClassWithDeclarations as NativeInterfaceWithDeclarations
    val nativeInterfaceWithoutDeclarations =
      nativeClassWithoutDeclarations as NativeInterfaceWithoutDeclarations

    // Do not change to assertEquals because we are testing the dispatch to equals().
    assertTrue(nativeInterfaceWithDeclarations.equals(nativeInterfaceWithoutDeclarations))
    assertTrue(nativeInterfaceWithoutDeclarations.equals(nativeInterfaceWithDeclarations))

    assertEquals(
      nativeInterfaceWithDeclarations.toString(),
      nativeInterfaceWithoutDeclarations.toString(),
    )
    assertTrue(nativeInterfaceWithDeclarations.toString().contains("Native Object with value"))
    assertEquals(
      nativeInterfaceWithDeclarations.hashCode(),
      nativeInterfaceWithoutDeclarations.hashCode(),
    )
  }
}
