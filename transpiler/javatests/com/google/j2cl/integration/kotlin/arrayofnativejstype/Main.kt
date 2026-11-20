/*
 * Copyright 2026 Google Inc.
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
package arrayofnativejstype

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertNotNull
import com.google.j2cl.integration.testing.Asserts.assertTrue
import com.google.j2cl.integration.testing.TestUtils.isWasm
import java.io.Serializable
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsProperty
import jsinterop.annotations.JsType

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object") open class MyNativeType

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "String")
private class MyOtherNativeType

private fun testSingleDimension() {
  val arr = arrayOfNulls<MyNativeType>(10)
  assertTrue(arr.size == 10)

  val element = MyNativeType()
  arr[0] = element
  assertTrue(arr[0] === element)
}

private fun testMultiDimension() {
  val arr2d = Array(3) { arrayOfNulls<MyNativeType>(2) }
  assertTrue(arr2d.size == 3)
  assertTrue(arr2d[0].size == 2)

  val element = MyNativeType()
  arr2d[1][1] = element
  assertTrue(arr2d[1][1] === element)

  val arr2dVar = arrayOfNulls<Array<MyNativeType>>(3)
  assertTrue(arr2dVar.size == 3)
  assertTrue(arr2dVar[0] == null)
}

private fun testArrayLiteral() {
  val element1 = MyNativeType()
  val element2 = MyNativeType()
  val arr = arrayOf(element1, element2)

  assertTrue(arr.size == 2)
  assertTrue(arr[0] === element1 && arr[1] === element2)
}

private fun testEquality() {
  val arr1 = arrayOfNulls<MyNativeType>(10)
  val arr2 = arrayOfNulls<MyNativeType>(10)
  assertTrue(arr1 === arr1)
  assertTrue(arr1 !== arr2)
  assertTrue(arr1 != null)

  val undefinedArr = getUndefined()
  assertTrue(undefinedArr == null)
  assertTrue(undefinedArr !== arr1)
}

@JsProperty(namespace = JsPackage.GLOBAL, name = "undefined")
private external fun getUndefined(): Array<MyNativeType?>?

// Trick:
// Object(arr) call is effectively a pass-through without native.js.
// Also validates that passed instance is actually not a Wasm object.

@JsMethod(namespace = JsPackage.GLOBAL, name = "Object")
private external fun passThrough(arr: Array<MyNativeType?>): Array<MyNativeType?>

@JsMethod(namespace = JsPackage.GLOBAL, name = "Object")
private external fun passThrough2d(arr: Array<Array<MyNativeType?>>): Array<Array<MyNativeType?>>

@JsMethod(namespace = JsPackage.GLOBAL, name = "Array.isArray")
private external fun isArray(arr: Array<MyNativeType?>): Boolean

@JsMethod(namespace = JsPackage.GLOBAL, name = "Array.isArray")
private external fun isArray(arr: Array<Array<MyNativeType?>>): Boolean

private fun testJsBoundary() {
  val element = MyNativeType()
  val arr = arrayOf<MyNativeType?>(element)
  assertTrue(isArray(arr))

  val result = passThrough(arr)
  assertTrue(isArray(result))
  assertTrue(result.size == 1)
  assertTrue(result[0] === element)

  val arr2d = arrayOf(arrayOf<MyNativeType?>(element))
  assertTrue(isArray(arr2d))

  val result2d = passThrough2d(arr2d)
  assertTrue(isArray(result2d))
  assertTrue(result2d.size == 1 && result2d[0].size == 1)
  assertTrue(result2d[0][0] === element)
}

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
private interface SimpleJsTypeReturnFromNative {}

private fun testJsTypeArray_returnFromNative() {
  val array: Array<SimpleJsTypeReturnFromNative> = returnJsTypeFromNative()
  assertEquals(2, array.size)
  assertNotNull(array[0])
}

@JsMethod(namespace = "arrayofnativejstype.helper")
private external fun returnJsTypeFromNative(): Array<SimpleJsTypeReturnFromNative>

@JsType(isNative = true, namespace = "arrayofnativejstype")
private class SimpleJsTypeWithProperty {
  @JsProperty external fun getId(): Int
}

private fun testJsTypeArray_returnFromNativeWithACall() {
  val array: Array<SimpleJsTypeWithProperty> = returnJsTypeWithIdsFromNative()
  assertEquals(2, array[1].getId())
}

@JsMethod(namespace = "arrayofnativejstype.helper")
private external fun returnJsTypeWithIdsFromNative(): Array<SimpleJsTypeWithProperty>

private fun testJsType3DimArray_castFromNativeWithACall() {
  @Suppress("UNCHECKED_CAST")
  val array = returnJsType3DimFromNative() as Array<Array<Array<SimpleJsTypeWithProperty>>>
  assertEquals(1, array.size)
  assertEquals(2, array[0].size)
  assertEquals(3, array[0][0].size)
  assertEquals(2, array[0][0][1].getId())
}

@JsMethod(namespace = "arrayofnativejstype.helper")
private external fun returnJsType3DimFromNative(): Any?

@JsMethod(namespace = "arrayofnativejstype.helper")
private external fun getSimpleJsType(i: Int): SimpleJsTypeWithProperty

@Suppress("UNCHECKED_CAST")
private fun testObjectArrayAssignment() {
  val array = arrayOfNulls<MyNativeType>(10) as Array<Any?>

  array[0] = MyNativeType()
  array[0] = MyOtherNativeType()
  array[0] = Any()

  val other1 = array as Array<MyOtherNativeType?>
  val other2 = emptyArray<Any?>() as Array<MyOtherNativeType?>
}

private fun testObjectArray_castFromNative() {
  val array = returnObjectArrayFromNative() as Array<SimpleJsTypeWithProperty>
  assertNotNull(array)
  assertEquals(3, array.size)
  assertEquals("1", array[0])
}

@Suppress("UNCHECKED_CAST")
private fun testJsTypeArray_objectArrayInterchangeability() {
  // In Wasm, Java Object[] and native JsType arrays have different representations and are not
  // interchangeable.
  if (isWasm()) {
    return
  }
  val objArray = arrayOfNulls<Any?>(1)

  val array = objArray as Array<Array<Array<SimpleJsTypeWithProperty>>>

  objArray[0] = arrayOfNulls<Any?>(2)
  (objArray[0] as Array<Any?>)[0] = arrayOfNulls<Any?>(3)
  array[0][0][1] = getSimpleJsType(2)
  assertEquals(1, array.size)
  assertEquals(2, array[0].size)
  assertEquals(3, array[0][0].size)
  assertEquals(2, array[0][0][1].getId())
}

@Suppress("UNCHECKED_CAST")
private fun testInstanceOf() {
  val array = arrayOfNulls<MyNativeType>(10) as Array<Any?>

  assertTrue(array.isArrayOf<MyNativeType>())
  assertTrue(array.isArrayOf<MyOtherNativeType>())
  assertTrue(array.isArrayOf<Any>())
}

@Suppress("USELESS_CAST")
private fun testObjectArray_instanceOf() {
  val array: Array<Any?> = emptyArray()
  assertTrue(array.isArrayOf<Any>())
  // TODO(b/184675805): Enable for Wasm when array metadata is fully implemented.
  if (!isWasm()) {
    assertFalse(array.isArrayOf<Double>())
  }
  assertFalse((array as Any) is IntArray)
  assertTrue(array is Serializable)
}

@Suppress("USELESS_CAST")
private fun testObjectArray_instanceOf_nativeJsType() {
  val array: Array<Any?> = emptyArray()
  assertFalse((array as Any) is SimpleJsTypeWithProperty)
  assertTrue(array.isArrayOf<SimpleJsTypeWithProperty>())
  assertTrue(array.isArrayOf<Array<SimpleJsTypeWithProperty>>())
  assertTrue(array.isArrayOf<Array<Array<SimpleJsTypeWithProperty>>>())
}

private fun testJsTypeArray_instanceOf() {
  val array: Any? = returnJsType3DimFromNative()
  if (!isWasm()) {
    // In Wasm, native JS arrays are not instances of Java Object[] or Serializable.
    assertTrue(array is Array<*> && array.isArrayOf<Any>())
    assertTrue(array is Serializable)
  }
  assertFalse(array is Array<*> && array.isArrayOf<Double>())
  assertFalse(array is IntArray)
}

private fun testJsTypeArray_instanceOf_nativeJsType() {
  val array: Any? = returnJsType3DimFromNative()
  assertFalse(array is SimpleJsTypeWithProperty)
  assertTrue(array is Array<*> && array.isArrayOf<SimpleJsTypeWithProperty>())
  assertTrue(array is Array<*> && array.isArrayOf<Array<SimpleJsTypeWithProperty>>())
  assertTrue(array is Array<*> && array.isArrayOf<Array<Array<SimpleJsTypeWithProperty>>>())
}

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
private interface NativeWildcardInterface {}

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "String")
private interface NativeStringInterface {}

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
private interface NativeObjectInterface {}

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "BufferSource")
private interface NativeTypedefInterface {}

@Suppress("UNCHECKED_CAST")
private fun testNativeArrays() {
  val nativeArray2d: Array<Array<NativeStringInterface>> = Array(10) { emptyArray() }
  nativeArray2d[1] = emptyArray<NativeObjectInterface>() as Array<NativeStringInterface>

  val nativeTypeDef2d: Array<Array<NativeTypedefInterface>> = Array(10) { emptyArray() }
  nativeTypeDef2d[1] = emptyArray<NativeObjectInterface>() as Array<NativeTypedefInterface>

  val nativeWildcard2d: Array<Array<NativeWildcardInterface>> = Array(10) { emptyArray() }
  nativeWildcard2d[1] = emptyArray<NativeObjectInterface>() as Array<NativeWildcardInterface>
}

private fun testNativeArrays_noMetadata() {
  assertHasNoMetadata(emptyArray<NativeStringInterface>())
  assertHasNoMetadata(emptyArray<Array<NativeStringInterface>>())
}

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Array") private class NativeArray

private fun assertHasNoMetadata(array: Any?) {
  assertEquals(nonNumericKeys(NativeArray()), nonNumericKeys(array))
}

@JsMethod(namespace = "arrayofnativejstype.helper")
private external fun returnObjectArrayFromNative(): Any?

@JsMethod(namespace = "arrayofnativejstype.helper")
private external fun nonNumericKeys(o: Any?): Array<String>

fun main() {
  testSingleDimension()
  testMultiDimension()
  testArrayLiteral()
  testEquality()
  testJsBoundary()
  testInstanceOf()
  testJsType3DimArray_castFromNativeWithACall()
  testJsTypeArray_instanceOf()
  testJsTypeArray_instanceOf_nativeJsType()
  testJsTypeArray_objectArrayInterchangeability()
  testJsTypeArray_returnFromNative()
  testJsTypeArray_returnFromNativeWithACall()
  testObjectArrayAssignment()
  testObjectArray_castFromNative()
  testObjectArray_instanceOf()
  testObjectArray_instanceOf_nativeJsType()
  testNativeArrays()
  testNativeArrays_noMetadata()
}
