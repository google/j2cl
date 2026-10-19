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
@file:Suppress("KotlinConstantConditions")

package jstypenative

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertNotEquals
import com.google.j2cl.integration.testing.Asserts.assertNotNull
import com.google.j2cl.integration.testing.Asserts.assertTrue
import com.google.j2cl.integration.testing.TestUtils.isWasm
import jsinterop.annotations.JsFunction
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsOverlay
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsProperty
import jsinterop.annotations.JsType
import kotlin.js.definedExternally

fun main(vararg args: String) {
  testClassLiterals()
  testGetClass()
  testToString()
  testEqualityOptimization()
  testNativeJsTypeWithStaticInitializer()
  testNativeInnerClass()
  testSpecialNativeInstanceOf()
  testVariableExternCollision()
  testAliasExternCollision()
}

@JsType(isNative = true, namespace = "jstypenative") internal class MyNativeJsType

@JsType(isNative = true, namespace = "jstypenative") private interface MyNativeJsTypeInterface

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
internal class NativeObject : MyNativeJsTypeInterface

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
private class FinalNativeObject : MyNativeJsTypeInterface

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
private interface MyNativeJsTypeInterfaceOnlyOneConcreteImplementor

fun testClassLiterals() {
  // TODO(b/301385322): Remove when class literals on native JS objects are implemented.
  if (isWasm()) {
    return
  }
  assertEquals("<native object>", MyNativeJsType::class.java.name)
  // Native classes have all the same class literal.
  assertEquals(MyNativeJsType::class.java, NativeObject::class.java)

  // native interfaces and native classes have different class literals to satisfy Java semantics
  // with respect to Class.isInterface etc.
  assertNotEquals(MyNativeJsType::class.java, MyNativeJsTypeInterface::class.java)
  // Native interfaces have all the same class literal.
  assertEquals(
    MyNativeJsTypeInterface::class.java,
    MyNativeJsTypeInterfaceOnlyOneConcreteImplementor::class.java,
  )

  // Native arrays have all the same class literal, and is Object[]::class.java.
  assertEquals(Array<Any?>::class.java, Array<MyNativeJsType>::class.java)
  assertEquals(Array<MyNativeJsType>::class.java, Array<MyNativeJsType>::class.java)
  assertEquals(Array<MyNativeJsType>::class.java, Array<MyNativeJsTypeInterface>::class.java)
  assertEquals(Array<MyNativeJsType>::class.java, Array<Array<MyNativeJsType>>::class.java)
  assertEquals(Array<MyNativeJsType>::class.java, Array<Array<MyNativeJsTypeInterface>>::class.java)
}

fun testGetClass() {
  // TODO(b/301385322): Remove when class literals on native JS objects are implemented.
  if (isWasm()) {
    return
  }
  val o: Any = createNativeObjectWithoutToString()
  assertEquals(MyNativeJsType::class.java, o.javaClass)

  val nativeInterface = createNativeObjectWithoutToString() as MyNativeJsTypeInterface
  assertEquals(MyNativeJsType::class.java, nativeInterface.javaClass)

  // Test that the dispatch to getClass in not messed up by incorrectly marking nativeObject1 as
  // exact and inlining Object.javaClass implementation.
  val nativeObject1 = NativeObject()
  assertEquals(MyNativeJsType::class.java, nativeObject1.javaClass)

  // Test that the dispatch to getClass in not messed up by incorrectly marking nativeObject2 as
  // exact and inlining Object.javaClass implementation.
  val nativeObject2: FinalNativeObject = createNativeObject()
  assertEquals(MyNativeJsType::class.java, nativeObject2.javaClass)

  assertEquals(Array<MyNativeJsType>::class.java, createNativeArray().javaClass)
}

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
internal class AnotherFinalNativeObject : MyNativeJsTypeInterface

private fun same(thisObject: Any?, thatObject: Any?): Boolean = thisObject === thatObject

fun testEqualityOptimization() {
  // TODO(b/568071684): Optimize boxing for == comparisons for JS and Java types in Wasm.
  if (isWasm()) {
    return
  }
  // Makes sure that == does not get optimized away due to static class incompatibility.

  val finalNativeObject = FinalNativeObject()

  val anotherFinalNativeObject = (finalNativeObject as Any) as AnotherFinalNativeObject
  // DeadCodeElimination could optimize statically to false due to type incompatibility, which
  // could happen if both variables were marked as exact.
  assertTrue(same(anotherFinalNativeObject, finalNativeObject))
}

