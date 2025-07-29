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
package instanceofexpression

import java.io.Serializable

class InstanceofExpressions {
  fun testInstanceofClass() {
    val `object`: Any = InstanceofExpressions()
    var b: Boolean
    b = `object` is Any
    b = `object` is InstanceofExpressions
    b = `object` is String?
  }

  fun testInstanceofInterface() {
    val o = Any()
    var b: Boolean
    b = o is Serializable
    b = object : Serializable {} is Serializable?
  }

  fun testInstanceofBoxedType() {
    val b: Any? = null

    var a = b is Byte
    a = b is Byte?
    a = b is Double
    a = b is Double?
    a = b is Float
    a = b is Float?
    a = b is Int
    a = b is Int?
    a = b is Long
    a = b is Long?
    a = b is Short
    a = b is Short?
    a = b is Number
    a = b is Number?
    a = b is Char
    a = b is Char?
    a = b is Boolean
    a = b is Boolean?

    val d: Double? = null
    a = d is Any
    a = d is Number?
    a = d is Double?

    val primitiveFloat: Float = 1f
    a = primitiveFloat is Any
    a = primitiveFloat is Number
    a = primitiveFloat is Number?
    a = primitiveFloat is Float
    a = primitiveFloat is Float?

    val primitiveDouble = 1.0
    a = primitiveDouble is Any
    a = primitiveDouble is Number
    a = primitiveDouble is Number?
    a = primitiveDouble is Double
    a = primitiveDouble is Double?

    val primitiveBoolean = true
    a = primitiveBoolean is Any
    a = primitiveBoolean is Boolean
    a = primitiveBoolean is Boolean?
  }

  fun testInstanceOfArray() {
    val obj = Any()
    val objectArray = arrayOfNulls<Any>(0)
    val objectDoubleArray: Array<Array<Any?>> = arrayOf(arrayOfNulls<Any>(0))
    val stringArray = arrayOfNulls<String>(0)

    var a: Boolean
    a = obj is Array<*>?
    a = obj is Array<*> && obj.isArrayOf<Any>()
    a = obj is Array<*> && obj.isArrayOf<String>()
    a = obj is Array<*> && obj.isArrayOf<Array<Any>>()
    a = obj is Array<*> && obj.isArrayOf<Array<String>>()

    a = objectArray is Array<*>?
    a = objectArray.isArrayOf<Any>()
    a = objectArray.isArrayOf<String>()
    a = objectArray.isArrayOf<Array<Any>>()
    a = objectArray.isArrayOf<Array<String>>()

    a = objectDoubleArray is Array<*>?
    a = objectDoubleArray.isArrayOf<Any>()
    a = objectDoubleArray.isArrayOf<Array<Any>>()
    a = objectDoubleArray.isArrayOf<Array<String>>()

    a = stringArray is Array<*>?
    a = stringArray.isArrayOf<Any>()
    a = stringArray.isArrayOf<String>()

    a = obj is ByteArray
    a = obj is ShortArray
    a = obj is IntArray
    a = obj is LongArray
    a = obj is FloatArray
    a = obj is DoubleArray
    a = obj is CharArray
    a = obj is BooleanArray
  }

  fun testNotInstanceOf() {
    val foo = Any()
    val a = foo !is String
    val b = foo !is String?

    val bar: Any? = null
    val c = bar !is String
    val d = bar !is String?
  }

  fun testPrecedence() {
    val b = (if (false) "foo" else "bar") is String
  }
}
