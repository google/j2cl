/*
 * Copyright 2026 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package jsinteropinstanceof

import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsConstructor
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsOverlay
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsType

fun main() {
  testInstanceOf_concreteJsType()
  testInstanceOf_extendsJsTypeWithProto()
  testInstanceOf_implementsJsType()
  testInstanceOf_implementsJsTypeWithPrototype()
  testInstanceOf_jsoWithNativeButtonProto()
  testInstanceOf_jsoWithoutProto()
  testInstanceOf_jsoWithProto()
  testInstanceOf_classWithCustomIsInstance()
  testInstanceOf_interfaceWithCustomIsInstance()
  testInstanceOf_withNameSpace()
}

@JsType(isNative = true, namespace = "test.foo", name = "MyNativeJsTypeInterface")
internal interface MyNativeJsTypeInterface

internal class MyNativeJsTypeInterfaceImpl : MyNativeJsTypeInterface

@JsType(isNative = true, namespace = "qux", name = "JsTypeTest_MyNativeJsType")
internal open class MyNativeJsType

internal open class MyNativeJsTypeSubclass @JsConstructor constructor() : MyNativeJsType()

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Array")
internal class MyNativeClassWithCustomIsInstance {
  companion object {
    @JsOverlay @JvmStatic fun `$isInstance`(o: Any?): Boolean = isCustomIsInstanceClassSingleton(o)
  }
}

private val CUSTOM_IS_INSTANCE_CLASS_SINGLETON: String = "CustomIsInstanceClass"

private fun isCustomIsInstanceClassSingleton(o: Any?): Boolean =
  o === CUSTOM_IS_INSTANCE_CLASS_SINGLETON

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Array")
internal interface MyNativeInterfaceWithCustomIsInstance {
  companion object {
    @JsOverlay
    @JvmStatic
    fun `$isInstance`(o: Any?): Boolean = isCustomIsInstanceInterfaceSingleton(o)
  }
}

private val CUSTOM_IS_INSTANCE_INTERFACE_SINGLETON: String = "CustomIsInstanceInterface"

private fun isCustomIsInstanceInterfaceSingleton(o: Any?): Boolean =
  o === CUSTOM_IS_INSTANCE_INTERFACE_SINGLETON

internal class MyNativeJsTypeSubclassWithIterator @JsConstructor constructor() :
  MyNativeJsType(), Iterable<Any?> {
  override fun iterator(): Iterator<Any?> {
    throw UnsupportedOperationException()
  }
}

@JsType(namespace = JsPackage.GLOBAL, name = "HTMLElement", isNative = true)
internal open class HTMLElementConcreteNativeJsType

@JsType(namespace = JsPackage.GLOBAL, name = "HTMLElement", isNative = true)
internal class HTMLElementAnotherConcreteNativeJsType

@JsType(namespace = JsPackage.GLOBAL, name = "HTMLButtonElement", isNative = true)
internal class HTMLButtonElementConcreteNativeJsType : HTMLElementConcreteNativeJsType()

/** Implements ElementLikeJsInterface. */
internal class ElementLikeNativeInterfaceImpl : ElementLikeNativeInterface {
  override fun getTagName(): String = "mytag"
}

/** A test class marked with JsType but isn't referenced from any Java code except instanceof. */
@JsType internal interface MyJsInterfaceWithOnlyInstanceofReference

/** A test class marked with JsType but isn't referenced from any Java code except instanceof. */
@JsType(isNative = true, namespace = "qux", name = "JsTypeTest_MyNativeJsType")
internal class AliasToMyNativeJsTypeWithOnlyInstanceofReference

@JsType(isNative = true, namespace = "testfoo.bar")
internal class MyNamespacedNativeJsType

@JsType internal open class ConcreteJsType