fun testToString() {
  val nativeObjectWithToString: Any? = createNativeObjectWithToString()
  // TODO(b/540445903): Consider forwarding native object methods.
  if (!isWasm()) {
    assertEquals("Native type", nativeObjectWithToString.toString())
  }

  val nativeObjectWithoutToString: Any? = createNativeObjectWithoutToString()
  // TODO(b/540445903): Consider forwarding native object methods.
  if (!isWasm()) {
    assertEquals("[object Object]", nativeObjectWithoutToString.toString())
  }

  val nativeArray: Any? = createNativeArray()
  // TODO(b/540445903): Consider forwarding native object methods.
  if (!isWasm()) {
    assertEquals("", nativeArray.toString())
  }
}

@JsMethod(namespace = "jstypenative.helper")
private external fun createNativeObject(): FinalNativeObject

@JsMethod(namespace = "jstypenative.helper")
private external fun createNativeObjectWithToString(): MyNativeJsType

@JsMethod(namespace = "jstypenative.helper", name = "createNativeObject")
private external fun createNativeObjectWithoutToString(): MyNativeJsType

@JsMethod(namespace = "jstypenative.helper") private external fun createNativeArray(): Any

@JsType(isNative = true)
internal class NativeJsTypeWithStaticInitializationAndFieldAccess {
  companion object {
    @JsOverlay @JvmField val o: Any? = 3
  }
}

@JsType(isNative = true)
internal class NativeJsTypeWithStaticInitializationAndStaticOverlayMethod {
  companion object {
    @JsOverlay @JvmField val o: Any? = 4

    @JsOverlay @JvmStatic fun getObject(): Any? = o
  }
}

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
internal class NativeJsTypeWithStaticInitializationAndInstanceOverlayMethod {
  companion object {
    @JsOverlay @JvmField val o: Any? = 5

    @JsOverlay @JvmStatic fun getObject(): Any? = o

    init {
      clinitCalled++
    }
  }
}

private var clinitCalled = 0

fun testNativeJsTypeWithStaticInitializer() {
  assertEquals(3 as Int?, NativeJsTypeWithStaticInitializationAndFieldAccess.o)
  assertEquals(0, clinitCalled)
  assertEquals(4 as Int?, NativeJsTypeWithStaticInitializationAndStaticOverlayMethod.getObject())
  assertEquals(
    5 as Int?,
    NativeJsTypeWithStaticInitializationAndInstanceOverlayMethod.Companion.getObject(),
  )
  assertEquals(1, clinitCalled)
}

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Function")
internal class NativeFunction

@JsMethod(namespace = "jstypenative.helper") private external fun createFunction(): Any?

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Array") internal class NativeArray

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Number") internal class NativeNumber

@JsMethod(namespace = "jstypenative.helper") private external fun createNumber(): Any?

@JsMethod(namespace = "jstypenative.helper") private external fun createBoxedNumber(): Any?

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "String") internal class NativeString

@JsMethod(namespace = "jstypenative.helper") private external fun createBoxedString(): Any?

@JsFunction
fun interface SomeFunctionInterface {
  fun m()
}

internal class SomeFunction : SomeFunctionInterface {
  override fun m() {}
}

