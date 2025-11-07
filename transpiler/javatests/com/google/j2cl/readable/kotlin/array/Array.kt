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
package array

import java.io.Serializable
import java.lang.Cloneable
import javaemul.internal.annotations.Wasm
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsType

class Arrays {
  fun testObjectArray() {
    // Creation
    var objects = arrayOfNulls<Any>(100)
    objects = arrayOfNulls(0)
    objects = arrayOf(null, null)
    var objects2d = Array<Array<Any?>?>(5) { arrayOfNulls(10) }
    objects2d = arrayOf(arrayOf(null, null), null)
    objects2d = arrayOfNulls(20)

    // Access
    var anObject = objects[0]
    anObject = objects2d[0]!![1]

    // Assignment
    objects[0] = null
    objects2d[0]!![1] = null

    val nonNull2dArray = Array(1) { Array(2) { Any() } }
    // Access and assignment without non-null assertion
    anObject = nonNull2dArray[0][1]
    nonNull2dArray[1][2] = Any()
  }

  fun testJavaTypeArray() {
    // Creation
    var objects = arrayOfNulls<SomeObject?>(100)
    objects = arrayOfNulls(0)
    objects = arrayOf(null, null)
    var objects2d = Array<Array<SomeObject?>?>(5) { arrayOfNulls(10) }
    objects2d = arrayOf(arrayOf(null, null), null)
    objects2d = arrayOfNulls(20)

    // Access
    var someObject = objects[0]
    someObject = objects2d[0]!![1]

    // Assignment
    objects[0] = null
    objects2d[0]!![1] = null

    val nonNull2dArray = arrayOf(Array(2) { SomeObject() })
    // Access and assignment without non-null assertion
    someObject = nonNull2dArray[0][1]
    nonNull2dArray[1][2] = SomeObject()
  }

  fun testIntArrays() {
    // Creation
    var ints = IntArray(100)
    ints = IntArray(0)
    ints = intArrayOf(0, 1)
    var ints2d = Array<IntArray?>(5) { IntArray(10) }
    ints2d = arrayOf(intArrayOf(1, 2), null)
    ints2d = arrayOfNulls(20)

    // Access
    var n = ints[0]
    n = ints2d[0]!![1]

    // Assignment
    ints[0] = 1
    ints2d[0]!![1] = 1

    val nonNull2dArray = Array(1) { IntArray(2) }
    // Access and assignment without non-null assertion
    n = nonNull2dArray[0][1]
    nonNull2dArray[1][2] = 1

    // Widening conversion
    val b: Byte = 1
    val c = 'a'
    ints = IntArray(b.toInt())
    ints[b.toInt()] = b.toInt()
    ints = IntArray(c.toInt())
    ints[c.code] = c.code
    ints = intArrayOf(b.toInt(), c.code)
  }

  fun testLongArrays() {
    // Creation
    var longs = LongArray(100)
    longs = LongArray(0)
    longs = longArrayOf(0, 1)
    var longs2d = Array<LongArray?>(5) { LongArray(10) }
    longs2d = arrayOf(longArrayOf(1, 2), null)
    longs2d = arrayOfNulls(20)

    // Access
    var n = longs[0]
    n = longs2d[0]!![1]

    // Assignment
    longs[0] = 1
    longs2d[0]!![1] = 1

    val nonNull2dArray = Array(1) { LongArray(2) }
    // Access and assignment without non-null assertion
    n = nonNull2dArray[0][1]
    nonNull2dArray[1][2] = 1L

    // Widening conversion
    val b: Byte = 1
    val c = 'a'
    longs = LongArray(b.toInt())
    longs[b.toInt()] = b.toLong()
    longs = LongArray(c.toInt())
    longs[c.code] = c.code.toLong()
    longs = longArrayOf(b.toLong(), c.code.toLong())
  }

  fun testDevirtualizedTypeArrays() {
    // Creation
    var booleans: Array<Boolean?> = Array(100) { null }
    booleans = Array(0) { null }
    booleans = arrayOf(true, false)
    var booleans2d = Array<Array<Boolean?>?>(5) { Array(10) { null } }
    booleans2d = arrayOf(arrayOf(true, false), null)
    booleans2d = arrayOfNulls(20)

    // Access
    var b = booleans[0]
    b = booleans2d[0]!![1]

    // Assignment
    booleans[0] = true
    booleans2d[0]!![1] = false

    val nonNull2dArray = Array(1) { Array(2) { true } }
    // Access and assignment without non-null assertion
    b = nonNull2dArray[0][1]
    nonNull2dArray[1][2] = true
  }