private fun testInstanceOf_jsoWithProto() {
  val o: Any? = createMyNativeJsType()

  assertTrue(o is Any)
  assertFalse(o is HTMLElementConcreteNativeJsType)
  assertFalse(o is HTMLElementAnotherConcreteNativeJsType)
  assertFalse(o is HTMLButtonElementConcreteNativeJsType)
  assertFalse(o is Iterator<*>)
  assertTrue(o is MyNativeJsType)
  assertFalse(o is MyNativeJsTypeSubclass)
  assertFalse(o is MyNativeJsTypeSubclassWithIterator)
  assertFalse(o is MyNativeJsTypeInterfaceImpl)
  assertFalse(o is MyNativeClassWithCustomIsInstance)
  assertFalse(o is MyNativeInterfaceWithCustomIsInstance)
  assertFalse(o is ElementLikeNativeInterfaceImpl)
  assertFalse(o is MyJsInterfaceWithOnlyInstanceofReference)
  assertTrue(o is AliasToMyNativeJsTypeWithOnlyInstanceofReference)
  assertFalse(o is ConcreteJsType)
  assertFalse(o is Array<*> && o.isArrayOf<MyNativeJsTypeInterface>())
  assertFalse(o is Array<*> && o.isArrayOf<Array<MyNativeJsTypeInterfaceImpl>>())
}

private fun testInstanceOf_jsoWithoutProto() {
  val o: Any? = createObject()

  assertTrue(o is Any)
  assertFalse(o is HTMLElementConcreteNativeJsType)
  assertFalse(o is HTMLElementAnotherConcreteNativeJsType)
  assertFalse(o is HTMLButtonElementConcreteNativeJsType)
  assertFalse(o is Iterator<*>)
  assertFalse(o is MyNativeJsType)
  assertFalse(o is MyNativeJsTypeSubclass)
  assertFalse(o is MyNativeJsTypeSubclassWithIterator)
  assertFalse(o is MyNativeJsTypeInterfaceImpl)
  assertFalse(o is MyNativeClassWithCustomIsInstance)
  assertFalse(o is MyNativeInterfaceWithCustomIsInstance)
  assertFalse(o is ElementLikeNativeInterfaceImpl)
  assertFalse(o is MyJsInterfaceWithOnlyInstanceofReference)
  assertFalse(o is AliasToMyNativeJsTypeWithOnlyInstanceofReference)
  assertFalse(o is ConcreteJsType)
  assertFalse(o is Array<*> && o.isArrayOf<MyNativeJsTypeInterface>())
  assertFalse(o is Array<*> && o.isArrayOf<Array<MyNativeJsTypeInterfaceImpl>>())
}

private fun testInstanceOf_jsoWithNativeButtonProto() {
  val o: Any? = createNativeButton()

  assertTrue(o is Any)
  assertTrue(o is HTMLElementConcreteNativeJsType)
  assertTrue(o is HTMLElementAnotherConcreteNativeJsType)
  assertTrue(o is HTMLButtonElementConcreteNativeJsType)
  assertFalse(o is Iterator<*>)
  assertFalse(o is MyNativeJsType)
  assertFalse(o is MyNativeJsTypeSubclass)
  assertFalse(o is MyNativeJsTypeSubclassWithIterator)
  assertFalse(o is MyNativeJsTypeInterfaceImpl)
  assertFalse(o is MyNativeClassWithCustomIsInstance)
  assertFalse(o is MyNativeInterfaceWithCustomIsInstance)
  assertFalse(o is ElementLikeNativeInterfaceImpl)
  assertFalse(o is MyJsInterfaceWithOnlyInstanceofReference)
  assertFalse(o is AliasToMyNativeJsTypeWithOnlyInstanceofReference)
  assertFalse(o is ConcreteJsType)
  assertFalse(o is Array<*> && o.isArrayOf<MyNativeJsTypeInterface>())
  assertFalse(o is Array<*> && o.isArrayOf<Array<MyNativeJsTypeInterfaceImpl>>())
}

