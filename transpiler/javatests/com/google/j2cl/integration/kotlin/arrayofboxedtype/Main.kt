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
package arrayofboxedtype

import com.google.j2cl.integration.testing.Asserts
import com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException

fun main(vararg unused: String) {
  testArrayInsertion()
  testArrayCast()
}

private fun assertThrowsArrayStoreException(array: Array<Any?>, o: Any?) {
  Asserts.assertThrowsArrayStoreException { array[0] = o }
}

private fun testArrayInsertion() {
  val booleanValue: Boolean? = true
  val characterValue: Char? = 'a'
  val byteValue: Byte? = 1
  val shortValue: Short? = 1
  val integerValue: Int? = 1
  val longValue: Long? = 1L
  val doubleValue: Double? = 1.0
  val floatValue: Float? = 1.0f
  val byteArray = arrayOfNulls<Byte>(2) as Array<Any?>
  byteArray[0] = byteValue
  assertThrowsArrayStoreException(byteArray, booleanValue)
  assertThrowsArrayStoreException(byteArray, characterValue)
  assertThrowsArrayStoreException(byteArray, doubleValue)
  assertThrowsArrayStoreException(byteArray, floatValue)
  assertThrowsArrayStoreException(byteArray, integerValue)
  assertThrowsArrayStoreException(byteArray, longValue)
  assertThrowsArrayStoreException(byteArray, shortValue)

  val doubleArray = arrayOfNulls<Double>(2) as Array<Any?>
  doubleArray[0] = doubleValue
  assertThrowsArrayStoreException(doubleArray, booleanValue)
  assertThrowsArrayStoreException(doubleArray, characterValue)
  assertThrowsArrayStoreException(doubleArray, byteValue)
  assertThrowsArrayStoreException(doubleArray, floatValue)
  assertThrowsArrayStoreException(doubleArray, integerValue)
  assertThrowsArrayStoreException(doubleArray, longValue)
  assertThrowsArrayStoreException(doubleArray, shortValue)

  val floatArray = arrayOfNulls<Float>(2) as Array<Any?>
  floatArray[0] = floatValue
  assertThrowsArrayStoreException(floatArray, booleanValue)
  assertThrowsArrayStoreException(floatArray, characterValue)
  assertThrowsArrayStoreException(floatArray, byteValue)
  assertThrowsArrayStoreException(floatArray, doubleValue)
  assertThrowsArrayStoreException(floatArray, integerValue)
  assertThrowsArrayStoreException(floatArray, longValue)
  assertThrowsArrayStoreException(floatArray, shortValue)

  val integerArray = arrayOfNulls<Int>(2) as Array<Any?>
  integerArray[0] = integerValue
  assertThrowsArrayStoreException(integerArray, booleanValue)
  assertThrowsArrayStoreException(integerArray, characterValue)
  assertThrowsArrayStoreException(integerArray, byteValue)
  assertThrowsArrayStoreException(integerArray, doubleValue)
  assertThrowsArrayStoreException(integerArray, floatValue)
  assertThrowsArrayStoreException(integerArray, longValue)
  assertThrowsArrayStoreException(integerArray, shortValue)

  val longArray = arrayOfNulls<Long>(2) as Array<Any?>
  longArray[0] = longValue
  assertThrowsArrayStoreException(longArray, booleanValue)
  assertThrowsArrayStoreException(longArray, characterValue)
  assertThrowsArrayStoreException(longArray, byteValue)
  assertThrowsArrayStoreException(longArray, doubleValue)
  assertThrowsArrayStoreException(longArray, floatValue)
  assertThrowsArrayStoreException(longArray, integerValue)
  assertThrowsArrayStoreException(longArray, shortValue)

  val shortArray = arrayOfNulls<Short>(2) as Array<Any?>
  shortArray[0] = shortValue
  assertThrowsArrayStoreException(shortArray, booleanValue)
  assertThrowsArrayStoreException(shortArray, characterValue)
  assertThrowsArrayStoreException(shortArray, byteValue)
  assertThrowsArrayStoreException(shortArray, doubleValue)
  assertThrowsArrayStoreException(shortArray, floatValue)
  assertThrowsArrayStoreException(shortArray, integerValue)
  assertThrowsArrayStoreException(shortArray, longValue)

  val characterArray = arrayOfNulls<Char>(2) as Array<Any?>
  characterArray[0] = characterValue
  assertThrowsArrayStoreException(characterArray, booleanValue)
  assertThrowsArrayStoreException(characterArray, byteValue)
  assertThrowsArrayStoreException(characterArray, doubleValue)
  assertThrowsArrayStoreException(characterArray, floatValue)
  assertThrowsArrayStoreException(characterArray, integerValue)
  assertThrowsArrayStoreException(characterArray, longValue)
  assertThrowsArrayStoreException(characterArray, shortValue)

  val booleanArray = arrayOfNulls<Boolean>(2) as Array<Any?>
  booleanArray[0] = booleanValue
  assertThrowsArrayStoreException(booleanArray, characterValue)
  assertThrowsArrayStoreException(booleanArray, byteValue)
  assertThrowsArrayStoreException(booleanArray, doubleValue)
  assertThrowsArrayStoreException(booleanArray, floatValue)
  assertThrowsArrayStoreException(booleanArray, integerValue)
  assertThrowsArrayStoreException(booleanArray, longValue)
  assertThrowsArrayStoreException(booleanArray, shortValue)

  val numberArray = arrayOfNulls<Number>(2) as Array<Any?>
  numberArray[0] = byteValue
  numberArray[0] = doubleValue
  numberArray[0] = floatValue
  numberArray[0] = integerValue
  numberArray[0] = longValue
  numberArray[0] = shortValue
  numberArray[0] = SubNumber()
  assertThrowsArrayStoreException(numberArray, booleanValue)
  assertThrowsArrayStoreException(numberArray, characterValue)

  val subnumberArray = arrayOfNulls<SubNumber>(2) as Array<Any?>
  subnumberArray[0] = SubNumber()
  assertThrowsArrayStoreException(subnumberArray, booleanValue)
  assertThrowsArrayStoreException(subnumberArray, characterValue)
  assertThrowsArrayStoreException(subnumberArray, byteValue)
  assertThrowsArrayStoreException(subnumberArray, doubleValue)
  assertThrowsArrayStoreException(subnumberArray, floatValue)
  assertThrowsArrayStoreException(subnumberArray, integerValue)
  assertThrowsArrayStoreException(subnumberArray, longValue)
  assertThrowsArrayStoreException(subnumberArray, shortValue)
}