  fun testStringArrays() {
    // Creation
    var strings = arrayOfNulls<String?>(100)
    strings = arrayOfNulls(0)
    strings = arrayOf(null, null)
    var strings2d = Array<Array<String?>?>(5) { Array(10) { null } }
    strings2d = arrayOf(arrayOf(null, null), null)
    strings2d = arrayOfNulls(20)

    // Access
    var b = strings[0]
    b = strings2d[0]!![1]

    // Assignment
    strings[0] = null
    strings2d[0]!![1] = null

    val nonNull2dArray = Array(1) { Array(2) { "" } }
    // Access and assignment without non-null assertion
    b = nonNull2dArray[0][1]
    nonNull2dArray[1][2] = ""
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "String") private class NativeType

  @Wasm("nop") // TODO(b/261079024) Remove when arrays of native types are supported.
  private fun testNativeArray() {
    // Creation
    var nativeObjects = arrayOfNulls<NativeType>(100)
    nativeObjects = arrayOfNulls(0)
    nativeObjects = arrayOf(null, null)
    var nativeObjects2d = Array<Array<NativeType?>?>(5) { arrayOfNulls(10) }
    nativeObjects2d = arrayOf(arrayOf(null, null), null)
    nativeObjects2d = arrayOfNulls<Array<NativeType?>?>(20)

    // Access
    var nativeObject = nativeObjects[0]
    nativeObject = nativeObjects2d[0]!![1]

    // Assignment
    nativeObjects[0] = null
    nativeObjects2d[0]!![1] = null
  }

  fun testErasureCastsOnArrayAccess() {
    val container: ArrayContainer<String> = ArrayContainer(arrayOf(""))
    val s = container.data[0]
  }

  fun testCovariance() {
    var objectArray: Array<out Any>? = null
    val stringArray: Array<String>? = null
    objectArray = stringArray
  }

  // Note: Array is not Cloneable and Serializable in Kotlin Native.
  fun testArraysSupertypeClosureTypes() {
    // TODO(b/458612609): `kotlin.Cloneable` is not supported and Kotlin Arrays do not implement
    // `java.lang.Cloneable`.
    // consumesCloneable(arrayOfNulls<Any?>(10))
    consumesSerializable(arrayOfNulls<Any?>(10))
  }

  fun testIterator() {
    val stringArray = Array<String?>(1) { null }
    for (e in stringArray) {}
    val byteArray = ByteArray(1)
    for (e in byteArray) {}
  }

  fun testArrayInitializer() {
    var ints = IntArray(10) { it * 2 }
    ints = IntArray(10, ::doubleIt)

    ints = IntArray(10) { x: Int? -> toNonNullable(x) }
    ints = IntArray(10, ::toNonNullable)

    ints = IntArray(10, ::acceptsVararg)
    ints = IntArray(10, ::tailingVararg)

    val initializer = { x: Int -> x }
    ints = IntArray(10, initializer)
    val otherInitializer = ::toNonNullable
    ints = IntArray(10, otherInitializer)

    var multidimensional = Array(10) { IntArray(1) { it * 2 } }

    var longs = LongArray(10) { it.toLong() }
    longs = LongArray(10, Int::toLong)
  }

  fun testArrayInitializerEscapesScope(): Int {
    val array =
      IntArray(5) {
        return@testArrayInitializerEscapesScope it + 1
      }
    return 0
  }

  fun consumesCloneable(cloneable: Cloneable?) {}

  fun consumesSerializable(serializable: Serializable?) {}

  fun testArrayAsTypeParameterBound() {
    val length = getLength(arrayOf(Any()))
  }
}

@Suppress("UPPER_BOUND_CANNOT_BE_ARRAY")
private fun <C> getLength(anArray: C): Int where C : Array<*>, C : Serializable {
  val i = anArray[0]
  return anArray.size
}

private fun toNonNullable(x: Int?): Int = x!!

private fun doubleIt(x: Int): Int = x * 2

private fun acceptsVararg(vararg x: Int): Int = x[0] * 2

private fun tailingVararg(x: Int, vararg rest: Int): Int = x * 2

private class ArrayContainer<T>(val data: Array<T>)

private class SomeObject