private fun testInstanceOf_implementsJsType() {
  // Foils type tightening.
  val o: Any? = ElementLikeNativeInterfaceImpl()

  assertTrue(o is Any)
  assertFalse(o is HTMLElementConcreteNativeJsType)
  assertFalse(o is HTMLElementAnotherConcreteNativeJsType)
  assertFalse(o is HTMLButtonElementConcreteNativeJsType)
  assertFalse(o is Iterator<*>)
  assertFalse(o is MyNativeJsType)
  assertFalse(o is MyNativeJsTypeInterfaceImpl)
  assertFalse(o is MyNativeClassWithCustomIsInstance)
  assertFalse(o is MyNativeInterfaceWithCustomIsInstance)
  assertTrue(o is ElementLikeNativeInterfaceImpl)
  assertFalse(o is MyJsInterfaceWithOnlyInstanceofReference)
  assertFalse(o is AliasToMyNativeJsTypeWithOnlyInstanceofReference)
  assertFalse(o is ConcreteJsType)
  assertFalse(o is Array<*> && o.isArrayOf<MyNativeJsTypeInterface>())
  assertFalse(o is Array<*> && o.isArrayOf<Array<MyNativeJsTypeInterfaceImpl>>())
}

private fun testInstanceOf_implementsJsTypeWithPrototype() {
  // Foils type tightening.
  val o: Any? = MyNativeJsTypeInterfaceImpl()

  assertTrue(o is Any)
  assertFalse(o is HTMLElementConcreteNativeJsType)
  assertFalse(o is HTMLElementAnotherConcreteNativeJsType)
  assertFalse(o is HTMLButtonElementConcreteNativeJsType)
  assertFalse(o is Iterator<*>)
  assertFalse(o is MyNativeJsType)
  assertTrue(o is MyNativeJsTypeInterfaceImpl)
  assertFalse(o is MyNativeClassWithCustomIsInstance)
  assertFalse(o is MyNativeInterfaceWithCustomIsInstance)
  assertFalse(o is ElementLikeNativeInterfaceImpl)
  assertFalse(o is MyJsInterfaceWithOnlyInstanceofReference)
  assertFalse(o is AliasToMyNativeJsTypeWithOnlyInstanceofReference)
  assertFalse(o is ConcreteJsType)
  assertFalse(o is Array<*> && o.isArrayOf<MyNativeJsTypeInterface>())
  assertFalse(o is Array<*> && o.isArrayOf<Array<MyNativeJsTypeInterfaceImpl>>())
}

private fun testInstanceOf_concreteJsType() {
  // Foils type tightening.
  val o: Any? = ConcreteJsType()

  assertTrue(o is Any)
  assertFalse(o is HTMLElementConcreteNativeJsType)
  assertFalse(o is HTMLElementAnotherConcreteNativeJsType)
  assertFalse(o is HTMLButtonElementConcreteNativeJsType)
  assertFalse(o is Iterator<*>)
  assertFalse(o is MyNativeJsType)
  assertFalse(o is MyNativeJsTypeSubclass)
  assertFalse(o is MyNativeJsTypeSubclassWithIterator)
  assertFalse(o is MyNativeJsTypeInterfaceImpl)
  assertFalse(o is MyNativeClassWithCustomIsInstance)
  assertFalse(o is MyNativeInterfaceWithCustomIsInstance)
  assertFalse(o is ElementLikeNativeInterfaceImpl)
  assertFalse(o is MyJsInterfaceWithOnlyInstanceofReference)
  assertFalse(o is AliasToMyNativeJsTypeWithOnlyInstanceofReference)
  assertTrue(o is ConcreteJsType)
  assertFalse(o is Array<*> && o.isArrayOf<MyNativeJsTypeInterface>())
  assertFalse(o is Array<*> && o.isArrayOf<Array<MyNativeJsTypeInterfaceImpl>>())
}

private fun testInstanceOf_extendsJsTypeWithProto() {
  // Foils type tightening.
  val o: Any? = MyNativeJsTypeSubclassWithIterator()

  assertTrue(o is Any)
  assertTrue(o is MyNativeJsType)
  assertFalse(o is MyNativeJsTypeSubclass)
  assertTrue(o is MyNativeJsTypeSubclassWithIterator)
  assertFalse(o is HTMLElementConcreteNativeJsType)
  assertFalse(o is HTMLElementAnotherConcreteNativeJsType)
  assertFalse(o is HTMLButtonElementConcreteNativeJsType)
  assertTrue(o is Iterable<*>)
  assertFalse(o is MyNativeJsTypeInterfaceImpl)
  assertFalse(o is MyNativeClassWithCustomIsInstance)
  assertFalse(o is MyNativeInterfaceWithCustomIsInstance)
  assertFalse(o is ElementLikeNativeInterfaceImpl)
  assertFalse(o is MyJsInterfaceWithOnlyInstanceofReference)
  assertTrue(o is AliasToMyNativeJsTypeWithOnlyInstanceofReference)
  assertFalse(o is ConcreteJsType)
  assertFalse(o is Array<*> && o.isArrayOf<MyNativeJsTypeInterface>())
  assertFalse(o is Array<*> && o.isArrayOf<Array<MyNativeJsTypeInterfaceImpl>>())
}