private fun castToByteException(array: Array<Any?>) {
  assertThrowsClassCastException {
    val unused = array as Array<Byte?>
  }
}

private fun castToDoubleException(array: Array<Any?>) {
  assertThrowsClassCastException {
    val unused = array as Array<Double?>
  }
}

private fun castToFloatException(array: Array<Any?>) {
  assertThrowsClassCastException {
    val unused = array as Array<Float?>
  }
}

private fun castToIntegerException(array: Array<Any?>) {
  assertThrowsClassCastException {
    val unused = array as Array<Int?>
  }
}

private fun castToLongException(array: Array<Any?>) {
  assertThrowsClassCastException {
    val unused = array as Array<Long?>
  }
}

private fun castToShortException(array: Array<Any?>) {
  assertThrowsClassCastException {
    val unused = array as Array<Short?>
  }
}

private fun castToCharacterException(array: Array<Any?>) {
  assertThrowsClassCastException {
    val unused = array as Array<Char?>
  }
}

private fun castToBooleanException(array: Array<Any?>) {
  assertThrowsClassCastException {
    val unused = array as Array<Boolean?>
  }
}

private fun castToNumberException(array: Array<Any?>) {
  assertThrowsClassCastException {
    val unused = array as Array<Number?>
  }
}

private fun testArrayCast() {
  val byteArray = arrayOfNulls<Byte>(2) as Array<Any?>
  val unusedByteArray = byteArray as Array<Byte?>
  var unusedNumberArray = byteArray as Array<Number?>
  castToDoubleException(byteArray)
  castToFloatException(byteArray)
  castToIntegerException(byteArray)
  castToLongException(byteArray)
  castToShortException(byteArray)
  castToCharacterException(byteArray)
  castToBooleanException(byteArray)

  val doubleArray = arrayOfNulls<Double>(2) as Array<Any?>
  val unusedDoubleArray = doubleArray as Array<Double?>
  unusedNumberArray = doubleArray as Array<Number?>
  castToByteException(doubleArray)
  castToFloatException(doubleArray)
  castToIntegerException(doubleArray)
  castToLongException(doubleArray)
  castToShortException(doubleArray)
  castToCharacterException(doubleArray)
  castToBooleanException(doubleArray)

  val floatArray = arrayOfNulls<Float>(2) as Array<Any?>
  val unusedFloatArray = floatArray as Array<Float?>
  unusedNumberArray = floatArray as Array<Number?>
  castToByteException(floatArray)
  castToDoubleException(floatArray)
  castToIntegerException(floatArray)
  castToLongException(floatArray)
  castToShortException(floatArray)
  castToCharacterException(floatArray)
  castToBooleanException(floatArray)

  val integerArray = arrayOfNulls<Int>(2) as Array<Any?>
  val unusedIntegerArray = integerArray as Array<Int?>
  unusedNumberArray = integerArray as Array<Number?>
  castToByteException(integerArray)
  castToDoubleException(integerArray)
  castToFloatException(integerArray)
  castToLongException(integerArray)
  castToShortException(integerArray)
  castToCharacterException(integerArray)
  castToBooleanException(integerArray)

  val longArray = arrayOfNulls<Long>(2) as Array<Any?>
  val unusedLongArray = longArray as Array<Long?>
  unusedNumberArray = longArray as Array<Number?>
  castToByteException(longArray)
  castToDoubleException(longArray)
  castToFloatException(longArray)
  castToIntegerException(longArray)
  castToShortException(longArray)
  castToCharacterException(longArray)
  castToBooleanException(longArray)

  val shortArray = arrayOfNulls<Short>(2) as Array<Any?>
  val unusedShortArray = shortArray as Array<Short?>
  unusedNumberArray = shortArray as Array<Number?>
  castToByteException(shortArray)
  castToDoubleException(shortArray)
  castToFloatException(shortArray)
  castToIntegerException(shortArray)
  castToLongException(shortArray)
  castToCharacterException(shortArray)
  castToBooleanException(shortArray)

  val characterArray = arrayOfNulls<Char>(2) as Array<Any?>
  val unusedCharacterArray = characterArray as Array<Char?>
  castToByteException(characterArray)
  castToDoubleException(characterArray)
  castToFloatException(characterArray)
  castToIntegerException(characterArray)
  castToLongException(characterArray)
  castToShortException(characterArray)
  castToBooleanException(characterArray)
  castToNumberException(characterArray)

  val booleanArray = arrayOfNulls<Boolean>(2) as Array<Any?>
  val unusedBooleanArray = booleanArray as Array<Boolean?>
  castToByteException(booleanArray)
  castToDoubleException(booleanArray)
  castToFloatException(booleanArray)
  castToIntegerException(booleanArray)
  castToLongException(booleanArray)
  castToShortException(booleanArray)
  castToCharacterException(booleanArray)
  castToNumberException(booleanArray)

  val numberArray = arrayOfNulls<Number>(2) as Array<Any?>
  unusedNumberArray = numberArray as Array<Number?>
  castToByteException(numberArray)
  castToDoubleException(numberArray)
  castToFloatException(numberArray)
  castToIntegerException(numberArray)
  castToLongException(numberArray)
  castToShortException(numberArray)
  castToCharacterException(numberArray)
  castToBooleanException(numberArray)

  // test cast from SubNumber[] to Number[].
  val subnumberArray = arrayOfNulls<SubNumber>(2) as Array<Any?>
  unusedNumberArray = subnumberArray as Array<Number?>
  val unusedSbArray = subnumberArray as Array<SubNumber?>

  // test cast between double[] and Double[]
  val o1 = arrayOfNulls<Double>(2) as Any
  val o2 = DoubleArray(2) as Any
  assertThrowsClassCastException {
    val unused = o1 as DoubleArray
  }
  assertThrowsClassCastException {
    val unused = o2 as Array<Double>
  }

  // test cast between boolean[] and Boolean[]
  val o3 = arrayOfNulls<Boolean>(2) as Any
  val o4 = BooleanArray(2) as Any
  assertThrowsClassCastException {
    val unused = o3 as BooleanArray
  }
  assertThrowsClassCastException {
    val unused = o4 as Array<Boolean>
  }
}
