/*
 * Copyright 2015 Google Inc.
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
package jsinteroptests

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertNotNull
import com.google.j2cl.integration.testing.Asserts.assertTrue
import com.google.j2cl.integration.testing.Asserts.fail
import java.io.Serializable
import jsinterop.annotations.JsFunction
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsProperty
import jsinterop.annotations.JsType

/** Tests JsType with array functionality. */
object JsTypeArrayTest {
  fun testAll() {
    testJsTypeArray()
    testJsType3DimArray_castFromNativeWithACall()
    testJsTypeArray_asAField()
    testJsTypeArray_asAParam()
    testJsTypeArray_instanceOf()
    testJsTypeArray_objectArrayInterchangeability()
    testJsTypeArray_returnFromNative()
    testJsTypeArray_returnFromNativeWithACall()
    testObjectArray_castFromNative()
    testObjectArray_instanceOf()
    testJsFunctionArray()
    testNativeArrays()
  }

  /* MAKE SURE EACH TYPE IS ONLY USED ONCE PER TEST CASE */
  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "String") private class SomeJsType

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  private class SomeOtherJsType

  @Suppress("UNCHECKED_CAST")
  private fun testJsTypeArray() {
    val array = arrayOfNulls<SomeJsType>(10) as Array<Any?>

    array[0] = SomeJsType()
    array[0] = SomeOtherJsType()
    array[0] = Any()

    assertTrue(array.isArrayOf<SomeJsType>())
    assertTrue(array.isArrayOf<SomeOtherJsType>())
    assertTrue(array.isArrayOf<Any>())

    val other1 = array as Array<SomeOtherJsType?>
    val other2 = emptyArray<Any?>() as Array<SomeOtherJsType?>
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  private interface SimpleJsTypeReturnFromNative {}

  private fun testJsTypeArray_returnFromNative() {
    val array: Array<SimpleJsTypeReturnFromNative> = returnJsTypeFromNative()
    assertEquals(2, array.size)
    assertNotNull(array[0])
  }

  @JsMethod
  @JvmStatic
  private external fun returnJsTypeFromNative(): Array<SimpleJsTypeReturnFromNative>

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  internal interface SimpleJsTypeReturnFromNativeWithAMethod {
    @JsProperty fun getId(): Int
  }

  private fun testJsTypeArray_returnFromNativeWithACall() {
    val array: Array<SimpleJsTypeReturnFromNativeWithAMethod> = returnJsTypeWithIdsFromNative()
    assertEquals(2, array[1].getId())
  }

  @JsMethod
  @JvmStatic
  private external fun returnJsTypeWithIdsFromNative():
    Array<SimpleJsTypeReturnFromNativeWithAMethod>

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  private interface SimpleJsTypeAsAField {}

  @JsType
  private class SimpleJsTypeAsAFieldHolder {
    var arrayField: Array<SimpleJsTypeAsAField>? = null
  }

  private fun testJsTypeArray_asAField() {
    val holder = SimpleJsTypeAsAFieldHolder()
    fillArrayField(holder)
    val array = holder.arrayField
    assertEquals(2, array!!.size)
    assertNotNull(array!![0])
  }

  @JsMethod @JvmStatic private external fun fillArrayField(holder: SimpleJsTypeAsAFieldHolder)

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  private interface SimpleJsTypeAsAParam {}

  @JsType
  private class SimpleJsTypeAsAParamHolder {
    internal var theParam: Array<SimpleJsTypeAsAParam>? = null

    fun setArrayParam(param: Array<SimpleJsTypeAsAParam>) {
      theParam = param
    }
  }

  private fun testJsTypeArray_asAParam() {
    val holder = SimpleJsTypeAsAParamHolder()
    fillArrayParam(holder)
    val array = holder.theParam
    assertEquals(2, array!!.size)
    assertNotNull(array!![0])
  }

  @JsMethod @JvmStatic private external fun fillArrayParam(holder: SimpleJsTypeAsAParamHolder)

  @JsType(isNative = true)
  private class SimpleJsTypeReturnForMultiDimArray {
    @JsProperty external fun getId(): Int
  }

  private fun testJsType3DimArray_castFromNativeWithACall() {
    @Suppress("UNCHECKED_CAST")
    val array =
      returnJsType3DimFromNative() as Array<Array<Array<SimpleJsTypeReturnForMultiDimArray>>>
    assertEquals(1, array.size)
    assertEquals(2, array[0].size)
    assertEquals(3, array[0][0].size)
    assertEquals(2, array[0][0][1].getId())
  }

  @JsMethod @JvmStatic private external fun returnJsType3DimFromNative(): Any?

  @JsMethod
  @JvmStatic
  private external fun getSimpleJsType(i: Int): SimpleJsTypeReturnForMultiDimArray

  private fun testObjectArray_castFromNative() {
    val array = returnObjectArrayFromNative() as Array<SimpleJsTypeReturnForMultiDimArray>
    assertNotNull(array)
    assertEquals(3, array.size)
    assertEquals("1", array[0])
  }

  @Suppress("UNCHECKED_CAST")
  private fun testJsTypeArray_objectArrayInterchangeability() {
    val objArray = arrayOfNulls<Any?>(1)

    val array = objArray as Array<Array<Array<SimpleJsTypeReturnForMultiDimArray>>>

    objArray[0] = arrayOfNulls<Any?>(2)
    (objArray[0] as Array<Any?>)[0] = arrayOfNulls<Any?>(3)
    array[0][0][1] = getSimpleJsType(2)
    assertEquals(1, array.size)
    assertEquals(2, array[0].size)
    assertEquals(3, array[0][0].size)
    assertEquals(2, array[0][0][1].getId())
  }

  @Suppress("USELESS_CAST")
  private fun testObjectArray_instanceOf() {
    val array: Array<Any?> = emptyArray()
    assertTrue(array.isArrayOf<Any>())
    assertFalse(array.isArrayOf<Double>())
    assertFalse((array as Any) is IntArray)
    assertFalse((array as Any) is SimpleJsTypeReturnForMultiDimArray)
    assertTrue(array.isArrayOf<SimpleJsTypeReturnForMultiDimArray>())
    assertTrue(array.isArrayOf<Array<SimpleJsTypeReturnForMultiDimArray>>())
    assertTrue(array.isArrayOf<Array<Array<SimpleJsTypeReturnForMultiDimArray>>>())
    assertTrue(array is Serializable)
  }

  private fun testJsTypeArray_instanceOf() {
    val array: Any? = returnJsType3DimFromNative()
    assertTrue(array is Array<*> && array.isArrayOf<Any>())
    assertFalse(array is Array<*> && array.isArrayOf<Double>())
    assertFalse(array is IntArray)
    assertFalse(array is SimpleJsTypeReturnForMultiDimArray)
    assertTrue(array is Array<*> && array.isArrayOf<SimpleJsTypeReturnForMultiDimArray>())
    assertTrue(array is Array<*> && array.isArrayOf<Array<SimpleJsTypeReturnForMultiDimArray>>())
    assertTrue(
      array is Array<*> && array.isArrayOf<Array<Array<SimpleJsTypeReturnForMultiDimArray>>>()
    )
    assertTrue(array is Serializable)
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

    assertHasNoMetadata(emptyArray<NativeStringInterface>())
    assertHasNoMetadata(emptyArray<Array<NativeStringInterface>>())

    val nativeTypeDef2d: Array<Array<NativeTypedefInterface>> = Array(10) { emptyArray() }
    nativeTypeDef2d[1] = emptyArray<NativeObjectInterface>() as Array<NativeTypedefInterface>

    val nativeWildcard2d: Array<Array<NativeWildcardInterface>> = Array(10) { emptyArray() }
    nativeWildcard2d[1] = emptyArray<NativeObjectInterface>() as Array<NativeWildcardInterface>
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Array") private class NativeArray

  private fun assertHasNoMetadata(array: Any?) {
    assertEquals(nonNumericKeys(NativeArray()), nonNumericKeys(array))
  }

  @JsFunction
  private fun interface SomeFunction {
    fun m(i: Int): Int
  }

  @JsFunction
  private fun interface SomeOtherFunction {
    fun n(o: Any?, j: Int): Int
  }

  private class SomeFunctionImplementation : SomeFunction {
    override fun m(i: Int): Int = 0
  }

  @Suppress("UNCHECKED_CAST")
  private fun testJsFunctionArray() {
    val someFunctionArray: Array<Any?> = arrayOfNulls<SomeFunction>(10) as Array<Any?>

    // @JsFunction arrays can contain any type of function.
    someFunctionArray[0] = returnSomeFunction()

    // @JsFunction arrays are interchangeable with each other.
    assertTrue(someFunctionArray.isArrayOf<SomeFunction>())
    assertTrue(someFunctionArray.isArrayOf<SomeOtherFunction>())
    assertFalse(emptyArray<Any?>().isArrayOf<SomeOtherFunction>())

    // @JsFunction arrays are subclasses of Object[] which are Serializable.
    assertTrue(someFunctionArray is Serializable)

    // @JsFunction arrays are interchangeable
    val someOtherFunctionArray = someFunctionArray as Array<SomeOtherFunction>

    someOtherFunctionArray[0] = { a, b -> 2 }
    // @JsFunction arrays accept any @JsFunction implementation.
    someFunctionArray[2] = SomeOtherFunction { a, b -> 2 }
    (someOtherFunctionArray as Array<Any?>)[0] = SomeFunctionImplementation()

    val someFunctionImplementationArray: Array<Any?> =
      arrayOfNulls<SomeFunctionImplementation>(1) as Array<Any?>

    // @JsFunction arrays variables can reference any function implementation implementation arrays,
    // whether its the declared @JsFunction interface or not.
    val aSomeFunctionArray = someFunctionImplementationArray as Array<SomeFunction>
    val aSomeOtherFunctionArray = someFunctionImplementationArray as Array<SomeOtherFunction>

    // @JsFunction implementation arrays can only contain particular functions.
    someFunctionImplementationArray[0] = SomeFunctionImplementation()
    try {
      // SomeFunctionImplementation[] arrays can only contain the specific JsFunction
      // implementation.
      someFunctionImplementationArray[0] = SomeFunction { a -> a + 1 }
      fail("Should have thrown")
    } catch (expected: ArrayStoreException) {}

    try {
      // @JsFunction arrays can only contain functions.
      someFunctionArray[1] = Object()
      fail("Should have thrown")
    } catch (expected: ArrayStoreException) {}
  }

  @JsMethod @JvmStatic private external fun returnObjectArrayFromNative(): Any?

  @JsMethod @JvmStatic private external fun returnSomeFunction(): Any?

  @JsMethod @JvmStatic private external fun nonNumericKeys(o: Any?): Array<String>
}