private fun testInstanceOf_classWithCustomIsInstance() {
  val o: Any? = CUSTOM_IS_INSTANCE_CLASS_SINGLETON

  assertTrue(o is Any)
  assertTrue(o is String)
  assertFalse(o is HTMLElementConcreteNativeJsType)
  assertFalse(o is HTMLElementAnotherConcreteNativeJsType)
  assertFalse(o is HTMLButtonElementConcreteNativeJsType)
  assertFalse(o is Iterator<*>)
  assertFalse(o is MyNativeJsType)
  assertFalse(o is MyNativeJsTypeSubclass)
  assertFalse(o is MyNativeJsTypeSubclassWithIterator)
  assertFalse(o is MyNativeJsTypeInterfaceImpl)
  assertTrue(o is MyNativeClassWithCustomIsInstance)
  assertFalse(o is MyNativeInterfaceWithCustomIsInstance)
  assertFalse(o is ElementLikeNativeInterfaceImpl)
  assertFalse(o is MyJsInterfaceWithOnlyInstanceofReference)
  assertFalse(o is AliasToMyNativeJsTypeWithOnlyInstanceofReference)
  assertFalse(o is ConcreteJsType)
  assertFalse(o is Array<*> && o.isArrayOf<MyNativeJsTypeInterface>())
  assertFalse(o is Array<*> && o.isArrayOf<Array<MyNativeJsTypeInterfaceImpl>>())
}

private fun testInstanceOf_interfaceWithCustomIsInstance() {
  val o: Any? = CUSTOM_IS_INSTANCE_INTERFACE_SINGLETON

  assertTrue(o is Any)
  assertTrue(o is String)
  assertFalse(o is HTMLElementConcreteNativeJsType)
  assertFalse(o is HTMLElementAnotherConcreteNativeJsType)
  assertFalse(o is HTMLButtonElementConcreteNativeJsType)
  assertFalse(o is Iterator<*>)
  assertFalse(o is MyNativeJsType)
  assertFalse(o is MyNativeJsTypeSubclass)
  assertFalse(o is MyNativeJsTypeSubclassWithIterator)
  assertFalse(o is MyNativeJsTypeInterfaceImpl)
  assertFalse(o is MyNativeClassWithCustomIsInstance)
  assertTrue(o is MyNativeInterfaceWithCustomIsInstance)
  assertFalse(o is ElementLikeNativeInterfaceImpl)
  assertFalse(o is MyJsInterfaceWithOnlyInstanceofReference)
  assertFalse(o is AliasToMyNativeJsTypeWithOnlyInstanceofReference)
  assertFalse(o is ConcreteJsType)
  assertFalse(o is Array<*> && o.isArrayOf<MyNativeJsTypeInterface>())
  assertFalse(o is Array<*> && o.isArrayOf<Array<MyNativeJsTypeInterfaceImpl>>())
}

private fun testInstanceOf_withNameSpace() {
  val obj1: Any? = createMyNamespacedJsInterface()

  assertTrue(obj1 is MyNamespacedNativeJsType)
  assertFalse(obj1 is MyNativeJsType)
}

private fun createMyNativeJsType(): Any = MyNativeJsType()

private fun createMyNamespacedJsInterface(): Any? = MyNamespacedNativeJsType()

@JsMethod(namespace = "jsinteropinstanceof.JsTypeTestHelper")
private external fun createNativeButton(): Any?

@JsMethod(namespace = "jsinteropinstanceof.JsTypeTestHelper")
private external fun createObject(): Any?