fun testSpecialNativeInstanceOf() {
  val aJsFunction: Any? = SomeFunction()
  // True cases.
  assertTrue(aJsFunction is NativeFunction)
  assertTrue(aJsFunction is SomeFunctionInterface)
  assertTrue(aJsFunction is NativeObject)
  // False cases.
  assertFalse(aJsFunction is NativeArray)
  assertFalse(aJsFunction is NativeNumber)
  assertFalse(aJsFunction is NativeString)

  val anotherFunction: Any? = createFunction()
  // True cases.
  assertTrue(anotherFunction is NativeFunction)
  assertTrue(anotherFunction is SomeFunctionInterface)
  assertTrue(anotherFunction is NativeObject)
  // False cases.
  assertFalse(anotherFunction is NativeArray)
  assertFalse(anotherFunction is NativeNumber)
  assertFalse(anotherFunction is NativeString)

  val aString: Any? = "Hello"
  // True cases.
  // b/31271239: String primitives are not an instance of NativeString in JS.
  // assertTrue(aString instanceof NativeString);
  // False cases.
  assertFalse(aString is NativeFunction)
  assertFalse(aString is NativeObject)
  assertFalse(aString is NativeArray)
  assertFalse(aString is NativeNumber)

  val aBoxedString: Any? = createBoxedString()
  // True cases.
  // Note that boxed strings are (surprisingly) not strings but objects.
  assertTrue(aBoxedString is NativeObject)
  // False cases.
  assertFalse(aBoxedString is NativeFunction)
  assertFalse(aBoxedString is NativeArray)
  assertFalse(aBoxedString is NativeNumber)
  // b/31271239: String wrapper objects ARE an instance of NativeString in JS.
  // assertFalse(aBoxedString is NativeString);

  val anArray: Any? = emptyArray<String>()
  // True cases.
  assertTrue(anArray is NativeArray)
  assertTrue(anArray is NativeObject)
  // False cases.
  assertFalse(anArray is NativeFunction)
  assertFalse(anArray is NativeNumber)
  assertFalse(anArray is NativeString)

  val aNativeArray: Any? = createNativeArray()
  // True cases.
  assertTrue(aNativeArray is NativeArray)
  assertTrue(anArray is NativeObject)
  // False cases.
  assertFalse(aNativeArray is NativeFunction)
  assertFalse(aNativeArray is NativeNumber)
  assertFalse(aNativeArray is NativeString)

  val aNumber: Any? = 3.0
  // True cases.
  // b/31271239: This compiles into number primitives and are not an instance of
  // NativeNumber in JS.
  // assertTrue(aNumber is NativeNumber);
  // False cases.
  assertFalse(aNumber is NativeArray)
  assertFalse(aNumber is NativeObject)
  assertFalse(aNumber is NativeFunction)
  assertFalse(aNumber is NativeString)

  val anotherNumber: Any? = createNumber()
  // True cases.
  // b/31271239: Number primitives are not an instance of NativeNumber in JS.
  // assertTrue(anotherNumber is NativeNumber);
  // False cases.
  assertFalse(anotherNumber is NativeArray)
  assertFalse(anotherNumber is NativeObject)
  assertFalse(anotherNumber is NativeFunction)
  assertFalse(anotherNumber is NativeString)

  val aBoxedNumber: Any? = createBoxedNumber()
  // True cases.
  assertTrue(aBoxedNumber is NativeObject)
  // False cases.
  // b/31271239: Number wrapper objects ARE an instance of NativeNumber in JS.
  // assertFalse(aBoxedNumber is NativeNumber);
  assertFalse(aBoxedNumber is NativeArray)
  assertFalse(aBoxedNumber is NativeFunction)
  assertFalse(aBoxedNumber is NativeString)

  val anObject: Any? = Any()
  // True cases.
  assertTrue(anObject is NativeObject)
  // False cases.
  assertFalse(anObject is NativeNumber)
  assertFalse(anObject is NativeArray)
  assertFalse(anObject is NativeFunction)
  assertFalse(anObject is NativeString)

  val nullObject: Any? = null

  assertFalse(nullObject is NativeObject)
  assertFalse(nullObject is NativeArray)
  assertFalse(nullObject is NativeFunction)
  assertFalse(nullObject is NativeString)
  assertFalse(nullObject is NativeNumber)

  val undefined: Any? = getUndefined()
  assertFalse(undefined is NativeObject)
  assertFalse(undefined is NativeArray)
  assertFalse(undefined is NativeFunction)
  assertFalse(undefined is NativeString)
  assertFalse(undefined is NativeNumber)
}

@JsProperty(namespace = JsPackage.GLOBAL) private external fun getUndefined(): Any?

@SuppressWarnings("unused")
fun testVariableExternCollision() {
  val Int8Array: Any? = null
  // A variable name that would collide with an extern.
  assertNotNull(getInt8ArrayBytesPerElement())
}

@JsProperty(namespace = JsPackage.GLOBAL, name = "Int8Array.BYTES_PER_ELEMENT")
private external fun getInt8ArrayBytesPerElement(): Double

fun testAliasExternCollision() {
  val unused: Float32Array = Float32Array()
  // make sure it is referenced hence aliased.
  assertNotNull(getFloat32ArrayBytesPerElement())
}

internal class Float32Array {}

@JsProperty(namespace = JsPackage.GLOBAL, name = "Float32Array.BYTES_PER_ELEMENT")
private external fun getFloat32ArrayBytesPerElement(): Any?

@JsType(isNative = true, namespace = "jstypenative", name = "MyNativeJsType")
internal class MyNativeJsTypeWithInner {
  @JsType(isNative = true)
  internal class Inner(n: Int) {
    @JvmField val n: Int = definedExternally
  }
}

// This is implemented in MyNativeJsType.js on purpose to make sure that the reference to
// the enclosing module is considered a Java type and all the scheme to avoid dependency
// cycles is correct.
@JsType(isNative = true, namespace = "jstypenative", name = "MyNativeJsType.Inner")
internal class MyNativeJsTypeInner(n: Int) {
  @JsProperty external fun getN(): Int
}

fun testNativeInnerClass() {
  var o = MyNativeJsTypeWithInner.Inner(3)
  assertTrue(o is MyNativeJsTypeWithInner.Inner)
  assertTrue((o as Any) is MyNativeJsTypeInner)
  assertEquals(3, o.n)
  o = (MyNativeJsTypeInner(4) as Any) as MyNativeJsTypeWithInner.Inner
  assertEquals(4, o.n)
}
