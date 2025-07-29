/*
 * Copyright 2023 Google Inc.
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
package classliteral

fun main() {
  val voidClass: Class<Void> = Void.TYPE

  val primitiveBooleanClass: Class<Boolean> = Boolean::class.javaPrimitiveType!!
  val primitiveBooleanArrayClass: Class<BooleanArray> = BooleanArray::class.java
  val primitiveBooleanArrayArrayClass: Class<Array<BooleanArray>> = Array<BooleanArray>::class.java
  val booleanClass: Class<Boolean> = Boolean::class.java
  val booleanArrayClass: Class<Array<Boolean>> = Array<Boolean>::class.java
  val booleanArrayArrayClass: Class<Array<Array<Boolean>>> = Array<Array<Boolean>>::class.java

  val primitiveCharClass: Class<Char> = Char::class.javaPrimitiveType!!
  val primitiveCharArrayClass: Class<CharArray> = CharArray::class.java
  val primitiveCharArrayArrayClass: Class<Array<CharArray>> = Array<CharArray>::class.java
  val charClass: Class<Char> = Char::class.java
  val charArrayClass: Class<Array<Char>> = Array<Char>::class.java
  val charArrayArrayClass: Class<Array<Array<Char>>> = Array<Array<Char>>::class.java

  val primitiveByteClass: Class<Byte> = Byte::class.javaPrimitiveType!!
  val primitiveByteArrayClass: Class<ByteArray> = ByteArray::class.java
  val primitiveByteArrayArrayClass: Class<Array<ByteArray>> = Array<ByteArray>::class.java
  val byteClass: Class<Byte> = Byte::class.java
  val byteArrayClass: Class<Array<Byte>> = Array<Byte>::class.java
  val byteArrayArrayClass: Class<Array<Array<Byte>>> = Array<Array<Byte>>::class.java

  val primitiveShortClass: Class<Short> = Short::class.javaPrimitiveType!!
  val primitiveShortArrayClass: Class<ShortArray> = ShortArray::class.java
  val primitiveShortArrayArrayClass: Class<Array<ShortArray>> = Array<ShortArray>::class.java
  val shortClass: Class<Short> = Short::class.java
  val shortArrayClass: Class<Array<Short>> = Array<Short>::class.java
  val shortArrayArrayClass: Class<Array<Array<Short>>> = Array<Array<Short>>::class.java

  val primitiveIntClass: Class<Int> = Int::class.javaPrimitiveType!!
  val primitiveIntArrayClass: Class<IntArray> = IntArray::class.java
  val primitiveIntArrayArrayClass: Class<Array<IntArray>> = Array<IntArray>::class.java
  val intClass: Class<Int> = Int::class.java
  val intArrayClass: Class<Array<Int>> = Array<Int>::class.java
  val intArrayArrayClass: Class<Array<Array<Int>>> = Array<Array<Int>>::class.java

  val primitiveLongClass: Class<Long> = Long::class.javaPrimitiveType!!
  val primitiveLongArrayClass: Class<LongArray> = LongArray::class.java
  val primitiveLongArrayArrayClass: Class<Array<LongArray>> = Array<LongArray>::class.java
  val longClass: Class<Long> = Long::class.java
  val longArrayClass: Class<Array<Long>> = Array<Long>::class.java
  val longArrayArrayClass: Class<Array<Array<Long>>> = Array<Array<Long>>::class.java

  val primitiveFloatClass: Class<Float> = Float::class.javaPrimitiveType!!
  val primitiveFloatArrayClass: Class<FloatArray> = FloatArray::class.java
  val primitiveFloatArrayArrayClass: Class<Array<FloatArray>> = Array<FloatArray>::class.java
  val floatClass: Class<Float> = Float::class.java
  val floatArrayClass: Class<Array<Float>> = Array<Float>::class.java
  val floatArrayArrayClass: Class<Array<Array<Float>>> = Array<Array<Float>>::class.java

  val primitiveDoubleClass: Class<Double> = Double::class.javaPrimitiveType!!
  val primitiveDoubleArrayClass: Class<DoubleArray> = DoubleArray::class.java
  val primitiveDoubleArrayArrayClass: Class<Array<DoubleArray>> = Array<DoubleArray>::class.java
  val doubleClass: Class<Double> = Double::class.java
  val doubleArrayClass: Class<Array<Double>> = Array<Double>::class.java
  val doubleArrayArrayClass: Class<Array<Array<Double>>> = Array<Array<Double>>::class.java

  val anyClass: Class<Any> = Any::class.java
  val anyArrayClass: Class<Array<Any>> = Array<Any>::class.java
  val anyArrayArrayClass: Class<Array<Array<Any>>> = Array<Array<Any>>::class.java

  val nonAnyClass: Class<ClassLiteral> = ClassLiteral::class.java
  val nonAnyArrayClass: Class<Array<ClassLiteral>> = Array<ClassLiteral>::class.java
  val nonAnyArrayArrayClass: Class<Array<Array<ClassLiteral>>> =
    Array<Array<ClassLiteral>>::class.java

  var o: Any?
  o = ClassLiteral()::class.java
  o = ClassLiteral()
  o = o::class.java

  val i = 4
  o = i::class.java
  o = i::class.javaObjectType

  val intAsAny: Any = 4
  o = intAsAny::class.java
  o = intAsAny::class.javaObjectType

  val nullableInt: Int? = 4
  o = nullableInt!!::class.java
  o = nullableInt!!::class.javaObjectType

  val primitiveBooleanArrayClassObjectType: Class<BooleanArray> = BooleanArray::class.javaObjectType
  val primitiveBooleanArrayArrayClassObjectType: Class<Array<BooleanArray>> =
    Array<BooleanArray>::class.javaObjectType
  val booleanArrayClassObjectType: Class<Array<Boolean>> = Array<Boolean>::class.javaObjectType
  val booleanArrayArrayClassObjectType: Class<Array<Array<Boolean>>> =
    Array<Array<Boolean>>::class.javaObjectType
}

class ClassLiteral
